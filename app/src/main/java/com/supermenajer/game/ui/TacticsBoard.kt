package com.supermenajer.game.ui

import androidx.compose.material.icons.filled.KeyboardArrowDown

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Dynamics
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Positions
import com.supermenajer.game.game.Roles
import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Slot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private class DragState(val fromSlot: Int, val player: Player, val pos: Offset)

/** Rol adının FM tarzı kısaltması: "Kanat forvet" → "KF", "Sahte 9" → "S9". */
fun roleAbbr(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.joinToString("") { it.first().toString().uppercase(java.util.Locale.forLanguageTag("tr")) }

/* ---------- Perspektif saha geometrisi ---------- */

private const val P_TOP = 0.03f
private const val P_BOTTOM = 0.97f
private const val P_INSET = 0.11f

/** Derinlikteki (0 üst, 1 alt) yan boşluk oranı: üst taraf daha dar. */
private fun insetAt(fy: Float): Float = P_INSET * (1f - ((fy - P_TOP) / (P_BOTTOM - P_TOP)).coerceIn(0f, 1f))

/** Yanal (0 sol..1 sağ) ve dikey (0..1) kesirden perspektif sahadaki x kesri. */
private fun persX(lat: Float, fy: Float): Float {
    val ins = insetAt(fy)
    return ins + lat * (1f - 2 * ins)
}

/** Dizilişin saha dışı oyuncu derinlik aralığı (en geri, en ileri). */
private fun depthRange(slots: List<Slot>): Pair<Float, Float> {
    val out = slots.filter { it.pos != com.supermenajer.game.model.Pos.GK }
    val lo = out.minOfOrNull { it.y } ?: 0f
    val hi = out.maxOfOrNull { it.y } ?: 1f
    return lo to (if (hi - lo < 0.05f) lo + 0.05f else hi)
}

/** Diziliş noktası → saha kesri (x, y). Kendi kalemiz altta, hücum yukarı; kaleci ayrı sırada. */
private fun slotFrac(s: Slot, range: Pair<Float, Float>): Pair<Float, Float> {
    val fy = if (s.pos == com.supermenajer.game.model.Pos.GK) 0.9f
    else 0.65f - ((s.y - range.first) / (range.second - range.first)).coerceIn(0f, 1f) * 0.52f
    val lat = 0.1f + s.x * 0.8f
    return persX(lat, fy) to fy
}

/** SM tarzı perspektif saha: çim şeritleri, çizgiler, kaleler ve kalın kenar. */
private fun DrawScope.drawPerspectivePitch(mini: Boolean = false) {
    val w = size.width
    val h = size.height
    fun pt(lat: Float, fy: Float) = Offset(persX(lat, fy) * w, fy * h)
    fun quad(l0: Float, l1: Float, y0: Float, y1: Float) = Path().apply {
        val a = pt(l0, y0); val b = pt(l1, y0); val c = pt(l1, y1); val d = pt(l0, y1)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    // Kenar kalınlığı (3D)
    val edge = quad(0f, 1f, P_TOP, P_BOTTOM)
    drawPath(edge, Color(0xFF3E7A2E))
    translateY(if (mini) 2f else 7f) { drawPath(edge, Color(0xFF2B5A20)) }
    drawPath(edge, Color(0xFF59B33B))
    // Çim şeritleri ve damalı doku
    val bands = 12
    for (i in 0 until bands) {
        val y0 = P_TOP + (P_BOTTOM - P_TOP) * i / bands
        val y1 = P_TOP + (P_BOTTOM - P_TOP) * (i + 1) / bands
        if (i % 2 == 0) drawPath(quad(0f, 1f, y0, y1), Color(0xFF66C246))
        if (!mini) for (j in 0 until 8) if ((i + j) % 2 == 0) drawPath(quad(j / 8f, (j + 1) / 8f, y0, y1), Color.White.copy(alpha = 0.035f))
    }
    val line = Color.White.copy(alpha = 0.9f)
    val sw = if (mini) 1.2f else 2.6f
    drawPath(quad(0f, 1f, P_TOP, P_BOTTOM), line, style = Stroke(sw))
    val mid = (P_TOP + P_BOTTOM) / 2
    drawLine(line, pt(0f, mid), pt(1f, mid), sw)
    // Orta yuvarlak (perspektifle elips)
    val cw = (persX(0.64f, mid) - persX(0.36f, mid)) * w
    drawOval(line, Offset(w / 2 - cw / 2, mid * h - cw * 0.32f), androidx.compose.ui.geometry.Size(cw, cw * 0.64f), style = Stroke(sw))
    if (mini) return
    drawCircle(line, 3f, Offset(w / 2, mid * h))
    // Ceza sahaları ve kale alanları
    val pa = 0.15f
    val ga = 0.055f
    drawPath(quad(0.22f, 0.78f, P_BOTTOM - pa, P_BOTTOM), line, style = Stroke(sw))
    drawPath(quad(0.37f, 0.63f, P_BOTTOM - ga, P_BOTTOM), line, style = Stroke(sw))
    drawPath(quad(0.22f, 0.78f, P_TOP, P_TOP + pa), line, style = Stroke(sw))
    drawPath(quad(0.37f, 0.63f, P_TOP, P_TOP + ga), line, style = Stroke(sw))
    // Kaleler
    val gl = pt(0.44f, P_BOTTOM)
    val gr = pt(0.56f, P_BOTTOM)
    drawRect(Color.White.copy(alpha = 0.85f), Offset(gl.x, gl.y), androidx.compose.ui.geometry.Size(gr.x - gl.x, h * 0.02f), style = Stroke(sw))
    val tl = pt(0.44f, P_TOP)
    val tr = pt(0.56f, P_TOP)
    drawRect(Color.White.copy(alpha = 0.85f), Offset(tl.x, tl.y - h * 0.018f), androidx.compose.ui.geometry.Size(tr.x - tl.x, h * 0.018f), style = Stroke(sw))
}

private inline fun DrawScope.translateY(dy: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(0f, dy)
    block()
    drawContext.transform.translate(0f, -dy)
}

/** Ok: gövde + üçgen uç. */
private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, width: Float) {
    val ang = atan2(to.y - from.y, to.x - from.x)
    val head = width * 1.9f
    val base = Offset(to.x - cos(ang) * head, to.y - sin(ang) * head)
    drawLine(color, from, base, width)
    val p = Path().apply {
        moveTo(to.x, to.y)
        lineTo(base.x + cos(ang + 1.5708f) * width * 1.3f, base.y + sin(ang + 1.5708f) * width * 1.3f)
        lineTo(base.x - cos(ang + 1.5708f) * width * 1.3f, base.y - sin(ang + 1.5708f) * width * 1.3f)
        close()
    }
    drawPath(p, color)
}

/** Talimat sekmesine göre sahada görsel ipucu (genişlik okları, hücum yönleri, savunma hattı). */
private fun DrawScope.drawInstructionOverlay(g: Game, sub: Int) {
    val t = g.userTeam
    val w = size.width
    val h = size.height
    fun pt(lat: Float, fy: Float) = Offset(persX(lat, fy) * w, fy * h)
    val aw = w * 0.022f
    when (sub) {
        0 -> {
            // Genişlik: dar → içeri, geniş → dışarı
            val c = Color(0xFF7FB2FF).copy(alpha = 0.6f)
            for (fy in listOf(0.3f, 0.55f)) {
                if (t.width == 0) { arrow(pt(0.02f, fy), pt(0.16f, fy), c, aw); arrow(pt(0.98f, fy), pt(0.84f, fy), c, aw) }
                if (t.width == 2) { arrow(pt(0.16f, fy), pt(0.02f, fy), c, aw); arrow(pt(0.84f, fy), pt(0.98f, fy), c, aw) }
            }
            // Mentalite: hücumda ileri, savunmada geri ok
            val m = Color.White.copy(alpha = 0.35f)
            if (t.mentality == 2) arrow(pt(0.5f, 0.62f), pt(0.5f, 0.36f), m, aw * 1.4f)
            if (t.mentality == 0) arrow(pt(0.5f, 0.36f), pt(0.5f, 0.62f), m, aw * 1.4f)
        }
        1 -> {
            val y = Color(0xFFFFE14D).copy(alpha = 0.55f)
            val wide = t.width == 2
            arrow(pt(0.5f, 0.86f), pt(0.5f, 0.12f), y, aw * 1.3f)
            arrow(pt(0.45f, 0.62f), pt(if (wide) 0.12f else 0.3f, 0.18f), y, aw)
            arrow(pt(0.55f, 0.62f), pt(if (wide) 0.88f else 0.7f, 0.18f), y, aw)
            if (t.passing == 2) arrow(pt(0.35f, 0.8f), pt(0.45f, 0.16f), Color(0xFFFF7043).copy(alpha = 0.6f), aw * 0.8f)
            if (t.counter) {
                val gr = Color(0xFF4CE29B).copy(alpha = 0.55f)
                arrow(pt(0.08f, 0.6f), pt(0.08f, 0.18f), gr, aw)
                arrow(pt(0.92f, 0.6f), pt(0.92f, 0.18f), gr, aw)
            }
        }
        2 -> {
            // Pres bölgesi turuncu, savunma hattı çizgi
            val lineY = when (t.defLine) { 0 -> 0.68f; 2 -> 0.46f; else -> 0.57f }
            val pressY = when (t.pressing) { 0 -> 0.5f; 2 -> P_TOP; else -> 0.3f }
            val zone = Path().apply {
                val a = pt(0f, pressY); val b = pt(1f, pressY); val c = pt(1f, lineY); val d = pt(0f, lineY)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
            }
            drawPath(zone, Color(0xFFFF8A00).copy(alpha = 0.22f))
            drawLine(Color(0xFFFF6D00), pt(0f, lineY), pt(1f, lineY), aw * 0.5f)
            if (t.offsideTrap) drawLine(Color.White.copy(alpha = 0.7f), pt(0.05f, lineY - 0.02f), pt(0.95f, lineY - 0.02f), aw * 0.25f)
        }
    }
}

/* ---------- Kart ve küçük bileşenler ---------- */

/** SM tarzı oyuncu kartı: yüz, güç halkası, isim, mevki ve kondisyon çubuğu. */
@Composable
fun SmPlayerCard(
    g: Game, p: Player?, ovr: Int?, code: String, codeColor: Color, selected: Boolean,
    shirt: Color, collar: Color, width: Dp = 64.dp, dim: Boolean = false,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier.width(width).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2B3560), Color(0xFF141A36))))
            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Color.White.copy(alpha = 0.25f), shape)
            .alpha(if (dim) 0.45f else 1f),
    ) {
        Box(Modifier.fillMaxWidth().height(width * 0.62f)) {
            if (p != null) {
                Box(Modifier.align(Alignment.BottomStart).padding(start = 4.dp)) { PlayerFace(p, width * 0.6f, shirt, collar) }
            } else {
                Text("+", color = TextDim, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
            }
            if (ovr != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(width * 0.36f).clip(CircleShape)
                        .background(Color(0xFF0B1028)).border(2.dp, ovrColor(ovr), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$ovr", fontWeight = FontWeight.Black, fontSize = (width.value * 0.17f).sp, color = Color.White) }
            }
            if (p != null && !p.available) Text(if (p.injuredWeeks > 0) "🚑" else "🟥", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
        }
        Text(
            p?.name?.substringAfterLast(' ')?.uppercase() ?: "BOŞ", fontSize = (width.value * 0.14f).sp, lineHeight = (width.value * 0.16f).sp,
            fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 2.dp, vertical = 1.dp),
        )
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF0B1028)).padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (p != null && p.number > 0) Text("${p.number}", color = TextDim, fontSize = (width.value * 0.12f).sp, fontWeight = FontWeight.Bold)
            Text(code, color = codeColor, fontWeight = FontWeight.Black, fontSize = (width.value * 0.15f).sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1)
            if (p != null) Box(Modifier.size(width * 0.12f).clip(CircleShape).background(if (p.morale >= 65) Green else if (p.morale >= 40) Gold else Red))
        }
        if (p != null) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black)) {
                Box(Modifier.fillMaxWidth((p.fitness / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(fitColor(p.fitness)))
            }
        }
    }
}

