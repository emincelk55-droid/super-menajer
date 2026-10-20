package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Screen
import kotlin.math.sin

const val APP_VERSION = "v2.2"

private val tips = listOf(
    "İpucu: DEVAM düğmesi günleri tek tek geçirir, önemli bir olayda durur.",
    "İpucu: Oyuncuları taktik ekranında sürükleyerek yerlerini değiştirebilirsin.",
    "İpucu: Gözlemci raporu olmadan oyuncunun gerçek gücünü bilemezsin.",
    "İpucu: Genç oyuncuları kiralık göndermek gelişimlerini hızlandırır.",
    "İpucu: Yönetimin güveni sıfıra inerse kovulursun. Hedefi takip et!",
)

/** Oyun markası: koyu lacivert-mor gradyan, stadyum ışıkları ve perspektif saha çizgileri. */
@Composable
fun MenuBackground(modifier: Modifier = Modifier, glow: Float = 1f, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B0F2A), Color(0xFF141238), Color(0xFF070A1C))))) {
        Canvas(Modifier.fillMaxSize()) { drawStadium(glow) }
        content()
    }
}

private fun DrawScope.drawStadium(glow: Float) {
    val w = size.width
    val h = size.height
    // Projektörler
    for (x in listOf(0.08f, 0.92f)) {
        drawCircle(
            Brush.radialGradient(listOf(Color(0x5546F0C8).copy(alpha = 0.33f * glow), Color.Transparent), Offset(w * x, -h * 0.05f), h * 0.9f),
            h * 0.9f, Offset(w * x, -h * 0.05f),
        )
    }
    drawCircle(
        Brush.radialGradient(listOf(Color(0x447B4DFF), Color.Transparent), Offset(w * 0.5f, h * 1.1f), h * 0.9f),
        h * 0.9f, Offset(w * 0.5f, h * 1.1f),
    )
    // Perspektif saha: alt yarıda yamuk
    val top = h * 0.62f
    val bottom = h * 1.05f
    val tl = Offset(w * 0.3f, top)
    val tr = Offset(w * 0.7f, top)
    val br = Offset(w * 0.98f, bottom)
    val bl = Offset(w * 0.02f, bottom)
    val pitch = Path().apply { moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }
    drawPath(pitch, Brush.verticalGradient(listOf(Color(0x2219E3A1), Color(0x0019E3A1)), top, bottom))
    val line = Color.White.copy(alpha = 0.10f)
    drawPath(pitch, line, style = Stroke(2f))
    val midY = top + (bottom - top) * 0.45f
    fun xAt(y: Float, left: Boolean): Float {
        val t = (y - top) / (bottom - top)
        return if (left) tl.x + (bl.x - tl.x) * t else tr.x + (br.x - tr.x) * t
    }
    drawLine(line, Offset(xAt(midY, true), midY), Offset(xAt(midY, false), midY), 2f)
    drawOval(line, Offset(w * 0.43f, midY - h * 0.045f), Size(w * 0.14f, h * 0.09f), style = Stroke(2f))
    // Yıldız tozu
    for (i in 0 until 40) {
        val x = ((i * 97) % 100) / 100f * w
        val y = ((i * 53) % 60) / 100f * h
        drawCircle(Color.White.copy(alpha = 0.05f + (i % 5) * 0.03f), 1.2f + (i % 3), Offset(x, y))
    }
}

/** Kalkan biçimli oyun logosu. */
@Composable
fun GameCrest(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.14f)
            lineTo(w * 0.94f, h * 0.62f)
            quadraticTo(w * 0.8f, h * 0.88f, w * 0.5f, h)
            quadraticTo(w * 0.2f, h * 0.88f, w * 0.06f, h * 0.62f)
            lineTo(0f, h * 0.14f)
            close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF1DF0B0), Color(0xFF0B8F78), Color(0xFF14204A))))
        drawPath(shield, Color.White.copy(alpha = 0.85f), style = Stroke(w * 0.035f))
        // Top
        val c = Offset(w * 0.5f, h * 0.5f)
        val r = w * 0.22f
        drawCircle(Color.White, r, c)
        drawCircle(Color(0xFF14204A), r, c, style = Stroke(w * 0.02f))
        val pent = Path()
        for (i in 0 until 5) {
            val a = Math.toRadians(-90.0 + i * 72.0)
            val x = c.x + (r * 0.42f * kotlin.math.cos(a)).toFloat()
            val y = c.y + (r * 0.42f * sin(a)).toFloat()
            if (i == 0) pent.moveTo(x, y) else pent.lineTo(x, y)
        }
        pent.close()
        drawPath(pent, Color(0xFF14204A))
        // Yıldızlar
        for (i in -1..1) drawCircle(Color(0xFFF5C542), w * 0.03f, Offset(w * (0.5f + i * 0.13f), h * 0.17f))
    }
}

