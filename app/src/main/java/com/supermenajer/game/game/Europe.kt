package com.supermenajer.game.game

import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Team
import kotlin.random.Random

/** Avrupa Kupası: 8 grup x 4 takım, ardından tek maç eleme (son 16 → final). */
object Europe {
    /** Lig haftası → o haftadan sonra oynanan Avrupa turu. */
    val afterWeek = listOf(2, 4, 7, 9, 13, 15, 20, 23, 27, 32)

    /** Grup içi eşleşmeler (4 takımın grup içindeki sırasına göre), 6 maç günü. */
    private val groupRounds = listOf(
        listOf(0 to 1, 2 to 3), listOf(0 to 2, 3 to 1), listOf(3 to 0, 1 to 2),
        listOf(1 to 0, 3 to 2), listOf(2 to 0, 1 to 3), listOf(0 to 3, 2 to 1),
    )

    class Row(val team: Team) {
        var p = 0; var w = 0; var d = 0; var l = 0; var gf = 0; var ga = 0
        val pts get() = w * 3 + d
        val gd get() = gf - ga
    }

    private fun day(g: Game, round: Int): Int = g.calendar.indexOfFirst { it.type == 2 && it.number == round } + 1

    /** Türk temsilcileri: geçen sezonun ilk 4'ü ya da (ilk sezonda) en güçlü 4 takım. */
    private fun turkishEntrants(g: Game): List<Team> {
        val q = g.euroQualified.mapNotNull { id -> g.teams.firstOrNull { it.id == id } }
        return q.ifEmpty { g.divisionTeams(0).sortedBy { it.expected }.take(4) }.take(4)
    }

    fun start(g: Game, rnd: Random) {
        g.euroWinner = -1
        g.euroTeams = mutableListOf()
        if (g.calendar.none { it.type == 2 }) return
        val foreign = g.teams.filter { it.division == 2 }.sortedByDescending { it.reputation }
        if (foreign.size < 28) return
        val turks = turkishEntrants(g)
        val all = (turks + foreign.take(32 - turks.size)).sortedByDescending { it.reputation }
        // 4 torba; aynı gruba iki Türk takımı düşmesin.
        var groups: List<MutableList<Team>> = emptyList()
        for (attempt in 0 until 50) {
            val gs = List(8) { mutableListOf<Team>() }
            for (pot in 0 until 4) {
                val potTeams = all.subList(pot * 8, pot * 8 + 8).shuffled(rnd)
                potTeams.forEachIndexed { i, t -> gs[i] += t }
            }
            groups = gs
            if (gs.all { grp -> grp.count { it.division != 2 } <= 1 }) break
        }
        g.euroTeams = groups.flatten().map { it.id }.toMutableList()
        for ((gi, grp) in groups.withIndex()) {
            groupRounds.forEachIndexed { r, pairs ->
                for ((a, b) in pairs) g.fixtures += Fixture(day(g, r), grp[a].id, grp[b].id, comp = Comp.EURO, round = r, group = gi)
            }
        }
        for (t in turks) {
            val gi = g.euroTeams.indexOf(t.id) / 4
            val rivals = g.euroTeams.subList(gi * 4, gi * 4 + 4).filter { it != t.id }.map { g.team(it).name }
            if (t.id == g.userTeamId) {
                g.addNews("Avrupa Kupası kurası", "Grup ${groupLetter(gi)}'deyiz! Rakiplerimiz: ${rivals.joinToString()}.")
            }
        }
    }

    fun groupLetter(i: Int) = ('A' + i).toString()

    fun groupOf(g: Game, teamId: Int): Int = g.euroTeams.indexOf(teamId).let { if (it < 0) -1 else it / 4 }

    fun groupTable(g: Game, group: Int): List<Row> {
        val ids = g.euroTeams.subList(group * 4, group * 4 + 4)
        val rows = ids.associateWith { Row(g.team(it)) }
        for (f in g.fixtures) {
            if (f.comp != Comp.EURO || f.group != group || !f.played) continue
            val h = rows.getValue(f.home)
            val a = rows.getValue(f.away)
            h.p++; a.p++
            h.gf += f.hg; h.ga += f.ag; a.gf += f.ag; a.ga += f.hg
            when {
                f.hg > f.ag -> { h.w++; a.l++ }
                f.hg < f.ag -> { a.w++; h.l++ }
                else -> { h.d++; a.d++ }
            }
        }
        return rows.values.sortedWith(compareByDescending<Row> { it.pts }.thenByDescending { it.gd }.thenByDescending { it.gf }.thenByDescending { it.team.reputation })
    }

