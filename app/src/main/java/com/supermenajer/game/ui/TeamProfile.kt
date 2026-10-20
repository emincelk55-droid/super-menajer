package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Scouting
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Team

/** Rakip takım profili: kulüp bilgisi, form, kadro (tahmini güçlerle) ve son maçlar. */
@Composable
fun TeamProfileDialog(g: Game, t: Team) {
    val close = { GameRepo.openTeam = -1 }
    val squad = g.squad(t.id).sortedWith(compareBy<com.supermenajer.game.model.Player> { it.pos.ordinal }.thenByDescending { Scouting.estimate(g, it) })
    val results = g.fixtures.filter { it.played && it.involves(t.id) }.sortedBy { it.week }.takeLast(5).reversed()
    val next = g.fixtures.filter { !it.played && it.involves(t.id) }.minByOrNull { it.week }
    WideDialog(onDismissRequest = close, maxWidth = 960.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(t.color1).copy(alpha = 0.55f), CardTop))).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamBadge(t, 60.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("${Generator.flag(g.flagOf(t))} ${g.leagueName(t)} · ${posText(g, t)} sıra · ${t.points} puan", color = TextMain.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("Teknik direktör: ${if (t.id == g.userTeamId) g.managerName else g.managers[t.id] ?: "-"} · ${t.stadiumName.ifBlank { t.name + " Stadı" }} (${"%,d".format(t.stadium).replace(',', '.')})", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("★".repeat(repStars(t.reputation)) + "☆".repeat(5 - repStars(t.reputation)), color = Gold, fontSize = 14.sp)
                    Text("Güç ${Lineups.strength(g, t).toInt()} · ${money(t.budget)}", color = TextDim, fontSize = 12.sp)
                    if (t.form.isNotEmpty()) FormDots(t.form)
                }
                Text("✕", fontSize = 20.sp, color = TextDim, modifier = Modifier.padding(start = 10.dp).clip(CircleShape).clickable { close() }.padding(6.dp))
            }
            Row(Modifier.weight(1f).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyColumn(Modifier.weight(1.3f).fillMaxHeight()) {
                    item { Text("KADRO (${squad.size}) · güçler gözlemci tahminidir", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    for (pos in Pos.entries) {
                        val list = squad.filter { it.pos == pos }
                        if (list.isEmpty()) continue
                        item { Text(pos.label.uppercase(), color = posColor(pos), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp)) }
                        items(list.size) { i ->
                            val p = list[i]
                            val r = Scouting.range(g, p, p.overall)
                            Row(
                                Modifier.fillMaxWidth().clickable { close(); GameRepo.selectedPlayer = p }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(Positions.det(p).code, color = posColor(p.pos), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(38.dp))
                                Text(p.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${Generator.flag(p.nation)} ${p.age}", fontSize = 12.sp, color = TextDim, modifier = Modifier.width(56.dp))
                                Text(if (p.apps > 0) "${p.apps}M ${p.goals}G" else "", fontSize = 11.sp, color = TextDim, modifier = Modifier.width(60.dp))
                                Text(if (r.first == r.last) "${r.first}" else "${r.first}-${r.last}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ovrColor((r.first + r.last) / 2), modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        DashCard("Son Maçlar", Modifier.fillMaxWidth()) {
                            if (results.isEmpty()) Text("Henüz maç oynamadı.", color = TextDim, fontSize = 13.sp)
                            results.forEachIndexed { i, f ->
                                if (i > 0) HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
                                ResultRow(g, f, t.id)
                            }
                        }
                    }
                    next?.let { f ->
                        item {
                            DashCard("Sıradaki Maç", Modifier.fillMaxWidth()) {
                                val opp = g.team(if (f.home == t.id) f.away else f.home)
                                Text("${opp.name} (${if (f.home == t.id) "iç saha" else "deplasman"}) · ${com.supermenajer.game.game.GameCalendar.format(com.supermenajer.game.game.GameCalendar.dateOf(g, f.week), false)}", fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        DashCard("Kulüp", Modifier.fillMaxWidth()) {
                            val top = squad.filter { it.goals > 0 }.maxByOrNull { it.goals }
                            Text("Hedef: ${Season.objectiveText(t)}", fontSize = 13.sp)
                            Text("Diziliş: ${t.formation} · ${listOf("Defansif", "Dengeli", "Hücum")[t.mentality.coerceIn(0, 2)]}", fontSize = 13.sp)
                            top?.let { Text("Gol kralı: ${it.name} (${it.goals})", fontSize = 13.sp) }
                            Text("Ortalama yaş: ${"%.1f".format(squad.map { it.age }.average())}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
