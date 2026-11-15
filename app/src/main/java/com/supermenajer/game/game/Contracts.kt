package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.random.Random

/**
 * Serbest kalma bedeli ve ön sözleşme. İspanya'da her oyuncunun bedeli vardır; diğer liglerde
 * bazı oyuncuların. Bedeli ödeyen kulüp, satıcının onayı olmadan oyuncuyla anlaşabilir.
 * Sözleşmesinin son yılına girmiş oyuncuyla devre arasından sonra ön sözleşme imzalanabilir.
 */
object Contracts {
    fun assignClauses(g: Game, rnd: Random = Random.Default) {
        for (p in g.players) {
            if (p.teamId < 0 || p.releaseClause > 0) continue
            val t = g.teamOrNull(p.teamId) ?: continue
            val spain = com.supermenajer.game.model.Leagues.of(t.division)?.country == "İspanya"
            if (spain || rnd.nextDouble() < 0.2) {
                val raw = p.value * (if (spain) rnd.nextDouble(2.2, 4.0) else rnd.nextDouble(1.8, 3.0))
                p.releaseClause = (raw / 500_000).toLong().coerceAtLeast(1) * 500_000
            }
        }
    }

    /** Serbest kalma bedelini ödeyebilir mi; olmazsa nedeni. */
    fun clauseBlock(g: Game, p: Player): String? = when {
        p.releaseClause <= 0 || p.teamId < 0 -> L("Bu oyuncunun serbest kalma bedeli yok.", "This player has no release clause.")
        p.teamId == g.userTeamId -> L("Zaten senin oyuncun.", "He's already your player.")
        !Season.transferWindowOpen(g) -> L("Transfer dönemi kapalı.", "The transfer window is closed.")
        g.userTeam.budget < p.releaseClause -> L("Bütçe yetmiyor (${money(p.releaseClause)}).", "Not enough budget (${money(p.releaseClause)}).")
        !Economy.fitsWages(g, g.userTeam, p.demandWage) -> L("Maaş bütçesi bu oyuncunun maaşını kaldırmıyor.", "The wage budget can't cover this player's wages.")
        else -> null
    }

    /** Ön sözleşme: başka kulübün son yılındaki oyuncusu, 17. haftadan sonra. */
    fun preBlock(g: Game, p: Player): String? = when {
        p.teamId < 0 || p.teamId == g.userTeamId -> L("Ön sözleşme yalnız başka kulüplerin oyuncularıyla yapılır.", "Pre-contracts can only be agreed with other clubs' players.")
        p.preContract == g.userTeamId -> L("Ön sözleşme zaten imzalandı; sezon sonunda gelecek.", "The pre-contract is already signed; he joins at the end of the season.")
        p.preContract >= 0 -> L("Oyuncu başka bir kulüple ön sözleşme imzaladı.", "The player has signed a pre-contract with another club.")
        p.contractYears > 1 -> L("Sözleşmesinin bitmesine 1 yıldan fazla var.", "More than a year is left on his contract.")
        g.leagueWeekAt(g.week) < 17 && !g.seasonOver -> L("Ön sözleşme sezonun ikinci yarısında (17. haftadan sonra) yapılabilir.", "Pre-contracts can be agreed in the second half of the season (after week 17).")
        g.userTeam.budget < signingBonus(p) -> L("İmza parası için bütçe yetmiyor (${money(signingBonus(p))}).", "Not enough budget for the signing bonus (${money(signingBonus(p))}).")
        !Economy.fitsWages(g, g.userTeam, p.demandWage) -> L("Maaş bütçesi bu oyuncunun maaşını kaldırmıyor.", "The wage budget can't cover this player's wages.")
        else -> null
    }

    fun signingBonus(p: Player): Long = (p.demandWage * 12).coerceAtLeast(50_000)

    /** Oyuncunun kabul olasılığı: büyük kulüp, iyi maaş ve gelişim fırsatı. */
    fun preChance(g: Game, p: Player): Double {
        val us = g.userTeam
        val from = g.teamOrNull(p.teamId)
        val repGap = us.reputation - (from?.reputation ?: 60)
        return (0.55 + repGap * 0.02 + if (p.age >= 30) 0.1 else 0.0).coerceIn(0.1, 0.92)
    }

    fun signPre(g: Game, p: Player, rnd: Random = Random.Default): String {
        preBlock(g, p)?.let { return it }
        if (rnd.nextDouble() > preChance(g, p)) {
            p.preContract = -2 // bu sezon reddetti
            return L("${p.name} teklifini reddetti; kulübünde kalmayı düşünüyor.", "${p.name} turned down your offer; he's thinking of staying at his club.")
        }
        val bonus = signingBonus(p)
        Economy.earn(g.userTeam, Economy.BUYS, -bonus)
        p.preContract = g.userTeamId
        g.addNews(L("✍️ Ön sözleşme", "✍️ Pre-contract"), L("${p.name} ile ön sözleşme imzalandı (imza parası ${money(bonus)}). Sezon sonunda bedelsiz olarak kadromuza katılacak.", "Pre-contract signed with ${p.name} (signing bonus ${money(bonus)}). He'll join us on a free at the end of the season."))
        return L("${p.name} ile ön sözleşme imzalandı!", "Pre-contract signed with ${p.name}!")
    }

    /** Sezon sonu: ön sözleşmeli oyuncular yeni kulübüne geçer. Diğer reddedişler sıfırlanır. */
    fun processPre(g: Game) {
        for (p in g.players) {
            if (p.preContract == -2) p.preContract = -1
        }
    }
}
