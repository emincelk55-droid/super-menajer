package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * FM / CM tarzı diyaloglu transfer görüşmesi:
 * 1) Kulüple bonservis: bedel, peşin ya da taksit, sonraki satıştan pay veya kiralama.
 * 2) Menajerle sözleşme: haftalık maaş, süre, imza parası ve söz verilen kadro rolü.
 * Her tarafın bir sabrı vardır; ciddi olmayan teklifler sabrı hızla tüketir.
 */
object TransferTalks {
    const val CLUB = 0
    const val AGENT = 1
    const val DONE = 2
    const val FAILED = 3

    val roleNames = listOf("Yıldız oyuncu", "İlk 11 oyuncusu", "Rotasyon", "Yedek", "Genç yetenek")

    class Line(val ours: Boolean, val who: String, val text: String)

    class Negotiation(val player: Player) {
        var stage = CLUB
        val log = mutableListOf<Line>()
        var clubPatience = 3
        var agentPatience = 3
        var fee = 0L
        var installments = false
        var sellOn = 0
        var loan = false
        var agreedFee = 0L
        var wage = 0L
        var years = 3
        var bonus = 0L
        var role = 2
        var counterFee: Long? = null
        var counterWage: Long? = null
    }

    private fun r25k(v: Double) = (v / 25_000).roundToLong().coerceAtLeast(1) * 25_000
    private fun r500(v: Double) = (v / 500).roundToLong().coerceAtLeast(1) * 500

    fun sellerName(g: Game, p: Player) = if (p.teamId >= 0) g.team(p.teamId).name else "Serbest"

    /** Oyuncunun takımımızdaki beklenen rolü. */
    fun expectedRole(g: Game, p: Player): Int {
        val str = Lineups.strength(g, g.userTeam)
        return when {
            p.age <= 20 && p.potential >= str + 3 && p.overall < str - 2 -> 4
            p.overall >= str + 4 -> 0
            p.overall >= str - 1 -> 1
            p.overall >= str - 5 -> 2
            else -> 3
        }
    }

    private fun preferredYears(p: Player): IntRange = when {
        p.age <= 23 -> 4..5
        p.age <= 29 -> 3..4
        p.age <= 32 -> 2..3
        else -> 1..2
    }

    fun start(g: Game, p: Player): Negotiation {
        val n = Negotiation(p)
        val asking = GameRepo.askingPrice(p)
        n.role = expectedRole(g, p)
        n.years = preferredYears(p).first
        n.wage = p.demandWage
        if (p.teamId < 0) {
            n.stage = AGENT
            n.bonus = r25k(p.demandWage * 6.0)
            n.log += Line(false, "Menajer", "Merhaba hocam. ${p.name} serbest ve birkaç kulüpten ilgi görüyor. Haftalık ${money(p.demandWage)} civarı ve iyi bir imza parası bekliyoruz.")
            return n
        }
        val seller = g.team(p.teamId)
        n.fee = r25k(asking * 0.85)
        val key = p.id in seller.lineup
        n.log += Line(
            false, "${seller.name} sportif direktörü",
            when {
                p.listed -> "${p.name} satış listemizde. Makul bir teklif gelirse görüşürüz."
                key -> "${p.name} takımımızın önemli bir parçası. Onu bırakmak için çok iyi bir teklif görmemiz gerek."
                else -> "${p.name} için teklifinizi dinliyoruz. Rakamı duyalım."
            },
        )
        return n
    }

