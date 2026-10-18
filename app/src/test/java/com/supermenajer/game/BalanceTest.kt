package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Game
import org.junit.Test
import kotlin.random.Random

/** Uzun dönem denge raporu: güç enflasyonu ve ekonomi. */
class BalanceTest {
    private fun playDay(g: Game, rnd: Random) {
        g.todayUserFixture()?.let { f ->
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame(); Season.matchNews(g, this) }
        }
        Season.simulateRest(g, g.week, rnd)
        Season.endDay(g, rnd)
        if (g.fired) Season.takeJob(g, g.jobOffers.first())
    }

    @Test
    fun tenSeasons() {
        val rnd = Random(5)
        val g = Generator.newWorld(rnd)
        g.userTeamId = 5
        Season.start(g, rnd)
        repeat(10) {
            while (!g.seasonOver) playDay(g, rnd)
            val sl = g.table(0)
            val str = g.divisionTeams(0).map { Lineups.strength(g, it) }
            val eu = g.divisionTeams(2).map { Lineups.strength(g, it) }
            val active = g.players.filter { it.teamId >= 0 }
            println(
                "BAL ${g.season} SL güç ort=${"%.1f".format(str.average())} max=${"%.1f".format(str.max())} | AVR ort=${"%.1f".format(eu.average())}" +
                    " | 80+=${active.count { it.overall >= 80 }} 85+=${active.count { it.overall >= 85 }} max=${active.maxOf { it.overall }}" +
                    " | bütçe 1.=${sl[0].budget / 1000}K 2.=${sl[1].budget / 1000}K 9.=${sl[8].budget / 1000}K 18.=${sl[17].budget / 1000}K" +
                    " | maaş/limit 1.=${com.supermenajer.game.game.Economy.squadWages(g, sl[0].id) / 1000}K/${sl[0].wageBudget / 1000}K" +
                    " 18.=${com.supermenajer.game.game.Economy.squadWages(g, sl[17].id) / 1000}K/${sl[17].wageBudget / 1000}K" +
                    " | serbest=${g.players.count { it.teamId < 0 }}" +
                    " | SEN ${g.userTeam.short} ${g.position(g.userTeamId)}. bütçe=${g.userTeam.budget / 1000}K rapor=${g.userTeam.lastReport.map { "${it.key.take(8)}:${it.value / 1000}K" }}"
            )
            Season.newSeason(g, rnd)
        }
    }
}
