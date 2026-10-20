package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.random.Random

/**
 * A2 (rezerv) takımı: kullanıcının A takım kadrosunda yer bulamayan oyuncuları burada
 * haftalık maçlara çıkar. Gençler maç oynayarak gelişir, tecrübeliler ise burada mutsuz olur.
 */
object Reserves {
    fun squad(g: Game): List<Player> = g.squad(g.userTeamId).filter { it.reserve }

    /** A2'ye gönderilebilir mi (kadroda en az 18 A takım oyuncusu kalmalı). */
    fun canSend(g: Game, p: Player): String? = when {
        p.teamId != g.userTeamId -> "Oyuncu takımında değil."
        p.reserve -> "Zaten A2 takımında."
        g.squad(g.userTeamId).count { !it.reserve } <= 18 -> "A takımda en az 18 oyuncu kalmalı."
        else -> null
    }

    fun weekly(g: Game, leagueWeek: Int, rnd: Random) {
        if (g.fired) return
        val sq = squad(g)
        if (sq.size < 2) return
        val us = g.userTeam
        val opp = g.divisionTeams(us.division).filter { it.id != us.id }.randomOrNull(rnd) ?: return
        val avg = sq.sortedByDescending { it.overall }.take(11).map { it.overall }.average()
        val (x, y) = National.play(avg, opp.reputation * 0.85, rnd, if (rnd.nextBoolean()) 1.0 else -1.0)
        g.a2Results.add(0, "$leagueWeek. hafta|${us.short} A2 $x-$y ${opp.short} A2")
        while (g.a2Results.size > 20) g.a2Results.removeAt(g.a2Results.lastIndex)
        for (p in sq) {
            p.fitness = (p.fitness - 4).coerceAtLeast(70.0)
            if (p.age <= 22 && p.overall < p.potential && rnd.nextDouble() < 0.18 * Traits.devFactor(p)) {
                when (rnd.nextInt(6)) {
                    0 -> p.pace = (p.pace + 1).coerceAtMost(97)
                    1 -> p.shooting = (p.shooting + 1).coerceAtMost(97)
                    2 -> p.passing = (p.passing + 1).coerceAtMost(97)
                    3 -> p.dribbling = (p.dribbling + 1).coerceAtMost(97)
                    4 -> p.defending = (p.defending + 1).coerceAtMost(97)
                    else -> p.physical = (p.physical + 1).coerceAtMost(97)
                }
            }
            // 24 yaş üstü oyuncular A2'de olmaktan hoşlanmaz
            if (p.age >= 24) p.morale = (p.morale - 1.5).coerceAtLeast(5.0)
            else p.morale = (p.morale + 0.5).coerceAtMost(100.0)
        }
    }
}