    /** Kulübe teklif. */
    fun clubOffer(g: Game, n: Negotiation) {
        val p = n.player
        if (p.teamId < 0) return
        val seller = g.team(p.teamId)
        val dir = "${seller.name} sportif direktörü"
        val us = g.userTeam
        if (n.loan) {
            n.log += Line(true, "Sen", "${p.name}'i sezon sonuna kadar kiralamak istiyoruz. Maaşının tamamını biz ödeyeceğiz.")
            val ok = p.id !in seller.lineup && p.age <= 25 && g.squad(seller.id).size > 20
            if (ok) {
                n.agreedFee = 0
                n.stage = AGENT
                n.log += Line(false, dir, "Oynama süresi bulacaksa olur. Kiralamayı kabul ediyoruz, oyuncuyla konuşabilirsiniz.")
                agentOpening(g, n)
            } else {
                n.clubPatience--
                n.log += Line(false, dir, if (p.id in seller.lineup) "Kendisi ilk 11'imizde, kiralık vermeyiz." else "Bu oyuncuyu kiralık vermeyi düşünmüyoruz.")
                if (n.clubPatience <= 0) fail(n, dir, "Görüşmeleri burada bitirelim.")
            }
            return
        }
        val terms = buildString {
            append("${money(n.fee)}")
            if (n.installments) append(", yarısı peşin yarısı gelecek sezon")
            if (n.sellOn > 0) append(", sonraki satıştan %${n.sellOn} pay")
        }
        n.log += Line(true, "Sen", "$terms teklif ediyoruz.")
        if (us.budget < if (n.installments) n.fee / 2 else n.fee) {
            n.log += Line(false, "Mali direktörün", "Hocam bu rakam için bütçemiz yetmiyor.")
            return
        }
        val asking = GameRepo.askingPrice(p).toDouble()
        val eff = n.fee * (1 + 0.04 * n.sellOn / 10.0) * (if (n.installments) 0.95 else 1.0)
        when {
            eff >= asking || (n.counterFee != null && n.fee >= n.counterFee!!) -> {
                n.agreedFee = n.fee
                n.stage = AGENT
                n.log += Line(false, dir, listOf("Anlaştık! Oyuncuyla görüşmenize izin veriyoruz.", "Teklifinizi kabul ediyoruz. Şimdi menajeriyle konuşabilirsiniz.", "Tamam, el sıkışalım. Oyuncunun kararı artık.").random())
                agentOpening(g, n)
            }
            eff >= asking * 0.8 -> {
                n.clubPatience--
                val need = r25k(asking / ((1 + 0.04 * n.sellOn / 10.0) * (if (n.installments) 0.95 else 1.0)))
                val c = r25k((n.fee + need) / 2.0).coerceAtLeast(n.fee + 25_000)
                n.counterFee = c
                n.log += Line(false, dir, listOf("Yaklaştınız ama yeterli değil. ${money(c)} olursa anlaşırız.", "${money(c)} bizim son rakamımıza yakın. Düşünün.", "Bu teklifle olmaz. ${money(c)} verirseniz masadan kalkmayız.").random())
                if (n.clubPatience <= 0) fail(n, dir, "Sabrımız tükendi, görüşmeleri bitiriyoruz.")
            }
            else -> {
                n.clubPatience -= 2
                n.log += Line(false, dir, listOf("Bu teklif ciddi değil.", "Şaka mı yapıyorsunuz? Oyuncunun değerinin çok altında.", "Bu rakamı konuşmayız bile.").random())
                if (n.clubPatience <= 0) fail(n, dir, "Görüşmeleri kesiyoruz.")
            }
        }
    }

    private fun agentOpening(g: Game, n: Negotiation) {
        val p = n.player
        val exp = expectedRole(g, p)
        n.role = exp
        n.wage = p.demandWage
        n.years = preferredYears(p).first
        n.log += Line(
            false, "Menajer",
            "Müvekkilim ${g.userTeam.name}'a gelmeye sıcak bakıyor. Haftalık ${money(p.demandWage)}, ${preferredYears(p).first}-${preferredYears(p).last} yıllık sözleşme ve kendisine '${roleNames[exp]}' rolü bekliyor.",
        )
    }

    /** Menajerin memnuniyeti: 1.0 ve üstü kabul. */
    fun agentScore(g: Game, n: Negotiation): Double {
        val p = n.player
        val demand = p.demandWage.coerceAtLeast(500).toDouble()
        var s = n.wage / demand
        s += (n.bonus / (demand * 26)).coerceAtMost(0.3)
        val exp = expectedRole(g, p)
        // Daha büyük rol sözü oyuncuyu memnun eder (yıldız=0 en büyük)
        val diff = exp - n.role
        s += if (diff > 0) 0.06 * diff else 0.15 * diff
        val pref = preferredYears(p)
        if (n.years !in pref) s -= 0.04 * minOf(abs(n.years - pref.first), abs(n.years - pref.last))
        if (n.loan) s += 0.1
        s += Perks.agentBonus(g)
        return s
    }

