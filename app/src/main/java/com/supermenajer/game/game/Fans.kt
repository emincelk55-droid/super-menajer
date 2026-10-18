package com.supermenajer.game.game

import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Team
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Taraftar memnuniyeti (0-100). Sonuçlar, derbiler, transferler, bilet fiyatı ve kupalarla değişir.
 * Etkileri: seyirci sayısı, sponsor teklifleri, yönetim güveni ve evde "12. adam" desteği.
 */
object Fans {
    fun mood(v: Double): String = when {
        v >= 85 -> "🔥 Coşkulu"
        v >= 70 -> "😃 Mutlu"
        v >= 55 -> "🙂 Memnun"
        v >= 40 -> "😐 Kararsız"
        v >= 25 -> "😠 Huzursuz"
        else -> "🤬 Öfkeli"
    }

    fun change(g: Game, delta: Double, reason: String) {
        if (g.fired || delta == 0.0) return
        g.fans = (g.fans + delta).coerceIn(0.0, 100.0)
        val d = delta.roundToInt()
        if (d == 0) return
        g.fanLog.add(0, "${if (d > 0) "+" else ""}$d $reason")
        while (g.fanLog.size > 6) g.fanLog.removeAt(g.fanLog.size - 1)
    }

    private fun isUser(g: Game, t: Team?) = t != null && !g.fired && t.id == g.userTeamId

    /** Seyirci talebi çarpanı (kullanıcının kulübü için). */
    fun attendanceFactor(g: Game, t: Team): Double = if (isUser(g, t)) 0.82 + g.fans / 250.0 else 1.0

    /** Sponsor teklifi çarpanı. */
    fun sponsorFactor(g: Game, t: Team): Double = if (isUser(g, t)) 0.85 + g.fans / 333.0 else 1.0

    /** Evde coşkulu taraftar: oyunculara küçük performans desteği. */
    fun homeBoost(g: Game, f: Fixture): Double =
        if (!g.fired && f.home == g.userTeamId && g.fans >= 75) 0.015 + (g.fans - 75) * 0.001 else 0.0

    /** Maç sonucu sonrası. */
    fun afterMatch(g: Game, f: Fixture) {
        if (g.fired || !f.played) return
        val us = g.userTeamId
        if (f.home != us && f.away != us) return
        val mine = if (f.home == us) f.hg else f.ag
        val theirs = if (f.home == us) f.ag else f.hg
        val opp = g.team(if (f.home == us) f.away else f.home)
        val diff = Lineups.strength(g, g.userTeam) - Lineups.strength(g, opp)
        val derby = Talks.isDerby(g, f)
        val big = Talks.bigMatch(g, f)
        val m = if (derby) 2.0 else if (big) 1.5 else 1.0
        val gd = mine - theirs
        val cupWin = f.winner == us && gd == 0
        val (delta, text) = when {
            gd > 0 || cupWin -> {
                val upset = if (diff < -3) 2.0 else 0.0
                val heavy = if (gd >= 3) 2.0 else 0.0
                (3.0 + upset + heavy) * m to when {
                    derby -> "Derbi zaferi: ${opp.name}"
                    upset > 0 -> "Sürpriz galibiyet: ${opp.name}"
                    gd >= 3 -> "Farklı galibiyet: ${opp.name}"
                    else -> "Galibiyet: ${opp.name}"
                }
            }
            gd == 0 -> (if (diff > 3) -1.5 else 0.5) * m to "Beraberlik: ${opp.name}"
            else -> {
                val heavy = if (gd <= -3) 3.0 else 0.0
                val shock = if (diff > 3) 1.5 else 0.0
                -(3.0 + heavy + shock) * m to when {
                    derby -> "Derbi yenilgisi: ${opp.name}"
                    gd <= -3 -> "Ağır yenilgi: ${opp.name}"
                    else -> "Yenilgi: ${opp.name}"
                }
            }
        }
        change(g, delta, text)
    }

    /** Lig haftası: bilet fiyatı etkisi, dengeye dönüş, protesto ve coşku haberleri. */
    fun weekly(g: Game, rnd: Random) {
        if (g.fired) return
        val us = g.userTeam
        g.fans += (55 - g.fans) * 0.02
        g.fans += when (us.ticketPrice) { 0 -> 0.4; 2 -> -0.6; else -> 0.0 }
        g.fans = g.fans.coerceIn(0.0, 100.0)
        g.confidence = (g.confidence + (g.fans - 50) / 50.0 * 0.4).coerceIn(0.0, 100.0)
        if (g.fans < 22 && rnd.nextDouble() < 0.25) {
            g.confidence = (g.confidence - 2).coerceAtLeast(0.0)
            g.addNews("📢 Taraftar protestosu", "${us.name} taraftarları stat önünde yönetimi ve teknik direktörü protesto etti. Yönetim tedirgin!")
        } else if (g.fans >= 88 && rnd.nextDouble() < 0.15) {
            g.addNews("🎉 Taraftar coştu", "${us.name} taraftarları şehirde konvoy yaptı! Tribünler tıklım tıklım, sponsorlar da memnun.")
        }
    }

    /** Transferler: yıldız satışı taraftarı kızdırır, büyük transfer coşturur. */
    fun sold(g: Game, p: Player) {
        val top = g.squad(g.userTeamId).plus(p).sortedByDescending { it.overall }.take(3)
        if (p in top) change(g, -6.0, "Yıldız satışı: ${p.name}")
    }

    fun bought(g: Game, p: Player) {
        val str = Lineups.strength(g, g.userTeam)
        if (p.overall >= str + 2) change(g, 4.0 + ((p.overall - str - 2) * 0.5).coerceAtMost(4.0), "Yıldız transferi: ${p.name}")
    }

    /** Sezon sonu: kupalar, hedef, düşme/yükselme. */
    fun seasonEnd(g: Game, titles: Int, metObjective: Boolean, missedBadly: Boolean) {
        if (titles > 0) change(g, 10.0 * titles, "Kupa sevinci ($titles)")
        if (g.userTeamId in g.euroQualified) change(g, 5.0, "Avrupa bileti")
        if (metObjective) change(g, 5.0, "Sezon hedefi tuttu") else if (missedBadly) change(g, -8.0, "Hayal kırıklığı yaratan sezon")
    }

    fun reset(g: Game) {
        g.fans = 55.0
        g.fanLog.clear()
    }

    fun text(g: Game): String = "${mood(g.fans)} · %${g.fans.roundToInt()}"

    @Suppress("unused")
    private fun near(a: Double, b: Double) = abs(a - b) < 1e-9
}
