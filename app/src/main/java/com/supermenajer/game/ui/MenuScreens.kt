package com.supermenajer.game.ui

import androidx.compose.ui.graphics.drawscope.withTransform

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
import androidx.compose.ui.draw.shadow
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

const val APP_VERSION = "v2.15"

private val tips get() = listOf(
    L("İpucu: DEVAM düğmesi günleri tek tek geçirir, önemli bir olayda durur.", "Tip: The CONTINUE button moves day by day and stops on important events."),
    L("İpucu: Oyuncuları taktik ekranında sürükleyerek yerlerini değiştirebilirsin.", "Tip: Drag players on the tactics screen to swap their positions."),
    L("İpucu: Gözlemci raporu olmadan oyuncunun gerçek gücünü bilemezsin.", "Tip: Without a scout report you can't know a player's real ability."),
    L("İpucu: Genç oyuncuları kiralık göndermek gelişimlerini hızlandırır.", "Tip: Loaning out young players speeds up their development."),
    L("İpucu: Yönetimin güveni sıfıra inerse kovulursun. Hedefi takip et!", "Tip: If the board's confidence drops to zero you're sacked. Keep an eye on the objective!"),
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

/** Oyun logosu: uygulama ikonuyla aynı katmanlar (bkz. LogoArt, tools/gen_logo.py). */
@Composable
fun GameCrest(size: Dp) {
    val paths = androidx.compose.runtime.remember {
        LogoArt.layers.map { l ->
            val d = when (l) {
                is LogoLayer.Fill -> l.d
                is LogoLayer.Gradient -> l.d
                is LogoLayer.Stroke -> l.d
                is LogoLayer.Clip -> l.d
                LogoLayer.EndClip -> ""
            }
            if (d.isEmpty()) null else androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath()
        }
    }
    Canvas(Modifier.size(size)) {
        val b = LogoArt.bounds
        val s = minOf(this.size.width / b[2], this.size.height / b[3])
        val ox = (this.size.width - b[2] * s) / 2 - b[0] * s
        val oy = (this.size.height - b[3] * s) / 2 - b[1] * s
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
            var clipDepth = 0
            LogoArt.layers.forEachIndexed { i, l ->
                val path = paths[i]
                when (l) {
                    is LogoLayer.Fill -> drawPath(path!!, Color(l.color))
                    is LogoLayer.Gradient -> drawPath(path!!, Brush.linearGradient(*l.stops.map { it.first to Color(it.second) }.toTypedArray(), start = Offset(l.x1, l.y1), end = Offset(l.x2, l.y2)))
                    is LogoLayer.Stroke -> drawPath(path!!, Color(l.color), style = Stroke(0.9f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    is LogoLayer.Clip -> { drawContext.canvas.save(); drawContext.canvas.clipPath(path!!); clipDepth++ }
                    LogoLayer.EndClip -> if (clipDepth > 0) { drawContext.canvas.restore(); clipDepth-- }
                }
            }
        }
    }
}

@Composable
private fun Wordmark(big: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { BrandWordmark(if (big) 40.sp else 26.sp) }
}

/** Marka yazısı: SÜPER altın, MENAJER zümrüt degrade, hafif parıltılı. Ana menü ve mağaza görselinde aynı. */
@Composable
fun BrandWordmark(size: androidx.compose.ui.unit.TextUnit, align: Alignment.Horizontal = Alignment.CenterHorizontally) {
    Column(horizontalAlignment = align) {
        Text(
            L("SÜPER", "SUPER"), fontWeight = FontWeight.Black, letterSpacing = 8.sp, lineHeight = size * 1.05f, fontSize = size,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFF6CC), Color(0xFFE2B33C), Color(0xFFB8891A))),
                shadow = androidx.compose.ui.graphics.Shadow(Color(0xFFE2B33C).copy(alpha = 0.45f), Offset(0f, 0f), 18f),
            ),
        )
        Text(
            L("MENAJER", "MANAGER"), fontWeight = FontWeight.Black, letterSpacing = 8.sp, lineHeight = size * 1.05f, fontSize = size,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFF7CFFD8), Green, Color(0xFF0B9F78))),
                shadow = androidx.compose.ui.graphics.Shadow(Green.copy(alpha = 0.5f), Offset(0f, 0f), 20f),
            ),
        )
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
        progress.value < 0.25f -> L("Ligler yükleniyor...", "Loading leagues...")
        progress.value < 0.5f -> L("6.500 oyuncu hazırlanıyor...", "Preparing 6,500 players...")
        progress.value < 0.75f -> L("Stadyumlar açılıyor...", "Opening the stadiums...")
        progress.value < 1f -> L("Fikstür çekiliyor...", "Drawing the fixtures...")
        else -> L("Hazır!", "Ready!")
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
            Text(L("FUTBOL MENAJERLİK OYUNU · 2026/27", "FOOTBALL MANAGEMENT GAME · 2026/27"), color = TextDim, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.width(360.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                Box(
                    Modifier.fillMaxWidth(progress.value).fillMaxHeight().clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green, Color(0xFF7DFFD8)))),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (done) {
                Text(L("▶  BAŞLAMAK İÇİN DOKUN", "▶  TAP TO START"), color = Green, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp, modifier = Modifier.alpha(pulse))
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
                        Text(L("SÜPER MENAJER", "SUPER MANAGER"), fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
                        Text(L("$APP_VERSION · 17 lig · 12 ülke", "$APP_VERSION · 17 leagues · 12 countries"), color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (last != null) {
                    MenuButton("▶", L("DEVAM ET", "CONTINUE"), "${last.team} · ${last.detail.substringAfterLast(" · ")}", primary = true, enterDelay = 80) { load(last.n) }
                    Spacer(Modifier.height(6.dp))
                }
                MenuButton("＋", L("YENİ KARİYER", "NEW CAREER"), L("Kulübünü seç, efsaneni yaz", "Pick your club, write your legend"), selected = panel == 2, enterDelay = 140) { panel = 2 }
                Spacer(Modifier.height(6.dp))
                MenuButton("📂", L("KARİYER YÜKLE", "LOAD CAREER"), L("${slots.count { it.exists }}/3 kayıt", "${slots.count { it.exists }}/3 saves"), selected = panel == 1, enterDelay = 200) { panel = 1 }
                Spacer(Modifier.height(6.dp))
                MenuButton("👥", L("İKİ KİŞİLİK MAÇ", "TWO-PLAYER MATCH"), L("Aynı telefonda arkadaşınla oyna", "Play a friend on the same phone"), selected = panel == 4, enterDelay = 260) { panel = 4 }
                Spacer(Modifier.height(6.dp))
                MenuButton("❓", L("NASIL OYNANIR", "HOW TO PLAY"), L("Kısa rehber", "Quick guide"), selected = panel == 3, enterDelay = 320) { panel = 3 }
                Spacer(Modifier.height(6.dp))
                MenuButton("✕", L("ÇIKIŞ", "QUIT"), null, enterDelay = 380) { (ctx as? android.app.Activity)?.finishAndRemoveTask() }
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
                        Text(L("SON KARİYER", "LAST CAREER"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlotBadge(last, 74.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(last.team, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1)
                                if (last.manager.isNotEmpty()) Text(L("Menajer: ${last.manager}", "Manager: ${last.manager}"), color = TextDim, fontSize = 13.sp)
                                Text(last.detail, color = TextDim, fontSize = 13.sp)
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green))).clickable { load(last.n) },
                            contentAlignment = Alignment.Center,
                        ) { Text(L("KALDIĞIN YERDEN DEVAM ET  ▶", "CONTINUE WHERE YOU LEFT OFF  ▶"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                        Text(L("Yeni bir kulüple başlamak için YENİ KARİYER'i, diğer kayıtların için KARİYER YÜKLE'yi seç.", "Choose NEW CAREER to start with a new club, or LOAD CAREER for your other saves."), color = TextDim, fontSize = 12.sp)
                    }
                    1 -> {
                        Text(L("KAYIT YUVALARI", "SAVE SLOTS"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        slots.forEach { s ->
                            SlotRow(s, if (s.exists) L("YÜKLE", "LOAD") else L("YENİ", "NEW"), confirmDelete == s.n,
                                onDelete = { if (confirmDelete == s.n) { GameRepo.deleteSlot(s.n); confirmDelete = 0 } else confirmDelete = s.n },
                            ) { if (s.exists) load(s.n) else startNew(s.n) }
                        }
                        BackupPanel(slots)
                    }
                    2 -> {
                        Text(L("YENİ KARİYER · YUVA SEÇ", "NEW CAREER · CHOOSE SLOT"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(L("Dolu bir yuvayı seçersen eski kariyer silinir.", "Choosing a used slot deletes the old career."), color = TextDim, fontSize = 12.sp)
                        slots.forEach { s -> SlotRow(s, if (s.exists) L("ÜZERİNE YAZ", "OVERWRITE") else L("BAŞLA", "START"), false, onDelete = null) { startNew(s.n) } }
                    }
                    4 -> VersusPanel()
                    else -> {
                        Text(L("NASIL OYNANIR", "HOW TO PLAY"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        listOf(
                            "🏟" to L("Bir kulüp seç. Yönetimin sezon hedefi ve transfer bütçesi kulübe göre değişir.", "Pick a club. The board's season objective and transfer budget depend on the club."),
                            "▶" to L("Sağ üstteki DEVAM ET ile günler ilerler; maç günü maça çıkarsın.", "CONTINUE at the top right moves the days; on match day you play the match."),
                            "📋" to L("Taktik ekranında dizilişi, oyuncu rollerini ve takım talimatlarını belirle.", "Set your formation, player roles and team instructions on the tactics screen."),
                            "💶" to L("Transfer döneminde oyuncu al-sat, gözlemci gönder, sözleşmeleri yönet.", "Buy and sell players, send scouts and manage contracts in the transfer window."),
                            "📈" to L("Gençleri geliştir, tesisleri büyüt, yönetimin ve taraftarın güvenini koru.", "Develop youngsters, grow your facilities and keep the board's and fans' trust."),
                        ).forEach { (i, t) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(i, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                                Text(t, fontSize = 14.sp)
                            }
                        }
                    }
                }
                if (error) Text(L("Kayıt dosyası okunamadı.", "The save file couldn't be read."), color = Red)
            }
        }
    }
}

@Composable
private fun MenuButton(icon: String, title: String, sub: String?, primary: Boolean = false, selected: Boolean = false, enterDelay: Int = 0, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (primary) Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green))
    else Brush.horizontalGradient(listOf(Color(0xE6182046), Color(0xE6101734)))
    Row(
        Modifier.appear(enterDelay, offsetDp = 22f).fillMaxWidth().height(if (sub != null) 52.dp else 44.dp).pressClick(0.96f, onClick = onClick)
            .then(if (primary) Modifier.shadow(10.dp, shape, ambientColor = Green, spotColor = Green) else Modifier)
            .clip(shape).background(bg).then(if (primary) Modifier.shine() else Modifier)
            .border(if (selected) 2.dp else 1.dp, if (selected) Green else Green.copy(alpha = if (primary) 0f else 0.25f), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (primary) Color.Black else TextMain
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (primary) Color.Black.copy(alpha = 0.15f) else Green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { GIcon(icon, 20.dp, if (primary) Color.Black else Green) }
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
                Text(L("Yuva ${s.n} · boş", "Slot ${s.n} · empty"), color = TextDim, fontSize = 15.sp)
            }
        }
        if (s.exists && onDelete != null) {
            TextButton(onClick = onDelete) { Text(if (confirmDelete) L("Sil?", "Delete?") else "🗑", color = if (confirmDelete) Red else TextDim, fontWeight = FontWeight.Bold) }
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(if (s.exists && action == L("YÜKLE", "LOAD")) Green else Color.Transparent)
                .border(1.dp, Green, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        ) { Text(action, color = if (s.exists && action == L("YÜKLE", "LOAD")) Color.Black else Green, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}


/** Arka planda uzun işlem sürerken: dokunuşları engelleyen yarı saydam katman. */
@Composable
fun BusyOverlay(msg: String) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xEE10163A)).border(1.dp, Teal.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = Teal, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(msg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}


/** İki kişilik maç: iki oyuncu birer takım seçer; maçta ikisi de kendi takımını yönetir. */
@Composable
private fun VersusPanel() {
    var world by remember { mutableStateOf<com.supermenajer.game.model.Game?>(GameRepo.preparedWorld) }
    LaunchedEffect(Unit) {
        if (world == null) world = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { GameRepo.awaitWorld() }
    }
    Text(L("İKİ KİŞİLİK MAÇ", "TWO-PLAYER MATCH"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Text(L("Telefonu aranızda paylaşın: 1. oyuncu ev sahibi takımı, 2. oyuncu deplasman takımını yönetir (mentalite ve oyuncu değişikliği). Kariyere etkisi yoktur.", "Share the phone: player 1 controls the home team, player 2 the away team (mentality and substitutions). It doesn't affect your career."), color = TextDim, fontSize = 12.sp)
    val w = world
    if (w == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(color = Teal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(L("Takımlar yükleniyor...", "Loading teams..."), color = TextDim)
        }
        return
    }
    val leagues = com.supermenajer.game.model.Leagues.topLeagues
    var l1 by remember { mutableIntStateOf(0) }
    var l2 by remember { mutableIntStateOf(0) }
    val teams1 = remember(l1) { w.divisionTeams(leagues[l1].id).sortedByDescending { com.supermenajer.game.game.Lineups.strength(w, it) } }
    val teams2 = remember(l2) { w.divisionTeams(leagues[l2].id).sortedByDescending { com.supermenajer.game.game.Lineups.strength(w, it) } }
    var t1 by remember(l1) { mutableIntStateOf(0) }
    var t2 by remember(l2) { mutableIntStateOf(if (l1 == l2) 1 else 0) }
    for ((k, pair) in listOf((l1 to t1) to teams1, (l2 to t2) to teams2).withIndex()) {
        val (sel, list) = pair
        val team = list.getOrNull(sel.second)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF151D3D)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(L("${k + 1}. OYUNCU", "PLAYER ${k + 1}"), fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (k == 0) Teal else Gold, modifier = Modifier.width(86.dp))
            if (team != null) TeamBadge(team, 34.dp)
            Spacer(Modifier.width(8.dp))
            SmDropdown(leagues[sel.first].name, leagues.map { it.name }, Modifier.weight(1f)) { if (k == 0) l1 = it else l2 = it }
            Spacer(Modifier.width(6.dp))
            SmDropdown(team?.name ?: "-", list.map { "${it.name} (${com.supermenajer.game.game.Lineups.strength(w, it).toInt()})" }, Modifier.weight(1.3f)) { if (k == 0) t1 = it else t2 = it }
        }
    }
    val a = teams1.getOrNull(t1)
    val b = teams2.getOrNull(t2)
    val ok = a != null && b != null && a.id != b.id
    Box(
        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
            .background(if (ok) Brush.horizontalGradient(listOf(Color(0xFF0FB98A), Green)) else Brush.horizontalGradient(listOf(Surface2, Surface2)))
            .clickable(enabled = ok) { GameRepo.takePreparedWorld(); GameRepo.startVersus(w, a!!.id, b!!.id) },
        contentAlignment = Alignment.Center,
    ) { Text(if (ok) L("MAÇA BAŞLA  ▶", "KICK OFF  ▶") else L("İki farklı takım seçin", "Pick two different teams"), color = if (ok) Color.Black else TextDim, fontWeight = FontWeight.Black, fontSize = 15.sp) }
}


/**
 * Kayıt yedekleme: her yuva dosyaya aktarılır (Drive, WhatsApp, telefon) ya da dosyadan geri yüklenir.
 * Telefon değişince ya da oyun silinince kariyer kaybolmasın.
 */
@Composable
private fun BackupPanel(slots: List<GameRepo.SlotInfo>) {
    // Dosya seçici yalnız gerçek ekranda var (önizleme ve testlerde yok)
    if (androidx.activity.compose.LocalActivityResultRegistryOwner.current == null) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var target by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    val exporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) msg = runCatching {
            val bytes = GameRepo.exportSlot(target) ?: error("empty")
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            L("✓ Yuva $target dışa aktarıldı.", "✓ Slot $target exported.")
        }.getOrElse { L("Dışa aktarılamadı.", "Export failed.") }
    }
    val importer = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            msg = GameRepo.importSlot(target, bytes) ?: L("✓ Yedek Yuva $target'e yüklendi.", "✓ Backup restored to slot $target.")
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(L("💾 YEDEKLE / GERİ YÜKLE", "💾 BACKUP / RESTORE"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Text(
        L("Kariyerini dosyaya kaydet (Drive, WhatsApp, telefon). Telefon değişirse dosyayı içe aktararak devam et.",
            "Save your career to a file (Drive, WhatsApp, phone). If you change phones, import the file to continue."),
        color = TextDim, fontSize = 11.sp, lineHeight = 14.sp,
    )
    slots.forEach { s ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(L("Yuva ${s.n}", "Slot ${s.n}"), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
            if (s.exists) Text(
                L("⬆ Dışa aktar", "⬆ Export"), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Green.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { target = s.n; exporter.launch(GameRepo.exportName(s.n)) }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Text(
                L("⬇ İçe aktar", "⬇ Import"), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { target = s.n; importer.launch(arrayOf("*/*")) }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    msg?.let { Text(it, color = Gold, fontSize = 12.sp) }
}
