package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Team

val Bg = Color(0xFF0D1117)
val Surface1 = Color(0xFF161B22)
val Surface2 = Color(0xFF21262D)
val Line = Color(0xFF30363D)
val Green = Color(0xFF2EC27E)
val Gold = Color(0xFFF5C542)
val Red = Color(0xFFE5534B)
val Blue = Color(0xFF58A6FF)
val TextMain = Color(0xFFE6EDF3)
val TextDim = Color(0xFF8B949E)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green, onPrimary = Color.Black,
            secondary = Gold, onSecondary = Color.Black,
            background = Bg, onBackground = TextMain,
            surface = Surface1, onSurface = TextMain,
            surfaceVariant = Surface2, onSurfaceVariant = TextDim,
            error = Red, outline = Line,
        ),
        content = content,
    )
}

fun ovrColor(o: Int): Color = when {
    o >= 80 -> Gold
    o >= 72 -> Green
    o >= 64 -> Color(0xFF7CC4FA)
    o >= 56 -> Color(0xFFB0B8C1)
    else -> Color(0xFF6E7681)
}

fun posColor(p: Pos): Color = when (p) {
    Pos.GK -> Color(0xFFF0883E)
    Pos.DF -> Color(0xFF388BFD)
    Pos.MF -> Color(0xFF2EA043)
    Pos.FW -> Color(0xFFDA3633)
}

fun fitColor(f: Double): Color = when {
    f >= 85 -> Green
    f >= 70 -> Gold
    else -> Red
}

fun ratingColor(r: Double): Color = when {
    r >= 7.5 -> Green
    r >= 6.5 -> Color(0xFF9BE9A8)
    r >= 5.8 -> Gold
    else -> Red
}

fun contentOn(c: Color): Color = if (c.luminance() > 0.5f) Color.Black else Color.White

@Composable
fun OvrBadge(ovr: Int, size: Dp = 34.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(ovrColor(ovr)),
        contentAlignment = Alignment.Center,
    ) {
        Text("$ovr", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = (size.value * 0.42f).sp)
    }
}

