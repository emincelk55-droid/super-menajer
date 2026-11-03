package com.supermenajer.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/* ---------- Ana sayfa: hareketli gece stadyumu ---------- */

/** Bir döngünün süresi (ms); bütün hareketler bu döngüye bağlıdır. */
private const val LOOP_MS = 16000

/**
 * Ana sayfanın arkasındaki canlı stadyum: gece göğü, salınan projektör ışıkları, takım
 * renklerinde tribün, tribünde dolaşan "Meksika dalgası", patlayan fotoğraf flaşları,
 * yükselen ışık tozu ve perspektif saha. Kartların arkasında kalacak kadar soluktur.
 */
@Composable
fun StadiumBackdrop(c1: Color, c2: Color, modifier: Modifier = Modifier) {
    // Değer çizim sırasında okunur: her karede yalnız tuval yeniden çizilir (pil tasarrufunda sabit kare)
    val anim = if (fxLoopsOff()) null else rememberInfiniteTransition(label = "stadium").animateFloat(
        0f, 1f, infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)), label = "stadiumT",
    )
    val crowd = remember(c1, c2) { crowdDots(c1, c2) }
    val dust = remember { List(28) { Random(it * 13 + 5).let { r -> Triple(r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat()) } } }
    Canvas(modifier) { drawStadiumScene(anim?.value ?: 0.3f, c1, c2, crowd, dust) }
}

private class Fan(val u: Float, val row: Int, val color: Color, val flashPhase: Float, val flashes: Boolean)

private fun crowdDots(c1: Color, c2: Color): List<Fan> {
    val r = Random(42)
    val palette = listOf(c1, c1, c2, Color(0xFFDDE3F5), Color(0xFF3A4370))
    return buildList {
        for (row in 0 until 9) {
            val n = 70 + row * 6
            for (i in 0 until n) {
                add(Fan((i + r.nextFloat() * 0.6f) / n, row, palette[r.nextInt(palette.size)], r.nextFloat(), r.nextInt(14) == 0))
            }
        }
    }
}

