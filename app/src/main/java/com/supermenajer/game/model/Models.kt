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
) {
    val played: Int get() = won + drawn + lost
    val points: Int get() = won * 3 + drawn
    val gd: Int get() = gf - ga

    fun resetTable() {
        won = 0; drawn = 0; lost = 0; gf = 0; ga = 0; form = ""
    }
}

@Serializable
class Fixture(
    val week: Int,
    val home: Int,
    val away: Int,
    var hg: Int = -1,
    var ag: Int = -1,
) {
    val played: Boolean get() = hg >= 0
}

@Serializable
class News(val season: Int, val week: Int, val title: String, val body: String)

@Serializable
class SeasonRecord(val season: Int, val champion: String, val userTeam: String, val userPos: Int, val topScorer: String)

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
) {
    fun player(id: Int): Player? = players.firstOrNull { it.id == id }
    fun team(id: Int): Team = teams.first { it.id == id }
    val userTeam: Team get() = team(userTeamId)
    fun squad(teamId: Int): List<Player> = players.filter { it.teamId == teamId }
    val totalWeeks: Int get() = (teams.size - 1) * 2

    fun table(): List<Team> = teams.sortedWith(
        compareByDescending<Team> { it.points }
            .thenByDescending { it.gd }
            .thenByDescending { it.gf }
            .thenBy { it.name }
    )

    fun position(teamId: Int): Int = table().indexOfFirst { it.id == teamId } + 1
    fun weekFixtures(w: Int): List<Fixture> = fixtures.filter { it.week == w }
    fun nextUserFixture(): Fixture? =
        fixtures.firstOrNull { !it.played && (it.home == userTeamId || it.away == userTeamId) }

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
