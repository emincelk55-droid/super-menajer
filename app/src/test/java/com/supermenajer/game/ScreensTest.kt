package com.supermenajer.game

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.model.IncomingOffer
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

    private fun advance(days: Int) {
        val g = GameRepo.game!!
        repeat(days) {
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipDay()
        }
    }

    @Test
    fun screens() {
        GameRepo.init(Files.createTempDirectory("sm").toFile())
        GameRepo.screen = Screen.NEW_GAME
        shot("01_newgame")
        GameRepo.newCareer(Generator.newWorld(), "Emin Hoca", 0)
        val g = GameRepo.game!!
        GameRepo.tab = 4
        GameRepo.selectedPlayer = g.players.first { it.teamId == 25 && it.overall > 55 }
        shot("00_negotiate_open")
        GameRepo.selectedPlayer = null
        advance(6)
        g.offers += IncomingOffer(g.squad(0).maxBy { it.overall }.id, 0, 9_000_000, g.week)
        GameRepo.tab = 0; shot("02_home")
        GameRepo.tab = 2; GameRepo.tacticsMode = 1; shot("03_training")
        GameRepo.tab = 3; GameRepo.leagueTab = 2; shot("04_cup")
        GameRepo.leagueTab = 1; shot("05_firstleague")
        GameRepo.tab = 4; shot("06_transfer")
        GameRepo.selectedPlayer = g.squad(5).maxBy { it.overall }
        shot("07_negotiate")
        GameRepo.selectedPlayer = g.players.first { it.teamId == 25 }
        shot("08_negotiate2")
        GameRepo.selectedPlayer = null
        while (g.today?.type != 2 || g.todayUserFixture() == null) advance(1)
        GameRepo.playMatch(live = true)
        repeat(60) { GameRepo.match!!.step() }
        shot("13_euro_match")
        GameRepo.finishMatch(); GameRepo.closeSummary()
        GameRepo.tab = 0; shot("14_home_euro")
        GameRepo.tab = 3; GameRepo.leagueTab = 5; shot("15_euro_tab")
        advance(14)
        GameRepo.tab = 1; shot("09_squad")
        GameRepo.tab = 0; shot("10_home_later")
        // kupa maçı canlı
        while (g.today?.type != 1 || g.todayUserFixture() == null) {
            if (g.seasonOver) break
            advance(1)
        }
        if (g.todayUserFixture() != null) {
            GameRepo.playMatch(live = true)
            GameRepo.match!!.runToEnd()
            shot("11_cupmatch")
            GameRepo.finishMatch()
        }
        while (!g.seasonOver) advance(1)
        GameRepo.tab = 0; shot("12_seasonend")
    }
}
