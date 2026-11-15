package com.supermenajer.game.game

import com.supermenajer.game.engine.Ev
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.Side
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Game

/**
 * Menajer özellikleri (Soccer Manager tarzı): oyun tarzına göre kendiliğinden kazanılır ve kalıcıdır.
 * Yetenek ağacından farklı olarak puan harcanmaz; maçlarda ne yaptığın belirler. Her birinin küçük bir etkisi var.
 */
object ManagerTraits {
    class Trait(val id: String, val icon: String, val name: String, val how: String, val effect: String, val need: Int, val stat: String)

    private val allCache = LangCached { listOf(
        Trait("youth", "🌱", L("Genç Dostu", "Youth Friendly"), L("21 yaş altı oyunculara 60 kez forma ver", "Give under-21 players 60 appearances"), L("Gençler %8 daha hızlı gelişir", "Youngsters develop 8% faster"), 60, "u21"),
        Trait("attack", "⚔️", L("Hücum Ustası", "Attack Master"), L("Lig maçlarında 3+ gol attığın 8 maç", "8 league matches where you scored 3+ goals"), L("Takımın hücumu %1,5 güçlü", "Team attack 1.5% stronger"), 8, "goalfest"),
        Trait("wall", "🧱", L("Savunma Ustası", "Defence Master"), L("Gol yemeden biten 15 maç", "15 clean sheets"), L("Takımın savunması %1,5 güçlü", "Team defence 1.5% stronger"), 15, "clean"),
        Trait("derby", "🔥", L("Derbi Kralı", "Derby King"), L("3 derbi kazan", "Win 3 derbies"), L("Derbilerde takım %3 güçlü", "Team 3% stronger in derbies"), 3, "derbyWin"),
        Trait("comeback", "🔄", L("Geri Dönüş Ustası", "Comeback King"), L("Geriye düştüğün 5 maçı kazan", "Win 5 matches after going behind"), L("Gerideyken son 20 dakikada hücum %4 güçlü", "Attack 4% stronger in the last 20 minutes when behind"), 5, "comeback"),
        Trait("cup", "🏆", L("Kupa Adamı", "Cup Man"), L("Kupa ve Avrupa maçlarında 12 galibiyet", "12 wins in cup and European matches"), L("Kupa maçlarında takım %2 güçlü", "Team 2% stronger in cup matches"), 12, "cupWin"),
    ) }
    val all get() = allCache.get()

    fun has(g: Game, id: String) = id in g.traitsEarned

    fun progress(g: Game, t: Trait): Int = g.traitStats[t.stat] ?: 0

    private fun inc(g: Game, stat: String, by: Int = 1) { g.traitStats[stat] = (g.traitStats[stat] ?: 0) + by }

    /** Kullanıcının maçından sonra: istatistikler ve yeni kazanılan özellikler. */
    fun afterMatch(g: Game, sim: MatchSim, us: Side) {
        val them = sim.other(us)
        val won = if (sim.isCup) sim.cupWinner()?.id == us.team.id else us.goals > them.goals
        inc(g, "u21", us.used.count { it.p.age <= 21 })
        if (Comp.isLeague(sim.fixture.comp) && us.goals >= 3) inc(g, "goalfest")
        if (them.goals == 0) inc(g, "clean")
        if (won && Talks.isDerby(g, sim.fixture)) inc(g, "derbyWin")
        if (won && (Comp.isCup(sim.fixture.comp) || sim.fixture.comp == Comp.EURO)) inc(g, "cupWin")
        if (won) {
            // Maç içinde geriye düşülmüş mü
            var u = 0; var o = 0; var behind = false
            for (e in sim.events) if (e.type == Ev.GOAL) { if (e.side == us.index) u++ else o++; if (o > u) behind = true }
            if (behind) inc(g, "comeback")
        }
        for (t in all) {
            if (t.id !in g.traitsEarned && progress(g, t) >= t.need) {
                g.traitsEarned += t.id
                g.addNews(L("${t.icon} Yeni menajer özelliği: ${t.name}", "${t.icon} New manager trait: ${t.name}"), L("${t.how} hedefini tamamladın. Etkisi: ${t.effect}. (Kulüp → Menajer profilinde)", "You completed the goal: ${t.how}. Effect: ${t.effect}. (Club → Manager profile)"))
            }
        }
    }

    // ---- Etkiler (MatchSim.strength ve gelişim) ----
    fun youthDev(g: Game) = if (has(g, "youth")) 1.08 else 1.0

    /** Kullanıcı tarafının hücum ve savunma çarpanı (dakika ve skora bağlı olanlar dahil). */
    fun matchFactors(g: Game, sim: MatchSim, us: Side): Pair<Double, Double> {
        var a = 1.0; var d = 1.0
        if (has(g, "attack")) a *= 1.015
        if (has(g, "wall")) d *= 1.015
        if (has(g, "derby") && Talks.isDerby(g, sim.fixture)) { a *= 1.03; d *= 1.03 }
        if (has(g, "cup") && (Comp.isCup(sim.fixture.comp) || sim.fixture.comp == Comp.EURO)) { a *= 1.02; d *= 1.02 }
        if (has(g, "comeback") && sim.minute >= 70 && us.goals < sim.other(us).goals) a *= 1.04
        return a to d
    }
}
