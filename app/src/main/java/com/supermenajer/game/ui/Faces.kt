package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.model.Player
import kotlin.random.Random

/**
 * Prosedürel oyuncu yüzü: oyuncunun kimliği ve uyruğundan her seferinde aynı yüz çizilir
 * (ten rengi, saç modeli ve rengi, sakal, kaşlar, gözler). Gerçek kişilerin fotoğrafı kullanılmaz.
 */
class FaceSpec(
    val skin: Color,
    val hair: Color,
    val hairStyle: Int,
    val beard: Int,
    val eyeColor: Color,
    val brow: Int,
    val faceW: Float,
    val noseW: Float,
    val mouth: Int,
)

private val skinLight = listOf(Color(0xFFF6D3B8), Color(0xFFF1C27D), Color(0xFFEFC3A4), Color(0xFFE8B996))
private val skinOlive = listOf(Color(0xFFD9A57A), Color(0xFFC68A5E), Color(0xFFDDA77F), Color(0xFFCF9B70))
private val skinBrown = listOf(Color(0xFFA86B45), Color(0xFF8D5524), Color(0xFF9C6644))
private val skinDark = listOf(Color(0xFF6B4226), Color(0xFF5A3A22), Color(0xFF4A2F1B), Color(0xFF7A4B2A))
private val skinEast = listOf(Color(0xFFF3D2B3), Color(0xFFEAC39E), Color(0xFFE5BC92))

private val hairBlack = Color(0xFF1B1B1B)
private val hairDark = Color(0xFF3B2A20)
private val hairBrown = Color(0xFF6B4423)
private val hairBlond = Color(0xFFD9B26B)
private val hairRed = Color(0xFFA0522D)
private val hairGrey = Color(0xFF9E9E9E)

private val africa = setOf("SN", "NG", "CI", "GH", "CM")
private val northAfrica = setOf("MA", "DZ")
private val med = setOf("TR", "AL", "GR", "BA", "IT", "ES", "PT", "RS", "HR")
private val nordic = setOf("DK", "SE", "NO", "NL", "DE", "PL", "CZ", "AT", "CH", "BE", "UA", "GB", "FR")
private val latin = setOf("BR", "CO", "US", "AR", "UY")
private val east = setOf("JP", "KR")

fun faceSpec(p: Player): FaceSpec {
    val r = Random(p.id * 7919L + p.nation.hashCode())
    fun <T> pick(l: List<T>) = l[r.nextInt(l.size)]
    val n = p.nation
    // Karma ülkelerde (Fransa, İngiltere, Brezilya...) ten rengi çeşitlidir
    val mixedRoll = r.nextDouble()
    val skin = when {
        n in africa -> pick(skinDark + skinDark + skinBrown)
        n in northAfrica -> pick(skinOlive + skinBrown)
        n in east -> pick(skinEast)
        n in latin -> pick(if (mixedRoll < 0.35) skinBrown + skinDark else skinOlive + skinLight)
        n == "FR" || n == "GB" || n == "BE" || n == "NL" || n == "DE" ->
            pick(if (mixedRoll < 0.22) skinDark + skinBrown else if (mixedRoll < 0.35) skinOlive else skinLight)
        n in med -> pick(skinOlive + skinLight)
        else -> pick(skinLight)
    }
    val dark = skin in skinDark || skin in skinBrown
    var hair = when {
        dark || n in east -> hairBlack
        n in nordic && !dark -> pick(listOf(hairBlond, hairBrown, hairBrown, hairDark, hairBlond, hairRed))
        n in med || n in northAfrica || n in latin -> pick(listOf(hairBlack, hairDark, hairDark, hairBrown))
        else -> pick(listOf(hairDark, hairBrown, hairBlack))
    }
    if (p.age >= 32 && r.nextDouble() < 0.35) hair = hairGrey
    val style = when {
        dark -> pick(listOf(0, 1, 1, 4, 4, 6, 2))
        else -> pick(listOf(1, 2, 2, 3, 3, 5, 6, 0))
    }
    val beard = if (p.age < 20) 0 else pick(listOf(0, 0, 0, 1, 1, 2, 3))
    val eye = when {
        hair == hairBlond && r.nextDouble() < 0.6 -> pick(listOf(Color(0xFF4A7BB7), Color(0xFF5B8C5A)))
        else -> pick(listOf(Color(0xFF3E2A1E), Color(0xFF2B1B12), Color(0xFF4E3B2A)))
    }
    return FaceSpec(skin, hair, style, beard, eye, r.nextInt(3), 0.78f + r.nextFloat() * 0.1f, 0.10f + r.nextFloat() * 0.05f, r.nextInt(3))
}