    /** Avrupa gününün sonunda: ödüller, gruptan çıkanlar, sonraki turun kurası. */
    fun advance(g: Game, round: Int, rnd: Random) {
        val fs = g.fixtures.filter { it.comp == Comp.EURO && it.round == round }
        for (f in fs) {
            if (round < Comp.EURO_KO) {
                when {
                    f.hg > f.ag -> g.team(f.home).budget += 600_000
                    f.hg < f.ag -> g.team(f.away).budget += 600_000
                    else -> { g.team(f.home).budget += 200_000; g.team(f.away).budget += 200_000 }
                }
            } else if (f.winner >= 0) {
                g.team(f.winner).budget += listOf(1_500_000L, 2_500_000L, 4_000_000L, 10_000_000L)[round - Comp.EURO_KO]
            }
        }
        val us = g.userTeamId
        when {
            round < Comp.EURO_KO - 1 -> Unit
            round == Comp.EURO_KO - 1 -> {
                val winners = (0 until 8).map { groupTable(g, it)[0].team }
                val runners = (0 until 8).map { groupTable(g, it)[1].team }
                val ug = groupOf(g, us)
                if (ug >= 0) {
                    val pos = groupTable(g, ug).indexOfFirst { it.team.id == us } + 1
                    if (pos <= 2) {
                        g.addNews("Avrupa'da gruptan çıktık!", "Grup ${groupLetter(ug)}'yi $pos. sırada bitirdik ve son 16'dayız!")
                        g.confidence = (g.confidence + 5).coerceAtMost(100.0)
                    } else {
                        g.addNews("Avrupa'ya veda", "Grup ${groupLetter(ug)}'de $pos. olduk ve Avrupa Kupası'na veda ettik.")
                    }
                }
                // Grup birincisi, başka grubun ikincisini ağırlar.
                val shift = rnd.nextInt(1, 8)
                val day = day(g, Comp.EURO_KO)
                for (i in 0 until 8) {
                    g.fixtures += Fixture(day, winners[i].id, runners[(i + shift) % 8].id, comp = Comp.EURO, round = Comp.EURO_KO)
                }
                announce(g, Comp.EURO_KO)
            }
            round == Comp.euroRounds.lastIndex -> {
                val w = fs.firstOrNull()?.winner ?: return
                g.euroWinner = w
                val t = g.team(w)
                t.reputation = (t.reputation + 3).coerceAtMost(97)
                g.addNews("Avrupa'nın en büyüğü belli oldu!", "Avrupa Kupası'nı ${t.name} kazandı! 🏆")
                if (w == us) {
                    g.confidence = 100.0
                    g.addNews("AVRUPA FATİHİ!", "İnanılmaz! Avrupa Kupası'nı kaldırdık! Bu başarı yıllarca konuşulacak hocam!")
                }
            }
            else -> {
                val winners = fs.mapNotNull { f -> f.winner.takeIf { it >= 0 } }.shuffled(rnd)
                val day = day(g, round + 1)
                for (i in 0 until winners.size / 2) {
                    val a = winners[2 * i]
                    val b = winners[2 * i + 1]
                    g.fixtures += Fixture(day, a, b, comp = Comp.EURO, round = round + 1)
                }
                announce(g, round + 1)
            }
        }
    }

    private fun announce(g: Game, round: Int) {
        val us = g.userTeamId
        g.fixtures.firstOrNull { it.comp == Comp.EURO && it.round == round && it.involves(us) }?.let {
            val opp = g.team(if (it.home == us) it.away else it.home)
            g.addNews("Avrupa kurası", "Avrupa Kupası ${Comp.euroRounds[round]} turunda rakibimiz: ${opp.name} (${opp.city}).")
        }
    }
}
