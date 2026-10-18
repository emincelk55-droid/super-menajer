package com.supermenajer.game.model

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Serializable
enum class Pos(val short: String, val label: String) {
    GK("KL", "Kaleci"),
    DF("DF", "Defans"),
    MF("OS", "Orta Saha"),
    FW("FV", "Forvet"),
}

@Serializable
class Player(
    val id: Int,
    var name: String,
    var age: Int,
    val nation: String,
    var pos: Pos,
    var pace: Int,
    var shooting: Int,
    var passing: Int,
    var dribbling: Int,
    var defending: Int,
    var physical: Int,
    var goalkeeping: Int,
    var potential: Int,
    var teamId: Int,
    var fitness: Double = 100.0,
    var morale: Double = 70.0,
    var injuredWeeks: Int = 0,
    var suspended: Int = 0,
    var yellowCards: Int = 0,
    var apps: Int = 0,
    var goals: Int = 0,
    var assists: Int = 0,
    var ratingSum: Double = 0.0,
    /** Sezon başındaki genel güç; gelişimi göstermek için. */
    var startOvr: Int = 0,
) {
    val overall: Int get() = ovrAt(pos)

    fun ovrAt(p: Pos): Int {
        val v = when (p) {
            Pos.GK -> goalkeeping * 0.8 + physical * 0.1 + passing * 0.1
            Pos.DF -> defending * 0.5 + physical * 0.2 + pace * 0.15 + passing * 0.15
            Pos.MF -> passing * 0.4 + dribbling * 0.2 + defending * 0.15 + shooting * 0.1 + physical * 0.15
            Pos.FW -> shooting * 0.45 + pace * 0.2 + dribbling * 0.25 + physical * 0.1
        }
        return v.roundToInt().coerceIn(1, 99)
    }

    val avgRating: Double get() = if (apps > 0) ratingSum / apps else 0.0
    val available: Boolean get() = injuredWeeks == 0 && suspended == 0

    val value: Long
        get() {
            val gap = (potential - overall).coerceAtLeast(0)
            val ageF = when {
                age <= 21 -> 1.4 + gap * 0.04
                age <= 25 -> 1.2 + gap * 0.02
                age <= 29 -> 1.0
                age <= 31 -> 0.7
                age <= 33 -> 0.45
                else -> 0.25
            }
            val base = 50_000.0 * 1.16.pow(overall - 45) * ageF
            return ((base / 25_000).roundToLong() * 25_000).coerceAtLeast(25_000)
        }

    /** Haftalık maaş. */
    val wage: Long get() = ((value * 0.0022 / 500).roundToLong() * 500).coerceAtLeast(1_500)
}

@Serializable
class Team(
    val id: Int,
    val name: String,
    val short: String,
    val city: String,
    val color1: Long,
    val color2: Long,
    var reputation: Int,
    var budget: Long,
    var formation: String = "4-4-2",
    /** 0 = defansif, 1 = dengeli, 2 = hücum */
    var mentality: Int = 1,
    /** 0 = düşük, 1 = orta, 2 = yüksek */
    var pressing: Int = 1,
    var lineup: MutableList<Int> = mutableListOf(),
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0,
    var gf: Int = 0,
    var ga: Int = 0,
    var form: String = "",
    var expected: Int = 9,
    /** 0 = Süper Lig, 1 = 1. Lig */
    var division: Int = 0,
    /** 0 dengeli, 1 hücum, 2 savunma, 3 fizik, 4 gençler */
    var training: Int = 0,
    /** 0 hafif, 1 normal, 2 yoğun */
    var intensity: Int = 1,
) {
    val played: Int get() = won + drawn + lost
    val points: Int get() = won * 3 + drawn
    val gd: Int get() = gf - ga

    fun resetTable() {
        won = 0; drawn = 0; lost = 0; gf = 0; ga = 0; form = ""
    }
}

object Comp {
    const val SUPER = 0
    const val FIRST = 1
    const val CUP = 2
    const val EURO = 3
    fun name(c: Int) = when (c) { SUPER -> "Süper Lig"; FIRST -> "1. Lig"; CUP -> "Türkiye Kupası"; else -> "Avrupa Kupası" }
    val cupRounds = listOf("Son 32", "Son 16", "Çeyrek Final", "Yarı Final", "Final")
    /** 0-5 grup maçları, 6 son 16, 7 çeyrek final, 8 yarı final, 9 final */
    val euroRounds = listOf("Grup 1. maç", "Grup 2. maç", "Grup 3. maç", "Grup 4. maç", "Grup 5. maç", "Grup 6. maç", "Son 16", "Çeyrek Final", "Yarı Final", "Final")
    const val EURO_KO = 6
    fun roundName(comp: Int, round: Int) = if (comp == EURO) euroRounds[round] else cupRounds[round]

