package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.supermenajer.game.engine.Pitch2D
import com.supermenajer.game.engine.Pitch2D.Companion.L
import com.supermenajer.game.engine.Pitch2D.Companion.W
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/* ---------- 2,5D maç görünümü: yan tribünden topu izleyen TV kamerası ---------- */

/**
 * Perspektif kamera. Dünya: x sahanın boyu (0..L), y eni (0 uzak taç, W yakın taç), z yükseklik (m).
 * Kamera yakın tribünde, yüksekte; yalnız x ekseninde topu yumuşakça izler.
 */
private class TvCam(val camX: Float, w: Float, h: Float, focusY: Float) {
    private val yCam = W + 34f
    private val hCam = 23f
    private val cosT: Float
    private val sinT: Float
    val f: Float
    private val cx = w / 2
    private val cy: Float

    init {
        val lookY = W * 0.42f
        val th = atan2(hCam, yCam - lookY)
        cosT = cos(th); sinT = sin(th)
        fun vn(y: Float): Float { val yf = yCam - y; val z = -hCam; return (yf * sinT + z * cosT) / (yf * cosT - z * sinT) }
        // TV yakınlığı: ortada ~44 m genişlik görünür (oyuncular seçilebilir boyda)
        val dC = (yCam - lookY) * cosT + hCam * sinT
        f = w * dC / 44f
        // Dikeyde topu izle; üstte tribün şeridi kalsın, altta sahanın dışı boş görünmesin
        val want = h * 0.55f + f * vn(focusY)
        val lo = h + f * vn(W + 6f)
        val hi = h * 0.2f + f * vn(-3f)
        cy = if (lo <= hi) want.coerceIn(lo, hi) else (lo + hi) / 2
    }

    /** Dünya noktasını ekrana izdüşürür; [scaleOut] o derinlikte 1 metrenin piksel karşılığı. */
    fun p(x: Float, y: Float, z: Float = 0f): Offset {
        val X = x - camX
        val yf = yCam - y
        val Z = z - hCam
        val depth = yf * cosT - Z * sinT
        val v = yf * sinT + Z * cosT
        return Offset(cx + f * X / depth, cy - f * v / depth)
    }

    fun meter(y: Float, z: Float = 0f): Float {
        val yf = yCam - y
        val depth = yf * cosT - (z - hCam) * sinT
        return f / depth
    }
}

/** Kamera durumu: kareler arası yumuşak takip. */
private class CamState { var x = Float.NaN; var y = Float.NaN }

/** Çizilecek oyuncu (canlı ya da tekrar karesinden). */
private class TvMan(
    val id: Int, val side: Int, val gk: Boolean, val number: Int, val x: Float, val y: Float,
    val alpha: Float, val speed: Float, val focus: Boolean, val name: String,
)

