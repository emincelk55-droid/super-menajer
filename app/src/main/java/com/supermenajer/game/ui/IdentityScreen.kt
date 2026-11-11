package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.model.Game

/* ---------- Kulüp kimliği: arma ve forma editörü ---------- */

/** Editörde seçilebilen renkler. */
val KitColors = listOf(
    0xFFD50000, 0xFFC62828, 0xFF880E4F, 0xFFFF6F00, 0xFFFFC107, 0xFFFFEB3B, 0xFF2E7D32, 0xFF00897B,
    0xFF81D4FA, 0xFF1565C0, 0xFF0D47A1, 0xFF283593, 0xFF6A1B9A, 0xFF795548, 0xFF111111, 0xFFFFFFFF,
)

val CrestShapeNames = listOf("Kalkan", "Yuvarlak", "Modern", "Sivri")
val CrestPatternNames = listOf("Çizgili", "Yarım", "Bantlı", "Üst bant", "Kuşak", "Düz")
val KitPatternNames = listOf("Düz", "Dikey çizgi", "Yatay çizgi", "Yarım", "Kuşak")

/** Forma deseni: [c2] rengiyle verilen alana (formanın gövdesi) çizilir. */
fun DrawScope.drawKitPattern(c2: Color, pattern: Int, left: Float, top: Float, w: Float, h: Float) {
    when (pattern) {
        1 -> { val n = 7; for (i in 0 until n) if (i % 2 == 1) drawRect(c2, Offset(left + w * i / n, top), androidx.compose.ui.geometry.Size(w / n + 0.5f, h)) }
        2 -> { val n = 6; for (i in 0 until n) if (i % 2 == 1) drawRect(c2, Offset(left, top + h * i / n), androidx.compose.ui.geometry.Size(w, h / n + 0.5f)) }
        3 -> drawRect(c2, Offset(left + w / 2, top), androidx.compose.ui.geometry.Size(w / 2, h))
        4 -> drawPath(Path().apply {
            moveTo(left, top + h * 0.05f); lineTo(left + w * 0.28f, top); lineTo(left + w, top + h * 0.78f); lineTo(left + w, top + h); lineTo(left + w * 0.72f, top + h); lineTo(left, top + h * 0.3f); close()
        }, c2)
        else -> {}
    }
}

