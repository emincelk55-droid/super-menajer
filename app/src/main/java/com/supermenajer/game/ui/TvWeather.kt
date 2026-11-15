package com.supermenajer.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.supermenajer.game.engine.Conditions
import com.supermenajer.game.engine.MatchConditions
import kotlin.math.sin

/* ---------- 2,5D maç: hava durumu ve ışık (yağmur, kar, rüzgâr, sıcak, bulut, akşam projektörleri) ---------- */

/** Görünümün kullandığı hava/ışık ayarı. */
class TvSky(val weather: Int, val night: Boolean, val heavy: Boolean) {
    val rain get() = weather == Conditions.RAIN
    val snow get() = weather == Conditions.SNOW
    val wind get() = weather == Conditions.WIND
    val heat get() = weather == Conditions.HEAT
    val cloudy get() = weather == Conditions.CLOUDY

    /** Oyuncu gölgesi: güneşte belirgin, bulutta/yağmurda soluk; akşam dört projektörden dört gölge. */
    val shadowAlpha: Float get() = when {
        night -> 0.13f
        rain || snow || cloudy -> 0.16f
        heat -> 0.36f
        else -> 0.28f
    }

    companion object {
        val CLEAR = TvSky(Conditions.CLEAR, night = false, heavy = false)
        fun of(c: MatchConditions?): TvSky = if (c == null) CLEAR else TvSky(c.weather, c.night, c.heavyPitch)
    }
}

/** Gökyüzü ve tribün arkası: akşam koyu lacivert, gündüz havaya göre. */
fun skyBrush(s: TvSky, h: Float): Brush = when {
    s.night -> Brush.verticalGradient(listOf(Color(0xFF03050E), Color(0xFF0B1230)), 0f, h * 0.3f)
    s.rain || s.snow -> Brush.verticalGradient(listOf(Color(0xFF4A5363), Color(0xFF2E3442)), 0f, h * 0.3f)
    s.cloudy -> Brush.verticalGradient(listOf(Color(0xFF6E7A8C), Color(0xFF3A4252)), 0f, h * 0.3f)
    else -> Brush.verticalGradient(listOf(Color(0xFF5DA9E9), Color(0xFF9CC9EE)), 0f, h * 0.3f)
}

/** Çim rengi: kar beyazlatır, yağmur koyulaştırır, sıcak sarartır. */
fun grassTint(base: Color, s: TvSky): Color = when {
    s.snow -> androidx.compose.ui.graphics.lerp(base, Color(0xFFE9EEF2), 0.32f)
    s.rain -> androidx.compose.ui.graphics.lerp(base, Color(0xFF14502A), 0.3f)
    s.heat -> androidx.compose.ui.graphics.lerp(base, Color(0xFF9DB04A), 0.22f)
    s.cloudy -> androidx.compose.ui.graphics.lerp(base, Color(0xFF2B5E3A), 0.15f)
    else -> base
}

/** Zemin üstü efekt (oyunculardan önce): ıslak parlama, ağır zeminde çamurlu orta, akşam ışık havuzları. */
fun DrawScope.drawGroundFx(s: TvSky, groundTop: Float) {
    val gh = size.height - groundTop
    if (gh <= 0f) return
    if (s.rain) {
        // Islak çim: kameraya yakın kısımda gökyüzünü yansıtan parlaklık
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent), groundTop, size.height), Offset(0f, groundTop), Size(size.width, gh))
    }
    if (s.heavy) {
        // Ağır zemin: ceza sahaları ve orta çevresi yıpranmış, kahverengimsi lekeler
        drawRect(Brush.radialGradient(listOf(Color(0xFF6B5A33).copy(alpha = 0.28f), Color.Transparent), Offset(size.width / 2, groundTop + gh * 0.55f), size.width * 0.35f), Offset(0f, groundTop), Size(size.width, gh))
    }
    if (s.night) {
        // Projektörlerin aydınlattığı alan: ortası parlak, kenarlar kararır
        drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), Offset(size.width / 2, groundTop + gh * 0.45f), size.width * 0.6f), Offset(0f, groundTop), Size(size.width, gh))
    }
}

