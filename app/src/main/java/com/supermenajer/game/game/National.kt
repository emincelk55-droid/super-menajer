package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.math.exp
import kotlin.random.Random

/**
 * Milli takım kariyeri (FM'deki "Kulüp ve Uluslararası" gibi): kulüple birlikte bir milli
 * takımı da çalıştırırsın. Lig takviminin belirli haftalarında milli maç penceresi açılır;
 * sezon sonunda Avrupa Şampiyonası / Dünya Kupası oynanır.
 */
object National {
    val names = mapOf(
        "TR" to "Türkiye", "GB" to "İngiltere", "ES" to "İspanya", "DE" to "Almanya", "IT" to "İtalya", "FR" to "Fransa",
        "PT" to "Portekiz", "NL" to "Hollanda", "BR" to "Brezilya", "AR" to "Arjantin", "BE" to "Belçika", "HR" to "Hırvatistan",
        "UY" to "Uruguay", "CO" to "Kolombiya", "SN" to "Senegal", "MA" to "Fas", "NG" to "Nijerya", "CI" to "Fildişi Sahili",
        "DK" to "Danimarka", "SE" to "İsveç", "NO" to "Norveç", "CH" to "İsviçre", "AT" to "Avusturya", "PL" to "Polonya",
        "RS" to "Sırbistan", "US" to "ABD", "JP" to "Japonya", "KR" to "Güney Kore", "GH" to "Gana", "CM" to "Kamerun",
        "DZ" to "Cezayir", "MX" to "Meksika", "EC" to "Ekvador", "CZ" to "Çekya", "UA" to "Ukrayna", "GR" to "Yunanistan",
        "SC" to "İskoçya", "WL" to "Galler", "IE" to "İrlanda", "HU" to "Macaristan", "SK" to "Slovakya", "SI" to "Slovenya",
        "AL" to "Arnavutluk", "GE" to "Gürcistan", "BA" to "Bosna-Hersek", "PY" to "Paraguay", "ML" to "Mali", "CD" to "Kongo DC", "XK" to "Kosova", "RO" to "Romanya", "CL" to "Şili", "AU" to "Avustralya",
    )

    fun name(code: String) = names[code] ?: code

    /** Milli maç pencereleri (lig haftası). */
    val windows = setOf(6, 13, 22, 29)

    /** Kadro kurabilecek kadar oyuncusu olan uluslar, güce göre sıralı. */
    fun nations(g: Game): List<String> =
        g.players.asSequence().filter { it.teamId >= 0 }.groupBy { it.nation }
            .filter { it.value.size >= 18 }.keys.sortedByDescending { strengthOf(g, it) }

    private fun pool(g: Game, code: String): List<Player> = g.players.filter { it.nation == code && it.teamId >= 0 && it.injuredWeeks == 0 }

    /** Otomatik kadro: 3 kaleci + en iyi 20 saha oyuncusu. */
    fun autoSquad(g: Game, code: String): List<Player> {
        val p = pool(g, code)
        return p.filter { it.pos == Pos.GK }.sortedByDescending { it.overall }.take(3) +
            p.filter { it.pos != Pos.GK }.sortedByDescending { it.overall }.take(20)
    }

    /** Kullanıcının çağırdığı kadro (geçersizler atılır, eksik kalırsa otomatik tamamlanır). */
    fun squad(g: Game): List<Player> {
        val code = g.nationalTeam
        val mine = g.natSquad.mapNotNull { g.player(it) }.filter { it.nation == code && it.teamId >= 0 }
        if (mine.size >= 16) return mine
        return (mine + autoSquad(g, code).filter { it !in mine }).take(23)
    }

    /** İlk 11'in ortalaması (1 kaleci + 10 saha oyuncusu). */
    fun strengthOf(g: Game, code: String, squad: List<Player>? = null): Double {
        val s = squad ?: autoSquad(g, code)
        val gk = s.filter { it.pos == Pos.GK }.maxOfOrNull { it.overall } ?: 50
        val out = s.filter { it.pos != Pos.GK }.sortedByDescending { it.overall }.take(10).map { it.overall }
        if (out.isEmpty()) return 40.0
        return (gk + out.sum()) / (out.size + 1.0)
    }

    /** Görevi almak için gereken menajer itibarı: en güçlü uluslar daha zor. */
    fun requiredRep(g: Game, code: String): Int = requiredRep(nations(g).indexOf(code))
    fun requiredRep(rank: Int): Int = when { rank in 0..3 -> 70; rank in 4..9 -> 50; else -> 30 }

    private fun goals(rnd: Random, lambda: Double): Int {
        // Poisson
        val l = exp(-lambda)
        var k = 0
        var p = 1.0
        do { k++; p *= rnd.nextDouble() } while (p > l)
        return k - 1
    }

