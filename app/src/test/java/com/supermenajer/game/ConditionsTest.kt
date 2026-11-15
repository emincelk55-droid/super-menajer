package com.supermenajer.game

import com.supermenajer.game.engine.Conditions
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.GameCalendar
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/** Maç günü koşulları: sabit, mevsime uygun; sert hakem daha çok kart gösterir. */
class ConditionsTest {
    @Test
    fun conditionsAndReferees() {
        val rnd = Random(4)
        val g = Generator.newWorld(rnd)
        g.userTeamId = g.divisionTeams(0)[3].id
        Season.start(g, rnd)
        val fx = g.fixtures.filter { it.comp == 10 }
        val conds = fx.map { it to Conditions.of(g, it) }
        // Aynı maç için hep aynı
        assertEquals(conds.first().second.summary(), Conditions.of(g, conds.first().first).summary())
        // Kar yalnız kışın
        for ((f, c) in conds) if (c.weather == Conditions.SNOW) {
            val m = LocalDate.ofEpochDay(GameCalendar.dateOf(g, f.week)).monthValue
            assertTrue("karlı maç kışta olmalı: ay $m", m in listOf(12, 1, 2))
        }
        val kinds = conds.groupingBy { it.second.weather }.eachCount()
        println("HAVA ${kinds.mapKeys { Conditions.icons[it.key] }}")
        assertTrue(kinds.size >= 4)
        // Sert hakem daha çok kart
        val strict = conds.filter { it.second.refStrict >= 1.15 }.take(60)
        val lenient = conds.filter { it.second.refStrict <= 0.88 }.take(60)
        fun cards(list: List<Pair<com.supermenajer.game.model.Fixture, com.supermenajer.game.engine.MatchConditions>>): Double =
            list.map { (f, _) -> MatchSim(g, f, -1, true, Random(f.week * 13 + f.home)).apply { runToEnd() }.let { it.home.yellows + it.away.yellows } }.average()
        val cs = cards(strict)
        val cl = cards(lenient)
        println("KART sert=${"%.2f".format(cs)} serbest=${"%.2f".format(cl)} (${strict.size}/${lenient.size} maç)")
        assertTrue("sert hakem daha çok kart göstermeli", cs > cl * 1.15)
    }
}
