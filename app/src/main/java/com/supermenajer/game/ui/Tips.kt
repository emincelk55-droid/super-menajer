package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * İlk oynayış rehberi: bir ekran ilk kez açılınca kısa ipucu balonu. Her ipucu bir kez gösterilir
 * (cihazda saklanır, kariyerler arasında tekrar etmez); "İpuçlarını kapat" hepsini susturur.
 */
object Tips {
    class Tip(val title: String, val text: String)

    val all: Map<String, Tip> = linkedMapOf(
        "home" to Tip(L("🏠 Ana sayfa", "🏠 Home"), L("Sağ üstteki DEVAM ET günleri ilerletir; maç günü ya da önemli bir olayda kendiliğinden durur. Karar bekleyen kartlar (basın, sağlık, teklifler) burada en üstte çıkar.", "CONTINUE at the top right moves the days forward and stops by itself on match day or when something important happens. Cards waiting for a decision (press, medical, offers) appear at the top here.")),
        "squad" to Tip(L("👥 Kadro", "👥 Squad"), L("Oyuncuya dokun: profili açılır. Moral, kondisyon ve sözleşmeye göz kulak ol. 🌱 Gelişim sekmesinde gençlerini ve kiralıktakileri takip edersin.", "Tap a player to open his profile. Keep an eye on morale, fitness and contracts. Follow your youngsters and loanees on the 🌱 Development tab.")),
        "tactics" to Tip(L("📋 Taktik", "📋 Tactics"), L("Oyuncuları sürükleyip sahaya bırak. Turuncu/kırmızı işaret mevkisine uymadığını gösterir. Emin değilsen 'Taktik Oluştur' kadrona en uygun oyun tarzını kurar.", "Drag players onto the pitch. An orange/red mark means he doesn't suit the position. If unsure, 'Build Tactic' sets up the style that best fits your squad.")),
        "league" to Tip(L("🏆 Lig", "🏆 League"), L("Puan durumu, fikstür ve takvim burada. 📈 Analiz sekmesi takımının nerede iyi, nerede kötü olduğunu grafiklerle gösterir.", "Table, fixtures and calendar are here. The 📈 Analysis tab shows with charts where your team is good and where it struggles.")),
        "transfer" to Tip(L("💼 Transfer", "💼 Transfers"), L("Filtrele, kısa listeye ekle, gözlemci gönder. Teklifte önce kulüple bonservisi, sonra oyuncuyla maaşı konuşursun. Maaş bütçesini aşma!", "Filter, add to your shortlist, send scouts. In a bid you first agree the fee with the club, then the wage with the player. Don't exceed the wage budget!")),
        "inbox" to Tip(L("✉️ Gelen kutusu", "✉️ Inbox"), L("Yönetimden, oyunculardan ve basından gelen her şey burada. Önemli mesajlar ana sayfaya da düşer.", "Everything from the board, players and press is here. Important messages also appear on the home screen.")),
        "club" to Tip(L("🏟️ Kulüp", "🏟️ Club"), L("Finans, stadyum, tesisler, sponsorlar ve teknik ekip. İyi bir antrenör gençleri hızlı geliştirir; iyi bir gözlemci rakipleri net görür.", "Finances, stadium, facilities, sponsors and staff. A good coach develops youngsters fast; a good scout sees rivals clearly.")),
        "match" to Tip(L("⚽ Maç", "⚽ Match"), L("Alttan hızı (1x/3x/⚡) ve mentaliteyi seç. 📋 Taktik ile maçı durdurup diziliş ve talimat değiştirebilirsin; yardımcı antrenörün sarı çerçeveli önerilerine dikkat et.", "Choose the speed (1x/3x/⚡) and mentality at the bottom. With 📋 Tactics you can pause the match and change formation and instructions; watch for your assistant's yellow-framed suggestions.")),
    )

    /** Ekrandaki ipucu (yoksa null). */
    var current by mutableStateOf<String?>(null)
        private set

    private fun prefs() = Fx.prefsOrNull()

    fun enabled(): Boolean = prefs()?.getBoolean("tipsOn", true) ?: false

    fun seen(key: String): Boolean = prefs()?.getStringSet("tipsSeen", emptySet())?.contains(key) ?: true

    /** Bağlam açıldığında çağrılır: görülmemişse ipucunu gösterir. */
    fun show(key: String) {
        if (current != null || !enabled() || seen(key) || key !in all) return
        current = key
    }

    fun dismiss() {
        val k = current ?: return
        val p = prefs()
        p?.edit()?.putStringSet("tipsSeen", (p.getStringSet("tipsSeen", emptySet()) ?: emptySet()) + k)?.apply()
        current = null
    }

    fun disableAll() {
        prefs()?.edit()?.putBoolean("tipsOn", false)?.apply()
        current = null
    }

    /** Ayarlar'dan ipuçlarını baştan göster. */
    fun reset() {
        prefs()?.edit()?.putBoolean("tipsOn", true)?.putStringSet("tipsSeen", emptySet())?.apply()
    }

    /** Ekran görüntüsü testleri için: ipucunu tercih dosyası olmadan göster. */
    internal fun forceShow(key: String?) { current = key }
}

/** Ekranın altında beliren rehber balonu. */
@Composable
fun TipBubble() {
    val key = Tips.current ?: return
    val tip = Tips.all[key] ?: return
    Box(Modifier.fillMaxSize().padding(bottom = 64.dp, start = 12.dp, end = 12.dp), contentAlignment = Alignment.BottomCenter) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.appear(offsetDp = 30f).widthIn(max = 640.dp).shadow(14.dp, shape).clip(shape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF1E2A66), Color(0xFF141A3A))))
                .border(1.5.dp, Teal, shape).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("💡", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tip.title, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Teal)
                Text(tip.text, fontSize = 13.sp, lineHeight = 18.sp, color = TextMain)
                Text(L("İpuçlarını kapat", "Turn off tips"), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp).clickable { Tips.disableAll() })
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.pressClick(0.94f) { Tips.dismiss() }.clip(RoundedCornerShape(10.dp)).background(Teal).padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(L("Anladım", "Got it"), color = Color(0xFF06231A), fontWeight = FontWeight.Black, fontSize = 13.sp) }
        }
    }
}
