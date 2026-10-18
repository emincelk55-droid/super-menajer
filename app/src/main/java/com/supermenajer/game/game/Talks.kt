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

    val preOptions = listOf(
        "🧘 Sakin olun, kendi oyunumuzu oynayalım",
        "🔥 Onlara kim olduğumuzu gösterin!",
        "😊 Baskı yok, keyfini çıkarın",
        "⚠️ Bu maç bizim için hayati!",
    )
    val halfOptions = listOf("👏 Övgü", "🧘 Sakin", "😠 Sert")
    val postOptions = listOf("👏 Tebrikler", "🙂 Fena değil", "😠 Hayal kırıklığı")

    class Result(val message: String, val reactions: List<String>)

    fun isDerby(g: Game, f: Fixture): Boolean {
        val h = g.team(f.home)
        val a = g.team(f.away)
        return h.city == a.city && Leagues.of(h.division)?.country == "Türkiye" && Leagues.of(a.division)?.country == "Türkiye"
    }

    fun bigMatch(g: Game, f: Fixture) = isDerby(g, f) || Comp.isCup(f.comp) || f.comp == Comp.EURO

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
                    d >= 3 -> "Favori biziz."
                    d <= -3 -> "Rakip bizden güçlü, sürpriz peşindeyiz."
                    else -> "Güçler denk."
                },
            )
            if (isDerby(sim.game, sim.fixture)) append(" DERBİ! Şehrin hâkimi belli olacak.")
            else if (big) append(" Büyük maç, eleme heyecanı.")
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
                0 -> (if (diff >= 3) 5.0 else 2.0) to "Takım sakin ve odaklı."
                1 -> when {
                    diff >= 2 -> 5.0 to "Favori olduklarını biliyorlar, sahaya iştahla çıkıyorlar!"
                    diff <= -2 -> -3.0 to "Güçlü rakip karşısında bu söz oyuncuları baskı altına soktu."
                    else -> 2.0 to "Oyuncular hırslı."
                }
                2 -> when {
                    diff <= -2 -> 6.0 to "Baskısız ortam oyuncuları rahatlattı, sürprize hazırlar!"
                    diff >= 2 -> -3.0 to "Oyuncular maçı fazla hafife aldı."
                    else -> 1.0 to "Oyuncular rahat."
                }
                else -> when {
                    big -> 6.0 to "Maçın önemini biliyorlar, ateş gibiler!"
                    rnd.nextDouble() < 0.5 -> 4.0 to "Oyuncular mesajı aldı."
                    else -> -4.0 to "Fazla baskı! Bazı oyuncular gergin."
                }
            }
            HALF -> when (choice) {
                0 -> when {
                    score > 0 -> 6.0 to "Övgülerin oyuncuları coşturdu!"
                    score == 0 -> 3.0 to "Oyuncular güvenini hissetti."
                    else -> -2.0 to "Geride olmamıza rağmen övgü, oyuncuları fazla rahatlattı."
                }
                1 -> 2.5 to "Sakin konuşman takıma güven verdi."
                else -> when {
                    score < 0 -> if (rnd.nextDouble() < 0.7) 9.0 to "Sert çıkışın oyuncuları ateşledi!" else -5.0 to "Sert çıkışın ters tepti, oyuncular gergin."
                    score == 0 -> if (rnd.nextDouble() < 0.5) 5.0 to "Oyuncular mesajı aldı, daha agresifler." else -3.0 to "Oyuncular sert tavrından rahatsız oldu."
                    else -> -4.0 to "Önde olmamıza rağmen sert çıkışın oyuncuları gerdi."
                }
            }
            else -> when (choice) {
                0 -> when {
                    score > 0 -> 4.0 to "Oyuncular galibiyetin ve övgünün tadını çıkarıyor."
                    score == 0 -> 1.0 to "Oyuncular övgüye şaşırdı ama memnun."
                    else -> -2.0 to "Yenilgiye rağmen övgü, oyuncuları rehavete soktu."
                }
                1 -> (if (score >= 0) 2.0 else 1.0) to "Ölçülü bir konuşma, kimse rahatsız olmadı."
                else -> when {
                    score > 0 -> -5.0 to "Kazandıkları maç sonrası eleştiri oyuncuları kırdı!"
                    score == 0 -> if (rnd.nextDouble() < 0.5) 3.0 to "Oyuncular hırslandı." else -3.0 to "Oyuncular eleştiriyi haksız buldu."
                    else -> if (rnd.nextDouble() < 0.6) 4.0 to "Oyuncular hatalarını kabul etti, bir sonraki maça bileniyorlar." else -5.0 to "Soyunma odasında gerginlik var."
                }
            }
        }
        val changes = sim.applyTalk(kind, base, StaffLogic.talkFactor(sim.game))
        val label = listOf("Maç öncesi konuşma", "Devre arası konuşması", "Maç sonu konuşması")[kind]
        if (kind != POST) sim.note("$label: $msg")
        val reactions = changes.shuffled(rnd).take(3).map { (m, d) ->
            m.p.name + " " + when {
                d >= 5 -> "çok motive 🔥"
                d >= 2 -> "odaklandı 👍"
                d > -2 -> "tepkisiz 😐"
                else -> "gergin 😬"
            }
        }
        return Result(msg, reactions)
    }

    private fun MatchSim.strengthDiffSafe(): Double = runCatching { strengthDiff(this) }.getOrDefault(0.0)
}
