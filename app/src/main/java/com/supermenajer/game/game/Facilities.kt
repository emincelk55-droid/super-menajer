package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Project
import com.supermenajer.game.model.Team
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** Stadyum, seyirci, bilet geliri ve tesisler (antrenman, altyapı, sağlık). */
object Facilities {
    const val STADIUM = 0
    const val TRAINING = 1
    const val YOUTH = 2
    const val MEDICAL = 3
    val names = listOf("Stadyum", "Antrenman tesisi", "Altyapı akademisi", "Sağlık merkezi")
    val icons = listOf("🏟️", "🏋️", "🌱", "🏥")
    val priceNames = listOf("Ucuz", "Normal", "Pahalı")
    private val priceDemand = listOf(1.15, 1.0, 0.82)
    private val priceIncome = listOf(0.75, 1.0, 1.35)

    /** Taraftar tabanı: itibar ve lig seviyesine göre. */
    fun baseFans(t: Team): Double {
        val lg = Leagues.of(t.division)
        val tierF = if (lg?.tier == 2) 0.6 else 1.0
        return (6_000 + t.reputation * t.reputation * 6.0) * tierF
    }

    /** Henüz tesisi olmayan takımlara itibara uygun başlangıç değerleri. */
    fun ensure(t: Team) {
        if (t.stadium > 0) return
        t.stadium = ((baseFans(t) * 0.9 / 1000).roundToInt() * 1000).coerceIn(5_000, 80_000)
        val lvl = ((t.reputation - 35) / 7).coerceIn(1, 9)
        t.trainingFac = lvl
        t.youthFac = lvl
        t.medicalFac = lvl
        if (t.stadiumName.isEmpty()) t.stadiumName = t.name.substringBefore(" ") + " Arena"
    }

    fun ensureAll(g: Game) = g.teams.forEach { ensure(it) }

    /** Maç günü seyirci sayısı. */
    fun attendance(g: Game, f: Fixture, rnd: Random): Int {
        val h = g.team(f.home)
        ensure(h)
        val wins = h.form.count { it == 'G' }
        val formF = 0.88 + wins * 0.05
        val bigF = if (Talks.bigMatch(g, f)) 1.2 else 1.0
        val demand = baseFans(h) * formF * bigF * Fans.attendanceFactor(g, h) * priceDemand[h.ticketPrice] * rnd.nextDouble(0.93, 1.07)
        return demand.roundToInt().coerceIn(500, h.stadium)
    }

    /** Seyirci başı gelir (ligin zenginliğine ve bilet fiyatına göre). */
    fun perFan(t: Team): Double = 3.0 * (Leagues.of(t.division)?.wealth ?: 1.0) * priceIncome[t.ticketPrice]

    fun ticketIncome(g: Game, f: Fixture): Long {
        val h = g.team(f.home)
        val perk = if (h.id == g.userTeamId) Perks.income(g) else 1.0
        return (f.attendance * perFan(h) * perk).roundToLong()
    }

    /** Maç sonrası: seyirci kaydı. */
    fun record(g: Game, f: Fixture) {
        val h = g.team(f.home)
        h.lastAttendance = f.attendance
        h.attTotal += f.attendance
        h.attGames++
    }

    fun avgAttendance(t: Team): Int = if (t.attGames == 0) t.lastAttendance else (t.attTotal / t.attGames).toInt()

    // ---- Tesis etkileri (tüm takımlar) ----

    /** Antrenman tesisi: gelişim çarpanı (5 → 1.0, 10 → 1.13). */
    fun trainingFactor(level: Int) = 0.88 + level.coerceAtLeast(1) * 0.025
    fun trainingFactor(t: Team?) = if (t == null) 1.0 else trainingFactor(t.trainingFac)

    /** Sağlık merkezi: sakatlık riski çarpanı ve haftalık ek iyileşme şansı. */
    fun medicalRisk(level: Int) = 1.15 - level.coerceAtLeast(1) * 0.03
    fun medicalHeal(level: Int) = level.coerceAtLeast(1) * 0.015
    fun medicalRisk(t: Team?) = if (t == null) 1.0 else medicalRisk(t.medicalFac)
    fun medicalHeal(t: Team?) = if (t == null) 0.0 else medicalHeal(t.medicalFac)

    /** Altyapı akademisi: gençlerin güç ve potansiyel bonusu, altın çocuk ihtimali. */
    fun youthBonus(level: Int) = (level.coerceAtLeast(1) - 5) * 1.2
    fun wonderkidChance(level: Int) = 0.012 + level.coerceAtLeast(1) * 0.003
    fun youthBonus(t: Team) = youthBonus(t.youthFac)
    fun wonderkidChance(t: Team) = wonderkidChance(t.youthFac)

    // ---- Projeler ----

    fun upgradeCost(level: Int): Long = ((250_000 * level.toDouble().pow(1.5)) / 50_000).roundToLong() * 50_000
    fun upgradeWeeks(level: Int) = 6 + level

    class Option(val seats: Int, val cost: Long, val weeks: Int)

    fun stadiumOptions(t: Team): List<Option> {
        val w = Leagues.of(t.division)?.wealth ?: 1.0
        return listOf(
            Option(5_000, ((1_500_000 * w) / 50_000).roundToLong() * 50_000, 16),
            Option(10_000, ((3_200_000 * w) / 50_000).roundToLong() * 50_000, 26),
        )
    }

