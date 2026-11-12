package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import kotlin.random.Random

/**
 * Kurgusal isimler: gerçek veriden gelen kulüp, kısa kod, stadyum, teknik direktör ve oyuncu adlarını
 * tamamen uydurma adlarla değiştirir (şehir, renk, güç ve kadro yapısı aynı kalır). Mağazada gerçek
 * kulüp ve futbolcu adı kullanmamak için. Aynı dünyada aynı sonucu verir; [undo] ile geri alınır.
 */
object Fictional {
    /** Geri alma bilgisi: değişen her adın eski hâli. */
    class Undo(
        val teams: Map<Int, Triple<String, String, String>>,
        val managers: Map<Int, String>,
        val players: Map<Int, String>,
    )

    private val suffix = mapOf(
        "TR" to listOf("{c}spor", "{c} Gücü", "{c} Yıldızspor", "{c} İdmanyurdu", "{c} Gençlik", "{c} Kartalları", "{c} Aslanları", "{c} Fırtınası"),
        "GB" to listOf("{c} Rovers", "{c} Athletic", "{c} Town", "{c} Wanderers", "{c} Rangers", "{c} Borough", "{c} Royals", "{c} Harriers"),
        "ES" to listOf("CD {c}", "{c} CF", "Racing {c}", "Unión {c}", "Deportivo {c}", "{c} Balompié", "Sporting {c}", "Atlético {c} Sur"),
        "DE" to listOf("FC {c}", "SV {c}", "{c} 1910", "TuS {c}", "SpVgg {c}", "Viktoria {c}", "Germania {c}", "Rot-Weiß {c}"),
        "IT" to listOf("{c} Calcio", "AC {c}", "US {c}", "Virtus {c}", "Pro {c}", "Polisportiva {c}", "Atletico {c}", "Real {c} 1920"),
        "FR" to listOf("{c} FC", "AS {c}", "Stade {c} Nord", "Racing {c}", "US {c}", "Union {c}", "Étoile {c}", "Olympique {c} Est"),
        "PT" to listOf("{c} SC", "Desportivo {c}", "União {c}", "Clube {c}", "Atlético {c}", "Académico {c}", "Estrela {c}", "Juventude {c}"),
        "NL" to listOf("{c} FC", "SC {c}", "VV {c}", "Quick {c}", "Sparta {c} Noord", "Victoria {c}", "DOS {c}", "Excelsior {c} Zuid"),
    )
    private val stadium = listOf("{c} Arena", "{c} Park", "{c} Stadı", "Yeni {c} Stadyumu", "{c} Kalesi", "{c} Vadisi")

    /** Lig kimliğinden ülke kodu (isim kalıbı için). */
    private fun country(division: Int): String = when (division) {
        0, 1 -> "TR"; 10, 11 -> "GB"; 12, 13 -> "ES"; 14, 15 -> "DE"; 16, 17 -> "IT"; 18 -> "FR"; 19 -> "PT"; 20 -> "NL"
        else -> com.supermenajer.game.model.Leagues.of(division)?.let { l -> listOf("GB", "ES", "DE", "IT", "FR", "PT", "NL").getOrNull(l.id % 7) } ?: "GB"
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("\\b(fc|cf|afc|sc|sk|fk|ac|as|cd)\\b"), "").replace(Regex("[^\\p{L}0-9]"), "")

