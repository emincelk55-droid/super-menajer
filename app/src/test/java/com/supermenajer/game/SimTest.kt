package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SimTest {
    @Test
    fun simulateSeasons() {
        val rnd = Random(42)
        val g = Generator.newWorld(rnd)
        g.userTeamId = 9
        g.managerName = "Test"
        Season.start(g, rnd)
        repeat(3) { s ->
            var goals = 0; var matches = 0; var homeWins = 0; var draws = 0
            while (!g.seasonOver) {
                val w = g.week
                val f = g.nextUserFixture()!!
                val sim = MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd)
                sim.runToEnd(); sim.applyToGame()
                Season.simulateRest(g, w, rnd)
                for (x in g.weekFixtures(w)) {
                    goals += x.hg + x.ag; matches++
                    if (x.hg > x.ag) homeWins++ else if (x.hg == x.ag) draws++
                }
                Season.endWeek(g, rnd)
            }
            val t = g.table()
            println("Sezon ${g.season}: maç=$matches gol/maç=${"%.2f".format(goals.toDouble() / matches)} ev=${homeWins * 100 / matches}% ber=${draws * 100 / matches}%")
            t.forEachIndexed { i, team -> println("  ${i + 1}. ${team.name} beklenen=${team.expected} P=${team.points} ${team.gf}-${team.ga} bütçe=${team.budget / 1000}K") }
            val top = g.players.maxBy { it.goals }
            println("  Gol kralı: ${top.name} ${top.goals}  | oyuncu sayısı=${g.players.size}")
            assertTrue(matches == 306)
            Season.newSeason(g, rnd)
        }
    }
}
