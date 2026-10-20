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
        p.releaseClause <= 0 || p.teamId < 0 -> "Bu oyuncunun serbest kalma bedeli yok."
        p.teamId == g.userTeamId -> "Zaten senin oyuncun."
        !Season.transferWindowOpen(g) -> "Transfer dönemi kapalı."
        g.userTeam.budget < p.releaseClause -> "Bütçe yetmiyor (${money(p.releaseClause)})."
        !Economy.fitsWages(g, g.userTeam, p.demandWage) -> "Maaş bütçesi bu oyuncunun maaşını kaldırmıyor."
        else -> null
    }

    /** Ön sözleşme: başka kulübün son yılındaki oyuncusu, 17. haftadan sonra. */
    fun preBlock(g: Game, p: Player): String? = when {
        p.teamId < 0 || p.teamId == g.userTeamId -> "Ön sözleşme yalnız başka kulüplerin oyuncularıyla yapılır."
        p.preContract == g.userTeamId -> "Ön sözleşme zaten imzalandı; sezon sonunda gelecek."
        p.preContract >= 0 -> "Oyuncu başka bir kulüple ön sözleşme imzaladı."
        p.contractYears > 1 -> "Sözleşmesinin bitmesine 1 yıldan fazla var."
        g.leagueWeekAt(g.week) < 17 && !g.seasonOver -> "Ön sözleşme sezonun ikinci yarısında (17. haftadan sonra) yapılabilir."
        g.userTeam.budget < signingBonus(p) -> "İmza parası için bütçe yetmiyor (${money(signingBonus(p))})."
        !Economy.fitsWages(g, g.userTeam, p.demandWage) -> "Maaş bütçesi bu oyuncunun maaşını kaldırmıyor."
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
            return "${p.name} teklifini reddetti; kulübünde kalmayı düşünüyor."
        }
        val bonus = signingBonus(p)
        Economy.earn(g.userTeam, Economy.BUYS, -bonus)
        p.preContract = g.userTeamId
        g.addNews("✍️ Ön sözleşme", "${p.name} ile ön sözleşme imzalandı (imza parası ${money(bonus)}). Sezon sonunda bedelsiz olarak kadromuza katılacak.")
        return "${p.name} ile ön sözleşme imzalandı!"
    }

    /** Sezon sonu: ön sözleşmeli oyuncular yeni kulübüne geçer. Diğer reddedişler sıfırlanır. */
    fun processPre(g: Game) {
        for (p in g.players) {
            if (p.preContract == -2) p.preContract = -1
        }
    }
}
