package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Inbox
import com.supermenajer.game.model.News

/** Kulüp menüsü: teknik ekip, tesisler, sponsorlar, finans ve taraftar. */
@Composable
fun ClubTab(g: Game) {
    val modes = listOf("👔 Teknik Ekip", "🏟️ Tesisler", "💼 Sponsorlar", "💰 Finans", "📣 Taraftar")
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            modes.forEachIndexed { i, n ->
                val dot = i == 2 && g.sponsorOffers.isNotEmpty()
                Chip(if (dot) "$n 🔴" else n, GameRepo.clubMode == i) { GameRepo.clubMode = i }
            }
        }
        when (GameRepo.clubMode) {
            0 -> StaffView(g)
            1 -> FacilitiesView(g)
            2 -> SponsorsView(g)
            3 -> TwoCards({ FinanceCard(g) }, { BoardCard(g) })
            else -> TwoCards({ FansCard(g) }, { FanInfoCard() })
        }
    }
}

@Composable
private fun TwoCards(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { left() }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { right() }
    }
}

@Composable
private fun FanInfoCard() {
    SectionCard("Taraftarı ne etkiler?") {
        listOf(
            "✅ Galibiyetler, özellikle derbi ve sürpriz galibiyetler",
            "✅ Yıldız transferleri, kupalar, Avrupa bileti",
            "✅ Ucuz bilet ve stadyum büyütme",
            "❌ Ağır ve derbi yenilgileri",
            "❌ En iyi oyuncularını satmak",
            "❌ Pahalı bilet, küme düşme",
        ).forEach { Text(it, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        Spacer(Modifier.height(8.dp))
        Text("Memnun taraftar statı doldurur, sponsorları çeker ve yönetimin güvenini artırır. %75'in üstünde evde 12. adam olur!", color = TextDim, fontSize = 12.sp)
    }
}

/** Gelen kutusu: solda mesaj listesi, sağda seçili mesaj (CM tarzı). */
@Composable
fun InboxTab(g: Game) {
    GameRepo.version
    val cat = GameRepo.inboxCat
    val list = g.news.filter { cat == Inbox.ALL || it.category == cat }
    val sel = GameRepo.inboxSel?.takeIf { it in g.news } ?: list.firstOrNull()
    androidx.compose.runtime.LaunchedEffect(sel) { if (sel != null && !sel.read) GameRepo.openNews(sel) }
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(330.dp).fillMaxHeight().background(Surface1)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Inbox.names.forEachIndexed { i, n ->
                    val unread = g.news.count { !it.read && (i == Inbox.ALL || it.category == i) }
                    Chip(if (unread > 0) "$n ($unread)" else n, cat == i) { GameRepo.inboxCat = i; GameRepo.inboxSel = null }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(list) { n -> MessageRow(g, n, n === sel) }
            }
            if (g.unread > 0) {
                TextButton(onClick = { GameRepo.readAll() }, modifier = Modifier.fillMaxWidth()) { Text("Tümünü okundu say") }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp)) {
            if (sel == null) {
                Text("Mesaj yok.", color = TextDim)
            } else {
                Text(Inbox.names[sel.category].uppercase(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(sel.title, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("${sel.season} · ${g.dayLabel(sel.week).ifEmpty { "Sezon arası" }}", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text(sel.body, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(16.dp))
                MessageAction(g, sel)
            }
        }
    }
}

@Composable
private fun MessageRow(g: Game, n: News, selected: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) Surface2 else Color.Transparent)
            .clickable { GameRepo.openNews(n) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (n.read) Color.Transparent else Red))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                n.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, color = if (n.read) TextDim else TextMain,
            )
            Text(n.body, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        Text(g.dayLabel(n.week).substringBefore(" ·").take(10), fontSize = 10.sp, color = TextDim, maxLines = 1)
    }
}

/** Mesaja göre ilgili ekrana kısayol. */
@Composable
private fun MessageAction(g: Game, n: News) {
    val t = n.title.lowercase()
    val (label, go) = when {
        "sponsor" in t -> "Sponsorlara git" to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 2 }
        "inşaat" in t || "altyapı" in t -> "Tesislere git" to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 1 }
        "teknik ekip" in t -> "Teknik ekibe git" to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 0 }
        "taraftar" in t -> "Taraftar durumuna git" to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 4 }
        "teklif" in t && "kariyer" !in t -> "Ana sayfaya git" to { GameRepo.tab = GameRepo.TAB_HOME }
        "gözlemci" in t -> "Gözlem listesine git" to { GameRepo.tab = GameRepo.TAB_TRANSFER }
        "yardımcı antrenör" in t -> "Kadroya git" to { GameRepo.tab = GameRepo.TAB_SQUAD }
        "kariyer" in t || "kovul" in t -> "Ana sayfaya git" to { GameRepo.tab = GameRepo.TAB_HOME }
        else -> return
    }
    OutlinedButton(onClick = go) { Text("$label →") }
}