    /** Menajere sözleşme teklifi. */
    fun agentOffer(g: Game, n: Negotiation) {
        val p = n.player
        val us = g.userTeam
        n.log += Line(true, "Sen", "Haftalık ${money(n.wage)}, ${n.years} yıl, imza parası ${money(n.bonus)}, rolü: ${roleNames[n.role]}.")
        if (!Economy.fitsWages(g, us, n.wage)) {
            n.log += Line(false, "Mali direktörün", "Bu maaş limitimizi aşıyor (${money(Economy.squadWages(g, us.id))} + ${money(n.wage)} > ${money(us.wageBudget)}). Önce satış yap ya da transfer bütçesinden maaşa kaydır.")
            return
        }
        val upfront = (if (n.installments) n.agreedFee / 2 else n.agreedFee) + n.bonus
        if (us.budget < upfront) {
            n.log += Line(false, "Mali direktörün", "Bonservis ve imza parası için bütçe yetmiyor.")
            return
        }
        val sc = agentScore(g, n)
        when {
            sc >= 1.0 || (n.counterWage != null && n.wage >= n.counterWage!! && sc >= 0.97) -> complete(g, n)
            sc >= 0.82 -> {
                n.agentPatience--
                val need = r500(n.wage + (1.02 - sc) * p.demandWage)
                n.counterWage = need
                n.log += Line(false, "Menajer", listOf("Biraz daha çıkmanız lazım. Haftalık ${money(need)} olursa imzalarız.", "Müvekkilim ${money(need)}/hafta altına imza atmaz.", "Yakınız. ${money(need)} haftalık ile anlaşma tamam.").random())
                if (n.agentPatience <= 0) fail(n, "Menajer", "Başka kulüplerle görüşeceğiz, iyi günler.")
            }
            else -> {
                n.agentPatience -= 2
                n.log += Line(false, "Menajer", listOf("Bu teklif müvekkilime hakaret.", "Bu şartlarla görüşmeye devam edemeyiz.", "Hocam bu rakamlar ligin altında.").random())
                if (n.agentPatience <= 0) fail(n, "Menajer", "Görüşmeleri bitiriyoruz.")
            }
        }
    }

    private fun fail(n: Negotiation, who: String, text: String) {
        n.stage = FAILED
        n.log += Line(false, who, text)
        GameRepo.blockTalks(n.player)
    }

    private fun complete(g: Game, n: Negotiation) {
        val p = n.player
        val us = g.userTeam
        val fromId = p.teamId
        val fromName = sellerName(g, p)
        if (n.loan && fromId >= 0) {
            g.team(fromId).lineup.replaceAll { if (it == p.id) -1 else it }
            g.movePlayer(p, us.id)
            p.loanFrom = fromId
            p.salary = n.wage
            p.squadRole = n.role
            p.morale = 80.0
            g.addNews("Kiralık transfer!", "${p.name} sezon sonuna kadar kiralık olarak ${us.name}'da. (${fromName})")
        } else {
            if (fromId >= 0) {
                val seller = g.team(fromId)
                Economy.earn(seller, Economy.SALES, n.agreedFee)
                seller.lineup.replaceAll { if (it == p.id) -1 else it }
                paySellOn(g, p, n.agreedFee, seller.id)
            }
            val now = if (n.installments) n.agreedFee / 2 else n.agreedFee
            Economy.earn(us, Economy.BUYS, -now)
            if (n.installments) g.pendingPayments += n.agreedFee - now
            if (n.bonus > 0) Economy.earn(us, Economy.BUYS, -n.bonus)
            g.movePlayer(p, us.id)
            p.salary = n.wage
            p.contractYears = n.years
            p.squadRole = n.role
            p.listed = false
            p.morale = 82.0
            if (n.sellOn > 0 && fromId >= 0) { p.sellOnClub = fromId; p.sellOnPct = n.sellOn } else { p.sellOnClub = -1; p.sellOnPct = 0 }
            Fans.bought(g, p)
            Career.signing(g, p.name, n.agreedFee)
            Dynamics.newSigning(g)
            g.addNews(
                "✍️ Transfer!",
                "${p.name} (${p.age}, ${p.pos.label}, ${p.overall}) ${if (fromId >= 0) "${money(n.agreedFee)} bonservisle $fromName'dan" else "serbest olarak"} ${us.name}'a imza attı. " +
                    "${n.years} yıllık sözleşme, haftalık ${money(n.wage)}" + (if (n.installments) " · taksitli ödeme" else "") + ".",
            )
        }
        Roles.ensureNumbers(g, us)
        n.stage = DONE
        n.log += Line(false, "Menajer", "Anlaştık! ${p.name} imzayı atıyor. 🎉 Forma numarası: ${p.number}")
        GameRepo.afterTransfer()
    }

    /** Sonraki satıştan pay: oyuncu satılınca eski kulübüne yüzde ödenir. */
    fun paySellOn(g: Game, p: Player, price: Long, sellerId: Int) {
        if (p.sellOnPct <= 0 || p.sellOnClub < 0 || p.sellOnClub == sellerId) return
        val club = g.teamOrNull(p.sellOnClub) ?: return
        val cut = price * p.sellOnPct / 100
        g.teamOrNull(sellerId)?.let { Economy.earn(it, Economy.SALES, -cut) }
        Economy.earn(club, Economy.SALES, cut)
        if (sellerId == g.userTeamId) g.addNews("Sonraki satıştan pay", "${p.name} satışından ${club.name}'a %${p.sellOnPct} pay (${money(cut)}) ödendi.")
        p.sellOnClub = -1
        p.sellOnPct = 0
    }

    @Suppress("unused")
    private val rnd = Random.Default
}
