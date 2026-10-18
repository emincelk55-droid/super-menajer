package com.supermenajer.game.model

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
)

object Leagues {
    /** v0.3-v0.4'ten kalan, ülkesi olmayan eski Avrupa kulüpleri. */
    const val LEGACY_EURO = 2

    val all = listOf(
        LeagueDef(0, "Türkiye", "TR", "Süper Lig", 1, 1.0, Comp.CUP),
        LeagueDef(1, "Türkiye", "TR", "1. Lig", 2, 1.0, Comp.CUP),
        LeagueDef(10, "İngiltere", "GB", "İngiltere Ligi", 1, 1.8, 30),
        LeagueDef(11, "İngiltere", "GB", "İngiltere 2. Ligi", 2, 1.3, 30),
        LeagueDef(12, "İspanya", "ES", "İspanya Ligi", 1, 1.4, 31),
        LeagueDef(13, "İspanya", "ES", "İspanya 2. Ligi", 2, 1.0, 31),
        LeagueDef(14, "Almanya", "DE", "Almanya Ligi", 1, 1.4, 32),
        LeagueDef(15, "Almanya", "DE", "Almanya 2. Ligi", 2, 1.0, 32),
        LeagueDef(16, "İtalya", "IT", "İtalya Ligi", 1, 1.3, 33),
        LeagueDef(17, "İtalya", "IT", "İtalya 2. Ligi", 2, 1.0, 33),
        LeagueDef(18, "Fransa", "FR", "Fransa Ligi", 1, 1.1, 34),
        LeagueDef(19, "Portekiz", "PT", "Portekiz Ligi", 1, 0.6, 35),
        LeagueDef(20, "Hollanda", "NL", "Hollanda Ligi", 1, 0.6, 36),
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
    fun cupName(comp: Int): String = (cupCountry(comp) ?: "Türkiye") + " Kupası"

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
        else -> "TR"
    }
}
