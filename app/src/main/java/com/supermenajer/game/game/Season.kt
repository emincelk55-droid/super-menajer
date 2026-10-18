package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Day
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.IncomingOffer
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.SeasonRecord
import com.supermenajer.game.model.Team
import kotlin.math.roundToLong
import kotlin.random.Random

object Season {
    const val LEAGUE_WEEKS = 34
    private val cupAfterWeek = listOf(5, 11, 17, 24, 31)

    val trainingNames = listOf("Dengeli", "Hücum", "Savunma", "Fizik", "Gençler")
    val intensityNames = listOf("Hafif", "Normal", "Yoğun")

    fun buildCalendar(): MutableList<Day> {
        val c = mutableListOf<Day>()
        for (w in 1..LEAGUE_WEEKS) {
            c += Day(0, w)
            val r = cupAfterWeek.indexOf(w)
            if (r >= 0) c += Day(1, r)
            val e = Europe.afterWeek.indexOf(w)
            if (e >= 0) c += Day(2, e)
        }
        return c
    }

    private fun leagueDay(cal: List<Day>, week: Int): Int = cal.indexOfFirst { it.type == 0 && it.number == week } + 1
    private fun cupDay(cal: List<Day>, round: Int): Int = cal.indexOfFirst { it.type == 1 && it.number == round } + 1

    fun makeLeagueFixtures(teamIds: List<Int>, comp: Int, cal: List<Day>, rnd: Random): List<Fixture> {
        val ids = teamIds.shuffled(rnd).toMutableList()
        val n = ids.size
        val rounds = n - 1
        val pairs = mutableListOf<Triple<Int, Int, Int>>()
        for (r in 0 until rounds) {
            for (i in 0 until n / 2) {
                val a = ids[i]
                val b = ids[n - 1 - i]
                val swap = if (i == 0) r % 2 == 1 else i % 2 == 1
                pairs += if (swap) Triple(r + 1, b, a) else Triple(r + 1, a, b)
            }
            ids.add(1, ids.removeAt(n - 1))
        }
        val all = pairs + pairs.map { Triple(it.first + rounds, it.third, it.second) }
        return all.map { (w, h, a) -> Fixture(leagueDay(cal, w), h, a, comp = comp) }
    }

    fun start(g: Game, rnd: Random = Random.Default) {
        g.teams.forEach { it.resetTable() }
        g.calendar = buildCalendar()
        g.fixtures = (
            makeLeagueFixtures(g.divisionTeams(0).map { it.id }, Comp.SUPER, g.calendar, rnd) +
                makeLeagueFixtures(g.divisionTeams(1).map { it.id }, Comp.FIRST, g.calendar, rnd)
            ).toMutableList()
        g.week = 1
        g.seasonOver = false
        g.cupWinner = -1
        g.offers.clear()
        computeExpectations(g)
        g.players.forEach { it.startOvr = it.overall }
        for (t in g.teams) {
            if (t.id == g.userTeamId && t.lineup.size == 11) Lineups.validate(g, t) else Lineups.autoPick(g, t)
        }
        val cupTeams = g.divisionTeams(0) + g.divisionTeams(1).sortedByDescending { it.reputation }.take(14)
        drawCup(g, 0, cupTeams, rnd)
        Europe.start(g, rnd)
    }

    /** Eski (v0.1) kayıtları yeni yapıya taşır. */
    fun migrate(g: Game, rnd: Random = Random.Default) {
        if (g.saveVersion < 2) migrateV2(g, rnd)
        if (g.saveVersion < 3) {
            if (g.teams.none { it.division == 2 }) Generator.addEuropeanTeams(g, rnd)
            g.saveVersion = 3
            g.addNews("Güncelleme: v0.3", "Avrupa Kupası geldi! Süper Lig'i ilk 4'te bitiren takımlar gelecek sezon Avrupa'da 28 dev kulüple yarışacak.")
        }
    }

