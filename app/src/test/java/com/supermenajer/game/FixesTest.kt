package com.supermenajer.game

import com.supermenajer.game.game.Events
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** v2.1 hata düzeltmeleri ve zorluk ayarı. */
class FixesTest {
    @Test
    fun fixes() {
        GameRepo.init(Files.createTempDirectory("sm21").toFile())
        val easy = Generator.newWorld(Random(21)).also { Season.computeExpectations(it) }
        val teamId = easy.divisionTeams(0)[3].id
        val base = easy.team(teamId).budget
        GameRepo.newCareer(easy, "Test", teamId, difficulty = 0, noFiring = true)
        val g = GameRepo.game!!
        assertTrue(g.userTeam.budget > base)
        assertTrue(g.noFiring)

        // Kiralık gelen oyuncu tekrar kiralık gönderilemez
        val other = g.teams.first { it.id != teamId }
        val loanee = g.squad(other.id).first { it.age <= 23 }
        g.movePlayer(loanee, teamId)
        loanee.loanFrom = other.id
        assertNotNull(GameRepo.loanOut(loanee))
        assertEquals(other.id, loanee.loanFrom)

        // Karar beklerken ayrılan oyuncu için olay düşer, para harcanmaz
        val gone = g.squad(teamId).first { it.id != loanee.id }
        g.pendingEvent = GameEvent(Events.HOMESICK, gone.id, "t", "t", listOf("a", "b", "c"), 1_000_000)
        g.movePlayer(gone, other.id)
        val budget = g.userTeam.budget
        Events.resolve(g, 0)
        assertEquals(budget, g.userTeam.budget)
        assertNull(g.pendingEvent)

        // Emekliliğini açıklayan oyuncu sezon sonunda gerçekten bırakır; sezon istatistiği arşivlenir
        val vet = g.squad(teamId).first { it.id != loanee.id }
        g.retiring += vet.id
        val scorer = g.squad(teamId).first { it.id != vet.id && it.id != loanee.id && it.age < 30 }
        scorer.apps = 10; scorer.goals = 4; scorer.ratingSum = 70.0
        Season.newSeason(g, Random(1))
        assertNull(g.player(vet.id))
        assertTrue(g.retiring.isEmpty())
        assertTrue(scorer.career.single().contains("|10|4|"))

        // Ana menüye dönünce kariyer kapanır, kayıt başka yuvaya yazılmaz
        GameRepo.closeCareer()
        assertNull(GameRepo.game)
        GameRepo.save(force = true)
    }
}
