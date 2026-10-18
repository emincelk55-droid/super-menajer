package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Team
import kotlin.random.Random

/** Yabancı liglerin kurgusal kulüpleri. Tüm isimler uydurmadır. */
object WorldGen {
    private class CountryDef(
        val country: String,
        /** Ligdeki oyuncuların çoğunluğunun uyruğu. */
        val nation: String,
        val towns: List<String>,
        /** {T} kasaba adının yerine geçer. */
        val patterns: List<String>,
        /** Bilinen büyük kulüpler (ilk sıradakiler en güçlü). */
        val stars: List<String>,
        val topRep: IntProgression,
        val secondRep: IntProgression?,
    )

    private val countries = listOf(
        CountryDef(
            "İngiltere", "GB",
            listOf(
                "Westford", "Ashbury", "Bramwell", "Coldharbour", "Dunmore", "Elmstead", "Fairhaven", "Greystone", "Hollowmere",
                "Kestrel Bay", "Langford", "Millbrook", "Netherby", "Oakhurst", "Pemberton", "Queensmead", "Ravenscar", "Stonebury",
                "Thornfield", "Upperton", "Valebridge", "Whitcliff", "Yarrowby", "Ashcombe", "Blackmoor", "Castlegate", "Dalwick",
                "Embury", "Foxley", "Glenford", "Harrowdale", "Lowmoor", "Marshfield", "Brackenhill",
            ),
            listOf("{T} United", "{T} City", "{T} Rovers", "{T} Athletic", "{T} Town", "{T} Albion", "{T} Wanderers", "{T} County"),
            listOf("Northbridge United", "Kingsport City", "Redhill Rovers"),
            92 downTo 66, 64 downTo 47,
        ),
        CountryDef(
            "İspanya", "ES",
            listOf(
                "Sierra Verde", "Puerto Alto", "Mirabel", "Peñaroja", "Riosol", "Torrealta", "Valdemora", "Almenara", "Brisamar",
                "Cerralba", "Fuentesol", "Lunaria", "Oroverde", "Pinares", "Quintanar", "Robledo", "Santa Brisa", "Urdaleta",
                "Vegaclara", "Zarzalejo", "Aguasol", "Bellamar", "Costablanca", "Encinar", "Ferral", "Gavilán", "Isla Verde",
                "Juncal", "Lomas Altas", "Montealto", "Navaluz", "Olivar", "Rocalba", "Solana",
            ),
            listOf("CD {T}", "UD {T}", "{T} CF", "SD {T}", "Real {T}", "Atlético {T}", "Deportivo {T}", "Racing {T}"),
            listOf("Real Montaña CF", "Atlético Levante Sur", "Valle Real CF"),
            92 downTo 62, 60 downTo 44,
        ),
        CountryDef(
            "Almanya", "DE",
            listOf(
                "Altenbrück", "Bergau", "Dornfeld", "Eichwald", "Falkenau", "Grünhain", "Hohenfels", "Kaltenbach", "Lindenau",
                "Mühlberg", "Neustein", "Oberhaus", "Rosenthal", "Schwarzbach", "Talheim", "Unterberg", "Wolfsau", "Zellingen",
                "Adlerhof", "Birkenfeld", "Donautal", "Elbstadt", "Fichtenau", "Goldbach", "Hasenberg", "Isarburg", "Kronwald",
                "Lahnfeld", "Moorbach", "Nebelstein", "Ostwald", "Rabenau", "Seeburg", "Tannheim",
            ),
            listOf("FC {T}", "SV {T}", "VfB {T}", "TSV {T}", "SC {T}", "VfL {T}", "1. FC {T}", "SpVgg {T}", "Fortuna {T}", "Union {T}"),
            listOf("FC Rheinstadt", "Borussia Waldheim", "Eintracht Nordberg"),
            90 downTo 62, 60 downTo 44,
        ),
        CountryDef(
            "İtalya", "IT",
            listOf(
                "Valtorre", "Montecalvo", "Pietralunga", "Roccabianca", "Sangiorgio", "Torrenera", "Castelverde", "Borgoalto",
                "Fiumerosso", "Lagobello", "Marinella", "Novacella", "Ortavalle", "Portosole", "Querceto", "Rivabella", "Santalba",
                "Tornalba", "Valdoro", "Zafferia", "Acquaviva", "Belcastro", "Collefiorito", "Dorsalto", "Gallerano", "Lucerna",
                "Montenero", "Pianello", "Rocca Alta", "Selvanera", "Brezzano", "Corvara", "Fontebella", "Monteluce",
            ),
            listOf("AC {T}", "AS {T}", "US {T}", "FC {T}", "SS {T}", "{T} Calcio", "Sporting {T}", "Virtus {T}", "Pro {T}"),
            listOf("AC Lombardia", "Vesuvio Calcio", "FC Adriatico"),
            90 downTo 62, 60 downTo 44,
        ),
        CountryDef(
            "Fransa", "FR",
            listOf(
                "Côte-Belle", "Valmont", "Beaurivage", "Châteaublanc", "Montclair", "Rochefière", "Saint-Aubert", "Fontclaire",
                "Grandpré", "Haute-Rive", "Lavande", "Mirabeau", "Normandel", "Pontvert", "Sainte-Alouette", "Belcourt",
            ),
            listOf("FC {T}", "AS {T}", "Olympique {T}", "Stade {T}", "RC {T}", "SC {T}", "US {T}", "Racing {T}"),
            listOf("Olympique Lumière", "AS Bretagne"),
            88 downTo 58, null,
        ),
        CountryDef(
            "Portekiz", "PT",
            listOf(
                "Algarvia", "Belavista", "Costa Azul", "Estrela do Sul", "Fonte Nova", "Lusitânia", "Marinhal", "Minho Verde",
                "Oriente", "Penalva", "Ribeira", "Santa Luzia", "Serra Alta", "Tejo Azul", "Vale Formoso", "Vila Mar",
            ),
            listOf("FC {T}", "SC {T}", "CD {T}", "GD {T}", "Académico {T}", "União {T}", "Sporting {T}"),
            listOf("Sporting Atlântico", "FC Douro"),
            82 downTo 48, null,
        ),
        CountryDef(
            "Hollanda", "NL",
            listOf(
                "Amsteldorp", "Bloemwijk", "Dijkstad", "Eemhaven", "Friesvelde", "Grevenhorst", "Havenburg", "IJsselmond",
                "Kanaalzicht", "Lindewoud", "Molenveld", "Noordkaap", "Oosterdijk", "Rietveld", "Slingeland", "Tulpendal",
            ),
            listOf("FC {T}", "SC {T}", "VV {T}", "Sparta {T}", "Go Ahead {T}", "Quick {T}", "{T} United"),
            listOf("Grachten FC", "Polder Utd"),
            82 downTo 48, null,
        ),
    )

