package com.supermenajer.game

import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Formations
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class LineupTest {
    @Test
    fun bestElevenUsesNaturalPositions() {
        val g = Generator.newWorld(Random(21)).also { Season.computeExpectations(it) }
        g.reindex()
        var wrong = 0
        var checked = 0
        for (t in g.teams) {
            for (f in Formations.names) {
                Lineups.changeFormation(g, t, f)
                Lineups.autoPick(g, t)
                val slots = Formations.slots(t.formation)
                val squad = g.squad(t.id).filter { it.available }
                t.lineup.forEachIndexed { i, id ->
                    val p = g.player(id) ?: return@forEachIndexed
                    val pos = slots[i].pos
                    // Yeterli doğal oyuncu varken başka mevkiden oyuncu konmamalı
                    val naturals = squad.count { it.pos == pos }
                    val needed = slots.count { it.pos == pos }
                    if (naturals >= needed) {
                        checked++
                        // Oyuncunun öğrenilmiş/gerçek 2. mevkisi de doğal sayılır
                        if (p.pos != pos && !com.supermenajer.game.game.Positions.canPlay(p, com.supermenajer.game.game.Positions.slotDet(slots[i]))) wrong++
                    }
                }
            }
        }
        println("DİZİLİŞ: $checked mevki kontrol edildi, $wrong yanlış yerleşim")
        assertEquals(0, wrong)
    }
}
