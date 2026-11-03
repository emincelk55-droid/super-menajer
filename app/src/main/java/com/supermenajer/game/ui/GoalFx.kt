package com.supermenajer.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------- Gol kutlaması: patlama halkası, dev GOL yazısı ve konfeti ---------- */

/** Kutlamanın toplam süresi (ms). */
const val GOAL_FX_MS = 2800

/**
 * Sahanın üstüne çizilen gol kutlaması. [confetti] kapalıyken (rakip golü) yalnız
 * sönük bir yazı ve halka görünür. Dokunmaları engellemez.
 */
@Composable
fun BoxScope.GoalCelebration(c1: Color, c2: Color, confetti: Boolean, seed: Int) {
    val still = fxStill()
    val p = remember(seed) { Animatable(if (still) 0.35f else 0f) }
    LaunchedEffect(seed) {
        if (!still) p.animateTo(1f, tween(GOAL_FX_MS, easing = LinearEasing))
    }
    GoalCelebrationFrame(p.value, c1, c2, confetti, seed)
}

/** Kutlamanın tek karesi; [progress] 0..1. Ekran görüntüsü testleri de bunu kullanır. */
@Composable
fun BoxScope.GoalCelebrationFrame(progress: Float, c1: Color, c2: Color, confetti: Boolean, seed: Int) {
    val parts = remember(seed) { confettiParticles(seed, listOf(c1, c2, Gold, Color.White, Teal)) }
    val t = progress * GOAL_FX_MS / 1000f
    Canvas(Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height
        // Ortadan genişleyen iki ışık halkası
        for (k in 0..1) {
            val q = ((progress - k * 0.08f) / 0.35f).coerceIn(0f, 1f)
            if (q in 0.001f..0.999f) {
                drawCircle(
                    (if (confetti) c1 else Color.White).copy(alpha = (1f - q) * 0.7f),
                    radius = q * maxOf(w, h) * 0.6f, center = Offset(w / 2, h / 2), style = Stroke(width = (1f - q) * 26f + 2f),
                )
            }
        }
        // İlk anda sahayı aydınlatan flaş
        val flash = (1f - progress / 0.12f).coerceIn(0f, 1f)
        if (flash > 0f) drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = flash * 0.55f), Color.Transparent), Offset(w / 2, h / 2), maxOf(w, h) * 0.6f))
        if (!confetti) return@Canvas
        val fade = ((1f - progress) / 0.2f).coerceIn(0f, 1f)
        for (c in parts) {
            val tt = t - c.delay
            if (tt <= 0f) continue
            // İki köşeden fırlatılan konfeti: atış + yerçekimi + rüzgârda salınma
            val x0 = if (c.left) 0f else w
            val vx = c.speed * cos(c.angle) * h * (if (c.left) 1f else -1f)
            val vy = -c.speed * sin(c.angle) * h
            val g = 1.25f * h
            val sway = if (tt > 0.6f) sin(tt * c.swayFreq + c.phase) * w * 0.018f else 0f
            val x = x0 + vx * tt * 0.8f + sway
            val y = h + vy * tt + 0.5f * g * tt * tt
            if (y > h + 40f || x < -40f || x > w + 40f) continue
            val sz = c.size * h / 260f
            rotate(c.rot0 + c.spin * tt, Offset(x, y)) {
                // Dönen kâğıt etkisi: genişlik kosinüsle daralıp açılır
                scale(scaleX = 1f, scaleY = cos(tt * c.flip).coerceIn(-1f, 1f).let { if (kotlin.math.abs(it) < 0.15f) 0.15f else it }, pivot = Offset(x, y)) {
                    val col = c.color.copy(alpha = fade)
                    if (c.round) drawCircle(col, sz * 0.45f, Offset(x, y))
                    else drawRect(col, Offset(x - sz / 2, y - sz / 4), Size(sz, sz / 2))
                }
            }
        }
    }
    GoalText(progress, c1, c2, confetti)
}

@Composable
private fun BoxScope.GoalText(progress: Float, c1: Color, c2: Color, ours: Boolean) {
    // 0..0.14 arası taşarak büyür, 0.8'den sonra söner
    val inQ = (progress / 0.14f).coerceIn(0f, 1f)
    val back = 1.70158f
    val ease = 1f + (back + 1f) * (inQ - 1f).let { it * it * it } + back * (inQ - 1f).let { it * it }
    val pulse = 1f + 0.04f * sin(progress * 20f)
    val sc = (0.3f + 0.7f * ease) * (if (inQ >= 1f) pulse else 1f)
    val alpha = ((1f - progress) / 0.2f).coerceIn(0f, 1f) * inQ.coerceAtLeast(0.15f)
    val top = lerp(c1, Color.White, 0.55f)
    Text(
        if (ours) "GOOOL!" else "GOL",
        modifier = Modifier.align(Alignment.Center).graphicsLayer { scaleX = sc; scaleY = sc; this.alpha = alpha; rotationZ = -6f * (1f - inQ) },
        style = TextStyle(
            brush = Brush.verticalGradient(if (ours) listOf(Color.White, Gold, top) else listOf(Color.White, Color(0xFFB8C0D8))),
            fontSize = if (ours) 64.sp else 44.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp,
            shadow = Shadow(if (ours) c2.copy(alpha = 0.9f) else Color.Black, Offset(0f, 6f), 18f),
        ),
    )
}

private class Confetto(
    val left: Boolean, val angle: Float, val speed: Float, val delay: Float, val color: Color, val size: Float,
    val rot0: Float, val spin: Float, val flip: Float, val swayFreq: Float, val phase: Float, val round: Boolean,
)

private fun confettiParticles(seed: Int, colors: List<Color>): List<Confetto> {
    val r = Random(seed)
    return List(110) { i ->
        Confetto(
            left = i % 2 == 0,
            angle = ((52 + r.nextInt(30)) * PI / 180).toFloat(),
            speed = 1.35f + r.nextFloat() * 0.75f,
            delay = r.nextFloat() * 0.35f,
            color = colors[r.nextInt(colors.size)],
            size = 7f + r.nextFloat() * 6f,
            rot0 = r.nextFloat() * 360f,
            spin = (r.nextFloat() - 0.5f) * 720f,
            flip = 4f + r.nextFloat() * 10f,
            swayFreq = 3f + r.nextFloat() * 4f,
            phase = r.nextFloat() * 6.28f,
            round = r.nextInt(5) == 0,
        )
    }
}