@Composable
private fun Wordmark(big: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SÜPER", fontSize = if (big) 40.sp else 26.sp, fontWeight = FontWeight.Black, color = TextMain, letterSpacing = 8.sp, lineHeight = if (big) 42.sp else 28.sp)
        Text("MENAJER", fontSize = if (big) 40.sp else 26.sp, fontWeight = FontWeight.Black, color = Green, letterSpacing = 8.sp, lineHeight = if (big) 42.sp else 28.sp)
    }
}

/**
 * Açılış ekranı: logo, yükleme çubuğu ve ipuçları. Dünya arka planda hazırlanır;
 * yükleme bitince "dokun ve başla" ile ana menüye geçilir.
 */
@Composable
fun SplashScreen() {
    val progress = remember { Animatable(0f) }
    var tip by remember { mutableIntStateOf((System.currentTimeMillis() / 1000 % tips.size).toInt()) }
    LaunchedEffect(Unit) {
        GameRepo.prepareWorldAsync()
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(3500); tip = (tip + 1) % tips.size }
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a",
    )
    val done = progress.value >= 1f
    val stage = when {
        progress.value < 0.25f -> "Ligler yükleniyor..."
        progress.value < 0.5f -> "6.500 oyuncu hazırlanıyor..."
        progress.value < 0.75f -> "Stadyumlar açılıyor..."
        progress.value < 1f -> "Fikstür çekiliyor..."
        else -> "Hazır!"
    }
    MenuBackground(
        Modifier.clickable(remember { MutableInteractionSource() }, null, enabled = done) { GameRepo.screen = Screen.TITLE },
        glow = 0.6f + 0.4f * progress.value,
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GameCrest(96.dp)
            Spacer(Modifier.height(10.dp))
            Wordmark(big = true)
            Text("FUTBOL MENAJERLİK OYUNU · 2026/27", color = TextDim, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.width(360.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                Box(
                    Modifier.fillMaxWidth(progress.value).fillMaxHeight().clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green, Color(0xFF7DFFD8)))),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (done) {
                Text("▶  BAŞLAMAK İÇİN DOKUN", color = Green, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp, modifier = Modifier.alpha(pulse))
            } else {
                Text("$stage  ${(progress.value * 100).toInt()}%", color = TextDim, fontSize = 13.sp)
            }
        }
        Text(
            tips[tip], color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = 24.dp, end = 24.dp),
        )
        Text(APP_VERSION, color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp))
    }
}

