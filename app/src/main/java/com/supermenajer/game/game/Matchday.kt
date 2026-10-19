package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * OSM / FM tarzı maç günü ve antrenman araçları: antrenman kampı, rakibe özel gizli antrenman,
 * oyunculara galibiyet primi ve sezon öncesi hazırlık maçları.
 */
object Matchday {
    const val BONUS = "Oyuncu primleri"
    const val CAMP = "Antrenman kampı"
    val bonusNames = listOf("Prim yok", "Normal prim", "Yüksek prim")

    private fun r25k(v: Double) = (v / 25_000).roundToLong().coerceAtLeast(1) * 25_000

    fun campCost(g: Game): Long = r25k((Economy.squadWages(g, g.userTeamId) * 3.0).coerceAtLeast(250_000.0))
    fun secretCost(g: Game): Long = r25k((Economy.squadWages(g, g.userTeamId) * 0.6).coerceAtLeast(50_000.0))
    fun bonusCost(g: Game, level: Int): Long = r25k(Economy.squadWages(g, g.userTeamId) * if (level == 2) 0.3 else 0.12)

    fun campActive(g: Game) = g.date in 1..g.campUntil

    /** 7 günlük antrenman kampı: moral ve kondisyon tazelenir, gelişim hızlanır. Sezonda en fazla 2 kez. */
    fun camp(g: Game): String {
        val us = g.userTeam
        if (g.campUses >= 2) return "Bu sezon iki kamp hakkını kullandın."
        if (campActive(g)) return "Takım zaten kampta."
        val cost = campCost(g)
        if (us.budget < cost) return "Kamp için bütçe yetersiz (${money(cost)})."
        Economy.earn(us, CAMP, -cost)
        g.campUses++
        g.campUntil = g.date + 7
        for (p in g.squad(us.id)) {
            p.morale = (p.morale + 8).coerceAtMost(100.0)
            p.fitness = 100.0
        }
        Career.unlock(g, "camp")
        g.addNews("🏕️ Antrenman kampı", "Takım 7 günlük kampa girdi (${money(cost)}). Moral ve kondisyon tazelendi, kamp boyunca gelişim %40 daha hızlı.")
        return "Kamp başladı!"
    }

    /** Sıradaki maç için rakibe özel gizli antrenman (+%2.5 performans). */
    fun secretTraining(g: Game): String {
        val f = g.nextUserFixture() ?: return "Yaklaşan maç yok."
        if (g.secretWeek == f.week) return "Bu maç için gizli antrenman zaten yapıldı."
        val cost = secretCost(g)
        if (g.userTeam.budget < cost) return "Bütçe yetersiz (${money(cost)})."
        Economy.earn(g.userTeam, CAMP, -cost)
        g.secretWeek = f.week
        val opp = g.team(if (f.home == g.userTeamId) f.away else f.home)
        g.addNews("🕵️ Gizli antrenman", "${opp.name} maçı için kapalı kapılar ardında özel çalışma yapıldı (${money(cost)}).")
        return "Gizli antrenman yapıldı: ${opp.name} maçında oyuncular %2.5 daha etkili olacak."
    }

    /** Maç sonrası galibiyet primi ödemesi ve moral. */
    fun afterMatch(g: Game, won: Boolean) {
        val us = g.userTeam
        val lvl = us.winBonus
        if (!won || lvl == 0) return
        val cost = bonusCost(g, lvl)
        Economy.earn(us, BONUS, -cost)
        for (p in g.squad(us.id)) p.morale = (p.morale + 2.0 * lvl).coerceAtMost(100.0)
    }

    fun canFriendly(g: Game) = !g.fired && g.week == 1 && !g.isMatchDay && g.friendlies < 3

    /** Hazırlık maçı: sonuç istatistiklere yazılmaz; kondisyon, moral ve biraz gelir getirir. Bir gün geçer. */
    fun friendly(g: Game, rnd: Random = Random.Default): String {
        if (!canFriendly(g)) return "Hazırlık maçları sadece sezon öncesinde (en fazla 3)."
        val us = g.userTeam
        val opp = g.teams.filter { it.id != us.id && it.division != us.division && it.division != Leagues.LEGACY_EURO && kotlin.math.abs(it.reputation - us.reputation) <= 12 }
            .randomOrNull(rnd) ?: return "Rakip bulunamadı."
        val f = Fixture(0, us.id, opp.id, comp = Comp.FRIENDLY)
        val savedFit = g.squad(us.id).associate { it.id to it.fitness }
        val sim = MatchSim(g, f, us.id, autoUserSubs = true, rnd = rnd)
        sim.runToEnd()
        // Hazırlık maçı resmi istatistiklere yazılmaz
        for (p in g.squad(us.id)) p.fitness = (savedFit[p.id] ?: 100.0).coerceAtMost(100.0)
        val won = sim.home.goals > sim.away.goals
        for (p in g.squad(us.id)) p.morale = (p.morale + if (won) 3 else if (sim.home.goals == sim.away.goals) 1 else -1).coerceIn(5.0, 100.0)
        val income = r25k(150_000.0 * (Leagues.of(us.division)?.wealth ?: 1.0))
        Economy.earn(us, Economy.TICKETS, income)
        g.friendlies++
        val scorers = sim.events.filter { it.type == com.supermenajer.game.engine.Ev.GOAL && it.side == 0 }
            .mapNotNull { e -> sim.home.used.firstOrNull { it.p.id == e.playerId }?.p?.name?.substringAfterLast(' ') }
        val res = "${us.name} ${sim.home.goals}-${sim.away.goals} ${opp.name}"
        g.addNews("🤝 Hazırlık maçı", "$res" + (if (scorers.isNotEmpty()) " · Goller: ${scorers.joinToString()}" else "") + ". Gelir: ${money(income)}.")
        g.date++
        Season.freeDay(g, rnd)
        return res
    }
}
