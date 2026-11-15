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
import androidx.compose.foundation.layout.fillMaxSize
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
    var selId by remember { mutableIntStateOf(GameRepo.editorPlayer) }
    var tab by remember { mutableIntStateOf(GameRepo.editorTab) }
    val close = { GameRepo.editorOpen = false; GameRepo.editorPlayer = -1; GameRepo.editorTab = 0 }
    val list = remember(query, GameRepo.version) {
        val q = query.trim().lowercase()
        (if (q.length < 2) g.squad(g.userTeamId) else g.players.filter { it.name.lowercase().contains(q) }).take(40)
    }
    WideDialog(onDismissRequest = close, maxWidth = 960.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp)).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("🛠 Veri Editörü", "🛠 Data Editor"), fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text("✕", fontSize = 20.sp, color = TextDim, modifier = Modifier.clip(CircleShape).clickable { close() }.padding(6.dp))
            }
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(L("👤 Oyuncular", "👤 Players"), L("🏟 Kulüpler", "🏟 Clubs"), L("📦 İsim paketi", "📦 Name pack")).forEachIndexed { i, t ->
                    SmallChip(t, tab == i, Modifier.width(140.dp)) { tab = i }
                }
            }
            when (tab) {
                1 -> Box(Modifier.weight(1f)) { ClubNames(g) }
                2 -> Box(Modifier.weight(1f)) { NamePackPanel(g) }
                else -> {
                    Text(L("Oyuncu bilgilerini düzenle. Kendi takımın listelenir; başka oyuncu için en az 2 harf yaz.", "Edit player details. Your own squad is listed; type at least 2 letters for other players."), color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.width(300.dp).fillMaxHeight()) {
                            OutlinedTextField(query, { query = it.take(30) }, singleLine = true, placeholder = { Text(L("🔍 Oyuncu ara", "🔍 Search player")) }, modifier = Modifier.fillMaxWidth())
                            LazyColumn(Modifier.weight(1f)) {
                                val shown = g.player(selId)?.takeIf { it !in list }?.let { listOf(it) + list } ?: list
                                items(shown.size) { i ->
                                    val p = shown[i]
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(if (p.id == selId) Teal.copy(alpha = 0.18f) else Color.Transparent)
                                            .clickable { selId = p.id }.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(Positions.det(p).code, color = posColor(p.pos), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(p.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(g.teamOrNull(p.teamId)?.name ?: L("Serbest", "Free agent"), color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("${p.overall}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        val p = g.player(selId)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            if (p == null) Text(L("Düzenlemek için soldan bir oyuncu seç.", "Pick a player on the left to edit."), color = TextDim, modifier = Modifier.align(Alignment.Center))
                            else EditForm(g, p)
                        }
                    }
                }
            }
        }
    }
}

/** Kulüp adları: ara, seç, adını ve 3 harfli kısa adını değiştir. */
@Composable
private fun ClubNames(g: Game) {
    var query by remember { mutableStateOf("") }
    var selId by remember { mutableIntStateOf(g.userTeamId) }
    val list = remember(query, GameRepo.version) {
        val q = query.trim().lowercase()
        g.teams.filter { g.squad(it.id).isNotEmpty() && (q.isEmpty() || it.name.lowercase().contains(q) || it.short.lowercase() == q) }
            .sortedWith(compareBy({ it.id != g.userTeamId }, { it.division }, { it.name })).take(60)
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.width(300.dp).fillMaxHeight()) {
            OutlinedTextField(query, { query = it.take(30) }, singleLine = true, placeholder = { Text(L("🔍 Kulüp ara", "🔍 Search club")) }, modifier = Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f)) {
                items(list.size) { i ->
                    val t = list[i]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (t.id == selId) Teal.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { selId = t.id }.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.short, color = TextDim, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(g.leagueName(t), color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        val t = g.teamOrNull(selId)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (t == null) return@Column
            var name by remember(t.id) { mutableStateOf(t.name) }
            var short by remember(t.id) { mutableStateOf(t.short) }
            var msg by remember(t.id) { mutableStateOf<String?>(null) }
            Text(L("Kulübün adı haberlerde, fikstürde, tabloda ve maçta görünür.", "The club name appears in news, fixtures, tables and matches."), color = TextDim, fontSize = 12.sp)
            OutlinedTextField(name, { name = it.take(32); msg = null }, label = { Text(L("Kulüp adı", "Club name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(short, { short = it.take(3).uppercase(); msg = null }, label = { Text(L("Kısa ad (3 harf)", "Short name (3 letters)")) }, singleLine = true, modifier = Modifier.width(200.dp))
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF34F5C0), Color(0xFF12C795))))
                    .clickable { msg = GameRepo.renameTeam(t, name, short) ?: L("✓ Kaydedildi", "✓ Saved") },
                contentAlignment = Alignment.Center,
            ) { Text(L("Kaydet", "Save"), color = Color(0xFF06231A), fontWeight = FontWeight.Black) }
            msg?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
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
    val labels = listOf(L("Hız", "Pace"), L("Şut", "Shooting"), L("Pas", "Passing"), L("Teknik", "Technique"), L("Savunma", "Defending"), L("Fizik", "Physical"), L("Kalecilik", "Goalkeeping"))
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(name, { name = it.take(28); saved = false }, label = { Text(L("Ad", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SliderRow(L("Yaş", "Age"), age, 15f..42f) { age = it; saved = false }
        SliderRow(L("Potansiyel", "Potential"), pot, 30f..99f) { pot = it; saved = false }
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
        ) { Text(if (saved) L("✓ Kaydedildi (güç ${p.overall})", "✓ Saved (overall ${p.overall})") else L("Kaydet", "Save"), color = Color(0xFF06231A), fontWeight = FontWeight.Black) }
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

/** İsim paketi: düzenlemeleri dışa aktar, tüm isimlerin şablonunu çıkar, başkasının paketini içe aktar. */
@Composable
private fun NamePackPanel(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableIntStateOf(0) } // 0 düzenlemeler, 1 şablon
    val hasPicker = androidx.activity.compose.LocalActivityResultRegistryOwner.current != null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            L("Oyuncu ve kulüp adlarını değiştir, bir dosya olarak arkadaşlarınla paylaş. Başkasının hazırladığı isim paketini içe aktarınca adlar bu kariyerde değişir.",
                "Rename players and clubs and share them as a file with friends. Importing someone else's name pack renames them in this career."),
            fontSize = 13.sp, lineHeight = 17.sp,
        )
        Text(L("Bu kariyerdeki düzenlemeler: ${g.nameEdits.size}", "Edits in this career: ${g.nameEdits.size}"), color = Teal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        var exportPack: (() -> Unit)? = null
        var exportTemplate: (() -> Unit)? = null
        var importPack: (() -> Unit)? = null
        if (hasPicker) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val exporter = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain"),
            ) { uri ->
                if (uri != null) msg = runCatching {
                    val text = if (mode == 1) com.supermenajer.game.game.NamePack.template(g) else com.supermenajer.game.game.NamePack.export(g)
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    L("✓ Dosya kaydedildi.", "✓ File saved.")
                }.getOrElse { L("Kaydedilemedi.", "Couldn't save.") }
            }
            val importer = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                    msg = GameRepo.importNamePack(bytes)
                }
            }
            exportPack = { mode = 0; exporter.launch("SuperMenajer_isimler.txt") }
            exportTemplate = { mode = 1; exporter.launch("SuperMenajer_sablon.txt") }
            importPack = { importer.launch(arrayOf("text/*", "*/*")) }
        }
        PackButton(L("📤 Düzenlemelerimi dışa aktar", "📤 Export my edits"), L("Yalnız değiştirdiğin adlar (paylaşmak için)", "Only the names you changed (for sharing)"), Green) { exportPack?.invoke() }
        PackButton(L("📄 Şablon dışa aktar (tüm adlar)", "📄 Export template (all names)"), L("Bütün kulüp ve oyuncu adları; bilgisayarda/telefonda düzenleyip geri yükle", "Every club and player name; edit it on a computer or phone and import it back"), Teal) { exportTemplate?.invoke() }
        PackButton(L("📥 İsim paketi içe aktar", "📥 Import name pack"), L("Arkadaşından ya da topluluktan gelen .txt paketi", "A .txt pack from a friend or the community"), Gold) { importPack?.invoke() }
        msg?.let { Text(it, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Text(L("Dosya biçimi", "File format"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "K;Galatasarey;Galatasaray;GS\nO;Arda Gülar;Arda Güler",
            fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(8.dp),
        )
        Text(
            L("K = kulüp (eski ad; yeni ad; kısa ad), O = oyuncu (eski ad; yeni ad). # ile başlayan satırlar not.",
                "K = club (old name; new name; short name), O = player (old name; new name). Lines starting with # are comments."),
            color = TextDim, fontSize = 11.sp, lineHeight = 14.sp,
        )
        if (g.nameEdits.isNotEmpty()) {
            Text(L("SON DÜZENLEMELER", "RECENT EDITS"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            g.nameEdits.takeLast(12).reversed().forEach { e ->
                val c = e.split(";")
                if (c.size >= 3) Text("${if (c[0] == "K") "🏟" else "👤"} ${c[1]} → ${c[2]}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PackButton(title: String, sub: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(title, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(sub, color = TextDim, fontSize = 11.sp)
    }
}
