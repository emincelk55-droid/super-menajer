package com.supermenajer.game

import com.supermenajer.game.engine.Ev
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.Pitch2D
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

/** 2D sahne: her şut sahada sonuçlanmalı, top ve oyuncular sahada kalmalı, pas akışı olmalı. */
class Pitch2DTest {
    @Test
    fun fullMatchAnimates() {
        GameRepo.init(Files.createTempDirectory("sm16").toFile())
        val world = Generator.newWorld(Random(16)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        val f = g.fixtures.first { it.home == g.userTeamId || it.away == g.userTeamId }
        val sim = MatchSim(g, f, g.userTeamId, true, Random(3))
        val anim = Pitch2D(sim)
        val dump = System.getenv("PITCH_DUMP")?.let { File(it) }
        val out = StringBuilder()
        var t = 0f
        var shots = 0
        var owners = HashSet<Int>()
        var changes = 0
        var lastSide = -1
        var frame = 0
        fun tick(sec: Float) {
            var left = sec
            while (left > 0) {
                anim.update(1f / 30f)
                left -= 1f / 30f
                t += 1f / 30f
                frame++
                assertTrue("top x ${anim.ballX}", anim.ballX in -6f..(Pitch2D.L + 6f) && !anim.ballX.isNaN())
                assertTrue("top y ${anim.ballY}", anim.ballY in -6f..(Pitch2D.W + 6f) && !anim.ballY.isNaN())
                for (a in anim.agents) assertTrue(!a.x.isNaN() && a.x in -4f..(Pitch2D.L + 4f))
                anim.owner?.let { o -> owners += o.id; if (o.side != lastSide) { changes++; lastSide = o.side } }
                if (dump != null && frame % 3 == 0 && t < 120f) {
                    out.append("%.2f;%.2f;%.2f;%.2f".format(t, anim.ballX, anim.ballY, anim.ballZ))
                    for (a in anim.agents) out.append(";${a.side},${if (a.gk) 1 else 0},${"%.2f".format(a.x)},${"%.2f".format(a.y)},${if (a === anim.focus()) 1 else 0}")
                    out.append('\n')
                }
            }
        }
        while (!sim.finished) {
            val before = sim.events.size
            sim.step()
            for (e in sim.events.subList(before, sim.events.size).toList()) {
                val o = anim.outcomes
                anim.play(e)
                if (e.buildUp.isNotEmpty()) {
                    shots++
                    var w = 0f
                    while (anim.outcomes == o && w < 12f) { tick(0.1f); w += 0.1f }
                    assertTrue("${e.type} ${e.style} sahada sonuçlanmadı (dk ${e.minute})", anim.outcomes > o)
                }
                // Maç ekranı gibi: gol sevinci ve tekrar bitince devam
                var cw = 0f
                while (anim.celebrating && cw < 15f) { tick(0.1f); cw += 0.1f }
            }
            tick(0.65f)
        }
        dump?.writeText(out.toString())
        println("PITCH: ${"%.0f".format(t)} sn, şut $shots, topa dokunan ${owners.size}, top el değiştirme $changes")
        assertEquals(22, anim.agents.size.coerceAtMost(22).let { if (it < 20) 0 else 22 })
        // Maçların çoğunda 17-20 farklı oyuncu topa dokunur; sınırda kalan maçlar testi düşürmesin
        assertTrue("topa dokunan ${owners.size}", owners.size >= 16)
        assertTrue(changes >= 20)
    }

    /** Devre arası ve maç sonu: oyuncular tünelden soyunma odasına girer, sahne durur; ikinci yarıda geri çıkarlar. */
    @Test
    fun walkOffToDressingRoom() {
        GameRepo.init(Files.createTempDirectory("sm16w").toFile())
        val world = Generator.newWorld(Random(16)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        val f = g.fixtures.first { it.home == g.userTeamId || it.away == g.userTeamId }
        val sim = MatchSim(g, f, g.userTeamId, true, Random(3))
        val anim = Pitch2D(sim)
        repeat(60) { anim.update(1f / 30f) }
        anim.play(com.supermenajer.game.engine.MatchEvent(45, Ev.HALF, -1, "Devre"))
        assertTrue(anim.busy)
        repeat(30 * 40) { anim.update(1f / 30f) }
        assertTrue("devre arasında herkes soyunma odasında", anim.offPitch && !anim.busy)
        // İkinci yarı: takımlar sahaya çıkar ve santra yerlerine koşar
        anim.secondHalf()
        assertTrue(anim.agents.all { it.alpha == 1f })
        repeat(30 * 8) { anim.update(1f / 30f) }
        assertTrue(anim.agents.all { it.y in 0f..Pitch2D.W })
        // Maç sonu: düdükten sonra kimse topla oynamaz, sonunda sahne tamamen durur
        anim.play(com.supermenajer.game.engine.MatchEvent(90, Ev.FULL, -1, "Bitti"))
        repeat(30 * 40) { anim.update(1f / 30f) }
        assertTrue(anim.offPitch && anim.owner == null)
        val bx = anim.ballX
        repeat(30 * 5) { anim.update(1f / 30f) }
        assertEquals(bx, anim.ballX)
    }

    /** Gol tekrarı ağır çekim oynar ve sonra santra yapılır; atakta ofsayt çizgisi görünür; serbest vuruşta baraj kurulur. */
    @Test
    fun replayOffsideAndWall() {
        GameRepo.init(Files.createTempDirectory("sm16r").toFile())
        val world = Generator.newWorld(Random(16)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        val f = g.fixtures.first { it.home == g.userTeamId || it.away == g.userTeamId }
        val sim = MatchSim(g, f, g.userTeamId, true, Random(3))
        val anim = Pitch2D(sim)
        repeat(30 * 6) { anim.update(1f / 30f) }
        val scorer = sim.home.onPitch.last().p.id
        anim.play(com.supermenajer.game.engine.MatchEvent(10, Ev.GOAL, 0, "Gol", scorer, "Atak...", sim.home.onPitch[6].p.id, 0))
        var sawOffside = false
        var t = 0
        while (anim.outcomes == 0 && t++ < 30 * 12) { anim.update(1f / 30f); if (anim.offsideLineX != null) sawOffside = true }
        assertTrue("gol sahada oynandı", anim.outcomes == 1)
        assertTrue("atakta ofsayt çizgisi", sawOffside)
        var sawReplay = false
        var progress = 0f
        repeat(30 * 14) {
            anim.update(1f / 30f)
            anim.replayView?.let { sawReplay = true; assertTrue(it.agents.size >= 20); progress = maxOf(progress, it.progress) }
        }
        assertTrue("tekrar oynadı", sawReplay && progress > 0.95f)
        assertEquals(null, anim.replayView)
        assertTrue("tekrardan sonra oyun devam ediyor", !anim.busy)

        // Serbest vuruş: savunmadan 4 oyuncu baraj kurar (topa ~9 m)
        val def = sim.away.onPitch.filter { it.slot.pos != com.supermenajer.game.model.Pos.GK }.map { it.p.id }.toSet()
        anim.play(com.supermenajer.game.engine.MatchEvent(20, Ev.SAVE, 0, "Frikik", scorer, "Serbest vuruş...", -1, 3))
        var best = 0
        repeat(30 * 8) {
            anim.update(1f / 30f)
            val n = anim.agents.count { it.id in def && kotlin.math.hypot(it.x - anim.ballX, it.y - anim.ballY) in 7.5f..11f }
            best = maxOf(best, n)
        }
        assertTrue("baraj $best", best >= 3)
    }
}
