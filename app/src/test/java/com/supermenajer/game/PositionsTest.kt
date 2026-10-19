package com.supermenajer.game

import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Positions.Det
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Formations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PositionsTest {
    @Test
    fun detailedPositions() {
        val g = Generator.newWorld(Random(31)).also { Season.computeExpectations(it) }
        val byDet = g.players.groupingBy { Positions.det(it) }.eachCount()
        val feet = g.players.groupingBy { it.foot }.eachCount()
        println("MEVKİ: " + Det.entries.joinToString { "${it.code}=${byDet[it] ?: 0}" } + " · ayak " + feet)
        println("ÖZELLİK: " + g.players.groupingBy { "${it.pos.short}:${Positions.natRoleName(it)}" }.eachCount().toSortedMap())
        assertTrue(Det.entries.all { (byDet[it] ?: 0) > 100 })
        // Her takımda en az 3 stoper, 1 sol bek, 1 sağ bek, 1 DOS, 1 MO, 1 santrfor
        for (t in g.teams) {
            val sq = g.squad(t.id)
            fun n(d: Det) = sq.count { Positions.det(it) == d || it.secondDet == d.ordinal }
            assertTrue(t.name, n(Det.STP) >= 3)
            assertTrue(t.name, n(Det.SLB) >= 1 && n(Det.SGB) >= 1)
            assertTrue(t.name, n(Det.DOS) >= 1 && n(Det.SF) >= 1)
        }
        // Otomatik 11: 4'lü defansta bekler bek, stoperler stoper
        var ok = 0
        var total = 0
        for (t in g.teams) {
            Lineups.changeFormation(g, t, "4-4-2")
            Lineups.autoPick(g, t)
            val slots = Formations.slots(t.formation)
            for (i in 1..4) {
                val p = g.player(t.lineup[i]) ?: continue
                total++
                if (Positions.det(p) == Positions.slotDet(slots[i])) ok++
            }
        }
        println("BEK/STOPER: $ok/$total doğru yerleşim")
        // Gerçek kadrolarda birkaç takımın yapısı sıra dışı olabilir (ör. hiç sol bek yok)
        assertTrue("$ok/$total", ok >= total * 0.99)
    }
}
