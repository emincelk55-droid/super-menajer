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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Season
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
    val mode = GameRepo.tacticsMode
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Taktik", mode == 0) { GameRepo.tacticsMode = 0 }
            Chip("Antrenman", mode == 1) { GameRepo.tacticsMode = 1 }
        }
        Spacer(Modifier.height(4.dp))
        if (mode == 0) TacticsBoard(g) else TrainingContent(g)
    }
}

@Composable
private fun TrainingContent(g: Game) {
    val t = g.userTeam
    val squad = g.squad(t.id)
    val focusInfo = listOf(
        "Tüm özellikler mevkiye göre dengeli gelişir.",
        "Şut, teknik ve pas çalışılır.",
        "Savunma ve fizik çalışılır.",
        "Hız ve fizik çalışılır. Kondisyon artar.",
        "21 yaş altı oyuncular çok daha hızlı gelişir, diğerleri yavaşlar.",
    )
    val intensityInfo = listOf(
        "Gelişim yavaş, oyuncular maçlar arasında daha iyi dinlenir, sakatlık riski düşük.",
        "Dengeli gelişim ve dinlenme.",
        "Gelişim hızlı, ama oyuncular daha yorgun kalır ve antrenmanda sakatlanma riski artar.",
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("Haftalık antrenman odağı") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.supermenajer.game.game.Season.trainingNames.forEachIndexed { i, n -> Chip(n, t.training == i) { GameRepo.setTraining(i) } }
                }
                Spacer(Modifier.height(8.dp))
                Text(focusInfo[t.training], color = TextDim, fontSize = 12.sp)
            }
        }
        item {
            SectionCard("Yoğunluk") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.supermenajer.game.game.Season.intensityNames.forEachIndexed { i, n -> Chip(n, t.intensity == i, Modifier.weight(1f)) { GameRepo.setIntensity(i) } }
                }
                Spacer(Modifier.height(8.dp))
                Text(intensityInfo[t.intensity], color = TextDim, fontSize = 12.sp)
            }
        }
        item { Text("BU SEZON GELİŞENLER", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        val devs = squad.filter { it.startOvr > 0 && it.overall != it.startOvr }.sortedByDescending { it.overall - it.startOvr }
        if (devs.isEmpty()) item { Text("Henüz değişim yok. Gençler maç oynadıkça ve antrenmanla gelişir.", color = TextDim, fontSize = 13.sp) }
        items(devs, key = { it.id }) { p ->
            val d = p.overall - p.startOvr
            PlayerRow(g, p, p.id in t.lineup, extra = "${p.startOvr} → ${p.overall} (${if (d > 0) "+" else ""}$d) · Pot. ${p.potential}")
        }
    }
}

