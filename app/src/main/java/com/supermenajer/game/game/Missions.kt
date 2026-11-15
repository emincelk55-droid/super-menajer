package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/** Sezon görevleri (meydan okumalar): tamamlanınca menajer XP'si ve yönetimden ödül bütçesi. */
object Missions {
    class M(val id: String, val title: String, val target: Int, val xp: Int)

    private val poolCache = LangCached { listOf(
        M("streak3", L("3 maç üst üste kazan", "Win 3 matches in a row"), 3, 60),
        M("bigwin", L("Bir maçı 3 farkla kazan", "Win a match by 3 goals"), 1, 40),
        M("clean3", L("3 maçı gol yemeden bitir", "Keep 3 clean sheets"), 3, 50),
        M("youth5", L("21 yaş altı oyuncuları 5 maç ilk 11'de oynat", "Start under-21 players in 5 matches"), 5, 50),
        M("goals20", L("Sezonda 20 gol at", "Score 20 goals in the season"), 20, 50),
        M("sign1", L("Bir transfer yap", "Make a signing"), 1, 30),
        M("derby", L("Bir derbi kazan", "Win a derby"), 1, 60),
        M("top3", L("Lig sıralamasında ilk 3'e gir", "Reach the top 3 in the league"), 1, 80),
    ) }
    val pool get() = poolCache.get()

    fun ensure(g: Game, rnd: kotlin.random.Random = kotlin.random.Random.Default) {
        if (g.missionSeason == g.season && g.missions.isNotEmpty()) return
        g.missionSeason = g.season
        g.missions = pool.shuffled(rnd).take(5).map { "${it.id}|${it.title}|${it.target}|0|${it.xp}|0" }.toMutableList()
    }

    class View(val id: String, val title: String, val target: Int, val progress: Int, val xp: Int, val done: Boolean)

    fun list(g: Game): List<View> = g.missions.mapNotNull { s ->
        val c = s.split("|")
        if (c.size < 6) null else View(c[0], c[1], c[2].toInt(), c[3].toInt(), c[4].toInt(), c[5] == "1")
    }

    /** İlerleme ekle (ya da set=true ile değeri ayarla). */
    fun progress(g: Game, id: String, amount: Int, set: Boolean = false) {
        g.missions = g.missions.map { s ->
            val c = s.split("|").toMutableList()
            if (c.size < 6 || c[0] != id || c[5] == "1") return@map s
            val v = if (set) amount else c[3].toInt() + amount
            c[3] = v.coerceIn(0, c[2].toInt()).toString()
            if (v >= c[2].toInt()) {
                c[5] = "1"
                val xp = c[4].toInt()
                Perks.addXp(g, xp)
                val prize = 250_000L * (xp / 20)
                Economy.earn(g.userTeam, Economy.BOARD, prize)
                g.addNews(L("🎯 Görev tamamlandı: ${c[1]}", "🎯 Task complete: ${c[1]}"), L("+$xp XP ve yönetimden ${money(prize)} ödül!", "+$xp XP and a ${money(prize)} reward from the board!"))
                Social.post(g, L("Görev başarıyla tamamlandı: ${c[1]}! 👏", "Task completed: ${c[1]}! 👏"))
            }
            c.joinToString("|")
        }.toMutableList()
    }

    /** Maç sonrası güncelleme. */
    fun afterMatch(g: Game, gf: Int, ga: Int, derby: Boolean, league: Boolean, youthStarters: Int) {
        val won = gf > ga
        // Seri: kaybedilirse sıfırlanır
        val streak = list(g).firstOrNull { it.id == "streak3" }
        if (streak != null && !streak.done) progress(g, "streak3", if (won) streak.progress + 1 else 0, set = true)
        if (gf - ga >= 3) progress(g, "bigwin", 1)
        if (ga == 0) progress(g, "clean3", 1)
        if (youthStarters > 0) progress(g, "youth5", 1)
        progress(g, "goals20", gf)
        if (derby && won) progress(g, "derby", 1)
        if (league && g.userTeam.played >= 5 && g.position(g.userTeamId) <= 3) progress(g, "top3", 1)
    }
}
