package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/**
 * Menajer yetenek ağacı (Soccer Manager / FC tarzı): maçlar, kupalar ve başarımlar deneyim (XP) kazandırır,
 * her seviyede bir yetenek puanı gelir. Yetenekler oyunun ilgili sistemlerine gerçekten etki eder.
 */
object Perks {
    class Perk(val id: String, val icon: String, val name: String, val desc: String, val max: Int = 3)

    private val allCache = LangCached { listOf(
        Perk("nego", "🤝", L("Pazarlık ustası", "Master negotiator"), L("Bonservis istekleri seviye başına %5 düşer, menajerler daha kolay ikna olur.", "Transfer fee demands drop 5% per level and agents are easier to convince.")),
        Perk("youth", "🌱", L("Genç geliştirici", "Youth developer"), L("21 yaş altı oyuncular ve bireysel antrenman seviye başına %10 daha hızlı gelişir.", "Under-21 players and individual training develop 10% faster per level.")),
        Perk("tactic", "🧠", L("Taktik dehası", "Tactical genius"), L("Maçlarda takımın seviye başına %1 daha güçlü.", "Your team is 1% stronger in matches per level.")),
        Perk("motiv", "📣", L("Motivasyoncu", "Motivator"), L("Takım konuşmaları ve kenar çizgisi seviye başına %15 daha etkili.", "Team talks and touchline shouts are 15% more effective per level.")),
        Perk("econ", "💰", L("Ekonomist", "Economist"), L("Bilet ve sponsor gelirleri seviye başına %5 artar.", "Ticket and sponsor income rises 5% per level.")),
        Perk("scout", "🔭", L("Gözlemci ağı", "Scouting network"), L("Gözlemci ücreti seviye başına %25 düşer, 2. seviyeden itibaren haftalık ek öneri.", "Scouting fees drop 25% per level, an extra weekly suggestion from level 2.")),
        Perk("disc", "🧱", L("Disiplin", "Discipline"), L("Oyuncularının faul ve kart riski seviye başına %10 azalır.", "Your players' foul and card risk drops 10% per level.")),
        Perk("pr", "🎙️", L("Halkla ilişkiler", "Public relations"), L("Taraftarı memnun eden olaylar seviye başına %15 daha etkili, protesto riski azalır.", "Events that please the fans are 15% more effective per level, lower protest risk.")),
    ) }
    val all get() = allCache.get()

    fun lvl(g: Game, id: String): Int = g.perks[id] ?: 0

    /** Seviye: her seviye bir öncekinden biraz daha fazla XP ister. */
    fun level(xp: Int): Int {
        var lv = 1
        var need = 120
        var x = xp
        while (x >= need) { x -= need; lv++; need += 40 }
        return lv
    }

    fun xpToNext(xp: Int): Pair<Int, Int> {
        var need = 120
        var x = xp
        while (x >= need) { x -= need; need += 40 }
        return x to need
    }

    fun points(g: Game): Int = level(g.managerXp) - 1 - g.perks.values.sum()

    fun addXp(g: Game, amount: Int) {
        if (g.fired || amount <= 0) return
        val before = level(g.managerXp)
        g.managerXp += amount
        val after = level(g.managerXp)
        if (after > before) g.addNews(L("⬆️ Menajer seviyesi $after", "⬆️ Manager level $after"), L("Deneyim kazandın ve bir yetenek puanı aldın! Kulüp → Yetenekler'den harca.", "You gained experience and earned a skill point! Spend it in Club → Skills."))
    }

    fun upgrade(g: Game, id: String): String {
        val p = all.firstOrNull { it.id == id } ?: return ""
        if (points(g) <= 0) return L("Yetenek puanın yok. Maç kazanarak deneyim topla.", "No skill points. Win matches to gain experience.")
        if (lvl(g, id) >= p.max) return L("${p.name} zaten en üst seviyede.", "${p.name} is already at the top level.")
        g.perks[id] = lvl(g, id) + 1
        return L("${p.icon} ${p.name} seviye ${lvl(g, id)}!", "${p.icon} ${p.name} level ${lvl(g, id)}!")
    }

    // ---- Etkiler ----
    fun askingFactor(g: Game) = 1.0 - 0.05 * lvl(g, "nego")
    fun agentBonus(g: Game) = 0.03 * lvl(g, "nego")
    fun youthDev(g: Game) = (1.0 + 0.1 * lvl(g, "youth")) * ManagerTraits.youthDev(g)
    fun tactics(g: Game) = 1.0 + 0.01 * lvl(g, "tactic")
    fun motivation(g: Game) = 1.0 + 0.15 * lvl(g, "motiv")
    fun income(g: Game) = 1.0 + 0.05 * lvl(g, "econ")
    fun scoutCost(g: Game) = 1.0 - 0.25 * lvl(g, "scout")
    fun extraScout(g: Game) = if (lvl(g, "scout") >= 2) 1 else 0
    fun foul(g: Game) = 1.0 - 0.1 * lvl(g, "disc")
    fun fanBoost(g: Game) = 1.0 + 0.15 * lvl(g, "pr")
}