    private fun migrateV2(g: Game, rnd: Random) {
        if (g.calendar.isEmpty()) g.calendar = MutableList(LEAGUE_WEEKS) { Day(0, it + 1) }
        if (g.teams.none { it.division == 1 }) {
            Generator.addLowerDivision(g, rnd)
            g.fixtures.addAll(makeLeagueFixtures(g.divisionTeams(1).map { it.id }, Comp.FIRST, g.calendar, rnd))
            for (t in g.divisionTeams(1)) Lineups.autoPick(g, t)
            // Geçmiş haftaları 1. Lig için hızlıca oynat.
            for (d in 1 until g.week.coerceAtMost(g.calendar.size + 1)) {
                g.weekFixtures(d).filter { !it.played }.forEach {
                    MatchSim(g, it, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame() }
                }
            }
            computeExpectations(g)
        }
        g.players.forEach { if (it.startOvr == 0) it.startOvr = it.overall }
        g.saveVersion = 2
        g.addNews(
            "Güncelleme: v0.2",
            "Yenilikler: 1. Lig, Türkiye Kupası (gelecek sezon), transfer pazarlığı ve dönemleri, antrenman, yönetim güveni. İyi oyunlar hocam!",
        )
    }

    private fun drawCup(g: Game, round: Int, teams: List<Team>, rnd: Random) {
        val day = cupDay(g.calendar, round)
        if (day <= 0) return
        val list = teams.shuffled(rnd)
        for (i in 0 until list.size / 2) {
            val a = list[2 * i]
            val b = list[2 * i + 1]
            // Küçük takım ev sahibi olur.
            val (h, aw) = if (a.reputation <= b.reputation) a to b else b to a
            g.fixtures += Fixture(day, h.id, aw.id, comp = Comp.CUP, round = round)
        }
        val us = g.userTeamId
        g.fixtures.firstOrNull { it.comp == Comp.CUP && it.round == round && it.involves(us) }?.let {
            val opp = g.team(if (it.home == us) it.away else it.home)
            g.addNews("Kupa kurası", "Türkiye Kupası ${Comp.cupRounds[round]} turunda rakibimiz: ${opp.name}.")
        }
    }

    fun computeExpectations(g: Game) {
        for (div in 0..1) {
            g.divisionTeams(div).sortedByDescending { Lineups.strength(g, it) }.forEachIndexed { i, t -> t.expected = i + 1 }
        }
    }

    fun objectiveTarget(expected: Int): Int = when {
        expected <= 1 -> 1
        expected <= 3 -> 3
        expected <= 6 -> 6
        expected <= 10 -> 10
        else -> 15
    }

    fun objectiveText(t: Team): String = when (objectiveTarget(t.expected)) {
        1 -> if (t.division == 0) "Şampiyonluk" else "Şampiyon olarak yükselmek"
        3 -> if (t.division == 0) "İlk 3" else "Süper Lig'e yükselmek (ilk 3)"
        6 -> "İlk 6"
        10 -> "İlk 10"
        else -> if (t.division == 0) "Ligde kalmak" else "Orta sıralar (ilk 15)"
    }

    fun transferWindowOpen(g: Game): Boolean {
        if (g.seasonOver) return true
        val w = g.leagueWeekAt(g.week)
        return w < 4 || w in 16..18
    }

    fun windowText(g: Game): String = when {
        transferWindowOpen(g) -> "Transfer dönemi AÇIK" + if (!g.seasonOver) " (${if (g.leagueWeekAt(g.week) < 4) "4" else "19"}. haftaya kadar)" else ""
        g.leagueWeekAt(g.week) < 16 -> "Transfer dönemi kapalı · Ara transfer 17. haftada açılır"
        else -> "Transfer dönemi kapalı · Sezon sonunda açılır"
    }

