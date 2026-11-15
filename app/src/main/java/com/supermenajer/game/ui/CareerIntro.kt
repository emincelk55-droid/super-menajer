package com.supermenajer.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game

private class TourPage(val icon: String, val title: String, val text: String)

private fun tour(g: Game) = listOf(
    TourPage("👋", L("Hoş geldin hocam!", "Welcome, boss!"), L("Menajerlik kariyerine başlamaya hazırsın. ${g.userTeam.name} yönetimi senden bu sezon şunu bekliyor: ${Season.objectiveText(g.userTeam)}.", "You're ready to start your managerial career. The ${g.userTeam.name} board expects this from you this season: ${Season.objectiveText(g.userTeam)}.")),
    TourPage("▶", L("Devam Et", "Continue"), L("Sağ üstteki yeşil DEVAM ET düğmesi günleri ilerletir. Transfer teklifi, basın toplantısı, sakatlık gibi önemli bir olay olunca kendiliğinden durur; maç günü seni maça götürür.", "The green CONTINUE button at the top right moves the days forward. It stops by itself when something important happens, like a transfer offer, press conference or injury; on match day it takes you to the match.")),
    TourPage("☰", L("Menü ve kartlar", "Menu and cards"), L("Sol üstteki ☰ menüden Kadro, Taktik, Transfer, Lig ve Kulüp bölümlerine gidersin. Ana sayfadaki kartlara dokunmak da seni ilgili bölüme götürür.", "Use the ☰ menu at the top left to reach Squad, Tactics, Transfers, League and Club. Tapping the cards on the home screen also takes you to the related section.")),
    TourPage("📋", L("Taktik", "Tactics"), L("Taktik ekranında oyuncuları sahaya sürükle, dizilişini seç, talimatları ve rolleri belirle. Taktik Oluştur ile kadrona uygun bir oyun tarzı seçebilirsin.", "On the tactics screen drag players onto the pitch, pick your formation and set instructions and roles. With Build Tactic you can choose a playing style that suits your squad.")),
    TourPage("🔁", L("Transfer", "Transfers"), L("Transfer döneminde oyuncu ara, gözlemci gönder, ⭐ kısa listene ekle ve ⚖ ile kendi oyuncularınla karşılaştır. Bütçen: ${money(g.userTeam.budget)}.", "During the transfer window search for players, send scouts, add them to your ⭐ shortlist and compare them with your own players using ⚖. Budget: ${money(g.userTeam.budget)}.")),
    TourPage("🏛", L("Yönetimin güveni", "Board confidence"), L("Üst çubuktaki 🏛 göstergesi yönetimin sana güvenidir. Sıfıra inerse kovulursun. Hedefe ulaş, taraftarı ve soyunma odasını mutlu tut. Bol şans!", "The 🏛 indicator on the top bar is the board's confidence in you. If it hits zero you're sacked. Reach your objective and keep the fans and the dressing room happy. Good luck!")),
)

/** Kariyerin ilk açılışı: "göreve getirildi" haberi ve 6 sayfalık kısa rehber. */
@Composable
fun CareerIntro(g: Game) {
    var page by remember { mutableIntStateOf(0) }
    val pages = tour(g)
    fun finish() { g.introDone = true; GameRepo.changed() }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        if (page == 0) NewsFlash(g) { page = 1 }
        else {
            val p = pages[page - 1]
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(CardTop, CardBottom))).border(1.5.dp, Teal.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Teal.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(p.icon, fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp))
                    Text(p.title, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Text("${page}/${pages.size}", color = TextDim, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(p.text, fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L("Rehberi geç", "Skip guide"), color = TextDim, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { finish() }.padding(8.dp))
                    Spacer(Modifier.weight(1f))
                    if (page > 1) IntroButton(L("‹ Önceki", "‹ Previous"), false) { page-- }
                    IntroButton(if (page == pages.size) L("Başla ▶", "Start ▶") else L("Sonraki ›", "Next ›"), true) { if (page == pages.size) finish() else page++ }
                }
            }
        }
    }
}

@Composable
private fun IntroButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(42.dp).pressClick(0.95f, onClick = onClick).clip(RoundedCornerShape(10.dp))
            .background(if (primary) Brush.horizontalGradient(listOf(Color(0xFF34F5C0), Color(0xFF12C795))) else Brush.horizontalGradient(listOf(Color(0xFF1B2452), Color(0xFF111735))))
            .border(1.dp, if (primary) Color.White.copy(alpha = 0.4f) else CardLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontWeight = FontWeight.Black, color = if (primary) Color(0xFF06231A) else TextMain) }
}

/** TV haber bülteni: stüdyo duvarı, menajer silueti ve "son dakika" alt bandı. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NewsFlash(g: Game, onNext: () -> Unit) {
    val t = g.userTeam
    val c1 = Color(t.color1)
    val c2 = Color(t.color2)
    val blink by rememberInfiniteTransition(label = "b").animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth(0.88f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF070A1C))
            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).clickable(onClick = onNext),
    ) {
        // Stüdyo: kulüp renklerinde döşeme ve ortada menajer
        Box(Modifier.fillMaxWidth().height(210.dp)) {
            Column(Modifier.fillMaxSize()) {
                repeat(4) { r ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(6) { k ->
                            val alt = (r + k) % 2 == 0
                            Box(
                                Modifier.weight(1f).fillMaxSize().padding(1.dp).background(if (alt) c1.copy(alpha = 0.85f) else c2.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (alt) "SM" else t.short, color = (if (alt) contentOn(c1) else contentOn(c2)).copy(alpha = 0.6f), fontWeight = FontWeight.Black, fontSize = 13.sp) }
                        }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.Center).size(130.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A2F45), Color(0xFF0B0E1C)))).border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(g.managerName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(), fontWeight = FontWeight.Black, fontSize = 40.sp)
                    Text(Generator.flag(g.managerNation), fontSize = 20.sp)
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).background(Gold).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(L("SON DAKİKA", "BREAKING NEWS"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.alpha(blink))
            }
            Box(Modifier.align(Alignment.TopStart).padding(10.dp)) { TeamBadge(t, 48.dp) }
        }
        // Alt bant
        Box(Modifier.fillMaxWidth().background(Gold).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                L("${t.name.uppercase()} MENAJER OLARAK ${g.managerName.uppercase()}'İ GÖREVE GETİRDİ", "${t.name.uppercase()} APPOINT ${g.managerName.uppercase()} AS MANAGER"),
                color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF0B1028)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCrest(26.dp)
            Spacer(Modifier.width(8.dp))
            Text(L("Yönetimin hedefi: ${Season.objectiveText(t)} · Bütçe ${money(t.budget)}", "Board objective: ${Season.objectiveText(t)} · Budget ${money(t.budget)}"), color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(L("Devam ›", "Continue ›"), color = Teal, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
        }
    }
}
