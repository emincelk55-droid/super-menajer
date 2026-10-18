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
        if (unhappy.isNotEmpty()) notes += "😟 Mutsuz oyuncular var: ${unhappy.take(3).joinToString { it.name }}. Birebir konuşma iyi gelebilir."

        val recent = g.fixtures.filter { it.played && it.involves(us.id) && Comp.isLeague(it.comp) }.sortedBy { it.week }.takeLast(3)
        val lost = recent.count { (it.home == us.id && it.hg < it.ag) || (it.away == us.id && it.ag < it.hg) }
        if (recent.size == 3 && lost >= 2) notes += "📉 Son 3 maçın ${lost}'ini kaybettik. Daha dengeli bir oyun anlayışı ya da diziliş değişikliği düşünebiliriz."
        if (recent.size == 3 && lost == 0 && recent.all { (it.home == us.id && it.hg > it.ag) || (it.away == us.id && it.ag > it.hg) }) {
            notes += "🔥 Üst üste 3 galibiyet! Takımın morali yüksek, bu düzeni bozmayalım."
        }

        val poor = squad.filter { it.id in us.lineup && it.apps >= 5 && it.avgRating < 6.2 }.minByOrNull { it.avgRating }
        if (poor != null) notes += "🔻 ${poor.name} formsuz (ort. ${"%.1f".format(poor.avgRating)}). Yedeklerden birine şans verebiliriz."

        val star = squad.filter { it.apps >= 5 }.maxByOrNull { it.avgRating }
        if (star != null && star.avgRating >= 7.2) notes += "⭐ ${star.name} harika gidiyor (ort. ${"%.1f".format(star.avgRating)})."

        val young = squad.filter { it.age <= 21 && it.startOvr > 0 && it.overall - it.startOvr >= 2 }.maxByOrNull { it.overall - it.startOvr }
        if (young != null) notes += "🌱 Genç ${young.name} hızla gelişiyor (${young.startOvr} → ${young.overall})."

        val tired = squad.filter { it.id in us.lineup && it.fitness < 70 }
        if (tired.size >= 3) notes += "🥵 İlk 11'de ${tired.size} yorgun oyuncu var. Rotasyon ya da hafif antrenman düşünebiliriz."

        val expiring = squad.filter { it.contractYears == 1 && it.overall >= Lineups.strength(g, us) - 2 }
        if (expiring.isNotEmpty() && leagueWeek >= 15) notes += "📝 Önemli oyuncuların sözleşmesi bitiyor: ${expiring.take(3).joinToString { it.name }}."

        if (Economy.squadWages(g, us.id) > us.wageBudget) notes += "💸 Maaş limitini aştık. Satış yapmadan yeni transfer yapamayız."

        if (notes.isEmpty()) notes += "👍 Her şey yolunda hocam, takım hazır."
        val name = StaffLogic.of(g, StaffLogic.ASSISTANT)?.name ?: "Yardımcı antrenör"
        // Kaliteli yardımcı daha çok konuya değinir
        val count = (1 + StaffLogic.q(g, StaffLogic.ASSISTANT) / 6).coerceIn(1, 4)
        g.addNews("📋 Yardımcı antrenör raporu", "$name: " + notes.shuffled(rnd).take(count).joinToString("\n"))
    }
}
