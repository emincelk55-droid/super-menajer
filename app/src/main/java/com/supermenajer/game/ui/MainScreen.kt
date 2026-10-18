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
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler {
        if (GameRepo.tab != 0) GameRepo.tab = 0 else confirmExit = true
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(g)
        Box(Modifier.weight(1f)) {
            when (GameRepo.tab) {
                0 -> HomeTab(g)
                1 -> SquadTab(g)
                2 -> TacticsTab(g)
                3 -> LeagueTab(g)
                4 -> TransferTab(g)
            }
        }
        NavigationBar(containerColor = Surface1) {
            val items = listOf(
                "Ana Sayfa" to Icons.Filled.Home,
                "Kadro" to Icons.Filled.Groups,
                "Taktik" to Icons.Filled.SportsSoccer,
                "Lig" to Icons.Filled.EmojiEvents,
                "Transfer" to Icons.Filled.SwapHoriz,
            )
            items.forEachIndexed { i, (label, icon) ->
                NavigationBarItem(
                    selected = GameRepo.tab == i,
                    onClick = { GameRepo.tab = i },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black, indicatorColor = Green,
                        selectedTextColor = Green, unselectedIconColor = TextDim, unselectedTextColor = TextDim,
                    ),
                )
            }
        }
    }
    GameRepo.selectedPlayer?.let { PlayerDialog(g, it) }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Ana menüye dön?") },
            text = { Text("İlerlemen otomatik olarak kaydedildi.") },
            confirmButton = {
                TextButton(onClick = { GameRepo.save(force = true); confirmExit = false; GameRepo.screen = Screen.TITLE }) { Text("Ana Menü") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun TopBar(g: Game) {
    val t = g.userTeam
    Row(
        Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamBadge(t, 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val wk = if (g.seasonOver) "Sezon bitti" else g.dayLabel(g.week)
            Text("${Comp.name(t.division)} · $wk", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(t.budget), color = if (t.budget >= 0) Gold else Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Maaş ${money(Economy.squadWages(g, t.id)).removePrefix("€")}/${money(t.wageBudget).removePrefix("€")}",
                color = if (Economy.squadWages(g, t.id) > t.wageBudget) Red else TextDim, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun HomeTab(g: Game) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            g.fired -> item { FiredCard(g) }
            g.seasonOver -> item { SeasonEndCard(g) }
            else -> {
                val f = g.todayUserFixture()
                item { if (f != null) NextMatchCard(g, f) else RestDayCard(g) }
            }
        }
        g.press?.let { q -> item { PressCard(g, q) } }
        if (g.offers.isNotEmpty() && !g.fired) item { IncomingOffersCard(g) }
        if (Europe.groupOf(g, g.userTeamId) >= 0) item { EuroGroupCard(g, Europe.groupOf(g, g.userTeamId)) }
        item { BoardCard(g) }
        item { FinanceCard(g) }
        item { MiniTable(g) }
        item { RecentResults(g) }
        item { NewsCard(g) }
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
    SectionCard("Sıradaki maç · $title") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            for ((i, t) in listOf(h, a).withIndex()) {
                if (i == 1) Text("VS", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextDim)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamBadge(t, 56.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(t.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(teamSubtitle(g, t), color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
        if (Comp.isKnockout(f.comp, f.round)) {
            Spacer(Modifier.height(8.dp))
            Text("Tek maç eleme. Beraberlikte penaltılar!", color = Gold, fontSize = 12.sp)
        } else if (f.comp == Comp.EURO) {
            Spacer(Modifier.height(8.dp))
            Text("Avrupa Kupası grup maçı · Gruptan ilk 2 çıkar. Galibiyet primi: €600K", color = Gold, fontSize = 12.sp)
        }
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "⚠️ İlk 11'de oynayamayacak oyuncu var (${issues.joinToString { it.name }}). Maçta otomatik değiştirilecek.",
                color = Gold, fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { GameRepo.playMatch(live = true) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
        ) { Text("▶  MAÇA ÇIK", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { GameRepo.playMatch(live = false) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        ) { Text("Hızlı Sonuç") }
    }
}

@Composable
private fun RestDayCard(g: Game) {
    val d = g.today
    SectionCard(g.dayLabel(g.week)) {
        Text(
            when (d?.type) {
                1 -> "Bu kupa turunda maçımız yok."
                2 -> "Bu Avrupa gecesinde maçımız yok. Diğer maçları Lig → Avrupa sekmesinden takip edebilirsin."
                else -> "Bugün maçımız yok."
            },
            fontSize = 15.sp,
        )
        g.nextUserFixture()?.let { nf ->
            val opp = g.team(if (nf.home == g.userTeamId) nf.away else nf.home)
            Spacer(Modifier.height(6.dp))
            Text("Sıradaki maç: ${opp.name} · ${g.dayLabel(nf.week)}", color = TextDim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { GameRepo.skipToNextMatch() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("⏩  SONRAKİ MAÇA GİT", fontWeight = FontWeight.Bold)
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
private fun BoardCard(g: Game) {
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
    val order = listOf(Economy.PRIZE, Economy.BONUS, Economy.BOARD, Economy.EURO, Economy.CUP, Economy.TICKETS, Economy.SALES, Economy.BUYS)
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
private fun FinanceCard(g: Game) {
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
