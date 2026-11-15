package com.supermenajer.game.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Facilities
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Project
import com.supermenajer.game.model.Team

private fun num(n: Int) = "%,d".format(n).replace(',', '.')

@Composable
fun FacilitiesView(g: Game) {
    @Suppress("UNUSED_VARIABLE") val v = GameRepo.version
    var message by remember { mutableStateOf<String?>(null) }
    val us = g.userTeam
    Facilities.ensure(us)
    LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(330.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Text(L("Transfer bütçesi: ${money(us.budget)}", "Transfer budget: ${money(us.budget)}"), color = TextDim, fontSize = 12.sp)
            message?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
        item(key = "stadium") { StadiumCard(g, us) { message = it } }
        for (type in listOf(Facilities.TRAINING, Facilities.YOUTH, Facilities.MEDICAL)) {
            item(key = "fac$type") { FacilityCard(g, us, type) { message = it } }
        }
    }
}

@Composable
private fun ProjectProgress(p: Project) {
    val done = p.totalWeeks - p.weeksLeft
    Text(L("🏗️ İnşaat sürüyor · ${p.weeksLeft} hafta kaldı", "🏗️ Under construction · ${p.weeksLeft} weeks left"), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Bar(done / p.totalWeeks.toFloat(), Gold, Modifier.fillMaxWidth(), 8.dp)
}

@Composable
private fun StadiumCard(g: Game, us: Team, onMessage: (String) -> Unit) {
    SectionCard(L("🏟️ Stadyum", "🏟️ Stadium")) {
        Text(us.stadiumName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(L("Kapasite: ${num(us.stadium)} kişi", "Capacity: ${num(us.stadium)}"), color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        val avg = Facilities.avgAttendance(us)
        val fill = if (us.stadium > 0) avg / us.stadium.toFloat() else 0f
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(L("Son maç", "Last match"), color = TextDim, fontSize = 11.sp)
                Text(if (us.lastAttendance > 0) num(us.lastAttendance) else "-", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(L("Ortalama", "Average"), color = TextDim, fontSize = 11.sp)
                Text(if (avg > 0) num(avg) else "-", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(L("Doluluk", "Occupancy"), color = TextDim, fontSize = 11.sp)
                Text(if (avg > 0) "%${(fill * 100).toInt()}" else "-", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (fill >= 0.95f) Green else TextMain)
            }
        }
        Spacer(Modifier.height(6.dp))
        Bar(fill, if (fill >= 0.95f) Green else Blue, Modifier.fillMaxWidth(), 8.dp)
        if (fill >= 0.97f) Text(L("Stat her maç doluyor, büyütmenin tam zamanı!", "The stadium is full every match, perfect time to expand!"), color = Green, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))
        Text(L("Bilet fiyatı", "Ticket price"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Facilities.priceNames.forEachIndexed { i, name ->
                Chip(name, us.ticketPrice == i) { GameRepo.setTicketPrice(i) }
            }
        }
        Text(
            L("Seyirci başı ≈ ${money(Facilities.perFan(us).toLong())} · ", "Per fan ≈ ${money(Facilities.perFan(us).toLong())} · ") + when (us.ticketPrice) {
                0 -> L("Stat daha çok dolar, gelir azalır.", "Fuller stadium, lower income.")
                2 -> L("Daha az seyirci, ama kişi başı gelir yüksek.", "Fewer fans, but higher income per head.")
                else -> L("Dengeli fiyat.", "Balanced price.")
            },
            color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(12.dp))
        val proj = g.projects.firstOrNull { it.type == Facilities.STADIUM }
        if (proj != null) {
            ProjectProgress(proj)
            Text(L("+${num(proj.amount)} koltuk ekleniyor.", "+${num(proj.amount)} seats being added."), color = TextDim, fontSize = 12.sp)
        } else {
            Text(L("Stadyumu büyüt", "Expand the stadium"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Facilities.stadiumOptions(us).forEachIndexed { i, o ->
                OutlinedButton(
                    onClick = { onMessage(GameRepo.startProject(Facilities.STADIUM, i)) },
                    enabled = us.budget >= o.cost && us.stadium + o.seats <= 90_000,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                ) {
                    Text(L("+${num(o.seats)} koltuk · ${money(o.cost)} · ${o.weeks} hafta", "+${num(o.seats)} seats · ${money(o.cost)} · ${o.weeks} weeks"), fontSize = 13.sp)
                }
            }
        }
    }
}

private fun effectText(type: Int, level: Int): String = when (type) {
    Facilities.TRAINING -> L("Oyuncu gelişimi: %${(Facilities.trainingFactor(level) * 100).toInt()}", "Player development: %${(Facilities.trainingFactor(level) * 100).toInt()}")
    Facilities.YOUTH -> {
        val b = Facilities.youthBonus(level)
        L("Genç kalitesi ${if (b >= 0) "+" else ""}${"%.0f".format(b)} · altın çocuk şansı %${"%.1f".format(Facilities.wonderkidChance(level) * 100)} · sezonda ${1 + level / 4} genç", "Youth quality ${if (b >= 0) "+" else ""}${"%.0f".format(b)} · wonderkid chance %${"%.1f".format(Facilities.wonderkidChance(level) * 100)} · ${1 + level / 4} youngsters a season")
    }
    else -> L("Sakatlık riski: %${(Facilities.medicalRisk(level) * 100).toInt()} · hızlı iyileşme şansı %${(Facilities.medicalHeal(level) * 100).toInt()}", "Injury risk: %${(Facilities.medicalRisk(level) * 100).toInt()} · quick recovery chance %${(Facilities.medicalHeal(level) * 100).toInt()}")
}

private val facInfo get() = mapOf(
    Facilities.TRAINING to L("Daha iyi tesis, oyuncuların haftalık gelişimini hızlandırır.", "Better facilities speed up players' weekly development."),
    Facilities.YOUTH to L("Akademi, her sezon başında A takıma çıkan gençlerin sayısını ve kalitesini belirler.", "The academy decides how many youngsters join the first team each season and how good they are."),
    Facilities.MEDICAL to L("Sakatlık riskini azaltır, sakat oyuncuların dönüşünü hızlandırır.", "Reduces injury risk and speeds up injured players' return."),
)

@Composable
private fun FacilityCard(g: Game, us: Team, type: Int, onMessage: (String) -> Unit) {
    val lvl = Facilities.level(us, type)
    SectionCard("${Facilities.icons[type]} ${Facilities.names[type]}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("Seviye $lvl/10", "Level $lvl/10"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Bar(lvl / 10f, if (lvl >= 8) Gold else Green, Modifier.weight(1f), 8.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(facInfo[type] ?: "", color = TextDim, fontSize = 12.sp)
        Text(effectText(type, lvl), color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val proj = g.projects.firstOrNull { it.type == type }
        when {
            proj != null -> ProjectProgress(proj)
            lvl >= 10 -> Text(L("En üst seviye! 🏆", "Max level! 🏆"), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            else -> {
                val cost = Facilities.upgradeCost(lvl)
                Text(L("Seviye ${lvl + 1}: ${effectText(type, lvl + 1)}", "Level ${lvl + 1}: ${effectText(type, lvl + 1)}"), color = TextDim, fontSize = 11.sp)
                Button(
                    onClick = { onMessage(GameRepo.startProject(type)) },
                    enabled = us.budget >= cost,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                ) {
                    Text(L("Geliştir · ${money(cost)} · ${Facilities.upgradeWeeks(lvl)} hafta", "Upgrade · ${money(cost)} · ${Facilities.upgradeWeeks(lvl)} weeks"), fontSize = 13.sp)
                }
            }
        }
    }
}
