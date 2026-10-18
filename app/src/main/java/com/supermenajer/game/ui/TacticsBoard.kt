package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.math.roundToInt

private class DragState(val fromSlot: Int, val player: Player, val pos: Offset)

/**
 * OSM / FM tarzı taktik tahtası: sahadaki oyuncu kartlarını sürükleyip yer değiştir,
 * yedeklerden kartı (basılı tutup) sahaya sürükle, sahadan yedek alanına sürükleyerek çıkar.
 * Dokunarak seçme de çalışır.
 */
@Composable
fun TacticsBoard(g: Game) {
    GameRepo.version
    val t = g.userTeam
    var sel by remember { mutableIntStateOf(-1) }
    var drag by remember { mutableStateOf<DragState?>(null) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var benchRect by remember { mutableStateOf(Rect.Zero) }
    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    val slots = Formations.slots(t.formation)
    val shirt = Color(t.color1)
    val collar = Color(t.color2)
    val density = LocalDensity.current

    fun drop(d: DragState) {
        val target = slotRects.entries.firstOrNull { it.value.contains(d.pos) }?.key
        when {
            target != null && d.fromSlot >= 0 && target != d.fromSlot -> GameRepo.swapSlots(d.fromSlot, target)
            target != null && d.fromSlot < 0 -> if (d.player.available) GameRepo.putInSlot(target, d.player.id)
            target == null && d.fromSlot >= 0 && benchRect.contains(d.pos) -> GameRepo.removeFromLineup(d.player)
        }
        sel = -1
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootPos = it.positionInRoot() }) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, bottom = 8.dp)) {
            // Saha
            BoxWithConstraints(
                Modifier.fillMaxHeight().aspectRatio(1.4f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(12.dp)),
            ) {
                val bw = maxWidth
                val bh = maxHeight
                val maxDepth = slots.maxOf { it.y }.coerceAtLeast(0.1f)
                Canvas(Modifier.fillMaxSize()) { drawPitch(vertical = false) }
                slots.forEachIndexed { i, s ->
                    val p = g.player(t.lineup.getOrNull(i) ?: -1)
                    val cx = bw * (0.08f + s.y / maxDepth * 0.82f)
                    val cy = bh * (0.1f + s.x * 0.8f)
                    val dragging = drag?.fromSlot == i
                    Box(
                        Modifier.offset(x = cx - 30.dp, y = cy - 24.dp).width(60.dp)
                            .onGloballyPositioned { slotRects[i] = it.boundsInRoot() }
                            .pointerInput(i, p?.id) {
                                if (p == null) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { off -> drag = DragState(i, p, (slotRects[i]?.topLeft ?: Offset.Zero) + off) },
                                    onDrag = { ch, amt -> ch.consume(); drag = drag?.let { DragState(it.fromSlot, it.player, it.pos + amt) } },
                                    onDragEnd = { drag?.let { drop(it) }; drag = null },
                                    onDragCancel = { drag = null },
                                )
                            }
                            .clickable {
                                when {
                                    sel == -1 -> sel = i
                                    sel == i -> sel = -1
                                    else -> { GameRepo.swapSlots(sel, i); sel = -1 }
                                }
                            }
                            .alpha(if (dragging) 0.35f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        PitchToken(g, p, s.pos, sel == i, shirt, collar)
                    }
                }
            }
            // Sağ panel: yedekler / talimatlar
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 10.dp, end = 10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Formations.names.forEach { f -> Chip(f, t.formation == f) { sel = -1; GameRepo.setFormation(f) } }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("👥 Yedekler", GameRepo.tacticsPanel == 0) { GameRepo.tacticsPanel = 0 }
                    Chip("📋 Talimatlar", GameRepo.tacticsPanel == 1) { GameRepo.tacticsPanel = 1 }
                    Spacer(Modifier.weight(1f))
                    val (atk, mid, def) = Lineups.lineStrength(g, t)
                    Text("H $atk · O $mid · S $def", color = TextDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                if (GameRepo.tacticsPanel == 0) {
                    Text(
                        when {
                            drag != null -> "Bir mevkiye bırak · sahadan buraya bırakırsan oyuncu çıkar"
                            sel >= 0 -> "Seçili: ${slots[sel].pos.label}. Bir yedeğe dokun ya da sahada başka bir oyuncuya dokun."
                            else -> "Kartları sürükle: sahada yer değiştir, yedeği basılı tutup sahaya sürükle."
                        },
                        color = if (sel >= 0 || drag != null) Gold else TextDim, fontSize = 11.sp, maxLines = 2,
                    )
                    val pos = if (sel >= 0) slots[sel].pos else null
                    val pool = g.squad(t.id).filter { it.id !in t.lineup }
                        .sortedWith(compareByDescending<Player> { pos != null && it.pos == pos }.thenBy { it.pos.ordinal }.thenByDescending { if (pos != null) it.ovrAt(pos) else it.overall })
                    LazyVerticalGrid(
                        GridCells.Adaptive(66.dp),
                        Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (drag?.fromSlot?.let { it >= 0 } == true) Red.copy(alpha = 0.15f) else Surface1)
                            .onGloballyPositioned { benchRect = it.boundsInRoot() },
                        contentPadding = PaddingValues(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(pool, key = { it.id }) { p ->
                            var myRect by remember { mutableStateOf(Rect.Zero) }
                            val ovr = if (pos != null) p.ovrAt(pos) else p.overall
                            Box(
                                Modifier.onGloballyPositioned { myRect = it.boundsInRoot() }
                                    .pointerInput(p.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { off -> drag = DragState(-1, p, myRect.topLeft + off) },
                                            onDrag = { ch, amt -> ch.consume(); drag = drag?.let { DragState(it.fromSlot, it.player, it.pos + amt) } },
                                            onDragEnd = { drag?.let { drop(it) }; drag = null },
                                            onDragCancel = { drag = null },
                                        )
                                    }
                                    .clickable {
                                        if (sel >= 0 && p.available) { GameRepo.putInSlot(sel, p.id); sel = -1 } else GameRepo.selectedPlayer = p
                                    }
                                    .alpha(if (!p.available) 0.45f else if (drag?.player?.id == p.id) 0.35f else 1f),
                            ) {
                                PlayerCard(p, ovr, p.pos.short, 62.dp, shirt, collar, highlight = if (pos != null && p.pos == pos) Green else Color.Transparent)
                                if (p.injuredWeeks > 0) Text("🚑", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
                                else if (p.suspended > 0) Text("🟥", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd))
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            OutlinedButton(onClick = { sel = -1; GameRepo.autoLineup() }, modifier = Modifier.fillMaxWidth()) { Text("🪄 Otomatik en iyi 11") }
                        }
                    }
                } else {
                    InstructionsPanel(g)
                }
            }
        }
        // Sürüklenen kart parmağı takip eder
        drag?.let { d ->
            val half = with(density) { 31.dp.toPx() }
            Box(
                Modifier.offset { IntOffset((d.pos.x - rootPos.x - half).roundToInt(), (d.pos.y - rootPos.y - half * 1.3f).roundToInt()) },
            ) {
                PlayerCard(d.player, d.player.overall, d.player.pos.short, 62.dp, shirt, collar, highlight = Gold)
            }
        }
    }
}

