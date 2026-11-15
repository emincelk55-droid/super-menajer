package com.supermenajer.game.model

import com.supermenajer.game.game.L

/** Bir ligin sabit tanımı (kayda yazılmaz). Takımın `division` alanı lig kimliğini tutar. */
data class LeagueDef(
    val id: Int,
    val country: String,
    val flag: String,
    val name: String,
    val tier: Int,
    /** Lig ödüllerinin ve maaş limitlerinin çarpanı. */
    val wealth: Double,
    /** Bu ülkenin kupa yarışmasının kimliği. */
    val cupComp: Int,
    /** Avrupa Kupası kontenjanı (toplam 32); Güney Amerika ligleri 0. */
    val euroSlots: Int = 0,
)

object Leagues {
    /** v0.3-v0.4'ten kalan, ülkesi olmayan eski Avrupa kulüpleri. */
    const val LEGACY_EURO = 2

    val all = listOf(
        LeagueDef(0, "Türkiye", "TR", L("Süper Lig", "Super League"), 1, 1.0, Comp.CUP, 4),
        LeagueDef(1, "Türkiye", "TR", L("1. Lig", "First League"), 2, 1.0, Comp.CUP),
        LeagueDef(10, "İngiltere", "GB", L("İngiltere Ligi", "English League"), 1, 1.8, 30, 4),
        LeagueDef(11, "İngiltere", "GB", L("İngiltere 2. Ligi", "English Second Tier"), 2, 1.3, 30),
        LeagueDef(12, "İspanya", "ES", L("İspanya Ligi", "Spanish League"), 1, 1.4, 31, 4),
        LeagueDef(13, "İspanya", "ES", L("İspanya 2. Ligi", "Spanish Second Tier"), 2, 1.0, 31),
        LeagueDef(14, "Almanya", "DE", L("Almanya Ligi", "German League"), 1, 1.4, 32, 4),
        LeagueDef(15, "Almanya", "DE", L("Almanya 2. Ligi", "German Second Tier"), 2, 1.0, 32),
        LeagueDef(16, "İtalya", "IT", L("İtalya Ligi", "Italian League"), 1, 1.3, 33, 4),
        LeagueDef(17, "İtalya", "IT", L("İtalya 2. Ligi", "Italian Second Tier"), 2, 1.0, 33),
        LeagueDef(18, "Fransa", "FR", L("Fransa Ligi", "French League"), 1, 1.1, 34, 3),
        LeagueDef(19, "Portekiz", "PT", L("Portekiz Ligi", "Portuguese League"), 1, 0.6, 35, 3),
        LeagueDef(20, "Hollanda", "NL", L("Hollanda Ligi", "Dutch League"), 1, 0.6, 36, 3),
        LeagueDef(21, "Belçika", "BE", L("Belçika Ligi", "Belgian League"), 1, 0.5, 37, 2),
        LeagueDef(22, "İskoçya", "GB", L("İskoçya Ligi", "Scottish League"), 1, 0.45, 38, 1),
        LeagueDef(23, "Brezilya", "BR", L("Brezilya Ligi", "Brazilian League"), 1, 0.55, 39),
        LeagueDef(24, "Arjantin", "AR", L("Arjantin Ligi", "Argentine League"), 1, 0.45, 40),
    )
    private val byId = all.associateBy { it.id }

    fun of(id: Int): LeagueDef? = byId[id]
    val countries: List<String> = all.map { it.country }.distinct()
    fun ofCountry(country: String): List<LeagueDef> = all.filter { it.country == country }
    fun top(country: String): LeagueDef = ofCountry(country).first { it.tier == 1 }
    fun second(country: String): LeagueDef? = ofCountry(country).firstOrNull { it.tier == 2 }
    val topLeagues: List<LeagueDef> = all.filter { it.tier == 1 }
    fun cupCountry(comp: Int): String? = all.firstOrNull { it.cupComp == comp }?.country
    fun flagOf(country: String): String = all.firstOrNull { it.country == country }?.flag ?: countryCode(country)
    fun cupName(comp: Int): String = (cupCountry(comp) ?: "Türkiye").let { c -> com.supermenajer.game.game.L("$c Kupası", countryName(c) + " Cup") }

    private val countryEn = mapOf(
        "Türkiye" to "Turkey", "İngiltere" to "England", "İspanya" to "Spain", "Almanya" to "Germany", "İtalya" to "Italy",
        "Fransa" to "France", "Portekiz" to "Portugal", "Hollanda" to "Netherlands", "Belçika" to "Belgium", "İskoçya" to "Scotland",
        "Brezilya" to "Brazil", "Arjantin" to "Argentina", "Hırvatistan" to "Croatia", "Ukrayna" to "Ukraine", "Yunanistan" to "Greece",
        "Sırbistan" to "Serbia", "Norveç" to "Norway", "Avusturya" to "Austria", "Danimarka" to "Denmark", "Çekya" to "Czechia",
    )

    /** Ülke adının ekranda görünen hâli (kodda ülke adları Türkçe anahtar olarak kalır). */
    fun countryName(c: String): String = if (com.supermenajer.game.game.Lang.en) countryEn[c] ?: c else c

    /** Eski Avrupa kulüplerinin ülke adından bayrak kodu. */
    fun countryCode(country: String): String = when (country) {
        "İngiltere", "İskoçya" -> "GB"
        "İspanya" -> "ES"
        "Almanya" -> "DE"
        "İtalya" -> "IT"
        "Fransa" -> "FR"
        "Portekiz" -> "PT"
        "Hollanda" -> "NL"
        "Belçika" -> "BE"
        "Hırvatistan" -> "HR"
        "Ukrayna" -> "UA"
        "Yunanistan" -> "GR"
        "Sırbistan" -> "RS"
        "Norveç" -> "NO"
        "Avusturya" -> "AT"
        "Danimarka" -> "DK"
        "Çekya" -> "CZ"
        "Brezilya" -> "BR"
        "Arjantin" -> "AR"
        else -> "TR"
    }
}
