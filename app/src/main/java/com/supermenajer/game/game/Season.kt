package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.SeasonRecord
import kotlin.random.Random

object Season {
    fun makeFixtures(teamIds: List<Int>, rnd: Random): MutableList<Fixture> {
        val ids = teamIds.shuffled(rnd).toMutableList()
        val n = ids.size
        val rounds = n - 1
        val first = mutableListOf<Fixture>()
        for (r in 0 until rounds) {
            for (i in 0 until n / 2) {
                val a = ids[i]
                val b = ids[n - 1 - i]
                val swap = if (i == 0) r % 2 == 1 else i % 2 == 1
                first += if (swap) Fixture(r + 1, b, a) else Fixture(r + 1, a, b)
            }
            ids.add(1, ids.removeAt(n - 1))
        }
        val second = first.map { Fixture(it.week + rounds, it.away, it.home) }
        return (first + second).toMutableList()
    }

    fun start(g: Game, rnd: Random = Random.Default) {
        g.teams.forEach { it.resetTable() }
        g.fixtures = makeFixtures(g.teams.map { it.id }, rnd)
        g.week = 1
        g.seasonOver = false
        computeExpectations(g)
        for (t in g.teams) {
            if (t.id == g.userTeamId && t.lineup.size == 11) Lineups.validate(g, t) else Lineups.autoPick(g, t)
        }
    }

    fun computeExpectations(g: Game) {
        g.teams.sortedByDescending { Lineups.strength(g, it) }.forEachIndexed { i, t -> t.expected = i + 1 }
    }

    fun objectiveTarget(expected: Int): Int = when {
        expected <= 1 -> 1
        expected <= 3 -> 3
        expected <= 6 -> 6
        expected <= 10 -> 10
        else -> 15
    }

    fun objectiveText(expected: Int): String = when (objectiveTarget(expected)) {
        1 -> "Şampiyonluk"
        3 -> "İlk 3"
        6 -> "İlk 6"
        10 -> "İlk 10"
        else -> "Ligde kalmak"
    }

    /** Haftanın kalan maçlarını hızlıca oynatır. */
    fun simulateRest(g: Game, week: Int, rnd: Random = Random.Default) {
        for (f in g.weekFixtures(week)) {
            if (f.played) continue
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame() }
        }
    }

    /** Kullanıcı maçından sonra haber üretir. */
    fun matchNews(g: Game, sim: MatchSim) {
        val us = sim.userSide ?: return
        val them = sim.other(us)
        val h = sim.home
        val a = sim.away
        val score = "${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}"
        val title = when {
            us.goals > them.goals && us.goals - them.goals >= 3 -> "Farklı galibiyet!"
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
    }

    fun endWeek(g: Game, rnd: Random = Random.Default) {
        val w = g.week
        for (f in g.weekFixtures(w)) {
            val h = g.team(f.home)
            h.budget += 50_000L + h.reputation * 2_500L
        }
        for (t in g.teams) t.budget += 30_000L - g.squad(t.id).sumOf { it.wage }
        for (p in g.players) {
            p.fitness = (p.fitness + 22).coerceAtMost(100.0)
            p.morale += (70 - p.morale) * 0.08
            if (p.injuredWeeks > 0) p.injuredWeeks--
            if (p.suspended > 0) p.suspended--
            if (p.age <= 24 && p.overall < p.potential && rnd.nextDouble() < 0.14 + (p.potential - p.overall) * 0.01) {
                improve(p, 1, rnd)
            } else if (p.age >= 31 && rnd.nextDouble() < 0.04 + (p.age - 31) * 0.02) {
                decline(p, 1, rnd)
            }
        }
        g.week++
        if (w % 6 == 0 && g.week <= g.totalWeeks) {
            val leader = g.table().first()
            g.addNews("Lig lideri", "$w. hafta sonunda zirvede ${leader.points} puanla ${leader.name} var.")
        }
        if (g.week > g.totalWeeks) seasonEnd(g)
    }

    fun improve(p: Player, n: Int, rnd: Random) {
        repeat(n) {
            when (p.pos) {
                Pos.GK -> p.goalkeeping++
                Pos.DF -> when (rnd.nextInt(4)) { 0 -> p.defending++; 1 -> p.physical++; 2 -> p.pace++; else -> p.passing++ }
                Pos.MF -> when (rnd.nextInt(5)) { 0 -> p.passing++; 1 -> p.dribbling++; 2 -> p.defending++; 3 -> p.shooting++; else -> p.physical++ }
                Pos.FW -> when (rnd.nextInt(4)) { 0 -> p.shooting++; 1 -> p.dribbling++; 2 -> p.pace++; else -> p.physical++ }
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

    private fun seasonEnd(g: Game) {
        val table = g.table()
        val champ = table.first()
        val userPos = table.indexOfFirst { it.id == g.userTeamId } + 1
        table.forEachIndexed { i, t ->
            t.budget += (table.size - i) * 250_000L + if (i == 0) 3_000_000L else 0L
            t.reputation = (t.reputation + (t.expected - (i + 1)) / 3).coerceIn(45, 95)
        }
        val top = g.players.filter { it.teamId >= 0 }.maxByOrNull { it.goals }
        val topText = top?.let { "${it.name} (${it.goals} gol)" } ?: "-"
        g.history += SeasonRecord(g.season, champ.name, g.userTeam.name, userPos, topText)
        g.seasonOver = true
        g.addNews("Sezon sona erdi", "${g.season} sezonunun şampiyonu ${champ.name}! Gol kralı: $topText.")
        if (champ.id == g.userTeamId) g.addNews("ŞAMPİYONUZ!", "Tebrikler hocam! ${champ.name} ligin zirvesinde sezonu tamamladı!")
    }

    fun newSeason(g: Game, rnd: Random = Random.Default) {
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
        g.teams.forEach { t -> t.lineup.removeAll { id -> retired.any { it.id == id } } }
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
        g.addNews("Yeni sezon", "${g.season} sezonu başlıyor! Yönetimin hedefi: ${objectiveText(g.userTeam.expected)}.")
    }
}
