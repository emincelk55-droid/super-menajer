package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Facilities
import com.supermenajer.game.game.Fans
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.Sponsors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class FansTest {
    @Test
    fun fanSatisfaction() {
        GameRepo.init(Files.createTempDirectory("sm10").toFile())
        val world = Generator.newWorld(Random(6)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[3].id)
        val g = GameRepo.game!!
        g.confidence = 100.0
        val us = g.userTeam
        assertEquals(60.0, g.fans, 0.001)

        // Etkiler: memnun taraftar daha çok seyirci ve daha iyi sponsor teklifi demek
        g.fans = 90.0
        val highSponsor = Sponsors.slotValue(g, us, 0)
        val highAtt = Fans.attendanceFactor(g, us)
        g.fans = 15.0
        assertTrue(Sponsors.slotValue(g, us, 0) < highSponsor)
        assertTrue(Fans.attendanceFactor(g, us) < highAtt)
        assertEquals(1.0, Fans.attendanceFactor(g, g.divisionTeams(0).first { it.id != us.id }), 0.0)

        // Yıldız satışı taraftarı kızdırır
        g.fans = 60.0
        val star = g.squad(us.id).maxBy { it.overall }
        val buyer = g.divisionTeams(10).first()
        buyer.budget = 500_000_000
        GameRepo.acceptOffer(com.supermenajer.game.game.Offer(star, buyer, 10_000_000))
        assertTrue(g.fans < 60.0)
        assertTrue(g.fanLog.first().contains("Yıldız satışı"))

        // Coşkulu taraftar evde destek verir
        g.fans = 95.0
        val home = g.fixtures.first { it.home == us.id && !it.played }
        assertTrue(Fans.homeBoost(g, home) > 0)
        val sim = MatchSim(g, home, us.id, autoUserSubs = true, rnd = Random(2))
        assertTrue(sim.home.onPitch.all { it.boost > 0 })
        assertTrue(sim.fixture.attendance <= us.stadium)

        // Bir sezon: memnuniyet 0-100 aralığında kalır, değişim geçmişi tutulur
        g.fans = 60.0
        while (!g.seasonOver && !g.fired) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        println("TARAFTAR: ${Fans.text(g)} · ${g.fanLog}")
        assertTrue(g.fans in 0.0..100.0)
        assertTrue(g.fanLog.isNotEmpty())
        assertTrue(Facilities.avgAttendance(us) > 0)
    }
}
