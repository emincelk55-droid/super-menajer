package com.supermenajer.game

import com.supermenajer.game.game.AutoPilot
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Preview
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Sezon önizlemesi: sezon başında tahmin, sezon sonunda tahmin-gerçek haberi. */
class PreviewTest {
    @Test
    fun previewAndReview() {
        GameRepo.init(Files.createTempDirectory("prev").toFile())
        val w = Generator.newWorld(Random(12)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Test", w.divisionTeams(0).sortedBy { it.reputation }[6].id)
        val g = GameRepo.game!!
        g.introDone = true
        val rows = Preview.rows(g)
        assertEquals(g.divisionTeams(g.userTeam.division).size, rows.size)
        assertTrue(rows.any { it.teamId == g.userTeamId })
        assertTrue("favori en yüksek ihtimal", rows[0].odds >= rows[1].odds)
        assertTrue(rows.sumOf { it.odds } in 90..100)
        assertTrue(g.news.any { it.title.contains("önizleme") || it.title.contains("preview") })
        val log = AutoPilot.Log()
        val rnd = Random(3)
        var n = 0
        while (log.seasons < 1 && n++ < 4000) AutoPilot.step(log, rnd)
        assertTrue("sezon sonu tahmin haberi", g.news.any { it.title.startsWith("📺") && !it.title.contains("önizleme") })
        assertTrue("yeni sezonun önizlemesi", Preview.rows(g).isNotEmpty())
        println("ONIZLEME " + g.news.filter { it.title.startsWith("📺") }.take(3).joinToString(" || ") { it.title + ": " + it.body })
    }
}
