package com.supermenajer.game.game

import com.supermenajer.game.model.Game

/**
 * Menajer yetenek ağacı (Soccer Manager / FC tarzı): maçlar, kupalar ve başarımlar deneyim (XP) kazandırır,
 * her seviyede bir yetenek puanı gelir. Yetenekler oyunun ilgili sistemlerine gerçekten etki eder.
 */
object Perks {
    class Perk(val id: String, val icon: String, val name: String, val desc: String, val max: Int = 3)

    val all = listOf(
        Perk("nego", "🤝", "Pazarlık ustası", "Bonservis istekleri seviye başına %5 düşer, menajerler daha kolay ikna olur."),
        Perk("youth", "🌱", "Genç geliştirici", "21 yaş altı oyuncular ve bireysel antrenman seviye başına %10 daha hızlı gelişir."),
        Perk("tactic", "🧠", "Taktik dehası", "Maçlarda takımın seviye başına %1 daha güçlü."),
        Perk("motiv", "📣", "Motivasyoncu", "Takım konuşmaları ve kenar çizgisi seviye başına %15 daha etkili."),
        Perk("econ", "💰", "Ekonomist", "Bilet ve sponsor gelirleri seviye başına %5 artar."),
        Perk("scout", "🔭", "Gözlemci ağı", "Gözlemci ücreti seviye başına %25 düşer, 2. seviyeden itibaren haftalık ek öneri."),
        Perk("disc", "🧱", "Disiplin", "Oyuncularının faul ve kart riski seviye başına %10 azalır."),
        Perk("pr", "🎙️", "Halkla ilişkiler", "Taraftarı memnun eden olaylar seviye başına %15 daha etkili, protesto riski azalır."),
    )

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
        if (after > before) g.addNews("⬆️ Menajer seviyesi $after", "Deneyim kazandın ve bir yetenek puanı aldın! Kulüp → Yetenekler'den harca.")
    }

    fun upgrade(g: Game, id: String): String {
        val p = all.firstOrNull { it.id == id } ?: return ""
        if (points(g) <= 0) return "Yetenek puanın yok. Maç kazanarak deneyim topla."
        if (lvl(g, id) >= p.max) return "${p.name} zaten en üst seviyede."
        g.perks[id] = lvl(g, id) + 1
        return "${p.icon} ${p.name} seviye ${lvl(g, id)}!"
    }

    // ---- Etkiler ----
    fun askingFactor(g: Game) = 1.0 - 0.05 * lvl(g, "nego")
    fun agentBonus(g: Game) = 0.03 * lvl(g, "nego")
    fun youthDev(g: Game) = 1.0 + 0.1 * lvl(g, "youth")
    fun tactics(g: Game) = 1.0 + 0.01 * lvl(g, "tactic")
    fun motivation(g: Game) = 1.0 + 0.15 * lvl(g, "motiv")
    fun income(g: Game) = 1.0 + 0.05 * lvl(g, "econ")
    fun scoutCost(g: Game) = 1.0 - 0.25 * lvl(g, "scout")
    fun extraScout(g: Game) = if (lvl(g, "scout") >= 2) 1 else 0
    fun foul(g: Game) = 1.0 - 0.1 * lvl(g, "disc")
    fun fanBoost(g: Game) = 1.0 + 0.15 * lvl(g, "pr")
}
