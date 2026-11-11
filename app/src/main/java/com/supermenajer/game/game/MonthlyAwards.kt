package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import java.time.LocalDate

/**
 * Aylık ödüller (kullanıcının liginde): Ayın Oyuncusu, Ayın Genci, Ayın Menajeri. Ay değişince bir önceki ayın
 * istatistikleri (ay başı anlık görüntüsüne göre fark) değerlendirilir; kazanan bizden ise moral/itibar artar.
 */
object MonthlyAwards {
    const val PLAYER = "oyuncu"
    const val YOUNG = "genç"
    const val MANAGER = "menajer"

    private val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

    class Award(val season: Int, val month: String, val kind: String, val id: Int, val text: String)

    private fun key(epochDay: Long): Int = LocalDate.ofEpochDay(epochDay).let { it.year * 12 + it.monthValue - 1 }

    private fun snap(g: Game) {
        val div = g.userTeam.division
        g.monthSnap.clear()
        for (p in g.players) if (p.teamId >= 0 && g.teamOrNull(p.teamId)?.division == div) {
            g.monthSnap[p.id] = "${p.apps}|${p.goals}|${p.assists}|${p.ratingSum}"
        }
    }

    /** Her gün sonunda: ay değiştiyse geçen ayın ödüllerini dağıtır. */
    fun check(g: Game) {
        if (g.date <= 0 || g.fired) return
        val now = key(g.date)
        if (g.awardMonth < 0) { g.awardMonth = now; snap(g); return }
        if (now == g.awardMonth) return
        val prev = g.awardMonth
        g.awardMonth = now
        award(g, prev)
        snap(g)
    }

    /** Sezon başında istatistikler sıfırlandığı için anlık görüntü de yenilenir. */
    fun newSeason(g: Game) { snap(g); if (g.date > 0) g.awardMonth = key(g.date) }

    private fun award(g: Game, monthKey: Int) {
        val div = g.userTeam.division
        val monthName = months[monthKey % 12]
        class Row(val id: Int, val apps: Int, val goals: Int, val assists: Int, val rating: Double)
        val rows = g.players.filter { it.teamId >= 0 && g.teamOrNull(it.teamId)?.division == div }.mapNotNull { p ->
            val s = g.monthSnap[p.id]?.split("|") ?: return@mapNotNull null
            val apps = p.apps - s[0].toInt()
            if (apps < 2) return@mapNotNull null
            Row(p.id, apps, p.goals - s[1].toInt(), p.assists - s[2].toInt(), (p.ratingSum - s[3].toDouble()) / apps)
        }
        if (rows.isEmpty()) return
        val out = mutableListOf<Award>()
        rows.maxByOrNull { it.rating + it.goals * 0.08 + it.assists * 0.05 }?.let { r ->
            out += Award(g.season, monthName, PLAYER, r.id, "${r.apps} maç · ${r.goals} gol · ${r.assists} asist · %.2f ort.".format(r.rating))
        }
        rows.filter { (g.player(it.id)?.age ?: 99) <= 21 }.maxByOrNull { it.rating + it.goals * 0.08 }?.let { r ->
            out += Award(g.season, monthName, YOUNG, r.id, "${r.apps} maç · ${r.goals} gol · %.2f ort.".format(r.rating))
        }
        // Ayın menajeri: o ay ligde en çok puan toplayan takım
        val pts = HashMap<Int, Int>()
        for (f in g.fixtures) {
            if (!f.played || f.comp != div) continue
            if (key(GameCalendar.dateOf(g, f.week)) != monthKey) continue
            val (h, a) = when { f.hg > f.ag -> 3 to 0; f.hg < f.ag -> 0 to 3; else -> 1 to 1 }
            pts[f.home] = (pts[f.home] ?: 0) + h
            pts[f.away] = (pts[f.away] ?: 0) + a
        }
        pts.maxByOrNull { it.value }?.let { (tid, p) ->
            val mgr = if (tid == g.userTeamId) g.managerName else g.managers[tid] ?: "-"
            out += Award(g.season, monthName, MANAGER, tid, "$mgr · ${g.team(tid).name} · ayda $p puan")
        }
        for (a in out) {
            g.monthAwards += listOf(a.season, a.month, a.kind, a.id, a.text).joinToString("|") { it.toString().replace("|", " ") }
            val ours = if (a.kind == MANAGER) a.id == g.userTeamId else g.player(a.id)?.teamId == g.userTeamId
            if (!ours) continue
            if (a.kind == MANAGER) {
                g.managerRep = (g.managerRep + 2).coerceAtMost(100)
                g.confidence = (g.confidence + 3).coerceAtMost(100.0)
                g.addNews("🏅 $monthName ayının menajeri: ${g.managerName}!", "Ligde ayın en başarılı teknik direktörü seçildin (${a.text}). Yönetim ve taraftar çok memnun.")
            } else g.player(a.id)?.let { p ->
                p.morale = (p.morale + 8).coerceAtMost(100.0)
                g.addNews("🏅 $monthName ayının ${if (a.kind == PLAYER) "oyuncusu" else "genç oyuncusu"}: ${p.name}", "${a.text}. Tebrikler!")
            }
        }
        while (g.monthAwards.size > 60) g.monthAwards.removeAt(0)
        g.monthAwardsNew = true
    }

    /** Son verilen ayın ödülleri. */
    fun latest(g: Game): List<Award> {
        val all = g.monthAwards.mapNotNull { l -> l.split("|").takeIf { it.size >= 5 }?.let { Award(it[0].toInt(), it[1], it[2], it[3].toInt(), it[4]) } }
        val last = all.lastOrNull() ?: return emptyList()
        return all.filter { it.season == last.season && it.month == last.month }
    }
}
