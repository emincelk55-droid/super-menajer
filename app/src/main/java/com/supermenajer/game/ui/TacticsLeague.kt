package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game

/** Saha çizimi. vertical=true ise kaleler üstte ve altta. */
fun DrawScope.drawPitch(vertical: Boolean) {
    val w = size.width
    val h = size.height
    val stripes = 10
    for (i in 0 until stripes) {
        val c = if (i % 2 == 0) Color(0xFF1E6B3C) else Color(0xFF237544)
        if (vertical) drawRect(c, Offset(0f, h * i / stripes), Size(w, h / stripes + 1))
        else drawRect(c, Offset(w * i / stripes, 0f), Size(w / stripes + 1, h))
    }
    val line = Color.White.copy(alpha = 0.55f)
    val sw = 2.dp.toPx()
    val m = 6.dp.toPx()
    drawRect(line, Offset(m, m), Size(w - 2 * m, h - 2 * m), style = Stroke(sw))
    if (vertical) {
        drawLine(line, Offset(m, h / 2), Offset(w - m, h / 2), sw)
        drawCircle(line, w * 0.14f, Offset(w / 2, h / 2), style = Stroke(sw))
        val bw = w * 0.56f; val bh = h * 0.15f
        drawRect(line, Offset((w - bw) / 2, m), Size(bw, bh), style = Stroke(sw))
        drawRect(line, Offset((w - bw) / 2, h - m - bh), Size(bw, bh), style = Stroke(sw))
        val gw = w * 0.26f; val gh = h * 0.055f
        drawRect(line, Offset((w - gw) / 2, m), Size(gw, gh), style = Stroke(sw))
        drawRect(line, Offset((w - gw) / 2, h - m - gh), Size(gw, gh), style = Stroke(sw))
    } else {
        drawLine(line, Offset(w / 2, m), Offset(w / 2, h - m), sw)
        drawCircle(line, h * 0.16f, Offset(w / 2, h / 2), style = Stroke(sw))
        val bw = w * 0.15f; val bh = h * 0.56f
        drawRect(line, Offset(m, (h - bh) / 2), Size(bw, bh), style = Stroke(sw))
        drawRect(line, Offset(w - m - bw, (h - bh) / 2), Size(bw, bh), style = Stroke(sw))
        val gw = w * 0.055f; val gh = h * 0.26f
        drawRect(line, Offset(m, (h - gh) / 2), Size(gw, gh), style = Stroke(sw))
        drawRect(line, Offset(w - m - gw, (h - gh) / 2), Size(gw, gh), style = Stroke(sw))
    }
    drawCircle(line, 3.dp.toPx(), Offset(w / 2, h / 2))
}