    /** Günün kalan maçlarını hızlıca oynatır. */
    fun simulateRest(g: Game, day: Int, rnd: Random = Random.Default) {
        for (f in g.weekFixtures(day)) {
            if (f.played) continue
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame() }
        }
    }

    /** Kullanıcı maçından sonra haber ve yönetim güveni. */
    fun matchNews(g: Game, sim: MatchSim) {
        val us = sim.userSide ?: return
        val them = sim.other(us)
        val h = sim.home
        val a = sim.away
        val score = "${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}" +
            if (sim.penHome >= 0) " (Penaltılar ${sim.penHome}-${sim.penAway})" else ""
        val won = if (sim.isCup) sim.cupWinner()?.id == us.team.id else us.goals > them.goals
        val euro = sim.fixture.comp == Comp.EURO
        val title = when {
            euro && sim.isCup && won -> "Avrupa'da tur atladık!"
            euro && sim.isCup -> "Avrupa'ya veda ettik"
            euro && us.goals > them.goals -> "Avrupa'da galibiyet!"
            euro && us.goals == them.goals -> "Avrupa'da beraberlik"
            euro -> "Avrupa'da mağlubiyet"
            sim.isCup && won -> "Kupada tur atladık!"
            sim.isCup -> "Kupaya veda ettik"
            us.goals - them.goals >= 3 -> "Farklı galibiyet!"
            us.goals > them.goals -> "Galibiyet!"
            us.goals == them.goals -> "Beraberlik"
            else -> "Mağlubiyet"
        }
        val scorers = us.used.filter { it.goals > 0 }.joinToString { if (it.goals > 1) "${it.p.name} (${it.goals})" else it.p.name }
        val motm = sim.manOfTheMatch()
        val body = buildString {
            append(score)
            if (scorers.isNotEmpty()) append(". Gollerimiz: $scorers")
            motm?.let { append(". Maçın oyuncusu: ${it.p.name} (${"%.1f".format(it.rating)})") }
        }
        g.addNews(title, body)
        for (m in us.used) {
            if (m.injured) g.addNews("Sakatlık", "${m.p.name} sakatlandı ve yaklaşık ${m.injWeeks - 1} hafta forma giyemeyecek.")
            if (m.red) g.addNews("Kırmızı kart", "${m.p.name} kırmızı kart nedeniyle cezalı duruma düştü.")
        }
        // Yönetim güveni
        val t = us.team
        var delta = if (euro) {
            if (sim.isCup) (if (won) 4.0 else 0.0) else if (us.goals > them.goals) 2.0 else if (us.goals < them.goals) -0.5 else 0.5
        } else if (sim.isCup) {
            if (won) 2.0 else if (t.expected <= 3 && sim.fixture.round <= 1) -6.0 else -1.0
        } else when {
            us.goals > them.goals -> 3.0
            us.goals == them.goals -> 0.0
            else -> -3.5
        }
        if (!sim.isCup && !euro && g.leagueWeekAt(g.week) >= 6) {
            val pos = g.position(t.id)
            val target = objectiveTarget(t.expected)
            delta += when {
                pos <= target -> 1.0
                pos <= target + 3 -> -0.5
                else -> -2.0
            }
        }
        g.confidence = (g.confidence + delta).coerceIn(0.0, 100.0)
    }

    fun endDay(g: Game, rnd: Random = Random.Default) {
        val d = g.today ?: return
        val dayFixtures = g.weekFixtures(g.week)
        for (f in dayFixtures) {
            val h = g.team(f.home)
            h.budget += 50_000L + h.reputation * 2_500L
            if (f.comp == Comp.EURO) h.budget += 400_000L
            if (f.comp == Comp.CUP) {
                val prize = listOf(150_000L, 250_000L, 400_000L, 700_000L, 1_500_000L)[f.round]
                g.team(f.winner).budget += prize
            }
        }
        if (d.type == 2) {
            Europe.advance(g, d.number, rnd)
        } else if (d.type == 0) {
            weeklyUpdate(g, rnd)
            if (d.number % 6 == 0) {
                val leader = g.table(g.userTeam.division).first()
                g.addNews("Lig lideri", "${d.number}. hafta sonunda ${leader.name} ${leader.points} puanla zirvede.")
            }
        } else {
            advanceCup(g, d.number, rnd)
        }
        for (p in g.players) p.fitness = (p.fitness + recovery(g, p)).coerceAtMost(100.0)
        if (transferWindowOpen(g)) {
            aiTransfers(g, rnd)
            makeIncomingOffer(g, rnd)
        }
        g.offers.removeAll { g.week - it.day >= 4 || g.player(it.playerId)?.teamId != g.userTeamId }
        g.week++
        if (g.week > g.totalDays) seasonEnd(g)
        if (!g.fired && g.confidence <= 5 && (g.seasonOver || g.leagueWeekAt(g.week) >= 8)) fire(g, rnd)
    }

    private fun recovery(g: Game, p: Player): Double {
        if (p.teamId < 0) return 30.0
        return when (g.team(p.teamId).intensity) { 0 -> 28.0; 2 -> 16.0; else -> 22.0 }
    }

    private fun weeklyUpdate(g: Game, rnd: Random) {
        for (t in g.teams) t.budget += 30_000L - g.squad(t.id).sumOf { it.wage }
        for (p in g.players) {
            p.morale += (70 - p.morale) * 0.08
            if (p.injuredWeeks > 0) p.injuredWeeks--
            if (p.suspended > 0) p.suspended--
            val team = if (p.teamId >= 0) g.team(p.teamId) else null
            val focus = team?.training ?: 0
            val intensity = team?.intensity ?: 1
            val intF = when (intensity) { 0 -> 0.7; 2 -> 1.4; else -> 1.0 }
            val youthF = if (focus == 4) (if (p.age <= 21) 1.8 else 0.7) else 1.0
            val gap = p.potential - p.overall
            if (p.age <= 24 && gap > 0 && rnd.nextDouble() < (0.14 + gap * 0.01) * intF * youthF) {
                improve(p, 1, rnd, focus)
            } else if (p.age in 25..29 && gap > 0 && rnd.nextDouble() < 0.05 * intF) {
                improve(p, 1, rnd, focus)
            } else if (p.age >= 31 && rnd.nextDouble() < (0.04 + (p.age - 31) * 0.02) / intF.coerceAtMost(1.0)) {
                decline(p, 1, rnd)
            }
            // Antrenman sakatlığı
            if (team != null && p.injuredWeeks == 0) {
                val risk = when (intensity) { 0 -> 0.002; 2 -> 0.012; else -> 0.005 }
                if (rnd.nextDouble() < risk) {
                    p.injuredWeeks = rnd.nextInt(1, 4)
                    if (team.id == g.userTeamId) g.addNews("Antrenmanda sakatlık", "${p.name} antrenmanda sakatlandı (${p.injuredWeeks} hafta).")
                }
            }
        }
    }

    private fun advanceCup(g: Game, round: Int, rnd: Random) {
        val winners = g.fixtures.filter { it.comp == Comp.CUP && it.round == round }.mapNotNull { f -> f.winner.takeIf { it >= 0 } }
        if (round == Comp.cupRounds.lastIndex) {
            val w = winners.firstOrNull() ?: return
            g.cupWinner = w
            val t = g.team(w)
            t.budget += 3_000_000L
            t.reputation = (t.reputation + 2).coerceAtMost(95)
            g.addNews("Türkiye Kupası sahibini buldu!", "${t.name} kupayı müzesine götürdü! 🏆")
            if (w == g.userTeamId) {
                g.confidence = (g.confidence + 15).coerceAtMost(100.0)
                g.addNews("KUPA BİZİM!", "Tebrikler hocam! Türkiye Kupası'nı kazandık! Taraftar sokaklarda!")
            }
        } else {
            drawCup(g, round + 1, winners.map { g.team(it) }, rnd)
        }
    }

    fun improve(p: Player, n: Int, rnd: Random, focus: Int = 0) {
        repeat(n) {
            if (p.pos == Pos.GK) {
                if (focus == 3 && rnd.nextInt(3) == 0) p.physical++ else p.goalkeeping++
                return@repeat
            }
            when (focus) {
                1 -> when (rnd.nextInt(3)) { 0 -> p.shooting++; 1 -> p.dribbling++; else -> p.passing++ }
                2 -> when (rnd.nextInt(3)) { 0 -> p.defending++; 1 -> p.defending++; else -> p.physical++ }
                3 -> when (rnd.nextInt(2)) { 0 -> p.pace++; else -> p.physical++ }
                else -> when (p.pos) {
                    Pos.DF -> when (rnd.nextInt(4)) { 0 -> p.defending++; 1 -> p.physical++; 2 -> p.pace++; else -> p.passing++ }
                    Pos.MF -> when (rnd.nextInt(5)) { 0 -> p.passing++; 1 -> p.dribbling++; 2 -> p.defending++; 3 -> p.shooting++; else -> p.physical++ }
                    else -> when (rnd.nextInt(4)) { 0 -> p.shooting++; 1 -> p.dribbling++; 2 -> p.pace++; else -> p.physical++ }
                }
            }
        }
        clamp(p)
    }

    fun decline(p: Player, n: Int, rnd: Random) {
        repeat(n) {
            if (p.pos == Pos.GK) {
                if (rnd.nextBoolean()) p.goalkeeping-- else p.physical--
            } else when (rnd.nextInt(3)) {
                0 -> p.pace--
                1 -> p.physical--
                else -> when (p.pos) { Pos.DF -> p.defending--; Pos.MF -> p.passing--; else -> p.shooting-- }
            }
        }
        clamp(p)
    }

    private fun clamp(p: Player) {
        p.pace = p.pace.coerceIn(10, 99); p.shooting = p.shooting.coerceIn(10, 99)
        p.passing = p.passing.coerceIn(10, 99); p.dribbling = p.dribbling.coerceIn(10, 99)
        p.defending = p.defending.coerceIn(10, 99); p.physical = p.physical.coerceIn(10, 99)
        p.goalkeeping = p.goalkeeping.coerceIn(5, 99)
    }

    private fun r25k(v: Double): Long = (v / 25_000).roundToLong() * 25_000

    /** Yapay zekâ kulüplerinin kendi aralarındaki transferleri. */
    private fun aiTransfers(g: Game, rnd: Random) {
        repeat(2) {
            if (rnd.nextDouble() > 0.45) return@repeat
            val buyer = g.teams.filter { it.id != g.userTeamId && it.budget > 2_000_000 }.randomOrNull(rnd) ?: return@repeat
            val squad = g.squad(buyer.id)
            if (squad.size >= 28) return@repeat
            // En zayıf mevkiyi bul
            val need = mapOf(Pos.GK to 1, Pos.DF to 4, Pos.MF to 4, Pos.FW to 2)
            val weakest = need.minBy { (pos, c) ->
                squad.filter { it.pos == pos }.sortedByDescending { it.overall }.take(c).minOfOrNull { it.overall } ?: 0
            }.key
            val bar = squad.filter { it.pos == weakest }.sortedByDescending { it.overall }.take(need.getValue(weakest)).minOfOrNull { it.overall } ?: 0
            val maxPrice = buyer.budget * 0.6
            val candidates = g.players.filter { p ->
                p.pos == weakest && p.teamId != buyer.id && p.teamId != g.userTeamId && p.overall >= bar + 2 && p.age <= 31 &&
                    (p.teamId < 0 || (g.team(p.teamId).reputation <= buyer.reputation + 4 && g.squad(p.teamId).size > 19)) &&
                    p.value * (if (p.teamId < 0) 0.15 else 1.15) <= maxPrice
            }.sortedByDescending { it.overall }.take(5)
            val p = candidates.randomOrNull(rnd) ?: return@repeat
            val price = r25k(p.value * if (p.teamId < 0) 0.15 else rnd.nextDouble(1.0, 1.3))
            val from = if (p.teamId >= 0) g.team(p.teamId) else null
            from?.let { it.budget += price; it.lineup.replaceAll { id -> if (id == p.id) -1 else id } }
            buyer.budget -= price
            p.teamId = buyer.id
            if (p.overall >= 66 || price >= 1_000_000) {
                g.addNews("Transfer", "${p.name} (${p.overall}) → ${buyer.name}" + (from?.let { " · ${it.name} kasasına ${money(price)} girdi" } ?: " · serbest oyuncu olarak imzaladı"))
            }
        }
    }

    /** Kullanıcının oyuncularına gelen teklifler. */
    private fun makeIncomingOffer(g: Game, rnd: Random) {
        if (g.offers.size >= 3 || rnd.nextDouble() > 0.25) return
        val us = g.userTeam
        val sq = g.squad(us.id).filter { it.age >= 19 && g.offers.none { o -> o.playerId == it.id } }.sortedByDescending { it.overall }.take(16)
        val p = sq.randomOrNull(rnd) ?: return
        val price = r25k(p.value * rnd.nextDouble(0.9, 1.35))
        val buyer = g.teams.filter { it.id != us.id && it.budget >= price && it.reputation >= us.reputation - 12 && g.squad(it.id).size < 28 }
            .randomOrNull(rnd) ?: return
        g.offers += IncomingOffer(p.id, buyer.id, price, g.week)
        g.addNews("Transfer teklifi!", "${buyer.name}, ${p.name} için ${money(price)} teklif etti. Ana sayfadan yanıt verebilirsin.")
    }

    private fun fire(g: Game, rnd: Random) {
        g.fired = true
        val cur = g.userTeam
        g.jobOffers = g.teams.filter { it.division < 2 && it.id != cur.id && it.reputation <= cur.reputation - 3 }
            .shuffled(rnd).take(3).map { it.id }.toMutableList()
        if (g.jobOffers.isEmpty()) g.jobOffers = g.teams.filter { it.division < 2 && it.id != cur.id }.sortedBy { it.reputation }.take(3).map { it.id }.toMutableList()
        g.addNews("KOVULDUN!", "${cur.name} yönetimi kötü sonuçlar nedeniyle yollarını seninle ayırdı. Masada yeni teklifler var.")
    }

    fun takeJob(g: Game, teamId: Int) {
        val old = g.userTeam.name
        g.userTeamId = teamId
        g.fired = false
        g.jobOffers.clear()
        g.offers.clear()
        g.confidence = 55.0
        Lineups.validate(g, g.userTeam)
        g.addNews("Yeni görev", "$old macerası bitti. Artık ${g.userTeam.name} teknik direktörüsün! Hedef: ${objectiveText(g.userTeam)}.")
    }

    private fun seasonEnd(g: Game) {
        g.seasonOver = true
        val us = g.userTeam
        for (div in 0..1) {
            val table = g.table(div)
            table.forEachIndexed { i, t ->
                t.budget += (if (div == 0) 250_000L else 100_000L) * (table.size - i) + if (i == 0) (if (div == 0) 3_000_000L else 1_000_000L) else 0L
                t.reputation = (t.reputation + (t.expected - (i + 1)) / 3).coerceIn(30, 95)
            }
        }
        val sl = g.table(0)
        val fl = g.table(1)
        val userPos = g.position(us.id)
        val top = g.players.filter { it.teamId >= 0 && g.team(it.teamId).division == 0 }.maxByOrNull { it.goals }
        val topText = top?.let { "${it.name} (${it.goals} gol)" } ?: "-"
        g.history += SeasonRecord(
            g.season, sl.first().name, us.name, userPos, topText,
            cupWinner = if (g.cupWinner >= 0) g.team(g.cupWinner).name else "",
            firstChampion = fl.first().name, userDivision = us.division,
            euroWinner = if (g.euroWinner >= 0) g.team(g.euroWinner).name else "",
        )
        g.euroQualified = sl.take(4).map { it.id }.toMutableList()
        if (us.id in g.euroQualified) g.addNews("AVRUPA'DAYIZ!", "${us.name} gelecek sezon Avrupa Kupası'nda mücadele edecek!")
        g.addNews("Sezon sona erdi", "${g.season} Süper Lig şampiyonu ${sl.first().name}! Gol kralı: $topText. Düşenler: ${sl.takeLast(3).joinToString { it.name }}. Yükselenler: ${fl.take(3).joinToString { it.name }}.")
        if (sl.first().id == us.id) g.addNews("ŞAMPİYONUZ!", "Tebrikler hocam! ${us.name} Süper Lig şampiyonu!")
        val target = objectiveTarget(us.expected)
        g.confidence = (g.confidence + when {
            userPos <= target -> 25.0
            userPos <= target + 3 -> -10.0
            else -> -30.0
        }).coerceIn(0.0, 100.0)
    }

    fun newSeason(g: Game, rnd: Random = Random.Default) {
        // Küme düşme / yükselme
        val down = g.table(0).takeLast(3)
        val up = g.table(1).take(3)
        val us = g.userTeam
        down.forEach { it.division = 1; it.reputation = (it.reputation - 4).coerceAtLeast(30) }
        up.forEach { it.division = 0; it.reputation = (it.reputation + 4).coerceAtMost(95) }
        if (us in down) g.addNews("Küme düştük", "${us.name} gelecek sezon 1. Lig'de mücadele edecek. Hemen geri dönmeliyiz!")
        if (us in up) g.addNews("SÜPER LİG'E YÜKSELDİK!", "${us.name} artık Süper Lig'de! Tarihi bir başarı hocam!")

        val retired = mutableListOf<Player>()
        for (p in g.players.toList()) {
            p.age++
            val gap = p.potential - p.overall
            when {
                p.age <= 23 && gap > 0 -> improve(p, (gap / 3).coerceIn(1, 6) + rnd.nextInt(0, 2), rnd)
                p.age <= 27 && gap > 0 -> improve(p, rnd.nextInt(0, 3), rnd)
                p.age >= 31 -> decline(p, rnd.nextInt(1, 2 + (p.age - 30)), rnd)
            }
            if (p.age >= 36 || (p.age >= 33 && rnd.nextDouble() < 0.25 + (p.age - 33) * 0.15)) {
                retired += p
                continue
            }
            p.apps = 0; p.goals = 0; p.assists = 0; p.ratingSum = 0.0
            p.yellowCards = 0; p.suspended = 0; p.fitness = 100.0
        }
        for (p in retired) {
            if (p.teamId == g.userTeamId) g.addNews("Jübile", "${p.name} ${p.age} yaşında futbolu bıraktı.")
            g.players.remove(p)
        }
        val retiredIds = retired.map { it.id }.toSet()
        g.teams.forEach { t -> t.lineup.replaceAll { if (it in retiredIds) -1 else it } }
        for (t in g.teams) {
            while (g.squad(t.id).size < 22) {
                val sq = g.squad(t.id)
                val pos = Generator.squadPlan.maxBy { (pos, n) -> n - sq.count { it.pos == pos } }.key
                val y = Generator.youth(g, t, pos, rnd)
                g.players += y
                if (t.id == g.userTeamId) g.addNews("Altyapıdan yeni yetenek", "${y.name} (${y.age}, ${pos.label}) A takıma yükseldi. Potansiyeli yüksek görünüyor!")
            }
        }
        Generator.topUpFreeAgents(g, rnd)
        g.season++
        start(g, rnd)
        g.addNews("Yeni sezon", "${g.season} sezonu başlıyor! Yönetimin hedefi: ${objectiveText(g.userTeam)}. Transfer dönemi açık.")
    }
}
