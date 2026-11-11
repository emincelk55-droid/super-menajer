package com.supermenajer.game

import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.WorldGen
import com.supermenajer.game.model.Leagues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Yeni ligler: Belçika, İskoçya, Brezilya, Arjantin kurulur; Avrupa kontenjanı 32'yi tutar; eski kayıtlara eklenir. */
class NewLeaguesTest {
    @Test
    fun newLeaguesExistAndEuroSlots() {
        val g = Generator.newWorld(Random(5))
        for (id in listOf(21, 22, 23, 24)) {
            val teams = g.divisionTeams(id)
            assertEquals("lig $id 18 takım", 18, teams.size)
            assertTrue(teams.all { g.squad(it.id).size >= 18 })
        }
        assertEquals(32, Leagues.topLeagues.sumOf { it.euroSlots })
        g.userTeamId = g.divisionTeams(21)[0].id
        Season.start(g, Random(5))
        val euro = g.euroTeams.map { g.team(it) }
        assertEquals(32, euro.size)
        assertTrue("Belçika Avrupa'da", euro.any { it.division == 21 })
        assertTrue("Brezilya Avrupa'da değil", euro.none { it.division == 23 || it.division == 24 })
        assertTrue("yeni liglerin kupaları", g.fixtures.any { it.comp == 37 } && g.fixtures.any { it.comp == 39 })
        assertTrue("yeni lig fikstürü", g.fixtures.any { it.comp == 23 })
    }

    @Test
    fun oldSaveGetsLeaguesNextSeason() {
        val g = Generator.newWorld(Random(8))
        val gone = g.teams.filter { it.division in 21..24 }.map { it.id }.toSet()
        g.removePlayers(g.players.filter { it.teamId in gone })
        g.teams.removeAll { it.id in gone }
        g.reindex()
        assertTrue(g.divisionTeams(23).isEmpty())
        assertTrue(WorldGen.addMissingLeagues(g, Random(1)))
        assertEquals(18, g.divisionTeams(23).size)
        assertTrue(!WorldGen.addMissingLeagues(g, Random(1)))
    }
}