/** Forma önizlemesi (kollu tişört). */
@Composable
fun KitView(c1: Color, c2: Color, pattern: Int, number: String, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val shirt = Path().apply {
                moveTo(w * 0.36f, h * 0.06f)
                quadraticBezierTo(w * 0.5f, h * 0.16f, w * 0.64f, h * 0.06f)
                lineTo(w * 0.9f, h * 0.17f); lineTo(w * 0.98f, h * 0.42f); lineTo(w * 0.8f, h * 0.48f); lineTo(w * 0.78f, h * 0.36f)
                lineTo(w * 0.78f, h * 0.95f); lineTo(w * 0.22f, h * 0.95f); lineTo(w * 0.22f, h * 0.36f)
                lineTo(w * 0.2f, h * 0.48f); lineTo(w * 0.02f, h * 0.42f); lineTo(w * 0.1f, h * 0.17f); close()
            }
            drawPath(shirt, Color.Black.copy(alpha = 0.3f), style = Stroke(w * 0.05f))
            clipPath(shirt) {
                drawRect(c1)
                drawKitPattern(c2, pattern, w * 0.22f, 0f, w * 0.56f, h)
                // Kollar ikinci renk kenarlı, yaka
                drawLine(c2, Offset(w * 0.98f, h * 0.42f), Offset(w * 0.8f, h * 0.48f), w * 0.06f)
                drawLine(c2, Offset(w * 0.02f, h * 0.42f), Offset(w * 0.2f, h * 0.48f), w * 0.06f)
                drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.22f))))
            }
            drawPath(Path().apply { moveTo(w * 0.36f, h * 0.06f); quadraticBezierTo(w * 0.5f, h * 0.16f, w * 0.64f, h * 0.06f) }, c2, style = Stroke(w * 0.04f))
            drawPath(shirt, lerp(c1, Color.Black, 0.4f), style = Stroke(w * 0.012f))
        }
        if (number.isNotEmpty()) {
            val numC = if (pattern == 0) contentOn(c1) else Color.White
            Text(
                number, color = numC, fontSize = (size.value * 0.26f).sp, fontWeight = FontWeight.Black,
                style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 4f)),
                modifier = Modifier.padding(top = (size.value * 0.12f).dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityView(g: Game) {
    val t = g.userTeam
    GameRepo.version // değişince yeniden çiz
    var name by remember(t.id) { mutableStateOf(t.name) }
    var short by remember(t.id) { mutableStateOf(t.short) }
    var msg by remember { mutableStateOf<String?>(null) }
    val c1 = Color(t.color1)
    val c2 = Color(t.color2)
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Sol: önizleme ve ad
        Column(Modifier.weight(0.8f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("🛡️ Kulüp kimliği") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    CrestView(c1, c2, crestShapeOf(t), crestPatternOf(t), t.crestIcon, t.short, 104.dp)
                    KitView(c1, c2, kitPatternOf(t), "10", 104.dp)
                }
                Spacer(Modifier.height(8.dp))
                val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = CardLine, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                OutlinedTextField(
                    name, { name = it.take(26) }, label = { Text("Kulüp adı") }, singleLine = true,
                    colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        short, { short = it.filter { c -> c.isLetter() }.uppercase().take(3) }, label = { Text("Kısa ad") }, singleLine = true,
                        colors = fieldColors, modifier = Modifier.width(110.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Adı kaydet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Gold)
                            .pressClick(enabled = name != t.name || short != t.short) { msg = GameRepo.renameClub(name, short) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                msg?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                Text("Renkler maçta formalara, armaya ve tüm ekranlara yansır.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        // Sağ: seçenekler
        Column(Modifier.weight(1.4f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("🛡️ Arma") {
                Text("Şekil", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OptionRow(CrestShapeNames.size, crestShapeOf(t), { i -> CrestView(c1, c2, i, crestPatternOf(t), t.crestIcon, t.short, 46.dp) }, { CrestShapeNames[it] }) { i ->
                    GameRepo.editIdentity { it.crestShape = i }
                }
                Text("Desen", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                OptionRow(CrestPatternNames.size, crestPatternOf(t), { i -> CrestView(c1, c2, crestShapeOf(t), i, 0, "", 42.dp) }, { CrestPatternNames[it] }) { i ->
                    GameRepo.editIdentity { it.crestPattern = i }
                }
                Text("Simge", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                OptionRow(CrestIcons.size, t.crestIcon, { i -> Text(if (i == 0) t.short else CrestIcons[i], fontSize = if (i == 0) 13.sp else 22.sp, fontWeight = FontWeight.Black) }, { if (it == 0) "Kısa ad" else "" }) { i ->
                    GameRepo.editIdentity { it.crestIcon = i }
                }
            }
            SectionCard("🎨 Renkler") {
                for ((k, label) in listOf("Ana renk", "İkinci renk").withIndex()) {
                    Text(label, color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (k == 1) 6.dp else 0.dp, bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in KitColors) {
                            val sel = (if (k == 0) t.color1 else t.color2) == col
                            Box(
                                Modifier.size(26.dp).clip(CircleShape).background(Color(col))
                                    .border(if (sel) 3.dp else 1.dp, if (sel) Gold else Color.White.copy(alpha = 0.25f), CircleShape)
                                    .pressClick { GameRepo.editIdentity { if (k == 0) it.color1 = col else it.color2 = col } },
                                contentAlignment = Alignment.Center,
                            ) { if (sel) Text("✓", color = contentOn(Color(col)), fontWeight = FontWeight.Black, fontSize = 14.sp) }
                        }
                    }
                }
            }
            SectionCard("👕 Forma") {
                OptionRow(KitPatternNames.size, kitPatternOf(t), { i -> KitView(c1, c2, i, "", 48.dp) }, { KitPatternNames[it] }) { i ->
                    GameRepo.editIdentity { it.kitPattern = i }
                }
            }
        }
    }
}

/** Yatay seçenek dizisi: her biri küçük önizleme ve ad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionRow(n: Int, selected: Int, preview: @Composable (Int) -> Unit, label: (Int) -> String, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        for (i in 0 until n) {
            val sel = i == selected
            Column(
                Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) Teal.copy(alpha = 0.18f) else Surface1)
                    .border(if (sel) 2.dp else 1.dp, if (sel) Teal else Color.Transparent, RoundedCornerShape(10.dp))
                    .pressClick { onPick(i) }.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) { preview(i) }
                val l = label(i)
                if (l.isNotEmpty()) Text(l, fontSize = 10.sp, color = if (sel) Teal else TextDim, fontWeight = FontWeight.Bold)
            }
        }
    }
}
