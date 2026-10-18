package com.supermenajer.game

import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.Facilities
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Comp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class FacilitiesTest {
    @Test
    fun stadiumAndFacilities() {
        GameRepo.init(Files.createTempDirectory("sm8").toFile())
        val world = Generator.newWorld(Random(3)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[4].id)
        val g = GameRepo.game!!
        val us = g.userTeam
        assertTrue(g.teams.all { it.stadium in 5_000..80_000 && it.trainingFac in 1..9 })
        println("STAT: ${us.name} ${us.stadiumName} ${us.stadium} · tesis ${us.trainingFac}/${us.youthFac}/${us.medicalFac}")

        // Proje: para düşer, N hafta sonra seviye artar
        us.budget = 50_000_000
        val lvl = us.trainingFac
        val cost = Facilities.upgradeCost(lvl)
        assertNull(Facilities.start(g, Facilities.TRAINING))
        assertEquals(50_000_000 - cost, us.budget)
        assertNotNull(Facilities.start(g, Facilities.TRAINING)) // aynı alanda ikinci inşaat yok
        assertNull(Facilities.start(g, Facilities.STADIUM, 0))
        val seats = us.stadium

        // Sezonu oyna: seyirci kapasiteyi aşmamalı, bilet geliri makul olmalı
        while (!g.seasonOver) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        val played = g.fixtures.filter { it.played }
        assertTrue(played.all { it.attendance in 1..g.team(it.home).stadium })
        assertTrue(played.filter { Comp.isLeague(it.comp) }.all { it.attendance > 0 })
        assertEquals(lvl + 1, us.trainingFac)
        assertEquals(seats + 5_000, us.stadium)
        assertTrue(g.projects.isEmpty())
        val tickets = us.lastReport[Economy.TICKETS] ?: 0L
        println("BİLET: ${tickets} · ortalama ${Facilities.avgAttendance(us)} / ${us.stadium}")
        assertTrue(tickets in 1_000_000L..12_000_000L)

        // Altyapı seçmeleri yeni sezonda gelir
        val before = g.squad(us.id).size
        val youthBefore = g.squad(us.id).count { it.age <= 17 }
        GameRepo.startNewSeason()
        println("KADRO: $before -> ${g.squad(us.id).size}, 17 yaş altı $youthBefore -> ${g.squad(us.id).count { it.age <= 17 }}")
        assertTrue(g.news.any { it.title.contains("Altyapı seçmeleri") })
        assertTrue(g.teams.all { it.trainingFac in 1..10 && it.youthFac in 1..10 })
    }
}
