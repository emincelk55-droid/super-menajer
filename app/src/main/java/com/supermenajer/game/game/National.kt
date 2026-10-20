package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.NatMatch
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Team
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

    /** Lig haftası sonunda: pencere haftasıysa milli maç günü gelir (DEVAM ile canlı oynanır). */
    fun weekly(g: Game, leagueWeek: Int, rnd: Random) {
        val code = g.nationalTeam
        if (code.isEmpty() || g.fired || leagueWeek !in windows || g.natLastWeek == leagueWeek) return
        g.natLastWeek = leagueWeek
        // Önceki maç oynanmadan yeni pencere geldiyse o maç hızlıca sonuçlanır
        if (g.natPending.isNotEmpty()) simulatePending(g, rnd)
        val others = nations(g).filter { it != code }
        if (others.isEmpty()) return
        val opp = others[rnd.nextInt(others.size.coerceAtMost(16))]
        val home = rnd.nextBoolean()
        val kind = if (leagueWeek == 6 || leagueWeek == 22) "Hazırlık maçı" else "Eleme maçı"
        g.natPending = "$opp|$kind|${if (home) 1 else 0}"
        val line = if (home) "${name(code)} - ${name(opp)}" else "${name(opp)} - ${name(code)}"
        g.addNews("🌍 Milli maç günü: $line", "$kind. DEVAM ET ile maça çık; kadronu Milli Takım ekranından düzenleyebilirsin.")
    }

    /** ko: tek maç eleme (beraberlikte penaltı); tour: yaz turnuvası maçı. */
    class Pending(val opp: String, val kind: String, val home: Boolean, val ko: Boolean, val tour: Boolean)

    fun pending(g: Game): Pending? {
        val c = g.natPending.split("|")
        if (g.nationalTeam.isEmpty() || c.size < 3) return null
        return Pending(c[0], c[1], c[2] == "1", c.getOrNull(3) == "1", c.getOrNull(4) == "1")
    }

    private val shorts = mapOf(
        "TR" to "TUR", "GB" to "ENG", "ES" to "ESP", "DE" to "GER", "IT" to "ITA", "FR" to "FRA", "PT" to "POR", "NL" to "NED",
        "BR" to "BRA", "AR" to "ARG", "BE" to "BEL", "HR" to "CRO", "DK" to "DEN", "SE" to "SWE", "CH" to "SUI", "AT" to "AUT",
        "PL" to "POL", "MA" to "MAR", "SN" to "SEN", "NG" to "NGA", "JP" to "JPN", "US" to "USA", "NO" to "NOR", "RS" to "SRB",
    )
    private val colors = mapOf(
        "TR" to (0xFFE30A17 to 0xFFFFFFFF), "GB" to (0xFFFFFFFF to 0xFF1A2A5A), "ES" to (0xFFC60B1E to 0xFFFFC400),
        "DE" to (0xFFFFFFFF to 0xFF111111), "IT" to (0xFF1F5FBF to 0xFFFFFFFF), "FR" to (0xFF1A2A6C to 0xFFFFFFFF),
        "PT" to (0xFF9B1B30 to 0xFF006847), "NL" to (0xFFFF6F00 to 0xFFFFFFFF), "BR" to (0xFFFFDC02 to 0xFF009C3B),
        "AR" to (0xFF74ACDF to 0xFFFFFFFF), "BE" to (0xFFD71921 to 0xFF111111), "HR" to (0xFFE8112D to 0xFFFFFFFF),
        "DK" to (0xFFC60C30 to 0xFFFFFFFF), "MA" to (0xFFC1272D to 0xFF006233), "SN" to (0xFFFFFFFF to 0xFF00853F),
        "JP" to (0xFF1B2A7A to 0xFFFFFFFF), "US" to (0xFFFFFFFF to 0xFF0A3161), "SE" to (0xFFFECC00 to 0xFF006AA7),
    )

    /** Ulus için geçici takım nesnesi (maç motoru için). */
    fun teamFor(code: String, id: Int, formation: String = "4-2-3-1"): Team {
        val (c1, c2) = colors[code] ?: run {
            val h = code.hashCode()
            (0xFF000000 or (h.toLong() and 0xFFFFFF)) to 0xFFFFFFFF
        }
        return Team(id, name(code), shorts[code] ?: name(code).take(3).uppercase(), name(code), c1, c2, 80, 0, formation).also {
            it.stadium = 52_000
            it.stadiumName = "${name(code)} Ulusal Stadı"
        }
    }

    const val USER_ID = -1000
    const val OPP_ID = -1001

    /** Bekleyen milli maç için maç motoru. */
    fun buildMatch(g: Game, live: Boolean): MatchSim? {
        val p = pending(g) ?: return null
        val code = g.nationalTeam
        val me = teamFor(code, USER_ID, g.natFormation)
        val them = teamFor(p.opp, OPP_ID)
        val h = if (p.home) me else them
        val a = if (p.home) them else me
        val fx = Fixture(g.week, h.id, a.id, comp = Comp.NATIONAL, round = if (p.ko) 1 else 0)
        val pools = mapOf(USER_ID to squad(g), OPP_ID to autoSquad(g, p.opp))
        return MatchSim(g, fx, USER_ID, autoUserSubs = !live, nat = NatMatch(h, a, pools, if (p.tour) p.kind else "Milli Maç · ${p.kind}"))
    }

    /** Maç sonucunu işler (kendi gollerimiz x, rakip y). */
    fun record(g: Game, x: Int, y: Int, scorers: List<String> = emptyList(), won: Boolean? = null, pens: String = "") {
        val p = pending(g) ?: return
        val code = g.nationalTeam
        g.natPending = ""
        if (p.tour) Tournament.userResult(g, p, x, y, won ?: (x > y), pens, scorers)
        if (p.tour) return
        val pts = when { x > y -> 3; x == y -> 1; else -> 0 }
        g.natPoints += pts
        val line = if (p.home) "${name(code)} $x-$y ${name(p.opp)}" else "${name(p.opp)} $y-$x ${name(code)}"
        g.natResults.add(0, "${g.season}|${p.kind}|$line|$pts")
        while (g.natResults.size > 30) g.natResults.removeAt(g.natResults.lastIndex)
        val who = if (scorers.isNotEmpty()) " Golcüler: ${scorers.joinToString()}." else ""
        g.addNews(
            "🌍 Milli maç: $line",
            "${p.kind} · ${when (pts) { 3 -> "Galibiyet! Taraftar milli takımın oyunundan memnun."; 1 -> "Beraberlik."; else -> "Mağlubiyet. Basın kadro tercihlerini sorguluyor." }}$who",
        )
        if (pts == 3) Perks.addXp(g, 15)
    }

    /** Bekleyen maçı hızlıca sonuçlandırır. */
    fun simulatePending(g: Game, rnd: Random = Random.Default) {
        val sim = buildMatch(g, live = false) ?: return
        sim.runToEnd()
        sim.applyNational()
        finish(g, sim)
    }

    /** Maç motorundan gelen sonucu kaydeder. */
    fun finish(g: Game, sim: MatchSim) {
        val us = sim.userSide ?: return
        val them = sim.other(us)
        val scorers = sim.events.filter { it.type == com.supermenajer.game.engine.Ev.GOAL && it.side == us.index }
            .mapNotNull { e -> us.used.firstOrNull { it.p.id == e.playerId }?.p?.name?.substringAfterLast(' ') }
        val won = sim.cupWinner()?.let { it === us.team }
        val pens = if (sim.penHome >= 0) (if (us === sim.home) "${sim.penHome}-${sim.penAway}" else "${sim.penAway}-${sim.penHome}") else ""
        record(g, us.goals, them.goals, scorers, if (sim.isCup) won else null, pens)
    }

    /** Sezon sonu turnuvası: en güçlü 8 ulus (kendi takımın dahil) eleme usulü. */
    fun summerTournament(g: Game, rnd: Random) {
        val code = g.nationalTeam
        if (code.isEmpty() || g.tourSeason == g.season) return
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
