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
}

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
        .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
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
    if (fxStill()) return@composed this
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