@Composable
fun MatchPitchTV(
    anim: Pitch2D, homeC: Color, awayC: Color, homeGk: Color, awayGk: Color,
    running: Boolean, timeScale: Float, modifier: Modifier = Modifier,
    homeAlt: Color = Color.White, awayAlt: Color = Color.White, homeKit: Int = 0, awayKit: Int = 0,
) {
    var frame by remember { mutableLongStateOf(0L) }
    val run by rememberUpdatedState(running)
    val scale by rememberUpdatedState(timeScale)
    val cam = remember(anim) { CamState() }
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
    val crowd = remember { crowdSeats() }
    Canvas(modifier.pointerInput(anim) { detectTapGestures { anim.skipReplay() } }) {
        val t = frame
        if (size.minDimension <= 0f) return@Canvas
        val rv = anim.replayView
        val ballX = rv?.ballX ?: anim.ballX
        val ballY = rv?.ballY ?: anim.ballY
        val ballZ = rv?.ballZ ?: anim.ballZ
        // Kamera topu izler; kenarlarda durur ki saha boşluğa taşmasın
        val half = 19f
        val target = ballX.coerceIn(half, L - half)
        cam.x = if (cam.x.isNaN()) target else cam.x + (target - cam.x) * 0.07f
        cam.y = if (cam.y.isNaN()) ballY else cam.y + (ballY - cam.y) * 0.05f
        val c = TvCam(cam.x, size.width, size.height, cam.y)

        drawStands(c, crowd, t)
        drawGrass(c)
        drawTvLines(c)
        for (left in listOf(true, false)) drawGoal(c, left, back = true)

        anim.offsideLineX?.takeIf { rv == null }?.let { ox ->
            drawLine(Color(0xFFFFE14D).copy(alpha = 0.6f), c.p(ox, 0f), c.p(ox, W), 2.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f)))
        }

        val focus = if (rv == null) anim.focus() else null
        val men = if (rv != null) rv.agents.mapIndexed { i, a -> TvMan(i, a.side, a.gk, a.number, a.x, a.y, 1f, 0f, false, "") }
        else anim.agents.filter { it.alpha > 0f }.map { a ->
            TvMan(a.id, a.side, a.gk, a.number, a.x, a.y, a.alpha, hypot(a.vx, a.vy), a === focus, a.name)
        }
        // Gölgeler önce, sonra uzaktan yakına oyuncular (yakındakiler öndekini örter)
        for (m in men) {
            val k = c.meter(m.y)
            val foot = c.p(m.x, m.y)
            drawOval(Color.Black.copy(alpha = 0.28f * m.alpha), Offset(foot.x - k * 0.45f + k * 0.35f, foot.y - k * 0.14f), Size(k * 1.2f, k * 0.3f))
        }
        // Topun gölgesi
        val bk = c.meter(ballY)
        val bs = c.p(ballX, ballY)
        drawOval(Color.Black.copy(alpha = (0.4f - ballZ * 0.03f).coerceAtLeast(0.12f)), Offset(bs.x - bk * 0.3f, bs.y - bk * 0.1f), Size(bk * 0.6f, bk * 0.2f))
        val ballDrawn = BooleanArray(1)
        fun drawBall() {
            val b = c.p(ballX, ballY, ballZ + 0.11f)
            val br = (bk * 0.24f).coerceAtLeast(2.6f)
            drawCircle(Color.White, br, b)
            drawCircle(Color(0xFF263238), br * 0.42f, b + Offset(br * 0.2f, -br * 0.15f))
            drawCircle(Color.Black.copy(alpha = 0.35f), br, b, style = androidx.compose.ui.graphics.drawscope.Stroke(br * 0.18f))
            ballDrawn[0] = true
        }
        for (m in men.sortedBy { it.y }) {
            if (!ballDrawn[0] && ballY < m.y) drawBall()
            val fill = if (m.gk) (if (m.side == 0) homeGk else awayGk) else if (m.side == 0) homeC else awayC
            val kit = if (m.gk) 0 else if (m.side == 0) homeKit else awayKit
            drawMan(c, m, fill, if (m.side == 0) homeAlt else awayAlt, kit, t, measurer, numCache)
        }
        if (!ballDrawn[0]) drawBall()
        for (left in listOf(true, false)) drawGoal(c, left, back = false)

        // Topu süren oyuncunun adı (başının üstünde)
        men.firstOrNull { it.focus }?.let { m ->
            val head = c.p(m.x, m.y, 2.35f)
            val tl = measurer.measure(m.name, TextStyle(color = Color.White, fontSize = (11f / fontScale).sp, fontWeight = FontWeight.Bold))
            val tx = (head.x - tl.size.width / 2f).coerceIn(2f, (size.width - tl.size.width - 2f).coerceAtLeast(2f))
            val ty = (head.y - tl.size.height - 2f).coerceAtLeast(2f)
            drawRoundRect(Color.Black.copy(alpha = 0.6f), Offset(tx - 6f, ty - 1f), Size(tl.size.width + 12f, tl.size.height + 2f), cornerRadius = CornerRadius(8f, 8f))
            drawText(tl, topLeft = Offset(tx, ty))
        }
        // Hafif vinyet: TV görüntüsü hissi
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)), Offset(size.width / 2, size.height / 2), size.maxDimension * 0.75f))
        if (rv != null) drawReplayBands(rv.progress, measurer)
    }
}

/* ---------- Sahne parçaları ---------- */

private val GrassA = Color(0xFF2F8F48)
private val GrassB = Color(0xFF3AA155)

