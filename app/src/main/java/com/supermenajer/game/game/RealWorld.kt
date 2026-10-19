package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Team
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Gerçek dünya verisi: resources/world/L<lig>.txt dosyalarındaki güncel kadrolar,
 * teknik direktörler ve stadyumlar. Güçler ve roller gerçeğe göre; isimler [Alias] ile
 * gerçeğe benzer (birebir aynı değil). Biçim için world/FORMAT.md.
 */
object RealWorld {
    const val SEASON_YEAR = 2026

    class PlayerRow(
        val number: Int, val name: String, val birth: Int, val nation: String, val det: Positions.Det, val det2: Positions.Det?,
        val foot: Int, val ovr: Int, val pot: Int, val attrs: IntArray,
        /** Oynayabildiği tüm mevkiler (ana mevki dahil). */
        val all: List<Positions.Det> = emptyList(),
    )

    class TeamRow(
        val name: String, val short: String, val city: String, val stadium: String, val capacity: Int,
        val coach: String, val coachNation: String, val c1: Long, val c2: Long, val rep: Int,
        val players: MutableList<PlayerRow> = mutableListOf(),
    )

    class LeagueData(val id: Int, val season: String, val name: String, val teams: List<TeamRow>)

    private val cache = HashMap<Int, LeagueData?>()

    @Synchronized
    fun load(id: Int): LeagueData? = cache.getOrPut(id) {
        val stream = RealWorld::class.java.getResourceAsStream("/world/L$id.txt") ?: return@getOrPut null
        runCatching { parse(id, stream.bufferedReader(Charsets.UTF_8).readLines()) }.getOrNull()
    }

    /** Ligin gerçek verisi kullanılabilir mi (18 takım, her biri yeterli kadro). */
    fun has(id: Int): Boolean = load(id)?.let { d -> d.teams.size >= 18 && d.teams.take(18).all { it.players.size >= 18 } } == true

    private fun color(s: String): Long = 0xFF000000 or s.removePrefix("#").toLong(16)

