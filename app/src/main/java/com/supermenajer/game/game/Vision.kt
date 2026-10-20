package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues

/**
 * Kulüp vizyonu: yönetimin 5 yıllık planı. Her sezon sonunda değerlendirilir;
 * başarılan hedefler güven ve itibar kazandırır, kaçırılanlar güveni düşürür.
 */
object Vision {
    class Goal(val id: String, val title: String, val deadline: Int, val status: Int, val start: Int)

    fun list(g: Game): List<Goal> = g.vision.mapNotNull { s ->
        val c = s.split("|")
        if (c.size < 5) null else Goal(c[0], c[1], c[2].toInt(), c[3].toInt(), c[4].toInt())
    }

    private fun goal(id: String, title: String, deadline: Int, start: Int) = "$id|$title|$deadline|0|$start"

    fun ensure(g: Game) {
        if (g.vision.isNotEmpty() || g.userTeamId < 0) return
        val t = g.userTeam
        val s = g.season
        val target = Season.objectiveTarget(t.expected)
        val tier1 = Leagues.of(t.division)?.tier == 1
        g.vision = mutableListOf(
            if (!tier1) goal("promote", "2 sezon içinde üst lige çık", s + 1, s)
            else if (target <= 2) goal("title", "3 sezon içinde lig şampiyonu ol", s + 2, s)
            else goal("top4", "4 sezon içinde ligi ilk 4'te bitir", s + 3, s),
            goal("europe", "5 sezon içinde Avrupa kupasında oyna", s + 4, s),
            goal("youth", "Her sezon 21 yaş altı bir oyuncu 15+ maç oynasın", s + 4, s),
            goal("finance", "Bütçeyi hiçbir sezon eksiye düşürme", s + 4, s),
            goal("facility", "Antrenman tesisini ${(t.trainingFac + 2).coerceAtMost(10)}. seviyeye çıkar", s + 3, s),
        )
    }

    /** Sezon sonu değerlendirmesi (sezon sıfırlanmadan önce çağrılır). */
    fun evaluate(g: Game) {
        ensure(g)
        val t = g.userTeam
        val pos = g.position(t.id)
        val tier1 = Leagues.of(t.division)?.tier == 1
        val young = g.squad(t.id).any { it.age <= 21 && it.apps >= 15 }
        val inEurope = t.id in g.euroTeams
        g.vision = g.vision.map { s ->
            val c = s.split("|").toMutableList()
            if (c.size < 5 || c[3] != "0") return@map s
            val deadline = c[2].toInt()
            val last = g.season >= deadline
            val done = when (c[0]) {
                "promote" -> tier1 || pos <= 2
                "title" -> tier1 && pos == 1
                "top4" -> tier1 && pos <= 4
                "europe" -> inEurope || (tier1 && pos <= 4)
                "facility" -> t.trainingFac >= c[1].filter { it.isDigit() }.toIntOrNull() ?: 99
                else -> null
            }
            val failNow = when (c[0]) {
                "youth" -> !young
                "finance" -> t.budget < 0
                else -> false
            }
            when {
                done == true -> { c[3] = "1"; reward(g, c[1], true) }
                failNow -> { c[3] = "2"; reward(g, c[1], false) }
                last && done == false -> { c[3] = "2"; reward(g, c[1], false) }
                last && done == null -> { c[3] = "1"; reward(g, c[1], true) }
            }
            c.joinToString("|")
        }.toMutableList()
    }

    private fun reward(g: Game, title: String, ok: Boolean) {
        if (ok) {
            g.confidence = (g.confidence + 10).coerceAtMost(100.0)
            g.managerRep = (g.managerRep + 3).coerceAtMost(100)
            Perks.addXp(g, 80)
            g.addNews("🏛 Vizyon hedefi başarıldı", "\"$title\" hedefi gerçekleşti! Yönetim çok memnun (+10 güven).")
        } else {
            g.confidence = (g.confidence - 10).coerceAtLeast(0.0)
            g.addNews("🏛 Vizyon hedefi kaçtı", "\"$title\" hedefi tutturulamadı. Yönetim hayal kırıklığı içinde (-10 güven).")
        }
    }
}