    /** Bir maçın eleme (tek maç, gerekirse penaltı) olup olmadığı. */
    fun isKnockout(comp: Int, round: Int) = comp == CUP || (comp == EURO && round >= EURO_KO)
}

@Serializable
class Fixture(
    /** Takvimdeki gün (1'den başlar). */
    val week: Int,
    val home: Int,
    val away: Int,
    var hg: Int = -1,
    var ag: Int = -1,
    val comp: Int = Comp.SUPER,
    val round: Int = 0,
    var pen: String = "",
    var winner: Int = -1,
    /** Avrupa grup maçlarında grup numarası (0-7). */
    val group: Int = -1,
) {
    val played: Boolean get() = hg >= 0
    fun involves(teamId: Int) = home == teamId || away == teamId
    val scoreText: String get() = if (!played) "-" else "$hg - $ag" + if (pen.isNotEmpty()) " (P $pen)" else ""
}

/** Takvim günü: type 0 = lig haftası (number = hafta), 1 = kupa turu, 2 = Avrupa (number = tur). */
@Serializable
class Day(val type: Int, val number: Int)

@Serializable
class IncomingOffer(val playerId: Int, val teamId: Int, var price: Long, val day: Int)

@Serializable
class News(val season: Int, val week: Int, val title: String, val body: String)

@Serializable
class SeasonRecord(
    val season: Int,
    val champion: String,
    val userTeam: String,
    val userPos: Int,
    val topScorer: String,
    val cupWinner: String = "",
    val firstChampion: String = "",
    val userDivision: Int = 0,
    val euroWinner: String = "",
)

@Serializable
class Game(
    var managerName: String,
    var userTeamId: Int,
    var season: Int = 2026,
    var week: Int = 1,
    val teams: MutableList<Team>,
    val players: MutableList<Player>,
    var fixtures: MutableList<Fixture>,
    val news: MutableList<News>,
    var nextPlayerId: Int,
    var seasonOver: Boolean = false,
    val history: MutableList<SeasonRecord> = mutableListOf(),
    var calendar: MutableList<Day> = mutableListOf(),
    /** Yönetimin güveni (0-100). 0'a inerse kovulursun. */
    var confidence: Double = 60.0,
    var fired: Boolean = false,
    var jobOffers: MutableList<Int> = mutableListOf(),
    val offers: MutableList<IncomingOffer> = mutableListOf(),
    var cupWinner: Int = -1,
    /** Avrupa Kupası'ndaki 32 takım; grup g = 4g..4g+3. */
    var euroTeams: MutableList<Int> = mutableListOf(),
    /** Gelecek sezon Avrupa'ya katılacak Türk takımları. */
    var euroQualified: MutableList<Int> = mutableListOf(),
    var euroWinner: Int = -1,
    /** Eski (v0.1) kayıtlarda alan yoktur ve 1 okunur. */
    var saveVersion: Int = 1,
) {
    fun player(id: Int): Player? = players.firstOrNull { it.id == id }
    fun team(id: Int): Team = teams.first { it.id == id }
    val userTeam: Team get() = team(userTeamId)
    fun squad(teamId: Int): List<Player> = players.filter { it.teamId == teamId }
    val totalDays: Int get() = calendar.size
    val today: Day? get() = calendar.getOrNull(week - 1)

    /** Verilen güne kadar oynanmış en son lig haftası (kupa günleri için bir önceki hafta). */
    fun leagueWeekAt(day: Int): Int =
        calendar.take(day.coerceIn(0, calendar.size)).lastOrNull { it.type == 0 }?.number ?: 0

    fun dayLabel(day: Int): String {
        val d = calendar.getOrNull(day - 1) ?: return ""
        return when (d.type) {
            0 -> "${d.number}. Hafta"
            1 -> "Kupa · ${Comp.cupRounds[d.number]}"
            else -> "Avrupa · ${Comp.euroRounds[d.number]}"
        }
    }

    /** Takımın bulunduğu lig ya da (yabancı kulüpse) ülkesi. */
    fun leagueName(t: Team): String = if (t.division == 2) t.city else Comp.name(t.division)
    fun isForeign(t: Team) = t.division == 2

    fun divisionTeams(div: Int): List<Team> = teams.filter { it.division == div }

    fun table(div: Int = 0): List<Team> = divisionTeams(div).sortedWith(
        compareByDescending<Team> { it.points }
            .thenByDescending { it.gd }
            .thenByDescending { it.gf }
            .thenBy { it.name }
    )

    fun position(teamId: Int): Int = table(team(teamId).division).indexOfFirst { it.id == teamId } + 1
    fun weekFixtures(w: Int): List<Fixture> = fixtures.filter { it.week == w }

    /** Kullanıcının bugünkü maçı (yoksa null). */
    fun todayUserFixture(): Fixture? = fixtures.firstOrNull { it.week == week && !it.played && it.involves(userTeamId) }

    /** Kullanıcının oynanmamış sıradaki maçı. */
    fun nextUserFixture(): Fixture? =
        fixtures.filter { !it.played && it.involves(userTeamId) }.minByOrNull { it.week }

    fun addNews(title: String, body: String) {
        news.add(0, News(season, week, title, body))
        while (news.size > 60) news.removeAt(news.size - 1)
    }
}

