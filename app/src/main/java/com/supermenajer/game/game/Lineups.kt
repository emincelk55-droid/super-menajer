package com.supermenajer.game.game

import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Slot
import com.supermenajer.game.model.Team

object Lineups {
    private fun fillOrder(slots: List<Slot>): List<Int> = slots.indices.sortedBy {
        when (slots[it].pos) { Pos.GK -> 0; Pos.FW -> 1; Pos.DF -> 2; Pos.MF -> 3 }
    }

    /**
     * Bir oyuncunun mevkiye uygunluğu. Kendi mevkisindeki oyuncu her zaman önce gelir;
     * başka mevkiden oyuncu ancak o mevkide uygun kimse kalmadıysa seçilir.
     */
    fun score(p: Player, slot: Slot): Double {
        val det = Positions.slotDet(slot)
        val fit = Positions.fit(p, det)
        val detBonus = if (Positions.det(p) == det) 40 else if (fit >= 0.95) 20 else 0
        val natural = p.pos == slot.pos || p.secondDet == det.ordinal
        return p.ovrAt(slot.pos) * (0.6 + 0.4 * p.fitness / 100.0) * fit + (if (natural) 1000 else 0) + detBonus
    }

    /** Mevcut formasyon için en iyi 11'i seçer. */
    fun autoPick(g: Game, t: Team) {
        t.lineup = MutableList(11) { -1 }
        validate(g, t)
    }

    /** Geçersiz (sakat, cezalı, kadroda olmayan) oyuncuları en iyi alternatifle değiştirir. */
    fun validate(g: Game, t: Team) {
        val slots = Formations.slots(t.formation)
        val avail = g.squad(t.id).filter { it.available }.associateBy { it.id }
        val lineup = MutableList(11) { t.lineup.getOrNull(it) ?: -1 }
        val used = mutableSetOf<Int>()
        for (i in 0 until 11) {
            val id = lineup[i]
            if (id !in avail || id in used) lineup[i] = -1 else used += id
        }
        for (i in fillOrder(slots)) {
            if (lineup[i] != -1) continue
            val best = avail.values.filter { it.id !in used }.maxByOrNull { score(it, slots[i]) } ?: continue
            lineup[i] = best.id
            used += best.id
        }
        t.lineup = lineup
    }

    /** Formasyon değişince aynı 11'i yeni mevkilere en uygun şekilde dağıtır. */
    fun changeFormation(g: Game, t: Team, formation: String) {
        val current = t.lineup.mapNotNull { g.player(it) }.toMutableList()
        t.formation = formation
        val slots = Formations.slots(formation)
        val lineup = MutableList(11) { -1 }
        // Mevcut 11'den sadece kendi mevkisine uyanlar yerleşir; kalan boşlukları kadrodan doğru mevkideki oyuncular doldurur
        for (i in fillOrder(slots)) {
            val best = current.filter { it.pos == slots[i].pos && it.available }.maxByOrNull { score(it, slots[i]) } ?: continue
            lineup[i] = best.id
            current.remove(best)
        }
        t.lineup = lineup
        validate(g, t)
    }

    fun bench(g: Game, t: Team, n: Int = 7): List<Player> {
        val rest = g.squad(t.id).filter { it.available && it.id !in t.lineup }.sortedByDescending { it.overall }
        val gk = rest.firstOrNull { it.pos == Pos.GK }
        val others = rest.filter { it !== gk }.take(if (gk != null) n - 1 else n)
        return listOfNotNull(gk) + others
    }

    /** Takım gücü (en iyi 11 ortalaması) — beklentiler ve ekranlar için. */
    fun strength(g: Game, t: Team): Double {
        val sq = g.squad(t.id)
        val need = mapOf(Pos.GK to 1, Pos.DF to 4, Pos.MF to 4, Pos.FW to 2)
        var sum = 0.0
        var n = 0
        for ((pos, c) in need) {
            sq.filter { it.pos == pos }.sortedByDescending { it.overall }.take(c).forEach { sum += it.overall; n++ }
        }
        return if (n == 0) 0.0 else sum / n
    }

    /** Taktik ekranı için hücum / orta saha / savunma göstergeleri. */
    fun lineStrength(g: Game, t: Team): Triple<Int, Int, Int> {
        val slots = Formations.slots(t.formation)
        var a = 0.0; var m = 0.0; var d = 0.0
        t.lineup.forEachIndexed { i, id ->
            val p = g.player(id) ?: return@forEachIndexed
            val e = p.ovrAt(slots[i].pos) * Positions.fit(p, Positions.slotDet(slots[i]))
            when (slots[i].pos) {
                Pos.GK -> { m += e * 0.1; d += e * 0.3 }
                Pos.DF -> { a += e * 0.12; m += e * 0.3; d += e }
                Pos.MF -> { a += e * 0.45; m += e; d += e * 0.4 }
                Pos.FW -> { a += e; m += e * 0.2; d += e * 0.1 }
            }
        }
        a /= 4.3; m /= 5.4; d /= 5.9
        when (t.mentality) { 0 -> { a *= 0.9; d *= 1.07 }; 2 -> { a *= 1.08; d *= 0.94 } }
        return Triple(a.toInt(), m.toInt(), d.toInt())
    }
}