/** Sahadaki küçük oyuncu göstergesi: yüz, güç rozeti ve soyadı. */
@Composable
private fun PitchToken(g: Game, p: Player?, pos: com.supermenajer.game.model.Pos, selected: Boolean, shirt: Color, collar: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .border(2.dp, if (selected) Color.White else if (p != null && p.pos != pos) Red else Color.Transparent, CircleShape),
            ) {
                if (p != null) PlayerFace(p, 36.dp, shirt, collar) else Box(Modifier.fillMaxSize().background(Color.DarkGray))
            }
            val ovr = p?.ovrAt(pos)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 2.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (ovr == null) Color.Gray else ovrColor(ovr)).padding(horizontal = 3.dp),
            ) { Text(ovr?.toString() ?: "?", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp, lineHeight = 12.sp) }
            if (p != null && !p.available) Text("🚑", fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(
            p?.name?.substringAfterLast(' ') ?: "Boş", fontSize = 9.sp, lineHeight = 10.sp, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp),
        )
    }
}

private class Instr(val key: String, val title: String, val options: List<String>, val info: List<String>)

private val instrs = listOf(
    Instr("tempo", "Tempo", listOf("Yavaş", "Normal", "Hızlı"), listOf("Top sizde kalır, daha az ama sabırlı atak.", "Dengeli oyun temposu.", "Direkt ve hızlı hücum; daha çok şut, daha çok yorgunluk.")),
    Instr("width", "Genişlik", listOf("Dar", "Normal", "Geniş"), listOf("Merkezde kalabalık, orta saha güçlenir.", "Dengeli genişlik.", "Kanatlardan oyun, daha çok orta; dar savunan rakibe karşı etkili.")),
    Instr("passing", "Pas stili", listOf("Kısa", "Karışık", "Uzun"), listOf("Kısa paslarla topa sahip ol.", "Duruma göre pas.", "Uzun toplar; yüksek savunma hattının arkasına etkili.")),
    Instr("defLine", "Savunma hattı", listOf("Derin", "Normal", "Yüksek"), listOf("Geride kal, alan verme.", "Dengeli hat.", "Rakibi sıkıştır; ama hızlı forvetlere açık.")),
)

/** Takım talimatları paneli. */
@Composable
private fun InstructionsPanel(g: Game) {
    val t = g.userTeam
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            InstrRow("Oyun anlayışı", listOf("Defansif", "Dengeli", "Hücum"), t.mentality) { GameRepo.setMentality(it) }
            InstrRow("Pres", listOf("Düşük", "Orta", "Yüksek"), t.pressing) { GameRepo.setPressing(it) }
        }
        items(instrs.size) { i ->
            val ins = instrs[i]
            val v = when (ins.key) { "tempo" -> t.tempo; "width" -> t.width; "passing" -> t.passing; else -> t.defLine }
            InstrRow(ins.title, ins.options, v) { GameRepo.setInstruction(ins.key, it) }
            Text(ins.info[v], color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
        }
        item {
            Text("ÖZEL TALİMATLAR", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("⚡ Kontra atak", t.counter) { GameRepo.setInstruction("counter", if (t.counter) 0 else 1) }
                Chip("🚩 Ofsayt tuzağı", t.offsideTrap) { GameRepo.setInstruction("offside", if (t.offsideTrap) 0 else 1) }
                Chip("⏳ Zaman geçir", t.timeWasting) { GameRepo.setInstruction("timewaste", if (t.timeWasting) 0 else 1) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Kontra: hücuma çıkan rakibe karşı hızlı ataklar. Ofsayt tuzağı: atakları keser ama bozulursa tehlikeli. Zaman geçir: 70'den sonra öndeyken oyunu yavaşlatır.",
                color = TextDim, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun InstrRow(title: String, options: List<String>, value: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 12.sp, color = TextMain, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(96.dp), maxLines = 1)
        options.forEachIndexed { i, o ->
            Chip(o, value == i, Modifier.weight(1f).padding(horizontal = 2.dp)) { onSelect(i) }
        }
    }
}