private fun Color.darker(f: Float = 0.82f) = Color(red * f, green * f, blue * f, alpha)

/** Yüzü bir tuvale çizer. Arka plan ve forma yakası takım renginde. */
fun DrawScope.drawFace(f: FaceSpec, shirt: Color, collar: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2
    // Omuzlar ve forma
    val body = Path().apply {
        moveTo(w * 0.0f, h)
        cubicTo(w * 0.04f, h * 0.84f, w * 0.26f, h * 0.80f, cx, h * 0.80f)
        cubicTo(w * 0.74f, h * 0.80f, w * 0.96f, h * 0.84f, w * 1.0f, h)
        close()
    }
    drawPath(body, shirt)
    // Boyun
    drawRect(f.skin.darker(0.9f), Offset(cx - w * 0.12f, h * 0.64f), Size(w * 0.24f, h * 0.19f))
    // Yaka
    val v = Path().apply {
        moveTo(cx - w * 0.14f, h * 0.805f); lineTo(cx, h * 0.9f); lineTo(cx + w * 0.14f, h * 0.805f)
    }
    drawPath(v, collar, style = Stroke(w * 0.035f))
    val fw = w * 0.6f * f.faceW
    val fh = h * 0.62f
    val top = h * 0.11f
    // Arka saç (uzun saç)
    if (f.hairStyle == 5) drawRoundRect(f.hair, Offset(cx - fw * 0.62f, top + fh * 0.05f), Size(fw * 1.24f, fh * 0.85f), CornerRadius(fw * 0.4f))
    // Kulaklar
    drawOval(f.skin.darker(0.92f), Offset(cx - fw * 0.58f, top + fh * 0.38f), Size(fw * 0.2f, fh * 0.22f))
    drawOval(f.skin.darker(0.92f), Offset(cx + fw * 0.38f, top + fh * 0.38f), Size(fw * 0.2f, fh * 0.22f))
    // Yüz
    drawOval(f.skin, Offset(cx - fw / 2, top), Size(fw, fh))
    // Sakal
    when (f.beard) {
        1 -> drawOval(f.hair.copy(alpha = 0.28f), Offset(cx - fw * 0.42f, top + fh * 0.55f), Size(fw * 0.84f, fh * 0.45f))
        2 -> {
            val beard = Path().apply {
                moveTo(cx - fw * 0.47f, top + fh * 0.5f)
                quadraticBezierTo(cx - fw * 0.42f, top + fh * 1.04f, cx, top + fh * 1.02f)
                quadraticBezierTo(cx + fw * 0.42f, top + fh * 1.04f, cx + fw * 0.47f, top + fh * 0.5f)
                lineTo(cx + fw * 0.30f, top + fh * 0.72f)
                quadraticBezierTo(cx, top + fh * 0.66f, cx - fw * 0.30f, top + fh * 0.72f)
                close()
            }
            drawPath(beard, f.hair.copy(alpha = 0.9f))
        }
        3 -> drawLine(f.hair, Offset(cx - fw * 0.17f, top + fh * 0.71f), Offset(cx + fw * 0.17f, top + fh * 0.71f), fh * 0.05f, StrokeCap.Round)
    }
    // Saç
    when (f.hairStyle) {
        0 -> drawArc(f.skin.darker(0.94f), 200f, 140f, false, Offset(cx - fw / 2, top), Size(fw, fh * 0.6f), style = Stroke(1f))
        1 -> drawArc(f.hair.copy(alpha = 0.75f), 180f, 180f, true, Offset(cx - fw * 0.5f, top - fh * 0.02f), Size(fw, fh * 0.42f))
        2 -> {
            drawArc(f.hair, 180f, 180f, true, Offset(cx - fw * 0.53f, top - fh * 0.06f), Size(fw * 1.06f, fh * 0.48f))
            drawRect(f.hair, Offset(cx - fw * 0.5f, top + fh * 0.12f), Size(fw * 0.1f, fh * 0.12f))
            drawRect(f.hair, Offset(cx + fw * 0.4f, top + fh * 0.12f), Size(fw * 0.1f, fh * 0.12f))
        }
        3 -> {
            drawArc(f.hair, 180f, 180f, true, Offset(cx - fw * 0.54f, top - fh * 0.1f), Size(fw * 1.08f, fh * 0.55f))
            val part = Path().apply {
                moveTo(cx - fw * 0.54f, top + fh * 0.18f)
                quadraticBezierTo(cx - fw * 0.1f, top + fh * 0.02f, cx + fw * 0.54f, top + fh * 0.2f)
                lineTo(cx + fw * 0.54f, top + fh * 0.05f); lineTo(cx - fw * 0.54f, top + fh * 0.05f); close()
            }
            drawPath(part, f.hair)
        }
        4 -> {
            // Kıvırcık / afro
            for (i in 0 until 9) {
                val a = Math.toRadians(180.0 + i * 22.5)
                val rx = cx + (fw * 0.5f) * kotlin.math.cos(a).toFloat()
                val ry = top + fh * 0.2f + (fh * 0.26f) * kotlin.math.sin(a).toFloat()
                drawCircle(f.hair, fw * 0.16f, Offset(rx, ry))
            }
            drawArc(f.hair, 180f, 180f, true, Offset(cx - fw * 0.52f, top - fh * 0.12f), Size(fw * 1.04f, fh * 0.6f))
        }
        5 -> drawArc(f.hair, 180f, 180f, true, Offset(cx - fw * 0.56f, top - fh * 0.08f), Size(fw * 1.12f, fh * 0.55f))
        else -> {
            // Yanlar kısa, üstü uzun
            drawArc(f.hair.copy(alpha = 0.5f), 180f, 180f, true, Offset(cx - fw * 0.5f, top), Size(fw, fh * 0.4f))
            drawRoundRect(f.hair, Offset(cx - fw * 0.28f, top - fh * 0.1f), Size(fw * 0.56f, fh * 0.24f), CornerRadius(fw * 0.12f))
        }
    }
    // Kaşlar
    val browY = top + fh * 0.36f
    val bt = fh * (0.035f + f.brow * 0.01f)
    drawLine(f.hair.darker(0.7f), Offset(cx - fw * 0.32f, browY), Offset(cx - fw * 0.1f, browY - fh * 0.015f), bt, StrokeCap.Round)
    drawLine(f.hair.darker(0.7f), Offset(cx + fw * 0.1f, browY - fh * 0.015f), Offset(cx + fw * 0.32f, browY), bt, StrokeCap.Round)
    // Gözler
    val eyeY = top + fh * 0.45f
    for (sx in listOf(-1f, 1f)) {
        val ex = cx + sx * fw * 0.2f
        drawOval(Color.White, Offset(ex - fw * 0.09f, eyeY - fh * 0.035f), Size(fw * 0.18f, fh * 0.07f))
        drawCircle(f.eyeColor, fh * 0.03f, Offset(ex, eyeY))
        drawCircle(Color.Black, fh * 0.013f, Offset(ex, eyeY))
    }
    // Burun
    val nose = Path().apply {
        moveTo(cx, eyeY + fh * 0.02f)
        lineTo(cx - fw * f.noseW, top + fh * 0.66f)
        lineTo(cx + fw * f.noseW * 0.6f, top + fh * 0.66f)
    }
    drawPath(nose, f.skin.darker(0.78f), style = Stroke(fh * 0.02f, cap = StrokeCap.Round))
    // Ağız
    val my = top + fh * 0.79f
    when (f.mouth) {
        0 -> drawArc(Color(0xFF8D3B3B), 20f, 140f, false, Offset(cx - fw * 0.15f, my - fh * 0.06f), Size(fw * 0.3f, fh * 0.1f), style = Stroke(fh * 0.025f, cap = StrokeCap.Round))
        1 -> drawLine(Color(0xFF8D3B3B), Offset(cx - fw * 0.13f, my), Offset(cx + fw * 0.13f, my), fh * 0.025f, StrokeCap.Round)
        else -> drawArc(Color(0xFF8D3B3B), 10f, 160f, true, Offset(cx - fw * 0.14f, my - fh * 0.05f), Size(fw * 0.28f, fh * 0.1f))
    }
}

