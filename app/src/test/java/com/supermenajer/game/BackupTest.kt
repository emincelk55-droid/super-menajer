package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Kayıt yedekleme: yuva dosyaya aktarılır, başka yuvaya geri yüklenir; bozuk dosya reddedilir. */
class BackupTest {
    @Test
    fun exportImportRoundTrip() {
        GameRepo.init(Files.createTempDirectory("smb").toFile())
        val w = Generator.newWorld(Random(31)).also { Season.computeExpectations(it) }
        GameRepo.slot = 1
        GameRepo.newCareer(w, "Yedek Hoca", w.divisionTeams(0)[2].id)
        val g = GameRepo.game!!
        val team = g.userTeam.name
        GameRepo.skipToNextMatch()
        val bytes = GameRepo.exportSlot(1)
        assertNotNull(bytes)
        assertTrue(GameRepo.exportName(1).endsWith(".smk"))
        assertNotNull("bozuk dosya reddedilmeli", GameRepo.importSlot(2, "çöp veri".toByteArray()))
        assertNull(GameRepo.importSlot(2, bytes))
        assertTrue(GameRepo.slots()[1].exists)
        assertTrue(GameRepo.load(2))
        assertEquals(team, GameRepo.game!!.userTeam.name)
        assertEquals("Yedek Hoca", GameRepo.game!!.managerName)
        assertEquals(g.date, GameRepo.game!!.date)
        // Diğer testler 1. yuvayı bekler
        GameRepo.closeCareer()
        GameRepo.slot = 1
    }
}
