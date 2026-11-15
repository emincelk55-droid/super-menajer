package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues

/**
 * CM tarzı takım konuşmaları. Etki; maçın bağlamına (favori/sürpriz, derbi, kupa, skor),
 * seçilen tona ve yardımcı antrenörün kalitesine göre değişir.
 */
object Talks {
    const val PRE = 0
    const val HALF = 1
    const val POST = 2

    private val preOptionsCache = LangCached { listOf(
        L("🧘 Sakin olun, kendi oyunumuzu oynayalım", "🧘 Stay calm, let's play our own game"),
        L("🔥 Onlara kim olduğumuzu gösterin!", "🔥 Show them who we are!"),
        L("😊 Baskı yok, keyfini çıkarın", "😊 No pressure, enjoy it"),
        L("⚠️ Bu maç bizim için hayati!", "⚠️ This match is vital for us!"),
    ) }
    val preOptions get() = preOptionsCache.get()
    private val halfOptionsCache = LangCached { listOf(L("👏 Övgü", "👏 Praise"), L("🧘 Sakin", "🧘 Calm"), L("😠 Sert", "😠 Harsh")) }
    val halfOptions get() = halfOptionsCache.get()
    private val postOptionsCache = LangCached { listOf(L("👏 Tebrikler", "👏 Well done"), L("🙂 Fena değil", "🙂 Not bad"), L("😠 Yetersiz", "😠 Not good enough")) }
    val postOptions get() = postOptionsCache.get()

    class Result(val message: String, val reactions: List<String>)

    /**
     * Derbi: aynı şehrin iki kulübü (İstanbul, Madrid, Milano, Londra...) ya da aynı ligin iki devi (ikisinin de
     * itibarı 86+, "klasik"). Milli maçlar derbi sayılmaz.
     */
    fun isDerby(g: Game, f: Fixture): Boolean = derbyName(g, f) != null

    /** Önümüzdeki 7 gün içindeki derbimiz (yoksa null) ve kalan gün. */
    fun upcomingDerby(g: Game): Pair<Fixture, Long>? {
        val f = g.nextUserFixture() ?: return null
        if (!isDerby(g, f)) return null
        val days = com.supermenajer.game.game.GameCalendar.dateOf(g, f.week) - g.date
        return if (days in 0..7) f to days else null
    }

    /** Derbinin adı ("İstanbul Derbisi", "Klasik"); derbi değilse null. */
    fun derbyName(g: Game, f: Fixture): String? {
        if (f.comp == Comp.NATIONAL) return null
        val h = g.teamOrNull(f.home) ?: return null
        val a = g.teamOrNull(f.away) ?: return null
        if (h.city.isNotBlank() && h.city.equals(a.city, ignoreCase = true) && Leagues.of(h.division)?.country == Leagues.of(a.division)?.country) {
            return L("${h.city} Derbisi", "${h.city} Derby")
        }
        if (h.division == a.division && h.reputation >= 86 && a.reputation >= 86) return L("Dev Maç · Klasik", "Big Match · Classic")
        return null
    }

    fun bigMatch(g: Game, f: Fixture) = f.comp == Comp.NATIONAL || isDerby(g, f) || Comp.isCup(f.comp) || f.comp == Comp.EURO

    /** Güç farkı (+ ise biz favoriyiz). Ev sahibi avantajı dahil. */
    fun strengthDiff(sim: MatchSim): Double {
        val us = sim.userSide ?: return 0.0
        val them = sim.other(us)
        val home = if (us === sim.home) 1.5 else -1.5
        return Lineups.strength(sim.game, us.team) - Lineups.strength(sim.game, them.team) + home
    }

    fun contextText(sim: MatchSim): String {
        val d = strengthDiff(sim)
        val big = bigMatch(sim.game, sim.fixture)
        return buildString {
            append(
                when {
                    d >= 3 -> L("Favori biziz.", "We're the favourites.")
                    d <= -3 -> L("Rakip bizden güçlü, sürpriz peşindeyiz.", "They're stronger than us, we're looking for an upset.")
                    else -> L("Güçler denk.", "The sides are evenly matched.")
                },
            )
            if (isDerby(sim.game, sim.fixture)) append(L(" DERBİ! Şehrin hâkimi belli olacak.", " DERBY! The city will find out who rules it."))
            else if (big) append(L(" Büyük maç, eleme heyecanı.", " A big match, knockout excitement."))
        }
    }

