package com.supermenajer.game

import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Names
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NamesTest {
    @Test
    fun realisticNames() {
        val w = Generator.newWorld(Random(9))
        val names = w.players.map { it.name }
        val dup = names.size - names.toSet().size
        println("İSİMLER: ${w.players.size} oyuncu, $dup tekrar. Örnek: " + w.divisionTeams(10)[0].let { t -> w.players.filter { it.teamId == t.id }.take(8).joinToString { "${it.name} (${it.nation})" } })
        println("TR örnek: " + w.players.filter { it.teamId == w.divisionTeams(0)[0].id }.take(8).joinToString { "${it.name} (${it.nation})" })
        assertTrue(names.none { Names.isBlocked(it) })
        assertTrue(dup < names.size / 20)
        // Takım içinde aynı isim yok
        assertTrue(w.teams.all { t -> w.players.filter { it.teamId == t.id }.let { s -> s.map { it.name }.toSet().size >= s.size - 1 } })
    }
}
