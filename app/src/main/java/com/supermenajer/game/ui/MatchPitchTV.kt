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
import kotlin.math.abs
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
private class TvCam(val camX: Float, w: Float, h: Float, focusY: Float, ballY: Float = focusY) {
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
        var y = if (lo <= hi) want.coerceIn(lo, hi) else (lo + hi) / 2
        // Top her zaman kadrajda (üstte skor bandına, altta yazı şeridine girmeden)
        y = y.coerceIn(h * 0.14f + f * vn(ballY), h * 0.78f + f * vn(ballY))
        cy = y
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

    /** [y] derinliğinde ekranın yarım genişliği (metre). */
    fun halfWidthAt(y: Float): Float = (cx) / meter(y)

    fun meter(y: Float, z: Float = 0f): Float {
        val yf = yCam - y
        val depth = yf * cosT - (z - hCam) * sinT
        return f / depth
    }
}

/** Kamera durumu: kareler arası yumuşak takip. */
private class CamState { var x = Float.NaN; var y = Float.NaN }

/** Topun son konumları (iz efekti) ve dönüş açısı. */
private class BallTrail {
    val xs = FloatArray(N); val ys = FloatArray(N); val zs = FloatArray(N)
    var count = 0
    var head = 0
    var spin = 0f
    var speed = 0f
    var lastT = 0L

    fun push(x: Float, y: Float, z: Float, t: Long) {
        if (count > 0) {
            val p = (head - 1 + N) % N
            val d = kotlin.math.sqrt((x - xs[p]) * (x - xs[p]) + (y - ys[p]) * (y - ys[p]) + (z - zs[p]) * (z - zs[p]))
            val dt = ((t - lastT) / 1e9f).coerceIn(1e-3f, 0.2f)
            speed = speed * 0.6f + (d / dt) * 0.4f
            spin += d * 2.6f * (if (x < xs[p]) -1f else 1f)
            if (d > 12f) count = 0 // sahne değişti (tekrar, santra): iz bağlanmasın
        }
        xs[head] = x; ys[head] = y; zs[head] = z
        head = (head + 1) % N
        count = (count + 1).coerceAtMost(N)
        lastT = t
    }

    /** i = 0 en yeni. */
    fun at(i: Int): Int = (head - 1 - i + N * 2) % N

    companion object { const val N = 7 }
}

/** Çizilecek oyuncu (canlı ya da tekrar karesinden). */
private class TvMan(
    val id: Int, val side: Int, val gk: Boolean, val number: Int, val x: Float, val y: Float,
    val alpha: Float, val speed: Float, val focus: Boolean, val name: String,
    val vx: Float = 0f, val pose: Int = 0, val poseT: Float = 0f, val poseDir: Float = 1f,
)

