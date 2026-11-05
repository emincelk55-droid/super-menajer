package com.supermenajer.game.ui

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.supermenajer.game.engine.Pitch2D
import com.supermenajer.game.engine.Pitch2D.Companion.L
import com.supermenajer.game.engine.Pitch2D.Companion.W

/** Sahanın dışında kalan pay (metre): kaleler ve taç çizgisi görünür kalsın. */
private const val M = 3.2f

/** Saha en-boy oranı (kenar payları dahil). */
const val PITCH_RATIO = (L + 2 * M) / (W + 2 * M)

/**
 * FM tarzı 2D maç sahası. Her karede [Pitch2D] ilerletilir: oyuncular ve top
 * akıcı şekilde hareket eder. running=false iken (devre arası, duraklatma) sahne donar.
 */
@Composable
fun MatchPitch(
    anim: Pitch2D, homeC: Color, awayC: Color, homeGk: Color, awayGk: Color,
    running: Boolean, timeScale: Float, modifier: Modifier = Modifier,
) {
    var frame by remember { mutableLongStateOf(0L) }
    val run by rememberUpdatedState(running)
    val scale by rememberUpdatedState(timeScale)
    LaunchedEffect(anim) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L && (run || anim.busy)) anim.update((t - last) / 1e9f * scale)
                last = t
                frame = t
            }
        }
    }
    val measurer = rememberTextMeasurer()
    val numCache = remember { HashMap<String, TextLayoutResult>() }
    Canvas(modifier.pointerInput(anim) { detectTapGestures { anim.skipReplay() } }) {
        frame // her karede yeniden çiz
        if (size.minDimension <= 0f) return@Canvas
        val sx = size.width / (L + 2 * M)
        val sy = size.height / (W + 2 * M)
        fun px(x: Float) = (x + M) * sx
        fun py(y: Float) = (y + M) * sy
        drawMetricPitch(sx, sy)
        val r = 1.35f * sx
        val rv = anim.replayView
        if (rv != null) {
            drawReplay(rv, r, sx, ::px, ::py, homeC, awayC, homeGk, awayGk, measurer, numCache, size)
            return@Canvas
        }
        // Ofsayt çizgisi: atakta savunmanın sondan ikinci oyuncusunun hizası
        anim.offsideLineX?.let { ox ->
            drawLine(
                Color(0xFFFFE14D).copy(alpha = 0.55f), Offset(px(ox), py(0f)), Offset(px(ox), py(W)), strokeWidth = 0.18f * sx,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1.2f * sx, 0.8f * sx)),
            )
        }
        // Oyuncu gölgeleri
        for (a in anim.agents) if (a.alpha > 0f) drawCircle(Color.Black.copy(alpha = 0.22f * a.alpha), r, Offset(px(a.x) + r * 0.25f, py(a.y) + r * 0.3f))
        val focus = anim.focus()
        for (a in anim.agents) {
            if (a.alpha <= 0f) continue
            val fill = if (a.gk) (if (a.side == 0) homeGk else awayGk) else if (a.side == 0) homeC else awayC
            val c = Offset(px(a.x), py(a.y))
            drawCircle(fill.copy(alpha = a.alpha), r, c)
            drawCircle(if (a === focus) Color(0xFFFFE14D) else Color.White.copy(alpha = 0.85f * a.alpha), r, c, style = Stroke(if (a === focus) r * 0.22f else r * 0.12f))
            if (a.number > 0 && a.alpha > 0.5f) {
                val key = "${a.number}-${contentOn(fill).hashCode()}-${r.toInt()}"
                val tl = numCache.getOrPut(key) {
                    measurer.measure(
                        "${a.number}",
                        TextStyle(color = contentOn(fill), fontSize = (r * 0.95f / density / fontScale).sp, fontWeight = FontWeight.Black),
                    )
                }
                drawText(tl, topLeft = Offset(c.x - tl.size.width / 2f, c.y - tl.size.height / 2f))
            }
        }
        // Topu süren oyuncunun adı
        if (focus != null) {
            val tl = measurer.measure(
                focus.name,
                TextStyle(color = Color.White, fontSize = (r * 0.85f / density / fontScale).sp, fontWeight = FontWeight.Bold),
            )
            val tx = (px(focus.x) - tl.size.width / 2f).coerceIn(2f, (size.width - tl.size.width - 2f).coerceAtLeast(2f))
            val ty = (py(focus.y) + r * 1.25f).coerceAtMost(size.height - tl.size.height - 2f).coerceAtLeast(0f)
            drawRoundRect(
                Color.Black.copy(alpha = 0.55f), Offset(tx - 6f, ty - 1f), Size(tl.size.width + 12f, tl.size.height + 2f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawText(tl, topLeft = Offset(tx, ty))
        }
        // Top: yerde gölge, havadayken yukarıda ve büyük
        val bx = px(anim.ballX)
        val by = py(anim.ballY)
        val z = anim.ballZ
        val br = 0.62f * sx * (1f + z * 0.06f)
        drawCircle(Color.Black.copy(alpha = (0.4f - z * 0.03f).coerceAtLeast(0.12f)), 0.6f * sx, Offset(bx + z * sx * 0.25f, by + z * sx * 0.12f))
        val ball = Offset(bx, by - z * sx * 0.55f)
        drawCircle(Color.White, br, ball)
        drawCircle(Color(0xFF263238), br, ball, style = Stroke(br * 0.25f))
    }
}

/** Kenar paylı gerçek ölçülü saha: tüm çizim alanına sığar (oran [PITCH_RATIO]). */
fun DrawScope.drawFmPitch() = drawMetricPitch(size.width / (L + 2 * M), size.height / (W + 2 * M))

