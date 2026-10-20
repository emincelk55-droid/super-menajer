package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameCalendar
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Perks
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player

/* ---------- SM tarzı ortak parçalar ---------- */

val CardTop = Color(0xFF161D42)
val CardBottom = Color(0xFF0E1431)
val CardLine = Color(0xFF2A3570)
val Teal = Color(0xFF2EE6C5)

/** Koyu, ince kenarlıklı SM kartı. */
@Composable
fun DashCard(
    title: String?, modifier: Modifier = Modifier, badge: Int = 0, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, CardLine, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp, color = TextMain, modifier = Modifier.weight(1f), maxLines = 1)
                if (badge > 0) {
                    Box(Modifier.clip(CircleShape).background(Red).padding(horizontal = 7.dp, vertical = 1.dp)) {
                        Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        content()
    }
}

/** Yuvarlak, kenarlıklı ikon düğmesi (☰, ‹, 📅, ⚙). */
@Composable
fun RoundIconButton(icon: String, size: Dp = 40.dp, dot: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(size)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFF2A3360), Color(0xFF141A3A))))
                .border(1.dp, Color(0xFF414C82), CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = (size.value * 0.45f).sp, color = TextMain, fontWeight = FontWeight.Bold) }
        if (dot) Box(Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(Red).border(1.5.dp, Color.White, CircleShape))
    }
}

/** Üst çubuktaki kaynak göstergesi (bütçe, maaş, güven). */
@Composable
private fun ResourceChip(icon: String, value: String, color: Color = TextMain, onClick: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0B1028))
            .border(1.dp, Color(0xFF2F3A6E), RoundedCornerShape(8.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

fun devamTitle(g: Game): String = when {
    GameRepo.advancing -> "Durdur"
    g.fired -> "Teklifler"
    g.pendingEvent != null -> "Karar Ver"
    g.seasonOver -> "Yeni Sezon"
    g.todayUserFixture() != null -> "Maça Çık"
    else -> "Devam Et"
}

/** SM tarzı üst çubuk: menü, geri, kulüp, kaynaklar ve büyük "Devam Et". */
@Composable
fun SmTopBar(g: Game, onMenu: () -> Unit) {
    val t = g.userTeam
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0C1131), Color(0xFF15103D), Color(0xFF0C1131))))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("☰", dot = g.unread > 0, onClick = onMenu)
        if (GameRepo.tab != GameRepo.TAB_HOME) {
            Spacer(Modifier.width(8.dp))
            RoundIconButton("‹") { GameRepo.tab = GameRepo.TAB_HOME }
        }
        Spacer(Modifier.width(10.dp))
        TeamBadge(t, 36.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(t.short, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
            Text(t.name, color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val wages = Economy.squadWages(g, t.id)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceChip("💶", money(t.budget), if (t.budget >= 0) Gold else Red) { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3 }
            ResourceChip("👥", money(wages).removePrefix("€") + "/" + money(t.wageBudget).removePrefix("€"), if (wages > t.wageBudget) Red else TextMain) {
                GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3
            }
            ResourceChip("🏛", "%${g.confidence.toInt()}", when { g.confidence >= 60 -> Green; g.confidence >= 30 -> Gold; else -> Red }) {
                GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3
            }
        }
        Spacer(Modifier.width(10.dp))
        DevamButton(g)
    }
}

