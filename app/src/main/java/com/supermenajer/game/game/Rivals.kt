package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.PressQuestion
import com.supermenajer.game.model.Team
import kotlin.random.Random

/**
 * Rakip menajerler: büyük maçlardan bir gün önce rakip hoca basına laf atar, sen cevap verirsin.
 * Cevabın kendi takımının moralini, rakibin hırsını ve taraftarı etkiler. Kafa kafaya karnesi tutulur.
 */
object Rivals {
    const val PRESS_KIND = 6

    private val quotes = listOf(
        "Bu sezon onlardan daha iyi bir takımız, sahada da göstereceğiz.",
        "Onların hocası çok konuşuyor; biz konuşmayı sahaya bırakıyoruz.",
        "Deplasmanda bile 3 puan için geliyoruz.",
        "Savunmaları bizim hücum hattımıza dayanamaz.",
        "Ligin en abartılan takımı onlar.",
        "Geçen maçın rövanşını alacağız.",
    )

    val answers = listOf(
        "Sahada cevap vereceğiz.",
        "Onlar konuşsun, biz kazanırız!",
        "Saygı duyuyoruz, güçlü bir rakip.",
    )

    /** Rakip sayılır mı: derbi ya da ligde güçlü/yakın bir takım. */
    private fun isRival(g: Game, opp: Team): Boolean {
        val us = g.userTeam
        val sameCity = opp.city == us.city
        return sameCity || opp.reputation >= us.reputation - 4
    }

    /** Maçtan bir gün önce rakip hocadan açıklama. */
    fun maybeJab(g: Game, rnd: Random = Random.Default) {
        if (g.fired || g.press != null) return
        val f = g.nextUserFixture() ?: return
        if (GameCalendar.daysUntil(g, f.week) != 1 || g.lastJabWeek == f.week) return
        val opp = g.team(if (f.home == g.userTeamId) f.away else f.home)
        val mgr = g.managers[opp.id] ?: return
        if (!isRival(g, opp) || rnd.nextDouble() > 0.55) return
        g.lastJabWeek = f.week
        val (w, d, l) = h2h(g, mgr)
        val rec = if (w + d + l > 0) " (Karşılaşmalarınız: $w G · $d B · $l M)" else ""
        val text = "${opp.name} hocası $mgr: \"${quotes[rnd.nextInt(quotes.size)]}\"$rec. Ne cevap vereceksin?"
        g.press = PressQuestion(text, answers, PRESS_KIND)
        g.addNews("🎙️ $mgr'den laf sokma!", text)
    }

    /** Cevabın etkisi: (moral, rakip hırsı, taraftar). */
    fun answer(g: Game, index: Int): String {
        val (morale, rival, fans, fb) = when (index) {
            1 -> listOf(4.0, 0.03, 3.0, "Oyuncular gaza geldi, taraftar bayıldı! Ama rakip de bu sözlerle hırslandı.")
            2 -> listOf(0.0, -0.01, -1.0, "Centilmence bir cevap. Rakip biraz rehavete kapıldı.")
            else -> listOf(2.0, 0.0, 1.0, "Sakin ve kendinden emin. Odak maçta.")
        }
        for (p in g.squad(g.userTeamId)) p.morale = (p.morale + (morale as Double)).coerceIn(5.0, 100.0)
        g.rivalBoost = rival as Double
        if ((fans as Double) != 0.0) Fans.change(g, fans, "Basın atışması")
        g.press = null
        return fb as String
    }

    fun h2h(g: Game, mgr: String): Triple<Int, Int, Int> {
        val c = g.h2h[mgr]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return Triple(0, 0, 0)
        return Triple(c.getOrElse(0) { 0 }, c.getOrElse(1) { 0 }, c.getOrElse(2) { 0 })
    }

    /** Maç sonrası: rakip hocayla karne. */
    fun record(g: Game, opp: Team, gf: Int, ga: Int) {
        val mgr = g.managers[opp.id] ?: return
        val (w, d, l) = h2h(g, mgr)
        g.h2h[mgr] = when { gf > ga -> "${w + 1},$d,$l"; gf == ga -> "$w,${d + 1},$l"; else -> "$w,$d,${l + 1}" }
        g.rivalBoost = 0.0
    }
}