/** SM tarzı sekme çubuğu: kenarlıklı kutu, seçilende turkuaz alt çizgi. */
@Composable
fun SmTabBar(tabs: List<String>, selected: Int, modifier: Modifier = Modifier, small: Boolean = false, onSelect: (Int) -> Unit) {
    Row(
        modifier.fillMaxWidth().then(
            if (small) Modifier else Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E1430)).border(1.dp, Teal.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        ),
    ) {
        tabs.forEachIndexed { i, t ->
            val sel = i == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(i) }.padding(top = if (small) 6.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    t, fontSize = if (small) 13.sp else if (tabs.size > 4) 14.sp else 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold, color = if (sel) Color.White else TextDim,
                )
                Spacer(Modifier.height(if (small) 5.dp else 8.dp))
                Box(Modifier.fillMaxWidth(0.75f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (sel) Teal else Color.Transparent))
            }
        }
    }
}

/** Açılır seçim kutusu (SM talimat satırları). */
@Composable
fun SmDropdown(value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0B1028))
                .border(1.dp, Teal.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).clickable { open = true }.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(24.dp).background(Teal.copy(alpha = 0.4f)))
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextMain,
                modifier = Modifier.width(36.dp).size(26.dp),
            )
        }
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.background(Color(0xFF141A36))) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o, fontSize = 14.sp) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}

