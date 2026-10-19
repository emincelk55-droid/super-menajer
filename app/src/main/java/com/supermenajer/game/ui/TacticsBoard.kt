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
import androidx.compose.foundation.lazy.items
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

/** Rol adının FM tarzı kısaltması: "Kanat forvet" → "KF", "Sahte 9" → "S9". */
fun roleAbbr(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.joinToString("") { it.first().toString().uppercase(java.util.Locale.forLanguageTag("tr")) }

/**
 * FM tarzı taktik ekranı: solda gerçek ölçülü saha ve forma şeklinde oyuncular (üstte rol),
 * sağda kadro listesi. Oyuncuları sahada sürükleyip yer değiştir, listeden basılı tutup
 * sahaya sürükle, sahadan listeye sürükleyerek çıkar. Dokunarak seçme de çalışır.
 */
@Composable
fun TacticsBoard(g: Game) {
    GameRepo.version
    // Talimatlar ve roller tam genişlikte; saha ve kadro listesi yalnız Diziliş'te
    when (GameRepo.tacticsPanel) {
        1 -> { InstructionsPanel(g); return }
        2 -> { RolesPanel(g); return }
    }
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
    val R = com.supermenajer.game.game.Roles

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
                Modifier.fillMaxHeight().aspectRatio(PITCH_RATIO, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(10.dp)),
            ) {
                val bw = maxWidth
                val bh = maxHeight
                val maxDepth = slots.maxOf { it.y }.coerceAtLeast(0.1f)
                Canvas(Modifier.fillMaxSize()) { drawFmPitch() }
                slots.forEachIndexed { i, s ->
                    val p = g.player(t.lineup.getOrNull(i) ?: -1)
                    val cx = bw * (0.07f + s.y / maxDepth * 0.8f)
                    val cy = bh * (0.11f + s.x * 0.78f)
                    val dragging = drag?.fromSlot == i
                    Box(
                        Modifier.offset(x = cx - 36.dp, y = cy - 30.dp).width(72.dp)
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
                        val roles = R.byPos.getValue(s.pos)
                        val role = roles[R.roleIndex(t, i).coerceIn(0, roles.size - 1)]
                        PitchToken(g, p, s, sel == i, shirt, collar, roleAbbr(role.name))
                    }
                }
            }
            // Sağ panel
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 10.dp, end = 10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Formations.names.forEach { f -> Chip(f, t.formation == f) { sel = -1; GameRepo.setFormation(f) } }
                }
                Spacer(Modifier.height(6.dp))
                // Takım gücü ve kimya
                val (atk, mid, def) = Lineups.lineStrength(g, t)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface1).padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MiniStat("Hücum", "$atk", ovrColor(atk))
                    MiniStat("Orta saha", "$mid", ovrColor(mid))
                    MiniStat("Savunma", "$def", ovrColor(def))
                    MiniStat("Takım kimyası", "${com.supermenajer.game.game.Dynamics.pct(g.cohesion)}", if (g.cohesion >= 65) Green else if (g.cohesion >= 40) Gold else Red)
                }
                Spacer(Modifier.height(6.dp))
                run {
                    run {
                        Text(
                            when {
                                drag != null -> "Bir mevkiye bırak · sahadan listeye bırakırsan oyuncu çıkar"
                                sel >= 0 -> "Seçili: ${com.supermenajer.game.game.Positions.slotDet(slots[sel]).label}. Listeden bir oyuncuya dokun ya da sahada başka bir oyuncuya dokun."
                                else -> "Oyuncuyu sürükle ya da dokunup seç. Listedeki oyuncuyu basılı tutup sahaya sürükle. ⓘ profili açar."
                            },
                            color = if (sel >= 0 || drag != null) Gold else TextDim, fontSize = 11.sp, maxLines = 2,
                        )
                        val selSlot = if (sel >= 0) slots[sel] else null
                        val xi = slots.indices.mapNotNull { i -> g.player(t.lineup.getOrNull(i) ?: -1)?.let { i to it } }
                        val pool = g.squad(t.id).filter { it.id !in t.lineup }
                            .sortedWith(
                                if (selSlot != null) compareByDescending<Player> { Lineups.score(it, selSlot) }
                                else compareBy<Player> { it.pos.ordinal }.thenBy { com.supermenajer.game.game.Positions.det(it).ordinal }.thenByDescending { it.overall },
                            )
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (drag?.fromSlot?.let { it >= 0 } == true) Red.copy(alpha = 0.15f) else Surface1)
                                .onGloballyPositioned { benchRect = it.boundsInRoot() },
                        ) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().background(Purple).padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("İlk 11", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
                                    Text("Güç", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                    Text("Rol", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                                }
                            }
                            items(xi, key = { "x" + it.second.id }) { (i, p) ->
                                val roles = R.byPos.getValue(slots[i].pos)
                                val role = roles[R.roleIndex(t, i).coerceIn(0, roles.size - 1)]
                                SquadLine(
                                    p, com.supermenajer.game.game.Positions.rating(p, slots[i]), roleAbbr(role.name), selected = sel == i,
                                    onClick = { sel = if (sel == i) -1 else if (sel >= 0) { GameRepo.swapSlots(sel, i); -1 } else i },
                                    dragMod = Modifier,
                                )
                            }
                            item {
                                Text(
                                    "YEDEKLER VE KADRO (${pool.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDim,
                                    modifier = Modifier.fillMaxWidth().background(Surface2).padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                            items(pool, key = { it.id }) { p ->
                                var myRect by remember { mutableStateOf(Rect.Zero) }
                                val exact = selSlot != null && com.supermenajer.game.game.Positions.det(p) == com.supermenajer.game.game.Positions.slotDet(selSlot)
                                Box(
                                    Modifier.onGloballyPositioned { myRect = it.boundsInRoot() }
                                        .background(if (exact) Green.copy(alpha = 0.12f) else Color.Transparent)
                                        .alpha(if (!p.available) 0.45f else if (drag?.player?.id == p.id) 0.35f else 1f),
                                ) {
                                    SquadLine(
                                        p, if (selSlot != null) com.supermenajer.game.game.Positions.rating(p, selSlot) else p.overall, null, selected = false,
                                        onClick = { if (sel >= 0 && p.available) { GameRepo.putInSlot(sel, p.id); sel = -1 } else GameRepo.selectedPlayer = p },
                                        dragMod = Modifier.pointerInput(p.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { off -> drag = DragState(-1, p, myRect.topLeft + off) },
                                                onDrag = { ch, amt -> ch.consume(); drag = drag?.let { DragState(it.fromSlot, it.player, it.pos + amt) } },
                                                onDragEnd = { drag?.let { drop(it) }; drag = null },
                                                onDragCancel = { drag = null },
                                            )
                                        },
                                    )
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { sel = -1; GameRepo.autoLineup() },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                ) { Text("🪄 Otomatik en iyi 11") }
                            }
                        }
                    }
                }
            }
        }
        // Sürüklenen oyuncu parmağı takip eder
        drag?.let { d ->
            val half = with(density) { 31.dp.toPx() }
            Box(
                Modifier.offset { IntOffset((d.pos.x - rootPos.x - half).roundToInt(), (d.pos.y - rootPos.y - half * 1.3f).roundToInt()) },
            ) {
                PlayerCard(d.player, d.player.overall, com.supermenajer.game.game.Positions.det(d.player).code, 62.dp, shirt, collar, highlight = Gold)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 14.sp)
        Text(label, color = TextDim, fontSize = 9.sp, lineHeight = 10.sp)
    }
}

