package com.supermenajer.game.game

import com.supermenajer.game.engine.Ev
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.Side
import com.supermenajer.game.model.Game

/** Sezon analizi: maç maç kayıt ve özet (gol beklentisi, form, gol dakikaları). */
object Analysis {
    class Match(
        val season: Int, val day: Int, val comp: Int, val opp: String, val home: Boolean,
        val gf: Int, val ga: Int, val xgf: Double, val xga: Double, val poss: Int, val shots: Int, val shotsAg: Int,
    ) {
        val points get() = if (gf > ga) 3 else if (gf == ga) 1 else 0
    }

    fun record(g: Game, sim: MatchSim, us: Side) {
        if (g.goalsByPeriod.size < 12) g.goalsByPeriod = MutableList(12) { 0 }
        val them = sim.other(us)
        val tp = (us.poss + them.poss).coerceAtLeast(1)
        g.matchLog += listOf(
            g.season, sim.fixture.week, sim.fixture.comp, them.team.short, if (sim.home === us) 1 else 0,
            us.goals, them.goals, "%.2f".format(java.util.Locale.US, us.xg), "%.2f".format(java.util.Locale.US, them.xg),
            us.poss * 100 / tp, us.shots, them.shots,
        ).joinToString("|")
        while (g.matchLog.size > 120) g.matchLog.removeAt(0)
        for (e in sim.events) if (e.type == Ev.GOAL) {
            val b = ((e.minute - 1) / 15).coerceIn(0, 5)
            if (e.side == us.index) g.goalsByPeriod[b]++ else g.goalsByPeriod[6 + b]++
        }
    }

    /** Bu sezonun maçları (eskiden yeniye). */
    fun season(g: Game): List<Match> = g.matchLog.mapNotNull { line ->
        val f = line.split("|")
        if (f.size < 12) return@mapNotNull null
        Match(f[0].toInt(), f[1].toInt(), f[2].toInt(), f[3], f[4] == "1", f[5].toInt(), f[6].toInt(), f[7].toDouble(), f[8].toDouble(), f[9].toInt(), f[10].toInt(), f[11].toInt())
    }.filter { it.season == g.season }

    /** Sezon başında dakika dilimleri sıfırlanır (maç kaydı geçmiş sezonları da tutar). */
    fun newSeason(g: Game) { g.goalsByPeriod = MutableList(12) { 0 } }
}
