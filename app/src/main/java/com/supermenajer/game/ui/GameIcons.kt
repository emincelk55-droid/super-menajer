package com.supermenajer.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stadium
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Oyun ikonları: Material Symbols (Apache 2.0, androidx material-icons-extended). Emojiler telefondan
 * telefona farklı görünür; arayüzdeki sabit ikonlar her cihazda aynı duran vektör ikonlara dönüşür.
 * Eşleşmeyen emoji olduğu gibi yazılır.
 */
private val iconMap: Map<String, ImageVector> = mapOf(
    "☰" to Icons.Rounded.Menu,
    "‹" to Icons.Rounded.ChevronLeft,
    "💶" to Icons.Rounded.Payments,
    "€" to Icons.Rounded.Payments,
    "👥" to Icons.Rounded.Groups,
    "🏛" to Icons.Rounded.AccountBalance,
    "📅" to Icons.Rounded.CalendarMonth,
    "⚙" to Icons.Rounded.Settings,
    "⚙️" to Icons.Rounded.Settings,
    "🏋" to Icons.Rounded.FitnessCenter,
    "🎯" to Icons.Rounded.TrackChanges,
    "🔭" to Icons.Rounded.PersonSearch,
    "🏆" to Icons.Rounded.EmojiEvents,
    "🏟" to Icons.Rounded.Stadium,
    "📋" to Icons.AutoMirrored.Rounded.Assignment,
    "▶" to Icons.Rounded.PlayArrow,
    "⚡" to Icons.Rounded.Bolt,
    "⏩" to Icons.Rounded.FastForward,
    "🤝" to Icons.Rounded.Handshake,
    "⏭" to Icons.Rounded.SkipNext,
    "🪄" to Icons.Rounded.AutoFixHigh,
    "🏠" to Icons.Rounded.Home,
    "✉" to Icons.Rounded.Mail,
    "🔁" to Icons.Rounded.SwapHoriz,
    "🌍" to Icons.Rounded.Public,
    "❓" to Icons.AutoMirrored.Rounded.HelpOutline,
    "⏸" to Icons.Rounded.Pause,
    "＋" to Icons.Rounded.Add,
    "📂" to Icons.Rounded.FolderOpen,
    "✕" to Icons.Rounded.Close,
    "🎤" to Icons.Rounded.Mic,
    "📰" to Icons.Rounded.Newspaper,
    "🚑" to Icons.Rounded.MedicalServices,
    "💬" to Icons.Rounded.Forum,
    "🎖" to Icons.Rounded.MilitaryTech,
    "🛡" to Icons.Rounded.Shield,
    "🎨" to Icons.Rounded.Palette,
    "👕" to Icons.Rounded.Checkroom,
    "⭐" to Icons.Rounded.Star,
    "📈" to Icons.Rounded.TrendingUp,
    "🌱" to Icons.Rounded.Spa,
    "🔥" to Icons.Rounded.LocalFireDepartment,
    "🏅" to Icons.Rounded.WorkspacePremium,
    "💰" to Icons.Rounded.Savings,
    "⚖" to Icons.Rounded.Balance,
    "🧠" to Icons.Rounded.Psychology,
    "🤖" to Icons.Rounded.SmartToy,
    "🎁" to Icons.Rounded.Redeem,
    "📝" to Icons.Rounded.EditNote,
    "📺" to Icons.Rounded.Tv,
    "🔍" to Icons.Rounded.Search,
    "📊" to Icons.Rounded.BarChart,
    "💼" to Icons.Rounded.Work,
    "🗣" to Icons.Rounded.RecordVoiceOver,
    "📣" to Icons.Rounded.Campaign,
    "📌" to Icons.Rounded.PushPin,
    "👤" to Icons.Rounded.Person,
    "👑" to Icons.Rounded.WorkspacePremium,
    "⚽" to Icons.Rounded.SportsSoccer,
)

fun gameIcon(key: String): ImageVector? = iconMap[key.trim()] ?: iconMap[key.trim().removeSuffix("️")]

/** Emoji anahtarına karşılık vektör ikon (yoksa emojinin kendisi). */
@Composable
fun GIcon(key: String, size: Dp, tint: Color = TextMain, modifier: Modifier = Modifier) {
    val v = gameIcon(key)
    if (v != null) Icon(v, contentDescription = null, tint = tint, modifier = modifier.size(size))
    else Text(key, fontSize = (size.value * 0.85f).sp, color = tint, modifier = modifier)
}

/** "👥  Takım" gibi emoji önekli etiketi ikon + yazı olarak çizer. */
@Composable
fun IconLabel(label: String, color: Color, fontSize: TextUnit, weight: FontWeight = FontWeight.Bold, iconTint: Color = color) {
    val first = label.trimStart().substringBefore(' ')
    val rest = label.trimStart().substringAfter(' ', "").trimStart()
    if (rest.isNotEmpty() && gameIcon(first) != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GIcon(first, (fontSize.value * 1.25f).dp, iconTint)
            Text(rest, fontWeight = weight, fontSize = fontSize, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else Text(label, fontWeight = weight, fontSize = fontSize, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Kart başlığı: baştaki emoji tanınırsa ikon olarak çizilir. */
@Composable
fun CardTitle(title: String, color: Color, fontSize: TextUnit, weight: FontWeight, iconTint: Color, modifier: Modifier = Modifier, letterSpacing: TextUnit = TextUnit.Unspecified, upper: Boolean = false) {
    val first = title.trimStart().substringBefore(' ')
    val rest = title.trimStart().substringAfter(' ', "").trimStart()
    val icon = if (rest.isNotEmpty()) gameIcon(first) else null
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size((fontSize.value * 1.3f).dp))
        Text(
            (if (icon != null) rest else title).let { if (upper) it.uppercase() else it },
            fontWeight = weight, fontSize = fontSize, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = letterSpacing,
        )
    }
}
