package com.supermenajer.game

import com.supermenajer.game.engine.MatchAdvice
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Maç içi diziliş değişikliği ve yardımcı antrenör önerileri. */
class MatchTacticsTest {
    @Test
    fun formationChangeAndAdvice() {
        GameRepo.init(Files.createTempDirectory("smt").toFile())
        val w = Generator.newWorld(Random(5)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "T", w.divisionTeams(0)[3].id)
        val g = GameRepo.game!!
        val f = g.fixtures.first { it.involves(g.userTeamId) }
        val sim = MatchSim(g, f, g.userTeamId, true, Random(2))
        val us = sim.userSide!!
        while (sim.minute < 30) sim.step()
        val target = Formations.map.keys.first { it != us.team.formation }
        val gkBefore = us.gk()!!.p.id
        assertTrue(sim.changeFormation(us, target))
        assertEquals(target, us.team.formation)
        val slots = Formations.map.getValue(target)
        assertTrue("herkes yeni dizilişte bir mevkide", us.onPitch.all { m -> slots.any { it === m.slot } })
        assertEquals(us.onPitch.size, us.onPitch.map { it.slot }.toSet().size)
        assertEquals(gkBefore, us.onPitch.first { it.slot.pos == Pos.GK }.p.id)
        assertEquals(11, us.team.lineup.size)

        // Gerideyken 60. dakikada yardımcı antrenör hücum önerir; uygulanınca mentalite değişir
        while (sim.minute < 60) sim.step()
        val op = sim.other(us)
        op.goals = us.goals + 1
        us.team.mentality = 1
        val tip = MatchAdvice.suggest(sim, us, emptySet())
        assertNotNull(tip)
        val attack = generateSequence { MatchAdvice.suggest(sim, us, setOf("press", "short", "tempo", "deep") + us.onPitch.flatMap { listOf("tired${it.p.id}", "card${it.p.id}") }) }.first()
        assertEquals("attack", attack?.key)
        attack!!.apply(sim, us)
        assertEquals(2, us.team.mentality)
        sim.runToEnd()
        assertTrue(sim.finished)
    }
}