private fun DrawScope.quad(c: TvCam, x0: Float, y0: Float, x1: Float, y1: Float, color: Color, z0: Float = 0f, z1: Float = 0f) {
    val path = Path().apply {
        val a = c.p(x0, y0, z0); val b = c.p(x1, y0, z0); val d = c.p(x1, y1, z1); val e = c.p(x0, y1, z1)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); lineTo(e.x, e.y); close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawGrass(c: TvCam) {
    // Çim: sahanın dışında da devam eder (kenar payları), şeritler 7,5 m
    val x0 = c.camX - 90f
    val x1 = c.camX + 90f
    quad(c, x0, -6f, x1, W + 10f, GrassA)
    var x = Math.floorDiv(x0.toInt(), 15) * 15f
    while (x < x1) {
        quad(c, x, -6f, x + 7.5f, W + 10f, GrassB)
        x += 15f
    }
    // Uzaktaki çim biraz sisli: derinlik hissi
    val far = c.p(0f, -6f).y
    val mid = c.p(0f, W * 0.5f).y
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), far, mid), Offset(0f, far), Size(size.width, mid - far))
}

private fun DrawScope.drawTvLines(c: TvCam) {
    val line = Color.White.copy(alpha = 0.85f)
    fun seg(x0: Float, y0: Float, x1: Float, y1: Float) {
        val k = c.meter((y0 + y1) / 2)
        drawLine(line, c.p(x0, y0), c.p(x1, y1), (k * 0.14f).coerceAtLeast(1.2f), StrokeCap.Round)
    }
    fun rect(x: Float, y: Float, w: Float, h: Float) { seg(x, y, x + w, y); seg(x + w, y, x + w, y + h); seg(x + w, y + h, x, y + h); seg(x, y + h, x, y) }
    fun arc(cx: Float, cy: Float, r: Float, from: Float, to: Float, n: Int = 36) {
        var px = cx + r * cos(from); var py = cy + r * sin(from)
        for (i in 1..n) {
            val a = from + (to - from) * i / n
            val nx = cx + r * cos(a); val ny = cy + r * sin(a)
            seg(px, py, nx, ny); px = nx; py = ny
        }
    }
    rect(0f, 0f, L, W)
    seg(L / 2, 0f, L / 2, W)
    arc(L / 2, W / 2, 9.15f, 0f, (2 * Math.PI).toFloat(), 48)
    for (left in listOf(true, false)) {
        rect(if (left) 0f else L - 16.5f, W / 2 - 20.16f, 16.5f, 40.32f)
        rect(if (left) 0f else L - 5.5f, W / 2 - 9.16f, 5.5f, 18.32f)
        val spot = if (left) 11f else L - 11f
        drawCircle(line, (c.meter(W / 2) * 0.2f).coerceAtLeast(1.5f), c.p(spot, W / 2))
        val a = 0.9273f // acos(5.5/9.15): yayın ceza sahası dışında kalan kısmı
        if (left) arc(spot, W / 2, 9.15f, -a, a, 16) else arc(spot, W / 2, 9.15f, (Math.PI - a).toFloat(), (Math.PI + a).toFloat(), 16)
    }
    drawCircle(line, (c.meter(W / 2) * 0.22f).coerceAtLeast(1.5f), c.p(L / 2, W / 2))
}

/** Kale: [back] true ise file ve arka direkler (oyunculardan önce), false ise ön direkler ve üst direk. */
private fun DrawScope.drawGoal(c: TvCam, left: Boolean, back: Boolean) {
    val gx = if (left) 0f else L
    val bx = if (left) -2f else L + 2f
    val y0 = W / 2 - Pitch2D.GOAL_HALF
    val y1 = W / 2 + Pitch2D.GOAL_HALF
    val hgt = 2.44f
    val net = Color.White.copy(alpha = 0.32f)
    val k = c.meter(W / 2)
    if (back) {
        // File: arka yüz ve yanlar, ağ çizgileri
        quad(c, bx, y0, bx, y1, Color.White.copy(alpha = 0.08f))
        val pts = listOf(c.p(bx, y0, 0f), c.p(bx, y1, 0f), c.p(bx, y1, hgt * 0.8f), c.p(bx, y0, hgt * 0.8f))
        drawPath(Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }, Color.White.copy(alpha = 0.10f))
        var y = y0
        while (y <= y1 + 0.01f) { drawLine(net, c.p(bx, y, 0f), c.p(bx, y, hgt * 0.8f), 1f); drawLine(net, c.p(bx, y, hgt * 0.8f), c.p(gx, y, hgt), 1f); y += 0.9f }
        var z = 0f
        while (z <= hgt * 0.8f) { drawLine(net, c.p(bx, y0, z), c.p(bx, y1, z), 1f); z += 0.5f }
        for (yy in listOf(y0, y1)) drawLine(net, c.p(gx, yy, hgt), c.p(bx, yy, hgt * 0.8f), 1.2f)
        // Uzak direk
        drawLine(Color.White, c.p(gx, y0, 0f), c.p(gx, y0, hgt), (k * 0.14f).coerceAtLeast(2f), StrokeCap.Round)
    } else {
        val sw = (k * 0.16f).coerceAtLeast(2.2f)
        drawLine(Color.White, c.p(gx, y1, 0f), c.p(gx, y1, hgt), sw, StrokeCap.Round)
        drawLine(Color.White, c.p(gx, y0, hgt), c.p(gx, y1, hgt), sw, StrokeCap.Round)
    }
}

