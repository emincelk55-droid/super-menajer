package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import kotlin.random.Random

/** Taraftar sosyal medya akışı: sonuçlara, transferlere ve yönetime tepkiler. */
object Social {
    private val users = listOf(
        "@tribunsesi", "@efsane_10", "@kale_arkasi", "@ultraslan", "@futbolanaliz", "@sahaninkral", "@ofsaytbayragi",
        "@derbi_ruhu", "@altyapi_takip", "@transfer_kulisi", "@taktik_tahtasi", "@sari_kart",
    )

    fun post(g: Game, text: String, rnd: Random = Random.Default) {
        val likes = rnd.nextInt(40, 4000)
        g.feed.add(0, "${users[rnd.nextInt(users.size)]}|${text.replace("|", " ")}|$likes")
        while (g.feed.size > 30) g.feed.removeAt(g.feed.lastIndex)
    }

    fun afterMatch(g: Game, gf: Int, ga: Int, opp: String, scorer: String?, rnd: Random = Random.Default) {
        val m = g.managerName.substringBefore(" ")
        val lines = when {
            gf - ga >= 3 -> listOf("Futbol şov! $opp'yi dağıttık 🔥 $m hoca sen çok yaşa!", "Bu takım şampiyonluk oynar, nokta. $gf-$ga 💪")
            gf > ga -> listOf("3 puan cebimizde! $gf-$ga ✅", "Zor maçtı ama kazandık, karakter var bu takımda 👊", "Hocanın oyuncu değişiklikleri maçı aldı 🧠")
            gf == ga -> listOf("$opp karşısında $gf-$ga... Bu kadrodan daha fazlasını bekliyoruz 😐", "1 puan da puandır ama tempo düşüktü.")
            ga - gf >= 3 -> listOf("Rezalet! $gf-$ga 😡 Bu taktik ne hocam?", "Yönetim acil açıklama yapmalı, bu böyle gitmez!")
            else -> listOf("Kötü sonuç $gf-$ga. Toparlanmamız lazım 😞", "Orta saha yine kayıptı, transfer şart!")
        }
        post(g, lines[rnd.nextInt(lines.size)], rnd)
        if (scorer != null && gf > 0 && rnd.nextBoolean()) post(g, "$scorer yine sahnede ⚽ Formasını hak ediyor!", rnd)
    }

    fun transfer(g: Game, name: String, inbound: Boolean, price: Long, rnd: Random = Random.Default) {
        val t = if (inbound) listOf("HOŞ GELDİN $name! 🙌 ${money(price)} iyi para ama değer.", "$name transferi tam isabet, kanat sorunu çözüldü!", "$name kimdir ya? Bu paraya daha iyisi alınırdı 🤔")
        else listOf("$name gitti mi?! Yönetim ne yapıyor 😤", "${money(price)} iyi para, $name için doğru zamanda satış.", "Elveda $name, teşekkürler her şey için ❤️")
        post(g, t[rnd.nextInt(t.size)], rnd)
    }
}
