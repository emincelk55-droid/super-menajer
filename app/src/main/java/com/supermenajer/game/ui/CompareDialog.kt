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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Scouting
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos

/** Bir satır: iki değer (gösterim + karşılaştırma için sayı), daha iyisi yeşil. */
private class CmpRow(val label: String, val a: String, val av: Double, val b: String, val bv: Double, val max: Double, val lowerBetter: Boolean = false)

/**
 * İki oyuncuyu yan yana karşılaştırır. Gözlemci raporu olmayan oyuncuda değerler
 * aralık olarak görünür ve karşılaştırmada aralığın ortası kullanılır.
 */
@Composable
fun CompareDialog(g: Game, a: Player, b: Player) {
    fun attr(p: Player, v: Int): Pair<String, Double> {
        val r = Scouting.range(g, p, v)
        return (if (r.first == r.last) "${r.first}" else "${r.first}-${r.last}") to (r.first + r.last) / 2.0
    }
    fun row(label: String, sel: (Player) -> Int): CmpRow {
        val (at, av) = attr(a, sel(a))
        val (bt, bv) = attr(b, sel(b))
        return CmpRow(label, at, av, bt, bv, 99.0)
    }
    val rows = buildList {
        add(row("Güç") { it.overall })
        add(
            CmpRow(
                "Potansiyel", Scouting.potText(g, a), if (Scouting.known(g, a)) a.potential.toDouble() else 0.0,
                Scouting.potText(g, b), if (Scouting.known(g, b)) b.potential.toDouble() else 0.0, 99.0,
            ),
        )
        if (a.pos == Pos.GK || b.pos == Pos.GK) add(row("Kalecilik") { it.goalkeeping })
        add(row("Hız") { it.pace })
        add(row("Şut") { it.shooting })
        add(row("Pas") { it.passing })
        add(row("Teknik") { it.dribbling })
        add(row("Savunma") { it.defending })
        add(row("Fizik") { it.physical })
        add(CmpRow("Yaş", "${a.age}", a.age.toDouble(), "${b.age}", b.age.toDouble(), 40.0, lowerBetter = true))
        add(CmpRow("Değer", money(a.value), a.value.toDouble(), money(b.value), b.value.toDouble(), maxOf(a.value, b.value, 1).toDouble()))
        add(CmpRow("Maaş talebi", money(a.demandWage), a.demandWage.toDouble(), money(b.demandWage), b.demandWage.toDouble(), maxOf(a.demandWage, b.demandWage, 1).toDouble(), lowerBetter = true))
        add(CmpRow("Bu sezon maç", "${a.apps}", a.apps.toDouble(), "${b.apps}", b.apps.toDouble(), maxOf(a.apps, b.apps, 1).toDouble()))
        add(CmpRow("Gol + asist", "${a.goals}+${a.assists}", (a.goals + a.assists).toDouble(), "${b.goals}+${b.assists}", (b.goals + b.assists).toDouble(), maxOf(a.goals + a.assists, b.goals + b.assists, 1).toDouble()))
        add(
            CmpRow(
                "Ort. puan", if (a.apps > 0) "%.2f".format(a.avgRating) else "-", a.avgRating,
                if (b.apps > 0) "%.2f".format(b.avgRating) else "-", b.avgRating, 10.0,
            ),
        )
    }
    WideDialog(onDismissRequest = { GameRepo.compare = null }, maxWidth = 760.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚖ Oyuncu Karşılaştırma", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("✕", fontSize = 20.sp, color = TextDim, modifier = Modifier.clip(CircleShape).clickable { GameRepo.compare = null }.padding(8.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompareHead(g, a, Modifier.weight(1f), Alignment.Start)
                Text("VS", fontWeight = FontWeight.Black, color = TextDim, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                CompareHead(g, b, Modifier.weight(1f), Alignment.End)
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                rows.forEach { r -> CompareLine(r) }
            }
            Spacer(Modifier.height(6.dp))
            val pin = GameRepo.comparePin?.let { g.player(it) }
            Text(
                "Başka bir oyuncuyla karşılaştırmak için onun profilinde ⚖'ye bas" + (pin?.let { " · Sabitlenen: ${it.name}" } ?: ""),
                color = TextDim, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun CompareHead(g: Game, p: Player, modifier: Modifier, align: Alignment.Horizontal) {
    val t = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0B1028)).clickable { GameRepo.compare = null; GameRepo.selectedPlayer = p }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (align == Alignment.End) Arrangement.End else Arrangement.Start,
    ) {
        if (align == Alignment.Start) PlayerFace(p, 52.dp, t?.let { Color(it.color1) } ?: Color(0xFF455A64), t?.let { Color(it.color2) } ?: Color.White)
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = align) {
            Text(p.name, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Positions.det(p).code} · ${p.age} yaş · ${Generator.flag(p.nation)}", color = TextDim, fontSize = 12.sp, maxLines = 1)
            Text(t?.name ?: "Serbest", color = Teal, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (align == Alignment.End) PlayerFace(p, 52.dp, t?.let { Color(it.color1) } ?: Color(0xFF455A64), t?.let { Color(it.color2) } ?: Color.White)
    }
}

@Composable
private fun CompareLine(r: CmpRow) {
    val aBetter = if (r.lowerBetter) r.av < r.bv else r.av > r.bv
    val bBetter = if (r.lowerBetter) r.bv < r.av else r.bv > r.av
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(r.a, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (aBetter) Green else TextMain, modifier = Modifier.width(84.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.fillMaxWidth((r.av / r.max).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(if (aBetter) Green else TextDim.copy(alpha = 0.6f)))
        }
        Text(r.label, color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(110.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth((r.bv / r.max).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(if (bBetter) Green else TextDim.copy(alpha = 0.6f)))
        }
        Text(r.b, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (bBetter) Green else TextMain, modifier = Modifier.width(84.dp), maxLines = 1, textAlign = TextAlign.End)
    }
}
