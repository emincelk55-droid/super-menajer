package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Dynamics
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.ManagerMarket
import com.supermenajer.game.game.Perks
import com.supermenajer.game.game.Generator
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.math.roundToInt

/** Ana sayfada bekleyen beklenmedik olay: menajer bir seçim yapmalı. */
@Composable
fun EventCard(g: Game) {
    val e = g.pendingEvent
    var result by remember { mutableStateOf<String?>(null) }
    if (e == null) {
        result?.let { r ->
            SectionCard("📌 Karar verildi") {
                Text(r, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) { Text("Tamam") }
            }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1)
            .border(1.5.dp, Gold, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        Text("⚡ BEKLENMEDİK OLAY", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            g.player(e.playerId)?.let { p ->
                val t = g.userTeam
                PlayerFace(p, 44.dp, Color(t.color1), Color(t.color2))
                Spacer(Modifier.width(10.dp))
            }
            Text(e.title, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(e.text, fontSize = 13.sp, color = TextMain)
        Spacer(Modifier.height(10.dp))
        e.options.forEachIndexed { i, o ->
            val first = i == 0
            if (first) {
                Button(onClick = { result = GameRepo.resolveEvent(i) }, modifier = Modifier.fillMaxWidth()) { Text(o, fontSize = 13.sp) }
            } else {
                OutlinedButton(onClick = { result = GameRepo.resolveEvent(i) }, modifier = Modifier.fillMaxWidth()) { Text(o, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(4.dp))
        }
        Text("Karar verene kadar takvim ilerlemez.", color = TextDim, fontSize = 11.sp)
    }
}

/** Menajer yetenek ağacı: seviye, deneyim ve yetenekler. */
@Composable
fun PerksView(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(0.8f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("⭐ Menajer seviyesi") {
                val lv = Perks.level(g.managerXp)
                val (cur, need) = Perks.xpToNext(g.managerXp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                        Text("$lv", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.managerName, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("Seviye $lv · toplam ${g.managerXp} XP", color = TextDim, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Bar(cur.toFloat() / need, Gold, Modifier.fillMaxWidth(), 8.dp)
                        Text("Sonraki seviye: $cur / $need XP", color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val pts = Perks.points(g)
                Text(
                    if (pts > 0) "🎁 Harcanabilir $pts yetenek puanın var!" else "Yetenek puanın yok.",
                    color = if (pts > 0) Green else TextDim, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                )
                msg?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
            }
            SectionCard("Nasıl XP kazanılır?") {
                listOf(
                    "Galibiyet" to "+6", "Beraberlik" to "+2", "Başarım açmak" to "+25",
                    "Sezon hedefini tutturmak" to "+60", "Kupa" to "+80", "Lig şampiyonluğu" to "+120", "Avrupa kupası" to "+150",
                ).forEach { (a, b) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(a, color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(b, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Adaptive(250.dp), Modifier.weight(1.4f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Perks.all.size) { i ->
                val p = Perks.all[i]
                val lv = Perks.lvl(g, p.id)
                val can = Perks.points(g) > 0 && lv < p.max
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (lv > 0) Gold.copy(alpha = 0.12f) else Surface1).padding(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.icon, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        repeat(p.max) { k ->
                            Box(Modifier.padding(start = 3.dp).size(10.dp).clip(CircleShape).background(if (k < lv) Gold else Surface2))
                        }
                    }
                    Text(p.desc, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = { msg = GameRepo.upgradePerk(p.id) }, enabled = can,
                        modifier = Modifier.fillMaxWidth().height(36.dp), contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                    ) { Text(if (lv >= p.max) "En üst seviye" else "Geliştir (${lv + 1}/${p.max})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** Menajer pazarı: rakip hocaların koltuk durumu ve boş koltuklara başvuru. */
@Composable
fun ManagerMarketView(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("📋 Boş koltuklar") {
                msg?.let { Text(it, color = Gold, fontSize = 13.sp); Spacer(Modifier.height(6.dp)) }
                if (g.vacancies.isEmpty()) {
                    Text("Şu an boş teknik direktörlük koltuğu yok. Kötü giden kulüplerin hocaları kovulduğunda burada görünür.", color = TextDim, fontSize = 12.sp)
                }
                g.vacancies.forEach { v ->
                    val t = g.teamOrNull(v.teamId) ?: return@forEach
                    val days = (v.until - g.date).coerceAtLeast(0)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        TeamBadge(t, 34.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · itibar ${t.reputation} · $days gün", color = TextDim, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { msg = GameRepo.applyJob(t.id) }, enabled = !v.applied,
                            contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(36.dp),
                        ) { Text(if (v.applied) "Başvuruldu" else "Başvur", fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Kabul edilme şansı itibarına (${g.managerRep}) ve kulübün büyüklüğüne bağlı.", color = TextDim, fontSize = 11.sp)
            }
        }
        val table = g.table(g.userTeam.division)
        Column(Modifier.weight(1.2f).fillMaxHeight()) {
            Text("LİGDEKİ HOCALAR · KOLTUK GÜVENCESİ", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Surface1)) {
                items(table, key = { it.id }) { t ->
                    val me = t.id == g.userTeamId
                    val s = if (me) g.confidence else g.mgrSecurity[t.id] ?: 60.0
                    val col = when { s >= 45 -> Green; s >= 25 -> Gold; else -> Red }
                    Row(
                        Modifier.fillMaxWidth().background(if (me) Blue.copy(alpha = 0.12f) else Color.Transparent).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${g.position(t.id)}.", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(26.dp))
                        TeamBadge(t, 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (me) "${g.managerName} (sen)" else g.managers[t.id] ?: "–", color = TextDim, fontSize = 11.sp, maxLines = 1)
                        }
                        Column(Modifier.width(120.dp), horizontalAlignment = Alignment.End) {
                            Bar((s / 100).toFloat(), col, Modifier.fillMaxWidth(), 6.dp)
                            Text(if (g.vacancies.any { it.teamId == t.id }) "Koltuk boş!" else ManagerMarket.securityText(s), color = col, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Takım dinamikleri: soyunma odası hiyerarşisi, uyum ve gruplar. */
@Composable
fun DynamicsView(g: Game) {
    val t = g.userTeam
    val tiers = Dynamics.tiers(g, t)
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("🤝 Takım uyumu") {
                val c = g.cohesion
                val col = when { c >= 65 -> Green; c >= 40 -> Gold; else -> Red }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${Dynamics.pct(c)}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = col)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Dynamics.cohesionText(c), fontWeight = FontWeight.Bold, color = col)
                        Bar((c / 100).toFloat(), col, Modifier.fillMaxWidth(), 8.dp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val pct = ((Dynamics.matchFactor(g) - 1.0) * 100)
                Text("Maç etkisi: ${if (pct >= 0) "+" else ""}${"%.1f".format(pct)}% güç", color = TextDim, fontSize = 12.sp)
                Text("Yüksek moral uyumu artırır. Çok sayıda yeni transfer ve önemli oyuncu satışları uyumu düşürür.", color = TextDim, fontSize = 11.sp)
            }
            SectionCard("👥 Soyunma odası grupları") {
                val groups = Dynamics.groups(g, t)
                if (groups.isEmpty()) Text("Belirgin bir grup yok.", color = TextDim, fontSize = 12.sp)
                groups.entries.sortedByDescending { it.value.size }.forEach { (name, list) ->
                    val avg = list.sumOf { it.morale } / list.size
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$name (${list.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(list.sortedByDescending { it.overall }.take(4).joinToString(", ") { it.name.substringAfterLast(' ') }, color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(Modifier.width(110.dp), horizontalAlignment = Alignment.End) {
                            Bar((avg / 100).toFloat(), if (avg >= 60) Green else if (avg >= 40) Gold else Red, Modifier.fillMaxWidth(), 6.dp)
                            Text(moraleText(avg), color = TextDim, fontSize = 11.sp)
                        }
                    }
                }
                Text("Etkili oyuncuların morali kendi grubuna yayılır.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        LazyColumn(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (tier in 0..2) {
                val list = tiers[tier].orEmpty()
                item(key = "t$tier") {
                    Text(Dynamics.tierNames[tier].uppercase(), color = if (tier == 0) Gold else TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                }
                items(list, key = { it.id }) { p -> HierarchyRow(g, p, tier) }
            }
        }
    }
}

@Composable
private fun HierarchyRow(g: Game, p: Player, tier: Int) {
    val t = g.userTeam
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (tier == 0) Gold.copy(alpha = 0.10f) else Surface1)
            .clickable { GameRepo.selectedPlayer = p }.padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerFace(p, if (tier == 2) 30.dp else 38.dp, Color(t.color1), Color(t.color2))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text((if (p.id == t.captain) "©️ " else "") + p.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Generator.flag(p.nation)} ${p.age} yaş · ${Dynamics.groupOf(g, t, p)}", color = TextDim, fontSize = 11.sp, maxLines = 1)
        }
        Text(moraleText(p.morale), color = if (p.morale >= 60) Green else if (p.morale >= 40) Gold else Red, fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Text("${Dynamics.influence(g, t, p).roundToInt()}", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp))
    }
}
