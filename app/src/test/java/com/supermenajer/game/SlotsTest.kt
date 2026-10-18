package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class SlotsTest {
    @Test
    fun threeSlots() {
        GameRepo.init(Files.createTempDirectory("slots").toFile())
        assertFalse(GameRepo.hasSave())
        for (n in listOf(1, 3)) {
            GameRepo.slot = n
            val w = Generator.newWorld(Random(n)).also { Season.computeExpectations(it) }
            GameRepo.newCareer(w, "Hoca $n", w.divisionTeams(if (n == 1) 0 else 10)[n].id)
            GameRepo.flush()
        }
        val s = GameRepo.slots()
        println("YUVALAR: " + s.joinToString { "${it.n}:${it.exists}:${it.team}:${it.detail}" })
        assertTrue(s[0].exists && s[2].exists && !s[1].exists)
        assertEquals("Hoca 3", s[2].manager)
        val team3 = GameRepo.game!!.userTeam.name
        assertTrue(GameRepo.load(1))
        assertTrue(GameRepo.game!!.userTeam.name != team3)
        assertTrue(GameRepo.load(3))
        assertEquals(team3, GameRepo.game!!.userTeam.name)
        GameRepo.deleteSlot(1)
        assertFalse(GameRepo.slots()[0].exists)
    }
}