/** Oyuncu gücü rozeti: tanımadığımız oyuncularda aralık gösterir (CM tarzı gizli özellik). */
@Composable
fun PlayerOvrBadge(g: com.supermenajer.game.model.Game, p: com.supermenajer.game.model.Player, size: Dp = 34.dp) {
    if (com.supermenajer.game.game.Scouting.known(g, p)) {
        OvrBadge(p.overall, size)
        return
    }
    val est = com.supermenajer.game.game.Scouting.estimate(g, p)
    Box(
        Modifier.size(width = size * 1.45f, height = size).clip(RoundedCornerShape(8.dp)).background(ovrColor(est).copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(com.supermenajer.game.game.Scouting.ovrText(g, p), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = (size.value * 0.32f).sp)
    }
}

/** Oyuncunun detaylı mevkisi (SLB, STP, DOS...). */
@Composable
fun DetChip(p: com.supermenajer.game.model.Player, small: Boolean = false) {
    Text(
        com.supermenajer.game.game.Positions.det(p).code, color = Color.White, fontSize = if (small) 8.sp else 11.sp, fontWeight = FontWeight.Bold,
        lineHeight = if (small) 9.sp else 13.sp,
        modifier = Modifier.background(posColor(p.pos), RoundedCornerShape(4.dp)).padding(horizontal = if (small) 3.dp else 6.dp, vertical = if (small) 0.dp else 2.dp),
    )
}

/** Sahadaki yerin detaylı mevkisi. */
@Composable
fun SlotChip(slot: com.supermenajer.game.model.Slot, small: Boolean = false) {
    Text(
        com.supermenajer.game.game.Positions.slotDet(slot).code, color = Color.White, fontSize = if (small) 8.sp else 11.sp, fontWeight = FontWeight.Bold,
        lineHeight = if (small) 9.sp else 13.sp,
        modifier = Modifier.background(posColor(slot.pos), RoundedCornerShape(4.dp)).padding(horizontal = if (small) 3.dp else 6.dp, vertical = if (small) 0.dp else 2.dp),
    )
}

@Composable
fun PosChip(pos: Pos, small: Boolean = false) {
    Text(
        pos.short, color = Color.White, fontSize = if (small) 8.sp else 11.sp, fontWeight = FontWeight.Bold, lineHeight = if (small) 9.sp else 13.sp,
        modifier = Modifier.background(posColor(pos), RoundedCornerShape(4.dp)).padding(horizontal = if (small) 3.dp else 6.dp, vertical = if (small) 0.dp else 2.dp),
    )
}

@Composable
fun TeamBadge(team: Team, size: Dp = 36.dp) {
    val c1 = Color(team.color1)
    val c2 = Color(team.color2)
    // Arma: kalkan şekli, takım kimliğine göre çizgili / yarım / bantlı desen
    val pattern = (team.id * 31 + team.name.length) % 4
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val shield = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.08f, h * 0.06f)
                lineTo(w * 0.92f, h * 0.06f)
                lineTo(w * 0.92f, h * 0.5f)
                quadraticBezierTo(w * 0.9f, h * 0.85f, w * 0.5f, h * 0.98f)
                quadraticBezierTo(w * 0.1f, h * 0.85f, w * 0.08f, h * 0.5f)
                close()
            }
            clipPath(shield) {
                drawRect(c1)
                when (pattern) {
                    0 -> for (i in 0 until 5) if (i % 2 == 1) drawRect(c2.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w * i / 5f, 0f), androidx.compose.ui.geometry.Size(w / 5f, h))
                    1 -> drawRect(c2.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w / 2f, 0f), androidx.compose.ui.geometry.Size(w / 2f, h))
                    2 -> drawRect(c2.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(0f, h * 0.38f), androidx.compose.ui.geometry.Size(w, h * 0.22f))
                    else -> drawRect(c2.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(w, h * 0.26f))
                }
            }
            drawPath(shield, c2, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.06f))
        }
        Text(
            team.short, color = Color.White, fontSize = (size.value * 0.25f).sp, fontWeight = FontWeight.Black,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(1f, 1f), 3f)),
            modifier = Modifier.padding(bottom = (size.value * 0.06f).dp),
        )
    }
}

@Composable
fun Bar(value: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Box(modifier.height(height).clip(RoundedCornerShape(3.dp)).background(Surface2)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).background(color))
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp),
    ) {
        if (title != null) {
            Text(title.uppercase(), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Green else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.Black else TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FormDots(form: String) {
    androidx.compose.foundation.layout.Row {
        form.forEach { c ->
            val col = when (c) { 'G' -> Green; 'B' -> Gold; else -> Red }
            Box(
                Modifier.padding(end = 3.dp).size(18.dp).clip(RoundedCornerShape(4.dp)).background(col),
                contentAlignment = Alignment.Center,
            ) { Text(c.toString(), fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

fun moraleText(m: Double): String = when {
    m >= 85 -> "Çok mutlu 😄"
    m >= 65 -> "Mutlu 🙂"
    m >= 45 -> "Normal 😐"
    m >= 25 -> "Mutsuz 🙁"
    else -> "Çok mutsuz 😠"
}

/**
 * Yatay ekrana uygun geniş pencere. Platform diyaloğu yerine ekranın üstüne çizilen bir katmandır;
 * böylece genişliği her cihazda aynı olur. Dışına dokunmak ya da geri tuşu kapatır.
 */
@Composable
fun WideDialog(onDismissRequest: () -> Unit, maxWidth: Dp = 600.dp, content: @Composable () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismissRequest)
    val none = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = none, indication = null, onClick = onDismissRequest),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth(0.92f).widthIn(max = maxWidth).padding(vertical = 10.dp)
                .clickable(interactionSource = none, indication = null) {},
        ) { content() }
    }
}