data class Slot(val pos: Pos, val x: Float, val y: Float)

object Formations {
    private fun s(p: Pos, x: Float, y: Float) = Slot(p, x, y)
    private val gk = s(Pos.GK, 0.5f, 0.05f)
    private val back4 = listOf(
        s(Pos.DF, 0.12f, 0.27f), s(Pos.DF, 0.37f, 0.22f), s(Pos.DF, 0.63f, 0.22f), s(Pos.DF, 0.88f, 0.27f)
    )

    val map: LinkedHashMap<String, List<Slot>> = linkedMapOf(
        "4-4-2" to listOf(gk) + back4 + listOf(
            s(Pos.MF, 0.12f, 0.52f), s(Pos.MF, 0.37f, 0.47f), s(Pos.MF, 0.63f, 0.47f), s(Pos.MF, 0.88f, 0.52f),
            s(Pos.FW, 0.37f, 0.77f), s(Pos.FW, 0.63f, 0.77f),
        ),
        "4-3-3" to listOf(gk) + back4 + listOf(
            s(Pos.MF, 0.25f, 0.48f), s(Pos.MF, 0.5f, 0.42f), s(Pos.MF, 0.75f, 0.48f),
            s(Pos.FW, 0.16f, 0.76f), s(Pos.FW, 0.5f, 0.81f), s(Pos.FW, 0.84f, 0.76f),
        ),
        "4-2-3-1" to listOf(gk) + back4 + listOf(
            s(Pos.MF, 0.36f, 0.4f), s(Pos.MF, 0.64f, 0.4f),
            s(Pos.MF, 0.15f, 0.62f), s(Pos.MF, 0.5f, 0.62f), s(Pos.MF, 0.85f, 0.62f),
            s(Pos.FW, 0.5f, 0.83f),
        ),
        "4-5-1" to listOf(gk) + back4 + listOf(
            s(Pos.MF, 0.1f, 0.52f), s(Pos.MF, 0.3f, 0.46f), s(Pos.MF, 0.5f, 0.42f), s(Pos.MF, 0.7f, 0.46f), s(Pos.MF, 0.9f, 0.52f),
            s(Pos.FW, 0.5f, 0.8f),
        ),
        "3-5-2" to listOf(
            gk, s(Pos.DF, 0.25f, 0.22f), s(Pos.DF, 0.5f, 0.2f), s(Pos.DF, 0.75f, 0.22f),
            s(Pos.MF, 0.08f, 0.52f), s(Pos.MF, 0.3f, 0.46f), s(Pos.MF, 0.5f, 0.4f), s(Pos.MF, 0.7f, 0.46f), s(Pos.MF, 0.92f, 0.52f),
            s(Pos.FW, 0.37f, 0.78f), s(Pos.FW, 0.63f, 0.78f),
        ),
        "5-3-2" to listOf(
            gk, s(Pos.DF, 0.07f, 0.32f), s(Pos.DF, 0.28f, 0.22f), s(Pos.DF, 0.5f, 0.2f), s(Pos.DF, 0.72f, 0.22f), s(Pos.DF, 0.93f, 0.32f),
            s(Pos.MF, 0.25f, 0.48f), s(Pos.MF, 0.5f, 0.45f), s(Pos.MF, 0.75f, 0.48f),
            s(Pos.FW, 0.37f, 0.77f), s(Pos.FW, 0.63f, 0.77f),
        ),
    )

    val names: List<String> get() = map.keys.toList()
    fun slots(name: String): List<Slot> = map[name] ?: map.getValue("4-4-2")
}
