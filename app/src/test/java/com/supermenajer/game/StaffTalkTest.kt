package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.StaffLogic
import com.supermenajer.game.game.Talks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class StaffTalkTest {
    @Test
    fun staffAndTalks() {
        GameRepo.init(Files.createTempDirectory("sm7").toFile())
        val world = Generator.newWorld(Random(2)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        assertEquals(4, g.staff.size)

        // Daha iyi antrenör al: gelişim çarpanı artmalı, eskisine tazminat ödenmeli
        val before = StaffLogic.coachFactor(g)
        val best = StaffLogic.market(g, StaffLogic.COACH).first()
        val oldWage = StaffLogic.of(g, StaffLogic.COACH)!!.wage
        val budget = g.userTeam.budget
        val msg = GameRepo.hireStaff(best)
        println("İŞE ALIM: $msg · çarpan $before -> ${StaffLogic.coachFactor(g)}")
        assertEquals(best.id, StaffLogic.of(g, StaffLogic.COACH)!!.id)
        assertTrue(StaffLogic.coachFactor(g) > before)
        assertEquals(budget - oldWage * 4, g.userTeam.budget)
        assertEquals(4, g.staff.size)

        // Maç öncesi konuşma: maç içi motivasyon ve bir kez yapılabilir
        val f = g.todayUserFixture() ?: g.nextUserFixture()!!
        val sim = MatchSim(g, f, g.userTeamId, autoUserSubs = false, rnd = Random(1))
        val r = Talks.talk(sim, Talks.PRE, 3)
        println("KONUŞMA: ${Talks.contextText(sim)} → ${r.message} | ${r.reactions}")
        assertTrue(r.reactions.size == 3)
        assertTrue(sim.userSide!!.onPitch.any { it.boost != 0.0 })
        assertTrue(Talks.talk(sim, Talks.PRE, 0).message.isEmpty())
        repeat(45) { sim.step() }
        Talks.talk(sim, Talks.HALF, 1)
        assertTrue(sim.talkDone)
        sim.runToEnd()
        val post = Talks.talk(sim, Talks.POST, 0)
        assertTrue(post.message.isNotEmpty())
        assertTrue(sim.talksDone[Talks.POST])
    }
}