@Composable
fun MatchPitchTV(
    anim: Pitch2D, homeC: Color, awayC: Color, homeGk: Color, awayGk: Color,
    running: Boolean, timeScale: Float, modifier: Modifier = Modifier,
    homeAlt: Color = Color.White, awayAlt: Color = Color.White, homeKit: Int = 0, awayKit: Int = 0,
    skyOverride: TvSky? = null,
) {
    // Maç günü koşulları: hava ve akşam ışıkları
    val sky = skyOverride ?: remember(anim) {
        TvSky.of(runCatching { if (anim.sim.nat != null) null else com.supermenajer.game.engine.Conditions.of(anim.sim.game, anim.sim.fixture) }.getOrNull())
    }
    var frame by remember { mutableLongStateOf(0L) }
    val run by rememberUpdatedState(running)
    val scale by rememberUpdatedState(timeScale)
    val cam = remember(anim) { CamState() }
    val trail = remember(anim) { BallTrail() }
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
        // Kamera: topu izler ama oyuncuların toplandığı yeri de kadrajda tutar (kornerde boş çime bakmasın).
        // Tekrarda gol atılan kale de kadraja girsin diye kaleye doğru kayar.
        val near = if (rv != null) rv.agents.filter { kotlin.math.abs(it.x - ballX) < 26f }.map { it.y }
        else anim.agents.filter { it.alpha > 0.5f && kotlin.math.abs(it.x - ballX) < 26f }.map { it.y }
        val crowdY = if (near.size >= 3) near.sorted()[near.size / 2] else W / 2
        val goalX = if (rv != null) (if (rv.goalX > L / 2) L else 0f) else ballX
        val tx = if (rv != null) ballX + (goalX - ballX) * 0.45f else ballX
        val target = tx.coerceIn(half, L - half)
        val ty = (ballY * 0.5f + crowdY * 0.5f).coerceIn(W * 0.15f, W * 0.85f)
        cam.x = if (cam.x.isNaN()) target else cam.x + (target - cam.x) * 0.07f
        cam.y = if (cam.y.isNaN()) ty else cam.y + (ty - cam.y) * 0.05f
        // Top yatayda da kadrajda kalsın (kale çizgisi ve köşe bayrağı dahil)
        val hw = TvCam(cam.x, size.width, size.height, cam.y, ballY).halfWidthAt(ballY) * 0.8f
        cam.x = cam.x.coerceIn(ballX - hw, ballX + hw)
        val c = TvCam(cam.x, size.width, size.height, cam.y, ballY)

        val sec = t / 1e9f
        drawStands(c, crowd, t, sky)
        drawFloodlights(sky, c.p(c.camX, -25f, 25f).y, c.camX, sec)
        drawGrass(c, sky)
        drawTvLines(c)
        drawGroundFx(sky, c.p(c.camX, -5f).y)
        for (left in listOf(true, false)) drawGoal(c, left, back = true)

        anim.offsideLineX?.takeIf { rv == null }?.let { ox ->
            drawLine(Color(0xFFFFE14D).copy(alpha = 0.6f), c.p(ox, 0f), c.p(ox, W), 2.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f)))
        }

        val focus = if (rv == null) anim.focus() else null
        val men = if (rv != null) rv.agents.map { a -> TvMan(a.id, a.side, a.gk, a.number, a.x, a.y, 1f, a.speed, false, "", a.vx, a.pose, a.poseT, a.poseDir) }
        else anim.agents.filter { it.alpha > 0f }.map { a ->
            TvMan(a.id, a.side, a.gk, a.number, a.x, a.y, a.alpha, hypot(a.vx, a.vy), a === focus, a.name, a.vx, a.pose, a.poseT, a.poseDir)
        }
        // Gölgeler önce, sonra uzaktan yakına oyuncular (yakındakiler öndekini örter)
        val shA = sky.shadowAlpha
        for (m in men) {
            val k = c.meter(m.y)
            val foot = c.p(m.x, m.y)
            if (sky.night) {
                // Dört köşe projektöründen dört soluk gölge
                for ((dx, dy) in listOf(0.55f to -0.05f, -0.55f to -0.05f, 0.4f to 0.08f, -0.4f to 0.08f)) {
                    drawOval(Color.Black.copy(alpha = shA * m.alpha), Offset(foot.x - k * 0.5f + k * dx, foot.y - k * 0.12f + k * dy), Size(k * 1.0f, k * 0.24f))
                }
            } else {
                val len = if (sky.heat) 1.5f else 1.2f
                drawOval(Color.Black.copy(alpha = shA * m.alpha), Offset(foot.x - k * 0.45f + k * 0.35f, foot.y - k * 0.14f), Size(k * len, k * 0.3f))
            }
        }
        // Topun gölgesi: yükseldikçe büyür ve soluklaşır
        val bk = c.meter(ballY)
        val bs = c.p(ballX, ballY)
        val hz = ballZ.coerceIn(0f, 12f)
        val sh = 1f + hz * 0.09f
        drawOval(Color.Black.copy(alpha = (0.42f - hz * 0.03f).coerceAtLeast(0.1f)), Offset(bs.x - bk * 0.24f * sh, bs.y - bk * 0.08f * sh), Size(bk * 0.48f * sh, bk * 0.16f * sh))
        if (Fx.enabled) trail.push(ballX, ballY, ballZ, t)
        val ballDrawn = BooleanArray(1)
        fun drawBall() {
            val b = c.p(ballX, ballY, ballZ + 0.11f)
            val br = (bk * 0.2f).coerceAtLeast(2.6f)
            // Sert şut / uzun top: arkasında solan iz
            if (trail.speed > 15f && trail.count >= 3) {
                val pts = (0 until trail.count).map { i -> val j = trail.at(i); c.p(trail.xs[j], trail.ys[j], trail.zs[j] + 0.11f) }
                val strength = ((trail.speed - 15f) / 12f).coerceIn(0.25f, 1f)
                for (i in 0 until pts.size - 1) {
                    val f = 1f - i / (pts.size - 1f)
                    drawLine(Color.White.copy(alpha = 0.42f * f * strength), pts[i], pts[i + 1], br * 1.6f * f, StrokeCap.Round)
                }
            }
            drawSoccerBall(b, br, trail.spin)
            ballDrawn[0] = true
        }
        for (m in men.sortedBy { it.y }) {
            if (!ballDrawn[0] && ballY < m.y) drawBall()
            val fill = if (m.gk) (if (m.side == 0) homeGk else awayGk) else if (m.side == 0) homeC else awayC
            val kit = if (m.gk) 0 else if (m.side == 0) homeKit else awayKit
            drawMan(c, m, fill, if (m.side == 0) homeAlt else awayAlt, kit, t, measurer, numCache, ballX)
        }
        if (!ballDrawn[0]) drawBall()
        for (left in listOf(true, false)) drawGoal(c, left, back = false)
        drawWeatherParticles(sky, sec, !Fx.enabled || Fx.lowPower)

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

