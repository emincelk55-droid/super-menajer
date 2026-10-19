package com.supermenajer.game

import com.supermenajer.game.game.GameCalendar
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

class CalendarTest {
    @Test
    fun dayByDay() {
        GameRepo.init(Files.createTempDirectory("sm12").toFile())
        val world = Generator.newWorld(Random(12)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[5].id, noFiring = true)
        val g = GameRepo.game!!
        g.confidence = 100.0
        val dates = GameCalendar.dates(g)
        assertTrue(dates.zipWithNext().all { (a, b) -> b > a })
        // Lig maçları cumartesi
        g.calendar.forEachIndexed { i, d -> if (d.type == 0) assertEquals(DayOfWeek.SATURDAY, LocalDate.ofEpochDay(dates[i]).dayOfWeek) }
        // Hazırlık dönemi: maç günü değil
        assertTrue(!g.isMatchDay)
        assertNull(g.todayUserFixture())
        println("TAKVİM: başlangıç ${GameCalendar.format(g.date)}, ilk maç ${GameCalendar.format(dates[0])}, son ${GameCalendar.format(dates.last())}")

        var steps = 0
        var stops = 0
        var matches = 0
        while (!g.seasonOver && !g.fired && steps < 2000) {
            val before = g.date
            val stop = GameRepo.advanceDay()
            steps++
            assertTrue(g.date >= before)
            if (stop) {
                stops++
                g.press = null
                g.offers.clear()
                if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary(); matches++ }
            }
        }
        println("TAKVİM: $steps adım, $stops durak, $matches maç")
        assertTrue(g.seasonOver)
        assertTrue(steps > 200)
        assertTrue(matches >= 34)
    }
}