    private val palette = listOf(
        0xFFD50000, 0xFF1565C0, 0xFF2E7D32, 0xFFFFEB3B, 0xFF6A1B9A, 0xFFFF6F00, 0xFF111111, 0xFFFFFFFF,
        0xFF0D47A1, 0xFF00897B, 0xFF880E4F, 0xFF81D4FA, 0xFFC62828, 0xFF795548, 0xFF283593, 0xFF9E9D24,
    )

    private fun reps(range: IntProgression, n: Int): List<Int> {
        val hi = range.first
        val lo = range.last
        return List(n) { i -> hi - ((hi - lo) * i.toDouble() / (n - 1)).toInt() }
    }

    private fun shortOf(name: String, used: MutableSet<String>): String {
        val words = name.split(" ").filter { w -> w.length > 2 && w.uppercase() != w && w !in setOf("Real", "Atlético", "Deportivo", "Racing", "Sporting", "Virtus", "Pro", "Olympique", "Stade", "Fortuna", "Union", "Sparta", "Quick", "Académico", "União", "Calcio", "United", "City", "Rovers", "Athletic", "Town", "Albion", "Wanderers", "County", "Ahead") }
        val base = (words.firstOrNull() ?: name).filter { it.isLetter() }.uppercase()
        var s = base.take(3)
        var i = 3
        while (s in used && i < base.length) { s = base.take(2) + base[i]; i++ }
        used += s
        return s
    }

    /** Türkiye dışındaki tüm ligleri ekler. */
    fun addForeignLeagues(g: Game, rnd: Random) {
        var nextId = (g.teams.maxOfOrNull { it.id } ?: -1) + 1
        val used = g.teams.map { it.short }.toMutableSet()
        for (c in countries) {
            val towns = c.towns.shuffled(rnd).toMutableList()
            val patterns = c.patterns
            val tiers = listOfNotNull(Leagues.top(c.country) to c.topRep, Leagues.second(c.country)?.let { it to c.secondRep!! })
            var starIdx = 0
            for ((league, range) in tiers) {
                for ((i, rep) in reps(range, 18).withIndex()) {
                    val name = if (league.tier == 1 && starIdx < c.stars.size && i == starIdx) {
                        c.stars[starIdx++]
                    } else {
                        patterns[rnd.nextInt(patterns.size)].replace("{T}", towns.removeAt(0))
                    }
                    val c1 = palette[rnd.nextInt(palette.size)]
                    var c2 = palette[rnd.nextInt(palette.size)]
                    if (c2 == c1) c2 = if (c1 == 0xFFFFFFFF) 0xFF111111 else 0xFFFFFFFF
                    val t = Team(
                        nextId++, name, shortOf(name, used), c.country, c1, c2, rep,
                        Generator.budgetFor(rep) * (if (league.tier == 1) 2 else 1), division = league.id,
                    )
                    g.addTeam(t)
                    Generator.fillSquad(g, t, rnd, c.nation)
                }
            }
        }
    }

    /** v0.4 kayıtlarını yeni dünyaya taşır: eski ülkesiz Avrupa kulüpleri kalkar, yeni ligler kurulur. */
    fun upgradeWorld(g: Game, rnd: Random) {
        val legacy = g.teams.filter { it.division == Leagues.LEGACY_EURO }.map { it.id }.toSet()
        g.removePlayers(g.players.filter { it.teamId in legacy })
        g.teams.removeAll { it.id in legacy }
        g.reindex()
        addForeignLeagues(g, rnd)
        g.worldUpgradePending = false
        g.addNews("Dünya genişledi!", "Artık İngiltere, İspanya, Almanya, İtalya, Fransa, Portekiz ve Hollanda ligleri de var. Avrupa Kupası'na her büyük ligin ilk 4'ü katılıyor.")
    }

    fun teamsOf(g: Game, country: String): List<Team> = g.teams.filter { g.countryOf(it) == country && it.division != Leagues.LEGACY_EURO }
}