/* ---------- Ana taktik ekranı ---------- */

/**
 * SM tarzı taktik ekranı: solda perspektif saha ve yüzlü oyuncu kartları, sağda
 * Kadro / Diziliş / Talimatlar / Roller sekmeleri. Oyuncuları sürükle-bırak ya da
 * dokunarak seçip değiştir (kadro kartına iki kez dokunmak profili açar).
 */
@Composable
fun TacticsBoard(g: Game) {
    GameRepo.version
    val t = g.userTeam
    val panel = GameRepo.tacticsPanel.coerceIn(0, 3)
    var sel by remember { mutableIntStateOf(-1) }
    var benchSel by remember { mutableIntStateOf(-1) }
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
        benchSel = -1
    }

    fun tapSlot(i: Int) {
        val b = g.player(benchSel)
        when {
            b != null -> { if (b.available) GameRepo.putInSlot(i, b.id); benchSel = -1; sel = -1 }
            sel == -1 -> sel = i
            sel == i -> sel = -1
            else -> { GameRepo.swapSlots(sel, i); sel = -1 }
        }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootPos = it.positionInRoot() }) {
        Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Sol: perspektif saha
            BoxWithConstraints(Modifier.weight(1.08f).fillMaxHeight()) {
                val bw = maxWidth
                val bh = maxHeight
                val range = depthRange(slots)
                Canvas(Modifier.fillMaxSize()) {
                    drawPerspectivePitch()
                    if (panel == 2) drawInstructionOverlay(g, GameRepo.instrSub)
                }
                val cardW = (bh.value * 0.175f).coerceIn(50f, 64f).dp
                slots.forEachIndexed { i, s ->
                    val p = g.player(t.lineup.getOrNull(i) ?: -1)
                    val (fx, fy) = slotFrac(s, range)
                    val d = Positions.slotDet(s)
                    val fit = p?.let { if (it.pos != s.pos && !Positions.canPlay(it, d)) 0.0 else Positions.fit(it, d) } ?: 1.0
                    val fitC = if (fit < 0.93) Red else if (fit < 1.0) Gold else Green
                    val dragging = drag?.fromSlot == i
                    Box(
                        Modifier.offset(x = bw * fx - cardW / 2, y = bh * fy - cardW * 0.62f)
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
                            .clickable { tapSlot(i) }
                            .alpha(if (dragging) 0.35f else 1f),
                    ) {
                        SmPlayerCard(g, p, p?.let { Positions.rating(it, s) }, d.code, fitC, sel == i, shirt, collar, cardW)
                    }
                }
                // Alt köşe: otomatik 11 ve diziliş / kimya kutusu
                Row(Modifier.align(Alignment.BottomStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton("🪄", 44.dp) { sel = -1; benchSel = -1; GameRepo.autoLineup() }
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF7B4DFF), Purple)))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).clickable { GameRepo.tacticWizard = true }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("🧠 Taktik Oluştur", fontWeight = FontWeight.Black, fontSize = 13.sp) }
                }
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xEE0B1028))
                        .border(1.dp, CardLine, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Diziliş", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(t.formation, color = Teal, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(30.dp).background(CardLine))
                    Column {
                        Text("Kimya", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${Dynamics.pct(g.cohesion)}", color = Teal, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Spacer(Modifier.width(4.dp))
                            Bar((g.cohesion / 100).toFloat(), Teal, Modifier.width(36.dp), 4.dp)
                        }
                    }
                }
            }
            // Sağ: sekmeler
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SmTabBar(listOf("Kadro", "Diziliş", "Talimatlar", "Roller", "Antrenman"), panel) { i ->
                    if (i == 4) GameRepo.tacticsMode = 1 else GameRepo.tacticsPanel = i
                    sel = -1; benchSel = -1
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                        .border(1.dp, if (drag?.fromSlot?.let { it >= 0 } == true && panel == 0) Red else CardLine, RoundedCornerShape(14.dp))
                        .onGloballyPositioned { if (panel == 0) benchRect = it.boundsInRoot() },
                ) {
                    when (panel) {
                        0 -> SquadPanel(
                            g, slots, sel, benchSel, drag, shirt, collar,
                            onTap = { p ->
                                when {
                                    sel >= 0 && p.available -> { GameRepo.putInSlot(sel, p.id); sel = -1 }
                                    benchSel == p.id -> { GameRepo.selectedPlayer = p; benchSel = -1 }
                                    else -> benchSel = p.id
                                }
                            },
                            onDragStart = { p, pos -> drag = DragState(-1, p, pos) },
                            onDrag = { amt -> drag = drag?.let { DragState(it.fromSlot, it.player, it.pos + amt) } },
                            onDragEnd = { drag?.let { drop(it) }; drag = null },
                        )
                        1 -> FormationPanel(g) { sel = -1; benchSel = -1; GameRepo.setFormation(it) }
                        2 -> InstructionsPanel(g, GameRepo.instrSub) { GameRepo.instrSub = it }
                        else -> RolesPanel(g)
                    }
                }
            }
        }
        if (GameRepo.tacticWizard) TacticWizard(g) { GameRepo.tacticWizard = false }
        if (GameRepo.roleDialog in slots.indices) RoleDialog(g, GameRepo.roleDialog) { GameRepo.roleDialog = -1 }
        // Sürüklenen oyuncu parmağı takip eder
        drag?.let { d ->
            val half = with(density) { 32.dp.toPx() }
            Box(Modifier.offset { IntOffset((d.pos.x - rootPos.x - half).roundToInt(), (d.pos.y - rootPos.y - half).roundToInt()) }) {
                SmPlayerCard(g, d.player, d.player.overall, Positions.det(d.player).code, Teal, true, shirt, collar)
            }
        }
    }
}

