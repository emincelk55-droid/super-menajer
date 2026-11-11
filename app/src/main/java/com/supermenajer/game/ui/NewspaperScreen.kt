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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Newspaper
import com.supermenajer.game.model.Game

private val Paper = Color(0xFFF2EAD8)
private val Ink = Color(0xFF1C1A17)
private val InkDim = Color(0xFF5B544A)
private val PaperRed = Color(0xFFC0241B)

private fun tagColor(tag: String) = when (tag) {
    "SÖYLENTİ" -> Color(0xFF7A3FB8)
    "DERBİ" -> Color(0xFFE0531A)
    "TRANSFER" -> Color(0xFF1F6FB8)
    "GOL KRALLIĞI" -> Color(0xFF2E7D32)
    "KULÜP" -> Color(0xFF8D5A00)
    else -> PaperRed
}

/** Ana sayfadaki gazete kısayolu: manşetin özeti. */
@Composable
fun NewspaperTeaser(g: Game, modifier: Modifier = Modifier) {
    val ed = remember(g.season, g.week, GameRepo.version) { Newspaper.edition(g) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.pressClick(0.98f) { GameRepo.newspaperOpen = true }.shadow(6.dp, shape).clip(shape)
            .background(Paper).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📰", fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${ed.masthead} · ${ed.date}", color = PaperRed, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(ed.main.headline, color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ed.side.firstOrNull { it.tag == "SÖYLENTİ" }?.let { "Söylenti: ${it.headline}" } ?: ed.main.body, color = InkDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Oku ›", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Haftanın gazetesi: büyük manşet ve iki sütunda haberler, söylentiler. */
@Composable
fun NewspaperDialog(g: Game) {
    val ed = remember(g.season, g.week) { Newspaper.edition(g) }
    val close = { GameRepo.newspaperOpen = false }
    WideDialog(onDismissRequest = close, maxWidth = 900.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.95f).clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Paper, Color(0xFFE9DEC6))))
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            // Künye
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sayı ${ed.number}", color = InkDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("✕", color = Ink, fontSize = 20.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { close() }.padding(4.dp))
            }
            Text(
                ed.masthead, color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 34.sp,
                letterSpacing = 4.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Rule(2.dp)
            Text(ed.date.uppercase() + "  ·  FUTBOLUN SESİ", color = InkDim, fontSize = 11.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp))
            Rule(1.dp)
            Spacer(Modifier.height(10.dp))
            // Manşet
            Tag(ed.main.tag)
            Text(ed.main.headline, color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 42.sp)
            Text(ed.main.body, color = Ink, fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            Rule(1.dp)
            Spacer(Modifier.height(10.dp))
            // İki sütun
            val half = (ed.side.size + 1) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (col in listOf(ed.side.take(half), ed.side.drop(half))) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (st in col) {
                            Column {
                                Tag(st.tag)
                                Text(st.headline, color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp)
                                Text(st.body, color = InkDim, fontFamily = FontFamily.Serif, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Söylentiler basına sızan iddialardır; her zaman gerçekleşmez.", color = InkDim, fontSize = 10.sp, fontFamily = FontFamily.Serif)
        }
    }
}

@Composable
private fun Rule(h: androidx.compose.ui.unit.Dp) = Box(Modifier.fillMaxWidth().height(h).background(Ink))

@Composable
private fun Tag(tag: String) {
    Text(
        tag, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 3.dp).clip(RoundedCornerShape(3.dp)).background(tagColor(tag)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