@Composable
private fun DevamButton(g: Game) {
    val shape = RoundedCornerShape(10.dp)
    val active = !g.fired
    val date = if (g.date > 0) GameCalendar.format(g.date) else "${g.season}"
    val wk = when {
        g.seasonOver -> "Sezon bitti"
        g.todayUserFixture() != null -> "Maç günü"
        !g.isMatchDay -> if (g.week == 1) "Hazırlık" else "Maç yok"
        else -> "Maç yok"
    }
    val main = if (GameRepo.advancing) listOf(Color(0xFFF5C542), Color(0xFFE0A21B)) else listOf(Color(0xFF34F5C0), Color(0xFF12C795))
    Row(
        Modifier.height(48.dp).width(210.dp).clip(shape).background(Brush.horizontalGradient(main))
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), shape)
            .clickable(enabled = active) { GameRepo.continueGame() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(devamTitle(g), color = Color(0xFF06231A), fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, lineHeight = 19.sp)
            Text("$date · $wk", color = Color(0xFF06231A).copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1, lineHeight = 11.sp)
        }
        Box(Modifier.fillMaxHeight().width(46.dp).background(Color(0xFF0B1A2E)), contentAlignment = Alignment.Center) {
            Text(if (GameRepo.advancing) "⏸" else "›", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TaskChip(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E1430))
            .border(1.dp, Teal.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF232B57)).border(1.dp, Color(0xFF414C82), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 15.sp) }
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Ana sayfanın ikinci satırı: görev kısayolları, kayan haber bandı, takvim ve ayarlar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickRow(g: Game) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Görevler", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDim)
        TaskChip("🏋", "Antrenman") { GameRepo.tab = GameRepo.TAB_TACTICS; GameRepo.tacticsMode = 1 }
        TaskChip("🎯", "Hedefler") { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3 }
        TaskChip("🔭", "Gözlemci") { GameRepo.tab = GameRepo.TAB_TRANSFER }
        val ticker = g.news.take(6).joinToString("     |     ") { it.title + ": " + it.body.take(70) }
        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp)).clickable { GameRepo.tab = GameRepo.TAB_INBOX }, contentAlignment = Alignment.CenterStart) {
            Text(
                ticker.ifEmpty { "Hoş geldin hocam!" }, color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        RoundIconButton("📅", 38.dp) { GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 3 }
        RoundIconButton("⚙", 38.dp) { GameRepo.settingsOpen = true }
    }
}

private class MenuEntry(val icon: String, val label: String, val tab: Int, val badge: (Game) -> Int = { 0 })

private val menuEntries = listOf(
    MenuEntry("🏠", "Ana Sayfa", GameRepo.TAB_HOME),
    MenuEntry("✉", "Gelen Kutusu", GameRepo.TAB_INBOX) { it.unread },
    MenuEntry("👥", "Kadro", GameRepo.TAB_SQUAD),
    MenuEntry("📋", "Taktik", GameRepo.TAB_TACTICS),
    MenuEntry("🔁", "Transfer", GameRepo.TAB_TRANSFER) { it.offers.size },
    MenuEntry("🏆", "Lig ve Kupalar", GameRepo.TAB_LEAGUE),
    MenuEntry("🏛", "Kulüp", GameRepo.TAB_CLUB),
    MenuEntry("🌍", "Milli Takım", GameRepo.TAB_NATIONAL),
)

