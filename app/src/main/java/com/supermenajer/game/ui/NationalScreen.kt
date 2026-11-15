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
            if (com.supermenajer.game.game.Tournament.active(g) || (g.tourSeason == g.season && g.tourTeams.size == 16 && g.seasonOver)) {
                item { TournamentCard(g) }
            }
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
                            Text(L("Milli takım · Teknik direktör ${g.managerName}", "National team · Manager ${g.managerName}"), color = TextDim, fontSize = 12.sp)
                        }
                        RingGauge((National.strengthOf(g, code, squad) / 100).toFloat(), 58.dp, Teal) {
                            Text("${National.strengthOf(g, code, squad).toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        NatStat(L("Bu sezon", "This season"), L("$w G · $d B · $l M", "$w W · $d D · $l L"))
                        NatStat(L("Puan", "Points"), "${g.natPoints}")
                        NatStat(
                            if (com.supermenajer.game.game.Tournament.active(g)) L("Turnuva", "Tournament") else L("Sıradaki pencere", "Next window"),
                            if (com.supermenajer.game.game.Tournament.active(g)) com.supermenajer.game.game.Tournament.stageText(g).substringBefore(" ·") else next?.let { L("$it. hafta", "Week $it") } ?: L("Yaz turnuvası", "Summer tournament"),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        L("Milli maçlar lig takviminin ${National.windows.sorted().joinToString(", ")}. haftalarında oynanır. Sezon sonunda ${if (g.season % 2 == 0) "Avrupa Şampiyonası" else "Dünya Kupası"} var.", "International matches are played in weeks ${National.windows.sorted().joinToString(", ")} of the league calendar. At the end of the season there's ${if (g.season % 2 == 0) "the European Championship" else "the World Cup"}."),
                        color = TextDim, fontSize = 12.sp,
                    )
                }
            }
            if (g.natPending.isNotEmpty()) item { NationalMatchCard(g) }
            item {
                DashCard(L("Diziliş", "Formation"), Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.supermenajer.game.model.Formations.names.forEach { f ->
                            SmallChip(f, g.natFormation == f, Modifier.weight(1f)) { g.natFormation = f; GameRepo.changed() }
                        }
                    }
                    Text(L("Milli maçlarda en iyi 11 bu dizilişe göre otomatik seçilir; maç sırasında değişiklik ve mentaliteyi sen yönetirsin.", "In international matches the best XI is picked automatically for this formation; you manage substitutions and mentality during the match."), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                DashCard(L("Sonuçlar", "Results"), Modifier.fillMaxWidth()) {
                    if (g.natResults.isEmpty()) Text(L("Henüz milli maç oynanmadı.", "No international matches played yet."), color = TextDim, fontSize = 13.sp)
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
                ) { Text(L("Milli takım görevini bırak", "Resign as national team manager"), color = Red, fontWeight = FontWeight.Bold) }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                DashCard(L("Milli Takım Kadrosu (${squad.size})", "National Team Squad (${squad.size})"), Modifier.fillMaxWidth()) {
                    Text(L("Kadro boşsa en iyi oyuncular otomatik çağrılır. Çıkar / çağır ile kendi kadronu kur (en fazla 26).", "If the squad is empty the best players are called up automatically. Drop / call up to build your own squad (max 26)."), color = TextDim, fontSize = 12.sp)
                    for (pos in Pos.entries) {
                        val list = squad.filter { it.pos == pos }.sortedByDescending { it.overall }
                        if (list.isEmpty()) continue
                        Text(pos.label.uppercase(), color = posColor(pos), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                        list.forEach { p -> CallRow(g, p, L("Çıkar", "Drop"), Red) }
                    }
                }
            }
            item {
                DashCard(L("Çağrılabilecek oyuncular", "Players available for call-up"), Modifier.fillMaxWidth()) {
                    if (cand.isEmpty()) Text(L("Başka aday yok.", "No other candidates."), color = TextDim, fontSize = 13.sp)
                    cand.forEach { p -> CallRow(g, p, L("Çağır", "Call up"), Green) }
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
            Text(L("${p.age} yaş · ${t?.name ?: ""}", "${p.age} yrs · ${t?.name ?: ""}"), color = TextDim, fontSize = 11.sp, maxLines = 1)
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
            DashCard(L("🌍 Milli Takım Görevi", "🌍 National Team Job"), Modifier.fillMaxWidth()) {
                Text(
                    L("Kulübünle birlikte bir milli takımı da çalıştırabilirsin. Güçlü uluslar daha yüksek menajer itibarı ister. Senin itibarın: ${g.managerRep}.", "You can manage a national team alongside your club. Stronger nations require a higher manager reputation. Your reputation: ${g.managerRep}."),
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
                Text(L("Güç ${str[code]}", "Strength ${str[code]}"), fontSize = 12.sp, color = TextDim, modifier = Modifier.width(70.dp))
                Text(L("İtibar $need+", "Reputation $need+"), fontSize = 12.sp, color = if (ok) Green else Red, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                Box(
                    Modifier.width(96.dp).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (ok) Teal else Color.Transparent).border(1.dp, if (ok) Teal else CardLine, RoundedCornerShape(8.dp))
                        .clickable(enabled = ok) { msg = GameRepo.takeNationalJob(code) },
                    contentAlignment = Alignment.Center,
                ) { Text(L("Görevi al", "Take the job"), color = if (ok) Color(0xFF06231A) else TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}


/** Yaz turnuvası: 4 grup tablosu ve eleme turu sonuçları. */
@Composable
fun TournamentCard(g: Game) {
    val T = com.supermenajer.game.game.Tournament
    val me = g.nationalTeam
    DashCard("🏆 ${T.title(g)} · ${T.stageText(g)}", Modifier.fillMaxWidth()) {
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0..1) {
                    val gi = row * 2 + col
                    Column(Modifier.weight(1f).padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0B1028)).padding(6.dp)) {
                        Row {
                            Text(L("${T.groupNames[gi]} Grubu", "Group ${T.groupNames[gi]}"), fontWeight = FontWeight.Black, fontSize = 12.sp, color = Teal, modifier = Modifier.weight(1f))
                            Text("AV", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                            Text("P", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                        }
                        T.standings(g, gi).forEachIndexed { i, c ->
                            val mine = c == me
                            Row(
                                Modifier.fillMaxWidth().background(if (mine) Teal.copy(alpha = 0.15f) else Color.Transparent).padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(4.dp, 14.dp).background(if (i < 2 && g.tourStage >= 3) Green else Color.Transparent))
                                Spacer(Modifier.width(4.dp))
                                Text("${Generator.flag(c)} ${National.name(c)}", fontSize = 12.sp, fontWeight = if (mine) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                val gd = (g.tourGf[c] ?: 0) - (g.tourGa[c] ?: 0)
                                Text(if (gd > 0) "+$gd" else "$gd", fontSize = 11.sp, color = TextDim, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                                Text("${g.tourPts[c] ?: 0}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
        val ko = g.tourLog.map { it.split("|") }.filter { it.size >= 2 && (it[0].toIntOrNull() ?: 0) >= 3 }
        if (ko.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(L("ELEME TURLARI", "KNOCKOUT ROUNDS"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val names = listOf(L("Çeyrek Final", "Quarter-final"), L("Yarı Final", "Semi-final"), L("Final", "Final"))
            ko.forEach { (st, line) ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(names.getOrElse(st.toInt() - 3) { "" }, color = TextDim, fontSize = 11.sp, modifier = Modifier.width(84.dp))
                    Text(line, fontSize = 12.sp, fontWeight = if (National.name(me) in line) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