/** Ana menü: Devam Et, Yeni Kariyer, Kariyer Yükle, Nasıl Oynanır, Çıkış. */
@Composable
fun TitleScreen() {
    GameRepo.version
    val slots = GameRepo.slots()
    val last = GameRepo.lastSlot()?.let { n -> slots.firstOrNull { it.n == n } }
    LaunchedEffect(Unit) { GameRepo.prepareWorldAsync() }
    // 0 özet, 1 yükle, 2 yeni kariyer, 3 nasıl oynanır
    var panel by remember { mutableIntStateOf(if (last == null) 2 else 0) }
    var error by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableIntStateOf(0) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    BackHandler { if (panel != 0 && last != null) panel = 0 else GameRepo.screen = Screen.SPLASH }

    fun load(n: Int) { if (GameRepo.load(n)) GameRepo.screen = Screen.MAIN else error = true }
    fun startNew(n: Int) { GameRepo.slot = n; GameRepo.screen = Screen.NEW_GAME }

    MenuBackground {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Sol: logo ve menü
            Column(Modifier.width(330.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameCrest(54.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SÜPER MENAJER", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
                        Text("$APP_VERSION · 13 lig · gerçek kadrolar", color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (last != null) {
                    MenuButton("▶", "DEVAM ET", "${last.team} · ${last.detail.substringAfterLast(" · ")}", primary = true) { load(last.n) }
                    Spacer(Modifier.height(6.dp))
                }
                MenuButton("＋", "YENİ KARİYER", "Kulübünü seç, efsaneni yaz", selected = panel == 2) { panel = 2 }
                Spacer(Modifier.height(6.dp))
                MenuButton("📂", "KARİYER YÜKLE", "${slots.count { it.exists }}/3 kayıt", selected = panel == 1) { panel = 1 }
                Spacer(Modifier.height(6.dp))
                MenuButton("❓", "NASIL OYNANIR", "Kısa rehber", selected = panel == 3) { panel = 3 }
                Spacer(Modifier.height(6.dp))
                MenuButton("✕", "ÇIKIŞ", null) { (ctx as? android.app.Activity)?.finishAndRemoveTask() }
            }
            // Sağ: seçili panel
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Color(0xCC0E1430))
                    .border(1.dp, Green.copy(alpha = 0.25f), RoundedCornerShape(18.dp)).padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (panel) {
                    0 -> if (last != null) {
                        Text("SON KARİYER", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlotBadge(last, 74.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(last.team, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1)
                                if (last.manager.isNotEmpty()) Text("Menajer: ${last.manager}", color = TextDim, fontSize = 13.sp)
                                Text(last.detail, color = TextDim, fontSize = 13.sp)
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green))).clickable { load(last.n) },
                            contentAlignment = Alignment.Center,
                        ) { Text("KALDIĞIN YERDEN DEVAM ET  ▶", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                        Text("Yeni bir kulüple başlamak için YENİ KARİYER'i, diğer kayıtların için KARİYER YÜKLE'yi seç.", color = TextDim, fontSize = 12.sp)
                    }
                    1 -> {
                        Text("KAYIT YUVALARI", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        slots.forEach { s ->
                            SlotRow(s, if (s.exists) "YÜKLE" else "YENİ", confirmDelete == s.n,
                                onDelete = { if (confirmDelete == s.n) { GameRepo.deleteSlot(s.n); confirmDelete = 0 } else confirmDelete = s.n },
                            ) { if (s.exists) load(s.n) else startNew(s.n) }
                        }
                    }
                    2 -> {
                        Text("YENİ KARİYER · YUVA SEÇ", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Dolu bir yuvayı seçersen eski kariyer silinir.", color = TextDim, fontSize = 12.sp)
                        slots.forEach { s -> SlotRow(s, if (s.exists) "ÜZERİNE YAZ" else "BAŞLA", false, onDelete = null) { startNew(s.n) } }
                    }
                    else -> {
                        Text("NASIL OYNANIR", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        listOf(
                            "🏟" to "Bir kulüp seç. Yönetimin sezon hedefi ve transfer bütçesi kulübe göre değişir.",
                            "▶" to "Sağ üstteki DEVAM ET ile günler ilerler; maç günü maça çıkarsın.",
                            "📋" to "Taktik ekranında dizilişi, oyuncu rollerini ve takım talimatlarını belirle.",
                            "💶" to "Transfer döneminde oyuncu al-sat, gözlemci gönder, sözleşmeleri yönet.",
                            "📈" to "Gençleri geliştir, tesisleri büyüt, yönetimin ve taraftarın güvenini koru.",
                        ).forEach { (i, t) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(i, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                                Text(t, fontSize = 14.sp)
                            }
                        }
                    }
                }
                if (error) Text("Kayıt dosyası okunamadı.", color = Red)
            }
        }
    }
}

@Composable
private fun MenuButton(icon: String, title: String, sub: String?, primary: Boolean = false, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (primary) Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green))
    else Brush.horizontalGradient(listOf(Color(0xE6182046), Color(0xE6101734)))
    Row(
        Modifier.fillMaxWidth().height(if (sub != null) 52.dp else 44.dp).clip(shape).background(bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) Green else Green.copy(alpha = if (primary) 0f else 0.25f), shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (primary) Color.Black else TextMain
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (primary) Color.Black.copy(alpha = 0.15f) else Green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text(icon, color = if (primary) Color.Black else Green, fontSize = 16.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = fg, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
            if (sub != null) Text(sub, color = if (primary) Color.Black.copy(alpha = 0.7f) else TextDim, fontSize = 11.sp, maxLines = 1)
        }
        Text("›", color = fg, fontSize = 24.sp)
    }
}

@Composable
private fun SlotBadge(s: GameRepo.SlotInfo, size: Dp) {
    val c = if (s.exists) Color(s.color) else Surface2
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.verticalGradient(listOf(c, c.copy(alpha = 0.6f))))
            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val text = if (s.exists) s.team.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase() else "${s.n}"
        Text(text, color = if (s.exists) contentOn(c) else TextDim, fontWeight = FontWeight.Black, fontSize = (size.value * 0.32f).sp)
    }
}

@Composable
private fun SlotRow(s: GameRepo.SlotInfo, action: String, confirmDelete: Boolean, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF151D3D))
            .border(1.dp, if (s.exists) Color(s.color).copy(alpha = 0.7f) else Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlotBadge(s, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (s.exists) {
                Text(s.team, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(listOf(s.manager, s.detail).filter { it.isNotEmpty() }.joinToString(" · "), color = TextDim, fontSize = 11.sp, maxLines = 2)
            } else {
                Text("Yuva ${s.n} · boş", color = TextDim, fontSize = 15.sp)
            }
        }
        if (s.exists && onDelete != null) {
            TextButton(onClick = onDelete) { Text(if (confirmDelete) "Sil?" else "🗑", color = if (confirmDelete) Red else TextDim, fontWeight = FontWeight.Bold) }
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(if (s.exists && action == "YÜKLE") Green else Color.Transparent)
                .border(1.dp, Green, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        ) { Text(action, color = if (s.exists && action == "YÜKLE") Color.Black else Green, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}
