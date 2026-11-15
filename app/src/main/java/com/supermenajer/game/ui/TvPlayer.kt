package com.supermenajer.game.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.supermenajer.game.engine.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/* ---------- 2,5D oyuncu figürü: eklemli uzuvlar, saç, boy farkı ve hareket pozları ---------- */

/** Oyuncunun kalıcı görünümü (kimliğinden türetilir: her maçta aynı). */
class Look(val skin: Color, val hair: Color, val hairStyle: Int, val height: Float) {
    companion object {
        private val skins = listOf(Color(0xFFF3CDA8), Color(0xFFE2B48A), Color(0xFFC68E62), Color(0xFF94603C), Color(0xFF6A4128), Color(0xFF4A2C1A))
        private val hairs = listOf(Color(0xFF1B1411), Color(0xFF3B2618), Color(0xFF6B4423), Color(0xFFC9A15A), Color(0xFF9C4A22))
        private val cache = HashMap<Long, Look>()

        fun of(id: Int, side: Int, gk: Boolean): Look = cache.getOrPut(id * 2L + side) {
            var h = (id * 2654435761L + side * 40503L) and 0xFFFFFFFL
            fun next(n: Int): Int { h = (h * 1103515245L + 12345L) and 0x7FFFFFFFL; return ((h ushr 8) % n).toInt() }
            val si = next(skins.size)
            // Koyu tende saç çoğunlukla siyah; açık tende sarı / kızıl da çıkabilir
            val hi = if (si >= 3) (if (next(5) == 0) 1 else 0) else next(hairs.size)
            val style = next(6)
            val height = (0.93f + next(13) * 0.01f) * (if (gk) 1.05f else 1f)
            Look(skins[si], hairs[hi], style, height)
        }
    }
}

/** Figürün o anki hareketi. */
class Motion(
    val face: Float, val run: Float, val phase: Float, val pose: Int, val poseT: Float, val poseDir: Float,
    val alpha: Float, val time: Float,
)

private fun ease(u: Float): Float { val x = u.coerceIn(0f, 1f); return x * x * (3f - 2f * x) }

/**
 * Oyuncuyu ayak noktası [foot] ve o derinlikte 1 metrenin piksel karşılığı [k] ile çizer.
 * Gövde yönü [Motion.face]: +1 sağa koşuyor/bakıyor. Döndürme ve yükselme pozlara göre uygulanır.
 * [body] forma rengi, [shorts] şort, [sock] tozluk; [shirt] forma deseni çizicisi (kırpılmış alana).
 */
