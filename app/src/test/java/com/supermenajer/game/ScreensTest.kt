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
import androidx.compose.foundation.background
import com.supermenajer.game.ui.GoalCelebrationFrame
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.supermenajer.game.ui.TifoOverlay
import com.supermenajer.game.ui.drawFmPitch
import java.nio.file.Files

class ScreensTest {
    init { com.supermenajer.game.ui.Fx.enabled = false }

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

    /** Gol kutlaması: konfetinin havada olduğu bir kare. */
    @Test
    fun goalCelebration() {
        paparazzi.snapshot("21_goal_celebration") {
            AppTheme {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF256F3B)),
                ) {
                    GoalCelebrationFrame(
                        0.42f, androidx.compose.ui.graphics.Color(0xFF6CABDD), androidx.compose.ui.graphics.Color.White, true, 7,
                    )
                }
            }
        }
    }

    /** Parlak kart seviyeleri: efsane, altın, gümüş, bronz (FUT kartı ve saha kartı). */
    @Test
    fun cardTiers() {
        val w = com.supermenajer.game.game.Generator.newWorld(kotlin.random.Random(3))
        val ps = w.players.take(4)
        val ovrs = listOf(91, 80, 70, 60)
        paparazzi.snapshot("22_card_tiers") {
            AppTheme {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxSize().background(com.supermenajer.game.ui.Bg).padding(24.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    ps.forEachIndexed { i, pl ->
                        com.supermenajer.game.ui.PlayerCard(pl, ovrs[i], "OOS", 100.dp, androidx.compose.ui.graphics.Color(0xFF6CABDD), androidx.compose.ui.graphics.Color.White)
                    }
                    ps.forEachIndexed { i, pl ->
                        com.supermenajer.game.ui.SmPlayerCard(w, pl, ovrs[i], "OOS", com.supermenajer.game.ui.Teal, false, androidx.compose.ui.graphics.Color(0xFF6CABDD), androidx.compose.ui.graphics.Color.White, 72.dp)
                    }
                }
            }
        }
    }

    /** Ana sayfanın arkasındaki canlı stadyum, kartlar olmadan. */
    @Test
    fun stadiumBackdrop() {
        paparazzi.snapshot("23_stadium_backdrop") {
            AppTheme {
                com.supermenajer.game.ui.StadiumBackdrop(
                    androidx.compose.ui.graphics.Color(0xFF6CABDD), androidx.compose.ui.graphics.Color.White, Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 2D saha: atakta ofsayt çizgisi ve gol tekrarı (ağır çekim, sinema bandı). */
    @Test
    fun pitchReplay() {
        GameRepo.init(Files.createTempDirectory("smr").toFile())
        val world = Generator.newWorld(kotlin.random.Random(16)).also { Season.computeExpectations(it) }
        GameRepo.newCareer(world, "Test", world.divisionTeams(10)[0].id)
        val g = GameRepo.game!!
        val f = g.fixtures.first { it.home == g.userTeamId || it.away == g.userTeamId }
        val sim = com.supermenajer.game.engine.MatchSim(g, f, g.userTeamId, true, kotlin.random.Random(3))
        val anim = com.supermenajer.game.engine.Pitch2D(sim)
        repeat(30 * 6) { anim.update(1f / 30f) }
        anim.play(com.supermenajer.game.engine.MatchEvent(10, com.supermenajer.game.engine.Ev.GOAL, 0, "Gol", sim.home.onPitch.last().p.id, "Atak", sim.home.onPitch[6].p.id, 0))
        var n = 0
        while (anim.offsideLineX == null && n++ < 200) anim.update(1f / 30f)
        repeat(12) { anim.update(1f / 30f) }
        val c1 = androidx.compose.ui.graphics.Color(sim.home.team.color1)
        val c2 = androidx.compose.ui.graphics.Color(sim.away.team.color1)
        val pitch = @androidx.compose.runtime.Composable {
            AppTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF256F3B))) {
                    com.supermenajer.game.ui.MatchPitch(
                        anim, c1, c2, androidx.compose.ui.graphics.Color(0xFF26C6DA), androidx.compose.ui.graphics.Color(0xFFFF7043), false, 1f,
                        Modifier.fillMaxSize(),
                    )
                }
            }
        }
        paparazzi.snapshot("24_offside_line") { pitch() }
        val tvShot = @androidx.compose.runtime.Composable {
            AppTheme {
                com.supermenajer.game.ui.MatchPitchTV(
                    anim, c1, androidx.compose.ui.graphics.Color(0xFFF2F2F2), androidx.compose.ui.graphics.Color(0xFF26C6DA), androidx.compose.ui.graphics.Color(0xFFFF7043), false, 1f,
                    Modifier.fillMaxSize(),
                )
            }
        }
        n = 0
        while (anim.replayView == null && n++ < 30 * 20) anim.update(1f / 30f)
        // Tekrarın başında da gol atılan kale kadrajda olmalı
        while ((anim.replayView?.progress ?: 1f) < 0.12f && n++ < 30 * 40) anim.update(1f / 30f)
        paparazzi.snapshot("25a_goal_replay_tv_start") { tvShot() }
        while ((anim.replayView?.progress ?: 1f) < 0.7f && n++ < 30 * 40) anim.update(1f / 30f)
        paparazzi.snapshot("25_goal_replay") { pitch() }
        paparazzi.snapshot("25b_goal_replay_tv") { tvShot() }
        // Korner: kamera boş çime değil, ceza sahasındaki kalabalığa bakmalı
        anim.skipReplay()
        repeat(30 * 8) { anim.update(1f / 30f) }
        anim.play(com.supermenajer.game.engine.MatchEvent(30, com.supermenajer.game.engine.Ev.CORNER, 0, "Korner", sim.home.onPitch.last().p.id, "", -1, 0))
        n = 0
        while (n++ < 30 * 3) anim.update(1f / 30f)
        paparazzi.snapshot("25c_corner_tv") { tvShot() }
    }

    /** Derbi: aynı şehrin kulüpleri ve iki devin maçı derbi sayılır; derbi haftası kartı ve tribün koreografisi. */
    @Test
    fun derbyAtmosphere() {
        val w = Generator.newWorld(kotlin.random.Random(6)).also { Season.computeExpectations(it) }
        val tr = w.divisionTeams(0)
        val ist = tr.filter { it.city.equals(tr.groupBy { t -> t.city.lowercase() }.maxBy { it.value.size }.key, ignoreCase = true) }
        val a = ist[0]
        val b = ist[1]
        val f = com.supermenajer.game.model.Fixture(5, a.id, b.id)
        org.junit.Assert.assertEquals("${a.city} Derbisi", com.supermenajer.game.game.Talks.derbyName(w, f))
        val small = tr.filter { it.city != a.city }.minBy { it.reputation }
        org.junit.Assert.assertNull(com.supermenajer.game.game.Talks.derbyName(w, com.supermenajer.game.model.Fixture(5, a.id, small.id)))
        w.userTeamId = a.id
        paparazzi.snapshot("26_derby_week") {
            AppTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.supermenajer.game.ui.Bg).padding(24.dp)) {
                    com.supermenajer.game.ui.DerbyWeekCard(w, f, 3)
                }
            }
        }
        paparazzi.snapshot("27_derby_tifo") {
            AppTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF256F3B))) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawFmPitch() }
                    TifoOverlay(a, "${a.city} Derbisi", androidx.compose.ui.graphics.Color(a.color1), androidx.compose.ui.graphics.Color(a.color2))
                }
            }
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
        // Kupa töreni ayrı testte; akıştaki ekranları kapatmasın
        GameRepo.game!!.ceremonySeen = 999
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
        // Gelişim sekmesi: gençlere örnek geçmiş sezonlar
        run {
            val g0 = GameRepo.game!!
            g0.squad(g0.userTeamId).filter { it.age <= 21 }.forEachIndexed { i, p ->
                p.ovrHistory = mutableListOf(p.overall - 9 + i % 3, p.overall - 6, p.overall - 2 - i % 2)
            }
            GameRepo.squadMode = 3
            shot("03a2_youth")
            GameRepo.squadMode = 0
        }
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
        com.supermenajer.game.CrashGuard.pending = "Süper Menajer çökme raporu"
        shot("03h_crash_report")
        com.supermenajer.game.CrashGuard.dismiss()
        g.offers += IncomingOffer(g.squad(g.userTeamId).maxBy { it.overall }.id, g.divisionTeams(12)[0].id, 20_000_000, g.week)
        g.press = PressQuestion("Rakip karşısında galibiyetin anahtarı neydi?", listOf("Oyuncularımın mücadelesi.", "Taktiğimiz tuttu, plan işledi.", "Henüz iyi oynamıyoruz."), 1)
        GameRepo.tab = 0; shot("04_home")
        com.supermenajer.game.ui.Fx.changeTextSize(1)
        shot("04b_home_bigtext")
        GameRepo.settingsOpen = true
        shot("04c_settings_bigtext")
        GameRepo.settingsOpen = false
        com.supermenajer.game.ui.Fx.changeTextSize(0)
        com.supermenajer.game.ui.Tips.forceShow("home")
        shot("04d_tip")
        com.supermenajer.game.ui.Tips.forceShow(null)
        GameRepo.reportOpen = false
        g.press = null
        GameRepo.tab = 3; GameRepo.leagueTab = 0; GameRepo.leagueSel = 10; shot("05_league_eng")
        GameRepo.leagueSel = 0; shot("06_league_tr")
        GameRepo.leagueSel = 23; shot("06b_league_br"); GameRepo.leagueSel = 0
        GameRepo.leagueTab = 1; shot("07_cup")
        GameRepo.leagueTab = 2; shot("08_euro")
        GameRepo.leagueTab = 3; shot("09_fixtures")
        GameRepo.tab = 2; GameRepo.tacticsMode = 0; shot("10_tactics")
        run {
            val f0 = GameRepo.game!!.userTeam.formation
            for (f in listOf("4-2-3-1", "3-5-2", "5-3-2")) { GameRepo.setFormation(f); shot("10t_formation_" + f.replace("-", "")) }
            GameRepo.setFormation(f0)
        }
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
        // Rakip takım profili, sağlık kararı, efsaneler
        GameRepo.tab = 0; GameRepo.openTeam = g.divisionTeams(g.userTeam.division).first { it.id != g.userTeamId }.id
        shot("10o_team_profile"); GameRepo.openTeam = -1
        g.squad(g.userTeamId).first().let { it.injuredWeeks = 5; it.injuryType = "Diz bağı zedelenmesi"; g.medical += it.id }
        g.pendingEvent = null
        shot("10p_medical")
        g.medical.clear(); g.squad(g.userTeamId).first().let { it.injuredWeeks = 0; it.injuryType = "" }
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 8; shot("10q_legends"); GameRepo.clubMode = 0
        com.supermenajer.game.game.Tournament.start(g, kotlin.random.Random(11))
        com.supermenajer.game.game.National.simulatePending(g)
        com.supermenajer.game.game.National.simulatePending(g)
        GameRepo.tab = GameRepo.TAB_NATIONAL; shot("10m_tournament")
        GameRepo.playNational(live = true)
        run {
            val sim = GameRepo.match!!
            repeat(20) { sim.step() }
            repeat(40) { GameRepo.pitchFor(sim).update(1f / 30f) }
        }
        shot("10n_tournament_match")
        GameRepo.finishMatch(); GameRepo.closeSummary()
        g.tourSeason = -1; g.tourTeams.clear()
        GameRepo.resignNational()
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 5; shot("10d_career")
        GameRepo.clubMode = 3; shot("10e_finance")
        // v1.5: dinamikler, yetenek ağacı, hoca pazarı, olay kartı
        GameRepo.tab = GameRepo.TAB_SQUAD; GameRepo.squadMode = 1; shot("17_dynamics"); GameRepo.squadMode = 0
        com.supermenajer.game.game.Perks.addXp(g, 420)
        GameRepo.upgradePerk("tactic"); GameRepo.upgradePerk("nego")
        g.traitsEarned += "wall"; g.traitsEarned += "derby"; g.traitStats["clean"] = 15; g.traitStats["derbyWin"] = 3; g.traitStats["u21"] = 34; g.traitStats["goalfest"] = 5; g.traitStats["cupWin"] = 2
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 6; shot("18_perks")
        GameRepo.editIdentity { it.crestShape = 1; it.crestPattern = 4; it.crestIcon = 3; it.kitPattern = 1; it.color2 = 0xFFFFFFFF }
        GameRepo.clubMode = 10; shot("18b_identity")
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
        com.supermenajer.game.ui.Fx.changeTvView(false); shot("12a_match_2d"); com.supermenajer.game.ui.Fx.changeTvView(true)
        run {
            val sim = GameRepo.match!!
            paparazzi.snapshot("12c_match_tactics") {
                AppTheme {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.supermenajer.game.ui.Bg)) {
                        com.supermenajer.game.ui.AdviceBanner(
                            com.supermenajer.game.engine.MatchAdvice.Tip("attack", "Gerideyiz hocam. Hücuma geçmenin zamanı.", "Hücum"),
                            Modifier.padding(8.dp), {}, {},
                        )
                        com.supermenajer.game.ui.MatchTacticsDialog(sim, sim.userSide!!) {}
                    }
                }
            }
        }
        // Düdük çaldı: oyuncular tokalaşıp tünele yürür
        run {
            val an = GameRepo.pitchFor(GameRepo.match!!)
            an.play(com.supermenajer.game.engine.MatchEvent(90, com.supermenajer.game.engine.Ev.FULL, -1, "Bitti"))
            repeat(30 * 6) { an.update(1f / 30f) }
        }
        shot("12b_walkoff")
        GameRepo.finishMatch()
        shot("13_summary")
        GameRepo.closeSummary()
        // gün atlama kartı
        while (g.todayUserFixture() != null && !g.seasonOver && !g.fired) advance(1)
        GameRepo.tab = 0; shot("14_restday")
        GameRepo.newspaperOpen = true
        shot("14b_newspaper")
        GameRepo.newspaperOpen = false
        run {
            val g0 = GameRepo.game!!
            var k = 0
            while (com.supermenajer.game.game.MonthlyAwards.latest(g0).isEmpty() && !g0.seasonOver && k++ < 60) advance(1)
            g0.monthAwardsNew = true
            GameRepo.tab = 0
            shot("14c_month_awards")
            g0.monthAwardsNew = false
        }
        while (!g.seasonOver && !g.fired) advance(1)
        GameRepo.tab = 0; shot("15_seasonend")
        GameRepo.awardsOpen = true
        shot("15b_awards")
        GameRepo.awardsOpen = false
        run {
            val g0 = GameRepo.game!!
            g0.trophies += "🏆 ${g0.season} ${g0.leagueName(g0.userTeam)} şampiyonu"
            g0.ceremonySeen = g0.trophies.size - 1
            shot("15c_trophy")
            g0.ceremonySeen = 999
        }
        GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 7
        shot("15d_analysis")
        GameRepo.leagueTab = 0; GameRepo.tab = GameRepo.TAB_HOME
        GameRepo.startNewSeason()
        GameRepo.tab = 3; GameRepo.leagueTab = 5; shot("16_history")
        GameRepo.leagueTab = 6; shot("16b_calendar")
        GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 9; shot("16c_vision"); GameRepo.clubMode = 0
        // Rakip hocanın laf sokması (basın kartı)
        run {
            val opp = g.divisionTeams(g.userTeam.division).first { it.id != g.userTeamId }
            g.press = com.supermenajer.game.model.PressQuestion("${opp.name} hocası ${g.managers[opp.id]}: \"Onların hocası çok konuşuyor; biz konuşmayı sahaya bırakıyoruz.\" Ne cevap vereceksin?", com.supermenajer.game.game.Rivals.answers, com.supermenajer.game.game.Rivals.PRESS_KIND)
            g.pendingEvent = null
            GameRepo.tab = 0; shot("16d_rival_press"); g.press = null
        }
        GameRepo.editorOpen = true; shot("16e_editor"); GameRepo.editorOpen = false
        GameRepo.flush()
        GameRepo.screen = Screen.TITLE; shot("00_title")
        // İki kişilik maç
        val vw = Generator.newWorld().also { Season.computeExpectations(it) }
        val top = vw.divisionTeams(0).sortedBy { it.expected }
        GameRepo.startVersus(vw, top[0].id, top[1].id)
        run {
            val sim = GameRepo.match!!
            repeat(25) { sim.step() }
            repeat(40) { GameRepo.pitchFor(sim).update(1f / 30f) }
        }
        shot("17_versus")
        GameRepo.finishMatch()
        shot("17b_versus_summary")
        GameRepo.closeSummary()
    }
}
