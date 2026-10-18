package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Team
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Kulüp ekonomisi. Maaşlar transfer bütçesinden ödenmez; her kulübün haftalık bir maaş limiti vardır.
 * Transfer bütçesine para; satışlar, kupa/Avrupa ödülleri, bilet gelirleri ve sezon sonu yönetim
 * ödeneğiyle (lig sırası + hedef primi) gelir. Başarı arttıkça bütçe ve itibar büyür.
 */
object Economy {
    const val TICKETS = "Bilet gelirleri"
    const val CUP = "Türkiye Kupası"
    const val EURO = "Avrupa Kupası"
    const val SALES = "Oyuncu satışları"
    const val BUYS = "Transfer harcamaları"
    const val BOARD = "Yönetim ödeneği"
    const val PRIZE = "Lig sıralama ödülü"
    const val BONUS = "Hedef primi"
    const val SCOUTING = "Gözlemci giderleri"
    const val STAFF = "Teknik ekip tazminatı"
    const val BUILD = "İnşaat ve tesisler"

    private val superPrize = listOf(
        20_000_000L, 13_000_000L, 9_000_000L, 7_000_000L, 5_500_000L, 4_500_000L, 4_000_000L, 3_500_000L, 3_000_000L,
        2_700_000L, 2_400_000L, 2_200_000L, 2_000_000L, 1_800_000L, 1_600_000L, 1_000_000L, 800_000L, 600_000L,
    )
    private val firstPrize = listOf(5_000_000L, 4_000_000L, 3_000_000L) + List(15) { 1_000_000L - it * 50_000L }

    private fun r5k(v: Double): Long = (v / 5_000).roundToLong() * 5_000

    /** Haftalık maaş limiti: itibara göre büyür. */
    fun wageCap(t: Team): Long {
        val base = 100_000.0 * 1.0349.pow(t.reputation - 56)
        val lg = Leagues.of(t.division)
        val divF = when {
            lg == null -> 1.3
            lg.tier == 2 -> 0.55 * kotlin.math.sqrt(lg.wealth)
            else -> kotlin.math.sqrt(lg.wealth)
        }
        return r5k(base * divF).coerceAtLeast(40_000)
    }

    /** Haftalık maaş yükü; kullanıcının takımında teknik ekip maaşları da dahil. */
    fun squadWages(g: Game, teamId: Int): Long =
        g.squad(teamId).sumOf { it.wage } + if (teamId == g.userTeamId) StaffLogic.wages(g) else 0L

    /** Yeni maaşla birlikte limit aşılıyor mu? */
    fun fitsWages(g: Game, t: Team, extraWage: Long, minusWage: Long = 0): Boolean =
        squadWages(g, t.id) - minusWage + extraWage <= t.wageBudget

    fun earn(t: Team, category: String, amount: Long) {
        t.budget += amount
        t.income[category] = (t.income[category] ?: 0L) + amount
    }

    /** Sezon başı: maaş limitleri ve gelir defteri sıfırlanır. */
    fun startSeason(g: Game) {
        for (t in g.teams) {
            t.wageBudget = wageCap(t)
            t.income = mutableMapOf()
        }
    }

    fun ticketIncome(t: Team): Long = 20_000L + t.reputation * 900L

    /** Sezon sonu yönetim ödeneği. Mali rapor lastReport'a yazılır. */
    fun seasonPayout(g: Game) {
        // Yapay zekâ kulüpleri harcamadığı paranın bir kısmını işletme giderlerine kullanır (sınırsız birikmesin).
        for (t in g.teams) if (t.id != g.userTeamId && t.budget > 0) t.budget = (t.budget * 0.6).toLong()
        for (t in g.divisionTeams(Leagues.LEGACY_EURO)) earn(t, BOARD, r5k(6_000_000.0 * (t.reputation / 70.0).pow(2)))
        for (div in Season.activeLeagues(g)) {
            val lg = Leagues.of(div) ?: continue
            val top = lg.tier == 1
            g.table(div).forEachIndexed { i, t ->
                val base = (if (top) 4_000_000.0 else 1_500_000.0) * (t.reputation / 70.0).pow(2)
                earn(t, BOARD, r5k(base))
                val prize = (if (top) superPrize else firstPrize).getOrElse(i) { 300_000L }
                earn(t, PRIZE, r5k(prize * lg.wealth))
                val target = Season.objectiveTarget(t.expected)
                val pos = i + 1
                val bonus = when {
                    pos <= target - 3 || (pos == 1 && target > 1) -> if (top) 6_000_000L else 2_500_000L
                    pos <= target -> if (top) 3_000_000L else 1_200_000L
                    else -> 0L
                }
                if (bonus > 0) earn(t, BONUS, bonus)
            }
        }
        for (t in g.teams) t.lastReport = t.income.toMutableMap()
    }

    /** Sözleşme uzatma için oyuncunun istediği maaş. null = uzatmak istemiyor. */
    fun renewalDemand(g: Game, p: Player): Long? {
        if (p.teamId < 0) return null
        val t = g.team(p.teamId)
        if (p.morale < 25) return null
        val star = p.overall >= Lineups.strength(g, t) + 5
        val f = (if (p.morale < 50) 1.2 else 1.0) * (if (star) 1.25 else 1.0)
        return ((p.demandWage * f) / 500).roundToLong() * 500
    }

    /** Sezon sonu sözleşmeler: bitenler ayrılır (yapay zekâ çoğunu yeniler). */
    fun processContracts(g: Game, rnd: Random) {
        val left = mutableListOf<String>()
        for (p in g.players.toList()) {
            if (p.teamId < 0) continue
            p.contractYears--
            if (p.contractYears > 0) continue
            val t = g.teamOrNull(p.teamId) ?: continue
            if (t.id != g.userTeamId && p.age <= 31 && p.overall >= Lineups.strength(g, t) - 8 && rnd.nextDouble() < 0.8) {
                p.contractYears = rnd.nextInt(2, 5)
                p.salary = p.demandWage
                continue
            }
            if (t.id == g.userTeamId) left += p.name
            t.lineup.replaceAll { if (it == p.id) -1 else it }
            g.movePlayer(p, -1)
            p.salary = 0
            p.contractYears = 0
        }
        if (left.isNotEmpty()) g.addNews("Sözleşmesi bitenler", "Sözleşmesi sona eren oyuncular kulüpten ayrıldı: ${left.joinToString()}.")
        // Serbest oyuncu havuzu çok büyümesin: en yaşlıları emekli olur.
        val free = g.squad(-1).sortedByDescending { it.age * 100 - it.overall }
        if (free.size > 150) g.removePlayers(free.take(free.size - 150))
    }
}