/** Kadro sekmesi: yedekler (en iyi 7) ve kadronun geri kalanı kart ızgarasında. */
@Composable
private fun SquadPanel(
    g: Game, slots: List<Slot>, sel: Int, benchSel: Int, drag: DragState?, shirt: Color, collar: Color,
    onTap: (Player) -> Unit, onDragStart: (Player, Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit,
) {
    val t = g.userTeam
    val selSlot = slots.getOrNull(sel)
    val pool = g.squad(t.id).filter { it.id !in t.lineup }
    val bench = pool.filter { it.available }.sortedByDescending { it.overall }.take(7)
    val rest = (pool - bench.toSet()).sortedWith(compareBy<Player> { it.pos.ordinal }.thenBy { Positions.det(it).ordinal }.thenByDescending { it.overall })
    LazyVerticalGrid(
        GridCells.Adaptive(68.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                when {
                    drag != null -> "Sahaya bırak · sahadan buraya bırakırsan oyuncu kadro dışı kalır"
                    sel >= 0 -> "${Positions.slotDet(slots[sel]).label} için bir oyuncu seç (yeşil: doğal mevki)"
                    benchSel >= 0 -> "Şimdi sahada bir mevkiye dokun · tekrar dokun: profil"
                    else -> "Dokun ve sahaya koy, basılı tutup sürükle. İki kez dokun: profil."
                },
                color = if (sel >= 0 || benchSel >= 0 || drag != null) Gold else TextDim, fontSize = 11.sp, maxLines = 2,
            )
        }
        for ((title, list) in listOf("Yedekler" to bench, "Kadro" to rest)) {
            if (list.isEmpty()) continue
            item(span = { GridItemSpan(maxLineSpan) }) { Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp) }
            items(list, key = { it.id }) { p ->
                var rect by remember { mutableStateOf(Rect.Zero) }
                val d = Positions.det(p)
                val exact = selSlot != null && Positions.canPlay(p, Positions.slotDet(selSlot))
                Box(
                    Modifier.onGloballyPositioned { rect = it.boundsInRoot() }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (exact) Green.copy(alpha = 0.25f) else Color.Transparent)
                        .pointerInput(p.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { off -> onDragStart(p, rect.topLeft + off) },
                                onDrag = { ch, amt -> ch.consume(); onDrag(amt) },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragEnd,
                            )
                        }
                        .clickable { onTap(p) }
                        .alpha(if (drag?.player?.id == p.id) 0.35f else 1f),
                    contentAlignment = Alignment.Center,
                ) {
                    SmPlayerCard(
                        g, p, selSlot?.let { Positions.rating(p, it) } ?: p.overall, d.code, posColor(p.pos),
                        benchSel == p.id, shirt, collar, 64.dp, dim = !p.available,
                    )
                }
            }
        }
    }
}

