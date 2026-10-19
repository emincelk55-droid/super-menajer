package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.random.Random

/**
 * FM / Top Eleven tarzı bireysel antrenman: oyuncuya bir özellik odağı verilir ve
 * yakın bir mevki öğretilebilir. Mevki öğrenilince oyuncu orada neredeyse tam verimle oynar.
 */
object Development {
    val focusNames = listOf("Hız", "Şut", "Pas", "Teknik", "Savunma", "Fizik", "Kalecilik")

    private val D = Positions.Det.entries

    /** Oyuncunun öğrenebileceği yakın mevkiler. */
    fun learnable(p: Player): List<Positions.Det> {
        val d = Positions.det(p)
        val list = when (d) {
            Positions.Det.KL -> emptyList()
            Positions.Det.SLB -> listOf(Positions.Det.SGB, Positions.Det.SLK, Positions.Det.STP)
            Positions.Det.SGB -> listOf(Positions.Det.SLB, Positions.Det.SGK, Positions.Det.STP)
            Positions.Det.STP -> listOf(Positions.Det.DOS, Positions.Det.SLB, Positions.Det.SGB)
            Positions.Det.DOS -> listOf(Positions.Det.STP, Positions.Det.MO)
            Positions.Det.MO -> listOf(Positions.Det.DOS, Positions.Det.OOS, Positions.Det.SLK, Positions.Det.SGK)
            Positions.Det.OOS -> listOf(Positions.Det.MO, Positions.Det.SF, Positions.Det.SLK, Positions.Det.SGK)
            Positions.Det.SLK -> listOf(Positions.Det.SGK, Positions.Det.OOS, Positions.Det.SF, Positions.Det.SLB)
            Positions.Det.SGK -> listOf(Positions.Det.SLK, Positions.Det.OOS, Positions.Det.SF, Positions.Det.SGB)
            Positions.Det.SF -> listOf(Positions.Det.OOS, Positions.Det.SLK, Positions.Det.SGK)
        }
        return list.filter { !Positions.canPlay(p, it) }
    }

    fun setFocus(p: Player, focus: Int) {
        p.trainFocus = if (p.trainFocus == focus) -1 else focus
    }

    fun setLearn(p: Player, det: Int) {
        if (p.learnDet == det) { p.learnDet = -1; p.learnProgress = 0; return }
        p.learnDet = det
        p.learnProgress = 0
    }

    /** Haftalık (lig günleri): kullanıcının oyuncuları için odak gelişimi ve mevki öğrenme. */
    fun weekly(g: Game, rnd: Random) {
        if (g.fired) return
        val coach = StaffLogic.coachFactor(g) * Perks.youthDev(g)
        for (p in g.squad(g.userTeamId)) {
            if (p.trainFocus >= 0 && p.overall < p.potential && p.age <= 31) {
                val chance = 0.12 * Traits.devFactor(p) * coach * (if (p.age <= 23) 1.3 else 1.0)
                if (rnd.nextDouble() < chance) bump(p, p.trainFocus)
            }
            if (p.learnDet >= 0) {
                val base = when { p.age <= 23 -> 7; p.age <= 29 -> 5; else -> 3 }
                val f = when (Traits.of(p)) { Traits.PRO -> 1.3; Traits.LAZY -> 0.7; else -> 1.0 }
                p.learnProgress += (base * f * coach.coerceAtMost(1.3)).toInt().coerceAtLeast(1)
                if (p.learnProgress >= 100) {
                    val learned = p.learnDet
                    if (p.secondDet < 0) p.secondDet = learned else if (learned !in p.extraDets) p.extraDets += learned
                    p.learnDet = -1
                    p.learnProgress = 0
                    g.addNews("🎓 Yeni mevki öğrenildi", "${p.name} artık ${D[learned].label} (${D[learned].code}) mevkisinde de oynayabiliyor.")
                }
            }
        }
    }

    private fun bump(p: Player, focus: Int) {
        when (focus) {
            0 -> p.pace = (p.pace + 1).coerceAtMost(97)
            1 -> p.shooting = (p.shooting + 1).coerceAtMost(97)
            2 -> p.passing = (p.passing + 1).coerceAtMost(97)
            3 -> p.dribbling = (p.dribbling + 1).coerceAtMost(97)
            4 -> p.defending = (p.defending + 1).coerceAtMost(97)
            5 -> p.physical = (p.physical + 1).coerceAtMost(97)
            else -> if (p.pos == Pos.GK) p.goalkeeping = (p.goalkeeping + 1).coerceAtMost(97)
        }
    }
}
