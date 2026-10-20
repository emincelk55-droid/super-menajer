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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.supermenajer.game.game.Positions
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.math.roundToInt

/**
 * Veri editörü: kariyer içinde oyuncu adı, yaşı, potansiyeli ve niteliklerini düzenle
 * (FM'deki oyun içi editör gibi). Değişiklikler kayda yazılır.
 */
@Composable
fun EditorDialog(g: Game) {
    var query by remember { mutableStateOf("") }
    var selId by remember { mutableIntStateOf(-1) }
    val list = remember(query, GameRepo.version) {
        val q = query.trim().lowercase()
        (if (q.length < 2) g.squad(g.userTeamId) else g.players.filter { it.name.lowercase().contains(q) }).take(40)
    }
    WideDialog(onDismissRequest = { GameRepo.editorOpen = false }, maxWidth = 960.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp)).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛠 Veri Editörü", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text("✕", fontSize = 20.sp, color = TextDim, modifier = Modifier.clip(CircleShape).clickable { GameRepo.editorOpen = false }.padding(6.dp))
            }
            Text("Oyuncu bilgilerini düzenle. Kendi takımın listelenir; başka oyuncu için en az 2 harf yaz.", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(300.dp).fillMaxHeight()) {
                    OutlinedTextField(query, { query = it.take(30) }, singleLine = true, placeholder = { Text("🔍 Oyuncu ara") }, modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f)) {
                        items(list.size) { i ->
                            val p = list[i]
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(if (p.id == selId) Teal.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable { selId = p.id }.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(Positions.det(p).code, color = posColor(p.pos), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                Text(p.name, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${p.overall}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                val p = g.player(selId)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (p == null) Text("Düzenlemek için soldan bir oyuncu seç.", color = TextDim, modifier = Modifier.align(Alignment.Center))
                    else EditForm(g, p)
                }
            }
        }
    }
}

@Composable
private fun EditForm(g: Game, p: Player) {
    var name by remember(p.id) { mutableStateOf(p.name) }
    var age by remember(p.id) { mutableFloatStateOf(p.age.toFloat()) }
    var pot by remember(p.id) { mutableFloatStateOf(p.potential.toFloat()) }
    val attrs = remember(p.id) {
        mutableStateOf(listOf(p.pace, p.shooting, p.passing, p.dribbling, p.defending, p.physical, p.goalkeeping).map { it.toFloat() })
    }
    var saved by remember(p.id) { mutableStateOf(false) }
    val labels = listOf("Hız", "Şut", "Pas", "Teknik", "Savunma", "Fizik", "Kalecilik")
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(name, { name = it.take(28); saved = false }, label = { Text("Ad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SliderRow("Yaş", age, 15f..42f) { age = it; saved = false }
        SliderRow("Potansiyel", pot, 30f..99f) { pot = it; saved = false }
        labels.forEachIndexed { i, l ->
            if (i == 6 && p.pos != Pos.GK) return@forEachIndexed
            SliderRow(l, attrs.value[i], 10f..99f) { v -> attrs.value = attrs.value.toMutableList().also { it[i] = v }; saved = false }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF34F5C0), Color(0xFF12C795))))
                .clickable {
                    val a = attrs.value.map { it.roundToInt() }
                    GameRepo.editPlayer(p, name.ifBlank { p.name }, age.roundToInt(), pot.roundToInt(), a)
                    saved = true
                },
            contentAlignment = Alignment.Center,
        ) { Text(if (saved) "✓ Kaydedildi (güç ${p.overall})" else "Kaydet", color = Color(0xFF06231A), fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun SliderRow(label: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(84.dp))
        Slider(value = v, onValueChange = { onChange(it.roundToInt().toFloat()) }, valueRange = range, modifier = Modifier.weight(1f))
        Text("${v.roundToInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}
