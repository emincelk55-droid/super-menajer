package com.supermenajer.game.game

import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.math.roundToInt

/**
 * FM tarzı detaylı nitelikler (1-20). Oyuncunun temel özelliklerinden, yaşından ve
 * kişiliğinden türetilir; her oyuncuya sabit küçük farklar eklenir ki aynı güçteki
 * iki oyuncu birbirinin kopyası olmasın.
 */
object Attributes {
    class Attr(val name: String, val value: Int)
    class Group(val title: String, val attrs: List<Attr>)

    private fun noise(p: Player, salt: Int): Int {
        var x = (p.id.toLong() * 2654435761L + salt * 40503L) and 0x7fffffff
        x = (x xor (x shr 13)) * 1274126177L and 0x7fffffff
        return (x % 9).toInt() - 4 // -4..4 (0-100 ölçeğinde)
    }

    private fun to20(v: Double) = (v / 5.0).roundToInt().coerceIn(1, 20)

    fun groups(p: Player): List<Group> {
        var salt = 0
        fun a(name: String, v: Double) = Attr(name, to20(v + noise(p, ++salt)))
        val trait = Traits.of(p)
        val exp = ((p.age - 22) * 2.2).coerceIn(-8.0, 14.0)
        val pace = p.pace.toDouble()
        val sho = p.shooting.toDouble()
        val pas = p.passing.toDouble()
        val dri = p.dribbling.toDouble()
        val def = p.defending.toDouble()
        val phy = p.physical.toDouble()
        val mental = Group(
            "ZİHİNSEL",
            listOf(
                a("Agresiflik", phy * 0.5 + def * 0.3 + 10 + if (trait == Traits.HOTHEAD) 18 else 0),
                a("Karar Alma", (pas + dri + def) / 3 + exp),
                a("Yaratıcılık", pas * 0.6 + dri * 0.4),
                a("Liderlik", 30 + exp * 2 + if (trait == Traits.LEADER) 35 else 0),
                a("Pozisyon Alma", if (p.pos == Pos.FW) sho * 0.8 + exp else def * 0.8 + exp + 5),
                a("Soğukkanlılık", sho * 0.5 + 30 + exp + if (trait == Traits.BIG_GAME) 12 else 0),
                a("Takım Oyunu", pas * 0.5 + def * 0.3 + 12 + if (trait == Traits.PRO || trait == Traits.LOYAL) 10 else 0),
                a("Çalışkanlık", phy * 0.4 + 35 + when (trait) { Traits.PRO -> 18; Traits.LAZY -> -18; else -> 0 }),
            ),
        )
        val physical = Group(
            "FİZİKSEL",
            listOf(
                a("Hız", pace),
                a("Hızlanma", pace * 0.8 + dri * 0.2),
                a("Çeviklik", dri * 0.5 + pace * 0.5),
                a("Dayanıklılık", phy * 0.7 + 22 - (p.age - 28).coerceAtLeast(0) * 2.0),
                a("Güç", phy),
                a("Zıplama", phy * 0.8 + 8),
            ),
        )
        val tech = if (p.pos == Pos.GK) {
            val gk = p.goalkeeping.toDouble()
            Group(
                "KALECİLİK",
                listOf(
                    a("Refleks", gk),
                    a("Bire Bir", gk * 0.8 + pace * 0.2),
                    a("Hava Hakimiyeti", gk * 0.6 + phy * 0.4),
                    a("Elle Oyun", gk * 0.9),
                    a("Ayakla Oyun", pas * 0.7 + 10),
                    a("Degaj", pas * 0.5 + phy * 0.3 + 15),
                    a("İletişim", gk * 0.5 + exp * 2 + 25),
                ),
            )
        } else {
            Group(
                "TEKNİK",
                listOf(
                    a("Bitiricilik", sho),
                    a("Uzun Şut", sho * 0.7 + pas * 0.3),
                    a("Pas", pas),
                    a("Orta Yapma", pas * 0.6 + dri * 0.4 - (if (p.pos == Pos.DF && Positions.det(p) == Positions.Det.STP) 15 else 0)),
                    a("Top Sürme", dri),
                    a("İlk Kontrol", dri * 0.5 + pas * 0.5),
                    a("Top Kapma", def),
                    a("Markaj", def * 0.8 + phy * 0.2),
                    a("Kafa", phy * 0.6 + def * 0.2 + sho * 0.2),
                ),
            )
        }
        return listOf(tech, mental, physical)
    }
}