/** FM tarzı kadro satırı: mevki, kondisyon halkası, isim, güç ve rol. */
@Composable
private fun SquadLine(p: Player, ovr: Int, role: String?, selected: Boolean, onClick: () -> Unit, dragMod: Modifier) {
    val det = com.supermenajer.game.game.Positions.det(p)
    Row(
        Modifier.fillMaxWidth().background(if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent)
            .then(dragMod).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(40.dp).clip(RoundedCornerShape(4.dp)).background(posColor(p.pos)).padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) { Text(det.code, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
        Spacer(Modifier.width(8.dp))
        // Kondisyon halkası
        Canvas(Modifier.size(18.dp)) {
            val sw = 3.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.15f), size.minDimension / 2 - sw / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawArc(
                fitColor(p.fitness), -90f, (p.fitness / 100 * 360).toFloat(), false,
                topLeft = Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (p.number > 0) "${p.number}. " else "") + p.name + when {
                    p.injuredWeeks > 0 -> " 🚑"
                    p.suspended > 0 -> " 🟥"
                    else -> ""
                },
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                com.supermenajer.game.game.Positions.codes(p) +
                    " · ${com.supermenajer.game.game.Positions.natRoleName(p)}",
                fontSize = 10.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) { OvrBadge(ovr, 26.dp) }
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            if (role != null) Text(
                role, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Purple).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            else Text(
                "ⓘ", color = TextDim, fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable { GameRepo.selectedPlayer = p }.padding(4.dp),
            )
        }
    }
}

