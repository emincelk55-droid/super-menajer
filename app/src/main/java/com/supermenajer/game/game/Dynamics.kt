package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Team
import kotlin.math.roundToInt

/**
 * FM tarzı takım dinamikleri: soyunma odası hiyerarşisi (takım liderleri, etkili oyuncular),
 * sosyal gruplar ve takım uyumu. Etkili oyuncuların morali gruplarına yayılır; uyum maç performansını etkiler.
 */
object Dynamics {
    val tierNames = listOf("👑 Takım liderleri", "⭐ Etkili oyuncular", "🙂 Diğer oyuncular")

    fun influence(g: Game, t: Team, p: Player): Double {
        var s = p.overall * 0.6 + (p.age - 18).coerceIn(0, 14) * 2.2 + p.apps * 0.3
        if (Traits.isLeader(p)) s += 25
        if (Roles.captain(g, t)?.id == p.id) s += 20
        if (p.id in t.lineup) s += 6
        if (p.loanFrom >= 0) s -= 15
        return s
    }

    /** 0 lider, 1 etkili, 2 diğer. */
    fun tiers(g: Game, t: Team): Map<Int, List<Player>> {
        val sorted = g.squad(t.id).sortedByDescending { influence(g, t, it) }
        return mapOf(0 to sorted.take(3), 1 to sorted.drop(3).take(5), 2 to sorted.drop(8))
    }

    fun groupOf(g: Game, t: Team, p: Player): String {
        val home = g.flagOf(t)
        return when {
            p.nation == home -> "Yerli oyuncular"
            p.nation in setOf("BR", "AR", "CO", "UY", "ES", "PT", "US") -> "Latin oyuncular"
            p.nation in setOf("SN", "NG", "CI", "GH", "CM", "MA", "DZ") -> "Afrikalı oyuncular"
            p.nation in setOf("JP", "KR") -> "Asyalı oyuncular"
            else -> "Avrupalı oyuncular"
        }
    }

    fun groups(g: Game, t: Team): Map<String, List<Player>> =
        g.squad(t.id).groupBy { groupOf(g, t, it) }.filter { it.value.size >= 2 }

    fun cohesionText(c: Double) = when {
        c >= 80 -> "Mükemmel uyum"
        c >= 65 -> "İyi uyum"
        c >= 45 -> "Ortalama uyum"
        c >= 30 -> "Zayıf uyum"
        else -> "Kopuk takım"
    }

    /** Maç gücü çarpanı (kullanıcı takımı): %97 - %103. */
    fun matchFactor(g: Game): Double = 0.97 + g.cohesion.coerceIn(0.0, 100.0) * 0.0006

    /** Haftalık: uyum ve etkili oyuncuların moral yayılımı. */
    fun weekly(g: Game) {
        if (g.fired) return
        val t = g.userTeam
        val sq = g.squad(t.id)
        if (sq.isEmpty()) return
        val avg = sq.sumOf { it.morale } / sq.size
        g.cohesion = (g.cohesion + 1.0 + (avg - 60) / 25.0).coerceIn(15.0, 100.0)
        val top = tiers(g, t).let { it[0].orEmpty() + it[1].orEmpty() }
        // Her oyuncu haftada en fazla ±1 etkilenir; kırgın oyuncu arkadaşlarının neşesiyle düzelmez
        val push = HashMap<Int, Int>()
        for (p in top) {
            val members = sq.filter { it.id != p.id && groupOf(g, t, it) == groupOf(g, t, p) }
            when {
                p.morale < 35 -> {
                    members.forEach { push[it.id] = (push[it.id] ?: 0) - 1 }
                    g.cohesion = (g.cohesion - 1).coerceAtLeast(15.0)
                }
                p.morale > 80 -> members.forEach { push[it.id] = (push[it.id] ?: 0) + 1 }
            }
        }
        for (m in sq) {
            val d = push[m.id] ?: continue
            if (d < 0) m.morale = (m.morale - 2).coerceAtLeast(5.0)
            else if (d > 0 && m.morale >= 40) m.morale = (m.morale + 1).coerceAtMost(100.0)
        }
    }

    /** Yeni transfer uyumu biraz bozar. */
    fun newSigning(g: Game) {
        g.cohesion = (g.cohesion - 2).coerceAtLeast(15.0)
    }

    /** Etkili bir oyuncu satılınca grubu üzülür. */
    fun soldPlayer(g: Game, p: Player) {
        val t = g.userTeam
        val tier = tiers(g, t)
        // Satılan oyuncu artık kadroda değil: etkisini kadrodaki yerine göre tahmin et
        val wasInfluential = influence(g, t, p) >= (tier[1]?.lastOrNull()?.let { influence(g, t, it) } ?: 999.0)
        if (!wasInfluential) return
        val grp = groupOf(g, t, p)
        g.squad(t.id).filter { groupOf(g, t, it) == grp }.forEach { it.morale = (it.morale - 4).coerceAtLeast(5.0) }
        g.cohesion = (g.cohesion - 5).coerceAtLeast(15.0)
        g.addNews("Soyunma odası sarsıldı", "${p.name}'in ayrılığı ${grp.lowercase()} arasında üzüntü yarattı. Takım uyumu düştü.")
    }

    fun pct(c: Double) = c.roundToInt()
}
