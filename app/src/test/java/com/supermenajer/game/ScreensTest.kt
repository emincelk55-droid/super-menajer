package com.supermenajer.game

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.ui.AppTheme
import com.supermenajer.game.ui.Bg
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 100.0)

    private fun shot(name: String) {
        paparazzi.snapshot(name) { AppTheme { Surface(Modifier.fillMaxSize(), color = Bg) { AppRoot() } } }
    }

    @Test
    fun screens() {
        GameRepo.init(Files.createTempDirectory("sm").toFile())
        GameRepo.screen = Screen.TITLE
        shot("01_title")
        GameRepo.screen = Screen.NEW_GAME
        shot("02_newgame")
        GameRepo.newCareer(Generator.newWorld(), "Emin Hoca", 3)
        repeat(3) { GameRepo.playMatch(live = false); GameRepo.closeSummary() }
        GameRepo.tab = 0; shot("03_home")
        GameRepo.tab = 1; shot("04_squad")
        GameRepo.tab = 2; shot("05_tactics")
        GameRepo.tab = 3; shot("06_league")
        GameRepo.tab = 4; shot("07_transfer")
        GameRepo.tab = 1
        GameRepo.selectedPlayer = GameRepo.game!!.squad(3).first()
        shot("08_player")
        GameRepo.selectedPlayer = null
        GameRepo.playMatch(live = true)
        val sim = GameRepo.match!!
        repeat(70) { sim.step() }
        shot("09_match")
        GameRepo.finishMatch()
        shot("10_summary")
    }
}