fun DrawScope.drawTvPlayer(
    foot: Offset, k: Float, look: Look, m: Motion,
    body: Color, shorts: Color, sock: Color,
    shirt: (DrawScope.(left: Float, top: Float, w: Float, h: Float) -> Unit)? = null,
    number: (DrawScope.(cx: Float, cy: Float, h: Float) -> Unit)? = null,
) {
    val a = m.alpha
    val h = 1.85f * k * look.height
    val dir = m.face
    val pu = m.poseT
    // ---- Pozdan eklem açıları (radyan; 0 = aşağı, artı = ileri) ----
    var frontThigh: Float; var backThigh: Float; var frontBend: Float; var backBend: Float
    var frontArm: Float; var backArm: Float; var frontElbow = 0.35f; var backElbow = 0.35f
    var lean = 0f // derece, ileri artı
    var lift = 0f // metre
    var spin = 0f // derece, tüm figür (ayak etrafında)
    var drop = 0f // metre (kayarken yere yaklaşır)
    val run = m.run
    val ph = m.phase
    // Koşu döngüsü: bacaklar zıt, öndeki dizden bükülür; kollar bacaklara ters
    frontThigh = sin(ph) * 0.62f * run + 0.04f
    backThigh = -sin(ph) * 0.62f * run - 0.04f
    frontBend = max(0f, sin(ph + 1.7f)) * 1.25f * run + 0.06f
    backBend = max(0f, -sin(ph + 1.7f)) * 1.25f * run + 0.06f
    frontArm = -sin(ph) * 0.75f * run + 0.1f
    backArm = sin(ph) * 0.75f * run - 0.1f
    frontElbow = 0.25f + 0.9f * run; backElbow = 0.25f + 0.9f * run
    lean = 9f * run
    lift = abs(sin(ph)) * 0.06f * run

    when (m.pose) {
        Pose.KICK -> if (pu >= 0f) {
            val u = (pu / Pose.duration(Pose.KICK)).coerceIn(0f, 1f)
            // Geri al, sonra sertçe öne savur
            frontThigh = if (u < 0.35f) -0.7f * ease(u / 0.35f) else -0.7f + 2.1f * ease((u - 0.35f) / 0.65f)
            frontBend = if (u < 0.35f) 1.5f * ease(u / 0.35f) else 1.5f * (1f - ease((u - 0.35f) / 0.4f))
            backThigh = -0.12f; backBend = 0.15f
            frontArm = -1.1f; backArm = 1.2f; frontElbow = 0.3f; backElbow = 0.3f
            lean = -10f * sin(u * PI.toFloat())
            lift = 0f
        }
        Pose.HEADER -> if (pu >= 0f) {
            val u = (pu / Pose.duration(Pose.HEADER)).coerceIn(0f, 1f)
            lift = sin(u * PI.toFloat()) * 0.55f
            frontThigh = 0.5f; backThigh = -0.3f; frontBend = 1.4f; backBend = 1.2f
            frontArm = 2.2f; backArm = -2.0f; frontElbow = 0.6f; backElbow = 0.6f
            lean = if (u < 0.5f) -12f else 22f * ease((u - 0.5f) / 0.5f)
        }
        Pose.TACKLE -> if (pu >= 0f) {
            val u = (pu / Pose.duration(Pose.TACKLE)).coerceIn(0f, 1f)
            val g = ease(min(u * 3f, 1f)) * (1f - ease((u - 0.75f) / 0.25f))
            // Gövde geriye yatar, öndeki bacak yerde uzanır, arkadaki bükülür
            spin = -42f * g * dir
            drop = 0.12f * g
            frontThigh = 0.15f + 0.35f * g; frontBend = 0.05f
            backThigh = 0.1f; backBend = 1.5f * g
            frontArm = -0.5f; backArm = -1.6f * g; frontElbow = 0.2f; backElbow = 0.3f
            lean = 0f; lift = 0f
        }
        Pose.DIVE -> {
            if (pu < 0f) {
                // Hazır duruş: dizler bükük, kollar açık
                frontThigh = 0.35f; backThigh = 0.3f; frontBend = 0.7f; backBend = 0.7f
                frontArm = 0.7f; backArm = 0.6f; frontElbow = 0.4f; backElbow = 0.4f
                lean = 12f; lift = 0f; drop = 0.08f
            } else {
                val u = (pu / Pose.duration(Pose.DIVE)).coerceIn(0f, 1f)
                val fly = ease(min(u * 3.2f, 1f))
                spin = 82f * fly * m.poseDir * dir
                lift = sin(min(u * 2.2f, 1f) * PI.toFloat()) * 0.7f
                frontThigh = -0.1f; backThigh = 0.15f; frontBend = 0.2f; backBend = 0.4f
                frontArm = 3.0f * fly; backArm = 2.7f * fly; frontElbow = 0.1f; backElbow = 0.2f
                lean = 0f
            }
        }
        Pose.CARD -> {
            frontThigh = 0.05f; backThigh = -0.05f; frontBend = 0.05f; backBend = 0.05f
            frontArm = 3.1f; frontElbow = 0f; backArm = 0.15f; backElbow = 0.2f
            lean = -3f; lift = 0f
        }
        Pose.CELEB, Pose.SLIDE -> {
            val sliding = m.pose == Pose.SLIDE && pu < 1.4f
            if (sliding) {
                val u = (pu / 1.4f).coerceIn(0f, 1f)
                // Dizüstü kayış: diz yerde, kollar havada, gövde geriye
                frontThigh = 1.45f; frontBend = 2.7f; backThigh = 1.1f; backBend = 2.6f
                drop = 0.42f * ease(min(u * 4f, 1f))
                frontArm = 2.75f; backArm = -2.75f + 5.5f; frontElbow = 0.15f; backElbow = 0.15f
                lean = -18f
                lift = 0f
            } else if (pu >= 0f) {
                // Kollar havada zıplama
                val b = abs(sin(m.time * 7.5f + ph))
                lift = b * 0.28f
                frontThigh = 0.1f + 0.3f * b; backThigh = -0.1f; frontBend = 0.6f * b; backBend = 0.9f * b
                frontArm = 2.7f + sin(m.time * 9f) * 0.25f; backArm = 2.5f - sin(m.time * 9f) * 0.25f
                frontElbow = 0.25f; backElbow = 0.25f
                lean = 0f
            }
        }
    }

    val px = foot.x
    val py = foot.y - (lift - drop) * k
    // Uzuvlar
    val thigh = h * 0.255f
    val shin = h * 0.245f
    val hipY = py - (thigh + shin) * 0.97f
    val shoulderY = py - h * 0.80f
    val bw = h * 0.27f
    val limbW = bw * 0.30f
    val armW = bw * 0.24f
    val shade = Color.Black.copy(alpha = 0.18f * a)

    fun leg(thetaHip: Float, bend: Float, near: Boolean) {
        val hx = px + (if (near) 1f else -1f) * bw * 0.12f
        val knee = Offset(hx + sin(thetaHip) * thigh * dir, hipY + cos(thetaHip) * thigh)
        val sh = thetaHip - bend
        val ankle = Offset(knee.x + sin(sh) * shin * dir, knee.y + cos(sh) * shin)
        val skinC = (if (near) look.skin else lerp(look.skin, Color.Black, 0.25f)).copy(alpha = a)
        val sockC = (if (near) sock else lerp(sock, Color.Black, 0.25f)).copy(alpha = a)
        // Uyluk (şort altı ten), baldır tozluk, krampon
        drawLine(skinC, Offset(hx, hipY), knee, limbW, StrokeCap.Round)
        drawLine(sockC, Offset(knee.x + (ankle.x - knee.x) * 0.12f, knee.y + (ankle.y - knee.y) * 0.12f), ankle, limbW * 0.92f, StrokeCap.Round)
        val toe = Offset(ankle.x + cos(sh) * limbW * 1.1f * dir, ankle.y - sin(sh) * limbW * 1.1f)
        drawLine(Color(0xFF15171C).copy(alpha = a), ankle, toe, limbW * 0.8f, StrokeCap.Round)
    }

    fun arm(theta: Float, elbow: Float, near: Boolean) {
        val sx = px + (if (near) 1f else -1f) * bw * 0.38f
        val sy = shoulderY + h * 0.035f
        val up = h * 0.165f
        val fore = h * 0.155f
        val el = Offset(sx + sin(theta) * up * dir, sy + cos(theta) * up)
        val ft = theta + elbow
        val hand = Offset(el.x + sin(ft) * fore * dir, el.y + cos(ft) * fore)
        val sleeve = (if (near) body else lerp(body, Color.Black, 0.25f)).copy(alpha = a)
        val skinC = (if (near) look.skin else lerp(look.skin, Color.Black, 0.25f)).copy(alpha = a)
        drawLine(sleeve, Offset(sx, sy), Offset(sx + (el.x - sx) * 0.6f, sy + (el.y - sy) * 0.6f), armW * 1.15f, StrokeCap.Round)
        drawLine(skinC, Offset(sx + (el.x - sx) * 0.55f, sy + (el.y - sy) * 0.55f), el, armW, StrokeCap.Round)
        drawLine(skinC, el, hand, armW * 0.92f, StrokeCap.Round)
    }

    rotate(spin, Offset(px, py)) {
        // Arka kol ve arka bacak önce (gövdenin arkasında kalır)
        rotate(lean * dir, Offset(px, hipY)) { arm(backArm, backElbow, near = false) }
        leg(backThigh, backBend, near = false)
        leg(frontThigh, frontBend, near = true)
        // Şort
        drawRoundRect(shorts.copy(alpha = a), Offset(px - bw * 0.5f, hipY - h * 0.075f), Size(bw, h * 0.15f), CornerRadius(bw * 0.18f))
        rotate(lean * dir, Offset(px, hipY)) {
            // Forma: omuzda geniş, belde dar
            val top = shoulderY
            val torso = Path().apply {
                addRoundRect(RoundRect(px - bw * 0.5f, top, px + bw * 0.5f, hipY + h * 0.01f, CornerRadius(bw * 0.32f)))
            }
            drawPath(torso, body.copy(alpha = a))
            if (shirt != null && a > 0.3f) clipPath(torso) { shirt(px - bw * 0.5f, top, bw, hipY - top) }
            // Kıvrım gölgesi ve sol üstten ışık
            clipPath(torso) {
                drawRect(shade, Offset(px + bw * 0.12f * dir, top), Size(bw * 0.5f, hipY - top))
                drawRect(Color.White.copy(alpha = 0.16f * a), Offset(px - bw * 0.5f, top), Size(bw * 0.3f, (hipY - top) * 0.6f))
            }
            number?.invoke(this, px, top + (hipY - top) * 0.48f, h)
            // Boyun ve baş
            val headR = h * 0.068f
            val hc = Offset(px + dir * h * 0.012f, shoulderY - headR * 1.15f)
            drawLine(look.skin.copy(alpha = a), Offset(px, shoulderY + h * 0.01f), Offset(hc.x, hc.y + headR * 0.6f), headR * 0.75f, StrokeCap.Round)
            drawHair(look, hc, headR, dir, a, back = true)
            drawCircle(look.skin.copy(alpha = a), headR, hc)
            drawCircle(Color.Black.copy(alpha = 0.12f * a), headR, hc + Offset(dir * headR * 0.35f, headR * 0.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(headR * 0.25f))
            drawHair(look, hc, headR, dir, a, back = false)
            // Ön kol en üstte
            arm(frontArm, frontElbow, near = true)
        }
    }
}

/** Saç: kısa, kazıtılmış, uzun (at kuyruğu), kıvırcık hacimli, kel, tepesi uzun. */
private fun DrawScope.drawHair(look: Look, c: Offset, r: Float, dir: Float, a: Float, back: Boolean) {
    val col = look.hair.copy(alpha = a)
    when (look.hairStyle) {
        0 -> if (!back) drawArc(col, 180f, 180f, true, Offset(c.x - r * 1.04f, c.y - r * 1.12f), Size(r * 2.08f, r * 1.7f))
        1 -> if (!back) drawArc(col.copy(alpha = a * 0.7f), 190f, 160f, true, Offset(c.x - r, c.y - r * 1.02f), Size(r * 2f, r * 1.5f))
        2 -> if (back) {
            drawArc(col, 180f, 180f, true, Offset(c.x - r * 1.08f, c.y - r * 1.15f), Size(r * 2.16f, r * 1.8f))
            drawLine(col, Offset(c.x - dir * r * 0.7f, c.y - r * 0.2f), Offset(c.x - dir * r * 1.25f, c.y + r * 1.1f), r * 0.55f, StrokeCap.Round)
        } else drawArc(col, 185f, 170f, true, Offset(c.x - r * 1.05f, c.y - r * 1.12f), Size(r * 2.1f, r * 1.6f))
        3 -> if (back) drawCircle(col, r * 1.38f, c + Offset(-dir * r * 0.12f, -r * 0.3f))
        4 -> {}
        else -> if (!back) {
            drawArc(col, 180f, 180f, true, Offset(c.x - r * 1.02f, c.y - r * 1.05f), Size(r * 2.04f, r * 1.4f))
            drawOval(col, Offset(c.x - r * 0.45f, c.y - r * 1.55f), Size(r * 0.9f, r * 0.8f))
        }
    }
}

/** Hız ve kimlikten koşu fazı (her oyuncu farklı adımda). */
fun runPhase(timeSec: Float, id: Int, speed: Float): Float = timeSec * (7.5f + min(speed, 8f) * 0.6f) + id * 1.7f
