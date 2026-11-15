package com.supermenajer.game.ui

import androidx.compose.foundation.verticalScroll

import androidx.compose.animation.togetherWith

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.Europe
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.PressQuestion
import com.supermenajer.game.model.Team
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.IncomingOffer
import com.supermenajer.game.model.Game

@Composable
fun MainScreen() {
    GameRepo.version
    val g = GameRepo.game ?: return
    BackHandler {
        if (GameRepo.tab != GameRepo.TAB_HOME) GameRepo.tab = GameRepo.TAB_HOME else GameRepo.settingsOpen = true
    }
    var menu by remember { mutableStateOf(false) }
    BackHandler(enabled = menu) { menu = false }
    // İlk kez açılan sekme için rehber ipucu
    androidx.compose.runtime.LaunchedEffect(GameRepo.tab, g.introDone) {
        if (g.introDone) Tips.show(listOf("home", "squad", "tactics", "league", "transfer", "inbox", "club", "").getOrElse(GameRepo.tab) { "" })
    }
    Box(Modifier.fillMaxSize()) {
        // Ana sayfada arkada canlı stadyum
        androidx.compose.animation.AnimatedVisibility(
            GameRepo.tab == GameRepo.TAB_HOME,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(500)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
        ) {
            StadiumBackdrop(Color(g.userTeam.color1), Color(g.userTeam.color2), Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize()) {
            SmTopBar(g) { menu = true }
            if (GameRepo.tab == GameRepo.TAB_HOME) QuickRow(g)
            androidx.compose.animation.AnimatedContent(
                GameRepo.tab, Modifier.weight(1f),
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260, delayMillis = 60)) +
                        androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300)) { it / 30 })
                        .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))
                },
                label = "tab",
            ) { tab ->
                when (tab) {
                    GameRepo.TAB_HOME -> HomeDashboard(g)
                    GameRepo.TAB_SQUAD -> SquadTab(g)
                    GameRepo.TAB_TACTICS -> TacticsTab(g)
                    GameRepo.TAB_LEAGUE -> LeagueTab(g)
                    GameRepo.TAB_TRANSFER -> TransferTab(g)
                    GameRepo.TAB_INBOX -> InboxTab(g)
                    GameRepo.TAB_CLUB -> ClubTab(g)
                    GameRepo.TAB_NATIONAL -> NationalTab(g)
                }
            }
        }
        if (menu) MenuDrawer(g) { menu = false }
        if (!g.introDone) CareerIntro(g)
    }
    g.teamOrNull(GameRepo.openTeam)?.let { TeamProfileDialog(g, it) }
    GameRepo.selectedPlayer?.let { PlayerDialog(g, it) }
    GameRepo.compare?.let { (a, b) ->
        val pa = g.player(a)
        val pb = g.player(b)
        if (pa != null && pb != null) CompareDialog(g, pa, pb) else GameRepo.compare = null
    }
    GameRepo.negotiation?.let { NegotiationDialog(g, it) }
    if (GameRepo.settingsOpen) SettingsDialog()
    if (GameRepo.editorOpen) EditorDialog(g)
    if (GameRepo.newspaperOpen) NewspaperDialog(g)
    if (GameRepo.awardsOpen) AwardsNightDialog(g)
    TrophyCeremonyIfNeeded(g)
}

/** Ayarlar: kaydet, ana menü, oyundan çık. */
@Composable
private fun SettingsDialog() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var confirm by remember { mutableStateOf(0) } // 1 ana menü, 2 çıkış
    var saved by remember { mutableStateOf(false) }
    WideDialog(onDismissRequest = { GameRepo.settingsOpen = false }, maxWidth = 420.dp) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(18.dp),
        ) {
            Text(L("⚙️ Ayarlar", "⚙️ Settings"), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            when (confirm) {
                0 -> {
                    SoundSettings()
                    Spacer(Modifier.height(8.dp))
                    // Dil seçimi (her iki dilde de anlaşılsın diye sabit metin)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌐 Dil / Language", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        for ((code, label) in listOf(false to "Türkçe", true to "English")) {
                            val sel = com.supermenajer.game.game.Lang.en == code
                            Text(
                                label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) Color.Black else TextMain,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) Green else Surface2)
                                    .clickable { com.supermenajer.game.game.Lang.set(code) }.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                    Text(L("Bazı adlar (ligler, roller) oyun yeniden açılınca değişir. Kayıttaki eski haberler yazıldıkları dilde kalır.", "Some names (leagues, roles) change after restarting the game. Old news in your save stays in the language it was written in."), color = TextDim, fontSize = 10.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { GameRepo.save(force = true); saved = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (saved) L("✓ Kaydedildi", "✓ Saved") else L("💾 Oyunu kaydet", "💾 Save game"))
                    }
                    OutlinedButton(onClick = { GameRepo.settingsOpen = false; GameRepo.editorOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(L("🛠 Veri editörü", "🛠 Data editor")) }
                    OutlinedButton(onClick = { Tips.reset(); GameRepo.settingsOpen = false }, modifier = Modifier.fillMaxWidth()) { Text(L("💡 İpuçlarını yeniden göster", "💡 Show tips again")) }
                    Spacer(Modifier.height(6.dp))
                    Text(L("🤖 TEST MODU", "🤖 TEST MODE"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(L("Yapay zekâ senin yerine oynar (maçlar, transferler, kararlar) ve oyunu denetler; bitince gelen kutuna rapor bırakır. Devam düğmesiyle durdurabilirsin.", "The AI plays for you (matches, transfers, decisions) and checks the game; when it's done it leaves a report in your inbox. Stop it with the continue button."), color = TextDim, fontSize = 11.sp, lineHeight = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { GameRepo.settingsOpen = false; GameRepo.autoPlay(7) }, modifier = Modifier.weight(1f)) { Text(L("1 hafta", "1 week")) }
                        OutlinedButton(onClick = { GameRepo.settingsOpen = false; GameRepo.autoPlay(30) }, modifier = Modifier.weight(1f)) { Text(L("1 ay", "1 month")) }
                        OutlinedButton(onClick = { GameRepo.settingsOpen = false; GameRepo.autoPlay(0) }, modifier = Modifier.weight(1f)) { Text(L("Sezon sonu", "End of season")) }
                    }
                    OutlinedButton(onClick = { confirm = 1 }, modifier = Modifier.fillMaxWidth()) { Text(L("🏠 Ana menüye dön", "🏠 Back to main menu")) }
                    OutlinedButton(onClick = { confirm = 2 }, modifier = Modifier.fillMaxWidth()) { Text(L("🚪 Oyundan çık", "🚪 Quit game"), color = Red) }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { GameRepo.settingsOpen = false }, modifier = Modifier.fillMaxWidth()) { Text(L("Oyuna dön", "Back to game")) }
                    Text(L("İlerlemen her gün sonunda otomatik kaydedilir.", "Your progress is saved automatically at the end of every day."), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        L("Ses kaynakları: tribün ve düdük kayıtları Romainbehar ve SpliceSound (CC0); gol kaydı Work With Sounds / ", "Sound sources: crowd and whistle recordings by Romainbehar and SpliceSound (CC0); goal recording by Work With Sounds / ") +
                            L("Torsten Nilsson (CC BY 4.0); müzik \"Carefree\" Kevin MacLeod, incompetech.com (CC BY 3.0).", "Torsten Nilsson (CC BY 4.0); music \"Carefree\" Kevin MacLeod, incompetech.com (CC BY 3.0)."),
                        color = TextDim.copy(alpha = 0.7f), fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
                else -> {
                    Text(if (confirm == 1) L("Ana menüye dönülsün mü?", "Return to the main menu?") else L("Oyundan çıkılsın mı?", "Quit the game?"), fontWeight = FontWeight.Bold)
                    Text(L("Oyun kaydedilecek.", "The game will be saved."), color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirm = 0 }, modifier = Modifier.weight(1f)) { Text(L("Vazgeç", "Cancel")) }
                        Button(
                            onClick = {
                                GameRepo.advancing = false
                                GameRepo.save(force = true)
                                GameRepo.settingsOpen = false
                                if (confirm == 1) GameRepo.closeCareer()
                                else { GameRepo.flush(); (ctx as? android.app.Activity)?.finishAndRemoveTask() }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(L("Evet", "Yes")) }
                    }
                }
            }
        }
    }
}

