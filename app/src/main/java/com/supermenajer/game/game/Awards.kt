package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos

/** Sezon sonu ödül gecesi: kullanıcının liginde sezonun en iyileri (sezon istatistikleri sıfırlanmadan önce). */
object Awards {
    class Award(val icon: String, val title: String, val player: Player?, val teamName: String, val stat: String, val ours: Boolean)

    fun season(g: Game): List<Award> {
        val div = g.userTeam.division
        val league = g.players.filter { it.teamId >= 0 && g.teamOrNull(it.teamId)?.division == div }
        val maxApps = league.maxOfOrNull { it.apps } ?: 0
        val regulars = league.filter { it.apps >= (maxApps * 0.45).toInt().coerceAtLeast(1) }
        fun award(icon: String, title: String, p: Player?, stat: (Player) -> String) =
            Award(icon, title, p, p?.let { g.teamOrNull(it.teamId)?.name } ?: "-", p?.let(stat) ?: "-", p?.teamId == g.userTeamId)
        val list = mutableListOf(
            award("⭐", "Yılın Oyuncusu", regulars.maxByOrNull { it.avgRating + it.goals * 0.02 + it.assists * 0.015 }) { "%.2f ort. not · ${it.goals} gol · ${it.assists} asist".format(it.avgRating) },
            award("⚽", "Gol Kralı", league.filter { it.goals > 0 }.maxByOrNull { it.goals * 100 - it.apps }) { "${it.goals} gol · ${it.apps} maç" },
            award("🅰️", "Asist Kralı", league.filter { it.assists > 0 }.maxByOrNull { it.assists * 100 - it.apps }) { "${it.assists} asist" },
            award("🌱", "Yılın Genci", regulars.filter { it.age <= 21 }.maxByOrNull { it.avgRating }) { "${it.age} yaş · %.2f ort. not".format(it.avgRating) },
            award("🧤", "Yılın Kalecisi", regulars.filter { it.pos == Pos.GK }.maxByOrNull { it.avgRating }) { "%.2f ort. not".format(it.avgRating) },
        )
        val champ = g.table(div).firstOrNull()
        if (champ != null) {
            val mgr = if (champ.id == g.userTeamId) g.managerName else g.managers[champ.id] ?: "-"
            list += Award("🎩", "Yılın Menajeri", null, champ.name, "$mgr · ${champ.points} puanla şampiyon", champ.id == g.userTeamId)
        }
        return list
    }
}
