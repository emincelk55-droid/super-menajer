package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Player
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Championship Manager tarzı gizli özellikler: başka kulüplerin oyuncularını tam olarak
 * ancak gözlemci raporundan sonra bilirsin. Gözlemci her hafta seçtiğin ülkeden rapor getirir.
 */
object Scouting {
    const val COST = 10_000L
    val focusNames = listOf("Genç yetenek", "Hazır oyuncu", "Kelepir")

    fun known(g: Game, p: Player): Boolean =
        p.teamId == g.userTeamId || p.loanFrom == g.userTeamId || p.id in g.scouted

    /** Bilinmeyen oyuncunun güç belirsizliği: kendi liginde ±, diğer liglerde daha geniş. */
    fun uncertainty(g: Game, p: Player): Int {
        if (known(g, p)) return 0
        val t = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
        return if (t != null && t.division == g.userTeam.division) 3 else 6
    }

    /** Değeri içeren ama ortalanmamış bir aralık (oyuncuya göre sabit). */
    fun range(g: Game, p: Player, v: Int): IntRange {
        val u = uncertainty(g, p)
        if (u == 0) return v..v
        val off = (p.id * 7 + v) % (2 * u + 1)
        val lo = (v - off).coerceAtLeast(1)
        return lo..(lo + 2 * u).coerceAtMost(99)
    }

    fun ovrText(g: Game, p: Player): String = range(g, p, p.overall).let { if (it.first == it.last) "${it.first}" else "${it.first}-${it.last}" }

    /** Sıralama ve filtreler için tahmini güç (gerçek değeri sızdırmaz). */
    fun estimate(g: Game, p: Player): Int = range(g, p, p.overall).let { (it.first + it.last) / 2 }

    fun potText(g: Game, p: Player): String = if (known(g, p)) "${p.potential}" else "?"

    fun requested(g: Game, p: Player) = p.id in g.scoutQueue

    /** Gözlemciyi gönder: ücret alınır, rapor ertesi gün gelir. Hata mesajı döner. */
    fun request(g: Game, p: Player): String? {
        if (known(g, p)) return "Bu oyuncuyu zaten tanıyoruz."
        if (requested(g, p)) return "Gözlemci zaten bu oyuncuyu izliyor."
        val us = g.userTeam
        if (us.budget < COST) return "Gözlem için bütçe yetersiz."
        Economy.earn(us, Economy.SCOUTING, -COST)
        g.scoutQueue += p.id
        return null
    }

    private fun stars(x: Double): String {
        val half = (x * 2).roundToInt().coerceIn(1, 10)
        return "★".repeat(half / 2) + (if (half % 2 == 1) "½" else "") + "☆".repeat(5 - (half + 1) / 2)
    }

    /** Yetenek ve potansiyeli takımının gücüne göre yıldızlar ve bir yorum. */
    fun report(g: Game, p: Player): String {
        val str = Lineups.strength(g, g.userTeam)
        val ab = ((p.overall - (str - 15)) / 5.0).coerceIn(0.5, 5.0)
        val po = ((p.potential - (str - 15)) / 5.0).coerceIn(0.5, 5.0)
        val verdict = when {
            p.age <= 21 && p.potential >= str + 8 -> "Geleceğin yıldızı! Kaçırmamalıyız."
            p.overall >= str + 3 -> "Takımı hemen bir üst seviyeye taşır."
            p.overall >= str - 1 -> "İlk 11'imize katkı verir."
            p.potential >= str + 2 -> "Gelişime açık, sabır isteyen bir yatırım."
            else -> "Şu an seviyemizin altında."
        }
        return "Yetenek ${stars(ab)} · Potansiyel ${stars(po)} · \"$verdict\""
    }

    /** Gün sonunda: bekleyen raporlar ve (lig haftalarında) gözlemcinin haftalık önerisi. */
    fun processDay(g: Game, leagueDay: Boolean, rnd: Random) {
        for (id in g.scoutQueue) {
            val p = g.player(id) ?: continue
            g.scouted += id
            if (id !in g.shortlist) g.shortlist += id
            val club = if (p.teamId >= 0) g.teamOrNull(p.teamId)?.name ?: "-" else "Serbest"
            g.addNews("Gözlemci raporu: ${p.name}", "${p.age} yaş, ${p.pos.label}, $club. Güç ${p.overall}, potansiyel ${p.potential}. ${report(g, p)}")
        }
        g.scoutQueue.clear()
        if (!leagueDay || g.fired) return
        val country = g.scoutCountry.ifEmpty { g.userCountry }
        val str = Lineups.strength(g, g.userTeam)
        val pool = g.players.filter { p ->
            p.teamId != g.userTeamId && p.loanFrom < 0 && p.id !in g.scouted &&
                (if (p.teamId < 0) true else g.teamOrNull(p.teamId)?.let { g.countryOf(it) == country } == true)
        }
        val pick = when (g.scoutFocus) {
            0 -> pool.filter { it.age <= 20 }.sortedByDescending { it.potential }.take(12)
            1 -> pool.filter { it.age in 21..30 && it.overall >= str - 2 }.sortedByDescending { it.overall }.take(15)
            else -> pool.filter { it.overall >= str - 4 && GameRepo.askingPrice(it) <= g.userTeam.budget * 0.4 }
                .sortedByDescending { it.overall * 1_000_000.0 / (it.value + 100_000) }.take(12)
        }.randomOrNull(rnd) ?: return
        g.scouted += pick.id
        if (pick.id !in g.shortlist) g.shortlist += pick.id
        while (g.shortlist.size > 40) g.shortlist.removeAt(0)
        val club = if (pick.teamId >= 0) g.teamOrNull(pick.teamId)?.name ?: "-" else "serbest oyuncu"
        g.addNews(
            "🔍 Gözlemci önerisi (${Leagues.flagOf(country).let { Generator.flag(it) }} $country)",
            "${pick.name} · ${pick.age} yaş · ${pick.pos.label} · $club. ${report(g, pick)} Transfer → Gözlem listesi'nde.",
        )
    }
}