/** Kulübün durumu tek bakışta: hedef, yönetim, taraftar, soyunma odası, form. */
@Composable
private fun ClubStatusCard(g: Game) {
    val t = g.userTeam
    val pos = g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    val (status, col) = when {
        t.played < 4 -> L("Sezon yeni başladı", "Season just started") to TextDim
        pos <= target -> L("Hedef yolunda ✓", "On track ✓") to Green
        pos <= target + 3 -> L("Hedefin biraz gerisinde", "Slightly behind target") to Gold
        else -> L("Yönetim endişeli!", "The board is worried!") to Red
    }
    SectionCard(L("Kulübün durumu", "Club status")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("Hedef: ${Season.objectiveText(t)}", "Objective: ${Season.objectiveText(t)}"), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, color = col, fontSize = 12.sp)
            }
            Text(if (t.played == 0) "–" else "$pos.", fontSize = 26.sp, fontWeight = FontWeight.Black, color = col)
        }
        Spacer(Modifier.height(8.dp))
        val c = g.confidence
        StatusBar(L("Yönetim", "Board"), c / 100, "${c.toInt()}%", when { c >= 60 -> Green; c >= 30 -> Gold; else -> Red })
        StatusBar(L("Taraftar", "Fans"), g.fans / 100, com.supermenajer.game.game.Fans.mood(g.fans), when { g.fans >= 70 -> Green; g.fans >= 40 -> Gold; else -> Red })
        StatusBar(L("Uyum", "Chemistry"), g.cohesion / 100, com.supermenajer.game.game.Dynamics.cohesionText(g.cohesion), when { g.cohesion >= 65 -> Green; g.cohesion >= 40 -> Gold; else -> Red })
        val squad = g.squad(t.id)
        val morale = if (squad.isEmpty()) 60.0 else squad.sumOf { it.morale } / squad.size
        StatusBar(L("Soyunma odası", "Dressing room"), morale / 100, moraleText(morale), when { morale >= 65 -> Green; morale >= 45 -> Gold; else -> Red })
        if (c < 25 && !g.fired) Text(L("⚠️ Koltuğun sallanıyor!", "⚠️ Your job is at risk!"), color = Red, fontSize = 12.sp)
        if (t.form.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("Form", "Form"), color = TextDim, fontSize = 12.sp, modifier = Modifier.width(96.dp))
                FormDots(t.form)
            }
        }
    }
}

@Composable
internal fun StatusBar(label: String, value: Double, text: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(96.dp), maxLines = 1)
        Bar(value.toFloat(), color, Modifier.weight(1f), 7.dp)
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(92.dp).padding(start = 8.dp))
    }
}

/** Son mesajlar: başlıklar, okunmamışlar kırmızı nokta ile; dokununca gelen kutusu. */
@Composable
private fun LatestMessagesCard(g: Game) {
    SectionCard(L("Son mesajlar", "Latest messages") + if (g.unread > 0) L(" · ${g.unread} okunmamış", " · ${g.unread} unread") else "") {
        g.news.take(4).forEach { n ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .clickable { GameRepo.openNews(n); GameRepo.inboxCat = 0; GameRepo.tab = GameRepo.TAB_INBOX }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (n.read) Color.Transparent else Red))
                Spacer(Modifier.width(8.dp))
                Text(n.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, color = if (n.read) TextDim else TextMain)
            }
        }
        TextButton(onClick = { GameRepo.tab = GameRepo.TAB_INBOX }) { Text(L("Gelen kutusu →", "Inbox →")) }
    }
}

