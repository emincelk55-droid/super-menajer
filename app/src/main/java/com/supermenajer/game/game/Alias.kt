package com.supermenajer.game.game

/**
 * Gerçek isimleri tanınır ama birebir aynı olmayacak şekilde değiştirir (PES tarzı).
 * Kural sabittir: aynı isim her zaman aynı takma ada dönüşür.
 *  - Kişiler: ön ad korunur, soyadı değişir ("Arda Güler" → "Arda Gülar").
 *  - Kulüp ve stadyum: en ayırt edici kelime değişir ("Galatasaray" → "Galatasarey").
 */
object Alias {
    private val vowelSwap = mapOf(
        'a' to 'e', 'e' to 'a', 'i' to 'e', 'o' to 'u', 'u' to 'o', 'ı' to 'i', 'ö' to 'o', 'ü' to 'u',
        'é' to 'e', 'è' to 'e', 'á' to 'a', 'à' to 'a', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ã' to 'a', 'õ' to 'o',
        'â' to 'a', 'ê' to 'e', 'ô' to 'o', 'ä' to 'e', 'ë' to 'e', 'ï' to 'i', 'y' to 'i',
    )
    private val vowels = vowelSwap.keys

    private val generic = setOf(
        "fc", "cf", "sk", "as", "ac", "sc", "ss", "us", "afc", "sv", "vfb", "vfl", "tsv", "rc", "rcd", "cd", "ud", "sd", "fk", "jk", "ogc",
        "psv", "az", "nec", "1.", "real", "club", "sporting", "united", "city", "town", "athletic", "atlético", "athletico", "calcio",
        "de", "del", "la", "le", "les", "di", "da", "do", "dos", "das", "der", "die", "the", "of", "and", "y", "e", "st.", "saint",
        "stade", "stadium", "stadio", "estadio", "estádio", "stadion", "stadyumu", "stadı", "arena", "park", "ground", "olympique",
        "racing", "deportivo", "spor", "kulübü", "fußball", "borussia", "eintracht", "fortuna", "hellas", "inter", "internazionale",
        "football", "futebol", "fútbol", "hotspur", "albion", "rovers", "wanderers", "county", "villa", "hove", "and", "1907", "1903",
    )

    /** Tek kelimeyi tanınır şekilde değiştir. */
    fun word(w: String): String {
        if (w.length < 3) return w
        val chars = w.toMutableList()
        // 1) Çift harf varsa birini at: Mbappé → Mbapé, Haaland → Haland
        for (i in 1 until chars.size) {
            if (chars[i].lowercaseChar() == chars[i - 1].lowercaseChar() && chars[i].isLetter()) {
                chars.removeAt(i)
                return chars.joinToString("")
            }
        }
        // 2) Son ünlüyü değiştir (son harf değilse önce onu dene): Güler → Gülar
        val idx = chars.indices.reversed().firstOrNull { i -> chars[i].lowercaseChar() in vowels && i > 0 }
        if (idx != null) {
            val c = chars[idx]
            val n = vowelSwap.getValue(c.lowercaseChar())
            chars[idx] = if (c.isUpperCase()) n.uppercaseChar() else n
            val out = chars.joinToString("")
            if (out != w) return out
        }
        return w + "e"
    }

    /** Kişi adı: ön adlar aynı, son kelime değişir. Tek isimliler (Pedri) de değişir. */
    fun person(name: String): String {
        val parts = name.trim().split(Regex("\\s+"))
        if (parts.isEmpty()) return name
        val last = parts.indices.reversed().firstOrNull { parts[it].length >= 3 && parts[it].lowercase() !in setOf("jr.", "júnior", "junior", "filho", "neto") }
            ?: (parts.size - 1)
        return parts.mapIndexed { i, p -> if (i == last) word(p) else p }.joinToString(" ")
    }

    /** Kulüp / stadyum adı: en uzun ayırt edici kelime değişir. */
    fun club(name: String): String {
        val parts = name.trim().split(Regex("\\s+"))
        val cand = parts.indices.filter { parts[it].lowercase().trimEnd('.', ',') !in generic && parts[it].any { c -> c.isLetter() } }
        val pick = cand.maxByOrNull { parts[it].length } ?: return parts.mapIndexed { i, p -> if (i == parts.lastIndex) word(p) else p }.joinToString(" ")
        return parts.mapIndexed { i, p -> if (i == pick) word(p) else p }.joinToString(" ")
    }
}