/** Akşam: tribün çatısında projektör sıraları ve ışık hüzmesi. [roofY] çatının ekrandaki yüksekliği. */
fun DrawScope.drawFloodlights(s: TvSky, roofY: Float, camX: Float, t: Float) {
    if (!s.night) return
    val w = size.width
    // Sahne genelinde soğuk ışık parıltısı
    drawRect(Brush.verticalGradient(listOf(Color(0xFFBFD6FF).copy(alpha = 0.10f), Color.Transparent), roofY - 30f, roofY + size.height * 0.25f), Offset(0f, roofY - 30f), Size(w, size.height * 0.25f + 30f))
    // Kamera kaydıkça ışık kümeleri de kayar (paralaks)
    val spacing = w * 0.42f
    val shift = -((camX * 9f) % spacing)
    var x = shift - spacing
    val d = density
    // Seyircilerin fotoğraf flaşları
    for (i in 0 until 10) {
        val on = ((t * 3f + hash(i, 9) * 50f).toInt() + i) % 9 == 0
        if (on) drawCircle(Color.White.copy(alpha = 0.85f), 1.6f * d, Offset(hash(i, 7) * w, (roofY + 6f * d) + hash(i, 8) * 22f * d))
    }
    while (x < w + spacing) {
        val cx = x
        val cy = roofY.coerceAtLeast(7f * d)
        val gr = 46f * d
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Color(0xFFCFE0FF).copy(alpha = 0.2f), Color.Transparent), Offset(cx, cy), gr), gr, Offset(cx, cy))
        // Lamba ızgarası
        for (r in 0 until 2) for (c in 0 until 6) {
            drawCircle(Color.White, 1.5f * d, Offset(cx - 11f * d + c * 4.4f * d, cy - 2.5f * d + r * 4f * d))
        }
        x += spacing
    }
}

/** Gökten düşen / esen parçacıklar (oyuncuların üstüne çizilir). */
fun DrawScope.drawWeatherParticles(s: TvSky, t: Float, still: Boolean) {
    val w = size.width
    val h = size.height
    val time = if (still) 1.3f else t
    when {
        s.rain -> {
            val n = 170
            val d = density
            val col = Color(0xFFDDE8F5).copy(alpha = 0.5f)
            for (i in 0 until n) {
                val sx = hash(i, 1) * (w + 200f) - 100f
                val sp = (420f + hash(i, 2) * 220f) * density
                val y = ((hash(i, 3) * h + time * sp) % (h + 60f)) - 30f
                val x = sx - y * 0.18f
                val len = (9f + hash(i, 4) * 8f) * d
                drawLine(col, Offset(x, y), Offset(x - len * 0.18f, y + len), 0.9f * d, StrokeCap.Round)
            }
            // Hafif sis: uzak taraf soluklaşır
            drawRect(Brush.verticalGradient(listOf(Color(0xFFB0BCCB).copy(alpha = 0.16f), Color.Transparent), 0f, h * 0.6f), size = Size(w, h * 0.6f))
        }
        s.snow -> {
            val pts = ArrayList<Offset>(160)
            val big = ArrayList<Offset>(60)
            for (i in 0 until 200) {
                val sp = 40f + hash(i, 2) * 50f
                val y = ((hash(i, 3) * h + time * sp) % (h + 20f)) - 10f
                val x = (hash(i, 1) * w + sin(time * 1.2f + i) * 14f + w) % w
                if (i % 4 == 0) big += Offset(x, y) else pts += Offset(x, y)
            }
            drawPoints(pts, PointMode.Points, Color.White.copy(alpha = 0.8f), strokeWidth = 2f * density, cap = StrokeCap.Round)
            drawPoints(big, PointMode.Points, Color.White.copy(alpha = 0.9f), strokeWidth = 3.4f * density, cap = StrokeCap.Round)
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), 0f, h * 0.55f), size = Size(w, h * 0.55f))
        }
        s.wind -> {
            val col = Color.White.copy(alpha = 0.16f)
            for (i in 0 until 26) {
                val sp = 260f + hash(i, 2) * 180f
                val x = ((hash(i, 1) * w + time * sp) % (w + 300f)) - 150f
                val y = hash(i, 3) * h
                val len = 60f + hash(i, 4) * 80f
                drawLine(col, Offset(x, y), Offset(x + len * density * 0.6f, y - len * 0.04f), 1.1f * density, StrokeCap.Round)
            }
        }
    }
    if (s.heat && !s.night) {
        // Güneş parlaması ve sıcak ton
        drawRect(Color(0xFFFFB74D).copy(alpha = 0.07f), size = size)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.55f), Color.Transparent), Offset(w * 0.12f, -h * 0.05f), h * 0.6f), h * 0.6f, Offset(w * 0.12f, -h * 0.05f))
    } else if (s.cloudy || s.rain || s.snow) {
        drawRect(Color(0xFF3A4556).copy(alpha = if (s.cloudy) 0.08f else 0.12f), size = size)
    }
    if (s.night) {
        // Akşam: kenarlar kararır, ortası projektörle aydınlık
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF020615).copy(alpha = 0.42f)), Offset(w / 2, h * 0.55f), w * 0.72f), size = size)
        drawRect(Color(0xFF0A1440).copy(alpha = 0.10f), size = size)
    }
}

private fun hash(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
}
