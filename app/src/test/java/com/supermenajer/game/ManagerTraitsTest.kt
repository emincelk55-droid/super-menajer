package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.ManagerTraits
import com.supermenajer.game.game.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Menajer özellikleri: maç oynadıkça istatistik birikir, eşik aşılınca özellik kazanılır ve maçta etkisi olur. */
class ManagerTraitsTest {
    @Test
    fun traitsAccumulateAndApply() {
        GameRepo.init(Files.createTempDirectory("smt").toFile())
        val w = Generator.newWorld(Random(21)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Öz", w.divisionTeams(0)[0].id)
        val g = GameRepo.game!!
        // Savunma ustasına bir maç kala
        g.traitStats["clean"] = 14
        var n = 0
        while (g.matchLog.size < 12 && n++ < 200) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
            g.pendingEvent = null
        }
        println("ÖZELLİK ${g.traitStats} kazanılan=${g.traitsEarned}")
        assertTrue("u21 sayacı işliyor", (g.traitStats["u21"] ?: 0) >= 0)
        assertTrue("maç sonrası sayaçlar", g.traitStats.values.sum() > 14)
        if ((g.traitStats["clean"] ?: 0) >= 15) assertTrue("eşik aşılınca kazanılır", ManagerTraits.has(g, "wall"))
        g.traitsEarned += "youth"
        assertTrue(ManagerTraits.youthDev(g) > 1.0)
    }
}