    fun talk(sim: MatchSim, kind: Int, choice: Int): Result {
        val us = sim.userSide ?: return Result("", emptyList())
        if (sim.talksDone[kind]) return Result("", emptyList())
        val rnd = sim.random
        val diff = sim.strengthDiffSafe()
        val big = bigMatch(sim.game, sim.fixture)
        val score = us.goals - sim.other(us).goals
        val (base, msg) = when (kind) {
            PRE -> when (choice) {
                0 -> (if (diff >= 3) 5.0 else 2.0) to L("Takım sakin ve odaklı.", "The team is calm and focused.")
                1 -> when {
                    diff >= 2 -> 5.0 to L("Favori olduklarını biliyorlar, sahaya iştahla çıkıyorlar!", "They know they're favourites and take the field hungry!")
                    diff <= -2 -> -3.0 to L("Güçlü rakip karşısında bu söz oyuncuları baskı altına soktu.", "Against a strong opponent your words put the players under pressure.")
                    else -> 2.0 to L("Oyuncular hırslı.", "The players are driven.")
                }
                2 -> when {
                    diff <= -2 -> 6.0 to L("Baskısız ortam oyuncuları rahatlattı, sürprize hazırlar!", "The pressure-free mood relaxed the players, they're ready for an upset!")
                    diff >= 2 -> -3.0 to L("Oyuncular maçı fazla hafife aldı.", "The players took the match too lightly.")
                    else -> 1.0 to L("Oyuncular rahat.", "The players are relaxed.")
                }
                else -> when {
                    big -> 6.0 to L("Maçın önemini biliyorlar, ateş gibiler!", "They know how important the match is, they're on fire!")
                    rnd.nextDouble() < 0.5 -> 4.0 to L("Oyuncular mesajı aldı.", "The players got the message.")
                    else -> -4.0 to L("Fazla baskı! Bazı oyuncular gergin.", "Too much pressure! Some players are tense.")
                }
            }
            HALF -> when (choice) {
                0 -> when {
                    score > 0 -> 6.0 to L("Övgülerin oyuncuları coşturdu!", "Your praise fired the players up!")
                    score == 0 -> 3.0 to L("Oyuncular güvenini hissetti.", "The players felt your trust.")
                    else -> -2.0 to L("Geride olmamıza rağmen övgü, oyuncuları fazla rahatlattı.", "Praise while we're behind made the players too comfortable.")
                }
                1 -> 2.5 to L("Sakin konuşman takıma güven verdi.", "Your calm talk gave the team confidence.")
                else -> when {
                    score < 0 -> if (rnd.nextDouble() < 0.7) 9.0 to L("Sert çıkışın oyuncuları ateşledi!", "Your outburst fired the players up!") else -5.0 to L("Sert çıkışın ters tepti, oyuncular gergin.", "Your outburst backfired, the players are tense.")
                    score == 0 -> if (rnd.nextDouble() < 0.5) 5.0 to L("Oyuncular mesajı aldı, daha agresifler.", "The players got the message and are more aggressive.") else -3.0 to L("Oyuncular sert tavrından rahatsız oldu.", "The players didn't like your harsh tone.")
                    else -> -4.0 to L("Önde olmamıza rağmen sert çıkışın oyuncuları gerdi.", "Your harshness while ahead made the players tense.")
                }
            }
            else -> when (choice) {
                0 -> when {
                    score > 0 -> 4.0 to L("Oyuncular galibiyetin ve övgünün tadını çıkarıyor.", "The players are enjoying the win and the praise.")
                    score == 0 -> 1.0 to L("Oyuncular övgüye şaşırdı ama memnun.", "The players were surprised by the praise but pleased.")
                    else -> -2.0 to L("Yenilgiye rağmen övgü, oyuncuları rehavete soktu.", "Praise despite the defeat made the players complacent.")
                }
                1 -> (if (score >= 0) 2.0 else 1.0) to L("Ölçülü bir konuşma, kimse rahatsız olmadı.", "A measured talk, nobody was upset.")
                else -> when {
                    score > 0 -> -5.0 to L("Kazandıkları maç sonrası eleştiri oyuncuları kırdı!", "Criticism after a win hurt the players!")
                    score == 0 -> if (rnd.nextDouble() < 0.5) 3.0 to L("Oyuncular hırslandı.", "The players are driven.") else -3.0 to L("Oyuncular eleştiriyi haksız buldu.", "The players found the criticism unfair.")
                    else -> if (rnd.nextDouble() < 0.6) 4.0 to L("Oyuncular hatalarını kabul etti, bir sonraki maça bileniyorlar.", "The players accepted their mistakes and are eager for the next match.") else -5.0 to L("Soyunma odasında gerginlik var.", "There's tension in the dressing room.")
                }
            }
        }
        val changes = sim.applyTalk(kind, base, StaffLogic.talkFactor(sim.game))
        val label = listOf(L("Maç öncesi konuşma", "Pre-match talk"), L("Devre arası konuşması", "Half-time talk"), L("Maç sonu konuşması", "Post-match talk"))[kind]
        if (kind != POST) sim.note("$label: $msg")
        val reactions = changes.shuffled(rnd).take(3).map { (m, d) ->
            m.p.name + " " + when {
                d >= 5 -> L("çok motive 🔥", "very motivated 🔥")
                d >= 2 -> L("odaklandı 👍", "focused 👍")
                d > -2 -> L("tepkisiz 😐", "unmoved 😐")
                else -> L("gergin 😬", "tense 😬")
            }
        }
        return Result(msg, reactions)
    }

    private fun MatchSim.strengthDiffSafe(): Double = runCatching { strengthDiff(this) }.getOrDefault(0.0)
}
