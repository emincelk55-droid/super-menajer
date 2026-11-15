package com.supermenajer.game.game

import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Game
import kotlin.random.Random

/**
 * CM17 tarzı yardımcı antrenör geri bildirimi: birkaç haftada bir gelen kutusuna
 * kadronun durumu hakkında kısa ve işe yarar bir rapor düşer.
 */
object Assistant {
    fun weekly(g: Game, leagueWeek: Int, rnd: Random) {
        if (g.fired || leagueWeek < 3 || leagueWeek % 3 != 0) return
        val us = g.userTeam
        val squad = g.squad(us.id)
        val notes = mutableListOf<String>()

        val unhappy = squad.filter { it.morale < 35 }
        if (unhappy.isNotEmpty()) notes += L("😟 Mutsuz oyuncular var: ${unhappy.take(3).joinToString { it.name }}. Birebir konuşma iyi gelebilir.", "😟 Some players are unhappy: ${unhappy.take(3).joinToString { it.name }}. A one-to-one talk could help.")

        val recent = g.fixtures.filter { it.played && it.involves(us.id) && Comp.isLeague(it.comp) }.sortedBy { it.week }.takeLast(3)
        val lost = recent.count { (it.home == us.id && it.hg < it.ag) || (it.away == us.id && it.ag < it.hg) }
        if (recent.size == 3 && lost >= 2) notes += L("📉 Son 3 maçın ${lost}'ini kaybettik. Daha dengeli bir oyun anlayışı ya da diziliş değişikliği düşünebiliriz.", "📉 We lost ${lost} of the last 3 matches. We could consider a more balanced approach or a change of formation.")
        if (recent.size == 3 && lost == 0 && recent.all { (it.home == us.id && it.hg > it.ag) || (it.away == us.id && it.ag > it.hg) }) {
            notes += L("🔥 Üst üste 3 galibiyet! Takımın morali yüksek, bu düzeni bozmayalım.", "🔥 Three wins in a row! Morale is high, let's not change a winning formula.")
        }

        val poor = squad.filter { it.id in us.lineup && it.apps >= 5 && it.avgRating < 6.2 }.minByOrNull { it.avgRating }
        if (poor != null) notes += L("🔻 ${poor.name} formsuz (ort. ${"%.1f".format(poor.avgRating)}). Yedeklerden birine şans verebiliriz.", "🔻 ${poor.name} is out of form (avg. ${"%.1f".format(poor.avgRating)}). We could give a substitute a chance.")

        val star = squad.filter { it.apps >= 5 }.maxByOrNull { it.avgRating }
        if (star != null && star.avgRating >= 7.2) notes += L("⭐ ${star.name} harika gidiyor (ort. ${"%.1f".format(star.avgRating)}).", "⭐ ${star.name} is doing brilliantly (avg. ${"%.1f".format(star.avgRating)}).")

        val young = squad.filter { it.age <= 21 && it.startOvr > 0 && it.overall - it.startOvr >= 2 }.maxByOrNull { it.overall - it.startOvr }
        if (young != null) notes += L("🌱 Genç ${young.name} hızla gelişiyor (${young.startOvr} → ${young.overall}).", "🌱 Young ${young.name} is developing fast (${young.startOvr} → ${young.overall}).")

        val tired = squad.filter { it.id in us.lineup && it.fitness < 70 }
        if (tired.size >= 3) notes += L("🥵 İlk 11'de ${tired.size} yorgun oyuncu var. Rotasyon ya da hafif antrenman düşünebiliriz.", "🥵 There are ${tired.size} tired players in the starting XI. We could rotate or ease off in training.")

        val expiring = squad.filter { it.contractYears == 1 && it.overall >= Lineups.strength(g, us) - 2 }
        if (expiring.isNotEmpty() && leagueWeek >= 15) notes += L("📝 Önemli oyuncuların sözleşmesi bitiyor: ${expiring.take(3).joinToString { it.name }}.", "📝 Key players' contracts are ending: ${expiring.take(3).joinToString { it.name }}.")

        if (Economy.squadWages(g, us.id) > us.wageBudget) notes += L("💸 Maaş limitini aştık. Satış yapmadan yeni transfer yapamayız.", "💸 We're over the wage limit. We can't sign anyone without selling first.")

        if (notes.isEmpty()) notes += L("👍 Her şey yolunda hocam, takım hazır.", "👍 All good, boss, the team is ready.")
        val name = StaffLogic.of(g, StaffLogic.ASSISTANT)?.name ?: L("Yardımcı antrenör", "Assistant manager")
        // Kaliteli yardımcı daha çok konuya değinir
        val count = (1 + StaffLogic.q(g, StaffLogic.ASSISTANT) / 6).coerceIn(1, 4)
        g.addNews(L("📋 Yardımcı antrenör raporu", "📋 Assistant manager report"), "$name: " + notes.shuffled(rnd).take(count).joinToString("\n"))
    }
}