/** Sahadaki oyuncu: forma, rol kısaltması, güç ve isim. Çerçeve rengi mevki uyumunu gösterir. */
@Composable
private fun PitchToken(g: Game, p: Player?, slot: com.supermenajer.game.model.Slot, selected: Boolean, shirt: Color, collar: Color, role: String) {
    val pos = slot.pos
    val fit = p?.let {
        val d = com.supermenajer.game.game.Positions.slotDet(slot)
        if (it.pos != pos && !com.supermenajer.game.game.Positions.canPlay(it, d)) 0.0 else com.supermenajer.game.game.Positions.fit(it, d)
    } ?: 1.0
    val fitC = if (fit < 0.93) Red else if (fit < 1.0) Gold else Green
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            role, color = if (fitC == Green) Color.Black else Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp, lineHeight = 10.sp,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(fitC).padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Box(Modifier.padding(top = 1.dp)) {
            Shirt(shirt, collar, p?.number?.takeIf { it > 0 }?.toString() ?: "", selected, Modifier.size(35.dp, 31.dp))
            val ovr = p?.let { com.supermenajer.game.game.Positions.rating(it, slot) }
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = 6.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (ovr == null) Color.Gray else ovrColor(ovr)).padding(horizontal = 3.dp),
            ) { Text(ovr?.toString() ?: "?", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp, lineHeight = 12.sp) }
            if (p != null && !p.available) Text("🚑", fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-10).dp))
        }
        Text(
            p?.name?.substringAfterLast(' ') ?: "Boş", fontSize = 10.sp, lineHeight = 11.sp, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
        )
    }
}

/** Forma çizimi: gövde, kollar ve yaka; ortada numara. */
@Composable
fun Shirt(body: Color, trim: Color, number: String, selected: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.32f, 0f)
                lineTo(w * 0.68f, 0f)
                lineTo(w, h * 0.2f)
                lineTo(w * 0.88f, h * 0.45f)
                lineTo(w * 0.78f, h * 0.38f)
                lineTo(w * 0.78f, h)
                lineTo(w * 0.22f, h)
                lineTo(w * 0.22f, h * 0.38f)
                lineTo(w * 0.12f, h * 0.45f)
                lineTo(0f, h * 0.2f)
                close()
            }
            drawPath(path, Color.Black.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Fill)
            drawPath(path, body)
            drawPath(path, if (selected) Color.White else trim, style = androidx.compose.ui.graphics.drawscope.Stroke(if (selected) 3.dp.toPx() else 1.5.dp.toPx()))
            drawArc(trim, 0f, 180f, false, Offset(w * 0.38f, -h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.22f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        Text(number, color = contentOn(body), fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.offset(y = 2.dp))
    }
}

