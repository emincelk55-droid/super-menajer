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

    val names = listOf("Dengeli", "Profesyonel", "Hırslı", "Tembel", "Lider", "Büyük maç oyuncusu", "Sakatlığa yatkın", "Sinirli", "Sadık")
    val icons = listOf("⚖️", "🎯", "🔥", "😴", "👑", "⭐", "🩹", "😤", "💙")
    val info = listOf(
        "Ne iyi ne kötü; istikrarlı bir karakter.",
        "Antrenmanda örnek; daha hızlı gelişir.",
        "Kazanmak için yanıp tutuşur; hızlı gelişir ama yedekte kalmaya tahammülü yok.",
        "Antrenmanı sevmez; gelişimi yavaştır.",
        "Sahadayken takım arkadaşlarını yukarı çeker. İyi bir kaptan adayı.",
        "Derbilerde ve kupa maçlarında daha iyi oynar.",
        "Sakatlık riski yüksek.",
        "Sert müdahaleler; kart görme riski yüksek.",
        "Kulübüne bağlıdır; morali kolay bozulmaz.",
    )

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
