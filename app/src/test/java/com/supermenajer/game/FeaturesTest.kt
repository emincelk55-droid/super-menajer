package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Scouting
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** v0.6 CM özellikleri: gözlemci, kiralık, mutluluk, wonderkid, ödüller. */
class FeaturesTest {
    @Test
    fun cmFeatures() {
        GameRepo.init(Files.createTempDirectory("sm6").toFile())
        val world = Generator.newWorld(Random(4)).also { Season.computeExpectations(it) }
        val wonderkids = world.players.count { it.age <= 19 && it.potential >= 83 }
        println("WONDERKID sayısı: $wonderkids")
        assertTrue(wonderkids in 15..120)
        GameRepo.newCareer(world, "Test", world.divisionTeams(0).first().id)
        val g = GameRepo.game!!
        val us = g.userTeam

        // Gözlemci: bilinmeyen oyuncu aralıkla görünür, rapordan sonra tam bilinir
        val target = g.players.first { it.teamId >= 0 && it.teamId != us.id && g.countryOf(g.team(it.teamId)) == "İspanya" }
        assertTrue(!Scouting.known(g, target))
        val r = Scouting.range(g, target, target.overall)
        assertTrue(target.overall in r && r.last - r.first == 2 * com.supermenajer.game.game.StaffLogic.scoutUncertainty(g, false))
        assertEquals("?", Scouting.potText(g, target))
        val budget = us.budget
        GameRepo.scout(target)
        assertEquals(budget - Scouting.COST, us.budget)

        // Kiralık: genç ve 11'de olmayan oyuncu
        val youngster = g.squad(us.id).filter { it.age <= 21 && it.id !in us.lineup }.minByOrNull { it.overall }!!
        assertNull(GameRepo.loanOut(youngster))
        assertEquals(us.id, youngster.loanFrom)
        assertTrue(youngster.teamId != us.id)
        assertTrue(g.squad(us.id).none { it.id == youngster.id })

        // Birebir konuşma haftada bir
        val star = g.squad(us.id).maxBy { it.overall }
        assertTrue(GameRepo.canTalk(star))
        GameRepo.talkToPlayer(star, 0)
        assertTrue(!GameRepo.canTalk(star))

        // Yıldızı kadro dışı bırak: zamanla transfer talep etmeli
        GameRepo.removeFromLineup(star)
        star.morale = 30.0
        if (g.todayUserFixture() != null) { GameRepo.playMatch(false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        assertTrue("Gözlemci raporu gelmeli", Scouting.known(g, target))
        var guard = 0
        while (!star.wantsAway && guard++ < 60 && !g.seasonOver) {
            if (star.id in us.lineup) GameRepo.removeFromLineup(star)
            if (g.todayUserFixture() != null) { GameRepo.playMatch(false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        println("TALEP: ${guard} adımda, moral ${star.morale.toInt()}")
        assertTrue("Yedekte kalan yıldız transfer talep etmeli", star.wantsAway)
        assertTrue(g.shortlist.isNotEmpty())

        // Sezonu bitir: ödüller ve kiralık dönüşü
        while (!g.seasonOver && !g.fired) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        val rec = g.history.last()
        println("ÖDÜLLER: ${rec.awards}")
        assertTrue(rec.awards.contains("Yılın oyuncusu"))
        GameRepo.startNewSeason()
        assertEquals(-1, youngster.loanFrom)
        assertEquals(us.id, youngster.teamId)
        assertNotNull(g.team(g.userTeamId))
        println("KİRALIK DÖNÜŞ: ${youngster.name} ${youngster.startOvr}")
    }
}