@Composable
fun LineStat(label: String, v: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OvrBadge(v, 40.dp)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
fun LeagueTab(g: Game) {
    val Comp = com.supermenajer.game.model.Comp
    val Leagues = com.supermenajer.game.model.Leagues
    if (GameRepo.leagueTab < 0) GameRepo.leagueTab = 0
    if (GameRepo.leagueSel < 0 || Leagues.of(GameRepo.leagueSel) == null) GameRepo.leagueSel = g.userTeam.division
    val view = GameRepo.leagueTab
    val sel = GameRepo.leagueSel
    var day by remember { mutableIntStateOf(g.week.coerceIn(1, g.totalDays)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Puan Durumu", "Kupa", "Avrupa", "Fikstür", "Krallık", "Geçmiş").forEachIndexed { i, n ->
                Chip(n, view == i) { GameRepo.leagueTab = i }
            }
        }
        if (view == 0 || view == 4) {
            // Lig seçici: önce ülke, sonra lig
            val country = Leagues.of(sel)?.country ?: g.userCountry
            Row(
                Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Season.activeCountries(g).forEach { c ->
                    Chip(Generator.flag(Leagues.flagOf(c)), c == country) { GameRepo.leagueSel = Leagues.top(c).id }
                }
                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(28.dp).background(Line))
                Leagues.ofCountry(country).filter { it.id in Season.activeLeagues(g) }.forEach { lg ->
                    Chip(lg.name, lg.id == sel) { GameRepo.leagueSel = lg.id }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        when (view) {
            0 -> LeagueTable(g, sel)
            1 -> CupView(g, Leagues.of(g.userTeam.division)?.cupComp ?: Comp.CUP)
            2 -> EuroView(g)
            3 -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (day > 1) day-- }) { Text("◀", fontSize = 18.sp) }
                    Text(g.dayLabel(day), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { if (day < g.totalDays) day++ }) { Text("▶", fontSize = 18.sp) }
                }
                val myCup = Leagues.of(g.userTeam.division)?.cupComp ?: Comp.CUP
                val country = g.userCountry
                val myLeagues = Leagues.ofCountry(country).map { it.id }
                val list = g.weekFixtures(day)
                val comps = listOf(Comp.EURO, myCup) + myLeagues
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    for (comp in comps) {
                        val fs = list.filter { it.comp == comp }.sortedByDescending { it.involves(g.userTeamId) }
                        if (fs.isEmpty()) continue
                        item { Text(Comp.name(comp).uppercase(), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
                        items(fs) { f ->
                            ResultRow(g, f, g.userTeamId)
                            HorizontalDivider(color = Line)
                        }
                    }
                    if (list.none { it.comp in comps }) item { Text("Bu gün ${country} için maç yok ya da kura henüz çekilmedi.", color = TextDim) }
                }
            }
            4 -> Scorers(g, sel)
            else -> HistoryView(g)
        }
    }
}

