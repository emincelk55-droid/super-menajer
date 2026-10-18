package com.supermenajer.game

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.IncomingOffer
import com.supermenajer.game.model.PressQuestion
import com.supermenajer.game.ui.AppTheme
import com.supermenajer.game.ui.Bg
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 2340, screenHeight = 1080, orientation = ScreenOrientation.LANDSCAPE),
        maxPercentDifference = 100.0,
    )

    private fun shot(name: String) {
        paparazzi.snapshot(name) { AppTheme { Surface(Modifier.fillMaxSize(), color = Bg) { AppRoot() } } }
    }

    private fun advance(days: Int) {
        val g = GameRepo.game!!
        repeat(days) {
            if (g.seasonOver) return
            if (g.todayUserFixture() != null) { GameRepo.playMatch(live = false); GameRepo.closeSummary() } else GameRepo.skipToNextMatch()
        }
    }

    @Test
    fun screens() {
        GameRepo.init(Files.createTempDirectory("sm").toFile())
        GameRepo.preparedWorld = Generator.newWorld().also { Season.computeExpectations(it) }
        GameRepo.screen = Screen.NEW_GAME
        shot("01_newgame")
        // İngiltere'den bir takımla başla
        val world = Generator.newWorld().also { Season.computeExpectations(it) }
        val eng = world.divisionTeams(10).sortedBy { it.expected }[1]
        GameRepo.newCareer(world, "Emin Hoca", eng.id)
        val g = GameRepo.game!!
        GameRepo.tab = 4
        GameRepo.selectedPlayer = g.players.first { it.teamId == g.divisionTeams(14)[3].id && it.overall > 60 }
        shot("02_negotiate")
        GameRepo.selectedPlayer = null
        shot("03_transfer")
        GameRepo.tab = 1; GameRepo.squadMode = 1
        shot("03a_staff")
        GameRepo.squadMode = 0
        GameRepo.selectedPlayer = g.squad(g.userTeamId).filter { it.age <= 21 }.minBy { it.overall }
        shot("03b_own_player")
        GameRepo.selectedPlayer = null
        advance(4)
        g.userTeam.budget += 5_000_000
        GameRepo.startProject(com.supermenajer.game.game.Facilities.TRAINING)
        GameRepo.tab = 1; GameRepo.squadMode = 2
        shot("03c_facilities")
        GameRepo.signSponsor(com.supermenajer.game.game.Sponsors.offers(g, 0)[1])
        GameRepo.squadMode = 3
        shot("03d_sponsors")
        GameRepo.squadMode = 0
        g.offers += IncomingOffer(g.squad(g.userTeamId).maxBy { it.overall }.id, g.divisionTeams(12)[0].id, 20_000_000, g.week)
        g.press = PressQuestion("Rakip karşısında galibiyetin anahtarı neydi?", listOf("Oyuncularımın mücadelesi.", "Taktiğimiz tuttu, plan işledi.", "Henüz iyi oynamıyoruz."), 1)
        GameRepo.tab = 0; GameRepo.reportOpen = true; shot("04_home")
        GameRepo.reportOpen = false
        g.press = null
        GameRepo.tab = 3; GameRepo.leagueTab = 0; GameRepo.leagueSel = 10; shot("05_league_eng")
        GameRepo.leagueSel = 0; shot("06_league_tr")
        GameRepo.leagueTab = 1; shot("07_cup")
        GameRepo.leagueTab = 2; shot("08_euro")
        GameRepo.leagueTab = 3; shot("09_fixtures")
        GameRepo.tab = 2; GameRepo.tacticsMode = 0; shot("10_tactics")
        // canlı maç, devre arası
        while (g.todayUserFixture() == null && !g.seasonOver && !g.fired) GameRepo.skipToNextMatch()
        GameRepo.playMatch(live = true)
        shot("10b_prematch")
        repeat(30) { GameRepo.match!!.step() }
        shot("11_match_ticker")
        repeat(15) { GameRepo.match!!.step() }
        repeat(20) { GameRepo.match!!.step() }
        shot("12_match")
        GameRepo.finishMatch()
        shot("13_summary")
        GameRepo.closeSummary()
        // gün atlama kartı
        while (g.todayUserFixture() != null && !g.seasonOver && !g.fired) advance(1)
        GameRepo.tab = 0; shot("14_restday")
        while (!g.seasonOver && !g.fired) advance(1)
        GameRepo.tab = 0; shot("15_seasonend")
        GameRepo.startNewSeason()
        GameRepo.tab = 3; GameRepo.leagueTab = 5; shot("16_history")
    }
}