private fun DrawScope.drawStadiumScene(t: Float, c1: Color, c2: Color, crowd: List<Fan>, dust: List<Triple<Float, Float, Float>>) {
    val w = size.width
    val h = size.height
    val tau = 2f * PI.toFloat()
    // Gece göğü
    drawRect(Brush.verticalGradient(listOf(Color(0xFF060A20), Color(0xFF14113F), Color(0xFF0A0E2A)), 0f, h))

    // Tribün halkası: üstü yay, altı sahanın çevresi
    val standTop = h * 0.30f
    val standBottom = h * 0.74f
    val bowl = Path().apply {
        moveTo(-w * 0.05f, standBottom + h * 0.1f)
        quadraticTo(w * 0.5f, standTop - h * 0.12f, w * 1.05f, standBottom + h * 0.1f)
        lineTo(w * 1.05f, h)
        lineTo(-w * 0.05f, h)
        close()
    }
    drawPath(bowl, Brush.verticalGradient(listOf(Color(0xFF1A2050), Color(0xFF0C1030)), standTop, h))
    // Çatı kenarı: takım renginde ince parlak şerit
    val rim = Path().apply {
        moveTo(-w * 0.05f, standBottom + h * 0.1f)
        quadraticTo(w * 0.5f, standTop - h * 0.12f, w * 1.05f, standBottom + h * 0.1f)
    }
    drawPath(rim, Brush.horizontalGradient(listOf(c1.copy(alpha = 0.1f), c1.copy(alpha = 0.55f), c2.copy(alpha = 0.4f), c1.copy(alpha = 0.1f))), style = Stroke(3f))

    // Seyirciler: yay boyunca sıra sıra; Meksika dalgası soldan sağa dolaşır
    val wave = t * 2f % 1f
    for (f in crowd) {
        val k = f.row / 9f
        // Yayın denklemi (ikinci derece Bezier): üst sıralar geniş yayda, alt sıralar sahaya yakın
        val y0 = standBottom + h * 0.1f - k * h * 0.02f
        val cy = standTop - h * 0.12f + k * h * 0.30f
        val u = f.u
        val x = (1 - u) * (1 - u) * (-w * 0.05f) + 2 * (1 - u) * u * (w * 0.5f) + u * u * (w * 1.05f)
        val yBase = (1 - u) * (1 - u) * y0 + 2 * (1 - u) * u * cy + u * u * y0
        val y = yBase + h * 0.018f + f.row * h * 0.021f
        if (y > h * 0.73f) continue
        val d = kotlin.math.abs(u - wave).let { minOf(it, 1f - it) }
        val lift = if (d < 0.05f) (1f - d / 0.05f) else 0f
        val r = (2f + k * 1.4f) * (w / 900f).coerceIn(0.8f, 2f)
        drawCircle(f.color.copy(alpha = 0.32f + lift * 0.45f), r, Offset(x, y - lift * r * 2.2f))
        if (f.flashes) {
            // Fotoğraf flaşı: döngü içinde kısa bir an patlar
            val ph = (t * 3f + f.flashPhase) % 1f
            if (ph < 0.025f) {
                val a = 1f - ph / 0.025f
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = a), Color.Transparent), Offset(x, y), r * 7f), r * 7f, Offset(x, y))
            }
        }
    }

    // Perspektif saha ve çim şeritleri
    val top = h * 0.74f
    val bottom = h * 1.04f
    val tl = Offset(w * 0.24f, top); val tr = Offset(w * 0.76f, top)
    val br = Offset(w * 1.02f, bottom); val bl = Offset(w * -0.02f, bottom)
    fun xAt(y: Float, left: Boolean): Float {
        val q = (y - top) / (bottom - top)
        return if (left) tl.x + (bl.x - tl.x) * q else tr.x + (br.x - tr.x) * q
    }
    for (s in 0 until 8) {
        val y1 = top + (bottom - top) * (s / 8f).let { it * it * 0.4f + it * 0.6f }
        val y2 = top + (bottom - top) * ((s + 1) / 8f).let { it * it * 0.4f + it * 0.6f }
        val stripe = Path().apply {
            moveTo(xAt(y1, true), y1); lineTo(xAt(y1, false), y1); lineTo(xAt(y2, false), y2); lineTo(xAt(y2, true), y2); close()
        }
        drawPath(stripe, if (s % 2 == 0) Color(0xFF1E7A45).copy(alpha = 0.55f) else Color(0xFF186A3B).copy(alpha = 0.55f))
    }
    val line = Color.White.copy(alpha = 0.22f)
    val pitch = Path().apply { moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }
    drawPath(pitch, line, style = Stroke(2f))
    val midY = top + (bottom - top) * 0.42f
    drawLine(line, Offset(xAt(midY, true), midY), Offset(xAt(midY, false), midY), 2f)
    drawOval(line, Offset(w * 0.44f, midY - h * 0.03f), Size(w * 0.12f, h * 0.06f), style = Stroke(2f))

    // Projektörler: kule, lamba paneli, salınan ışık konisi ve nabız gibi atan hale
    for ((i, x) in listOf(0.07f, 0.93f).withIndex()) {
        val lx = w * x
        val ly = h * 0.08f
        drawLine(Color(0xFF262C55), Offset(lx, ly), Offset(lx + (if (i == 0) w * 0.02f else -w * 0.02f), standTop + h * 0.15f), w * 0.006f)
        val sweep = sin((t + i * 0.5f) * tau) * w * 0.06f
        val target = Offset(w * (if (i == 0) 0.62f else 0.38f) + sweep, h * 0.86f)
        val beam = Path().apply {
            moveTo(lx - w * 0.015f, ly); lineTo(lx + w * 0.015f, ly)
            lineTo(target.x + w * 0.16f, target.y); lineTo(target.x - w * 0.16f, target.y); close()
        }
        drawPath(beam, Brush.linearGradient(listOf(Color(0xFFE8F4FF).copy(alpha = 0.16f), Color.Transparent), Offset(lx, ly), target))
        val pulse = 0.75f + 0.25f * sin((t * 4f + i * 0.3f) * tau)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFDFF3FF).copy(alpha = 0.55f * pulse), Color(0xFF46F0C8).copy(alpha = 0.12f), Color.Transparent), Offset(lx, ly), h * 0.35f), h * 0.35f, Offset(lx, ly))
        for (a in 0 until 3) for (b in 0 until 2) {
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.004f + 1f, Offset(lx + (a - 1) * w * 0.009f, ly + (b - 0.5f) * w * 0.009f))
        }
    }

    // Işık tozu: yavaşça yükselen parçacıklar
    for ((i, d) in dust.withIndex()) {
        val (dx, dy, sp) = d
        val y = ((dy - t * sp) % 1f + 1f) % 1f * h * 0.7f
        val x = dx * w + sin((t * 2f + i) * tau) * w * 0.01f
        drawCircle(Color.White.copy(alpha = 0.08f + (i % 4) * 0.03f), 1.5f + (i % 3), Offset(x, y))
    }
    // Kartların okunması için üstte ve altta hafif karartma
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.2f)), 0f, h))
}