/** Diziliş sekmesi: küçük saha resimleriyle diziliş seçimi ve hat güçleri. */
@Composable
private fun FormationPanel(g: Game, onSelect: (String) -> Unit) {
    val t = g.userTeam
    val (atk, mid, def) = Lineups.lineStrength(g, t)
    LazyVerticalGrid(
        GridCells.Adaptive(116.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LineStatBox("Hücum", atk)
                LineStatBox("Orta saha", mid)
                LineStatBox("Savunma", def)
            }
        }
        items(Formations.names) { f ->
            val selected = t.formation == f
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.clip(shape)
                    .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF1C2B5A), Color(0xFF2A1450))) else Brush.verticalGradient(listOf(Color(0xFF111735), Color(0xFF0B1028))))
                    .border(if (selected) 2.dp else 1.dp, if (selected) Teal else CardLine, shape)
                    .clickable { onSelect(f) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fs = Formations.slots(f)
                val md = depthRange(fs)
                Canvas(Modifier.fillMaxWidth().aspectRatio(1.15f)) {
                    drawPerspectivePitch(mini = true)
                    fs.forEach { s ->
                        val (fx, fy) = slotFrac(s, md)
                        drawCircle(Color.White, size.width * 0.045f, Offset(fx * size.width, fy * size.height))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(f, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (selected) Teal else TextMain)
            }
        }
    }
}

@Composable
private fun LineStatBox(label: String, v: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$v", color = ovrColor(v), fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun InstrRow(label: String, info: String, value: String, options: List<String>, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(info, color = TextDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        SmDropdown(value, options, Modifier.width(180.dp), onSelect)
    }
    HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
}

private val yesNo = listOf("Hayır", "Evet")

