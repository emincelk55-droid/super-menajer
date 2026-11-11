package com.supermenajer.game

import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Game
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/** Arma/forma editörü: seçimler kaydedilir, ad değişikliği doğrulanır. */
class IdentityTest {
    @Test
    fun editAndRename() {
        GameRepo.init(Files.createTempDirectory("smi").toFile())
        val w = Generator.newWorld(Random(4)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(w, "Kimlik", w.divisionTeams(0)[3].id)
        val g = GameRepo.game!!
        val other = g.teams.first { it.id != g.userTeamId }
        GameRepo.editIdentity { it.crestShape = 2; it.crestPattern = 5; it.crestIcon = 4; it.kitPattern = 2; it.color1 = 0xFF6A1B9A }
        assertTrue(GameRepo.renameClub("Ab", "XYZ").contains("en az"))
        assertTrue(GameRepo.renameClub("Yeni Kulüp", other.short).contains("başka"))
        assertTrue(GameRepo.renameClub("Yeni Kulüp", "Y1Z").contains("3 harf"))
        assertTrue(GameRepo.renameClub("Yeni Kulüp", "QQZ").startsWith("Kaydedildi"))
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val back = json.decodeFromString(Game.serializer(), json.encodeToString(Game.serializer(), g))
        val t = back.userTeam
        assertEquals("Yeni Kulüp", t.name)
        assertEquals("QQZ", t.short)
        assertEquals(listOf(2, 5, 4, 2), listOf(t.crestShape, t.crestPattern, t.crestIcon, t.kitPattern))
        assertEquals(0xFF6A1B9A, t.color1)
    }
}