    private val leagueIds = listOf(0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    /** Gerçek kulüp adları: kurgusal ad hiçbirine denk gelmesin. */
    private fun realClubNames(): Set<String> = buildSet {
        for (id in leagueIds) RealWorld.load(id)?.teams?.forEach { add(norm(it.name)); add(norm(Alias.club(it.name))) }
    }

    /** Gerçek futbolcu ve teknik direktör adları (ve benzeri takma adları). */
    private fun realPeople(): Set<String> = buildSet {
        for (id in leagueIds) RealWorld.load(id)?.teams?.forEach { t ->
            add(t.coach.lowercase()); add(Alias.person(t.coach).lowercase())
            t.players.forEach { add(it.name.lowercase()); add(Alias.person(it.name).lowercase()) }
        }
    }

    /**
     * Tanınmış futbolcuların ayırt edici soyadları (güç 66+, gerçek veride en çok 4 kez geçen): kurgusal adda
     * "Xhaka", "Mudryk" gibi yıldız soyadları çıkmasın. Demir, Silva gibi yaygın soyadları serbest kalır.
     */
    fun famousSurnames(): Set<String> {
        val count = HashMap<String, Int>()
        val strong = HashSet<String>()
        for (id in leagueIds) RealWorld.load(id)?.teams?.forEach { t ->
            for (p in t.players) {
                for (nm in listOf(p.name, Alias.person(p.name))) {
                    val sur = nm.substringAfterLast(' ').lowercase()
                    if (sur.length < 3) continue
                    count[sur] = (count[sur] ?: 0) + 1
                    if (p.ovr >= 66) strong += sur
                }
            }
        }
        return strong.filter { (count[it] ?: 0) <= 4 }.toHashSet()
    }

    /** Gerçek bir kişiyle aynı olmayan, yıldız soyadı taşımayan uydurma ad. */
    private fun personName(rnd: Random, nation: String, real: Set<String>, famous: Set<String> = emptySet()): String {
        fun bad(n: String) = n.lowercase() in real || n.substringAfterLast(' ').lowercase() in famous
        var n = Generator.fictionalName(rnd, nation)
        var k = 0
        while (bad(n) && k++ < 40) n = Generator.fictionalName(rnd, if (k > 20) "TR" else nation)
        return n
    }

    fun apply(g: Game): Undo {
        val rnd = Random(g.teams.size * 7919L + g.players.size)
        val real = realClubNames()
        val people = realPeople()
        val famous = famousSurnames()
        val usedNames = HashSet<String>()
        val usedShort = g.teams.filter { !it.real }.map { it.short }.toMutableSet()
        val teams = HashMap<Int, Triple<String, String, String>>()
        val managers = HashMap<Int, String>()
        for (t in g.teams.filter { it.real }) {
            teams[t.id] = Triple(t.name, t.short, t.stadiumName)
            val city = t.city.ifBlank { t.name.split(" ").maxBy { it.length } }
            val forms = suffix.getValue(country(t.division)).shuffled(rnd)
            val name = forms.asSequence().map { it.replace("{c}", city) }
                .firstOrNull { norm(it) !in real && it !in usedNames } ?: "$city ${usedNames.size + 1}"
            usedNames += name
            t.name = name
            // Kısa kod: adın şehir dışındaki ilk harfi + şehir, ya da şehrin ilk üç harfi
            val letters = name.filter { it.isLetter() }.uppercase()
            var short = (city.filter { it.isLetter() }.uppercase() + "XXX").take(3)
            var k = 0
            while (short in usedShort) { short = (letters.take(2) + letters.getOrElse(2 + k) { 'A' + k }).take(3); k++ }
            usedShort += short
            t.short = short
            t.stadiumName = stadium[rnd.nextInt(stadium.size)].replace("{c}", city)
            g.managers[t.id]?.let { old ->
                managers[t.id] = old
                g.managers[t.id] = personName(rnd, if (country(t.division) == "TR") "TR" else "GB", people)
            }
        }
        val players = HashMap<Int, String>()
        for (p in g.players) {
            players[p.id] = p.name
            p.name = personName(rnd, p.nation, people, famous)
        }
        g.fictionalNames = true
        g.reindex()
        return Undo(teams, managers, players)
    }

    fun undo(g: Game, u: Undo) {
        for ((id, v) in u.teams) g.teamOrNull(id)?.let { it.name = v.first; it.short = v.second; it.stadiumName = v.third }
        for ((id, n) in u.managers) g.managers[id] = n
        for ((id, n) in u.players) g.player(id)?.name = n
        g.fictionalNames = false
        g.reindex()
    }
}
