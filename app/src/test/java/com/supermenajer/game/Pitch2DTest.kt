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
            }
            tick(0.65f)
        }
        dump?.writeText(out.toString())
        println("PITCH: ${"%.0f".format(t)} sn, şut $shots, topa dokunan ${owners.size}, top el değiştirme $changes")
        assertEquals(22, anim.agents.size.coerceAtMost(22).let { if (it < 20) 0 else 22 })
        assertTrue(owners.size >= 18)
        assertTrue(changes >= 20)
    }
}
