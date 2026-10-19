package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Attributes
import com.supermenajer.game.game.Positions
import com.supermenajer.game.model.Player

/** FM tarzı nitelik rozeti rengi (1-20). */
fun attrColor(v: Int): Color = when {
    v >= 18 -> Color(0xFF22D3EE)
    v >= 15 -> Color(0xFF34D399)
    v >= 11 -> Color(0xFFFACC15)
    v >= 7 -> Color(0xFFFB923C)
    else -> Color(0xFFEF4444)
}

@Composable
fun AttrBadge(v: Int, known: Boolean = true, width: Dp = 30.dp) {
    Box(
        Modifier.width(width).height(20.dp).clip(RoundedCornerShape(4.dp)).background(if (known) attrColor(v) else Surface2),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (known) "$v" else "?", color = if (known) Color(0xFF111827) else TextDim, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

/** Üç sütunlu nitelik tablosu (teknik/kalecilik, zihinsel, fiziksel). */
@Composable
fun AttributeTable(p: Player, known: Boolean, extra: List<Pair<String, String>> = emptyList()) {
    val groups = Attributes.groups(p)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEachIndexed { gi, g ->
            Column(Modifier.weight(1f)) {
                Text(g.title, color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                g.attrs.forEachIndexed { i, a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            .background(if (i % 2 == 0) Color.White.copy(alpha = 0.035f) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(a.name, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        AttrBadge(a.value, known)
                    }
                }
                if (gi == groups.lastIndex) extra.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(k, fontSize = 13.sp, color = TextDim, modifier = Modifier.weight(1f))
                        Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val detSpots = mapOf(
    Positions.Det.KL to Offset(0.05f, 0.5f), Positions.Det.SLB to Offset(0.25f, 0.13f), Positions.Det.SGB to Offset(0.25f, 0.87f),
    Positions.Det.STP to Offset(0.2f, 0.5f), Positions.Det.DOS to Offset(0.38f, 0.5f), Positions.Det.MO to Offset(0.53f, 0.5f),
    Positions.Det.OOS to Offset(0.7f, 0.5f), Positions.Det.SLK to Offset(0.74f, 0.14f), Positions.Det.SGK to Offset(0.74f, 0.86f),
    Positions.Det.SF to Offset(0.9f, 0.5f),
)

/** FM'deki gibi mini saha: oyuncunun oynayabildiği mevkiler (yeşil doğal, sarı yakın, gri zayıf). */
@Composable
fun PositionMap(p: Player, modifier: Modifier = Modifier.size(150.dp, 96.dp)) {
    val main = Positions.det(p)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = Color.White.copy(alpha = 0.35f)
        val sw = 1.2.dp.toPx()
        drawRect(Color(0xFF1D3B2A))
        drawRect(line, style = Stroke(sw))
        drawLine(line, Offset(w / 2, 0f), Offset(w / 2, h), sw)
        drawCircle(line, h * 0.17f, Offset(w / 2, h / 2), style = Stroke(sw))
        drawRect(line, Offset(0f, h * 0.22f), Size(w * 0.15f, h * 0.56f), style = Stroke(sw))
        drawRect(line, Offset(w * 0.85f, h * 0.22f), Size(w * 0.15f, h * 0.56f), style = Stroke(sw))
        val r = 5.5.dp.toPx()
        for ((d, o) in detSpots) {
            if ((d == Positions.Det.KL) != (main == Positions.Det.KL)) continue
            val f = Positions.fit(p, d)
            val c = when {
                d == main -> Color(0xFF34D399)
                f >= 0.97 -> Color(0xFF34D399).copy(alpha = 0.75f)
                f >= 0.93 -> Color(0xFFFACC15)
                else -> Color.White.copy(alpha = 0.25f)
            }
            drawCircle(c, if (d == main) r * 1.25f else r, Offset(o.x * w, o.y * h))
        }
    }
}

/** Başlıkta küçük bilgi kutusu: değer üstte, açıklama altta. */
@Composable
fun HeaderStat(value: String, label: String, color: Color = TextMain) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = color, maxLines = 1)
        Text(label, color = TextDim, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
