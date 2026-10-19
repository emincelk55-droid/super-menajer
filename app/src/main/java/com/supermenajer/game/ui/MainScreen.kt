package com.supermenajer.game.ui

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

private class NavItem(val tab: Int, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val navItems = listOf(
    NavItem(GameRepo.TAB_HOME, "Ana Sayfa", Icons.Filled.Home),
    NavItem(GameRepo.TAB_INBOX, "Mesajlar", Icons.Filled.Email),
    NavItem(GameRepo.TAB_SQUAD, "Kadro", Icons.Filled.Groups),
    NavItem(GameRepo.TAB_TACTICS, "Taktik", Icons.Filled.SportsSoccer),
    NavItem(GameRepo.TAB_CLUB, "Kulüp", Icons.Filled.AccountBalance),
    NavItem(GameRepo.TAB_LEAGUE, "Lig", Icons.Filled.EmojiEvents),
    NavItem(GameRepo.TAB_TRANSFER, "Transfer", Icons.Filled.SwapHoriz),
)

@Composable
fun MainScreen() {
    GameRepo.version
    val g = GameRepo.game ?: return
    BackHandler {
        if (GameRepo.tab != GameRepo.TAB_HOME) GameRepo.tab = GameRepo.TAB_HOME else GameRepo.settingsOpen = true
    }
    Row(Modifier.fillMaxSize()) {
        NavRail(g)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            TopBar(g)
            Box(Modifier.weight(1f)) {
                when (GameRepo.tab) {
                    GameRepo.TAB_HOME -> HomeTab(g)
                    GameRepo.TAB_SQUAD -> SquadTab(g)
                    GameRepo.TAB_TACTICS -> TacticsTab(g)
                    GameRepo.TAB_LEAGUE -> LeagueTab(g)
                    GameRepo.TAB_TRANSFER -> TransferTab(g)
                    GameRepo.TAB_INBOX -> InboxTab(g)
                    GameRepo.TAB_CLUB -> ClubTab(g)
                }
            }
        }
    }
    GameRepo.selectedPlayer?.let { PlayerDialog(g, it) }
    GameRepo.negotiation?.let { NegotiationDialog(g, it) }
    if (GameRepo.settingsOpen) SettingsDialog()
}

/** Sol menü: sade, ikon + kısa etiket; mesajlarda okunmamış rozeti. */
@Composable
private fun NavRail(g: Game) {
    Column(
        Modifier.fillMaxHeight().width(68.dp).background(Surface1).padding(vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        navItems.forEach { item ->
            val sel = GameRepo.tab == item.tab
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { GameRepo.tab = item.tab }.padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (sel) Green else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = if (sel) Color.Black else TextDim, modifier = Modifier.size(20.dp))
                    }
                    val badge = if (item.tab == GameRepo.TAB_INBOX) g.unread else 0
                    if (badge > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).size(17.dp).clip(CircleShape).background(Red),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Text(item.label, fontSize = 9.sp, lineHeight = 11.sp, color = if (sel) Green else TextDim, maxLines = 1)
            }
        }
    }
}

/** Üst çubuk: geri, kulüp, tarih, para, ayarlar ve CM tarzı büyük DEVAM düğmesi. */
@Composable
private fun TopBar(g: Game) {
    val t = g.userTeam
    Row(
        Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (GameRepo.tab != GameRepo.TAB_HOME) {
            IconButton(onClick = { GameRepo.tab = GameRepo.TAB_HOME }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextMain)
            }
            Spacer(Modifier.width(4.dp))
        }
        TeamBadge(t, 30.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val wk = when {
                g.seasonOver -> "Sezon bitti"
                !g.isMatchDay -> if (g.week == 1) "Hazırlık dönemi" else "Maç yok"
                else -> g.dayLabel(g.week)
            }
            val date = if (g.date > 0) com.supermenajer.game.game.GameCalendar.format(g.date) else "${g.season}"
            Text("📅 $date · $wk", color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(t.budget), color = if (t.budget >= 0) Gold else Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val wages = Economy.squadWages(g, t.id)
            Text(
                "Maaş ${money(wages).removePrefix("€")}/${money(t.wageBudget).removePrefix("€")}",
                color = if (wages > t.wageBudget) Red else TextDim, fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { GameRepo.settingsOpen = true }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Ayarlar", tint = TextDim)
        }
        Spacer(Modifier.width(4.dp))
        Button(
            onClick = { GameRepo.continueGame() },
            enabled = !g.fired,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (GameRepo.advancing) Gold else Green, contentColor = Color.Black),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        ) { Text(GameRepo.continueLabel(g), fontWeight = FontWeight.Black, fontSize = 14.sp) }
    }
}

