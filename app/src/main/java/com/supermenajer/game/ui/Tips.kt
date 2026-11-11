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
        "home" to Tip("🏠 Ana sayfa", "Sağ üstteki DEVAM ET günleri ilerletir; maç günü ya da önemli bir olayda kendiliğinden durur. Karar bekleyen kartlar (basın, sağlık, teklifler) burada en üstte çıkar."),
        "squad" to Tip("👥 Kadro", "Oyuncuya dokun: profili açılır. Moral, kondisyon ve sözleşmeye göz kulak ol. 🌱 Gelişim sekmesinde gençlerini ve kiralıktakileri takip edersin."),
        "tactics" to Tip("📋 Taktik", "Oyuncuları sürükleyip sahaya bırak. Turuncu/kırmızı işaret mevkisine uymadığını gösterir. Emin değilsen 'Taktik Oluştur' kadrona en uygun oyun tarzını kurar."),
        "league" to Tip("🏆 Lig", "Puan durumu, fikstür ve takvim burada. 📈 Analiz sekmesi takımının nerede iyi, nerede kötü olduğunu grafiklerle gösterir."),
        "transfer" to Tip("💼 Transfer", "Filtrele, kısa listeye ekle, gözlemci gönder. Teklifte önce kulüple bonservisi, sonra oyuncuyla maaşı konuşursun. Maaş bütçesini aşma!"),
        "inbox" to Tip("✉️ Gelen kutusu", "Yönetimden, oyunculardan ve basından gelen her şey burada. Önemli mesajlar ana sayfaya da düşer."),
        "club" to Tip("🏟️ Kulüp", "Finans, stadyum, tesisler, sponsorlar ve teknik ekip. İyi bir antrenör gençleri hızlı geliştirir; iyi bir gözlemci rakipleri net görür."),
        "match" to Tip("⚽ Maç", "Alttan hızı (1x/3x/⚡) ve mentaliteyi seç. 📋 Taktik ile maçı durdurup diziliş ve talimat değiştirebilirsin; yardımcı antrenörün sarı çerçeveli önerilerine dikkat et."),
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
                Text("İpuçlarını kapat", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp).clickable { Tips.disableAll() })
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.pressClick(0.94f) { Tips.dismiss() }.clip(RoundedCornerShape(10.dp)).background(Teal).padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text("Anladım", color = Color(0xFF06231A), fontWeight = FontWeight.Black, fontSize = 13.sp) }
        }
    }
}
