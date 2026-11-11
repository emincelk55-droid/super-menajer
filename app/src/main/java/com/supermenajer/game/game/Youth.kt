package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player

/** Genç oyuncu gelişim yolu: potansiyel tahmini, gelişim hızı ve yardımcının önerisi. */
object Youth {
    class Row(
        val p: Player,
        /** Güç geçmişi: geçmiş sezon sonları + şimdiki güç. */
        val history: List<Int>,
        val potLow: Int,
        val potHigh: Int,
        /** Son sezondan bu yana artış. */
        val growth: Int,
        val loanClub: String?,
        val advice: String,
        /** 0 iyi gidiyor, 1 dikkat, 2 harekete geç. */
        val level: Int,
    )

    /** Gençlerimiz (21 yaş ve altı) ve kiralıktaki oyuncularımız. */
    fun rows(g: Game): List<Row> {
        val us = g.userTeam
        val str = Lineups.strength(g, us)
        val mine = g.squad(us.id).filter { it.age <= 21 && it.loanFrom < 0 }
        val loaned = g.players.filter { it.loanFrom == us.id }
        // Antrenör kalitesi tahminin isabetini belirler: iyi ekip potansiyeli dar aralıkla görür
        val spread = (6 - StaffLogic.q(g, StaffLogic.COACH) / 4).coerceIn(1, 6)
        return (mine + loaned).distinctBy { it.id }.map { p ->
            val hist = p.ovrHistory + p.overall
            val growth = if (p.ovrHistory.isNotEmpty()) p.overall - p.ovrHistory.last() else 0
            val loanClub = if (p.loanFrom == us.id) g.teamOrNull(p.teamId)?.name else null
            val ready = p.overall >= str - 3
            val (advice, level) = when {
                loanClub != null && p.apps < 3 && g.week > 10 -> "Kiralıkta forma bulamıyor (${p.apps} maç). Geri çağırmayı düşün." to 2
                loanClub != null -> "Kiralıkta ${p.apps} maç, ${p.goals} gol${if (p.apps > 0) ", ort. not %.1f".format(p.avgRating) else ""}." to 0
                ready -> "A takıma hazır. Düzenli forma verilebilir." to 0
                p.overall < str - 12 && p.apps < 3 && p.age >= 18 -> "Gelişmesi için maç oynaması lazım: kiralık önerilir." to 2
                p.potential - p.overall >= 12 -> "Yüksek potansiyel. Bireysel antrenmanla hızlandırılabilir." to 1
                growth <= 0 && p.ovrHistory.isNotEmpty() -> "Geçen sezondan beri gelişmedi. Antrenmanı gözden geçir." to 1
                else -> "Gelişimi normal seyrediyor." to 0
            }
            Row(p, hist, (p.potential - spread).coerceAtLeast(p.overall), (p.potential + spread).coerceAtMost(99), growth, loanClub, advice, level)
        }.sortedWith(compareByDescending<Row> { it.potHigh }.thenByDescending { it.p.overall })
    }
}