@Composable
fun TacticsTab(g: Game) {
    val t = g.userTeam
    var sel by remember { mutableIntStateOf(-1) }
    val slots = Formations.slots(t.formation)
    val (atk, mid, def) = Lineups.lineStrength(g, t)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Formations.names.forEach { f -> Chip(f, t.formation == f) { sel = -1; GameRepo.setFormation(f) } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LineStat("Hücum", atk); LineStat("Orta Saha", mid); LineStat("Savunma", def)
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(0.78f).clip(RoundedCornerShape(12.dp))) {
                val bw = maxWidth
                val bh = maxHeight
                Canvas(Modifier.fillMaxSize()) { drawPitch(vertical = true) }
                slots.forEachIndexed { i, s ->
                    val p = g.player(t.lineup.getOrNull(i) ?: -1)
                    val cx = bw * s.x
                    val cy = bh * (1f - s.y) - bh * 0.02f
                    val selected = sel == i
                    Column(
                        Modifier.offset(x = cx - 36.dp, y = cy - 26.dp).width(72.dp)
                            .clickable {
                                when {
                                    sel == -1 -> sel = i
                                    sel == i -> sel = -1
                                    else -> { GameRepo.swapSlots(sel, i); sel = -1 }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val mismatch = p != null && p.pos != s.pos
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (p == null) Color.DarkGray else ovrColor(p.ovrAt(s.pos)))
                                .border(3.dp, if (selected) Color.White else if (mismatch) Red else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(p?.ovrAt(s.pos)?.toString() ?: "?", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Text(
                            p?.name?.substringAfterLast(' ') ?: "Boş", fontSize = 11.sp, color = Color.White, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            Text(
                if (sel >= 0) "Seçili mevki: ${slots[sel].pos.label}. Aşağıdan bir oyuncuya ya da sahada başka bir oyuncuya dokun."
                else "Değiştirmek için sahadaki bir oyuncuya dokun. Kırmızı çerçeve: oyuncu kendi mevkisinde değil.",
                color = if (sel >= 0) Gold else TextDim, fontSize = 12.sp,
            )
        }
        item {
            SectionCard("Oyun anlayışı") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Defansif", "Dengeli", "Hücum").forEachIndexed { i, s -> Chip(s, t.mentality == i, Modifier.weight(1f)) { GameRepo.setMentality(i) } }
                }
                Spacer(Modifier.height(10.dp))
                Text("Pres", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Düşük", "Orta", "Yüksek").forEachIndexed { i, s -> Chip(s, t.pressing == i, Modifier.weight(1f)) { GameRepo.setPressing(i) } }
                }
                Spacer(Modifier.height(6.dp))
                Text("Yüksek pres topu kazanmayı kolaylaştırır ama oyuncular daha çabuk yorulur.", color = TextDim, fontSize = 11.sp)
            }
        }
        item {
            OutlinedButton(onClick = { sel = -1; GameRepo.autoLineup() }, modifier = Modifier.fillMaxWidth()) { Text("🪄 Otomatik En İyi 11") }
        }
        item {
            Text(if (sel >= 0) "MEVKİYE OYUNCU SEÇ" else "YEDEKLER", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val pos = if (sel >= 0) slots[sel].pos else null
        val pool = g.squad(t.id).filter { it.id !in t.lineup }
            .sortedWith(compareByDescending<com.supermenajer.game.model.Player> { pos != null && it.pos == pos }.thenByDescending { if (pos != null) it.ovrAt(pos) else it.overall })
        items(pool, key = { it.id }) { p ->
            val extra = if (pos != null) "Bu mevkide: ${p.ovrAt(pos)}" else null
            PlayerRow(g, p, false, extra) {
                if (sel >= 0 && p.available) { GameRepo.putInSlot(sel, p.id); sel = -1 } else GameRepo.selectedPlayer = p
            }
        }
    }
}

@Composable
private fun LineStat(label: String, v: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OvrBadge(v, 40.dp)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
fun LeagueTab(g: Game) {
    var tab by remember { mutableIntStateOf(0) }
    var week by remember { mutableIntStateOf(g.week.coerceIn(1, g.totalWeeks)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Puan Durumu", tab == 0) { tab = 0 }
            Chip("Fikstür", tab == 1) { tab = 1 }
            Chip("Krallık", tab == 2) { tab = 2 }
        }
        when (tab) {
            0 -> LeagueTable(g)
            1 -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (week > 1) week-- }) { Text("◀", fontSize = 18.sp) }
                    Text("$week. Hafta", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { if (week < g.totalWeeks) week++ }) { Text("▶", fontSize = 18.sp) }
                }
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    items(g.weekFixtures(week)) { f ->
                        ResultRow(g, f, g.userTeamId)
                        HorizontalDivider(color = Line)
                    }
                }
            }
            else -> Scorers(g)
        }
    }
}

@Composable
private fun LeagueTable(g: Game) {
    val table = g.table()
    LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
        item {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                Text("#", Modifier.width(26.dp), color = TextDim, fontSize = 12.sp)
                Text("Takım", Modifier.weight(1f), color = TextDim, fontSize = 12.sp)
                listOf("O", "G", "B", "M", "AV").forEach { Text(it, Modifier.width(28.dp), color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center) }
                Text("P", Modifier.width(32.dp), color = TextDim, fontSize = 12.sp, textAlign = TextAlign.End)
            }
        }
        items(table.size) { i ->
            val t = table[i]
            val me = t.id == g.userTeamId
            val zone = when {
                i == 0 -> Gold
                i < 4 -> Blue
                i >= table.size - 3 -> Red
                else -> Color.Transparent
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (me) Green.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(20.dp).background(zone))
                Text("${i + 1}", Modifier.width(23.dp).padding(start = 5.dp), fontSize = 13.sp)
                TeamBadge(t, 22.dp)
                Spacer(Modifier.width(6.dp))
                Text(t.name, Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (me) FontWeight.Bold else FontWeight.Normal)
                listOf(t.played, t.won, t.drawn, t.lost, t.gd).forEach {
                    Text("$it", Modifier.width(28.dp), fontSize = 13.sp, textAlign = TextAlign.Center, color = TextDim)
                }
                Text("${t.points}", Modifier.width(32.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
        item {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(Gold, "Şampiyon"); Legend(Blue, "Avrupa"); Legend(Red, "Düşme hattı")
            }
        }
    }
}

@Composable
private fun Legend(c: Color, s: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(c, RoundedCornerShape(2.dp)))
        Text(" $s", fontSize = 11.sp, color = TextDim)
    }
}

@Composable
private fun Scorers(g: Game) {
    val active = g.players.filter { it.teamId >= 0 }
    val goals = active.filter { it.goals > 0 }.sortedWith(compareByDescending<com.supermenajer.game.model.Player> { it.goals }.thenBy { it.apps }).take(15)
    val assists = active.filter { it.assists > 0 }.sortedByDescending { it.assists }.take(10)
    val rating = active.filter { it.apps >= 5 }.sortedByDescending { it.avgRating }.take(10)
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { RankCard(g, "Gol Krallığı", goals) { "${it.goals}" } }
        item { RankCard(g, "Asist", assists) { "${it.assists}" } }
        item { RankCard(g, "En Yüksek Ortalama (min. 5 maç)", rating) { "%.2f".format(it.avgRating) } }
    }
}

@Composable
private fun RankCard(g: Game, title: String, list: List<com.supermenajer.game.model.Player>, value: (com.supermenajer.game.model.Player) -> String) {
    SectionCard(title) {
        if (list.isEmpty()) Text("Henüz veri yok.", color = TextDim)
        list.forEachIndexed { i, p ->
            val t = g.team(p.teamId)
            Row(
                Modifier.fillMaxWidth().clickable { GameRepo.selectedPlayer = p }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}.", Modifier.width(26.dp), color = TextDim, fontSize = 13.sp)
                TeamBadge(t, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(p.name, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (t.id == g.userTeamId) FontWeight.Bold else FontWeight.Normal,
                    color = if (t.id == g.userTeamId) Green else TextMain)
                Text(value(p), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
