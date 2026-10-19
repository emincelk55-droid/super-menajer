package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Vacancy
import kotlin.random.Random

/**
 * FC tarzı menajer pazarı: her kulübün bir teknik direktörü ve koltuk güvencesi var.
 * Kötü giden hocalar kovulur, koltuk bir süre boş kalır ve menajer bu işlere başvurabilir.
 */
object ManagerMarket {
    fun ensure(g: Game, rnd: Random = Random(g.season.toLong())) {
        for (t in g.teams) {
            if (t.id == g.userTeamId || t.division == Leagues.LEGACY_EURO) continue
            if (t.id !in g.managers) g.managers[t.id] = Generator.personName(rnd, g.flagOf(t)).first
            if (t.id !in g.mgrSecurity) g.mgrSecurity[t.id] = rnd.nextDouble(55.0, 75.0)
        }
    }

    fun securityText(s: Double) = when {
        s >= 70 -> "Güvende"
        s >= 45 -> "Sağlam"
        s >= 25 -> "Baskı altında"
        else -> "Koltuğu sallanıyor"
    }

    /** Lig haftası sonunda: sonuçlara göre koltuk güvencesi, kovulmalar ve boş koltukların dolması. */
    fun weekly(g: Game, leagueWeek: Int, rnd: Random) {
        ensure(g, rnd)
        // Süresi dolan boş koltuklar yeni hocayla dolar
        val filled = g.vacancies.filter { g.date > it.until }
        for (v in filled) {
            val t = g.teamOrNull(v.teamId) ?: continue
            val name = Generator.personName(rnd, g.flagOf(t)).first
            g.managers[t.id] = name
            g.mgrSecurity[t.id] = 65.0
            if (t.division == g.userTeam.division) g.addNews("Yeni teknik direktör", "${t.name}, $name ile anlaştı.")
        }
        g.vacancies.removeAll(filled.toSet())
        if (leagueWeek < 6) return
        for (div in Season.activeLeagues(g)) {
            val table = g.table(div)
            table.forEachIndexed { i, t ->
                if (t.id == g.userTeamId || g.vacancies.any { it.teamId == t.id }) return@forEachIndexed
                val pos = i + 1
                val last = t.form.lastOrNull()
                var s = g.mgrSecurity[t.id] ?: 60.0
                s += (t.expected - pos) * 0.35 + when (last) { 'G' -> 1.5; 'M' -> -2.0; else -> 0.0 } + rnd.nextDouble(-1.0, 1.0)
                s = s.coerceIn(0.0, 100.0)
                g.mgrSecurity[t.id] = s
                if (s < 12 && rnd.nextDouble() < 0.35) fire(g, t.id, rnd)
            }
        }
    }

    private fun fire(g: Game, teamId: Int, rnd: Random) {
        val t = g.team(teamId)
        val old = g.managers[teamId] ?: "Teknik direktör"
        g.managers[teamId] = "Geçici: " + Generator.personName(rnd, g.flagOf(t)).first
        g.mgrSecurity[teamId] = 50.0
        g.vacancies += Vacancy(teamId, g.date + 21)
        if (t.division == g.userTeam.division || t.reputation >= 78) {
            g.addNews("🔥 Hoca kovuldu", "${t.name}, kötü sonuçlar nedeniyle $old ile yollarını ayırdı. Koltuk boş — Kulüp → Hoca Pazarı'ndan başvurabilirsin.")
        }
    }

    /** Boş koltuğa başvuru. Başarı itibara ve kulübün büyüklüğüne bağlı. */
    fun apply(g: Game, teamId: Int, rnd: Random = Random.Default): String {
        val v = g.vacancies.firstOrNull { it.teamId == teamId } ?: return "Bu koltuk artık boş değil."
        if (v.applied) return "Bu işe zaten başvurdun."
        v.applied = true
        val t = g.team(teamId)
        val gap = t.reputation - (g.managerRep + 25)
        val chance = (0.6 - gap * 0.05).coerceIn(0.03, 0.9)
        return if (rnd.nextDouble() < chance) {
            g.vacancies.remove(v)
            g.jobOffers = mutableListOf(teamId)
            g.careerOffer = true
            g.addNews("İş teklifi!", "${t.name} başvurunu olumlu karşıladı ve sana teknik direktörlük teklif etti. Ana sayfadan karar ver.")
            "${t.name} seni istiyor! Ana sayfadan teklifi kabul edebilirsin."
        } else {
            "${t.name} başka adaylarla devam etmeyi tercih etti."
        }
    }
}