/** Gerçek ölçülerle saha (105x68 m): çim şeritleri, ceza sahaları, kaleler. */
private fun DrawScope.drawMetricPitch(sx: Float, sy: Float) {
    fun px(x: Float) = (x + M) * sx
    fun py(y: Float) = (y + M) * sy
    drawRect(Color(0xFF2E8B47))
    val stripes = 14
    for (i in 0 until stripes) {
        if (i % 2 == 0) continue
        drawRect(Color(0xFF35994F), Offset(px(L * i / stripes), 0f), Size(L / stripes * sx + 1f, size.height))
    }
    val line = Color.White.copy(alpha = 0.8f)
    val sw = (0.22f * sx).coerceAtLeast(1.5f)
    fun rect(x: Float, y: Float, w: Float, h: Float) = drawRect(line, Offset(px(x), py(y)), Size(w * sx, h * sy), style = Stroke(sw))
    rect(0f, 0f, L, W)
    drawLine(line, Offset(px(L / 2), py(0f)), Offset(px(L / 2), py(W)), sw)
    drawCircle(line, 9.15f * sx, Offset(px(L / 2), py(W / 2)), style = Stroke(sw))
    drawCircle(line, 0.35f * sx, Offset(px(L / 2), py(W / 2)))
    for (left in listOf(true, false)) {
        val x0 = if (left) 0f else L - 16.5f
        rect(x0, W / 2 - 20.16f, 16.5f, 40.32f)
        val x1 = if (left) 0f else L - 5.5f
        rect(x1, W / 2 - 9.16f, 5.5f, 18.32f)
        val spot = if (left) 11f else L - 11f
        drawCircle(line, 0.3f * sx, Offset(px(spot), py(W / 2)))
        // Ceza yayı
        drawArc(
            line, if (left) -53f else 127f, 106f, false,
            Offset(px(spot - 9.15f), py(W / 2 - 9.15f)), Size(18.3f * sx, 18.3f * sy), style = Stroke(sw),
        )
        // Kale
        val gx = if (left) -2f else L
        drawRect(Color.White.copy(alpha = 0.25f), Offset(px(gx), py(W / 2 - Pitch2D.GOAL_HALF)), Size(2f * sx, 2 * Pitch2D.GOAL_HALF * sy))
        drawRect(Color.White, Offset(px(gx), py(W / 2 - Pitch2D.GOAL_HALF)), Size(2f * sx, 2 * Pitch2D.GOAL_HALF * sy), style = Stroke(sw))
    }
}

/** Gol tekrarı: kayıttan ağır çekim, kenarlarda sinema bandı ve "TEKRAR" etiketi. */
private fun DrawScope.drawReplay(
    rv: Pitch2D.ReplayView, r: Float, sx: Float, px: (Float) -> Float, py: (Float) -> Float,
    homeC: Color, awayC: Color, homeGk: Color, awayGk: Color,
    measurer: androidx.compose.ui.text.TextMeasurer, numCache: HashMap<String, TextLayoutResult>, sz: Size,
) {
    for (a in rv.agents) drawCircle(Color.Black.copy(alpha = 0.22f), r, Offset(px(a.x) + r * 0.25f, py(a.y) + r * 0.3f))
    for (a in rv.agents) {
        val fill = if (a.gk) (if (a.side == 0) homeGk else awayGk) else if (a.side == 0) homeC else awayC
        val c = Offset(px(a.x), py(a.y))
        drawCircle(fill, r, c)
        drawCircle(Color.White.copy(alpha = 0.85f), r, c, style = Stroke(r * 0.12f))
        if (a.number > 0) {
            val key = "${a.number}-${contentOn(fill).hashCode()}-${r.toInt()}"
            val tl = numCache.getOrPut(key) {
                measurer.measure("${a.number}", TextStyle(color = contentOn(fill), fontSize = (r * 0.95f / density / fontScale).sp, fontWeight = FontWeight.Black))
            }
            drawText(tl, topLeft = Offset(c.x - tl.size.width / 2f, c.y - tl.size.height / 2f))
        }
    }
    val bx = px(rv.ballX)
    val by = py(rv.ballY)
    val br = 0.62f * sx * (1f + rv.ballZ * 0.06f)
    drawCircle(Color.Black.copy(alpha = 0.3f), 0.6f * sx, Offset(bx + rv.ballZ * sx * 0.25f, by + rv.ballZ * sx * 0.12f))
    val ball = Offset(bx, by - rv.ballZ * sx * 0.55f)
    drawCircle(Color.White, br, ball)
    drawCircle(Color(0xFF263238), br, ball, style = Stroke(br * 0.25f))
    // Sinema bandı ve hafif kararma
    val band = sz.height * 0.07f
    drawRect(Color.Black.copy(alpha = 0.18f), size = sz)
    drawRect(Color.Black.copy(alpha = 0.75f), size = Size(sz.width, band))
    drawRect(Color.Black.copy(alpha = 0.75f), Offset(0f, sz.height - band), Size(sz.width, band))
    val label = measurer.measure("⟲ TEKRAR · dokun: geç", TextStyle(color = Color(0xFFFFE14D), fontSize = (band * 0.42f / density / fontScale).sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp))
    drawText(label, topLeft = Offset(sz.width * 0.03f, (band - label.size.height) / 2f))
    // İlerleme çizgisi
    drawRect(Color(0xFFFFE14D), Offset(0f, sz.height - band * 0.2f), Size(sz.width * rv.progress, band * 0.12f))
}