/** Ayarlar: kaydet, ana menü, oyundan çık. */
@Composable
private fun SettingsDialog() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var confirm by remember { mutableStateOf(0) } // 1 ana menü, 2 çıkış
    var saved by remember { mutableStateOf(false) }
    WideDialog(onDismissRequest = { GameRepo.settingsOpen = false }, maxWidth = 420.dp) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(18.dp)) {
            Text("⚙️ Ayarlar", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            when (confirm) {
                0 -> {
                    OutlinedButton(onClick = { GameRepo.save(force = true); saved = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (saved) "✓ Kaydedildi" else "💾 Oyunu kaydet")
                    }
                    OutlinedButton(onClick = { confirm = 1 }, modifier = Modifier.fillMaxWidth()) { Text("🏠 Ana menüye dön") }
                    OutlinedButton(onClick = { confirm = 2 }, modifier = Modifier.fillMaxWidth()) { Text("🚪 Oyundan çık", color = Red) }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { GameRepo.settingsOpen = false }, modifier = Modifier.fillMaxWidth()) { Text("Oyuna dön") }
                    Text("İlerlemen her gün sonunda otomatik kaydedilir.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    Text(if (confirm == 1) "Ana menüye dönülsün mü?" else "Oyundan çıkılsın mı?", fontWeight = FontWeight.Bold)
                    Text("Oyun kaydedilecek.", color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirm = 0 }, modifier = Modifier.weight(1f)) { Text("Vazgeç") }
                        Button(
                            onClick = {
                                GameRepo.save(force = true)
                                GameRepo.settingsOpen = false
                                if (confirm == 1) GameRepo.screen = Screen.TITLE
                                else { GameRepo.flush(); (ctx as? android.app.Activity)?.finishAndRemoveTask() }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Evet") }
                    }
                }
            }
        }
    }
}

/** Ana sayfa: solda bugünün işi ve yapılacaklar, sağda kulübün durumu. */
@Composable
private fun HomeTab(g: Game) {
    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(Modifier.weight(1.1f).fillMaxHeight(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                g.fired -> item { FiredCard(g) }
                g.seasonOver -> item { SeasonEndCard(g) }
                else -> {
                    val f = g.todayUserFixture()
                    item { if (f != null) NextMatchCard(g, f) else RestDayCard(g) }
                }
            }
            if (!g.fired && g.careerOffer && g.jobOffers.isNotEmpty()) item { CareerOfferCard(g) }
            g.press?.let { q -> item { PressCard(g, q) } }
            if (g.offers.isNotEmpty() && !g.fired) item { IncomingOffersCard(g) }
            if (g.sponsorOffers.isNotEmpty() && !g.fired) item { SponsorOffersCard(g) }
            if (Europe.groupOf(g, g.userTeamId) >= 0) item { EuroGroupCard(g, Europe.groupOf(g, g.userTeamId)) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ClubStatusCard(g) }
            item { LatestMessagesCard(g) }
            item { MiniTable(g) }
            item { RecentResults(g) }
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
        t.played < 4 -> "Sezon yeni başladı" to TextDim
        pos <= target -> "Hedef yolunda ✓" to Green
        pos <= target + 3 -> "Hedefin biraz gerisinde" to Gold
        else -> "Yönetim endişeli!" to Red
    }
    SectionCard("Kulübün durumu") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hedef: ${Season.objectiveText(t)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, color = col, fontSize = 12.sp)
            }
            Text(if (t.played == 0) "–" else "$pos.", fontSize = 26.sp, fontWeight = FontWeight.Black, color = col)
        }
        Spacer(Modifier.height(8.dp))
        val c = g.confidence
        StatusBar("Yönetim", c / 100, "${c.toInt()}%", when { c >= 60 -> Green; c >= 30 -> Gold; else -> Red })
        StatusBar("Taraftar", g.fans / 100, com.supermenajer.game.game.Fans.mood(g.fans), when { g.fans >= 70 -> Green; g.fans >= 40 -> Gold; else -> Red })
        val squad = g.squad(t.id)
        val morale = if (squad.isEmpty()) 60.0 else squad.sumOf { it.morale } / squad.size
        StatusBar("Soyunma odası", morale / 100, moraleText(morale), when { morale >= 65 -> Green; morale >= 45 -> Gold; else -> Red })
        if (c < 25 && !g.fired) Text("⚠️ Koltuğun sallanıyor!", color = Red, fontSize = 12.sp)
        if (t.form.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Form", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(96.dp))
                FormDots(t.form)
            }
        }
    }
}

