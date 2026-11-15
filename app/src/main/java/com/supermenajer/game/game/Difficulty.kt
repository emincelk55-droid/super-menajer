package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/** Zorluk seviyesi: 0 Kolay, 1 Normal, 2 Zor. Yeni kariyerde seçilir. */
object Difficulty {
    private val namesCache = LangCached { listOf(L("Kolay", "Easy"), L("Normal", "Normal"), L("Zor", "Hard")) }
    val names get() = namesCache.get()
    private val descriptionsCache = LangCached { listOf(
        L("Daha fazla bütçe, sabırlı yönetim, maçlarda küçük avantaj.", "More budget, a patient board, a small edge in matches."),
        L("Dengeli deneyim.", "A balanced experience."),
        L("Kısıtlı bütçe, sabırsız yönetim, rakipler daha zorlu.", "Tight budget, an impatient board, tougher opponents."),
    ) }
    val descriptions get() = descriptionsCache.get()

    /** Olumsuz sonuçlarda güven kaybı çarpanı. */
    fun patience(g: Game): Double = when (g.difficulty) { 0 -> 0.6; 2 -> 1.35; else -> 1.0 }

    /** Başlangıç transfer bütçesi çarpanı. */
    fun budgetFactor(g: Game): Double = budgetFactor(g.difficulty)
    fun budgetFactor(d: Int): Double = when (d) { 0 -> 1.5; 2 -> 0.7; else -> 1.0 }

    /** Kullanıcı takımının maç gücü çarpanı. */
    fun matchFactor(g: Game): Double = when (g.difficulty) { 0 -> 1.04; 2 -> 0.97; else -> 1.0 }
}
