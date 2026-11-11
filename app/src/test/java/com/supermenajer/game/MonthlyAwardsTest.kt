package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.MonthlyAwards
import com.supermenajer.game.game.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Aylık ödüller: birkaç ay oynanınca her ay oyuncu, genç ve menajer ödülü verilir. */
class MonthlyAwardsTest {
    @Test
    fun awardsEachMonth() {
        GameRepo.init(Files.createTempDirectory("smm").toFile())
        val w = Generator.newWorld(Random(12)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Ay", w.divisionTeams(0)[0].id)
        val g = GameRepo.game!!
        var n = 0
        while (g.monthAwards.size < 6 && !g.seasonOver && n++ < 200) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
            g.pendingEvent = null
        }
        val latest = MonthlyAwards.latest(g)
        println("AYLIK ${g.monthAwards.takeLast(6)}")
        assertTrue("en az iki ayın ödülü", g.monthAwards.map { it.split("|")[1] }.toSet().size >= 2)
        assertTrue(latest.any { it.kind == MonthlyAwards.PLAYER } && latest.any { it.kind == MonthlyAwards.MANAGER })
        assertTrue(g.monthAwardsNew)
    }
}