@Composable
private fun NextMatchCard(g: Game, f: Fixture) {
    val h = g.team(f.home)
    val a = g.team(f.away)
    val issues = g.userTeam.lineup.mapNotNull { g.player(it) }.filter { !it.available }
    val title = if (Comp.isCup(f.comp) || f.comp == Comp.EURO) {
        "${Comp.name(f.comp)} · ${Comp.roundName(f.comp, f.round)}"
    } else {
        "${Comp.name(f.comp)} · ${g.dayLabel(f.week)}"
    }
    SectionCard(L("Bugün · $title", "Today · $title")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            for ((i, t) in listOf(h, a).withIndex()) {
                if (i == 1) Text("VS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDim, modifier = Modifier.padding(horizontal = 6.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamBadge(t, 44.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(t.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(teamSubtitle(g, t), color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val note = when {
            Comp.isKnockout(f.comp, f.round) -> L("Tek maç eleme · beraberlikte penaltılar!", "Single-leg knockout · penalties if level!")
            f.comp == Comp.EURO -> L("Grup maçı · ilk 2 çıkar · galibiyet primi €600K", "Group match · top 2 go through · win bonus €600K")
            else -> null
        }
        if (note != null) Text(note, color = Gold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        if (issues.isNotEmpty()) {
            Text(
                L("⚠️ İlk 11'de oynayamayan: ${issues.joinToString { it.name }} (maçta otomatik değişir)", "⚠️ Unavailable in the starting XI: ${issues.joinToString { it.name }} (replaced automatically)"),
                color = Gold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        MatchPrep(g)
        Spacer(Modifier.height(8.dp))
        OpponentReport(g, if (f.home == g.userTeamId) a else h)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { GameRepo.playMatch(live = true) },
                modifier = Modifier.weight(1.4f).height(46.dp), shape = RoundedCornerShape(12.dp),
            ) { Text(L("▶  MAÇA ÇIK", "▶  KICK OFF"), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            OutlinedButton(
                onClick = { GameRepo.quickMatchAsync() },
                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp),
            ) { Text(L("Hızlı sonuç", "Quick result"), maxLines = 1) }
        }
    }
}

/** Maç hazırlığı: gizli antrenman ve galibiyet primi (OSM tarzı). */
@Composable
internal fun MatchPrep(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    val md = com.supermenajer.game.game.Matchday
    val nf = g.nextUserFixture()
    Column {
        Text(L("RAKİBE ÖZEL TALİMAT", "OPPOSITION INSTRUCTION"), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            listOf(L("🎯 Yıldızını tut", "🎯 Mark their star"), L("🚧 Kanatları kapat", "🚧 Close the flanks"), L("⚡ Önde baskı", "⚡ High press"), L("🏃 Arkaya koş", "🏃 Run in behind")).forEachIndexed { i, n ->
                SmallChip(n, g.oppInstr == i, Modifier.weight(1f)) { GameRepo.setOppInstr(i) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val done = nf != null && g.secretWeek == nf.week
            OutlinedButton(
                onClick = { msg = GameRepo.secretTraining() }, enabled = !done,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.height(34.dp),
            ) { Text(if (done) L("🕵️ Gizli antrenman ✓", "🕵️ Closed training ✓") else L("🕵️ Gizli antrenman ${money(md.secretCost(g))}", "🕵️ Closed training ${money(md.secretCost(g))}"), fontSize = 12.sp, maxLines = 1) }
            Text(L("Prim:", "Bonus:"), color = TextDim, fontSize = 11.sp)
            listOf(L("Yok", "None"), L("Normal", "Normal"), L("Yüksek", "High")).forEachIndexed { i, n ->
                Text(
                    n, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (g.userTeam.winBonus == i) Color.Black else TextMain,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (g.userTeam.winBonus == i) Gold else Surface2)
                        .clickable { GameRepo.setWinBonus(i) }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        val wb = g.userTeam.winBonus
        Text(
            msg ?: if (wb > 0) L("Galibiyette oyunculara ${money(md.bonusCost(g, wb))} prim · maçta +%${wb} motivasyon", "${money(md.bonusCost(g, wb))} bonus to players for a win · +%${wb} motivation in the match") else L("Galibiyet primi motivasyonu artırır; sadece kazanınca ödenir.", "A win bonus boosts motivation; it's only paid if you win."),
            color = if (msg != null) Gold else TextDim, fontSize = 11.sp, maxLines = 2,
        )
    }
}

@Composable
private fun RestDayCard(g: Game) {
    val d = g.today
    val cal = com.supermenajer.game.game.GameCalendar
    SectionCard(if (g.date > 0) cal.format(g.date) else g.dayLabel(g.week)) {
        Text(
            when {
                !g.isMatchDay && g.week == 1 -> L("Hazırlık dönemi. Transferlerini tamamla, taktiğini hazırla!", "Pre-season. Finish your transfers and prepare your tactics!")
                !g.isMatchDay -> L("Bugün maç yok. Antrenman günü.", "No match today. Training day.")
                d?.type == 1 -> L("Bu kupa turunda maçımız yok.", "We have no match in this cup round.")
                d?.type == 2 -> L("Bu Avrupa gecesinde maçımız yok.", "We have no match on this European night.")
                else -> L("Bugün maçımız yok.", "We have no match today.")
            },
            fontSize = 15.sp,
        )
        g.nextUserFixture()?.let { nf ->
            val opp = g.team(if (nf.home == g.userTeamId) nf.away else nf.home)
            Spacer(Modifier.height(4.dp))
            val left = if (g.date > 0) cal.daysUntil(g, nf.week) else -1
            val whenText = if (left > 0) L(" · $left gün sonra (${cal.format(cal.dateOf(g, nf.week), false)})", " · in $left days (${cal.format(cal.dateOf(g, nf.week), false)})") else " · ${g.dayLabel(nf.week)}"
            Text(L("Sıradaki maç: ${opp.name}$whenText", "Next match: ${opp.name}$whenText"), color = TextDim, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { GameRepo.continueGame() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                Text(if (GameRepo.advancing) L("⏸ DURDUR", "⏸ STOP") else L("▶ GÜN GÜN", "▶ DAY BY DAY"), fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(onClick = { GameRepo.skipToNextMatchAsync() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                Text(L("⏩ Maça atla", "⏩ Skip to match"), maxLines = 1)
            }
        }
        Text(L("DEVAM günleri tek tek geçer; transfer teklifi, sakatlık gibi önemli bir olayda durur.", "CONTINUE moves day by day and stops on important events such as transfer offers or injuries."), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        if (com.supermenajer.game.game.Matchday.canFriendly(g)) {
            var fr by remember { mutableStateOf<String?>(null) }
            OutlinedButton(onClick = { fr = GameRepo.friendly() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(L("🤝 Hazırlık maçı oyna (${g.friendlies}/3)", "🤝 Play a friendly (${g.friendlies}/3)"))
            }
            fr?.let { Text(L("Sonuç: $it", "Result: $it"), color = Gold, fontSize = 12.sp) }
        }
        if (g.nextUserFixture() != null) {
            Spacer(Modifier.height(6.dp))
            MatchPrep(g)
        }
    }
}

@Composable
internal fun FiredCard(g: Game) {
    SectionCard(L("Kovuldun!", "You're sacked!")) {
        Text(L("😞 Yönetim seninle yolları ayırdı.", "😞 The board has parted ways with you."), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(L("Ama futbol dünyası seni unutmadı. Şu kulüpler teknik direktör arıyor:", "But the football world hasn't forgotten you. These clubs are looking for a manager:"), color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        g.jobOffers.map { g.team(it) }.forEach { t ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamBadge(t, 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontWeight = FontWeight.Bold)
                    Text(L("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · ${posText(g, t)} sıra · ${money(t.budget)}", "${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · ${posText(g, t)} place · ${money(t.budget)}"), color = TextDim, fontSize = 12.sp)
                }
                Button(onClick = { GameRepo.takeJob(t.id) }) { Text(L("İmzala", "Sign")) }
            }
        }
    }
}

@Composable
internal fun IncomingOffersCard(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    SectionCard(L("Gelen transfer teklifleri", "Incoming transfer offers")) {
        msg?.let { Text(it, color = Gold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
        g.offers.toList().forEachIndexed { i, o: IncomingOffer ->
            val p = g.player(o.playerId) ?: return@forEachIndexed
            val buyer = g.team(o.teamId)
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(buyer, 30.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${buyer.name} → ${p.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(L("Teklif: ${money(o.price)} · Değeri: ${money(p.value)} · Güç ${p.overall}", "Offer: ${money(o.price)} · Value: ${money(p.value)} · Overall ${p.overall}"), color = TextDim, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { msg = GameRepo.acceptIncoming(o) }, modifier = Modifier.weight(1f)) { Text(L("Sat", "Sell"), fontSize = 13.sp) }
                OutlinedButton(onClick = { msg = GameRepo.counterIncoming(o) }, modifier = Modifier.weight(1f)) { Text(L("+%20 iste", "Ask +20%"), fontSize = 13.sp) }
                OutlinedButton(onClick = { GameRepo.rejectIncoming(o); msg = null }, modifier = Modifier.weight(1f)) { Text(L("Reddet", "Reject"), fontSize = 13.sp) }
            }
        }
    }
}

@Composable
internal fun SeasonEndCard(g: Game) {
    val rec = g.history.lastOrNull()
    val t = g.userTeam
    val pos = rec?.userPos ?: g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    SectionCard(L("Sezon sonu", "End of season")) {
        Text(L("🏆 ${rec?.leagueName?.ifEmpty { null } ?: Comp.name(t.division)} şampiyonu: ${rec?.champion ?: "-"}", "🏆 ${rec?.leagueName?.ifEmpty { null } ?: Comp.name(t.division)} champions: ${rec?.champion ?: "-"}"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (!rec?.cupWinner.isNullOrEmpty()) Text("🥇 ${Comp.name(Leagues.of(t.division)?.cupComp ?: Comp.CUP)}: ${rec?.cupWinner}", fontSize = 15.sp)
        if (!rec?.euroWinner.isNullOrEmpty()) Text(L("🌍 Avrupa Kupası: ${rec?.euroWinner}", "🌍 European Cup: ${rec?.euroWinner}"), fontSize = 15.sp)
        if (!rec?.firstChampion.isNullOrEmpty()) Text(L("Alt lig şampiyonu: ${rec?.firstChampion}", "Second-tier champions: ${rec?.firstChampion}"), color = TextDim, fontSize = 14.sp)
        Text(L("Gol kralı: ${rec?.topScorer ?: "-"}", "Top scorer: ${rec?.topScorer ?: "-"}"), color = TextDim, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.pressClick(0.96f) { GameRepo.awardsOpen = true }.clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFF7E08A), Gold))).padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text(L("🏆 Ödül Gecesi", "🏆 Awards Night"), color = Color(0xFF2A1E00), fontWeight = FontWeight.Black, fontSize = 14.sp) }
        Spacer(Modifier.height(12.dp))
        Text(L("${t.name} ${Comp.name(t.division)}'i $pos. sırada bitirdi.", "${t.name} finished $pos. in ${Comp.name(t.division)}."), fontSize = 16.sp)
        val lg = Leagues.of(t.division)
        if (lg != null && lg.tier == 1 && Leagues.second(lg.country) != null && pos >= 16) Text(L("⬇️ Küme düştük...", "⬇️ We were relegated..."), color = Red, fontWeight = FontWeight.Bold)
        if (lg != null && lg.tier == 2 && pos <= 3) Text(L("⬆️ ${Leagues.top(lg.country).name.uppercase()}'NE YÜKSELDİK!", "⬆️ PROMOTED TO ${Leagues.top(lg.country).name.uppercase()}!"), color = Gold, fontWeight = FontWeight.Bold)
        val (msg, col) = when {
            pos == 1 -> L("Efsane sezon! Taraftar seni omuzlarda taşıyor! 🎉", "A legendary season! The fans are carrying you on their shoulders! 🎉") to Gold
            pos <= target -> L("Yönetim hedefi (${Season.objectiveText(t)}) tuttu. Başkan çok memnun! 👏", "Board objective (${Season.objectiveText(t)}) achieved. The chairman is delighted! 👏") to Green
            pos <= target + 3 -> L("Hedefin biraz gerisinde kaldık. Yönetim yeni sezonda daha fazlasını bekliyor.", "We fell slightly short of the target. The board expects more next season.") to Gold
            else -> L("Hedefin çok gerisinde kaldık. Yönetim sabırsızlanıyor... 😬", "We fell well short of the target. The board is growing impatient... 😬") to Red
        }
        Spacer(Modifier.height(6.dp))
        Text(msg, color = col)
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(10.dp))
        ReportLines(t.lastReport)
        val expiring = g.squad(t.id).filter { it.contractYears == 1 }
        if (expiring.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(L("📝 Sözleşmesi biten ve yeni sezonda ayrılacak oyuncular:", "📝 Players whose contracts end and who leave next season:"), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            expiring.forEach { p ->
                Text(L("• ${p.name} (${com.supermenajer.game.game.Positions.det(p).code}, ${p.overall}) · uzatmak için dokun", "• ${p.name} (${com.supermenajer.game.game.Positions.det(p).code}, ${p.overall}) · tap to extend"), fontSize = 13.sp,
                    modifier = Modifier.clickable { GameRepo.selectedPlayer = p }.padding(vertical = 3.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(L("Sezon arası transfer dönemi açık: yeni sezondan önce kadronu güçlendirebilirsin.", "The close-season transfer window is open: strengthen your squad before the new season."), color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        val T = com.supermenajer.game.game.Tournament
        if (g.natPending.isNotEmpty() || T.needed(g)) {
            Button(
                onClick = { GameRepo.continueGame() },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) { Text(if (T.needed(g)) L("🏆 ${T.title(g).uppercase()}'NA BAŞLA", "🏆 START ${T.title(g).uppercase()}") else L("🏆 TURNUVA MAÇINA ÇIK", "🏆 PLAY TOURNAMENT MATCH"), fontWeight = FontWeight.Bold, color = Color.Black) }
        } else {
            Button(
                onClick = { GameRepo.startNewSeason() },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
            ) { Text(L("YENİ SEZONA BAŞLA (${g.season + 1})", "START NEW SEASON (${g.season + 1})"), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun FansCard(g: Game) {
    val f = g.fans
    val col = when { f >= 70 -> Green; f >= 40 -> Gold; else -> Red }
    SectionCard(L("📣 Taraftar", "📣 Fans")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(com.supermenajer.game.game.Fans.mood(f), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${f.toInt()}%", color = col, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.height(6.dp))
        Bar((f / 100).toFloat(), col, Modifier.fillMaxWidth(), 8.dp)
        Spacer(Modifier.height(6.dp))
        val effect = when {
            f >= 75 -> L("Tribünler dolu, evde 12. adam desteği var! Sponsorlar ilgili.", "The stands are full, the 12th man is behind us at home! Sponsors are interested.")
            f >= 40 -> L("Taraftar sonuçları izliyor. Galibiyetler ve derbiler moralini yükseltir.", "The fans are watching the results. Wins and derbies lift their spirits.")
            else -> L("Seyirci azalıyor, yönetim baskı altında. Derbi kazanmak iyi gelir!", "Attendances are falling and the board is under pressure. Winning a derby would help!")
        }
        Text(effect, color = TextDim, fontSize = 12.sp)
        if (g.fanLog.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            g.fanLog.take(4).forEach { line ->
                Text(line, fontSize = 12.sp, color = if (line.startsWith("+")) Green else Red, maxLines = 1)
            }
        }
    }
}

@Composable
fun BoardCard(g: Game) {
    val t = g.userTeam
    val pos = g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    val (status, col) = when {
        t.played == 0 -> L("Sezon başlamadı", "Season not started") to TextDim
        pos <= target -> L("Hedef yolunda ✓", "On track ✓") to Green
        pos <= target + 3 -> L("Hedefin biraz gerisinde", "Slightly behind target") to Gold
        else -> L("Yönetim endişeli!", "The board is worried!") to Red
    }
    SectionCard(L("Yönetim", "Board")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("Hedef: ${Season.objectiveText(t)}", "Objective: ${Season.objectiveText(t)}"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(status, color = col, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (t.played == 0) "–" else "$pos.", fontSize = 28.sp, fontWeight = FontWeight.Black, color = col)
                Text(L("sıra", "place"), color = TextDim, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        val c = g.confidence
        val cc = when { c >= 60 -> Green; c >= 30 -> Gold; else -> Red }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("Yönetim güveni", "Board confidence"), color = TextDim, fontSize = 13.sp, modifier = Modifier.width(110.dp))
            Bar((c / 100).toFloat(), cc, Modifier.weight(1f), 8.dp)
            Text(" ${c.toInt()}%", color = cc, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (c < 25 && !g.fired) Text(L("⚠️ Koltuğun sallanıyor! Sonuçlar düzelmezse kovulabilirsin.", "⚠️ Your job is at risk! You could be sacked if results don't improve."), color = Red, fontSize = 12.sp)
        if (t.form.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("Form: ", "Form: "), color = TextDim, fontSize = 13.sp)
                FormDots(t.form)
            }
        }
    }
}

@Composable
private fun MiniTable(g: Game) {
    val table = g.table(g.userTeam.division)
    val ui = table.indexOfFirst { it.id == g.userTeamId }
    val rows = (table.indices.take(5) + listOf(ui - 1, ui, ui + 1)).filter { it in table.indices }.distinct().sorted()
    SectionCard(L("Puan durumu · ${Comp.name(g.userTeam.division)}", "Table · ${Comp.name(g.userTeam.division)}")) {
        var prev = -1
        rows.forEach { i ->
            if (prev >= 0 && i != prev + 1) Text("···", color = TextDim, modifier = Modifier.padding(start = 8.dp))
            prev = i
            val t = table[i]
            val me = t.id == g.userTeamId
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (me) Green.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}", modifier = Modifier.width(24.dp), color = TextDim, fontSize = 13.sp)
                TeamBadge(t, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(t.name, modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (me) FontWeight.Bold else FontWeight.Normal)
                Text("${t.played}", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${t.points}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { GameRepo.tab = 3 }) { Text(L("Tüm tablo →", "Full table →")) }
    }
}

@Composable
private fun RecentResults(g: Game) {
    val res = g.fixtures.filter { it.played && it.involves(g.userTeamId) }.sortedBy { it.week }.takeLast(5).reversed()
    if (res.isEmpty()) return
    SectionCard(L("Son maçlar", "Recent matches")) {
        res.forEachIndexed { i, f ->
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
            if (Comp.isCup(f.comp) || f.comp == Comp.EURO) Text("${Comp.name(f.comp)} · ${Comp.roundName(f.comp, f.round)}", color = TextDim, fontSize = 11.sp)
            ResultRow(g, f, g.userTeamId)
        }
    }
}

@Composable
fun ResultRow(g: Game, f: Fixture, highlightTeam: Int = -1) {
    val h = g.team(f.home)
    val a = g.team(f.away)
    val involved = f.involves(highlightTeam)
    val col = when {
        !involved || !f.played -> Surface2
        Comp.isKnockout(f.comp, f.round) -> if (f.winner == highlightTeam) Green else Red
        else -> {
            val ug = if (f.home == highlightTeam) f.hg else f.ag
            val og = if (f.home == highlightTeam) f.ag else f.hg
            if (ug > og) Green else if (ug == og) Gold else Red
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.name, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (h.id == highlightTeam || f.winner == h.id) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.width(6.dp))
        TeamBadge(h, 20.dp)
        Column(Modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(col).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(if (f.played) "${f.hg} - ${f.ag}" else "  -  ", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = if (col == Surface2) TextMain else Color.Black)
            }
            if (f.pen.isNotEmpty()) Text("P ${f.pen}", fontSize = 10.sp, color = TextDim)
        }
        TeamBadge(a, 20.dp)
        Spacer(Modifier.width(6.dp))
        Text(a.name, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (a.id == highlightTeam || f.winner == a.id) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun NewsCard(g: Game) {
    SectionCard(L("Haberler", "News")) {
        g.news.take(12).forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))
            Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(n.body, color = TextDim, fontSize = 13.sp)
            Text("${n.season} · ${g.dayLabel(n.week).ifEmpty { "Sezon arası" }}", color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

/** Avrupa grubu puan tablosu. */
@Composable
fun EuroGroupCard(g: Game, group: Int) {
    SectionCard(L("Avrupa Kupası · Grup ${Europe.groupLetter(group)}", "European Cup · Group ${Europe.groupLetter(group)}")) {
        Row(Modifier.padding(horizontal = 6.dp)) {
            Text(L("Takım", "Team"), Modifier.weight(1f), color = TextDim, fontSize = 11.sp)
            listOf("O", "G", "B", "M", "AV", "P").forEach { Text(it, Modifier.width(26.dp), color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center) }
        }
        Europe.groupTable(g, group).forEachIndexed { i, r ->
            val me = r.team.id == g.userTeamId
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (me) Green.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(18.dp).background(if (i < 2) Blue else Color.Transparent))
                Spacer(Modifier.width(5.dp))
                TeamBadge(r.team, 22.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    Generator.flag(g.flagOf(r.team)) + " " + r.team.name,
                    Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
                )
                listOf(r.p, r.w, r.d, r.l, r.gd).forEach { Text("$it", Modifier.width(26.dp), fontSize = 12.sp, color = TextDim, textAlign = TextAlign.Center) }
                Text("${r.pts}", Modifier.width(26.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Gelir/gider kalemleri. */
@Composable
fun ReportLines(report: Map<String, Long>) {
    if (report.isEmpty()) return
    val order = listOf(Economy.PRIZE, Economy.BONUS, Economy.BOARD, Economy.EURO, Economy.CUP, Economy.TICKETS, Economy.SALES, Economy.BUYS, Economy.SCOUTING)
    report.entries.sortedBy { order.indexOf(it.key).let { i -> if (i < 0) 99 else i } }.forEach { (k, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(k, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text((if (v > 0) "+" else "") + money(v), color = if (v >= 0) Green else Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
    val total = report.values.sum()
    Row(Modifier.fillMaxWidth()) {
        Text(L("Net", "Net"), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text((if (total > 0) "+" else "") + money(total), color = if (total >= 0) Green else Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun FinanceCard(g: Game) {
    val t = g.userTeam
    val wages = Economy.squadWages(g, t.id)
    var showLast by remember { mutableStateOf(false) }
    SectionCard(L("Finans", "Finances")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("Transfer bütçesi", "Transfer budget"), color = TextDim, fontSize = 12.sp)
                Text(money(t.budget), color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(L("Haftalık maaş", "Weekly wages"), color = TextDim, fontSize = 12.sp)
                Text("${money(wages)} / ${money(t.wageBudget)}", color = if (wages > t.wageBudget) Red else TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Bar((wages.toFloat() / t.wageBudget.coerceAtLeast(1)).coerceIn(0f, 1f), if (wages > t.wageBudget) Red else if (wages > t.wageBudget * 0.9) Gold else Green, Modifier.fillMaxWidth(), 8.dp)
        if (wages > t.wageBudget) Text(L("Maaş limiti aşıldı! Yeni oyuncu alamazsın; önce satış yap ya da sözleşmesi bitenleri bırak.", "Wage limit exceeded! You can't sign new players; sell first or release players whose contracts are ending."), color = Red, fontSize = 12.sp)
        val expiring = g.squad(t.id).count { it.contractYears == 1 }
        if (expiring > 0) {
            Spacer(Modifier.height(6.dp))
            Text(L("📝 $expiring oyuncunun sözleşmesi sezon sonunda bitiyor. Kadro ekranından uzatabilirsin.", "📝 $expiring players' contracts end this season. You can extend them from the squad screen."), color = Gold, fontSize = 12.sp,
                modifier = Modifier.clickable { GameRepo.tab = 1 })
        }
        Spacer(Modifier.height(10.dp))
        Text(if (showLast) L("GEÇEN SEZON MALİ RAPOR", "LAST SEASON'S FINANCIAL REPORT") else L("BU SEZON", "THIS SEASON"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        val rep = if (showLast) t.lastReport else t.income
        if (rep.isEmpty()) Text(L("Henüz kayıt yok.", "No entries yet."), color = TextDim, fontSize = 12.sp) else ReportLines(rep)
        Spacer(Modifier.height(4.dp))
        Text(
            L("Sezon sonunda yönetim; lig sıranıza (şampiyona €20M), hedef primine ve kulüp itibarına göre bütçe ayırır. Başarı itibarı ve maaş limitini de büyütür.", "At the end of the season the board allocates a budget based on your league position (€20M for the champions), objective bonus and club reputation. Success also raises reputation and the wage limit."),
            color = TextDim, fontSize = 11.sp,
        )
        if (t.lastReport.isNotEmpty()) TextButton(onClick = { showLast = !showLast }) { Text(if (showLast) L("Bu sezonu göster", "Show this season") else L("Geçen sezonun raporu →", "Last season's report →")) }
    }
}

fun posText(g: Game, t: Team): String = if (t.played == 0) "–" else "${g.position(t.id)}."

/** Takımın altında gösterilen kısa bilgi: yabancıysa bayrak + lig, değilse lig + sıra. */
fun teamSubtitle(g: Game, t: Team): String =
    if (g.isForeign(t)) "${Generator.flag(g.flagOf(t))} ${g.leagueName(t)}"
    else L("${g.leagueName(t)} · ${posText(g, t)} sıra", "${g.leagueName(t)} · ${posText(g, t)} place")

@Composable
internal fun PressCard(g: Game, q: PressQuestion) {
    var result by remember(q) { mutableStateOf<String?>(null) }
    SectionCard(L("🎤 Basın toplantısı", "🎤 Press conference")) {
        Text(q.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        q.answers.forEachIndexed { i, a ->
            OutlinedButton(
                onClick = { result = GameRepo.answerPress(q, i) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
            ) { Text(a, fontSize = 13.sp, textAlign = TextAlign.Center) }
        }
        TextButton(onClick = { GameRepo.skipPress() }) { Text(L("Yorum yapmadan geç", "Skip without comment"), color = TextDim) }
    }
}

@Composable
internal fun CareerOfferCard(g: Game) {
    SectionCard(L("💼 Kariyer teklifi", "💼 Job offer")) {
        Text(L("Başarın büyük kulüplerin dikkatini çekti! Kabul edersen hemen yeni takımının başına geçersin.", "Your success has caught the eye of big clubs! If you accept you take charge of your new team immediately."), fontSize = 13.sp, color = TextDim)
        Spacer(Modifier.height(8.dp))
        g.jobOffers.mapNotNull { g.teamOrNull(it) }.forEach { t ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamBadge(t, 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontWeight = FontWeight.Bold)
                    Text(L("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · Bütçe ${money(t.budget)}", "${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · Budget ${money(t.budget)}"), color = TextDim, fontSize = 12.sp)
                }
                Button(onClick = { GameRepo.takeJob(t.id) }) { Text(L("Kabul", "Accept")) }
            }
        }
        TextButton(onClick = { GameRepo.declineCareerOffer() }) { Text(L("Reddet, kulübümde kalıyorum", "Decline, I'm staying at my club")) }
    }
}

/** Yardımcı antrenörün rakip raporu ve önerileri. */
@Composable
internal fun OpponentReport(g: Game, opp: Team) {
    val open = GameRepo.reportOpen
    val us = g.userTeam
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { GameRepo.reportOpen = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text(L("📋 Yardımcı antrenör: rakip raporu", "📋 Assistant manager: opposition report"), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(if (open) "▲" else "▼", color = TextDim)
        }
        if (!open) return@Column
        val (oa, om, od) = com.supermenajer.game.game.Lineups.lineStrength(g, opp)
        val (ua, um, ud) = com.supermenajer.game.game.Lineups.lineStrength(g, us)
        Spacer(Modifier.height(8.dp))
        Text("${opp.name}: ${opp.formation} · ${listOf("Defansif", "Dengeli", "Hücum")[opp.mentality]}" + if (opp.form.isNotEmpty()) L(" · Form ${opp.form}", " · Form ${opp.form}") else "", fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        listOf(Triple(L("Hücum", "Attack"), ua, oa), Triple(L("Orta saha", "Midfield"), um, om), Triple(L("Savunma", "Defending"), ud, od)).forEach { (n, mine, theirs) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$mine", fontWeight = FontWeight.Bold, color = if (mine >= theirs) Green else Red, modifier = Modifier.width(32.dp))
                Text(n, color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("$theirs", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }
        val danger = opp.lineup.mapNotNull { g.player(it) }.maxByOrNull { it.overall }
        val scorer = g.squad(opp.id).maxByOrNull { it.goals }?.takeIf { it.goals > 0 }
        val aq = com.supermenajer.game.game.StaffLogic.q(g, com.supermenajer.game.game.StaffLogic.ASSISTANT)
        if (aq >= 8) danger?.let { Text(L("En tehlikeli oyuncu: ${it.name} (${it.pos.short})", "Most dangerous player: ${it.name} (${it.pos.short})"), fontSize = 12.sp) }
        scorer?.let { Text(L("Golcüsü: ${it.name} (${it.goals} gol)", "Top scorer: ${it.name} (${it.goals} goals)"), fontSize = 12.sp) }
        Spacer(Modifier.height(6.dp))
        val advice = mutableListOf<String>()
        if (oa > ud + 3) advice += L("Hücumları güçlü. Defansif oynamayı ya da 5-3-2'yi düşünebilirsin.", "Their attack is strong. Consider playing defensively or a 5-3-2.")
        if (ua > od + 3) advice += L("Savunmaları zayıf. Hücum oynayıp üstlerine gitmeliyiz!", "Their defence is weak. We should attack and go at them!")
        if (om > um + 3) advice += L("Orta sahada ezilebiliriz. 4-5-1 veya 4-2-3-1 ile orta sahayı kalabalıklaştır.", "We could be overrun in midfield. Crowd it with a 4-5-1 or 4-2-3-1.")
        if (um > om + 3) advice += L("Orta saha bizde. Yüksek presle topu sürekli kazanabiliriz.", "We own the midfield. With a high press we can win the ball back constantly.")
        val tired = us.lineup.mapNotNull { g.player(it) }.filter { it.fitness < 75 }
        if (tired.isNotEmpty()) advice += L("${tired.joinToString { it.name }} yorgun (kondisyon %75 altı). Dinlendirmeyi düşün.", "${tired.joinToString { it.name }} are tired (fitness below 75%). Consider resting them.")
        val unhappy = us.lineup.mapNotNull { g.player(it) }.filter { it.morale < 30 }
        if (unhappy.isNotEmpty()) advice += L("${unhappy.joinToString { it.name }} çok mutsuz, performansı düşük olabilir.", "${unhappy.joinToString { it.name }} are very unhappy and may underperform.")
        if (advice.isEmpty()) advice += L("Güçler denk. Dengeli oynamak mantıklı hocam.", "The sides are evenly matched. A balanced approach makes sense, boss.")
        val shownAdvice = if (aq >= 12) advice else advice.take(1)
        shownAdvice.forEach { Text("🗣 $it", fontSize = 12.sp, color = Gold, modifier = Modifier.padding(vertical = 1.dp)) }
        if (aq < 12 && advice.size > 1) Text(L("Daha detaylı analiz için daha iyi bir yardımcı antrenör gerekli.", "A better assistant manager is needed for a more detailed analysis."), color = TextDim, fontSize = 11.sp)
    }
}

/** Ses ayarları: efektler, müzik ve genel ses düzeyi (cihazda saklanır). */
@Composable
fun SoundSettings() {
    val S = com.supermenajer.game.audio.Sound
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("🔊 Efektler", "🔊 Effects"), fontSize = 14.sp, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(S.sfxOn, { S.changeSfx(it) })
            Spacer(Modifier.width(18.dp))
            Text(L("🎵 Müzik", "🎵 Music"), fontSize = 14.sp, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(S.musicOn, { S.changeMusic(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("🔠 Yazı boyutu", "🔠 Text size"), fontSize = 14.sp, modifier = Modifier.weight(1f))
            listOf(L("Normal", "Normal"), L("Büyük", "Large"), L("Çok büyük", "Extra large")).forEachIndexed { i, l ->
                SmallChip(l, Fx.textSize == i, Modifier.width(92.dp).padding(start = 6.dp)) { Fx.changeTextSize(i) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("🎬 Gol tekrarı", "🎬 Goal replays"), fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(L("golden sonra ağır çekim", "slow motion after goals"), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            androidx.compose.material3.Switch(Fx.replays, { Fx.changeReplays(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("🔋 Pil tasarrufu", "🔋 Battery saver"), fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(L("sürekli dönen efektleri durdurur", "stops continuously animated effects"), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            androidx.compose.material3.Switch(Fx.lowPower, { Fx.changeLowPower(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("Ses düzeyi", "Volume"), fontSize = 14.sp, color = TextDim, modifier = Modifier.width(86.dp))
            androidx.compose.material3.Slider(
                S.volume, { S.changeVolume(it) }, Modifier.weight(1f),
                onValueChangeFinished = { S.click() },
            )
            Text("%${(S.volume * 100).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        }
    }
}

/** Uygulama düzeyi uyarılar: önceki çökmenin raporu ve yedekten kurtarılan kayıt. */
@Composable
fun SystemNotices() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val crash = com.supermenajer.game.CrashGuard.pending
    val restored = GameRepo.restoredNote
    if (crash != null) {
        WideDialog(onDismissRequest = { com.supermenajer.game.CrashGuard.dismiss() }, maxWidth = 460.dp) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(18.dp)) {
                Text(L("😓 Oyun beklenmedik şekilde kapandı", "😓 The game closed unexpectedly"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    L("Özür dileriz. İlerlemen son kayıttan devam ediyor. Hatayı düzeltebilmemiz için raporu paylaşabilirsin ", "Sorry about that. Your progress continues from the last save. You can share the report so we can fix the bug ") +
                        L("(WhatsApp, Telegram, e-posta). Raporda kişisel bilgi yok: sürüm, telefon modeli ve hatanın yeri.", "(WhatsApp, Telegram, email). The report has no personal information: version, phone model and where the error happened."),
                    color = TextDim, fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { com.supermenajer.game.CrashGuard.dismiss() }, modifier = Modifier.weight(1f)) { Text(L("Kapat", "Close")) }
                    Button(onClick = { com.supermenajer.game.CrashGuard.share(ctx) }, modifier = Modifier.weight(1f)) { Text(L("Raporu paylaş", "Share report")) }
                }
            }
        }
    } else if (restored != null) {
        WideDialog(onDismissRequest = { GameRepo.restoredNote = null }, maxWidth = 460.dp) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(18.dp)) {
                Text(L("🛟 Kayıt kurtarıldı", "🛟 Save recovered"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(restored, color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { GameRepo.restoredNote = null }, modifier = Modifier.fillMaxWidth()) { Text(L("Tamam", "OK")) }
            }
        }
    }
}
