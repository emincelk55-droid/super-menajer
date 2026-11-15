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

    private val quotesCache = LangCached { listOf(
        L("Bu sezon onlardan daha iyi bir takımız, sahada da göstereceğiz.", "We're a better team than them this season, and we'll show it on the pitch."),
        L("Onların hocası çok konuşuyor; biz konuşmayı sahaya bırakıyoruz.", "Their manager talks a lot; we let our football do the talking."),
        L("Deplasmanda bile 3 puan için geliyoruz.", "Even away from home, we're coming for 3 points."),
        L("Savunmaları bizim hücum hattımıza dayanamaz.", "Their defence can't cope with our attack."),
        L("Ligin en abartılan takımı onlar.", "They're the most overrated team in the league."),
        L("Geçen maçın rövanşını alacağız.", "We'll get revenge for the last game."),
    ) }
    private val quotes get() = quotesCache.get()

    private val answersCache = LangCached { listOf(
        L("Sahada cevap vereceğiz.", "We'll answer on the pitch."),
        L("Onlar konuşsun, biz kazanırız!", "Let them talk, we'll win!"),
        L("Saygı duyuyoruz, güçlü bir rakip.", "We respect them, they're a strong side."),
    ) }
    val answers get() = answersCache.get()

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
        val rec = if (w + d + l > 0) L(" (Karşılaşmalarınız: $w G · $d B · $l M)", " (Your record: $w W · $d D · $l L)") else ""
        val text = L("${opp.name} hocası $mgr: \"${quotes[rnd.nextInt(quotes.size)]}\"$rec. Ne cevap vereceksin?", "${opp.name} manager $mgr: \"${quotes[rnd.nextInt(quotes.size)]}\"$rec. How will you respond?")
        g.press = PressQuestion(text, answers, PRESS_KIND)
        g.addNews(L("🎙️ $mgr'den laf sokma!", "🎙️ A dig from $mgr!"), text)
    }

    /** Cevabın etkisi: (moral, rakip hırsı, taraftar). */
    fun answer(g: Game, index: Int): String {
        val (morale, rival, fans, fb) = when (index) {
            1 -> listOf(4.0, 0.03, 3.0, L("Oyuncular gaza geldi, taraftar bayıldı! Ama rakip de bu sözlerle hırslandı.", "The players are fired up and the fans loved it! But the opponent is also motivated by your words."))
            2 -> listOf(0.0, -0.01, -1.0, L("Centilmence bir cevap. Rakip biraz rehavete kapıldı.", "A gentlemanly reply. The opponent got a little complacent."))
            else -> listOf(2.0, 0.0, 1.0, L("Sakin ve kendinden emin. Odak maçta.", "Calm and confident. The focus is on the match."))
        }
        for (p in g.squad(g.userTeamId)) p.morale = (p.morale + (morale as Double)).coerceIn(5.0, 100.0)
        g.rivalBoost = rival as Double
        if ((fans as Double) != 0.0) Fans.change(g, fans, L("Basın atışması", "Press war"))
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
