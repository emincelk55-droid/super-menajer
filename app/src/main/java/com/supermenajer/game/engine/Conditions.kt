package com.supermenajer.game.engine

import com.supermenajer.game.game.GameCalendar
import com.supermenajer.game.game.L
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Team
import java.time.LocalDate
import kotlin.random.Random

/**
 * Maç günü koşulları: hava, zemin ve hakem. Maçtan önce bellidir (fikstür ve tarihten türetilir),
 * maç hazırlığında görünür ve maç motorunu etkiler.
 */
class MatchConditions(val weather: Int, val heavyPitch: Boolean, val refName: String, val refStrict: Double) {
    /** Şut üretimi: yağmur ve kar oyunu bozar. */
    val shotFactor: Double get() = when (weather) { Conditions.RAIN -> 0.96; Conditions.SNOW -> 0.9; Conditions.WIND -> 0.97; else -> 1.0 }

    /** Yorgunluk: sıcak ve ağır zemin daha çok yorar. */
    val fatigueFactor: Double get() = (if (weather == Conditions.HEAT) 1.18 else if (weather == Conditions.SNOW) 1.05 else 1.0) * (if (heavyPitch) 1.1 else 1.0)

    /** Takımın pas tarzına göre koşulların etkisi (rüzgârda uzun top, ağır zeminde kısa pas zorlaşır). */
    fun styleFactor(t: Team): Double = (if (weather == Conditions.WIND && t.passing == 2) 0.94 else 1.0) * (if (heavyPitch && t.passing == 0) 0.95 else 1.0)

    val weatherIcon: String get() = Conditions.icons[weather]
    val weatherName: String get() = Conditions.names[weather]
    val refText: String get() = when {
        refStrict >= 1.18 -> L("sert", "strict")
        refStrict <= 0.85 -> L("serbest", "lenient")
        else -> L("dengeli", "balanced")
    }

    /** Kısa özet: "🌧 Yağmurlu · ağır zemin · Hakem Ali Kaya (sert)". */
    fun summary(): String = "$weatherIcon $weatherName · " + (if (heavyPitch) L("ağır zemin", "heavy pitch") else L("iyi zemin", "good pitch")) +
        " · " + L("Hakem", "Referee") + " $refName ($refText)"

    /** Yardımcı antrenörün koşullara dair tavsiyesi (yoksa null). */
    fun advice(): String? = when {
        weather == Conditions.WIND -> L("Rüzgârlı havada uzun toplar savrulur; kısa ya da karışık pas daha güvenli.", "Long balls drift in the wind; short or mixed passing is safer.")
        heavyPitch -> L("Zemin ağır: kısa pas yerine direkt oyun ve uzun toplar daha etkili, kondisyon çabuk düşer.", "Heavy pitch: direct play and long balls work better than short passing, and fitness drops quickly.")
        weather == Conditions.HEAT -> L("Sıcakta oyuncular çabuk yorulur; pres ve tempoyu düşürmeyi düşün.", "Players tire quickly in the heat; consider lowering pressing and tempo.")
        refStrict >= 1.18 -> L("Hakem kart çıkarmayı seviyor; sert müdahalelere dikkat.", "The referee is card-happy; be careful with hard tackles.")
        else -> null
    }
}

object Conditions {
    const val CLEAR = 0
    const val CLOUDY = 1
    const val RAIN = 2
    const val WIND = 3
    const val SNOW = 4
    const val HEAT = 5

    val icons = listOf("☀️", "⛅", "🌧️", "💨", "❄️", "🥵")
    val names: List<String> get() = listOf(L("Açık", "Clear"), L("Bulutlu", "Cloudy"), L("Yağmurlu", "Rainy"), L("Rüzgârlı", "Windy"), L("Karlı", "Snowy"), L("Sıcak", "Hot"))

    private val refFirst = listOf("Ali", "Cüneyt", "Halil", "Mete", "Arda", "Volkan", "Zorbay", "Atilla", "Kadir", "Ümit", "Bahattin", "Abdulkadir")
    private val refLast = listOf("Kaya", "Çakır", "Umut", "Kalkavan", "Kardeşler", "Bilgiç", "Küçük", "Karaoğlan", "Sağlam", "Öztürk", "Şimşek", "Bitigen")

    /** Maçın koşulları (aynı maç için hep aynı). */
    fun of(g: Game, f: Fixture): MatchConditions {
        val rnd = Random(f.home * 7919L + f.away * 104729L + f.week * 31L + g.season * 1_000_003L)
        val month = runCatching { LocalDate.ofEpochDay(GameCalendar.dateOf(g, f.week)).monthValue }.getOrDefault(10)
        val r = rnd.nextDouble()
        val weather = when (month) {
            12, 1, 2 -> when { r < 0.14 -> SNOW; r < 0.44 -> RAIN; r < 0.56 -> WIND; r < 0.8 -> CLOUDY; else -> CLEAR }
            6, 7, 8 -> when { r < 0.26 -> HEAT; r < 0.34 -> RAIN; r < 0.44 -> CLOUDY; else -> CLEAR }
            else -> when { r < 0.22 -> RAIN; r < 0.34 -> WIND; r < 0.62 -> CLOUDY; else -> CLEAR }
        }
        val heavy = (weather == RAIN || weather == SNOW) && rnd.nextDouble() < 0.6
        val strict = (1.0 + (rnd.nextDouble() + rnd.nextDouble() - 1.0) * 0.4).coerceIn(0.75, 1.35)
        val name = refFirst[rnd.nextInt(refFirst.size)] + " " + refLast[rnd.nextInt(refLast.size)]
        return MatchConditions(weather, heavy, name, strict)
    }
}
