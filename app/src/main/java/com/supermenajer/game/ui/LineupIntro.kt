package com.supermenajer.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.Side
import com.supermenajer.game.model.Comp

/* ---------- Maç öncesi TV açılışı: iki takımın ilk 11'i yayın grafiği gibi ---------- */

/**
 * Canlı maçtan önce tam ekran yayın grafiği: ortada yatay saha, solda ev sahibi, sağda deplasman
 * dizilişi (numara ve soyadıyla); yanlarda arma, diziliş ve teknik direktör; altta maç koşulları.
 */
@Composable
fun LineupIntro(sim: MatchSim, homeC: Color, awayC: Color, onDone: () -> Unit) {
    val g = sim.game
    val f = sim.fixture
    val appear = remember { Animatable(if (Fx.enabled) 0f else 1f) }
    LaunchedEffect(Unit) { if (Fx.enabled) appear.animateTo(1f, tween(900)) }
    val cond = remember { runCatching { if (sim.nat != null) null else com.supermenajer.game.engine.Conditions.of(g, f) }.getOrNull() }
    val derby = remember { runCatching { com.supermenajer.game.game.Talks.derbyName(g, f) }.getOrNull() }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF060A1F), Color(0xFF0E1640), Color(0xFF060A1F))))
            .clickable(onClick = onDone),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Üst şerit: canlı etiketi, turnuva, stadyum
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    L("● CANLI", "● LIVE"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE53935)).padding(horizontal = 7.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    (derby?.let { "🔥 $it · " } ?: "") + Comp.name(f.comp) + (if (Comp.isLeague(f.comp)) L(" · ${f.week}. hafta", " · Matchday ${f.week}") else ""),
                    color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(sim.home.team.stadiumName.ifBlank { sim.home.team.city }, color = TextDim, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TeamColumn(sim, sim.home, homeC, Modifier.width(132.dp).alpha(appear.value))
                Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { IntroPitch(sim, homeC, awayC, appear.value) }
                TeamColumn(sim, sim.away, awayC, Modifier.width(132.dp).alpha(appear.value))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cond?.summary() ?: "", color = TextMain, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    L("Devam ▶", "Continue ▶"), color = Color(0xFF06231A), fontSize = 14.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF34F5C0), Color(0xFF12C795))))
                        .clickable(onClick = onDone).padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(sim: MatchSim, s: Side, c: Color, modifier: Modifier) {
    val g = sim.game
    val t = s.team
    val coach = if (t.id == g.userTeamId) g.managerName else g.managers[t.id] ?: ""
    val avg = s.onPitch.map { it.p.overall }.average().takeIf { !it.isNaN() } ?: 0.0
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamBadge(t, 64.dp)
        Spacer(Modifier.height(6.dp))
        Text(t.name, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 17.sp)
        Box(Modifier.padding(top = 4.dp).width(40.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c))
        Spacer(Modifier.height(6.dp))
        Text(t.formation, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(L("Ort. güç ${"%.0f".format(avg)}", "Avg. rating ${"%.0f".format(avg)}"), color = TextDim, fontSize = 11.sp)
        if (coach.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(L("Teknik direktör", "Manager"), color = TextDim, fontSize = 10.sp)
            Text(coach, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Yatay saha: ev sahibi soldan sağa, deplasman sağdan sola hücum eder. */
@Composable
private fun IntroPitch(sim: MatchSim, homeC: Color, awayC: Color, appear: Float) {
    BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight()) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2E8B47), Color(0xFF237338))))
            val stripes = 10
            for (i in 0 until stripes step 2) drawRect(Color.White.copy(alpha = 0.04f), Offset(size.width * i / stripes, 0f), Size(size.width / stripes, size.height))
            val line = Color.White.copy(alpha = 0.55f)
            val sw = 2f
            drawRect(line, Offset(4f, 4f), Size(size.width - 8f, size.height - 8f), style = Stroke(sw))
            drawLine(line, Offset(size.width / 2, 4f), Offset(size.width / 2, size.height - 4f), sw)
            drawCircle(line, size.height * 0.15f, Offset(size.width / 2, size.height / 2), style = Stroke(sw))
            for (left in listOf(true, false)) {
                val bw = size.width * 0.15f
                val x = if (left) 4f else size.width - 4f - bw
                drawRect(line, Offset(x, size.height * 0.22f), Size(bw, size.height * 0.56f), style = Stroke(sw))
            }
        }
        // Oyuncular: dizilişteki yerleri (y: kendi kalesinden uzaklık → yatay; x: yan konum → dikey)
        for ((side, c) in listOf(sim.home to homeC, sim.away to awayC)) {
            val home = side === sim.home
            side.onPitch.forEachIndexed { i, m ->
                val depth = m.slot.y.coerceIn(0f, 1f)
                val fx = if (home) 0.04f + depth * 0.42f else 0.96f - depth * 0.42f
                val fy = if (home) m.slot.x.coerceIn(0.06f, 0.94f) else 1f - m.slot.x.coerceIn(0.06f, 0.94f)
                val delay = (i / 11f) * 0.5f
                val a = ((appear - delay) / 0.5f).coerceIn(0f, 1f)
                PlayerDot(
                    m.p.number, m.p.name.substringAfterLast(' '), if (m.p.pos == com.supermenajer.game.model.Pos.GK) Color(0xFFFFB300) else c,
                    Modifier.offset(x = w * fx - 30.dp, y = h * fy - 14.dp).alpha(a),
                )
            }
        }
    }
}

@Composable
private fun PlayerDot(number: Int, name: String, c: Color, modifier: Modifier) {
    Column(modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(c).border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(if (number > 0) "$number" else "", color = contentOn(c), fontSize = 10.sp, fontWeight = FontWeight.Black) }
        Text(
            name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 3.dp),
        )
    }
}
