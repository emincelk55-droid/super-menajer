package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/** Sezon görevleri (meydan okumalar): tamamlanınca menajer XP'si ve yönetimden ödül bütçesi. */
object Missions {
    class M(val id: String, val title: String, val target: Int, val xp: Int)

    val pool = listOf(
        M("streak3", "3 maç üst üste kazan", 3, 60),
        M("bigwin", "Bir maçı 3 farkla kazan", 1, 40),
        M("clean3", "3 maçı gol yemeden bitir", 3, 50),
        M("youth5", "21 yaş altı oyuncuları 5 maç ilk 11'de oynat", 5, 50),
        M("goals20", "Sezonda 20 gol at", 20, 50),
        M("sign1", "Bir transfer yap", 1, 30),
        M("derby", "Bir derbi kazan", 1, 60),
        M("top3", "Lig sıralamasında ilk 3'e gir", 1, 80),
    )

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
                g.addNews("🎯 Görev tamamlandı: ${c[1]}", "+$xp XP ve yönetimden ${money(prize)} ödül!")
                Social.post(g, "Görev başarıyla tamamlandı: ${c[1]}! 👏")
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
