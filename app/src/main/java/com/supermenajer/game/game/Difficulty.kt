package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/** Zorluk seviyesi: 0 Kolay, 1 Normal, 2 Zor. Yeni kariyerde seçilir. */
object Difficulty {
    val names = listOf("Kolay", "Normal", "Zor")
    val descriptions = listOf(
        "Daha fazla bütçe, sabırlı yönetim, maçlarda küçük avantaj.",
        "Dengeli deneyim.",
        "Kısıtlı bütçe, sabırsız yönetim, rakipler daha zorlu.",
    )

    /** Olumsuz sonuçlarda güven kaybı çarpanı. */
    fun patience(g: Game): Double = when (g.difficulty) { 0 -> 0.6; 2 -> 1.35; else -> 1.0 }

    /** Başlangıç transfer bütçesi çarpanı. */
    fun budgetFactor(g: Game): Double = when (g.difficulty) { 0 -> 1.5; 2 -> 0.7; else -> 1.0 }

    /** Kullanıcı takımının maç gücü çarpanı. */
    fun matchFactor(g: Game): Double = when (g.difficulty) { 0 -> 1.04; 2 -> 0.97; else -> 1.0 }
}
