package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Team
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** v0.4 kaydı: Türkiye (2 lig) + 28 ülkesiz Avrupa kulübü (division 2), sezon ortasında. */
class MigrationV4Test {
    private fun playDay(g: Game, rnd: Random) {
        g.todayUserFixture()?.let { f ->
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame(); Season.matchNews(g, this, rnd) }
        }
        Season.simulateRest(g, g.week, rnd)
        Season.endDay(g, rnd)
        if (g.fired) Season.takeJob(g, g.jobOffers.first())
    }

    @Test
    fun legacyEuropeSave() {
        val rnd = Random(9)
        val g = Generator.newWorld(rnd)
        // Yeni ligleri sil, yerine 28 eski tip Avrupa kulübü koy
        val foreign = g.teams.filter { it.division >= 10 }
        g.removePlayers(g.players.filter { p -> foreign.any { it.id == p.teamId } })
        g.teams.removeAll(foreign)
        g.reindex()
        val countries = listOf("İngiltere", "İspanya", "Almanya", "İtalya", "Fransa", "Portekiz", "Hollanda", "İskoçya", "Belçika", "Hırvatistan")
        repeat(28) { i ->
            val t = Team(1000 + i, "Eski Kulüp $i", "E$i", countries[i % countries.size], 0xFF123456, 0xFFFFFFFF, 90 - i, 20_000_000, division = Leagues.LEGACY_EURO)
            g.addTeam(t)
            Generator.fillSquad(g, t, rnd, "GB")
        }
        g.userTeamId = 5
        g.managerName = "Eski"
        Season.start(g, rnd)
        assertEquals(32, g.euroTeams.size)
        g.saveVersion = 4
        repeat(20) { playDay(g, rnd) }
        // JSON üzerinden gidip gel (gerçek kayıt gibi)
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val loaded = json.decodeFromString(Game.serializer(), json.encodeToString(Game.serializer(), g))
        Season.migrate(loaded, rnd)
        assertEquals(12, loaded.saveVersion)
        assertTrue(loaded.worldUpgradePending)
        while (!loaded.seasonOver) playDay(loaded, rnd)
        assertTrue(loaded.fixtures.all { it.played })
        assertTrue(loaded.euroWinner >= 0)
        Season.newSeason(loaded, rnd)
        assertTrue(!loaded.worldUpgradePending)
        assertEquals(0, loaded.teams.count { it.division == Leagues.LEGACY_EURO })
        assertEquals(234, loaded.teams.size)
        assertEquals(32, loaded.euroTeams.size)
        assertTrue(loaded.players.none { p -> p.teamId >= 0 && loaded.teamOrNull(p.teamId) == null })
        while (!loaded.seasonOver) playDay(loaded, rnd)
        for (id in Season.activeLeagues(loaded)) for (t in loaded.divisionTeams(id)) assertEquals(34, t.played)
        println("GEÇİŞ TAMAM: ${loaded.userTeam.name} ${loaded.position(loaded.userTeamId)}. · Avrupa şampiyonu ${loaded.team(loaded.euroWinner).name} · kupalar ${loaded.cupWinners.size}")
    }
}
