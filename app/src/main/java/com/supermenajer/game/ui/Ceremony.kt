package com.supermenajer.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Awards
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.model.Game

/* ---------- Kupa töreni ve sezon sonu ödül gecesi ---------- */

/**
 * Yeni kazanılan kupa için tam ekran tören: sahne ışıkları, yükselen altın kupa, konfeti.
 * Her kupa bir kez kutlanır ([Game.ceremonySeen]).
 */
@Composable
fun TrophyCeremonyIfNeeded(g: Game) {
    if (g.ceremonySeen < 0) g.ceremonySeen = g.trophies.size
    val idx = g.ceremonySeen
    if (idx >= g.trophies.size) return
    TrophyCeremony(g, g.trophies[idx]) {
        g.ceremonySeen = idx + 1
        GameRepo.save(force = true)
        GameRepo.bump()
    }
}

@Composable
fun TrophyCeremony(g: Game, trophy: String, onClose: () -> Unit) {
    val c1 = Color(g.userTeam.color1)
    val c2 = Color(g.userTeam.color2)
    val still = fxStill()
    val rise = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 60f)) }
    // Konfeti her 2,8 sn'de yeniden patlar
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (!still) while (true) { kotlinx.coroutines.delay(2800); burst++ } }
    val sweep = if (still) 0.3f else rememberInfiniteTransition(label = "stage").animateFloat(
        0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "sweep",
    ).value
    val none = remember { MutableInteractionSource() }
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF05060F), Color(0xFF14102E), Color(0xFF05060F))))
            .clickable(interactionSource = none, indication = null) {},
    ) {
        // Sahne ışıkları
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (i in 0..2) {
                val sx = w * (0.2f + i * 0.3f)
                val tx = w * 0.5f + kotlin.math.sin((sweep + i * 0.33f) * 6.283f) * w * 0.18f
                val beam = Path().apply { moveTo(sx - w * 0.01f, 0f); lineTo(sx + w * 0.01f, 0f); lineTo(tx + w * 0.12f, h); lineTo(tx - w * 0.12f, h); close() }
                drawPath(beam, Brush.verticalGradient(listOf(Color(0xFFFFF1C2).copy(alpha = 0.22f), Color.Transparent)))
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFD4AF37).copy(alpha = 0.35f), Color.Transparent), Offset(w / 2, h * 0.45f), h * 0.55f), h * 0.55f, Offset(w / 2, h * 0.45f))
        }
        key(burst) { GoalCelebration(c1, c2, confetti = true, seed = 1000 + burst, showText = false) }
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("ŞAMPİYON!", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 8.sp,
                modifier = Modifier.graphicsLayer { alpha = rise.value.coerceIn(0f, 1f) })
            Canvas(
                Modifier.size(150.dp).graphicsLayer {
                    val r = rise.value
                    translationY = (1f - r) * 260f
                    scaleX = 0.6f + 0.4f * r; scaleY = 0.6f + 0.4f * r
                },
            ) { drawTrophy() }
            Text(
                trophy, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 4f), 12f)),
            )
            Text("${g.userTeam.name} · ${g.managerName}", color = TextDim, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            SmTrophyButton("KUPAYI KALDIR  🏆", onClose)
        }
    }
}

@Composable
private fun SmTrophyButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.pressClick(0.95f, onClick = onClick).shadow(12.dp, shape, ambientColor = Gold, spotColor = Gold).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFF7E08A), Gold, Color(0xFFB8891A)))).shine()
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) { Text(label, color = Color(0xFF2A1E00), fontWeight = FontWeight.Black, fontSize = 16.sp) }
}

/** Altın kupa: iki kulplu gövde, ayak ve kaide, parlaklık. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrophy() {
    val w = size.width
    val h = size.height
    val gold = Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFD4AF37), Color(0xFF8C6A10)), Offset(0f, 0f), Offset(w, h))
    // Kulplar
    val st = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.06f)
    drawArc(gold, 90f, 180f, false, Offset(w * 0.12f, h * 0.12f), Size(w * 0.26f, h * 0.3f), style = st)
    drawArc(gold, -90f, 180f, false, Offset(w * 0.62f, h * 0.12f), Size(w * 0.26f, h * 0.3f), style = st)
    // Gövde
    val cup = Path().apply {
        moveTo(w * 0.25f, h * 0.08f); lineTo(w * 0.75f, h * 0.08f)
        quadraticTo(w * 0.74f, h * 0.48f, w * 0.54f, h * 0.56f)
        lineTo(w * 0.46f, h * 0.56f)
        quadraticTo(w * 0.26f, h * 0.48f, w * 0.25f, h * 0.08f); close()
    }
    drawPath(cup, gold)
    drawRect(gold, Offset(w * 0.46f, h * 0.55f), Size(w * 0.08f, h * 0.16f))
    drawRect(gold, Offset(w * 0.34f, h * 0.7f), Size(w * 0.32f, h * 0.06f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2A4A), Color(0xFF1A1226))), Offset(w * 0.28f, h * 0.76f), Size(w * 0.44f, h * 0.18f))
    drawRect(gold, Offset(w * 0.36f, h * 0.82f), Size(w * 0.28f, h * 0.05f))
    // Parlama
    drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.34f, h * 0.14f), Offset(w * 0.38f, h * 0.4f), w * 0.03f)
}

/** Sezon sonu ödül gecesi: kartlar sırayla açılır; bizim oyuncumuz kazanırsa altın çerçeve. */
@Composable
fun AwardsNightDialog(g: Game) {
    val awards = remember(g.season) { Awards.season(g) }
    WideDialog(onDismissRequest = { GameRepo.awardsOpen = false }, maxWidth = 980.dp) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF15102E), Color(0xFF07081A))))
                .border(1.dp, Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.3f), Gold, Gold.copy(alpha = 0.3f))), RoundedCornerShape(18.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🏆 ÖDÜL GECESİ", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                    Text("${g.season}-${(g.season + 1) % 100} · ${g.leagueName(g.userTeam)}", color = TextDim, fontSize = 13.sp)
                }
                Text("✕", fontSize = 20.sp, color = TextDim, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { GameRepo.awardsOpen = false }.padding(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(GridCells.Adaptive(290.dp), Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(awards.size) { i -> AwardCard(g, awards[i], Modifier.appear(i * 260, 20f, 0.9f)) }
            }
        }
    }
}

@Composable
private fun AwardCard(g: Game, a: Awards.Award, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(if (a.ours) Brush.linearGradient(listOf(Color(0xFF3A2C10), Color(0xFF1B1530))) else Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(if (a.ours) 2.dp else 1.dp, if (a.ours) Gold else CardLine, shape).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val p = a.player
        if (p != null) {
            val t = g.teamOrNull(p.teamId)
            PlayerFace(p, 54.dp, t?.let { Color(it.color1) } ?: Color.Gray, t?.let { Color(it.color2) } ?: Color.White)
        } else {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(50)).background(Surface2), contentAlignment = Alignment.Center) { Text(a.icon, fontSize = 26.sp) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${a.icon} ${a.title.uppercase()}", color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(p?.name ?: a.teamName, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (p != null) a.teamName else "", color = TextDim, fontSize = 12.sp, maxLines = 1)
            Text(a.stat, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (a.ours) Text("🎉", fontSize = 22.sp)
    }
}

@Composable
private fun key(k: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(k) { content() }
