package com.supermenajer.game

import com.supermenajer.game.game.Alias
import com.supermenajer.game.game.Fictional
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.RealWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Kurgusal isimler: gerçek kulüp ve oyuncu adı kalmaz, kodlar benzersiz, geri alma her şeyi döndürür. */
class FictionalTest {
    @Test
    fun noRealNamesAndUndo() {
        val g = Generator.newWorld(Random(8))
        val before = g.teams.associate { it.id to Triple(it.name, it.short, it.stadiumName) }
        val playersBefore = g.players.associate { it.id to it.name }
        val t0 = System.currentTimeMillis()
        val undo = Fictional.apply(g)
        println("KURGU süre ${System.currentTimeMillis() - t0} ms")

        val realClubs = HashSet<String>()
        val realPeople = HashSet<String>()
        for (id in listOf(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)) {
            val d = RealWorld.load(id) ?: continue
            for (t in d.teams) {
                realClubs += t.name.lowercase(); realClubs += Alias.club(t.name).lowercase()
                for (p in t.players) { realPeople += p.name.lowercase(); realPeople += Alias.person(p.name).lowercase() }
            }
        }
        val clash = g.teams.filter { it.name.lowercase() in realClubs }
        assertTrue("gerçek kulüp adı kaldı: ${clash.map { it.name }}", clash.isEmpty())
        val peopleClash = g.players.count { it.name.lowercase() in realPeople }
        println("KURGU örnek: ${g.teams.filter { it.real }.take(8).joinToString { "${it.name} (${it.short}, ${it.stadiumName})" }}")
        println("KURGU oyuncu: ${g.players.take(6).joinToString { it.name }} · gerçekle aynı: $peopleClash")
        assertEquals(0, peopleClash)
        // Yıldız soyadları (Xhaka, Mudryk...) kurgusal dünyada geçmemeli
        val famous = Fictional.famousSurnames()
        val famousLeft = g.players.filter { it.name.substringAfterLast(' ').lowercase() in famous }
        println("KURGU yıldız soyadı kalan: ${famousLeft.size} / ${g.players.size} ${famousLeft.take(8).map { it.name }}")
        assertTrue(famousLeft.size <= g.players.size / 200)
        assertEquals(g.teams.size, g.teams.map { it.short }.toSet().size)
        assertTrue(g.fictionalNames)

        Fictional.undo(g, undo)
        assertEquals(before, g.teams.associate { it.id to Triple(it.name, it.short, it.stadiumName) })
        assertEquals(playersBefore, g.players.associate { it.id to it.name })
    }
}
