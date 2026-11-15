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
            gf - ga >= 3 -> listOf(L("Futbol şov! $opp'yi dağıttık 🔥 $m hoca sen çok yaşa!", "What a show! We demolished $opp 🔥 $m, you legend!"), L("Bu takım şampiyonluk oynar, nokta. $gf-$ga 💪", "This team will challenge for the title, full stop. $gf-$ga 💪"))
            gf > ga -> listOf(L("3 puan cebimizde! $gf-$ga ✅", "3 points in the bag! $gf-$ga ✅"), L("Zor maçtı ama kazandık, karakter var bu takımda 👊", "Tough game but we won, this team has character 👊"), L("Hocanın oyuncu değişiklikleri maçı aldı 🧠", "The manager's substitutions won the game 🧠"))
            gf == ga -> listOf(L("$opp karşısında $gf-$ga... Bu kadrodan daha fazlasını bekliyoruz 😐", "$gf-$ga against $opp... We expect more from this squad 😐"), L("1 puan da puandır ama tempo düşüktü.", "A point is a point, but the tempo was low."))
            ga - gf >= 3 -> listOf(L("Rezalet! $gf-$ga 😡 Bu taktik ne hocam?", "Disgraceful! $gf-$ga 😡 What tactic is this, boss?"), L("Yönetim acil açıklama yapmalı, bu böyle gitmez!", "The board must make a statement, this can't go on!"))
            else -> listOf(L("Kötü sonuç $gf-$ga. Toparlanmamız lazım 😞", "Bad result $gf-$ga. We need to regroup 😞"), L("Orta saha yine kayıptı, transfer şart!", "Lost the midfield again, we need signings!"))
        }
        post(g, lines[rnd.nextInt(lines.size)], rnd)
        if (scorer != null && gf > 0 && rnd.nextBoolean()) post(g, L("$scorer yine sahnede ⚽ Formasını hak ediyor!", "$scorer on the scoresheet again ⚽ He deserves his place!"), rnd)
    }

    fun transfer(g: Game, name: String, inbound: Boolean, price: Long, rnd: Random = Random.Default) {
        val t = if (inbound) listOf(L("HOŞ GELDİN $name! 🙌 ${money(price)} iyi para ama değer.", "WELCOME $name! 🙌 ${money(price)} is a lot, but he's worth it."), L("$name transferi tam isabet, kanat sorunu çözüldü!", "$name is a perfect signing, the wing problem is solved!"), L("$name kimdir ya? Bu paraya daha iyisi alınırdı 🤔", "Who is $name? We could have got someone better for that money 🤔"))
        else listOf(L("$name gitti mi?! Yönetim ne yapıyor 😤", "$name is gone?! What is the board doing 😤"), L("${money(price)} iyi para, $name için doğru zamanda satış.", "${money(price)} is good money, the right time to sell $name."), L("Elveda $name, teşekkürler her şey için ❤️", "Farewell $name, thanks for everything ❤️"))
        post(g, t[rnd.nextInt(t.size)], rnd)
    }
}
