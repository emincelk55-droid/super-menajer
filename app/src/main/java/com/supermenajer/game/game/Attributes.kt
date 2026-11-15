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
            L("ZİHİNSEL", "MENTAL"),
            listOf(
                a(L("Agresiflik", "Aggression"), phy * 0.5 + def * 0.3 + 10 + if (trait == Traits.HOTHEAD) 18 else 0),
                a(L("Karar Alma", "Decisions"), (pas + dri + def) / 3 + exp),
                a(L("Yaratıcılık", "Creativity"), pas * 0.6 + dri * 0.4),
                a(L("Liderlik", "Leadership"), 30 + exp * 2 + if (trait == Traits.LEADER) 35 else 0),
                a(L("Pozisyon Alma", "Positioning"), if (p.pos == Pos.FW) sho * 0.8 + exp else def * 0.8 + exp + 5),
                a(L("Soğukkanlılık", "Composure"), sho * 0.5 + 30 + exp + if (trait == Traits.BIG_GAME) 12 else 0),
                a(L("Takım Oyunu", "Teamwork"), pas * 0.5 + def * 0.3 + 12 + if (trait == Traits.PRO || trait == Traits.LOYAL) 10 else 0),
                a(L("Çalışkanlık", "Work Rate"), phy * 0.4 + 35 + when (trait) { Traits.PRO -> 18; Traits.LAZY -> -18; else -> 0 }),
            ),
        )
        val physical = Group(
            L("FİZİKSEL", "PHYSICAL"),
            listOf(
                a(L("Hız", "Pace"), pace),
                a(L("Hızlanma", "Acceleration"), pace * 0.8 + dri * 0.2),
                a(L("Çeviklik", "Agility"), dri * 0.5 + pace * 0.5),
                a(L("Dayanıklılık", "Stamina"), phy * 0.7 + 22 - (p.age - 28).coerceAtLeast(0) * 2.0),
                a(L("Güç", "Strength"), phy),
                a(L("Zıplama", "Jumping"), phy * 0.8 + 8),
            ),
        )
        val tech = if (p.pos == Pos.GK) {
            val gk = p.goalkeeping.toDouble()
            Group(
                L("KALECİLİK", "GOALKEEPING"),
                listOf(
                    a(L("Refleks", "Reflexes"), gk),
                    a(L("Bire Bir", "One on Ones"), gk * 0.8 + pace * 0.2),
                    a(L("Hava Hakimiyeti", "Aerial Reach"), gk * 0.6 + phy * 0.4),
                    a(L("Elle Oyun", "Handling"), gk * 0.9),
                    a(L("Ayakla Oyun", "Kicking"), pas * 0.7 + 10),
                    a(L("Degaj", "Throwing"), pas * 0.5 + phy * 0.3 + 15),
                    a(L("İletişim", "Communication"), gk * 0.5 + exp * 2 + 25),
                ),
            )
        } else {
            Group(
                L("TEKNİK", "TECHNICAL"),
                listOf(
                    a(L("Bitiricilik", "Finishing"), sho),
                    a(L("Uzun Şut", "Long Shots"), sho * 0.7 + pas * 0.3),
                    a(L("Pas", "Passing"), pas),
                    a(L("Orta Yapma", "Crossing"), pas * 0.6 + dri * 0.4 - (if (p.pos == Pos.DF && Positions.det(p) == Positions.Det.STP) 15 else 0)),
                    a(L("Top Sürme", "Dribbling"), dri),
                    a(L("İlk Kontrol", "First Touch"), dri * 0.5 + pas * 0.5),
                    a(L("Top Kapma", "Tackling"), def),
                    a(L("Markaj", "Marking"), def * 0.8 + phy * 0.2),
                    a(L("Kafa", "Heading"), phy * 0.6 + def * 0.2 + sho * 0.2),
                ),
            )
        }
        return listOf(tech, mental, physical)
    }
}