    fun parse(id: Int, lines: List<String>): LeagueData {
        var season = ""
        var lname = ""
        val teams = mutableListOf<TeamRow>()
        val dets = Positions.Det.entries.associateBy { it.name }
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val f = line.split("|")
            when (f[0]) {
                "LEAGUE" -> { season = f.getOrElse(2) { "" }; lname = f.getOrElse(3) { "" } }
                "TEAM" -> if (f.size >= 11) teams += TeamRow(
                    f[1], f[2], f[3], f[4], f[5].toIntOrNull() ?: 15_000, f[6], f[7], color(f[8]), color(f[9]), f[10].toIntOrNull() ?: 60,
                )
                "P" -> {
                    if (f.size < 11 || teams.isEmpty()) continue
                    val det = dets[f[5]] ?: continue
                    val a = f[10].split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (a.size != 6) continue
                    val ovr = f[8].toIntOrNull() ?: continue
                    teams.last().players += PlayerRow(
                        f[1].toIntOrNull() ?: 0, f[2], f[3].toIntOrNull() ?: 2000, f[4].uppercase(), det, dets[f[6]],
                        when (f[7]) { "L" -> 1; "B" -> 2; else -> 0 }, ovr, (f[9].toIntOrNull() ?: ovr).coerceAtLeast(ovr), a.toIntArray(),
                        f.getOrNull(11)?.split(",")?.mapNotNull { dets[it.trim()] } ?: emptyList(),
                    )
                }
            }
        }
        return LeagueData(id, season, lname, teams)
    }

    /** Detaylı mevkiden ana mevki. Kanatlar: golcü kanat forvet, oyun kurucu/ortacı kanat orta saha. */
    fun posOf(r: PlayerRow): Pos = when (r.det) {
        Positions.Det.KL -> Pos.GK
        Positions.Det.SLB, Positions.Det.SGB, Positions.Det.STP -> Pos.DF
        Positions.Det.DOS, Positions.Det.MO, Positions.Det.OOS -> Pos.MF
        Positions.Det.SF -> Pos.FW
        Positions.Det.SLK, Positions.Det.SGK -> if (r.attrs[1] >= r.attrs[2]) Pos.FW else Pos.MF
    }

    /** Ligi gerçek veriden kurar; kullanılan kısa kodlar [used] ile benzersiz tutulur. */
    fun addLeague(g: Game, id: Int, rnd: Random, used: MutableSet<String> = g.teams.map { it.short }.toMutableSet()) {
        val data = load(id) ?: return
        val wealth = com.supermenajer.game.model.Leagues.of(id)?.wealth ?: 1.0
        val tier1 = com.supermenajer.game.model.Leagues.of(id)?.tier == 1
        for (tr in data.teams.take(18)) {
            var short = tr.short.uppercase().take(3)
            var k = 0
            while (short in used) { short = tr.short.uppercase().take(2) + ('A' + k); k++ }
            used += short
            val nextId = (g.teams.maxOfOrNull { it.id } ?: -1) + 1
            val budget = (Generator.budgetFor(tr.rep) * (if (id >= 10 && tier1) 2.0 else 1.0) * wealth.coerceAtLeast(0.8)).toLong()
            val t = Team(nextId, Alias.club(tr.name), short, tr.city, tr.c1, tr.c2, tr.rep, budget, division = id)
            t.real = true
            t.stadium = tr.capacity.coerceIn(3_000, 99_000)
            t.stadiumName = Alias.club(tr.stadium)
            g.addTeam(t)
            if (tr.coach.isNotBlank()) {
                g.managers[t.id] = Alias.person(tr.coach)
                g.mgrSecurity[t.id] = rnd.nextDouble(55.0, 75.0)
            }
            val numbers = HashSet<Int>()
            for (r in tr.players) {
                val p = makePlayer(g.nextPlayerId++, r, t.id, rnd)
                if (p.number > 0 && !numbers.add(p.number)) p.number = 0
                g.addPlayer(p)
            }
            cover(g, t)
        }
    }

    /** Eksik temel mevkileri (3 stoper, iki bek, DOS, MO, santrfor) en uygun oyuncunun 2. mevkisiyle kapatır. */
    fun cover(g: Game, t: Team) {
        val sq = g.squad(t.id)
        fun has(d: Positions.Det) = sq.count { Positions.canPlay(it, d) }
        val need = listOf(
            Positions.Det.STP to 3, Positions.Det.SLB to 1, Positions.Det.SGB to 1,
            Positions.Det.DOS to 1, Positions.Det.MO to 1, Positions.Det.SF to 1,
        )
        for ((d, n) in need) {
            var have = has(d)
            while (have < n) {
                val c = sq.filter { it.secondDet < 0 && it.detail != d.ordinal && it.pos != Pos.GK }
                    .maxByOrNull { Positions.fit(it, d) * 100 + it.overall * 0.1 } ?: break
                c.secondDet = d.ordinal
                have++
            }
        }
    }

    fun makePlayer(id: Int, r: PlayerRow, teamId: Int, rnd: Random): Player {
        val pos = posOf(r)
        val a = r.attrs
        val age = (SEASON_YEAR - r.birth).coerceIn(15, 42)
        val gk = if (pos == Pos.GK) ((r.ovr - 0.1 * a[5] - 0.1 * a[2]) / 0.8).roundToInt().coerceIn(30, 97) else rnd.nextInt(8, 22)
        val p = Player(id, Alias.person(r.name), age, r.nation, pos, a[0], a[1], a[2], a[3], a[4], a[5], gk, r.pot, teamId)
        // Oyun içi güç formülü FIFA'dan farklı: profil korunarak genel güç hedefe kaydırılır
        if (pos != Pos.GK) repeat(5) {
            val d = r.ovr - p.overall
            if (d != 0) {
                p.pace = (p.pace + d).coerceIn(10, 97)
                p.shooting = (p.shooting + d).coerceIn(10, 97)
                p.passing = (p.passing + d).coerceIn(10, 97)
                p.dribbling = (p.dribbling + d).coerceIn(10, 97)
                p.defending = (p.defending + d).coerceIn(10, 97)
                p.physical = (p.physical + d).coerceIn(10, 97)
            }
        }
        p.potential = r.pot.coerceIn(p.overall, 96)
        p.detail = r.det.ordinal
        p.secondDet = r.det2?.takeIf { it != r.det }?.ordinal ?: -1
        p.extraDets = r.all.filter { it != r.det && it.ordinal != p.secondDet && (it == Positions.Det.KL) == (r.det == Positions.Det.KL) }
            .map { it.ordinal }.distinct().toMutableList()
        p.foot = r.foot
        p.number = r.number.coerceIn(0, 99)
        p.contractYears = rnd.nextInt(1, 6)
        p.salary = p.demandWage
        p.morale = rnd.nextInt(60, 80).toDouble()
        return p
    }
}
