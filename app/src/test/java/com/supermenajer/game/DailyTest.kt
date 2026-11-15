package com.supermenajer.game

import com.supermenajer.game.game.AutoPilot
import com.supermenajer.game.game.Challenges
import com.supermenajer.game.game.DailyReward
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Günlük ödül serisi ve maç görevleri. */
class DailyTest {
    private var day = 20_000L

    private fun career(seed: Int) {
        GameRepo.init(Files.createTempDirectory("daily").toFile())
        val w = Generator.newWorld(Random(seed)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Test", w.divisionTeams(0).sortedBy { it.reputation }[8].id)
        GameRepo.game!!.introDone = true
        DailyReward.today = { day }
    }

    @After
    fun reset() { DailyReward.today = { java.time.LocalDate.now().toEpochDay() } }

    @Test
    fun streakRules() {
        career(1)
        val g = GameRepo.game!!
        val budget0 = g.userTeam.budget
        assertTrue(DailyReward.available(g))
        assertEquals(1, DailyReward.nextDay(g))
        GameRepo.claimDaily()
        assertEquals(1, g.dailyStreak)
        assertTrue("1. gün para ödülü", g.userTeam.budget > budget0)
        // Aynı gün ikinci kez alınamaz
        assertFalse(DailyReward.available(g))
        assertNull(DailyReward.claim(g))
        // Ertesi gün seri sürer
        day++; GameRepo.claimDaily(); assertEquals(2, g.dailyStreak)
        // Bir gün atlanırsa seri sıfırlanır
        day += 2; assertEquals(1, DailyReward.nextDay(g)); GameRepo.claimDaily(); assertEquals(1, g.dailyStreak)
        assertEquals(2, g.dailyBest)
        // 7 gün üst üste: başarım, 8. gün döngü başa döner
        repeat(6) { day++; GameRepo.claimDaily() }
        assertEquals(7, g.dailyStreak)
        assertTrue("daily7" in g.achievements)
        day++; assertEquals(1, DailyReward.nextDay(g))
    }

    @Test
    fun challengesEachMatch() {
        career(2)
        val g = GameRepo.game!!
        Challenges.refresh(g, Random(3))
        val ids = Challenges.list(g).map { it.c.id }
        assertEquals(3, ids.distinct().size)
        assertEquals("tam bir sonuç görevi", 1, ids.count { it in listOf("win", "score2", "clean") })
        val log = AutoPilot.Log()
        val rnd = Random(9)
        var n = 0
        val xp0 = g.managerXp
        while (log.matches < 12 && n++ < 600) AutoPilot.step(log, rnd)
        assertTrue("maçlar oynandı", log.matches >= 12)
        assertTrue("görev tamamlandı (${g.challengesDone})", g.challengesDone > 0)
        assertTrue("12 maçta 36 görevden azı", g.challengesDone < 36)
        assertEquals(3, g.lastChallenges.size)
        assertTrue(g.lastChallenges.all { it.endsWith("|1") || it.endsWith("|2") })
        assertEquals("yeni görevler bekliyor", 3, Challenges.list(g).count { it.state == 0 })
        assertTrue(g.managerXp > xp0)
        println("GOREV ${log.matches} maç: ${g.challengesDone} görev, ${g.challengesPerfect} kusursuz, başarımlar ${g.achievements}")
    }
}
