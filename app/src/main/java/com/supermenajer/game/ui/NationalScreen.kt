package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.National
import com.supermenajer.game.game.Positions
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos

/** Milli takım: görev teklifleri ya da kadro, sonuçlar ve sıradaki pencere. */
@Composable
fun NationalTab(g: Game) {
    GameRepo.version
    if (g.nationalTeam.isEmpty()) { NationalOffers(g); return }
    val code = g.nationalTeam
    val squad = National.squad(g)
    val ids = squad.map { it.id }.toSet()
    val cand = remember(GameRepo.version) {
        g.players.filter { it.nation == code && it.teamId >= 0 && it.id !in ids }.sortedByDescending { it.overall }.take(20)
    }
    val week = g.leagueWeekAt(g.week)
    val next = National.windows.filter { it > week }.minOrNull()
    val season = g.natResults.map { it.split("|") }.filter { it.size >= 4 && it[0] == "${g.season}" }
    val w = season.count { it[3] == "3" }
    val d = season.count { it[3] == "1" }
    val l = season.size - w - d
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF3A1F6E), CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp)).padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Generator.flag(code), fontSize = 44.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(National.name(code).uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("Milli takım · Teknik direktör ${g.managerName}", color = TextDim, fontSize = 12.sp)
                        }
                        RingGauge((National.strengthOf(g, code, squad) / 100).toFloat(), 58.dp, Teal) {
                            Text("${National.strengthOf(g, code, squad).toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        NatStat("Bu sezon", "$w G · $d B · $l M")
                        NatStat("Puan", "${g.natPoints}")
                        NatStat("Sıradaki pencere", next?.let { "$it. hafta" } ?: "Yaz turnuvası")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Milli maçlar lig takviminin ${National.windows.sorted().joinToString(", ")}. haftalarında oynanır. Sezon sonunda ${if (g.season % 2 == 0) "Avrupa Şampiyonası" else "Dünya Kupası"} var.",
                        color = TextDim, fontSize = 12.sp,
                    )
                }
            }
            if (g.natPending.isNotEmpty()) item { NationalMatchCard(g) }
            item {
                DashCard("Diziliş", Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.supermenajer.game.model.Formations.names.forEach { f ->
                            SmallChip(f, g.natFormation == f, Modifier.weight(1f)) { g.natFormation = f; GameRepo.changed() }
                        }
                    }
                    Text("Milli maçlarda en iyi 11 bu dizilişe göre otomatik seçilir; maç sırasında değişiklik ve mentaliteyi sen yönetirsin.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                DashCard("Sonuçlar", Modifier.fillMaxWidth()) {
                    if (g.natResults.isEmpty()) Text("Henüz milli maç oynanmadı.", color = TextDim, fontSize = 13.sp)
                    g.natResults.take(10).forEachIndexed { i, r ->
                        val c = r.split("|")
                        if (c.size < 4) return@forEachIndexed
                        if (i > 0) HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(when (c[3]) { "3" -> Green; "1" -> Gold; else -> Red }))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c[2], fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${c[0]} · ${c[1]}", color = TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Red.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { GameRepo.resignNational() },
                    contentAlignment = Alignment.Center,
                ) { Text("Milli takım görevini bırak", color = Red, fontWeight = FontWeight.Bold) }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                DashCard("Milli Takım Kadrosu (${squad.size})", Modifier.fillMaxWidth()) {
                    Text("Kadro boşsa en iyi oyuncular otomatik çağrılır. Çıkar / çağır ile kendi kadronu kur (en fazla 26).", color = TextDim, fontSize = 12.sp)
                    for (pos in Pos.entries) {
                        val list = squad.filter { it.pos == pos }.sortedByDescending { it.overall }
                        if (list.isEmpty()) continue
                        Text(pos.label.uppercase(), color = posColor(pos), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                        list.forEach { p -> CallRow(g, p, "Çıkar", Red) }
                    }
                }
            }
            item {
                DashCard("Çağrılabilecek oyuncular", Modifier.fillMaxWidth()) {
                    if (cand.isEmpty()) Text("Başka aday yok.", color = TextDim, fontSize = 13.sp)
                    cand.forEach { p -> CallRow(g, p, "Çağır", Green) }
                }
            }
        }
    }
}

@Composable
private fun NatStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Teal)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun CallRow(g: Game, p: Player, action: String, c: Color) {
    val t = g.teamOrNull(p.teamId)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Positions.det(p).code, color = posColor(p.pos), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f).clickable { GameRepo.selectedPlayer = p }) {
            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${p.age} yaş · ${t?.name ?: ""}", color = TextDim, fontSize = 11.sp, maxLines = 1)
        }
        OvrBadge(p.overall, 26.dp)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.width(64.dp).height(28.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c, RoundedCornerShape(8.dp))
                .clickable { GameRepo.toggleCallUp(p) },
            contentAlignment = Alignment.Center,
        ) { Text(action, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

/** Görev yokken: ulusların listesi, güçleri ve gereken itibar. */
@Composable
private fun NationalOffers(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    val nations = remember { National.nations(g) }
    val str = remember { nations.associateWith { National.strengthOf(g, it).toInt() } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)) {
        item {
            DashCard("🌍 Milli Takım Görevi", Modifier.fillMaxWidth()) {
                Text(
                    "Kulübünle birlikte bir milli takımı da çalıştırabilirsin. Güçlü uluslar daha yüksek menajer itibarı ister. Senin itibarın: ${g.managerRep}.",
                    color = TextDim, fontSize = 13.sp,
                )
                msg?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        items(nations.size) { i ->
            val code = nations[i]
            val need = National.requiredRep(i)
            val ok = g.managerRep >= need
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF111735)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}.", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                Text(Generator.flag(code), fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(National.name(code), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("Güç ${str[code]}", fontSize = 12.sp, color = TextDim, modifier = Modifier.width(70.dp))
                Text("İtibar $need+", fontSize = 12.sp, color = if (ok) Green else Red, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                Box(
                    Modifier.width(96.dp).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (ok) Teal else Color.Transparent).border(1.dp, if (ok) Teal else CardLine, RoundedCornerShape(8.dp))
                        .clickable(enabled = ok) { msg = GameRepo.takeNationalJob(code) },
                    contentAlignment = Alignment.Center,
                ) { Text("Görevi al", color = if (ok) Color(0xFF06231A) else TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}