/** Yuvarlak oyuncu yüzü (liste ve küçük gösterimler için). */
@Composable
fun PlayerFace(p: Player, size: Dp, shirt: Color = Color(0xFF2E7D32), collar: Color = Color.White) {
    val spec = remember(p.id, p.age >= 32) { faceSpec(p) }
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3A4654), Color(0xFF1F2933)))),
    ) {
        Canvas(Modifier.size(size)) { drawFace(spec, shirt, collar) }
    }
}

/** Güç seviyesine göre kart rengi (bronz, gümüş, altın, efsane). */
fun cardColors(ovr: Int): List<Color> = when {
    ovr >= 85 -> listOf(Color(0xFF1B1B2F), Color(0xFF5B2C83), Color(0xFFD4AF37))
    ovr >= 75 -> listOf(Color(0xFFF7E08A), Color(0xFFD4AF37), Color(0xFF9C7A1C))
    ovr >= 65 -> listOf(Color(0xFFE8ECEF), Color(0xFFB8C1C8), Color(0xFF7D8890))
    else -> listOf(Color(0xFFE2B98B), Color(0xFFB57F4E), Color(0xFF7A5232))
}

/** FUT tarzı oyuncu kartı: güç, mevki, yüz ve isim. */
@Composable
fun PlayerCard(
    p: Player,
    ovr: Int,
    posLabel: String,
    width: Dp,
    shirt: Color,
    collar: Color,
    modifier: Modifier = Modifier,
    highlight: Color = Color.Transparent,
    known: Boolean = true,
) {
    val cols = cardColors(ovr)
    val dark = ovr >= 85
    val txt = if (dark) Color(0xFFF5E6A8) else Color(0xFF2A2116)
    val h = width * 1.45f
    Column(
        modifier.width(width).height(h).clip(RoundedCornerShape(width * 0.14f))
            .background(Brush.verticalGradient(cols))
            .border(2.dp, if (highlight == Color.Transparent) cols.last() else highlight, RoundedCornerShape(width * 0.14f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = width * 0.07f, top = width * 0.05f, end = width * 0.04f)) {
            Column {
                Text(if (known) "$ovr" else "?", color = txt, fontWeight = FontWeight.Black, fontSize = (width.value * 0.22f).sp, lineHeight = (width.value * 0.24f).sp)
                Text(posLabel, color = txt, fontWeight = FontWeight.Bold, fontSize = (width.value * 0.12f).sp, lineHeight = (width.value * 0.13f).sp)
            }
        }
        val spec = remember(p.id, p.age >= 32) { faceSpec(p) }
        Box(Modifier.size(width * 0.7f)) {
            Canvas(Modifier.size(width * 0.7f)) { drawFace(spec, shirt, collar) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = width * 0.1f).background(txt.copy(alpha = 0.4f)))
        Text(
            p.name.substringAfterLast(' '), color = txt, fontWeight = FontWeight.Bold,
            fontSize = (width.value * 0.13f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
        )
    }
}
