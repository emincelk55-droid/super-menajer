package com.supermenajer.game

import com.supermenajer.game.game.Alias
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.RealWorld
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Leagues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Gerçek dünya verisi: isimler benzer ama aynı değil, güçler veriden, ligler 18 takım. */
class RealWorldTest {
    @Test
    fun aliases() {
        val people = listOf("Arda Güler", "Kylian Mbappé", "Erling Haaland", "Lamine Yamal", "Victor Osimhen", "Harry Kane", "Jude Bellingham", "Pedri", "Okan Buruk", "Vinícius Júnior")
        val clubs = listOf("Galatasaray", "Fenerbahçe", "Beşiktaş", "Real Madrid", "FC Barcelona", "Liverpool", "Manchester City", "Bayern Munich", "Santiago Bernabéu", "Rams Park")
        for (n in people) { val a = Alias.person(n); println("ALIAS $n -> $a"); assertNotEquals(n, a) }
        for (n in clubs) { val a = Alias.club(n); println("ALIAS $n -> $a"); assertNotEquals(n, a) }
        // Tüm gerçek kulüp, hoca ve oyuncu isimleri değişmiş olmalı
        val same = mutableListOf<String>()
        for (id in listOf(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)) {
            val d = RealWorld.load(id) ?: continue
            for (t in d.teams) {
                if (Alias.club(t.name) == t.name) same += t.name
                if (Alias.person(t.coach) == t.coach) same += t.coach
                for (p in t.players) if (Alias.person(p.name) == p.name) same += p.name
            }
        }
        println("AYNI KALAN: ${same.size} ${same.take(40)}")
        println("ALIAS Tottenham Hotspur -> ${Alias.club("Tottenham Hotspur")}")
        assertEquals(0, same.size)
        assertEquals("Arda Gülar", Alias.person("Arda Güler"))
        assertEquals("Kylian Mbapé", Alias.person("Kylian Mbappé"))
    }

    @Test
    fun parsesAndBuilds() {
        val d = RealWorld.parse(
            99,
            listOf(
                "LEAGUE|99|2026-27|Test",
                "TEAM|Galatasaray|GAL|Istanbul|Rams Park|53387|Okan Buruk|TR|#A90432|#FDB912|86",
                "P|10|Leroy Sané|1996|DE|SGK|SLK|L|84|84|88,80,78,86,35,66",
                "P|1|Uğurcan Çakır|1996|TR|KL|-|R|81|82|48,20,62,40,25,72",
            ),
        )
        assertEquals(1, d.teams.size)
        val w = d.teams[0].players[0]
        val p = RealWorld.makePlayer(1, w, 0, Random(1))
        assertEquals(84, p.overall)
        assertEquals(com.supermenajer.game.game.Positions.Det.SGK.ordinal, p.detail)
        assertEquals(com.supermenajer.game.game.Positions.Det.SLK.ordinal, p.secondDet)
        assertEquals(10, p.number)
        assertEquals(1, p.foot)
        val k = RealWorld.makePlayer(2, d.teams[0].players[1], 0, Random(1))
        assertEquals(81, k.overall)

        // Gerçek veri yüklüyse dünya ondan kurulur
        GameRepo.init(Files.createTempDirectory("smrw").toFile())
        val world = Generator.newWorld(Random(5)).also { Season.computeExpectations(it) }
        for (lg in Leagues.all) {
            val n = world.divisionTeams(lg.id).size
            println("LEAGUE ${lg.name}: $n takım, gerçek=${RealWorld.has(lg.id)}")
            assertEquals(18, n)
        }
        if (RealWorld.has(0)) {
            val tr = world.divisionTeams(0)
            assertTrue(tr.all { it.real })
            println("TR: " + tr.joinToString { it.name })
            val best = world.players.filter { it.teamId in tr.map { t -> t.id } }.sortedByDescending { it.overall }.take(8)
            println("TR en iyiler: " + best.joinToString { "${it.name} ${it.overall}" })
            assertTrue(world.managers.size > 100)
            // Galatasaray 4-4-2: kanatlara kanatçılar, uyum tam
            val gs = tr.first()
            com.supermenajer.game.game.Lineups.changeFormation(world, gs, "4-4-2")
            com.supermenajer.game.game.Lineups.autoPick(world, gs)
            val sl = com.supermenajer.game.model.Formations.slots(gs.formation)
            val txt = gs.lineup.mapIndexed { i, id ->
                val p = world.player(id)!!
                val d = com.supermenajer.game.game.Positions.slotDet(sl[i])
                "${d.code}:${p.name}(${com.supermenajer.game.game.Positions.codes(p)}) fit=${com.supermenajer.game.game.Positions.fit(p, d)} r=${com.supermenajer.game.game.Positions.rating(p, sl[i])}"
            }
            println("GS442: " + txt.joinToString(" | "))
            assertTrue(gs.lineup.indices.all { i -> com.supermenajer.game.game.Positions.fit(world.player(gs.lineup[i])!!, com.supermenajer.game.game.Positions.slotDet(sl[i])) >= 0.97 })
        }
    }
}