@Composable
private fun HistoryView(g: Game) {
    val h = g.history.reversed()
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val titles = g.history.count { it.userPos == 1 && it.champion == it.userTeam }
            val cups = g.history.count { it.cupWinner == it.userTeam }
            val euros = g.history.count { it.euroWinner == it.userTeam }
            SectionCard("Kariyer · ${g.managerName}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Sezon" to g.history.size, "Lig 🏆" to titles, "Kupa 🥇" to cups, "Avrupa 🌍" to euros).forEach { (l, v) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$v", fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(l, color = TextDim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (h.isEmpty()) item { Text("İlk sezonun bitince kariyer geçmişin burada görünecek.", color = TextDim) }
        items(h) { r ->
            SectionCard("${r.season}-${(r.season + 1) % 100} · ${r.userTeam}") {
                Text("${r.leagueName.ifEmpty { "Lig" }}: ${r.userPos}. sıra" + if (r.userPos == 1) " 🏆" else "", fontWeight = FontWeight.Bold)
                Text("Şampiyon: ${r.champion}", color = TextDim, fontSize = 13.sp)
                if (r.cupWinner.isNotEmpty()) Text("Kupa: ${r.cupWinner}", color = TextDim, fontSize = 13.sp)
                if (r.euroWinner.isNotEmpty()) Text("Avrupa: ${r.euroWinner}", color = TextDim, fontSize = 13.sp)
                Text("Gol kralı: ${r.topScorer}", color = TextDim, fontSize = 13.sp)
                if (r.awards.isNotEmpty()) Text("🏅 ${r.awards}", color = Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EuroView(g: Game) {
    val Comp = com.supermenajer.game.model.Comp
    val ko = g.fixtures.filter { it.comp == Comp.EURO && it.round >= Comp.EURO_KO }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (g.euroWinner >= 0) item {
            SectionCard("Avrupa'nın en büyüğü") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    TeamBadge(g.team(g.euroWinner), 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(g.team(g.euroWinner).name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
        if (g.euroTeams.isEmpty()) {
            item {
                SectionCard("Avrupa Kupası") {
                    Text("Bu sezon Avrupa Kupası oynanmıyor.", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Süper Lig'i ilk 4'te bitiren takımlar gelecek sezon Avrupa'nın 28 dev kulübüyle yarışır: 8 grup, ardından son 16'dan finale tek maç eleme.", color = TextDim, fontSize = 13.sp)
                }
            }
            return@LazyColumn
        }
        for (r in Comp.euroRounds.indices.reversed().filter { it >= Comp.EURO_KO }) {
            val fs = ko.filter { it.round == r }.sortedByDescending { it.involves(g.userTeamId) || !g.isForeign(g.team(it.home)) || !g.isForeign(g.team(it.away)) }
            if (fs.isEmpty()) continue
            item {
                SectionCard(Comp.euroRounds[r]) {
                    fs.forEachIndexed { i, f ->
                        if (i > 0) HorizontalDivider(color = Line)
                        ResultRow(g, f, g.userTeamId)
                    }
                }
            }
        }
        // Kullanıcının ve Türk takımlarının grupları önce
        val order = (0 until 8).sortedBy { gi ->
            val ids = g.euroTeams.subList(gi * 4, gi * 4 + 4)
            when {
                g.userTeamId in ids -> 0
                ids.any { !g.isForeign(g.team(it)) } -> 1
                else -> 2
            }
        }
        for (gi in order) item { EuroGroupCard(g, gi) }
    }
}

@Composable
private fun CupView(g: Game, comp: Int) {
    val cup = g.fixtures.filter { it.comp == comp }
    val winner = g.cupWinners[comp] ?: -1
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(com.supermenajer.game.model.Comp.name(comp), fontWeight = FontWeight.Black, fontSize = 18.sp) }
        if (winner >= 0) item {
            SectionCard("Kupa sahibi") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    TeamBadge(g.team(winner), 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(g.team(winner).name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
        if (cup.isEmpty()) item { Text("Kupa yeni sezonda başlayacak.", color = TextDim) }
        for (r in com.supermenajer.game.model.Comp.cupRounds.indices.reversed()) {
            val fs = cup.filter { it.round == r }.sortedByDescending { it.involves(g.userTeamId) }
            if (fs.isEmpty()) continue
            item {
                SectionCard("${com.supermenajer.game.model.Comp.cupRounds[r]} · ${g.leagueWeekAt(fs.first().week)}. hafta sonrası") {
                    fs.forEachIndexed { i, f ->
                        if (i > 0) HorizontalDivider(color = Line)
                        ResultRow(g, f, g.userTeamId)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueTable(g: Game, div: Int) {
    val table = g.table(div)
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
            val lgDef = com.supermenajer.game.model.Leagues.of(div)
            val relegation = lgDef != null && lgDef.tier == 1 && com.supermenajer.game.model.Leagues.second(lgDef.country) != null
            val zone = if (lgDef?.tier != 2) when {
                i == 0 -> Gold
                i < 4 -> Blue
                relegation && i >= table.size - 3 -> Red
                else -> Color.Transparent
            } else if (i < 3) Green else Color.Transparent
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (me) Green.copy(alpha = 0.15f) else Color.Transparent)
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
                val lgDef = com.supermenajer.game.model.Leagues.of(div)
                if (lgDef?.tier != 2) {
                    Legend(Gold, "Şampiyon"); Legend(Blue, "Avrupa")
                    if (lgDef != null && com.supermenajer.game.model.Leagues.second(lgDef.country) != null) Legend(Red, "Küme düşme")
                } else Legend(Green, "Yükselme")
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
private fun Scorers(g: Game, div: Int) {
    val active = g.divisionTeams(div).flatMap { g.squad(it.id) }
    val goals = active.filter { it.goals > 0 }.sortedWith(compareByDescending<com.supermenajer.game.model.Player> { it.goals }.thenBy { it.apps }).take(15)
    val assists = active.filter { it.assists > 0 }.sortedByDescending { it.assists }.take(10)
    val rating = active.filter { it.apps >= 5 }.sortedByDescending { it.avgRating }.take(10)
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("${com.supermenajer.game.model.Comp.name(div)} · tüm kulvarlar", color = TextDim, fontSize = 12.sp) }
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
