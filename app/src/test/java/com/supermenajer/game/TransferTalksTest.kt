package com.supermenajer.game

import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.TransferTalks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

class TransferTalksTest {
    @Test
    fun negotiation() {
        GameRepo.init(Files.createTempDirectory("sm13").toFile())
        val world = Generator.newWorld(Random(13)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[0].id)
        val g = GameRepo.game!!
        val us = g.userTeam
        us.budget = 200_000_000
        us.wageBudget = 5_000_000
        val str = com.supermenajer.game.game.Lineups.strength(g, us)
        val target = g.players.first { it.teamId >= 0 && it.teamId != us.id && it.overall in (str.toInt() - 3)..(str.toInt() + 3) && g.squad(it.teamId).size > 20 && GameRepo.transferBlock(it) == null }
        val seller = g.team(target.teamId)
        assertEquals(null, GameRepo.startTalks(target))
        val n = GameRepo.negotiation!!
        assertEquals(TransferTalks.CLUB, n.stage)
        // Düşük teklif: sabır azalır
        n.fee = 25_000
        GameRepo.talkClub()
        assertTrue(n.clubPatience < 3)
        // Yüksek, taksitli ve sonraki satıştan paylı teklif kabul edilir
        n.fee = GameRepo.askingPrice(target) * 2
        n.installments = true
        n.sellOn = 10
        GameRepo.talkClub()
        assertEquals(TransferTalks.AGENT, n.stage)
        println("DİYALOG: " + n.log.joinToString(" | ") { "${it.who}: ${it.text}" })
        // Menajer: düşük maaş reddedilir, istenen maaş + iyi rol kabul edilir
        n.wage = 500
        GameRepo.talkAgent()
        assertEquals(TransferTalks.AGENT, n.stage)
        n.agentPatience = 3
        n.wage = target.demandWage * 11 / 10
        n.role = 0
        n.bonus = target.demandWage * 10
        val budgetBefore = us.budget
        GameRepo.talkAgent()
        assertEquals(TransferTalks.DONE, n.stage)
        assertEquals(us.id, target.teamId)
        assertEquals(n.wage, target.salary)
        assertEquals(10, target.sellOnPct)
        assertEquals(seller.id, target.sellOnClub)
        assertTrue(g.pendingPayments > 0)
        assertEquals(budgetBefore - n.agreedFee / 2 - n.bonus, us.budget)
        GameRepo.closeTalks()

        // Kiralık: ilk 11'de olmayan genç oyuncu
        val young = g.players.first { it.teamId >= 0 && it.teamId != us.id && it.age <= 22 && g.squad(it.teamId).size > 21 && it.id !in g.team(it.teamId).lineup && GameRepo.transferBlock(it) == null }
        GameRepo.startTalks(young)
        val l = GameRepo.negotiation!!
        l.loan = true
        GameRepo.talkClub()
        assertEquals(TransferTalks.AGENT, l.stage)
        l.wage = young.demandWage * 2
        GameRepo.talkAgent()
        assertEquals(TransferTalks.DONE, l.stage)
        assertEquals(us.id, young.teamId)
        assertTrue(young.loanFrom >= 0)
        GameRepo.closeTalks()

        // Bütçe kaydırma
        val wb = us.wageBudget
        val b = us.budget
        GameRepo.shiftBudget(10_000)
        assertEquals(wb + 10_000, us.wageBudget)
        assertEquals(b - 10_000 * Economy.SHIFT_RATIO, us.budget)
        GameRepo.shiftBudget(-10_000)
        assertEquals(wb, us.wageBudget)
    }
}