@Composable
private fun StatusBar(label: String, value: Double, text: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(96.dp), maxLines = 1)
        Bar(value.toFloat(), color, Modifier.weight(1f), 7.dp)
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(92.dp).padding(start = 8.dp))
    }
}

/** Son mesajlar: başlıklar, okunmamışlar kırmızı nokta ile; dokununca gelen kutusu. */
@Composable
private fun LatestMessagesCard(g: Game) {
    SectionCard("Son mesajlar" + if (g.unread > 0) " · ${g.unread} okunmamış" else "") {
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
        TextButton(onClick = { GameRepo.tab = GameRepo.TAB_INBOX }) { Text("Gelen kutusu →") }
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
    SectionCard("Bugün · $title") {
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
            Comp.isKnockout(f.comp, f.round) -> "Tek maç eleme · beraberlikte penaltılar!"
            f.comp == Comp.EURO -> "Grup maçı · ilk 2 çıkar · galibiyet primi €600K"
            else -> null
        }
        if (note != null) Text(note, color = Gold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        if (issues.isNotEmpty()) {
            Text(
                "⚠️ İlk 11'de oynayamayan: ${issues.joinToString { it.name }} (maçta otomatik değişir)",
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
            ) { Text("▶  MAÇA ÇIK", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            OutlinedButton(
                onClick = { GameRepo.playMatch(live = false) },
                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp),
            ) { Text("Hızlı sonuç", maxLines = 1) }
        }
    }
}

/** Maç hazırlığı: gizli antrenman ve galibiyet primi (OSM tarzı). */
@Composable
private fun MatchPrep(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    val md = com.supermenajer.game.game.Matchday
    val nf = g.nextUserFixture()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val done = nf != null && g.secretWeek == nf.week
            OutlinedButton(
                onClick = { msg = GameRepo.secretTraining() }, enabled = !done,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.height(34.dp),
            ) { Text(if (done) "🕵️ Gizli antrenman ✓" else "🕵️ Gizli antrenman ${money(md.secretCost(g))}", fontSize = 12.sp, maxLines = 1) }
            Text("Prim:", color = TextDim, fontSize = 11.sp)
            listOf("Yok", "Normal", "Yüksek").forEachIndexed { i, n ->
                Text(
                    n, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (g.userTeam.winBonus == i) Color.Black else TextMain,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (g.userTeam.winBonus == i) Gold else Surface2)
                        .clickable { GameRepo.setWinBonus(i) }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        val wb = g.userTeam.winBonus
        Text(
            msg ?: if (wb > 0) "Galibiyette oyunculara ${money(md.bonusCost(g, wb))} prim · maçta +%${wb} motivasyon" else "Galibiyet primi motivasyonu artırır; sadece kazanınca ödenir.",
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
                !g.isMatchDay && g.week == 1 -> "Hazırlık dönemi. Transferlerini tamamla, taktiğini hazırla!"
                !g.isMatchDay -> "Bugün maç yok. Antrenman günü."
                d?.type == 1 -> "Bu kupa turunda maçımız yok."
                d?.type == 2 -> "Bu Avrupa gecesinde maçımız yok."
                else -> "Bugün maçımız yok."
            },
            fontSize = 15.sp,
        )
        g.nextUserFixture()?.let { nf ->
            val opp = g.team(if (nf.home == g.userTeamId) nf.away else nf.home)
            Spacer(Modifier.height(4.dp))
            val left = if (g.date > 0) cal.daysUntil(g, nf.week) else -1
            val whenText = if (left > 0) " · $left gün sonra (${cal.format(cal.dateOf(g, nf.week), false)})" else " · ${g.dayLabel(nf.week)}"
            Text("Sıradaki maç: ${opp.name}$whenText", color = TextDim, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { GameRepo.continueGame() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                Text(if (GameRepo.advancing) "⏸ DURDUR" else "▶ GÜN GÜN", fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(onClick = { GameRepo.skipToNextMatch() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                Text("⏩ Maça atla", maxLines = 1)
            }
        }
        Text("DEVAM günleri tek tek geçer; transfer teklifi, sakatlık gibi önemli bir olayda durur.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        if (com.supermenajer.game.game.Matchday.canFriendly(g)) {
            var fr by remember { mutableStateOf<String?>(null) }
            OutlinedButton(onClick = { fr = GameRepo.friendly() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("🤝 Hazırlık maçı oyna (${g.friendlies}/3)")
            }
            fr?.let { Text("Sonuç: $it", color = Gold, fontSize = 12.sp) }
        }
        if (g.nextUserFixture() != null) {
            Spacer(Modifier.height(6.dp))
            MatchPrep(g)
        }
    }
}

@Composable
private fun FiredCard(g: Game) {
    SectionCard("Kovuldun!") {
        Text("😞 Yönetim seninle yolları ayırdı.", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text("Ama futbol dünyası seni unutmadı. Şu kulüpler teknik direktör arıyor:", color = TextDim, fontSize = 13.sp)
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
                    Text("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · ${posText(g, t)} sıra · ${money(t.budget)}", color = TextDim, fontSize = 12.sp)
                }
                Button(onClick = { GameRepo.takeJob(t.id) }) { Text("İmzala") }
            }
        }
    }
}

@Composable
private fun IncomingOffersCard(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    SectionCard("Gelen transfer teklifleri") {
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
                    Text("Teklif: ${money(o.price)} · Değeri: ${money(p.value)} · Güç ${p.overall}", color = TextDim, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { GameRepo.acceptIncoming(o); msg = null }, modifier = Modifier.weight(1f)) { Text("Sat", fontSize = 13.sp) }
                OutlinedButton(onClick = { msg = GameRepo.counterIncoming(o) }, modifier = Modifier.weight(1f)) { Text("+%20 iste", fontSize = 13.sp) }
                OutlinedButton(onClick = { GameRepo.rejectIncoming(o); msg = null }, modifier = Modifier.weight(1f)) { Text("Reddet", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun SeasonEndCard(g: Game) {
    val rec = g.history.lastOrNull()
    val t = g.userTeam
    val pos = rec?.userPos ?: g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    SectionCard("Sezon sonu") {
        Text("🏆 ${rec?.leagueName?.ifEmpty { null } ?: Comp.name(t.division)} şampiyonu: ${rec?.champion ?: "-"}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (!rec?.cupWinner.isNullOrEmpty()) Text("🥇 ${Comp.name(Leagues.of(t.division)?.cupComp ?: Comp.CUP)}: ${rec?.cupWinner}", fontSize = 15.sp)
        if (!rec?.euroWinner.isNullOrEmpty()) Text("🌍 Avrupa Kupası: ${rec?.euroWinner}", fontSize = 15.sp)
        if (!rec?.firstChampion.isNullOrEmpty()) Text("Alt lig şampiyonu: ${rec?.firstChampion}", color = TextDim, fontSize = 14.sp)
        Text("Gol kralı: ${rec?.topScorer ?: "-"}", color = TextDim, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text("${t.name} ${Comp.name(t.division)}'i $pos. sırada bitirdi.", fontSize = 16.sp)
        val lg = Leagues.of(t.division)
        if (lg != null && lg.tier == 1 && Leagues.second(lg.country) != null && pos >= 16) Text("⬇️ Küme düştük...", color = Red, fontWeight = FontWeight.Bold)
        if (lg != null && lg.tier == 2 && pos <= 3) Text("⬆️ ${Leagues.top(lg.country).name.uppercase()}'NE YÜKSELDİK!", color = Gold, fontWeight = FontWeight.Bold)
        val (msg, col) = when {
            pos == 1 -> "Efsane sezon! Taraftar seni omuzlarda taşıyor! 🎉" to Gold
            pos <= target -> "Yönetim hedefi (${Season.objectiveText(t)}) tuttu. Başkan çok memnun! 👏" to Green
            pos <= target + 3 -> "Hedefin biraz gerisinde kaldık. Yönetim yeni sezonda daha fazlasını bekliyor." to Gold
            else -> "Hedefin çok gerisinde kaldık. Yönetim sabırsızlanıyor... 😬" to Red
        }
        Spacer(Modifier.height(6.dp))
        Text(msg, color = col)
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(10.dp))
        ReportLines(t.lastReport)
        val expiring = g.squad(t.id).filter { it.contractYears == 1 }
        if (expiring.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("📝 Sözleşmesi biten ve yeni sezonda ayrılacak oyuncular:", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            expiring.forEach { p ->
                Text("• ${p.name} (${p.pos.short}, ${p.overall}) · uzatmak için dokun", fontSize = 13.sp,
                    modifier = Modifier.clickable { GameRepo.selectedPlayer = p }.padding(vertical = 3.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Sezon arası transfer dönemi açık: yeni sezondan önce kadronu güçlendirebilirsin.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { GameRepo.startNewSeason() },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
        ) { Text("YENİ SEZONA BAŞLA (${g.season + 1})", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun FansCard(g: Game) {
    val f = g.fans
    val col = when { f >= 70 -> Green; f >= 40 -> Gold; else -> Red }
    SectionCard("📣 Taraftar") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(com.supermenajer.game.game.Fans.mood(f), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${f.toInt()}%", color = col, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.height(6.dp))
        Bar((f / 100).toFloat(), col, Modifier.fillMaxWidth(), 8.dp)
        Spacer(Modifier.height(6.dp))
        val effect = when {
            f >= 75 -> "Tribünler dolu, evde 12. adam desteği var! Sponsorlar ilgili."
            f >= 40 -> "Taraftar sonuçları izliyor. Galibiyetler ve derbiler moralini yükseltir."
            else -> "Seyirci azalıyor, yönetim baskı altında. Derbi kazanmak iyi gelir!"
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
        t.played == 0 -> "Sezon başlamadı" to TextDim
        pos <= target -> "Hedef yolunda ✓" to Green
        pos <= target + 3 -> "Hedefin biraz gerisinde" to Gold
        else -> "Yönetim endişeli!" to Red
    }
    SectionCard("Yönetim") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hedef: ${Season.objectiveText(t)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(status, color = col, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (t.played == 0) "–" else "$pos.", fontSize = 28.sp, fontWeight = FontWeight.Black, color = col)
                Text("sıra", color = TextDim, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        val c = g.confidence
        val cc = when { c >= 60 -> Green; c >= 30 -> Gold; else -> Red }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Yönetim güveni", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(110.dp))
            Bar((c / 100).toFloat(), cc, Modifier.weight(1f), 8.dp)
            Text(" ${c.toInt()}%", color = cc, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (c < 25 && !g.fired) Text("⚠️ Koltuğun sallanıyor! Sonuçlar düzelmezse kovulabilirsin.", color = Red, fontSize = 12.sp)
        if (t.form.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Form: ", color = TextDim, fontSize = 13.sp)
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
    SectionCard("Puan durumu · ${Comp.name(g.userTeam.division)}") {
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
        TextButton(onClick = { GameRepo.tab = 3 }) { Text("Tüm tablo →") }
    }
}

@Composable
private fun RecentResults(g: Game) {
    val res = g.fixtures.filter { it.played && it.involves(g.userTeamId) }.sortedBy { it.week }.takeLast(5).reversed()
    if (res.isEmpty()) return
    SectionCard("Son maçlar") {
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
    SectionCard("Haberler") {
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
    SectionCard("Avrupa Kupası · Grup ${Europe.groupLetter(group)}") {
        Row(Modifier.padding(horizontal = 6.dp)) {
            Text("Takım", Modifier.weight(1f), color = TextDim, fontSize = 11.sp)
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
        Text("Net", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text((if (total > 0) "+" else "") + money(total), color = if (total >= 0) Green else Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun FinanceCard(g: Game) {
    val t = g.userTeam
    val wages = Economy.squadWages(g, t.id)
    var showLast by remember { mutableStateOf(false) }
    SectionCard("Finans") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Transfer bütçesi", color = TextDim, fontSize = 12.sp)
                Text(money(t.budget), color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Haftalık maaş", color = TextDim, fontSize = 12.sp)
                Text("${money(wages)} / ${money(t.wageBudget)}", color = if (wages > t.wageBudget) Red else TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Bar((wages.toFloat() / t.wageBudget.coerceAtLeast(1)).coerceIn(0f, 1f), if (wages > t.wageBudget) Red else if (wages > t.wageBudget * 0.9) Gold else Green, Modifier.fillMaxWidth(), 8.dp)
        if (wages > t.wageBudget) Text("Maaş limiti aşıldı! Yeni oyuncu alamazsın; önce satış yap ya da sözleşmesi bitenleri bırak.", color = Red, fontSize = 12.sp)
        val expiring = g.squad(t.id).count { it.contractYears == 1 }
        if (expiring > 0) {
            Spacer(Modifier.height(6.dp))
            Text("📝 $expiring oyuncunun sözleşmesi sezon sonunda bitiyor. Kadro ekranından uzatabilirsin.", color = Gold, fontSize = 12.sp,
                modifier = Modifier.clickable { GameRepo.tab = 1 })
        }
        Spacer(Modifier.height(10.dp))
        Text(if (showLast) "GEÇEN SEZON MALİ RAPOR" else "BU SEZON", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        val rep = if (showLast) t.lastReport else t.income
        if (rep.isEmpty()) Text("Henüz kayıt yok.", color = TextDim, fontSize = 12.sp) else ReportLines(rep)
        Spacer(Modifier.height(4.dp))
        Text(
            "Sezon sonunda yönetim; lig sıranıza (şampiyona €20M), hedef primine ve kulüp itibarına göre bütçe ayırır. Başarı itibarı ve maaş limitini de büyütür.",
            color = TextDim, fontSize = 11.sp,
        )
        if (t.lastReport.isNotEmpty()) TextButton(onClick = { showLast = !showLast }) { Text(if (showLast) "Bu sezonu göster" else "Geçen sezonun raporu →") }
    }
}

fun posText(g: Game, t: Team): String = if (t.played == 0) "–" else "${g.position(t.id)}."

/** Takımın altında gösterilen kısa bilgi: yabancıysa bayrak + lig, değilse lig + sıra. */
fun teamSubtitle(g: Game, t: Team): String =
    if (g.isForeign(t)) "${Generator.flag(g.flagOf(t))} ${g.leagueName(t)}"
    else "${g.leagueName(t)} · ${posText(g, t)} sıra"

@Composable
private fun PressCard(g: Game, q: PressQuestion) {
    var result by remember(q) { mutableStateOf<String?>(null) }
    SectionCard("🎤 Basın toplantısı") {
        Text(q.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        q.answers.forEachIndexed { i, a ->
            OutlinedButton(
                onClick = { result = GameRepo.answerPress(q, i) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
            ) { Text(a, fontSize = 13.sp, textAlign = TextAlign.Center) }
        }
        TextButton(onClick = { GameRepo.skipPress() }) { Text("Yorum yapmadan geç", color = TextDim) }
    }
}

@Composable
private fun CareerOfferCard(g: Game) {
    SectionCard("💼 Kariyer teklifi") {
        Text("Başarın büyük kulüplerin dikkatini çekti! Kabul edersen hemen yeni takımının başına geçersin.", fontSize = 13.sp, color = TextDim)
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
                    Text("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · Bütçe ${money(t.budget)}", color = TextDim, fontSize = 12.sp)
                }
                Button(onClick = { GameRepo.takeJob(t.id) }) { Text("Kabul") }
            }
        }
        TextButton(onClick = { GameRepo.declineCareerOffer() }) { Text("Reddet, kulübümde kalıyorum") }
    }
}

/** Yardımcı antrenörün rakip raporu ve önerileri. */
@Composable
private fun OpponentReport(g: Game, opp: Team) {
    val open = GameRepo.reportOpen
    val us = g.userTeam
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { GameRepo.reportOpen = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("📋 Yardımcı antrenör: rakip raporu", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(if (open) "▲" else "▼", color = TextDim)
        }
        if (!open) return@Column
        val (oa, om, od) = com.supermenajer.game.game.Lineups.lineStrength(g, opp)
        val (ua, um, ud) = com.supermenajer.game.game.Lineups.lineStrength(g, us)
        Spacer(Modifier.height(8.dp))
        Text("${opp.name}: ${opp.formation} · ${listOf("Defansif", "Dengeli", "Hücum")[opp.mentality]}" + if (opp.form.isNotEmpty()) " · Form ${opp.form}" else "", fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        listOf(Triple("Hücum", ua, oa), Triple("Orta saha", um, om), Triple("Savunma", ud, od)).forEach { (n, mine, theirs) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$mine", fontWeight = FontWeight.Bold, color = if (mine >= theirs) Green else Red, modifier = Modifier.width(32.dp))
                Text(n, color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("$theirs", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }
        val danger = opp.lineup.mapNotNull { g.player(it) }.maxByOrNull { it.overall }
        val scorer = g.squad(opp.id).maxByOrNull { it.goals }?.takeIf { it.goals > 0 }
        val aq = com.supermenajer.game.game.StaffLogic.q(g, com.supermenajer.game.game.StaffLogic.ASSISTANT)
        if (aq >= 8) danger?.let { Text("En tehlikeli oyuncu: ${it.name} (${it.pos.short})", fontSize = 12.sp) }
        scorer?.let { Text("Golcüsü: ${it.name} (${it.goals} gol)", fontSize = 12.sp) }
        Spacer(Modifier.height(6.dp))
        val advice = mutableListOf<String>()
        if (oa > ud + 3) advice += "Hücumları güçlü. Defansif oynamayı ya da 5-3-2'yi düşünebilirsin."
        if (ua > od + 3) advice += "Savunmaları zayıf. Hücum oynayıp üstlerine gitmeliyiz!"
        if (om > um + 3) advice += "Orta sahada ezilebiliriz. 4-5-1 veya 4-2-3-1 ile orta sahayı kalabalıklaştır."
        if (um > om + 3) advice += "Orta saha bizde. Yüksek presle topu sürekli kazanabiliriz."
        val tired = us.lineup.mapNotNull { g.player(it) }.filter { it.fitness < 75 }
        if (tired.isNotEmpty()) advice += "${tired.joinToString { it.name }} yorgun (kondisyon %75 altı). Dinlendirmeyi düşün."
        val unhappy = us.lineup.mapNotNull { g.player(it) }.filter { it.morale < 30 }
        if (unhappy.isNotEmpty()) advice += "${unhappy.joinToString { it.name }} çok mutsuz, performansı düşük olabilir."
        if (advice.isEmpty()) advice += "Güçler denk. Dengeli oynamak mantıklı hocam."
        val shownAdvice = if (aq >= 12) advice else advice.take(1)
        shownAdvice.forEach { Text("🗣 $it", fontSize = 12.sp, color = Gold, modifier = Modifier.padding(vertical = 1.dp)) }
        if (aq < 12 && advice.size > 1) Text("Daha detaylı analiz için daha iyi bir yardımcı antrenör gerekli.", color = TextDim, fontSize = 11.sp)
    }
}
