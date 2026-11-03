package com.supermenajer.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Game
import org.junit.Test
import kotlin.random.Random

/**
 * 15 sezonluk denge raporu: gerçek futbol verileriyle karşılaştırmak için maç sonuçları,
 * puanlar, yaş/güç eğrisi, emeklilik ve altyapı. Eşik denetimleri [BalanceTest] ve diğer
 * testlerde; bu test yalnızca "REP" satırları basar.
 */
class BalanceReportTest {
    private fun playDay(g: Game, rnd: Random) {
        if (!g.isMatchDay) { g.date++; Season.freeDay(g); return }
        g.todayUserFixture()?.let { f ->
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame(); Season.matchNews(g, this) }
        }
        Season.simulateRest(g, g.week, rnd)
        Season.endDay(g, rnd)
        if (g.fired) Season.takeJob(g, g.jobOffers.first())
        g.pendingEvent = null
        g.natPending = ""
    }

    @Test
    fun fifteenSeasons() {
        val rnd = Random(11)
        val g = Generator.newWorld(rnd)
        g.userTeamId = g.divisionTeams(0)[4].id
        Season.start(g, rnd)
        val champs = mutableMapOf<Int, MutableList<String>>()
        repeat(15) {
            var guard = 0
            while (!g.seasonOver && guard++ < 2000) playDay(g, rnd)
            val league = g.fixtures.filter { it.hg >= 0 && it.comp == g.team(it.home).division }
            val n = league.size.coerceAtLeast(1)
            val goals = league.sumOf { it.hg + it.ag } / n.toDouble()
            val home = league.count { it.hg > it.ag } * 100 / n
            val draw = league.count { it.hg == it.ag } * 100 / n
            val big = league.count { kotlin.math.abs(it.hg - it.ag) >= 4 } * 100.0 / n
            val nil = league.count { it.hg == 0 && it.ag == 0 } * 100.0 / n
            val tops = com.supermenajer.game.model.Leagues.topLeagues.map { it.id }.filter { g.divisionTeams(it).isNotEmpty() }
            val ppg = tops.map { d ->
                val tb = g.table(d)
                val games = (tb.size - 1) * 2.0
                champs.getOrPut(d) { mutableListOf() } += tb.first().short
                Triple(tb.first().points / games, tb[tb.size / 2].points / games, tb.last().points / games)
            }
            val act = g.players.filter { it.teamId >= 0 }
            fun band(a: IntRange) = act.filter { it.age in a }.map { it.overall }.average()
            val idsBefore = g.players.map { it.id }.toSet()
            println(
                "REP ${g.season} maç=$n gol/maç=${"%.2f".format(goals)} ev=%$home ber=%$draw dep=%${100 - home - draw} 0-0=%${"%.1f".format(nil)} 4+fark=%${"%.1f".format(big)}" +
                    " | puan/maç şamp=${"%.2f".format(ppg.map { it.first }.average())} orta=${"%.2f".format(ppg.map { it.second }.average())} son=${"%.2f".format(ppg.map { it.third }.average())}" +
                    " | yaş ort=${"%.1f".format(act.map { it.age }.average())} güç 18-21=${"%.0f".format(band(16..21))} 22-25=${"%.0f".format(band(22..25))}" +
                    " 26-29=${"%.0f".format(band(26..29))} 30-32=${"%.0f".format(band(30..32))} 33+=${"%.0f".format(band(33..45))}" +
                    " | 85+=${act.count { it.overall >= 85 }} 80+=${act.count { it.overall >= 80 }} 75+=${act.count { it.overall >= 75 }} oyuncu=${act.size}"
            )
            Season.newSeason(g, rnd)
            val idsAfter = g.players.map { it.id }.toSet()
            val gks = act.filter { it.pos == com.supermenajer.game.model.Pos.GK && it.overall >= 70 }
            val fws = act.filter { it.pos == com.supermenajer.game.model.Pos.FW && it.overall >= 70 }
            val dfs = act.filter { it.pos == com.supermenajer.game.model.Pos.DF && it.overall >= 70 }
            println("REP   nitelik KL kalecilik=${"%.1f".format(gks.map { it.goalkeeping }.average())} KL güç=${"%.1f".format(gks.map { it.overall }.average())} FW şut=${"%.1f".format(fws.map { it.shooting }.average())} FW güç=${"%.1f".format(fws.map { it.overall }.average())} DF savunma=${"%.1f".format(dfs.map { it.defending }.average())} | yaş dağılımı ≤21=${act.count { it.age <= 21 } * 100 / act.size}% 22-29=${act.count { it.age in 22..29 } * 100 / act.size}% 30+=${act.count { it.age >= 30 } * 100 / act.size}%")
            println("REP   emekli/ayrılan=${(idsBefore - idsAfter).size} yeni(altyapı)=${(idsAfter - idsBefore).size} serbest=${g.players.count { it.teamId < 0 }}")
        }
        for ((d, l) in champs) println("REP şampiyonlar lig $d: ${l.joinToString(" ")} (farklı ${l.toSet().size})")
    }
}
