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
        // Canlı turnuva: 4 grup + eleme; kullanıcının maçları bekleyen maç olarak oynanır
        val T = com.supermenajer.game.game.Tournament
        assertTrue(T.needed(g))
        T.start(g, Random(7))
        assertEquals(16, g.tourTeams.toSet().size)
        assertTrue(g.natPending.isNotEmpty())
        var guard = 0
        while (g.natPending.isNotEmpty() && guard++ < 10) {
            if (T.active(g) && g.tourStage >= 3) {
                val ko = com.supermenajer.game.game.National.buildMatch(g, live = false)!!
                assertTrue(ko.isCup)
            }
            com.supermenajer.game.game.National.simulatePending(g)
        }
        assertEquals(6, g.tourStage)
        assertEquals(1, g.tourAlive.size)
        assertTrue(!T.needed(g))
        // Grup aşaması: 24 maç, toplam puan 2-3 arası/maç
        val pts = g.tourPts.values.sum()
        assertTrue(pts in 48..72)
        com.supermenajer.game.game.National.summerTournament(g, Random(5)) // bu sezon oynandı: tekrar oynanmaz
        assertTrue(g.natResults.size >= 4)

        // Sağlık: hızlı dönüş süreyi yarıya indirir, hassasiyet ekler
        val hurt = g.squad(teamId).first { !it.reserve }
        hurt.injuredWeeks = 6; hurt.injuryType = "Hamstring sakatlığı"
        com.supermenajer.game.game.Medical.afterMatch(g, listOf(hurt))
        assertTrue(hurt.id in g.medical)
        GameRepo.medicalDecision(hurt, 2)
        assertEquals(3, hurt.injuredWeeks)
        assertTrue(hurt.fragileWeeks > 0 && hurt.id !in g.medical)
        hurt.injuredWeeks = 0

        // Serbest kalma bedeli: öde ve oyuncu gelsin
        val star = g.players.first { it.teamId >= 0 && it.teamId != teamId && it.overall in 60..70 && it.preContract < 0 }
        star.releaseClause = 1_000_000
        g.userTeam.budget = 500_000_000; g.userTeam.wageBudget = 50_000_000
        val res = GameRepo.payReleaseClause(star)
        assertEquals(teamId, star.teamId)
        assertTrue(res.contains("bizim"))

        // Ön sözleşme: sözleşme bitince bedelsiz gelir
        val pre = g.players.first { it.teamId >= 0 && it.teamId != teamId && it.contractYears > 1 && it.overall in 55..65 }
        pre.contractYears = 1
        pre.preContract = teamId
        com.supermenajer.game.game.Economy.processContracts(g, Random(3))
        assertEquals(teamId, pre.teamId)
        assertEquals(-1, pre.preContract)

        // Görevler ve sosyal medya
        com.supermenajer.game.game.Missions.ensure(g)
        assertEquals(5, g.missions.size)
        com.supermenajer.game.game.Missions.progress(g, com.supermenajer.game.game.Missions.list(g).first().id, 99)
        assertTrue(com.supermenajer.game.game.Missions.list(g).first().done)
        com.supermenajer.game.game.Social.afterMatch(g, 3, 0, "Rakip", "Golcü")
        assertTrue(g.feed.isNotEmpty())

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

        // Rakip menajer: laf sokma → basın sorusu; cevap rakibe hırs, karne tutulur
        val opp = g.divisionTeams(g.userTeam.division).first { it.id != g.userTeamId }
        val mgr = g.managers[opp.id]!!
        com.supermenajer.game.game.Rivals.record(g, opp, 2, 1)
        com.supermenajer.game.game.Rivals.record(g, opp, 0, 0)
        assertEquals(Triple(1, 1, 0), com.supermenajer.game.game.Rivals.h2h(g, mgr))
        g.press = com.supermenajer.game.model.PressQuestion("x", com.supermenajer.game.game.Rivals.answers, com.supermenajer.game.game.Rivals.PRESS_KIND)
        GameRepo.answerPress(g.press!!, 1)
        assertTrue(g.rivalBoost > 0 && g.press == null)

        // Vizyon: 5 hedef, sezon sonunda değerlendirilir
        com.supermenajer.game.game.Vision.ensure(g)
        assertEquals(5, g.vision.size)
        g.userTeam.budget = -1
        com.supermenajer.game.game.Vision.evaluate(g)
        assertTrue(com.supermenajer.game.game.Vision.list(g).first { it.id == "finance" }.status == 2)
        g.userTeam.budget = 10_000_000

        // Veri editörü
        val ed = g.squad(g.userTeamId).first { it.pos != com.supermenajer.game.model.Pos.GK }
        GameRepo.editPlayer(ed, "Yeni İsim", 22, 95, listOf(90, 90, 90, 90, 90, 90, 10))
        assertEquals("Yeni İsim", ed.name)
        assertTrue(ed.overall >= 85 && ed.potential >= ed.overall)

        // Arka plan işlemi: testte (ana iş parçacığı yok) senkron çalışır, meşgul durumu temizlenir
        GameRepo.skipToNextMatchAsync()
        assertNull(GameRepo.busy)

        // Ana menüye dönünce kariyer kapanır, kayıt başka yuvaya yazılmaz
        GameRepo.closeCareer()
        assertNull(GameRepo.game)
        GameRepo.save(force = true)
    }
}
