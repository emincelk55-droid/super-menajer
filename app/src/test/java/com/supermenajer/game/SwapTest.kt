package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.TransferTalks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Takaslı transfer: nakit yetmeyince takas oyuncusu farkı kapatır ve karşı kulübe geçer. */
class SwapTest {
    @Test
    fun swapClosesTheGap() {
        GameRepo.init(Files.createTempDirectory("swap").toFile())
        val w = Generator.newWorld(Random(4)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Test", w.divisionTeams(0).sortedByDescending { it.reputation }[0].id)
        val g = GameRepo.game!!
        g.userTeam.budget = 500_000_000
        g.userTeam.wageBudget = 50_000_000
        val seller = w.divisionTeams(0).sortedBy { it.reputation }[3]
        val target = g.squad(seller.id).filter { it.id !in seller.lineup }.maxBy { it.value }
        val asking = GameRepo.askingPrice(target)
        // Satıcıya uygun güçte bir takas oyuncusu
        val swap = TransferTalks.swapCandidates(g, target).first { TransferTalks.swapValue(g, seller, it) >= asking * 0.5 }
        val sv = TransferTalks.swapValue(g, seller, swap)
        println("TAKAS hedef ${target.name} ${target.value} isteniyor $asking · takas ${swap.name} değer ${swap.value} → satıcı gözünde $sv")

        // Yalnız nakit, istenenin %55'i: reddedilir
        val n1 = TransferTalks.start(g, target)
        n1.fee = (asking * 0.55).toLong()
        TransferTalks.clubOffer(g, n1)
        assertEquals(TransferTalks.CLUB, n1.stage)

        // Aynı nakit + takas: farkı kapatır, kabul
        val n2 = TransferTalks.start(g, target)
        n2.fee = ((asking - sv) * 1.02).toLong().coerceAtLeast(25_000)
        n2.swapId = swap.id
        TransferTalks.clubOffer(g, n2)
        assertEquals(TransferTalks.AGENT, n2.stage)
        n2.wage = target.demandWage * 2
        TransferTalks.agentOffer(g, n2)
        assertEquals(TransferTalks.DONE, n2.stage)
        assertEquals(g.userTeamId, target.teamId)
        assertEquals(seller.id, swap.teamId)
        assertTrue(swap.moves.last().endsWith("|T"))
        assertTrue(g.news.any { it.title.contains("Takas") })
    }
}
