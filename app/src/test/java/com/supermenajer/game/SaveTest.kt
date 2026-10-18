package com.supermenajer.game

import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Game
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class SaveTest {
    @Test
    fun roundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        val g = Generator.newWorld(Random(1))
        g.userTeamId = 10; g.managerName = "Test"
        Season.start(g, Random(1))
        repeat(10) { Season.simulateRest(g, g.week); Season.endDay(g) }
        val t0 = System.currentTimeMillis()
        val text = json.encodeToString(Game.serializer(), g)
        val t1 = System.currentTimeMillis()
        val back = json.decodeFromString(Game.serializer(), text)
        val t2 = System.currentTimeMillis()
        println("KAYIT boyut=${text.length / 1024}KB yazma=${t1 - t0}ms okuma=${t2 - t1}ms")
        assertEquals(g.players.size, back.players.size)
        assertEquals(g.teams.size, back.teams.size)
        assertEquals(g.fixtures.count { it.played }, back.fixtures.count { it.played })
        assertEquals(g.table(10).map { it.id }, back.table(10).map { it.id })
        assertEquals(g.squad(10).size, back.squad(10).size)
        assertEquals(g.userTeam.budget, back.userTeam.budget)
    }
}
