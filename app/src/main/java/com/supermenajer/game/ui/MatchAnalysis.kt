package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.PIM
import com.supermenajer.game.engine.Side

/* ---------- Maç sonu analizi: ısı haritası ve oyuncu istatistikleri ---------- */

/** İki takımın ısı haritası: topun takımdayken nerede oynandığı (hücum yönü sağa). */
@Composable
fun HeatMapCard(sim: MatchSim, homeC: Color, awayC: Color) {
    DashCard(L("🔥 Isı haritası", "🔥 Heat map"), Modifier.fillMaxWidth()) {
        Text(L("Topun takımdayken en çok oynandığı bölgeler (hücum yönü sağa).", "Where each team had the ball most (attacking to the right)."), color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((s, c) in listOf(sim.home to homeC, sim.away to awayC)) {
                Column(Modifier.weight(1f)) {
                    Text(s.team.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HeatPitch(s, c, Modifier.fillMaxWidth().aspectRatio(1.55f).clip(RoundedCornerShape(6.dp)))
                    val thirds = thirds(s)
                    Text(
                        L("Savunma %${thirds[0]} · Orta %${thirds[1]} · Hücum %${thirds[2]}", "Defence ${thirds[0]}% · Middle ${thirds[1]}% · Attack ${thirds[2]}%"),
                        color = TextDim, fontSize = 10.sp, maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Topun takımdayken sahanın üçte birlik bölgelerinde geçirdiği pay (%). */
fun thirds(s: Side): IntArray {
    val w = Side.HEAT_W
    val tot = s.heat.sum().coerceAtLeast(1)
    val cols = IntArray(3)
    for (i in s.heat.indices) {
        val x = i % w
        cols[when { x < w / 3 -> 0; x < w * 2 / 3 -> 1; else -> 2 }] += s.heat[i]
    }
    return IntArray(3) { cols[it] * 100 / tot }
}

@Composable
private fun HeatPitch(s: Side, c: Color, modifier: Modifier) {
    val max = s.heat.max().coerceAtLeast(1)
    val hot = Color(0xFFE53935)
    Canvas(modifier.background(Color(0xFF1F6B36))) {
        val w = size.width
        val h = size.height
        val cw = w / Side.HEAT_W
        val ch = h / Side.HEAT_H
        for (i in s.heat.indices) {
            val v = s.heat[i].toFloat() / max
            if (v <= 0f) continue
            val x = (i % Side.HEAT_W) * cw
            val y = (i / Side.HEAT_W) * ch
            drawRect(lerp(Color(0xFFFFEB3B), hot, v).copy(alpha = 0.12f + 0.68f * v), Offset(x, y), Size(cw + 0.5f, ch + 0.5f))
        }
        val line = Color.White.copy(alpha = 0.7f)
        val sw = 1.5f
        drawRect(line, Offset.Zero, size, style = Stroke(sw))
        drawLine(line, Offset(w / 2, 0f), Offset(w / 2, h), sw)
        drawCircle(line, h * 0.17f, Offset(w / 2, h / 2), style = Stroke(sw))
        drawRect(line, Offset(0f, h * 0.22f), Size(w * 0.16f, h * 0.56f), style = Stroke(sw))
        drawRect(line, Offset(w * 0.84f, h * 0.22f), Size(w * 0.16f, h * 0.56f), style = Stroke(sw))
        // Hücum yönü oku
        drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.42f, h * 0.92f), Offset(w * 0.58f, h * 0.92f), 2f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.58f, h * 0.92f), Offset(w * 0.55f, h * 0.88f), 2f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.58f, h * 0.92f), Offset(w * 0.55f, h * 0.96f), 2f)
    }
}

/** Oyuncu istatistikleri tablosu: not, şut (isabet), kilit pas, top kazanma, kurtarış. */
@Composable
fun PlayerStatsCard(side: Side) {
    DashCard(L("📊 Oyuncu istatistikleri", "📊 Player stats"), Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L("Oyuncu", "Player"), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            for (h in listOf(L("Not", "Rtg"), L("Şut", "Sh"), L("K.Pas", "KP"), L("Top K.", "Tkl"), L("Krt.", "Sv"))) {
                Text(h, color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(42.dp))
            }
        }
        HorizontalDivider(color = CardLine)
        side.used.sortedByDescending { it.rating }.forEach { m -> StatRow(m) }
        Text(
            L("Şut: toplam (isabetli) · K.Pas: şutla biten pas · Top K.: rakipten top kazanma · Krt.: kaleci kurtarışı",
                "Sh: total (on target) · KP: key passes · Tkl: ball recoveries · Sv: goalkeeper saves"),
            color = TextDim, fontSize = 9.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StatRow(m: PIM) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        PosChip(m.slot.pos)
        Spacer(Modifier.width(6.dp))
        Text(
            m.p.name.substringAfterLast(' ') + (if (m.goals > 0) " " + "⚽".repeat(m.goals) else "") + (if (m.assists > 0) " 🅰️" else ""),
            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        val r = m.rating.coerceIn(3.0, 10.0)
        Text("%.1f".format(r), color = ratingColor(r), fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.width(42.dp))
        Cell(if (m.shots > 0) "${m.shots} (${m.onTarget})" else "–")
        Cell(if (m.keyPasses > 0) "${m.keyPasses}" else "–")
        Cell(if (m.tackles > 0) "${m.tackles}" else "–")
        Cell(if (m.saves > 0) "${m.saves}" else "–")
    }
}

@Composable
private fun Cell(t: String) {
    Text(t, fontSize = 12.sp, textAlign = TextAlign.Center, color = if (t == "–") TextDim else TextMain, modifier = Modifier.width(42.dp))
}
