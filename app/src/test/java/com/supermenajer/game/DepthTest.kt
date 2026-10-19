package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Career
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Matchday
import com.supermenajer.game.game.Roles
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.Traits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** v1.3: kişilikler, roller, numaralar, kamp, gizli antrenman, hazırlık maçı, yönetim istekleri, başarımlar. */
class DepthTest {
    @Test
    fun depthFeatures() {
        GameRepo.init(Files.createTempDirectory("sm14").toFile())
        val world = Generator.newWorld(Random(14)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        g.confidence = 100.0
        val us = g.userTeam
        us.budget = 100_000_000

        // Kişilikler: her tür dünyada bulunur
        val counts = g.players.groupingBy { Traits.of(it) }.eachCount()
        println("KİŞİLİK: " + counts.toSortedMap().map { "${Traits.names[it.key]}=${it.value}" })
        assertEquals(Traits.names.size, counts.size)

        // Forma numaraları takım içinde benzersiz
        assertTrue(g.teams.all { t -> g.squad(t.id).map { it.number }.let { n -> n.all { it > 0 } && n.toSet().size == n.size } })

        // Roller motoru etkiler
        val f = g.nextUserFixture()!!
        val base = MatchSim(g, f, us.id, autoUserSubs = true, rnd = Random(1)).let { it.strength(it.userSide!!) }
        val slots = com.supermenajer.game.model.Formations.slots(us.formation)
        slots.forEachIndexed { i, s -> if (s.pos == com.supermenajer.game.model.Pos.MF) GameRepo.setRole(i, 1) } // ön libero
        val defensive = MatchSim(g, f, us.id, autoUserSubs = true, rnd = Random(1)).let { it.strength(it.userSide!!) }
        println("ROL: atk ${"%.1f".format(base.atk)}→${"%.1f".format(defensive.atk)} def ${"%.1f".format(base.def)}→${"%.1f".format(defensive.def)}")
        assertTrue(defensive.def > base.def && defensive.atk < base.atk)
        GameRepo.setDuty(1, us.lineup[9])
        assertEquals(us.lineup[9], Roles.penaltyTaker(g, us, us.lineup.mapNotNull { g.player(it) })!!.id)

        // Hazırlık maçı: sezon öncesi, istatistiğe yazılmaz, gün ilerler
        assertTrue(Matchday.canFriendly(g))
        val apps = g.squad(us.id).sumOf { it.apps }
        val d0 = g.date
        GameRepo.friendly()
        assertEquals(1, g.friendlies)
        assertEquals(apps, g.squad(us.id).sumOf { it.apps })
        assertTrue(g.date > d0)

        // Kamp ve gizli antrenman
        val b = us.budget
        GameRepo.camp()
        assertEquals(1, g.campUses)
        assertTrue(us.budget < b && Matchday.campActive(g))
        GameRepo.secretTraining()
        assertEquals(g.nextUserFixture()!!.week, g.secretWeek)

        // Kenar çizgisi: bekleme süresi
        GameRepo.skipToNextMatch()
        val sim = MatchSim(g, g.todayUserFixture()!!, us.id, autoUserSubs = true, rnd = Random(2))
        assertTrue(sim.userSide!!.onPitch.all { it.boost >= 0.025 })
        repeat(10) { sim.step() }
        sim.shout(0)
        assertTrue(!sim.canShout)

        // Yönetim isteği sezonda bir kez
        println("YÖNETİM: " + GameRepo.boardRequest(0))
        assertTrue(0 in g.boardAsked)
        assertTrue(GameRepo.boardRequest(0).contains("zaten"))

        // Sezon oyna: başarımlar ve rekorlar
        while (!g.seasonOver && !g.fired) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
        println("BAŞARIM: ${g.achievements} · itibar ${g.managerRep} · kupalar ${g.trophies} · rekor ${g.recBigWin}")
        assertTrue("camp" in g.achievements)
        assertTrue(g.recBigWin.isNotEmpty() || g.recLoss.isNotEmpty())
        assertTrue(Career.all.size >= 15)
    }
}
