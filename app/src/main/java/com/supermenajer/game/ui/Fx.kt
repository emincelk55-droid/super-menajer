package com.supermenajer.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/* ---------- v2.9 görsel efektler: basma, belirme ---------- */

/** Giriş animasyonları açık mı? Ekran görüntüsü testleri kapatır ki son hâl çizilsin. */
object Fx {
    @JvmField var enabled = true

    /** Pil tasarrufu: sürekli dönen efektler (parlama, stadyum, kart ışığı) durur; dokunma efektleri kalır. */
    var lowPower by androidx.compose.runtime.mutableStateOf(false)
        private set
    private var prefs: android.content.SharedPreferences? = null

    /** Yazı boyutu: 0 normal, 1 büyük, 2 çok büyük. */
    var textSize by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    val textScale: Float get() = when (textSize) { 1 -> 1.12f; 2 -> 1.24f; else -> 1f }

    /** Ana sayfa sade görünüm: yalnız maç, kulüp durumu, kararlar ve gazete (yeni oyuncu boğulmasın). */
    var homeSimple by androidx.compose.runtime.mutableStateOf(true)
        private set

    fun changeHomeSimple(v: Boolean) {
        homeSimple = v
        prefs?.edit()?.putBoolean("homeSimple", v)?.apply()
    }

    /** Tercih dosyası (ipuçları gibi küçük ayarlar için); testlerde null. */
    fun prefsOrNull(): android.content.SharedPreferences? = prefs

    fun changeTextSize(v: Int) {
        textSize = v.coerceIn(0, 2)
        prefs?.edit()?.putInt("textSize", textSize)?.apply()
    }

    /** Gol tekrarı (ağır çekim) açık mı. */
    var replays by androidx.compose.runtime.mutableStateOf(true)
        private set

    /** Maç görünümü: true 2,5D TV kamerası, false klasik üstten 2D. */
    var tvView by androidx.compose.runtime.mutableStateOf(true)
        private set

    fun changeTvView(on: Boolean) {
        tvView = on
        prefs?.edit()?.putBoolean("tvView", on)?.apply()
    }

    fun init(context: android.content.Context) {
        prefs = context.getSharedPreferences("fx", android.content.Context.MODE_PRIVATE).also {
            lowPower = it.getBoolean("lowPower", false)
            replays = it.getBoolean("replays", true)
            textSize = it.getInt("textSize", 0)
            homeSimple = it.getBoolean("homeSimple", true)
            tvView = it.getBoolean("tvView", true)
        }
    }

    fun changeReplays(on: Boolean) {
        replays = on
        prefs?.edit()?.putBoolean("replays", on)?.apply()
    }

    fun changeLowPower(on: Boolean) {
        lowPower = on
        prefs?.edit()?.putBoolean("lowPower", on)?.apply()
    }
}

/** Sürekli (sonsuz) animasyonlar kapalı mı: test, önizleme ya da pil tasarrufu. */
@androidx.compose.runtime.Composable
fun fxLoopsOff(): Boolean = fxStill() || Fx.lowPower

/** Animasyonsuz çizim: önizleme ya da animasyonların kapalı olduğu testler. */
@androidx.compose.runtime.Composable
fun fxStill(): Boolean = !Fx.enabled || LocalInspectionMode.current

/**
 * Dokununca yaylı şekilde hafifçe küçülen tıklama. Zincirin başına konmalı ki
 * arka plan ve kenarlık da birlikte küçülsün.
 */
fun Modifier.pressClick(scaleTo: Float = 0.95f, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press",
    )
    graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled) {
            com.supermenajer.game.audio.Sound.click()
            onClick()
        }
}

/** Basma efekti, tıklamayı zincirin sonunda ayrıca kuran yerler için (yalnız ölçek). */
fun Modifier.pressScale(src: MutableInteractionSource, scaleTo: Float = 0.96f): Modifier = composed {
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "pressS",
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * İlk görünüşte aşağıdan yukarı kayarak belirme. Önizleme/ekran görüntüsünde
 * (inspection mode) son hâliyle çizilir.
 */
fun Modifier.appear(delayMs: Int = 0, offsetDp: Float = 16f, fromScale: Float = 1f): Modifier = composed {
    if (fxStill()) return@composed this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        a.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
    }
    val px = with(LocalDensity.current) { offsetDp.dp.toPx() }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * px
        val sc = fromScale + (1f - fromScale) * a.value
        scaleX = sc; scaleY = sc
    }
}

/** Düğmenin üstünden periyodik olarak geçen parlak ışık şeridi (birincil düğmeler). */
fun Modifier.shine(periodMs: Int = 3200): Modifier = composed {
    if (fxLoopsOff()) return@composed this
    val t by androidx.compose.animation.core.rememberInfiniteTransition(label = "shine").animateFloat(
        -0.4f, 1.6f,
        androidx.compose.animation.core.infiniteRepeatable(tween(periodMs, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shineX",
    )
    drawWithContent {
        drawContent()
        // t 0..1 arasında soldan sağa geçer; aralığın dışında bekler.
        val x = t * size.width
        if (t in 0f..1f) drawRect(
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f), androidx.compose.ui.graphics.Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(x - size.height, 0f),
                end = androidx.compose.ui.geometry.Offset(x + size.height, size.height),
            ),
        )
    }
}