private fun DrawScope.drawGrass(c: TvCam, sky: TvSky) {
    // Çim: sahanın dışında da devam eder (kenar payları), şeritler 7,5 m
    val x0 = c.camX - 90f
    val x1 = c.camX + 90f
    val ga = grassTint(GrassA, sky)
    val gb = grassTint(GrassB, sky)
    quad(c, x0, -6f, x1, W + 10f, ga)
    var x = Math.floorDiv(x0.toInt(), 15) * 15f
    while (x < x1) {
        quad(c, x, -6f, x + 7.5f, W + 10f, gb)
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

private fun DrawScope.drawStands(c: TvCam, seats: List<Seat>, t: Long, sky: TvSky) {
    // Gökyüzü: havaya ve saate göre (akşam koyu, güneşte mavi)
    drawRect(skyBrush(sky, size.height), size = size)
    val baseY = -9f
    val top = c.p(c.camX, baseY - 16f, 25f).y
    val bottom = c.p(c.camX, -5f, 0f).y
    val standCols = if (sky.night) listOf(Color(0xFF0D1122), Color(0xFF1A1F35)) else listOf(Color(0xFF20263D), Color(0xFF2F3550))
    drawRect(Brush.verticalGradient(standCols, top, bottom), Offset(0f, top), Size(size.width, bottom - top))
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

private fun DrawScope.drawMan(
    c: TvCam, m: TvMan, fill: Color, alt: Color, kit: Int, t: Long,
    measurer: TextMeasurer, numCache: HashMap<String, TextLayoutResult>, ballX: Float,
) {
    val foot = c.p(m.x, m.y)
    val k = c.meter(m.y)
    val still = !Fx.enabled
    val sec = t / 1e9f
    // Bakış yönü: koşuyorsa koştuğu yöne, duruyorsa topa (pozlarda pozun yönüne)
    val face = when {
        m.pose != 0 && m.pose != com.supermenajer.game.engine.Pose.DIVE -> m.poseDir
        kotlin.math.abs(m.vx) > 0.6f -> if (m.vx < 0f) -1f else 1f
        else -> if (ballX < m.x) -1f else 1f
    }
    val run = if (still) 0.35f else (m.speed / 6.5f).coerceIn(0f, 1f)
    val phase = if (still) 0.9f + m.id else runPhase(sec, m.id, m.speed)
    val motion = Motion(face, run, phase, m.pose, m.poseT, m.poseDir, m.alpha, if (still) 0.3f else sec)
    val look = Look.of(m.id, m.side, m.gk)
    val sock = lerp(fill, Color.Black, 0.55f)
    val shorts = lerp(fill, Color.Black, 0.6f).let { if (m.gk) lerp(fill, Color.Black, 0.35f) else it }
    val a = m.alpha
    drawTvPlayer(
        foot, k, look, motion, fill, shorts, sock,
        shirt = if (kit > 0) { l, tp, w, h -> drawKitPattern(alt.copy(alpha = a), kit, l, tp, w, h) } else null,
        number = if (m.number > 0 && a > 0.5f) { cx, cy, h ->
            if (h > 26f) {
                val px = (h * 0.14f / density / fontScale)
                val key = "${m.number}-${contentOn(fill).hashCode()}-${px.toInt()}"
                val tl = numCache.getOrPut(key) {
                    measurer.measure("${m.number}", TextStyle(color = contentOn(fill), fontSize = px.sp, fontWeight = FontWeight.Black))
                }
                drawText(tl, topLeft = Offset(cx - tl.size.width / 2f, cy - tl.size.height / 2f))
            }
        } else null,
    )
    if (m.focus) drawOval(Color(0xFFFFE14D), Offset(foot.x - k * 0.75f, foot.y - k * 0.22f), Size(k * 1.5f, k * 0.44f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f))
}

private fun DrawScope.drawReplayBands(progress: Float, measurer: TextMeasurer) {
    val band = size.height * 0.07f
    drawRect(Color.Black.copy(alpha = 0.75f), size = Size(size.width, band))
    drawRect(Color.Black.copy(alpha = 0.75f), Offset(0f, size.height - band), Size(size.width, band))
    val label = measurer.measure(L("⟲ TEKRAR · dokun: geç", "⟲ REPLAY · tap: skip"), TextStyle(color = Color(0xFFFFE14D), fontSize = (band * 0.42f / density / fontScale).sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp))
    drawText(label, topLeft = Offset(size.width * 0.03f, (band - label.size.height) / 2f))
    drawRect(Color(0xFFFFE14D), Offset(0f, size.height - band * 0.2f), Size(size.width * progress, band * 0.12f))
}

/** Futbol topu: yuvarlak ışık, dönen beşgen deseni ve ince kontur (küçükken sade nokta). */
internal fun DrawScope.drawSoccerBall(c: Offset, r: Float, spin: Float) {
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFDCE1E6), Color(0xFF9AA4AE)), c + Offset(-r * 0.35f, -r * 0.4f), r * 1.6f), r, c)
    if (r >= 4f) {
        val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(clip) {
            val dark = Color(0xFF20262C)
            fun pent(cx: Float, cy: Float, pr: Float, rot: Float) {
                val p = Path()
                for (i in 0 until 5) {
                    val ang = rot + i * (2f * Math.PI.toFloat() / 5f) - Math.PI.toFloat() / 2f
                    val x = cx + cos(ang) * pr; val y = cy + sin(ang) * pr
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close(); drawPath(p, dark)
            }
            // Dönüş: desen top üzerinde kayar (yanlara gidince kısalır, küre hissi)
            val off = ((spin % 2f) + 2f) % 2f - 1f
            pent(c.x + off * r * 0.55f, c.y + r * 0.05f, r * 0.34f * (1f - abs(off) * 0.35f), spin * 0.4f)
            for (i in 0 until 5) {
                val ang = spin * 0.4f + i * 2f * Math.PI.toFloat() / 5f
                pent(c.x + off * r * 0.55f + cos(ang) * r * 0.95f, c.y + sin(ang) * r * 0.95f, r * 0.26f, ang)
            }
        }
    } else drawCircle(Color(0xFF263238), r * 0.4f, c + Offset(r * 0.2f, -r * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.35f), r, c, style = androidx.compose.ui.graphics.drawscope.Stroke((r * 0.14f).coerceAtLeast(0.8f)))
    // Parlama
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.22f, c + Offset(-r * 0.38f, -r * 0.42f))
}
