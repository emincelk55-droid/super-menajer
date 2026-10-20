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

        // Taktik Oluştur: uygunluk 1-5, uygulanınca diziliş ve talimatlar değişir
        val r = com.supermenajer.game.game.TacticStyles.ratings(g)
        assertTrue(r.values.all { it in 1..5 } && r.values.contains(5))
        val cat = com.supermenajer.game.game.TacticStyles.all.first { it.name == "Catenaccio" }
        com.supermenajer.game.game.TacticStyles.apply(g, cat, "5-3-2")
        assertEquals("5-3-2", g.userTeam.formation)
        assertEquals(0, g.userTeam.mentality)
        assertTrue(GameRepo.teamReport(g).contains("en zayıf"))

        // A2: gönderilen oyuncu maç kadrosuna giremez, haftalık A2 maçı oynanır
        val young = g.squad(teamId).filter { it.id !in g.userTeam.lineup }.minBy { it.age }
        assertNull(GameRepo.setReserve(young, true))
        assertTrue(young.reserve && !young.available)
        val second = g.squad(teamId).filter { it.id !in g.userTeam.lineup && !it.reserve }.minBy { it.age }
        GameRepo.setReserve(second, true)
        com.supermenajer.game.game.Reserves.weekly(g, 1, Random(2))
        assertTrue(g.a2Results.isNotEmpty())
        GameRepo.setReserve(young, false)
        assertTrue(young.available)

        // Milli takım: itibar yetmezse görev verilmez; pencerede maç, yaz turnuvası
        g.managerRep = 10
        assertNotNull(GameRepo.takeNationalJob("ES"))
        g.managerRep = 95
        assertNull(GameRepo.takeNationalJob("ES"))
        com.supermenajer.game.game.National.weekly(g, 6, Random(4))
        com.supermenajer.game.game.National.weekly(g, 6, Random(4))
        assertTrue(g.natPending.isNotEmpty() && g.natResults.isEmpty())
        // Canlı milli maç: maç motoru iki ulusun kadrosuyla oynar, kulüp istatistiklerine yazılmaz
        val appsBefore = g.players.sumOf { it.apps }
        val sim = com.supermenajer.game.game.National.buildMatch(g, live = false)!!
        assertTrue(sim.home.onPitch.size == 11 && sim.away.onPitch.size == 11)
        assertTrue(sim.userSide!!.onPitch.all { it.p.nation == "ES" })
        sim.runToEnd(); sim.applyNational(); com.supermenajer.game.game.National.finish(g, sim)
        assertEquals(appsBefore, g.players.sumOf { it.apps })
        assertTrue(g.natPending.isEmpty())
        assertEquals(1, g.natResults.size)
        com.supermenajer.game.game.National.summerTournament(g, Random(5))
        assertTrue(g.natResults.size >= 2)

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