/** Talimatlar sekmesi: Genel / Hücum / Savunma / Takım görevleri alt sekmeleri ve açılır seçimler. */
@Composable
private fun InstructionsPanel(g: Game, sub: Int, onSub: (Int) -> Unit) {
    val t = g.userTeam
    Column(Modifier.fillMaxSize()) {
        SmTabBar(listOf("Genel", "Hücum", "Savunma", "Takım görevleri"), sub, Modifier.padding(horizontal = 6.dp), small = true, onSelect = onSub)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            when (sub) {
                0 -> {
                    item {
                        val o = listOf("Defansif", "Dengeli", "Hücum")
                        val info = listOf("Geride kal, kontrayı bekle.", "Dengeli oyun.", "Rakip yarı sahada baskı, daha çok atak.")
                        InstrRow("Mentalite", info[t.mentality], o[t.mentality], o) { GameRepo.setMentality(it) }
                    }
                    item {
                        val o = listOf("Yavaş", "Normal", "Hızlı")
                        val info = listOf("Top sizde kalır, sabırlı atak.", "Dengeli oyun temposu.", "Direkt ve hızlı hücum; daha çok yorgunluk.")
                        InstrRow("Tempo", info[t.tempo], o[t.tempo], o) { GameRepo.setInstruction("tempo", it) }
                    }
                    item {
                        val o = listOf("Daha dar", "Normal", "Daha geniş")
                        val info = listOf("Merkezde kalabalık, orta saha güçlenir.", "Dengeli genişlik.", "Kanatlardan oyun, daha çok orta.")
                        InstrRow("Genişlik", info[t.width], o[t.width], o) { GameRepo.setInstruction("width", it) }
                    }
                }
                1 -> {
                    item {
                        val o = listOf("Kısa", "Karışık", "Uzun")
                        val info = listOf("Kısa paslarla topa sahip ol.", "Duruma göre pas.", "Uzun toplar; yüksek savunmanın arkasına etkili.")
                        InstrRow("Pas tarzı", info[t.passing], o[t.passing], o) { GameRepo.setInstruction("passing", it) }
                    }
                    item {
                        InstrRow("Kontra atak", "Hücuma çıkan rakibe karşı hızlı ataklar.", yesNo[if (t.counter) 1 else 0], yesNo) { GameRepo.setInstruction("counter", it) }
                    }
                    item {
                        val o = listOf("Merkezden", "Karışık", "İki kanattan")
                        InstrRow("Hücum yönü", "Genişlik ayarıyla birlikte çalışır.", o[t.width], o) { GameRepo.setInstruction("width", it) }
                    }
                }
                2 -> {
                    item {
                        val o = listOf("Düşük", "Orta", "Tüm saha")
                        val info = listOf("Rakibi bekle, enerji sakla.", "Dengeli pres.", "Önde bas, top kap; daha çok yorgunluk.")
                        InstrRow("Pres", info[t.pressing], o[t.pressing], o) { GameRepo.setPressing(it) }
                    }
                    item {
                        val o = listOf("Derin", "Normal", "Yüksek")
                        val info = listOf("Geride kal, alan verme.", "Dengeli hat.", "Rakibi sıkıştır; hızlı forvetlere açık.")
                        InstrRow("Savunma çizgisi", info[t.defLine], o[t.defLine], o) { GameRepo.setInstruction("defLine", it) }
                    }
                    item {
                        InstrRow("Ofsayt tuzağı", "Atakları keser ama bozulursa tehlikeli.", yesNo[if (t.offsideTrap) 1 else 0], yesNo) { GameRepo.setInstruction("offside", it) }
                    }
                    item {
                        InstrRow("Zaman geçirme", "70'den sonra öndeyken oyunu yavaşlatır.", yesNo[if (t.timeWasting) 1 else 0], yesNo) { GameRepo.setInstruction("timewaste", it) }
                    }
                }
                else -> dutiesItems(g)
            }
        }
    }
}

/** Roller sekmesi: her mevki için oyuncu, rol açıklaması ve açılır rol seçimi. */
@Composable
private fun RolesPanel(g: Game) {
    val t = g.userTeam
    val slots = Formations.slots(t.formation)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        item {
            Text("⭐ oyuncunun doğal rolü: o rolde %3 daha verimli oynar. Ayrıntı ve markaj için oyuncuya dokun.", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(slots.size) { i ->
            val p = g.player(t.lineup.getOrNull(i) ?: -1)
            val roles = Roles.byPos.getValue(slots[i].pos)
            val cur = Roles.roleIndex(t, i).coerceIn(0, roles.size - 1)
            val nat = p?.let { if (it.pos == slots[i].pos) Positions.natRole(it) else -1 } ?: -1
            val d = Positions.slotDet(slots[i])
            Column(Modifier.fillMaxWidth().clickable { GameRepo.roleDialog = i }.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(20.dp).background(posColor(slots[i].pos)))
                    Spacer(Modifier.width(8.dp))
                    Text(d.code, color = posColor(slots[i].pos), fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.width(48.dp))
                    Text(p?.name ?: "Boş", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (t.marking.getOrNull(i) == 1) Text("🎯 Adam adama", color = Gold, fontSize = 11.sp)
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(roles[cur].info, color = TextMain.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 3)
                    SmDropdown(
                        (if (cur == nat) "⭐ " else "") + roles[cur].name,
                        roles.mapIndexed { r, role -> (if (r == nat) "⭐ " else "") + role.name },
                        Modifier.width(190.dp),
                    ) { GameRepo.setRole(i, it) }
                }
            }
            HorizontalDivider(color = CardLine.copy(alpha = 0.6f))
        }
    }
}