    /** İki ulus arasında maç; skor döner. */
    fun play(a: Double, b: Double, rnd: Random, homeAdv: Double = 0.0): Pair<Int, Int> {
        val d = a + homeAdv - b
        return goals(rnd, 1.3 * exp(d / 9.0)).coerceAtMost(7) to goals(rnd, 1.3 * exp(-d / 9.0)).coerceAtMost(7)
    }

    /** Lig haftası sonunda: pencere haftasıysa bir milli maç oynanır. */
    fun weekly(g: Game, leagueWeek: Int, rnd: Random) {
        val code = g.nationalTeam
        if (code.isEmpty() || g.fired || leagueWeek !in windows || g.natLastWeek == leagueWeek) return
        g.natLastWeek = leagueWeek
        val others = nations(g).filter { it != code }
        if (others.isEmpty()) return
        val opp = others[rnd.nextInt(others.size.coerceAtMost(16))]
        val sq = squad(g)
        val me = strengthOf(g, code, sq) * Difficulty.matchFactor(g) * Perks.tactics(g)
        val home = rnd.nextBoolean()
        val (x, y) = play(me, strengthOf(g, opp), rnd, if (home) 1.5 else -1.5)
        val kind = if (leagueWeek == 6 || leagueWeek == 22) "Hazırlık maçı" else "Eleme maçı"
        val pts = when { x > y -> 3; x == y -> 1; else -> 0 }
        g.natPoints += pts
        val line = if (home) "${name(code)} $x-$y ${name(opp)}" else "${name(opp)} $y-$x ${name(code)}"
        g.natResults.add(0, "${g.season}|$kind|$line|$pts")
        while (g.natResults.size > 30) g.natResults.removeAt(g.natResults.lastIndex)
        val scorers = sq.filter { it.pos == Pos.FW || it.pos == Pos.MF }.sortedByDescending { it.shooting }.take(4)
        val who = if (x > 0 && scorers.isNotEmpty()) " Golcüler: " + (1..x).map { scorers[rnd.nextInt(scorers.size)].name.substringAfterLast(' ') }.distinct().joinToString() + "." else ""
        g.addNews(
            "🌍 Milli maç: $line",
            "$kind · ${when (pts) { 3 -> "Galibiyet! Taraftar milli takımın oyunundan memnun."; 1 -> "Beraberlik."; else -> "Mağlubiyet. Basın kadro tercihlerini sorguluyor." }}$who",
        )
        if (pts == 3) Perks.addXp(g, 15)
        // Milli takımda oynayan kulüp oyuncularımız biraz yorulur
        sq.filter { it.teamId == g.userTeamId }.forEach { it.fitness = (it.fitness - 8).coerceAtLeast(40.0) }
    }

    /** Sezon sonu turnuvası: en güçlü 8 ulus (kendi takımın dahil) eleme usulü. */
    fun summerTournament(g: Game, rnd: Random) {
        val code = g.nationalTeam
        if (code.isEmpty()) return
        val title = if (g.season % 2 == 0) "Avrupa Şampiyonası" else "Dünya Kupası"
        val field = (listOf(code) + nations(g).filter { it != code }.take(7)).shuffled(rnd).toMutableList()
        val str = field.associateWith { if (it == code) strengthOf(g, it, squad(g)) * Perks.tactics(g) else strengthOf(g, it) }
        var round = field
        var reached = "çeyrek finalde elendi"
        val stages = listOf("çeyrek finalde elendi", "yarı finalde elendi", "finalde kaybetti")
        var stage = 0
        while (round.size > 1) {
            val next = mutableListOf<String>()
            for (i in round.indices step 2) {
                val a = round[i]
                val b = round[i + 1]
                var (x, y) = play(str.getValue(a), str.getValue(b), rnd)
                if (x == y) { if (rnd.nextDouble() < 0.5 + (str.getValue(a) - str.getValue(b)) / 40.0) x++ else y++ }
                val w = if (x > y) a else b
                next += w
                if (a == code || b == code) {
                    g.natResults.add(0, "${g.season}|$title|${name(a)} $x-$y ${name(b)}|${if (w == code) 3 else 0}")
                    if (w != code) reached = stages[stage]
                }
            }
            round = next
            stage++
        }
        val champ = round.first()
        if (champ == code) {
            g.trophies += "${g.season} $title (${name(code)})"
            g.managerRep = (g.managerRep + 12).coerceAtMost(100)
            Perks.addXp(g, 150)
            g.addNews("🏆 ŞAMPİYONUZ!", "${name(code)}, $title'nı kazandı! Menajer olarak tarihe geçtin.")
        } else if (code in field) {
            g.addNews("🌍 $title", "${name(code)} turnuvada $reached. Şampiyon: ${name(champ)}.")
        }
    }
}
