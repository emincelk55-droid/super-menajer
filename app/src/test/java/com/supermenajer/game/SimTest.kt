package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Day
import com.supermenajer.game.model.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SimTest {
    private fun playDay(g: Game, rnd: Random) {
        val f = g.todayUserFixture()
        if (f != null) {
            val sim = MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd)
            sim.runToEnd(); sim.applyToGame(); Season.matchNews(g, sim)
        }
        Season.simulateRest(g, g.week, rnd)
        Season.endDay(g, rnd)
        if (g.fired) Season.takeJob(g, g.jobOffers.first())
    }

    private fun report(g: Game) {
        for (div in 0..1) {
            val fx = g.fixtures.filter { it.comp == div }
            val goals = fx.sumOf { it.hg + it.ag }
            println("${Comp.name(div)} ${g.season}: maç=${fx.size} gol/maç=${"%.2f".format(goals.toDouble() / fx.size)}")
            g.table(div).forEachIndexed { i, t -> if (i < 3 || i >= 15) println("  ${i + 1}. ${t.name} P=${t.points} bütçe=${t.budget / 1000}K") }
        }
        val cup = g.fixtures.filter { it.comp == Comp.CUP }
        println("  Kupa maçları=${cup.size} penaltı=${cup.count { it.pen.isNotEmpty() }} şampiyon=${g.team(g.cupWinner).name}")
        val euro = g.fixtures.filter { it.comp == Comp.EURO }
        if (euro.isNotEmpty()) {
            val turks = g.euroTeams.map { g.team(it) }.filter { g.countryOf(it) == "Türkiye" }
            val koTurks = euro.filter { it.round >= Comp.EURO_KO }.flatMap { listOf(it.home, it.away) }.distinct().count { g.countryOf(g.team(it)) == "Türkiye" }
            println("  Avrupa maçları=${euro.size} gol/maç=${"%.2f".format(euro.sumOf { it.hg + it.ag }.toDouble() / euro.size)} Türkler=${turks.map { it.short }} elemeye kalan Türk=$koTurks şampiyon=${g.team(g.euroWinner).name}")
        }
        println("  Kullanıcı: ${g.userTeam.name} güven=${g.confidence.toInt()} haber=${g.news.take(3).map { it.title }}")
    }

    @Test
    fun simulateSeasons() {
        val rnd = Random(7)
        val g = Generator.newWorld(rnd)
        assertEquals(234, g.teams.size)
        g.userTeamId = 30
        g.managerName = "Test"
        Season.start(g, rnd)
        repeat(3) {
            while (!g.seasonOver) playDay(g, rnd)
            assertEquals(306, g.fixtures.count { it.comp == Comp.SUPER && it.played })
            // Her ligdeki her takım 34 maç oynamış olmalı ve puan tablosu dolmalı
            for (id in Season.activeLeagues(g)) {
                for (t in g.divisionTeams(id)) assertEquals("${t.name} maç sayısı", 34, t.played)
                assertEquals(306 * 3 - g.fixtures.count { it.comp == id && it.hg == it.ag }, g.divisionTeams(id).sumOf { it.points })
            }
            assertTrue(g.fixtures.all { it.played })
            assertEquals(31, g.fixtures.count { it.comp == Comp.CUP })
            assertTrue(g.cupWinner >= 0)
            assertEquals(96 + 15, g.fixtures.count { it.comp == Comp.EURO && it.played })
            assertTrue(g.euroWinner >= 0)
            assertEquals(4, g.euroTeams.count { g.countryOf(g.team(it)) == "Türkiye" })
            assertEquals(32, g.euroTeams.size)
            // Her ülke kupası tamamlandı
            assertEquals(Season.activeCountries(g).size, g.cupWinners.size)
            report(g)
            Season.newSeason(g, rnd)
            assertEquals(18, g.divisionTeams(0).size)
        }
    }

    @Test
    fun migrateOldSave() {
        val rnd = Random(3)
        val g = Generator.newWorld(rnd)
        // v0.1 benzeri kayıt: sadece Süper Lig, takvimsiz, 10. haftada
        val lower = g.teams.filter { it.division != 0 }.map { it.id }.toSet()
        g.teams.removeAll { it.id in lower }
        g.players.removeAll { it.teamId in lower }
        g.saveVersion = 1
        g.reindex()
        g.userTeamId = 4
        g.calendar = MutableList(34) { Day(0, it + 1) }
        g.fixtures = Season.makeLeagueFixtures(g.teams.map { it.id }, Comp.SUPER, g.calendar, rnd).toMutableList()
        g.teams.forEach { Lineups.autoPick(g, it) }
        repeat(9) { playDay(g, rnd) }
        g.calendar = mutableListOf()
        Season.migrate(g, rnd)
        assertEquals(36, g.teams.size)
        assertTrue(g.worldUpgradePending)
        assertEquals(9 * 9, g.fixtures.count { it.comp == Comp.FIRST && it.played })
        assertEquals(10, g.saveVersion)
        assertTrue(g.euroTeams.isEmpty())
        while (!g.seasonOver) playDay(g, rnd)
        Season.newSeason(g, rnd)
        assertEquals(234, g.teams.size)
        assertTrue(!g.worldUpgradePending)
        assertEquals(32, g.euroTeams.size)
        while (!g.seasonOver) playDay(g, rnd)
        report(g)
    }

    @Test
    fun bigClubInEurope() {
        val rnd = Random(11)
        val g = Generator.newWorld(rnd)
        g.userTeamId = 0
        Season.start(g, rnd)
        assertTrue(g.userTeamId in g.euroTeams)
        // Aynı grupta aynı ülkeden iki takım olmamalı
        for (gi in 0 until 8) assertEquals(4, g.euroTeams.subList(gi * 4, gi * 4 + 4).map { g.countryOf(g.team(it)) }.toSet().size)
        val t0 = System.currentTimeMillis()
        while (!g.seasonOver) playDay(g, rnd)
        println("SÜRE: bir sezon ${System.currentTimeMillis() - t0} ms, ${g.totalDays} gün, ${g.fixtures.size} maç, ${g.players.size} oyuncu")
        report(g)
        println("  Haberler: " + g.news.filter { it.title.contains("Avrupa") }.take(6).joinToString(" | ") { it.title })
    }
}