/** FM tarzı "Taktik Oluştur": oyun tarzı seç, kadroya uygunluğunu gör, önerilen dizilişle uygula. */
@Composable
fun TacticWizard(g: Game, onClose: () -> Unit) {
    val S = com.supermenajer.game.game.TacticStyles
    val ratings = remember { S.ratings(g) }
    val best = remember { ratings.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet() }
    var sel by remember { mutableStateOf(ratings.maxBy { it.value }.key) }
    var formation by remember(sel) { mutableStateOf(sel.formations.first()) }
    val (pros, cons) = remember(sel) { S.prosCons(g, sel) }
    WideDialog(onDismissRequest = onClose, maxWidth = 900.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.dp, CardLine, RoundedCornerShape(16.dp)).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Taktik Oluştur", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.weight(1f))
                Text("★ Kadrona önerilen tarz", color = Teal, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.weight(1f)) {
                LazyColumn(Modifier.width(250.dp).fillMaxHeight()) {
                    items(S.all.size) { i ->
                        val s = S.all[i]
                        val r = ratings.getValue(s)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (s === sel) Purple.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { sel = s }.padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape).background(if (s === sel) Purple else Color.Transparent)
                                    .border(2.dp, if (s === sel) Purple else TextDim, CircleShape),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text((if (s in best) "★ " else "") + s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (s in best) Teal else TextMain, modifier = Modifier.weight(1f))
                            // Uygunluk: 5 parçalı çubuk
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { k ->
                                    Box(Modifier.size(7.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(if (k < r) (if (r >= 3) Green else Red) else Color.White.copy(alpha = 0.12f)))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Text(sel.name, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Teal)
                    Text(sel.desc, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    pros.forEach { Text("✅ $it", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                    cons.forEach { Text("⛔ $it", fontSize = 13.sp, color = Color(0xFFFFB4A8), modifier = Modifier.padding(vertical = 2.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Text("ÖNERİLEN DİZİLİŞLER", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sel.formations.forEach { f ->
                            val on = f == formation
                            Column(
                                Modifier.width(100.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0B1028))
                                    .border(if (on) 2.dp else 1.dp, if (on) Teal else CardLine, RoundedCornerShape(10.dp))
                                    .clickable { formation = f }.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val fs = Formations.slots(f)
                                val md = depthRange(fs)
                                Canvas(Modifier.fillMaxWidth().aspectRatio(1.2f)) {
                                    drawPerspectivePitch(mini = true)
                                    fs.forEach { s ->
                                        val (fx, fy) = slotFrac(s, md)
                                        drawCircle(Color.White, size.width * 0.045f, Offset(fx * size.width, fy * size.height))
                                    }
                                }
                                Text((if (f == sel.formations.first()) "★ " else "") + f, fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (on) Teal else TextMain)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val m = listOf("Defansif", "Dengeli", "Hücum")[sel.mentality]
                    val lvl = listOf("Düşük", "Orta", "Yüksek")
                    Text(
                        "Ayarlar: $m · Tempo ${lvl[sel.tempo]} · Genişlik ${lvl[sel.width]} · Pres ${lvl[sel.pressing]} · Savunma hattı ${lvl[sel.defLine]}" +
                            if (sel.counter) " · Kontra atak" else "",
                        color = TextDim, fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1B2452)).border(1.dp, CardLine, RoundedCornerShape(10.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Text("İptal", fontWeight = FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF7B4DFF), Purple)))
                        .clickable { S.apply(g, sel, formation); onClose() },
                    contentAlignment = Alignment.Center,
                ) { Text("Taktiği Onayla", fontWeight = FontWeight.Black) }
            }
        }
    }
}


private fun setStars(v: Double) = ((v - 45) / 9).roundToInt().coerceIn(1, 5)

/** Liderlik (1-20): yaş/tecrübe ve lider kişilik. */
fun leadership(p: Player): Int {
    val exp = ((p.age - 22) * 2.2).coerceIn(-8.0, 14.0)
    return ((30 + exp * 2 + if (com.supermenajer.game.game.Traits.isLeader(p)) 35 else 0) / 5).roundToInt().coerceIn(1, 20)
}

/** Takım görevleri: FM tarzı duran top kullanıcıları ve kaptan tabloları. */
private fun androidx.compose.foundation.lazy.LazyListScope.dutiesItems(g: Game) {
    val t = g.userTeam
    val xi = t.lineup.mapNotNull { g.player(it) }.filter { it.pos != com.supermenajer.game.model.Pos.GK }
    val pen = Roles.penaltyTaker(g, t, xi)
    val fk = Roles.freeKickTaker(g, t, xi)
    val cor = Roles.cornerTaker(g, t, xi)
    item {
        Text("DURAN TOP KULLANICILARI", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Oyuncu", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            listOf("Penaltı", "Frikik", "Korner").forEach { Text(it, color = TextDim, fontSize = 11.sp, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center) }
        }
    }
    items(xi.size) { i ->
        val p = xi[i]
        val vals = listOf(
            p.shooting * 0.75 + p.dribbling * 0.25 to (pen?.id == p.id),
            p.shooting * 0.5 + p.passing * 0.5 to (fk?.id == p.id),
            p.passing * 0.8 + p.dribbling * 0.2 to (cor?.id == p.id),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Positions.det(p).code, color = posColor(p.pos), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(36.dp))
            Text(p.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            vals.forEachIndexed { k, (v, on) ->
                val st = setStars(v)
                Box(
                    Modifier.width(72.dp).height(28.dp).padding(horizontal = 3.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (on) Gold.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (on) Gold else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { GameRepo.setDuty(k + 1, p.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("★".repeat(st) + "☆".repeat(5 - st), color = if (on) Gold else TextDim, fontSize = 10.sp) }
            }
        }
    }
    item {
        Text("Bir yıldız kutusuna dokunarak o görevi ver. Sarı kutu: şu anki kullanıcı.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        Text("KAPTANLAR", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Oyuncu", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            listOf("Durum" to 64, "Grup" to 64, "Yaş" to 34, "Lid" to 34, "" to 76).forEach { (h, w) ->
                Text(h, color = TextDim, fontSize = 11.sp, modifier = Modifier.width(w.dp), textAlign = TextAlign.Center)
            }
        }
    }
    val cap = Roles.captain(g, t)
    val vice = g.player(t.viceCaptain)
    val tiers = Dynamics.tiers(g, t)
    val squad = g.squad(t.id).filter { !it.reserve }.sortedByDescending { leadership(it) * 10 + it.overall / 10 }
    items(squad.size) { i ->
        val p = squad[i]
        val tier = when { tiers[0]?.contains(p) == true -> "Lider"; tiers[1]?.contains(p) == true -> "Etkili"; else -> "Normal" }
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = if (p.id == cap?.id || p.id == vice?.id) FontWeight.Black else FontWeight.Normal)
            Text(tier, fontSize = 11.sp, color = if (tier == "Lider") Gold else TextMain, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
            Text(Dynamics.groupOf(g, t, p).substringBefore(" "), fontSize = 11.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center, maxLines = 1)
            Text("${p.age}", fontSize = 12.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
            Text("${leadership(p)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (leadership(p) >= 15) Green else TextMain, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
            Row(Modifier.width(76.dp), horizontalArrangement = Arrangement.Center) {
                listOf("C" to (cap?.id == p.id), "V" to (vice?.id == p.id)).forEachIndexed { k, (l, on) ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(28.dp).clip(CircleShape).background(if (on) Gold else Color(0xFF1B2452))
                            .clickable { if (k == 0) GameRepo.setDuty(0, p.id) else GameRepo.setViceCaptain(p.id) },
                        contentAlignment = Alignment.Center,
                    ) { Text(l, fontWeight = FontWeight.Black, fontSize = 12.sp, color = if (on) Color.Black else TextDim) }
                }
            }
        }
    }
}


/** Rolün önemli özellikleri (rol katsayılarından). */
private fun keyAttrs(r: Roles.Role, pos: com.supermenajer.game.model.Pos): String {
    if (pos == com.supermenajer.game.model.Pos.GK) return if (r.mid > 0) "Refleks / Ayakla oyun / Bire bir / Pas" else "Refleks / Bire bir / Hava hakimiyeti / İletişim"
    val list = listOf(
        r.atk to "Hareketlenme / Hız", r.mid to "Pas / Karar alma", r.def to "Top kapma / Markaj / Pozisyon alma",
        r.shot - 1.0 to "Bitiricilik / Soğukkanlılık", r.assist - 1.0 to "Yaratıcılık / Orta yapma", r.header - 1.0 to "Kafa / Güç",
    )
    return list.sortedByDescending { it.first }.take(3).joinToString(" / ") { it.second }
}

/** FM tarzı rol penceresi: roller (★ önerilen), açıklama, önemli özellikler ve markaj tipi. */
@Composable
private fun RoleDialog(g: Game, slot: Int, onClose: () -> Unit) {
    val t = g.userTeam
    val slots = Formations.slots(t.formation)
    val s = slots[slot]
    val p = g.player(t.lineup.getOrNull(slot) ?: -1)
    val roles = Roles.byPos.getValue(s.pos)
    var sel by remember { mutableIntStateOf(Roles.roleIndex(t, slot).coerceIn(0, roles.size - 1)) }
    var marking by remember { mutableIntStateOf(t.marking.getOrNull(slot) ?: 0) }
    val nat = p?.let { if (it.pos == s.pos) Positions.natRole(it) else -1 } ?: -1
    WideDialog(onDismissRequest = onClose, maxWidth = 720.dp) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                .border(1.dp, CardLine, RoundedCornerShape(14.dp)).verticalScroll(rememberScrollState()),
        ) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p?.name ?: Positions.slotDet(s).label, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("✕", color = Color.Black, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onClose).padding(4.dp))
            }
            Column(Modifier.padding(14.dp)) {
                Row {
                    Text("Bu oyuncuyu görevlendirmek istediğin taktiksel role dokun", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("★ Önerilen rol", color = Teal, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                roles.indices.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEach { r ->
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { sel = r }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(20.dp).clip(CircleShape).background(if (r == sel) Purple else Color.Transparent)
                                        .border(2.dp, if (r == sel) Purple else TextDim, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { if (r == sel) Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    roles[r].name + if (r == nat) " ★" else "", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = if (r == sel) Teal else TextMain,
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (s.pos == com.supermenajer.game.model.Pos.DF || s.pos == com.supermenajer.game.model.Pos.MF) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Markaj:", fontSize = 13.sp, modifier = Modifier.width(70.dp))
                        listOf("Alan", "Oyuncu").forEachIndexed { k, l ->
                            Box(
                                Modifier.weight(1f).height(32.dp).background(if (marking == k) Purple else Color.Transparent).clickable { marking = k },
                                contentAlignment = Alignment.Center,
                            ) { Text(l, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                    Text(
                        if (marking == 0) "Alan savunması: oyuncu kendi bölgesini korur, dengeli oyun." else "Adam adama: en tehlikeli rakibi yakından takip eder; savunma güçlenir, oyun kurulumu biraz zayıflar.",
                        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0B1028)).padding(10.dp)) {
                    Text(roles[sel].info, fontSize = 14.sp)
                    Text("Önemli Özellikler: ${keyAttrs(roles[sel], s.pos)}", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF7B4DFF), Purple)))
                        .clickable { GameRepo.setRole(slot, sel); GameRepo.setMarking(slot, marking); onClose() },
                    contentAlignment = Alignment.Center,
                ) { Text("Onayla", fontWeight = FontWeight.Black) }
            }
        }
    }
}