/* ---------- Parlak oyuncu kartları ---------- */

/** Kart seviyesi: 3 efsane (85+), 2 altın (75+), 1 gümüş (65+), 0 bronz. */
fun cardTier(ovr: Int): Int = when {
    ovr >= 85 -> 3
    ovr >= 75 -> 2
    ovr >= 65 -> 1
    else -> 0
}

/** Seviyeye göre kart kenarı renkleri (üstten alta). */
fun tierBorder(tier: Int): List<androidx.compose.ui.graphics.Color> = when (tier) {
    3 -> listOf(androidx.compose.ui.graphics.Color(0xFFFFF1A8), androidx.compose.ui.graphics.Color(0xFFD4AF37), androidx.compose.ui.graphics.Color(0xFF9B59FF))
    2 -> listOf(androidx.compose.ui.graphics.Color(0xFFFFF1A8), androidx.compose.ui.graphics.Color(0xFFD4AF37))
    1 -> listOf(androidx.compose.ui.graphics.Color(0xFFF4F7FA), androidx.compose.ui.graphics.Color(0xFF9AA5AE))
    else -> listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
}

/**
 * Oyuncu kartı parlaklığı: üstte sabit cam parıltısı, ara ara kartın üstünden çapraz geçen
 * ışık şeridi (seviye yükseldikçe güçlenir) ve efsane kartlarda göz kırpan yıldızlar.
 * [seed] kartların aynı anda parlamaması için başlangıcı kaydırır.
 */
fun Modifier.cardShine(tier: Int, seed: Int, sparkleRight: Boolean = false): Modifier = composed {
    val still = fxLoopsOff()
    val period = 4200
    // Değer çizim sırasında okunur: her karede yalnız yeniden çizilir, yeniden oluşturulmaz (pil dostu)
    val anim: androidx.compose.runtime.State<Float>? = if (still) null else {
        val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "card")
        tr.animateFloat(
            0f, 1f,
            androidx.compose.animation.core.infiniteRepeatable(
                tween(period, easing = androidx.compose.animation.core.LinearEasing),
                initialStartOffset = androidx.compose.animation.core.StartOffset(Math.floorMod(seed * 977, period)),
            ),
            label = "cardT",
        )
    }
    val strength = when (tier) { 3 -> 0.55f; 2 -> 0.42f; 1 -> 0.3f; else -> 0.12f }
    drawWithContent {
        drawContent()
        val t = anim?.value ?: 0.18f
        val w = size.width
        val h = size.height
        val W = androidx.compose.ui.graphics.Color.White
        val T = androidx.compose.ui.graphics.Color.Transparent
        // Üst yarıda hafif cam parıltısı
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(W.copy(alpha = 0.16f * (tier + 1) / 4f), T), 0f, h * 0.45f))
        // Döngünün ilk %40'ında soldan sağa çapraz ışık şeridi
        val q = t / 0.4f
        if (q in 0f..1f) {
            val x = -w * 0.6f + q * (w * 2.2f)
            drawRect(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(T, W.copy(alpha = strength), T),
                    start = androidx.compose.ui.geometry.Offset(x - w * 0.35f, 0f),
                    end = androidx.compose.ui.geometry.Offset(x + w * 0.35f, h * 0.6f),
                ),
            )
        }
        if (tier == 3) {
            // Efsane kart: seed'e göre yerleşen, sırayla parlayan 4 yıldız
            val r = kotlin.random.Random(seed)
            repeat(4) { k ->
                // Yıldızlar güç sayısının olmadığı yarıda: FUT kartında sağ, saha kartında sol
                val sx = w * ((if (sparkleRight) 0.52f else 0.1f) + r.nextFloat() * 0.36f)
                val sy = h * (0.12f + r.nextFloat() * 0.28f)
                val ph = ((t * 2f + k * 0.25f) % 1f)
                val a = if (still) 0.8f else kotlin.math.sin(ph * Math.PI.toFloat()).coerceAtLeast(0f)
                val len = w * 0.07f * (0.6f + a * 0.4f)
                val c = androidx.compose.ui.graphics.Color(0xFFFFF6C8).copy(alpha = a)
                drawLine(c, androidx.compose.ui.geometry.Offset(sx - len, sy), androidx.compose.ui.geometry.Offset(sx + len, sy), w * 0.012f + 1f)
                drawLine(c, androidx.compose.ui.geometry.Offset(sx, sy - len), androidx.compose.ui.geometry.Offset(sx, sy + len), w * 0.012f + 1f)
                drawCircle(c, w * 0.018f + 1f, androidx.compose.ui.geometry.Offset(sx, sy))
            }
        }
    }
}
