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
        GameRepo.screen = Screen.SPLASH
        shot("00a_splash")
        GameRepo.screen = Screen.TITLE
        shot("00b_menu_empty")
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
        g.introDone = false; GameRepo.tab = 0
        shot("01b_intro_news")
        g.introDone = true
        GameRepo.tab = 4
        shot("03_transfer")
        val cmp = g.players.first { it.teamId >= 0 && it.teamId != g.userTeamId && it.pos == com.supermenajer.game.model.Pos.FW && it.overall > 75 }
        GameRepo.toggleWatch(cmp); GameRepo.compareWith(cmp)
        shot("03h_compare")
        GameRepo.compare = null; GameRepo.comparePin = null
        val tgt = g.players.first { it.teamId >= 0 && it.teamId != g.userTeamId && GameRepo.transferBlock(it) == null && g.squad(it.teamId).size > 20 }
        GameRepo.startTalks(tgt)
        GameRepo.negotiation!!.fee = GameRepo.askingPrice(tgt) / 2
        GameRepo.talkClub()
        GameRepo.negotiation!!.fee = GameRepo.askingPrice(tgt) * 2
        GameRepo.talkClub()
        shot("03_talks")
        GameRepo.closeTalks()
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 0
        shot("03a_staff")
        GameRepo.tab = GameRepo.TAB_SQUAD
        shot("03a_squad")
        GameRepo.selectedPlayer = g.squad(g.userTeamId).filter { it.age <= 21 }.minBy { it.overall }.also {
            GameRepo.setFocus(it, 3); GameRepo.setLearn(it, com.supermenajer.game.game.Development.learnable(it).first().ordinal); it.learnProgress = 35
        }
        shot("03b_own_player")
        GameRepo.selectedPlayer = null
        advance(4)
        g.userTeam.budget += 5_000_000
        GameRepo.startProject(com.supermenajer.game.game.Facilities.TRAINING)
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 1
        shot("03c_facilities")
        GameRepo.signSponsor(com.supermenajer.game.game.Sponsors.offers(g, 0)[1])
        GameRepo.clubMode = 2
        shot("03d_sponsors")
        GameRepo.clubMode = 3
        shot("03e_finance")
        GameRepo.tab = GameRepo.TAB_INBOX
        shot("03f_inbox")
        GameRepo.tab = GameRepo.TAB_HOME; GameRepo.settingsOpen = true
        shot("03g_settings")
        GameRepo.settingsOpen = false
        g.offers += IncomingOffer(g.squad(g.userTeamId).maxBy { it.overall }.id, g.divisionTeams(12)[0].id, 20_000_000, g.week)
        g.press = PressQuestion("Rakip karşısında galibiyetin anahtarı neydi?", listOf("Oyuncularımın mücadelesi.", "Taktiğimiz tuttu, plan işledi.", "Henüz iyi oynamıyoruz."), 1)
        GameRepo.tab = 0; shot("04_home")
        GameRepo.reportOpen = false
        g.press = null
        GameRepo.tab = 3; GameRepo.leagueTab = 0; GameRepo.leagueSel = 10; shot("05_league_eng")
        GameRepo.leagueSel = 0; shot("06_league_tr")
        GameRepo.leagueTab = 1; shot("07_cup")
        GameRepo.leagueTab = 2; shot("08_euro")
        GameRepo.leagueTab = 3; shot("09_fixtures")
        GameRepo.tab = 2; GameRepo.tacticsMode = 0; shot("10_tactics")
        GameRepo.tacticsPanel = 1; shot("10b_formations")
        GameRepo.tacticsPanel = 2; shot("10a_instructions")
        GameRepo.tacticsPanel = 3; shot("10c_roles"); GameRepo.tacticsPanel = 0
        GameRepo.tacticWizard = true; shot("10d_tactic_wizard"); GameRepo.tacticWizard = false
        GameRepo.tacticsPanel = 2; GameRepo.instrSub = 3; shot("10e_duties"); GameRepo.instrSub = 0
        GameRepo.tacticsPanel = 3; GameRepo.roleDialog = 4; shot("10f_role_dialog"); GameRepo.roleDialog = -1; GameRepo.tacticsPanel = 0
        // A2 takımı ve milli takım
        g.squad(g.userTeamId).filter { it.id !in g.userTeam.lineup }.sortedBy { it.age }.take(3).forEach { GameRepo.setReserve(it, true) }
        com.supermenajer.game.game.Reserves.weekly(g, 3, kotlin.random.Random(3))
        GameRepo.tab = 1; GameRepo.squadMode = 2; shot("10g_a2"); GameRepo.squadMode = 0
        GameRepo.tab = GameRepo.TAB_NATIONAL; shot("10h_national_offers")
        g.managerRep = 90; GameRepo.takeNationalJob("GB")
        com.supermenajer.game.game.National.weekly(g, 6, kotlin.random.Random(6))
        shot("10i_national")
        GameRepo.tab = 0; shot("10j_national_pending")
        GameRepo.playNational(live = true)
        run {
            val sim = GameRepo.match!!
            val an = GameRepo.pitchFor(sim)
            repeat(38) { sim.step() }
            repeat(60) { an.update(1f / 30f) }
        }
        shot("10k_national_match")
        GameRepo.finishMatch()
        shot("10l_national_summary")
        GameRepo.closeSummary()
        GameRepo.resignNational()
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 5; shot("10d_career")
        GameRepo.clubMode = 3; shot("10e_finance")
        // v1.5: dinamikler, yetenek ağacı, hoca pazarı, olay kartı
        GameRepo.tab = GameRepo.TAB_SQUAD; GameRepo.squadMode = 1; shot("17_dynamics"); GameRepo.squadMode = 0
        com.supermenajer.game.game.Perks.addXp(g, 420)
        GameRepo.upgradePerk("tactic"); GameRepo.upgradePerk("nego")
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 6; shot("18_perks")
        val rival = g.divisionTeams(g.userTeam.division).first { it.id != g.userTeamId }
        g.vacancies += com.supermenajer.game.model.Vacancy(rival.id, g.date + 14)
        g.managers[rival.id] = "Geçici: " + (g.managers[rival.id] ?: "")
        GameRepo.clubMode = 7; shot("19_manager_market")
        g.vacancies.clear()
        val ev = g.squad(g.userTeamId).first()
        g.pendingEvent = com.supermenajer.game.model.GameEvent(0, ev.id, "⚠️ Disiplin sorunu", "${ev.name} antrenmana geç kaldı ve yardımcı antrenörle tartıştı. Soyunma odası tepkini bekliyor.", listOf("Para cezası kes", "Özel olarak uyar", "Görmezden gel"))
        GameRepo.tab = 0; shot("20_event")
        g.pendingEvent = null
        // canlı maç, devre arası
        while (g.todayUserFixture() == null && !g.seasonOver && !g.fired) GameRepo.skipToNextMatch()
        GameRepo.playMatch(live = true)
        shot("10b_prematch")
        fun playAnim(steps: Int, extraSec: Float) {
            val sim = GameRepo.match!!
            val an = GameRepo.pitchFor(sim)
            repeat(steps) {
                val b = sim.events.size
                sim.step()
                for (e in sim.events.subList(b, sim.events.size).toList()) {
                    val o = an.outcomes
                    an.play(e)
                    var w = 0
                    while (e.buildUp.isNotEmpty() && an.outcomes == o && w++ < 300) an.update(1f / 30f)
                }
                repeat(20) { an.update(1f / 30f) }
            }
            repeat((extraSec * 30).toInt()) { an.update(1f / 30f) }
        }
        playAnim(30, 1.3f)
        shot("11_match_ticker")
        playAnim(15, 0f)
        shot("11b_halftime")
        playAnim(20, 0f)
        // Bir atağın ortasında dur
        run {
            val sim = GameRepo.match!!
            val an = GameRepo.pitchFor(sim)
            an.play(com.supermenajer.game.engine.MatchEvent(sim.minute, com.supermenajer.game.engine.Ev.SAVE, sim.home.index, "", sim.home.onPitch.last().p.id, "x", sim.home.onPitch[6].p.id, 0))
            repeat(60) { an.update(1f / 30f) }
        }
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
        GameRepo.flush()
        GameRepo.screen = Screen.TITLE; shot("00_title")
    }
}