/** Tribündeki seyirci noktaları (dünya koordinatında, kamera dönünce kayar). */
private class Seat(val x: Float, val row: Int, val col: Int, val ph: Float)

private fun crowdSeats(): List<Seat> {
    val r = Random(7)
    val out = ArrayList<Seat>()
    for (row in 0 until 12) {
        var x = -40f + r.nextFloat() * 0.6f
        while (x < L + 40f) { out += Seat(x, row, r.nextInt(6), r.nextFloat() * 6.28f); x += 0.55f + r.nextFloat() * 0.35f }
    }
    return out
}

private val CrowdColors = listOf(
    Color(0xFFE6E6E6), Color(0xFF2B2F45), Color(0xFFC9473E), Color(0xFF3D5BA8), Color(0xFFE2C24A), Color(0xFF7A7F95),
)

private fun DrawScope.drawStands(c: TvCam, seats: List<Seat>, t: Long) {
    // Gökyüzü/çatı: stadyumun karanlık üst kısmı ve projektör ışığı
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0F24), Color(0xFF1B2448))), size = size)
    val baseY = -9f
    val top = c.p(c.camX, baseY - 16f, 25f).y
    val bottom = c.p(c.camX, -5f, 0f).y
    drawRect(Brush.verticalGradient(listOf(Color(0xFF20263D), Color(0xFF2F3550)), top, bottom), Offset(0f, top), Size(size.width, bottom - top))
    // Seyirciler: sıra sıra, uzaklaştıkça küçülür; her biri hafifçe sallanır
    val still = !Fx.enabled || Fx.lowPower
    val time = t / 1e9f
    val buckets = Array(CrowdColors.size) { ArrayList<Offset>() }
    var dotPx = 2f
    for (s in seats) {
        if (s.x < c.camX - 70f || s.x > c.camX + 70f) continue
        val y = baseY - s.row * 1.3f
        val z = 1.2f + s.row * 2.0f
        val bob = if (still) 0f else (sin(time * 3f + s.ph) * 0.12f).coerceAtLeast(0f)
        buckets[s.col] += c.p(s.x, y, z + bob)
        if (s.row == 0) dotPx = c.meter(y, z) * 0.95f
    }
    for (i in buckets.indices) drawPoints(buckets[i], PointMode.Points, CrowdColors[i], strokeWidth = dotPx.coerceIn(1.5f, 6f), cap = StrokeCap.Round)
    // Reklam panoları
    val ads = listOf(Color(0xFF1565C0), Color(0xFFD32F2F), Color(0xFF2E7D32), Color(0xFFF9A825), Color(0xFF6A1B9A))
    var x = Math.floorDiv((c.camX - 80f).toInt(), 12) * 12f
    while (x < c.camX + 80f) {
        val i = Math.floorMod((x / 12f).toInt(), ads.size)
        val a = c.p(x, -4.2f, 0f); val b = c.p(x + 12f, -4.2f, 0.95f)
        drawRect(Brush.verticalGradient(listOf(lerp(ads[i], Color.White, 0.25f), ads[i]), b.y, a.y), Offset(a.x, b.y), Size(b.x - a.x, a.y - b.y))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(a.x + (b.x - a.x) * 0.15f, b.y + (a.y - b.y) * 0.42f), Size((b.x - a.x) * 0.7f, (a.y - b.y) * 0.16f))
        x += 12f
    }
}

private val Skins = listOf(Color(0xFFF1C9A5), Color(0xFFD9A37A), Color(0xFFA8714A), Color(0xFF6E4429), Color(0xFFE8B98F))

