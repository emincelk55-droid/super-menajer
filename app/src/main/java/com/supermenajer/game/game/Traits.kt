package com.supermenajer.game.game

import com.supermenajer.game.model.Player

/**
 * FM / CM tarzı oyuncu kişilikleri. Oyuncunun kimliğinden belirlenir (kayıtta ayrıca tutulmaz).
 * Gelişimi, morali, büyük maçlardaki performansı, sakatlık ve kart riskini etkiler.
 */
object Traits {
    const val BALANCED = 0
    const val PRO = 1
    const val AMBITIOUS = 2
    const val LAZY = 3
    const val LEADER = 4
    const val BIG_GAME = 5
    const val FRAGILE = 6
    const val HOTHEAD = 7
    const val LOYAL = 8

    private val namesCache = LangCached { listOf(L("Dengeli", "Balanced"), L("Profesyonel", "Professional"), L("Hırslı", "Ambitious"), L("Tembel", "Lazy"), L("Lider", "Leader"), L("Büyük maç oyuncusu", "Big-game player"), L("Sakatlığa yatkın", "Injury-prone"), L("Sinirli", "Hot-headed"), L("Sadık", "Loyal")) }
    val names get() = namesCache.get()
    val icons = listOf("⚖️", "🎯", "🔥", "😴", "👑", "⭐", "🩹", "😤", "💙")
    private val infoCache = LangCached { listOf(
        L("Ne iyi ne kötü; istikrarlı bir karakter.", "Neither good nor bad; a steady character."),
        L("Antrenmanda örnek; daha hızlı gelişir.", "A model in training; develops faster."),
        L("Kazanmak için yanıp tutuşur; hızlı gelişir ama yedekte kalmaya tahammülü yok.", "Burns to win; develops fast but can't stand being on the bench."),
        L("Antrenmanı sevmez; gelişimi yavaştır.", "Doesn't like training; develops slowly."),
        L("Sahadayken takım arkadaşlarını yukarı çeker. İyi bir kaptan adayı.", "Lifts his teammates on the pitch. A good captain candidate."),
        L("Derbilerde ve kupa maçlarında daha iyi oynar.", "Plays better in derbies and cup matches."),
        L("Sakatlık riski yüksek.", "High injury risk."),
        L("Sert müdahaleler; kart görme riski yüksek.", "Rough challenges; high risk of cards."),
        L("Kulübüne bağlıdır; morali kolay bozulmaz.", "Loyal to his club; his morale doesn't drop easily."),
    ) }
    val info get() = infoCache.get()

    // Ağırlıklar: dengeli en yaygın
    private val weights = intArrayOf(34, 12, 10, 8, 6, 7, 8, 8, 7)
    private val total = weights.sum()

    fun of(p: Player): Int {
        var h = (p.id.toLong() * 2654435761L + 97) and 0x7fffffff
        h = (h xor (h shr 13)) * 1274126177L and 0x7fffffff
        var x = (h % total).toInt()
        for (i in weights.indices) {
            x -= weights[i]
            if (x < 0) return i
        }
        return BALANCED
    }

    fun name(p: Player) = "${icons[of(p)]} ${names[of(p)]}"

    /** Haftalık gelişim çarpanı. */
    fun devFactor(p: Player) = when (of(p)) { PRO -> 1.2; AMBITIOUS -> 1.15; LAZY -> 0.72; else -> 1.0 }

    /** Maç içi performans çarpanı. */
    fun matchFactor(p: Player, bigMatch: Boolean) = when (of(p)) {
        BIG_GAME -> if (bigMatch) 1.05 else 1.0
        LAZY -> 0.99
        else -> 1.0
    }

    fun injuryFactor(p: Player) = (if (of(p) == FRAGILE) 1.9 else if (of(p) == PRO) 0.85 else 1.0) * (if (p.fragileWeeks > 0) 2.2 else 1.0)
    fun foulFactor(p: Player) = if (of(p) == HOTHEAD) 1.6 else 1.0
    fun isLeader(p: Player) = of(p) == LEADER

    /** Moral hassasiyeti: sadık oyuncular daha az, hırslılar daha çok etkilenir. */
    fun moraleSensitivity(p: Player) = when (of(p)) { LOYAL -> 0.6; AMBITIOUS -> 1.35; else -> 1.0 }
}