/** ☰ ile açılan yan menü. */
@Composable
fun MenuDrawer(g: Game, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose)) {
        Column(
            Modifier.width(280.dp).fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF171E48), Color(0xFF0B1028))))
                .border(1.dp, CardLine)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(g.userTeam, 46.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(g.userTeam.name, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(g.managerName, color = TextDim, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            menuEntries.forEach { e ->
                val sel = GameRepo.tab == e.tab
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Teal.copy(alpha = 0.16f) else Color.Transparent)
                        .border(1.dp, if (sel) Teal.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { GameRepo.tab = e.tab; onClose() }.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(e.icon, fontSize = 18.sp, modifier = Modifier.width(32.dp))
                    Text(e.label, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold, fontSize = 15.sp, color = if (sel) Teal else TextMain, modifier = Modifier.weight(1f))
                    val b = e.badge(g)
                    if (b > 0) Box(Modifier.clip(CircleShape).background(Red).padding(horizontal = 7.dp, vertical = 1.dp)) {
                        Text(if (b > 99) "99+" else "$b", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClose(); GameRepo.settingsOpen = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⚙", fontSize = 18.sp, modifier = Modifier.width(32.dp))
                Text("Ayarlar · Kaydet · Ana menü", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

/* ---------- Ana sayfa panosu ---------- */

/**
 * SM tarzı ana sayfa: üstte büyük maç kartı ve kulüp durumu, altında bekleyen kararlar,
 * sonra gelen kutusu / menajer / müsabakalar / spot lambası ve transferler / tesisler / takvim.
 */
@Composable
fun HomeDashboard(g: Game) {
    val pending = buildList<@Composable () -> Unit> {
        if (!g.fired && g.pendingEvent != null) add { EventCard(g) }
        if (g.fired) add { FiredCard(g) }
        if (g.seasonOver && !g.fired) add { SeasonEndCard(g) }
        if (!g.fired && g.careerOffer && g.jobOffers.isNotEmpty()) add { CareerOfferCard(g) }
        g.press?.let { q -> add { PressCard(g, q) } }
        if (g.offers.isNotEmpty() && !g.fired) add { IncomingOffersCard(g) }
        if (g.sponsorOffers.isNotEmpty() && !g.fired) add { SponsorOffersCard(g) }
        if (!g.fired && !g.seasonOver && g.nextUserFixture() != null) add { MatchPrepCard(g) }
        val eg = com.supermenajer.game.game.Europe.groupOf(g, g.userTeamId)
        if (eg >= 0) add { EuroGroupCard(g, eg) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(186.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMatchCard(g, Modifier.weight(2.15f).fillMaxHeight())
                ClubStatusMini(g, Modifier.weight(1f).fillMaxHeight())
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(184.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InboxCard(g, Modifier.weight(1f).fillMaxHeight())
                ManagerCard(g, Modifier.weight(1f).fillMaxHeight())
                CompetitionCard(g, Modifier.weight(1f).fillMaxHeight())
                SpotlightCard(g, Modifier.weight(1.05f).fillMaxHeight())
            }
        }
        if (pending.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..1) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            pending.filterIndexed { i, _ -> i % 2 == col }.forEach { it() }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransfersCard(g, Modifier.weight(1f).fillMaxHeight())
                FacilitiesMini(g, Modifier.weight(1f).fillMaxHeight())
                UpcomingCard(g, Modifier.weight(1f).fillMaxHeight())
                FormCard(g, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun RatingPill(v: Int) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0B1028)).border(2.5.dp, ovrColor(v), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text("$v", fontWeight = FontWeight.Black, fontSize = 16.sp) }
}

@Composable
private fun InfoCell(icon: String, title: String, sub: String, modifier: Modifier) {
    Row(modifier.padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SideButton(label: String, accent: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(if (accent) Brush.horizontalGradient(listOf(Color(0xFF34F5C0), Color(0xFF12C795))) else Brush.verticalGradient(listOf(Color(0xFF1B2452), Color(0xFF111735))))
            .border(1.dp, if (accent) Color.White.copy(alpha = 0.5f) else Teal.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (accent) Color(0xFF06231A) else TextMain, maxLines = 1) }
}

/** Sıradaki maç: iki arma, takım güçleri, turnuva/tarih/stadyum ve kısayollar. */
@Composable
private fun HeroMatchCard(g: Game, modifier: Modifier) {
    val f = g.todayUserFixture() ?: g.nextUserFixture()
    val shape = RoundedCornerShape(16.dp)
    if (f == null) {
        DashCard(if (g.fired) "Kovuldun" else "Sezon sona erdi", modifier) {
            Text(if (g.fired) "Yeni bir kulüp bul: teklifler aşağıda." else "Sezon özetin aşağıda. Yeni sezona hazır olduğunda DEVAM ET'e bas.", color = TextDim, fontSize = 14.sp)
        }
        return
    }
    val h = g.team(f.home)
    val a = g.team(f.away)
    val today = g.todayUserFixture() != null
    Row(
        modifier.clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(h.color1).copy(alpha = 0.22f), CardTop, CardBottom, Color(a.color1).copy(alpha = 0.22f))))
            .border(1.dp, CardLine, shape).padding(10.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                for ((i, t) in listOf(h, a).withIndex()) {
                    if (i == 1) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingPill(Lineups.strength(g, h).toInt())
                                Text("  -  ", color = TextDim, fontWeight = FontWeight.Black)
                                RatingPill(Lineups.strength(g, a).toInt())
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(if (today) "BUGÜN" else "${GameCalendar.daysUntil(g, f.week)} GÜN SONRA", color = if (today) Green else TextDim, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        TeamBadge(t, 56.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(t.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        Text(teamSubtitle(g, t), color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HorizontalDivider(color = CardLine.copy(alpha = 0.7f))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val comp = Comp.name(f.comp)
                val round = if (Comp.isCup(f.comp) || f.comp == Comp.EURO) Comp.roundName(f.comp, f.round) else g.dayLabel(f.week)
                InfoCell("🏆", comp, round, Modifier.weight(1.2f))
                val date = GameCalendar.dateOf(g, f.week)
                InfoCell("📅", GameCalendar.format(date, false).substringAfter(" "), GameCalendar.format(date, false).substringBefore(" ") + if (today) " · bugün" else "", Modifier.weight(0.8f))
                InfoCell("🏟", h.stadiumName.ifBlank { "${h.name} Stadı" }, "%,d kişilik".format(h.stadium).replace(',', '.'), Modifier.weight(1.3f))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.width(150.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SideButton("👥  Takım", modifier = Modifier.weight(1f)) { GameRepo.tab = GameRepo.TAB_SQUAD }
            SideButton("📋  Taktikler", modifier = Modifier.weight(1f)) { GameRepo.tab = GameRepo.TAB_TACTICS; GameRepo.tacticsMode = 0 }
            if (today) {
                SideButton("▶  MAÇA ÇIK", accent = true, modifier = Modifier.weight(1f)) { GameRepo.playMatch(live = true) }
                SideButton("⚡  Hızlı sonuç", modifier = Modifier.weight(1f)) { GameRepo.playMatch(live = false) }
            } else {
                SideButton("⏩  Maça atla", modifier = Modifier.weight(1f)) { GameRepo.skipToNextMatch() }
            }
        }
    }
}

@Composable
private fun ClubStatusMini(g: Game, modifier: Modifier) {
    val t = g.userTeam
    val pos = g.position(t.id)
    val target = Season.objectiveTarget(t.expected)
    val col = when {
        t.played < 4 -> TextMain
        pos <= target -> Green
        pos <= target + 3 -> Gold
        else -> Red
    }
    DashCard("Kulüp Durumu", modifier, onClick = { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 3 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hedef", color = TextDim, fontSize = 11.sp)
                Text(Season.objectiveText(t), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (t.played == 0) "–" else "$pos.", fontSize = 28.sp, fontWeight = FontWeight.Black, color = col)
        }
        Spacer(Modifier.height(4.dp))
        val squad = g.squad(t.id)
        val morale = if (squad.isEmpty()) 60.0 else squad.sumOf { it.morale } / squad.size
        MiniBar("Yönetim", g.confidence / 100)
        MiniBar("Taraftar", g.fans / 100)
        MiniBar("Uyum", g.cohesion / 100)
        MiniBar("Moral", morale / 100)
        if (t.form.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FormDots(t.form)
        }
    }
}

@Composable
private fun MiniBar(label: String, v: Double) {
    val c = when { v >= 0.6 -> Green; v >= 0.35 -> Gold; else -> Red }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.width(62.dp), maxLines = 1)
        Bar(v.toFloat().coerceIn(0f, 1f), c, Modifier.weight(1f), 6.dp)
        Text("${(v * 100).toInt()}", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun InboxCard(g: Game, modifier: Modifier) {
    DashCard("Gelen Kutusu", modifier, badge = g.unread, onClick = { GameRepo.inboxCat = 0; GameRepo.tab = GameRepo.TAB_INBOX }) {
        g.news.take(4).forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
            Row(
                Modifier.fillMaxWidth().clickable { GameRepo.openNews(n); GameRepo.inboxCat = 0; GameRepo.tab = GameRepo.TAB_INBOX }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (n.read) TextDim.copy(alpha = 0.4f) else Green))
                Spacer(Modifier.width(8.dp))
                Text(n.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, color = if (n.read) TextDim else TextMain)
            }
        }
        if (g.news.isEmpty()) Text("Mesaj yok", color = TextDim, fontSize = 13.sp)
    }
}

/** Halka gösterge: değer 0..1. */
@Composable
fun RingGauge(value: Float, size: Dp, color: Color, stroke: Dp = 6.dp, center: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            drawArc(color, -90f, 360f * value.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
private fun ManagerCard(g: Game, modifier: Modifier) {
    val lv = Perks.level(g.managerXp)
    val (cur, need) = Perks.xpToNext(g.managerXp)
    val pts = Perks.points(g)
    DashCard("Menajer", modifier, onClick = { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 6 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFF3A4580), Color(0xFF1A2050)))),
                contentAlignment = Alignment.Center,
            ) {
                val ini = g.managerName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
                Text(ini.ifEmpty { "M" }, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            RingGauge(cur.toFloat() / need, 64.dp, Green) { Text("$lv", fontWeight = FontWeight.Black, fontSize = 22.sp) }
        }
        Spacer(Modifier.height(4.dp))
        Text("$cur/$need XP", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.managerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (pts > 0) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Gold).padding(horizontal = 6.dp, vertical = 1.dp)) {
                Text("⭐ $pts", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(managerTitle(lv), color = TextDim, fontSize = 11.sp)
    }
}

fun managerTitle(lv: Int): String = when {
    lv >= 20 -> "Efsane"
    lv >= 14 -> "Elit"
    lv >= 9 -> "Usta"
    lv >= 5 -> "Tecrübeli"
    else -> "Yeni yetme"
}

@Composable
private fun CompetitionCard(g: Game, modifier: Modifier) {
    val div = g.userTeam.division
    val table = g.table(div)
    val ui = table.indexOfFirst { it.id == g.userTeamId }.coerceAtLeast(0)
    val start = (ui - 1).coerceIn(0, (table.size - 4).coerceAtLeast(0))
    DashCard("Müsabakalar", modifier, onClick = { GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 0 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Comp.name(div), color = Teal, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("O", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
            Text("P", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(2.dp))
        table.drop(start).take(4).forEachIndexed { k, t ->
            val i = start + k
            val me = t.id == g.userTeamId
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (me) Teal.copy(alpha = 0.18f) else Color.Transparent).padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                TeamBadge(t, 20.dp)
                Spacer(Modifier.width(6.dp))
                Text(t.short, fontWeight = if (me) FontWeight.Black else FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${t.played}", fontSize = 12.sp, color = TextDim, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                Text("${t.points}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
            }
        }
    }
}

private class Spot(val label: String, val p: Player)

/** Spot lambası: takımın yıldızı, gol kralı, parlayan genç, dünyanın en iyisi (5 sn'de bir döner). */
@Composable
private fun SpotlightCard(g: Game, modifier: Modifier) {
    val spots = remember(GameRepo.version / 20) {
        val div = g.userTeam.division
        val league = g.players.filter { it.teamId >= 0 && g.teamOrNull(it.teamId)?.division == div }
        listOfNotNull(
            g.squad(g.userTeamId).maxByOrNull { it.overall }?.let { Spot("Takımın yıldızı", it) },
            league.filter { it.goals > 0 }.maxByOrNull { it.goals * 100 + it.overall }?.let { Spot("Gol kralı yarışı", it) },
            league.filter { it.age <= 21 }.maxByOrNull { it.potential * 10 + it.overall }?.let { Spot("Parlayan genç", it) },
            g.players.filter { it.teamId >= 0 }.maxByOrNull { it.overall }?.let { Spot("Dünyanın en iyisi", it) },
        )
    }
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(spots.size) {
        while (spots.size > 1) { kotlinx.coroutines.delay(5000); page = (page + 1) % spots.size }
    }
    val s = spots.getOrNull(page % spots.size.coerceAtLeast(1))
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xFF221A55), CardBottom)))
            .border(1.5.dp, Purple.copy(alpha = 0.7f), shape)
            .clickable(enabled = s != null) { s?.let { GameRepo.selectedPlayer = it.p } }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Spot Lambası", fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            spots.indices.forEach { i ->
                Box(
                    Modifier.padding(start = 4.dp).size(9.dp).clip(CircleShape)
                        .background(if (i == page % spots.size) Teal else Color.White.copy(alpha = 0.25f))
                        .clickable { page = i },
                )
            }
        }
        if (s == null) return@Column
        val p = s.p
        val team = g.teamOrNull(p.teamId)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.label.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(p.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Generator.flag(p.nation)} ${team?.name ?: ""}", color = Teal, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpotStat("YAŞ", "${p.age}")
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF0B1028)).border(2.dp, ovrColor(p.overall), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${p.overall}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ovrColor(p.overall)) }
                    SpotStat("MEVKİ", Positions.det(p).name)
                }
            }
            PlayerFace(p, 70.dp, team?.let { Color(it.color1) } ?: Color(0xFF2E7D32), team?.let { Color(it.color2) } ?: Color.White)
        }
    }
}

