package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player

/** Kulüp efsaneleri: oyuncuların kariyer kayıtlarından kulübümüzdeki maç ve gol sayıları. */
object Legends {
    class Row(val name: String, val apps: Int, val goals: Int, val active: Boolean)

    /** Oyuncunun bu kulüpteki toplam maç ve golü (geçmiş sezonlar + bu sezon). */
    fun atClub(g: Game, p: Player, club: String): Pair<Int, Int> {
        var apps = 0
        var goals = 0
        for (line in p.career) {
            val c = line.split("|")
            if (c.size >= 4 && c[1] == club) { apps += c[2].toIntOrNull() ?: 0; goals += c[3].toIntOrNull() ?: 0 }
        }
        if (p.teamId == g.userTeamId) { apps += p.apps; goals += p.goals }
        return apps to goals
    }

    fun table(g: Game): List<Row> {
        val club = g.userTeam.name
        return g.players.mapNotNull { p ->
            val (a, gl) = atClub(g, p, club)
            if (a > 0) Row(p.name, a, gl, p.teamId == g.userTeamId) else null
        } + g.hallOfFame.mapNotNull { s ->
            val c = s.split("|")
            if (c.size >= 3) Row(c[0], c[1].toInt(), c[2].toInt(), false) else null
        }
    }
}