    fun level(t: Team, type: Int) = when (type) { TRAINING -> t.trainingFac; YOUTH -> t.youthFac; else -> t.medicalFac }

    fun busy(g: Game, type: Int) = g.projects.any { it.type == type }

    /** Kullanıcının projesini başlatır. Hata mesajı döner. */
    fun start(g: Game, type: Int, seatOption: Int = 0): String? {
        val us = g.userTeam
        ensure(us)
        if (busy(g, type)) return "Bu alanda zaten devam eden bir inşaat var."
        val project = if (type == STADIUM) {
            val o = stadiumOptions(us)[seatOption]
            if (us.stadium + o.seats > 90_000) return "Stadyum daha fazla büyütülemez."
            Project(STADIUM, o.seats, o.weeks, o.weeks, o.cost)
        } else {
            val lvl = level(us, type)
            if (lvl >= 10) return "${names[type]} zaten en üst seviyede."
            Project(type, lvl + 1, upgradeWeeks(lvl), upgradeWeeks(lvl), upgradeCost(lvl))
        }
        if (us.budget < project.cost) return "Bütçe yetersiz: ${money(project.cost)} gerekli."
        Economy.earn(us, Economy.BUILD, -project.cost)
        g.projects += project
        val what = if (type == STADIUM) "+${project.amount} koltuk" else "seviye ${project.amount}"
        g.addNews("🏗️ İnşaat başladı", "${names[type]} ($what) için çalışmalar başladı. ${project.weeksLeft} hafta sürecek.")
        return null
    }

    /** Lig haftası sonunda inşaatlar ilerler. */
    fun weekly(g: Game) {
        val us = g.userTeam
        val done = g.projects.filter { --it.weeksLeft <= 0 }
        for (p in done) {
            when (p.type) {
                STADIUM -> { us.stadium += p.amount; Fans.change(g, 4.0, "Stadyum büyüdü"); Career.unlock(g, "stadium") }
                TRAINING -> us.trainingFac = p.amount
                YOUTH -> us.youthFac = p.amount
                else -> us.medicalFac = p.amount
            }
            val what = if (p.type == STADIUM) "Yeni kapasite: ${"%,d".format(us.stadium).replace(',', '.')}" else "Yeni seviye: ${p.amount}/10"
            g.addNews("✅ İnşaat tamamlandı", "${names[p.type]} hazır! $what.")
        }
        g.projects.removeAll(done.toSet())
    }

    /** Sezon sonu: yapay zekâ kulüpleri de zenginse tesislerini geliştirir. */
    fun aiUpgrades(g: Game, rnd: Random) {
        for (t in g.teams) {
            if (t.id == g.userTeamId || t.division == Leagues.LEGACY_EURO) continue
            ensure(t)
            if (rnd.nextDouble() > 0.3) continue
            val type = listOf(TRAINING, YOUTH, MEDICAL).random(rnd)
            val lvl = level(t, type)
            val cost = upgradeCost(lvl)
            if (lvl >= 10 || t.budget < cost * 3) continue
            Economy.earn(t, Economy.BUILD, -cost)
            when (type) { TRAINING -> t.trainingFac++; YOUTH -> t.youthFac++; else -> t.medicalFac++ }
        }
        // Stadı sürekli dolan zengin kulüpler büyür
        for (t in g.teams) {
            if (t.id == g.userTeamId || t.attGames < 5) continue
            if (avgAttendance(t) >= t.stadium * 0.97 && t.budget > 15_000_000 && rnd.nextDouble() < 0.3) {
                val o = stadiumOptions(t)[0]
                Economy.earn(t, Economy.BUILD, -o.cost)
                t.stadium += o.seats
            }
        }
    }

    /** Sezon başı altyapı seçmeleri: akademinin seviyesine göre yeni gençler. */
    fun youthIntake(g: Game, rnd: Random) {
        if (g.fired) return
        val us = g.userTeam
        ensure(us)
        val count = 1 + us.youthFac / 4
        val names = mutableListOf<String>()
        repeat(count) {
            if (g.squad(us.id).size >= 30) return@repeat
            val pos = com.supermenajer.game.model.Pos.entries.random(rnd)
            val y = Generator.youth(g, us, pos, rnd)
            y.age = rnd.nextInt(16, 18)
            g.addPlayer(y)
            names += "${y.name} (${pos.short}, ${y.overall}, potansiyel ${y.potential}${if (y.potential >= 84) " 💎" else ""})"
            if (y.potential >= 84) Career.unlock(g, "wonderkid")
        }
        if (names.isNotEmpty()) {
            g.addNews("🌱 Altyapı seçmeleri", "Akademimizden (seviye ${us.youthFac}/10) A takıma yükselenler: ${names.joinToString()}.")
        }
    }

    /** Maç başında anlatımda kullanılmak üzere seyirci metni. */
    fun crowdText(sim: MatchSim): String {
        val n = sim.fixture.attendance
        if (n <= 0) return ""
        val h = sim.home.team
        val full = n >= h.stadium * 0.98
        return "${"%,d".format(n).replace(',', '.')} seyirci ${h.stadiumName}'da" + if (full) " · Stat tıklım tıklım!" else ""
    }

    @Suppress("unused")
    private fun isCup(f: Fixture) = Comp.isCup(f.comp)
}
