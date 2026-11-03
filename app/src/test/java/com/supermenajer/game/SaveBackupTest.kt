package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

/** Döner yedekler: bozuk ana kayıt en yeni sağlam yedekten açılır, yuva silinince yedekler de gider. */
class SaveBackupTest {
    @Test
    fun corruptSaveRestoresFromBackup() {
        val dir = Files.createTempDirectory("smbak").toFile()
        GameRepo.init(dir)
        val w = Generator.newWorld(Random(9)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Yedek", w.divisionTeams(0)[1].id)
        val season = GameRepo.game!!.season
        GameRepo.save(force = true); GameRepo.flush()
        GameRepo.save(force = true); GameRepo.flush()
        val main = File(dir, "kariyer.json")
        val bak = File(dir, "kariyer1.bak1")
        assertTrue(main.exists() && bak.exists())

        // Elektrik kesintisinde yarım yazılmış dosya
        main.writeText(main.readText().take(500))
        GameRepo.closeCareer()
        assertTrue(GameRepo.load(1))
        assertNotNull(GameRepo.restoredNote)
        assertEquals(season, GameRepo.game!!.season)
        GameRepo.flush()
        // Kurtarılan kariyer ana dosyaya yeniden yazıldı
        GameRepo.restoredNote = null
        GameRepo.closeCareer()
        assertTrue(GameRepo.load(1))
        assertEquals(null, GameRepo.restoredNote)

        // Hem ana kayıt hem yedekler bozuksa yükleme başarısız olur, başka yuvaya dokunulmaz
        GameRepo.flush()
        GameRepo.closeCareer()
        main.writeText("{bozuk")
        dir.listFiles()!!.filter { it.name.startsWith("kariyer1.bak") }.forEach { it.writeText("{bozuk") }
        assertFalse(GameRepo.load(1))

        GameRepo.deleteSlot(1)
        assertFalse(bak.exists() || main.exists())
    }
}
