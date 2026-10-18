package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Fixture
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
                TextButton(onClick = { GameRepo.save(); confirmExit = false; GameRepo.screen = Screen.TITLE }) { Text("Ana Menü") }
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
            val wk = if (g.seasonOver) "Sezon bitti" else "Hafta ${g.week}/${g.totalWeeks}"
            Text("${g.managerName} · ${g.season}-${(g.season + 1) % 100} · $wk", color = TextDim, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(t.budget), color = if (t.budget >= 0) Gold else Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Bütçe", color = TextDim, fontSize = 11.sp)
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
        if (g.seasonOver) {
            item { SeasonEndCard(g) }
        } else {
            g.nextUserFixture()?.let { f -> item { NextMatchCard(g, f) } }
        }
        item { BoardCard(g) }
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
    SectionCard("Sıradaki maç · Hafta ${f.week}") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamBadge(h, 56.dp)
                Spacer(Modifier.height(6.dp))
                Text(h.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp)
                Text("${g.position(h.id)}. sıra", color = TextDim, fontSize = 12.sp)
            }
            Text("VS", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextDim)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamBadge(a, 56.dp)
                Spacer(Modifier.height(6.dp))
                Text(a.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp)
                Text("${g.position(a.id)}. sıra", color = TextDim, fontSize = 12.sp)
            }
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
private fun SeasonEndCard(g: Game) {
    val rec = g.history.lastOrNull()
    val t = g.userTeam
    val pos = rec?.userPos ?: g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    SectionCard("Sezon sonu") {
        Text("🏆 Şampiyon: ${rec?.champion ?: "-"}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text("Gol kralı: ${rec?.topScorer ?: "-"}", color = TextDim)
        Spacer(Modifier.height(12.dp))
        Text("${t.name} ligi $pos. sırada bitirdi.", fontSize = 16.sp)
        val (msg, col) = when {
            pos == 1 -> "Efsane sezon! Taraftar seni omuzlarda taşıyor! 🎉" to Gold
            pos <= target -> "Yönetim hedefi (${Season.objectiveText(t.expected)}) tuttu. Başkan çok memnun! 👏" to Green
            pos <= target + 3 -> "Hedefin biraz gerisinde kaldık. Yönetim yeni sezonda daha fazlasını bekliyor." to Gold
            else -> "Hedefin çok gerisinde kaldık. Yönetim sabırsızlanıyor... 😬" to Red
        }
        Spacer(Modifier.height(6.dp))
        Text(msg, color = col)
        Spacer(Modifier.height(14.dp))
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
                Text("Hedef: ${Season.objectiveText(t.expected)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(status, color = col, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$pos.", fontSize = 28.sp, fontWeight = FontWeight.Black, color = col)
                Text("sıra", color = TextDim, fontSize = 11.sp)
            }
        }
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
    val table = g.table()
    val ui = table.indexOfFirst { it.id == g.userTeamId }
    val rows = (table.indices.take(5) + listOf(ui - 1, ui, ui + 1)).filter { it in table.indices }.distinct().sorted()
    SectionCard("Puan durumu") {
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
    val res = g.fixtures.filter { it.played && (it.home == g.userTeamId || it.away == g.userTeamId) }.takeLast(5).reversed()
    if (res.isEmpty()) return
    SectionCard("Son maçlar") {
        res.forEachIndexed { i, f ->
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
            ResultRow(g, f, g.userTeamId)
        }
    }
}

@Composable
fun ResultRow(g: Game, f: Fixture, highlightTeam: Int = -1) {
    val h = g.team(f.home)
    val a = g.team(f.away)
    val userGoals = if (f.home == highlightTeam) f.hg else f.ag
    val oppGoals = if (f.home == highlightTeam) f.ag else f.hg
    val col = when {
        highlightTeam != f.home && highlightTeam != f.away -> Surface2
        userGoals > oppGoals -> Green
        userGoals == oppGoals -> Gold
        else -> Red
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.name, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (h.id == highlightTeam) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.width(6.dp))
        TeamBadge(h, 20.dp)
        Box(
            Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(6.dp)).background(col).padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(if (f.played) "${f.hg} - ${f.ag}" else "  -  ", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = if (col == Surface2) TextMain else Color.Black)
        }
        TeamBadge(a, 20.dp)
        Spacer(Modifier.width(6.dp))
        Text(a.name, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (a.id == highlightTeam) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun NewsCard(g: Game) {
    SectionCard("Haberler") {
        g.news.take(10).forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))
            Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(n.body, color = TextDim, fontSize = 13.sp)
            Text("${n.season} · Hafta ${n.week}", color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
