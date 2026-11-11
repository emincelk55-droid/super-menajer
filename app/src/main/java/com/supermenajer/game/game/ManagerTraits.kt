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

    val all = listOf(
        Trait("youth", "🌱", "Genç Dostu", "21 yaş altı oyunculara 60 kez forma ver", "Gençler %8 daha hızlı gelişir", 60, "u21"),
        Trait("attack", "⚔️", "Hücum Ustası", "Lig maçlarında 3+ gol attığın 8 maç", "Takımın hücumu %1,5 güçlü", 8, "goalfest"),
        Trait("wall", "🧱", "Savunma Ustası", "Gol yemeden biten 15 maç", "Takımın savunması %1,5 güçlü", 15, "clean"),
        Trait("derby", "🔥", "Derbi Kralı", "3 derbi kazan", "Derbilerde takım %3 güçlü", 3, "derbyWin"),
        Trait("comeback", "🔄", "Geri Dönüş Ustası", "Geriye düştüğün 5 maçı kazan", "Gerideyken son 20 dakikada hücum %4 güçlü", 5, "comeback"),
        Trait("cup", "🏆", "Kupa Adamı", "Kupa ve Avrupa maçlarında 12 galibiyet", "Kupa maçlarında takım %2 güçlü", 12, "cupWin"),
    )

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
                g.addNews("${t.icon} Yeni menajer özelliği: ${t.name}", "${t.how} hedefini tamamladın. Etkisi: ${t.effect}. (Kulüp → Menajer profilinde)")
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
