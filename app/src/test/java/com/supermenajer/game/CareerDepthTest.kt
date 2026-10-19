package com.supermenajer.game

import com.supermenajer.game.game.Development
import com.supermenajer.game.game.Dynamics
import com.supermenajer.game.game.Events
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.ManagerMarket
import com.supermenajer.game.game.Perks
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** v1.5: bireysel antrenman, takım dinamikleri, olaylar, menajer pazarı, yetenek ağacı. */
class CareerDepthTest {
    private fun setup(): com.supermenajer.game.model.Game {
        GameRepo.init(Files.createTempDirectory("sm15").toFile())
        val world = Generator.newWorld(Random(15)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[5].id)
        return GameRepo.game!!.also { it.confidence = 100.0 }
    }

    @Test
    fun trainingAndLearning() {
        val g = setup()
        val p = g.squad(g.userTeamId).filter { it.age <= 23 && it.pos != com.supermenajer.game.model.Pos.GK }.maxBy { it.potential - it.overall }
        val target = Development.learnable(p).first()
        GameRepo.setLearn(p, target.ordinal)
        GameRepo.setFocus(p, 2)
        assertEquals(2, p.trainFocus)
        val pas = p.passing
        val rnd = Random(1)
        repeat(30) { Development.weekly(g, rnd) }
        assertEquals(target.ordinal, p.secondDet)
        assertEquals(-1, p.learnDet)
        assertTrue(p.passing > pas)
        assertEquals(0.98, Positions.fit(p, target), 0.001)
        assertTrue(g.news.any { it.title.contains("Yeni mevki") })
        // Aynı odak tekrar seçilince kapanır
        GameRepo.setFocus(p, 2)
        assertEquals(-1, p.trainFocus)
    }

    @Test
    fun dynamicsAndEvents() {
        val g = setup()
        val t = g.userTeam
        val tiers = Dynamics.tiers(g, t)
        assertEquals(3, tiers[0]!!.size)
        assertEquals(5, tiers[1]!!.size)
        assertTrue(Dynamics.groups(g, t).isNotEmpty())
        g.cohesion = 100.0
        val hi = Dynamics.matchFactor(g)
        g.cohesion = 0.0
        assertTrue(Dynamics.matchFactor(g) < hi)
        g.cohesion = 60.0
        g.squad(t.id).forEach { it.morale = 90.0 }
        Dynamics.weekly(g)
        assertTrue(g.cohesion > 60.0)

        val rnd = Random(3)
        var n = 0
        while (g.pendingEvent == null && n++ < 500) Events.weekly(g, rnd)
        val e = g.pendingEvent
        assertNotNull(e)
        assertTrue(e!!.options.size >= 2)
        val msg = GameRepo.resolveEvent(e.options.size - 1)
        assertTrue(msg.isNotEmpty())
        assertNull(g.pendingEvent)
    }

    @Test
    fun managerMarketAndPerks() {
        val g = setup()
        ManagerMarket.ensure(g)
        val rival = g.divisionTeams(g.userTeam.division).first { it.id != g.userTeamId }
        assertNotNull(g.managers[rival.id])
        val rnd = Random(9)
        var n = 0
        while (g.vacancies.none { it.teamId == rival.id } && n++ < 200) {
            g.mgrSecurity[rival.id] = 0.0
            rival.expected = 1
            ManagerMarket.weekly(g, 10, rnd)
        }
        assertTrue(g.vacancies.any { it.teamId == rival.id })
        assertTrue(g.managers[rival.id]!!.startsWith("Geçici"))
        g.managerRep = 100
        val r = GameRepo.applyJob(rival.id)
        assertTrue(r.isNotEmpty())
        assertTrue(g.vacancies.none { it.teamId == rival.id && !it.applied })

        // Yetenek ağacı
        assertEquals(1, Perks.level(0))
        assertEquals(0, Perks.points(g))
        val before = GameRepo.askingPrice(g.divisionTeams(0).first { it.id != g.userTeamId }.let { g.squad(it.id).first() })
        Perks.addXp(g, 120 + 160)
        assertEquals(3, Perks.level(g.managerXp))
        assertEquals(2, Perks.points(g))
        GameRepo.upgradePerk("nego")
        GameRepo.upgradePerk("nego")
        assertEquals(2, Perks.lvl(g, "nego"))
        assertEquals(0, Perks.points(g))
        assertTrue(GameRepo.upgradePerk("tactic").contains("puanın yok"))
        val after = GameRepo.askingPrice(g.divisionTeams(0).first { it.id != g.userTeamId }.let { g.squad(it.id).first() })
        assertTrue(after < before)
    }
}