/** FM tarzı roller ve görevler: her mevki için rol, kaptan ve duran top kullanıcıları. */
@Composable
private fun RolesPanel(g: Game) {
    val t = g.userTeam
    val slots = Formations.slots(t.formation)
    val R = com.supermenajer.game.game.Roles
    val onPitch = t.lineup.mapNotNull { g.player(it) }
    val cap = R.captain(g, t)
    val pen = R.penaltyTaker(g, t, onPitch)
    val fk = R.freeKickTaker(g, t, onPitch)
    val cor = R.cornerTaker(g, t, onPitch)
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        GridCells.Adaptive(340.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "C: kaptan · P: penaltı · F: frikik · K: korner — dokunarak görev ver. ⭐ oyuncunun doğal özelliği: o rolde %3 daha verimli oynar.",
                color = TextDim, fontSize = 13.sp,
            )
        }
        items(slots.size) { i ->
            val p = g.player(t.lineup.getOrNull(i) ?: -1)
            val roles = R.byPos.getValue(slots[i].pos)
            val cur = R.roleIndex(t, i).coerceIn(0, roles.size - 1)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SlotChip(slots[i])
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            "${p?.number?.takeIf { it > 0 }?.let { "$it. " } ?: ""}${p?.name ?: "Boş"}",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (p != null) Text(com.supermenajer.game.game.Positions.codes(p), color = TextDim, fontSize = 11.sp, maxLines = 1)
                    }
                    if (p != null) {
                        listOf("C" to (cap?.id == p.id), "P" to (pen?.id == p.id), "F" to (fk?.id == p.id), "K" to (cor?.id == p.id)).forEachIndexed { k, (lbl, on) ->
                            Box(
                                Modifier.padding(start = 4.dp).size(30.dp).clip(CircleShape).background(if (on) Gold else Surface2)
                                    .clickable { GameRepo.setDuty(k, p.id) },
                                contentAlignment = Alignment.Center,
                            ) { Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (on) Color.Black else TextDim) }
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val nat = p?.let { if (it.pos == slots[i].pos) com.supermenajer.game.game.Positions.natRole(it) else -1 } ?: -1
                    roles.forEachIndexed { r, role ->
                        Text(
                            (if (r == nat) "⭐ " else "") + role.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (r == cur) Color.Black else TextMain, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (r == cur) Green else Surface2)
                                .clickable { GameRepo.setRole(i, r) }.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                Text(roles[cur].info, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
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
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        GridCells.Adaptive(340.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            InstrCard("Oyun anlayışı", listOf("Defansif", "Dengeli", "Hücum"), t.mentality,
                listOf("Geride kal, kontrayı bekle.", "Dengeli oyun.", "Rakip yarı sahada baskı, daha çok atak.")) { GameRepo.setMentality(it) }
        }
        item {
            InstrCard("Pres", listOf("Düşük", "Orta", "Yüksek"), t.pressing,
                listOf("Rakibi bekle, enerji sakla.", "Dengeli pres.", "Önde bas, top kap; daha çok yorgunluk.")) { GameRepo.setPressing(it) }
        }
        items(instrs.size) { i ->
            val ins = instrs[i]
            val v = when (ins.key) { "tempo" -> t.tempo; "width" -> t.width; "passing" -> t.passing; else -> t.defLine }
            InstrCard(ins.title, ins.options, v, ins.info) { GameRepo.setInstruction(ins.key, it) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(12.dp)) {
                Text("ÖZEL TALİMATLAR", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("⚡ Kontra atak", t.counter) { GameRepo.setInstruction("counter", if (t.counter) 0 else 1) }
                    Chip("🚩 Ofsayt tuzağı", t.offsideTrap) { GameRepo.setInstruction("offside", if (t.offsideTrap) 0 else 1) }
                    Chip("⏳ Zaman geçir", t.timeWasting) { GameRepo.setInstruction("timewaste", if (t.timeWasting) 0 else 1) }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Kontra: hücuma çıkan rakibe karşı hızlı ataklar. Ofsayt tuzağı: atakları keser ama bozulursa tehlikeli. Zaman geçir: 70'den sonra öndeyken oyunu yavaşlatır.",
                    color = TextDim, fontSize = 12.sp,
                )
            }
        }
    }
}

/** Tek talimat kartı: başlık, üç seçenek ve seçilenin açıklaması. */
@Composable
private fun InstrCard(title: String, options: List<String>, value: Int, info: List<String>, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(12.dp)) {
        Text(title.uppercase(), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, o -> Chip(o, value == i, Modifier.weight(1f)) { onSelect(i) } }
        }
        Spacer(Modifier.height(6.dp))
        Text(info.getOrElse(value) { "" }, color = TextDim, fontSize = 12.sp, minLines = 2, maxLines = 2)
    }
}