private fun DrawScope.drawMan(c: TvCam, m: TvMan, fill: Color, alt: Color, kit: Int, t: Long, measurer: TextMeasurer, numCache: HashMap<String, TextLayoutResult>) {
    val foot = c.p(m.x, m.y)
    val k = c.meter(m.y)
    val h = 1.85f * k
    val a = m.alpha
    val bw = h * 0.3f
    val run = (m.speed / 6f).coerceIn(0f, 1f)
    val still = !Fx.enabled
    val sw = if (still) 0.35f else sin(t / 1e9f * 11f + m.id) * run
    val legLen = h * 0.44f
    val sock = lerp(fill, Color.Black, 0.55f)
    val shorts = lerp(fill, Color.Black, 0.6f).let { if (m.gk) lerp(fill, Color.Black, 0.35f) else it }
    // Bacaklar (koşarken açılıp kapanır)
    for (s in listOf(-1f, 1f)) {
        val dx = s * bw * 0.22f + s * sw * h * 0.09f
        drawLine(sock.copy(alpha = a), Offset(foot.x + dx, foot.y), Offset(foot.x + s * bw * 0.18f, foot.y - legLen), bw * 0.28f, StrokeCap.Round)
    }
    // Şort ve forma
    drawRoundRect(shorts.copy(alpha = a), Offset(foot.x - bw * 0.48f, foot.y - h * 0.56f), Size(bw * 0.96f, h * 0.16f), CornerRadius(bw * 0.15f))
    val shirtTop = foot.y - h * 0.86f
    drawRoundRect(fill.copy(alpha = a), Offset(foot.x - bw / 2, shirtTop), Size(bw, h * 0.34f), CornerRadius(bw * 0.3f))
    if (kit > 0 && a > 0.3f) {
        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(foot.x - bw / 2, shirtTop, foot.x + bw / 2, shirtTop + h * 0.34f, CornerRadius(bw * 0.3f))) }
        clipPath(clip) { drawKitPattern(alt.copy(alpha = a), kit, foot.x - bw / 2, shirtTop, bw, h * 0.34f) }
    }
    // Kollar
    for (s in listOf(-1f, 1f)) {
        drawLine(fill.copy(alpha = a), Offset(foot.x + s * bw * 0.45f, shirtTop + h * 0.05f), Offset(foot.x + s * bw * 0.62f - s * sw * h * 0.05f, shirtTop + h * 0.27f), bw * 0.22f, StrokeCap.Round)
    }
    // Hafif ışık: sol üstten
    drawRoundRect(Color.White.copy(alpha = 0.18f * a), Offset(foot.x - bw / 2, shirtTop), Size(bw * 0.45f, h * 0.34f), CornerRadius(bw * 0.3f))
    // Baş
    val skin = Skins[Math.floorMod(m.id * 7 + m.side, Skins.size)]
    drawCircle(skin.copy(alpha = a), h * 0.085f, Offset(foot.x, shirtTop - h * 0.07f))
    if (m.focus) drawOval(Color(0xFFFFE14D), Offset(foot.x - k * 0.75f, foot.y - k * 0.22f), Size(k * 1.5f, k * 0.44f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f))
    // Forma numarası: oyuncu yeterince büyükse
    if (m.number > 0 && h > 26f && a > 0.5f) {
        val px = (h * 0.15f / density / fontScale)
        val key = "${m.number}-${contentOn(fill).hashCode()}-${px.toInt()}"
        val tl = numCache.getOrPut(key) {
            measurer.measure("${m.number}", TextStyle(color = contentOn(fill), fontSize = px.sp, fontWeight = FontWeight.Black))
        }
        drawText(tl, topLeft = Offset(foot.x - tl.size.width / 2f, shirtTop + h * 0.17f - tl.size.height / 2f))
    }
}

private fun DrawScope.drawReplayBands(progress: Float, measurer: TextMeasurer) {
    val band = size.height * 0.07f
    drawRect(Color.Black.copy(alpha = 0.75f), size = Size(size.width, band))
    drawRect(Color.Black.copy(alpha = 0.75f), Offset(0f, size.height - band), Size(size.width, band))
    val label = measurer.measure("⟲ TEKRAR · dokun: geç", TextStyle(color = Color(0xFFFFE14D), fontSize = (band * 0.42f / density / fontScale).sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp))
    drawText(label, topLeft = Offset(size.width * 0.03f, (band - label.size.height) / 2f))
    drawRect(Color(0xFFFFE14D), Offset(0f, size.height - band * 0.2f), Size(size.width * progress, band * 0.12f))
}