@Composable
private fun SpotStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Teal)
    }
}

@Composable
private fun TransfersCard(g: Game, modifier: Modifier) {
    DashCard("Transferler", modifier, onClick = { GameRepo.tab = GameRepo.TAB_TRANSFER }) {
        if (g.transfers.isEmpty()) Text("Bu dönem henüz transfer yok.", color = TextDim, fontSize = 13.sp)
        g.transfers.take(4).forEachIndexed { i, line ->
            val c = line.split("|")
            if (c.size < 5) return@forEachIndexed
            if (i > 0) HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(shortName(c[0]), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("»", color = Teal, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(c[2], fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                val price = c[3].toLongOrNull() ?: 0
                Text(if (price > 0) money(price) else "Bedelsiz", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** "Arda Güler" → "A. GÜLER". */
fun shortName(n: String): String {
    val parts = n.trim().split(Regex("\\s+"))
    return if (parts.size < 2) n.uppercase() else "${parts.first().take(1)}. ${parts.drop(1).joinToString(" ")}".uppercase()
}

@Composable
private fun FacilitiesMini(g: Game, modifier: Modifier) {
    val t = g.userTeam
    val avg = (t.trainingFac + t.youthFac + t.medicalFac) / 3.0
    DashCard("Tesisler", modifier, onClick = { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 1 }) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏟", fontSize = 30.sp)
                Text("Stadyum", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("%,d".format(t.stadium).replace(',', '.'), color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                RingGauge((avg / 10).toFloat(), 58.dp, if (avg >= 6) Green else if (avg >= 3) Gold else Red) {
                    Text("%.1f".format(avg), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text("Genel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun UpcomingCard(g: Game, modifier: Modifier) {
    val next = g.fixtures.filter { !it.played && it.involves(g.userTeamId) }.sortedBy { it.week }.take(3)
    DashCard("Takvim", modifier, onClick = { GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 3 }) {
        if (next.isEmpty()) Text("Planlı maç yok.", color = TextDim, fontSize = 13.sp)
        next.forEachIndexed { i, f ->
            if (i > 0) HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
            val home = f.home == g.userTeamId
            val opp = g.team(if (home) f.away else f.home)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(GameCalendar.format(GameCalendar.dateOf(g, f.week), false), color = TextDim, fontSize = 11.sp, modifier = Modifier.width(78.dp), maxLines = 1)
                TeamBadge(opp, 20.dp)
                Spacer(Modifier.width(6.dp))
                Text(opp.short, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(if (home) "İÇ" else "DIŞ", color = if (home) Green else Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormCard(g: Game, modifier: Modifier) {
    val res = g.fixtures.filter { it.played && it.involves(g.userTeamId) }.sortedBy { it.week }.takeLast(3).reversed()
    DashCard("Son Maçlar", modifier, onClick = { GameRepo.tab = GameRepo.TAB_LEAGUE; GameRepo.leagueTab = 3 }) {
        if (res.isEmpty()) Text("Henüz maç oynanmadı.", color = TextDim, fontSize = 13.sp)
        res.forEach { f -> ResultCompact(g, f) }
    }
}

@Composable
private fun ResultCompact(g: Game, f: Fixture) {
    val h = g.team(f.home)
    val a = g.team(f.away)
    val ug = if (f.home == g.userTeamId) f.hg else f.ag
    val og = if (f.home == g.userTeamId) f.ag else f.hg
    val col = when {
        Comp.isKnockout(f.comp, f.round) -> if (f.winner == g.userTeamId) Green else Red
        ug > og -> Green
        ug == og -> Gold
        else -> Red
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.short, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Spacer(Modifier.width(6.dp))
        TeamBadge(h, 18.dp)
        Box(Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(6.dp)).background(col).padding(horizontal = 7.dp, vertical = 1.dp)) {
            Text("${f.hg}-${f.ag}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        TeamBadge(a, 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(a.short, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

/** Maç hazırlığı kartı: eksikler, gizli antrenman, prim, rakip raporu, hazırlık maçı. */
@Composable
private fun MatchPrepCard(g: Game) {
    val f = g.nextUserFixture() ?: return
    val opp = g.team(if (f.home == g.userTeamId) f.away else f.home)
    val issues = g.userTeam.lineup.mapNotNull { g.player(it) }.filter { !it.available }
    DashCard("Maç Hazırlığı · ${opp.name}", Modifier.fillMaxWidth()) {
        if (issues.isNotEmpty()) {
            Text("⚠️ İlk 11'de oynayamayan: ${issues.joinToString { it.name }}", color = Gold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
        }
        MatchPrep(g)
        Spacer(Modifier.height(6.dp))
        OpponentReport(g, opp)
        if (com.supermenajer.game.game.Matchday.canFriendly(g)) {
            var fr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            SideButton("🤝  Hazırlık maçı oyna (${g.friendlies}/3)", modifier = Modifier.padding(top = 6.dp).height(38.dp)) { fr = GameRepo.friendly() }
            fr?.let { Text("Sonuç: $it", color = Gold, fontSize = 12.sp) }
        }
    }
}
