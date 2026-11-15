package com.supermenajer.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.ui.AppTheme
import com.supermenajer.game.ui.Bg
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/** Dikey ekran: telefon dik tutulunca ana ekranlar. */
class PortraitTest {
    init { com.supermenajer.game.ui.Fx.enabled = false }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1080, screenHeight = 2340, orientation = ScreenOrientation.PORTRAIT),
        maxPercentDifference = 100.0,
    )

    private fun shot(name: String) {
        paparazzi.snapshot(name) { AppTheme { Surface(Modifier.fillMaxSize(), color = Bg) { AppRoot() } } }
    }

    @Test
    fun portraitScreens() {
        GameRepo.init(Files.createTempDirectory("smp").toFile())
        GameRepo.preparedWorld = Generator.newWorld(kotlin.random.Random(9)).also { Season.computeExpectations(it) }
        GameRepo.screen = Screen.TITLE; shot("p00_menu")
        val w = Generator.newWorld(kotlin.random.Random(9)).also { Season.computeExpectations(it) }
        com.supermenajer.game.game.Fictional.apply(w)
        GameRepo.newCareer(w, "Dik Ekran", w.divisionTeams(0).sortedByDescending { it.reputation }[3].id)
        val g = GameRepo.game!!
        g.introDone = true; g.ceremonySeen = 999
        GameRepo.screen = Screen.MAIN
        GameRepo.tab = GameRepo.TAB_HOME; shot("p01_home")
        GameRepo.tab = GameRepo.TAB_SQUAD; GameRepo.squadMode = 0; shot("p02_squad")
        GameRepo.tab = GameRepo.TAB_TACTICS; GameRepo.tacticsMode = 0; shot("p03_tactics")
        GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 0; GameRepo.leagueSel = g.userTeam.division; shot("p04_table")
        GameRepo.tab = GameRepo.TAB_TRANSFER; shot("p05_transfer")
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3; shot("p06_finance")
        GameRepo.tab = GameRepo.TAB_INBOX; shot("p07_inbox")
        GameRepo.selectedPlayer = g.squad(g.userTeamId).first(); shot("p08_profile"); GameRepo.selectedPlayer = null
        GameRepo.skipToNextMatch(); GameRepo.playMatch(live = true); shot("p09_match")
    }
}
