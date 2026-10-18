package com.supermenajer.game

import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.Sponsors
import com.supermenajer.game.model.Comp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class SponsorTest {
    @Test
    fun sponsorDeals() {
        GameRepo.init(Files.createTempDirectory("sm9").toFile())
        val world = Generator.newWorld(Random(5)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[1].id)
        val g = GameRepo.game!!
        g.confidence = 100.0
        val us = g.userTeam
        assertEquals(9, g.sponsorOffers.size)
        for (slot in 0..2) {
            val o = Sponsors.offers(g, slot)
            assertEquals(listOf(0, 1, 2), o.map { it.style })
            // Garantili > dengeli > performans sabit ödeme; primler ters yönde
            assertTrue(o[0].base > o[1].base && o[1].base > o[2].base)
            assertTrue(o[2].winBonus > o[1].winBonus && o[0].winBonus == 0L)
            println("TEKLİF ${Sponsors.slotNames[slot]}: " + o.joinToString { "${it.name} ${it.base}/${it.winBonus}/${it.titleBonus}" })
        }
        // Küçük kulübün teklifi büyük kulüpten düşük olmalı
        val small = g.divisionTeams(0).minBy { it.reputation }
        assertTrue(Sponsors.slotValue(g, small, 0) < Sponsors.slotValue(g, us, 0))

        val shirt = Sponsors.offers(g, Sponsors.SHIRT)[2]
        val stadium = Sponsors.offers(g, Sponsors.STADIUM)[0]
        assertNull(Sponsors.sign(g, shirt))
        assertNotNull(Sponsors.sign(g, Sponsors.offers(g, Sponsors.SHIRT).firstOrNull() ?: shirt))
        assertNull(Sponsors.sign(g, stadium))
        assertEquals("${stadium.name} Arena", us.stadiumName)
        assertEquals(3, g.sponsorOffers.size)

        while (!g.seasonOver && !g.fired) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        val wins = g.fixtures.count { Comp.isLeague(it.comp) && ((it.home == us.id && it.hg > it.ag) || (it.away == us.id && it.ag > it.hg)) }
        val got = us.lastReport[Economy.SPONSOR] ?: 0L
        val weeks = g.calendar.count { it.type == 0 }
        val expected = (shirt.base / weeks + stadium.base / weeks) * weeks + wins * shirt.winBonus
        println("SPONSOR GELİRİ: $got (beklenen en az $expected, $wins galibiyet)")
        assertTrue(got >= expected)
        assertTrue(g.teams.filter { it.id != us.id && it.division == 0 }.all { (it.lastReport[Economy.SPONSOR] ?: 0L) > 0 })

        // Yeni sezon: 1 yıllık performans sözleşmesi biter, garantili (3 yıl) devam eder
        GameRepo.startNewSeason()
        assertNull(Sponsors.current(g, Sponsors.SHIRT))
        assertEquals(2, Sponsors.current(g, Sponsors.STADIUM)!!.yearsLeft)
        assertEquals(6, g.sponsorOffers.size)

        // Takım değiştirince sözleşmeler eski kulüpte kalır, stadyum adı geri döner
        val other = g.divisionTeams(10)[0].id
        GameRepo.takeJob(other)
        assertEquals(Sponsors.defaultStadiumName(us), us.stadiumName)
        assertTrue(g.sponsors.isEmpty())
        assertEquals(9, g.sponsorOffers.size)
    }
}
