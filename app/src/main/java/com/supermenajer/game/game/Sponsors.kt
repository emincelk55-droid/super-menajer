package com.supermenajer.game.game

import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Sponsor
import com.supermenajer.game.model.Team
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sponsorluk anlaşmaları: forma, stadyum isim hakkı ve malzeme sponsoru.
 * Her boş alan için sezon başında üç teklif gelir: garantili (yüksek sabit), dengeli ve performans
 * (düşük sabit, yüksek galibiyet ve kupa primi). Teklifin değeri kulübün itibarına, liginin
 * zenginliğine, son başarılarına ve (stadyum için) kapasitesine göre değişir.
 */
object Sponsors {
    const val SHIRT = 0
    const val STADIUM = 1
    const val KIT = 2
    val slotNames = listOf("Forma sponsoru", "Stadyum isim hakkı", "Malzeme sponsoru")
    val slotIcons = listOf("👕", "🏟️", "👟")
    val styleNames = listOf("Garantili", "Dengeli", "Performans")
    val styleInfo = listOf(
        "Yüksek sabit ödeme, prim yok. Uzun ve güvenli.",
        "Orta sabit ödeme, galibiyet ve kupa primi.",
        "Düşük sabit ödeme, yüksek galibiyet ve kupa primi. Kazanırsan çok kazanırsın!",
    )
    private val slotBase = listOf(2_500_000.0, 1_500_000.0, 1_000_000.0)

    private val prefixes = listOf("Anadolu", "Boğaziçi", "Yıldız", "Kartal", "Mavi", "Atlas", "Zirve", "Ege", "Toros", "Nova", "Delta", "Vega", "Marmara", "Pusula", "Karadeniz", "Orion", "Lider", "Akdeniz")
    private val sectors = listOf("Bank", "Holding", "Enerji", "Telekom", "Havayolları", "Sigorta", "Gıda", "Otomotiv", "Teknoloji", "Lojistik", "Yapı", "İletişim")
    private val kitBrands = listOf("Stryde", "Kinetik", "Vortex", "Apex Sport", "Falco", "Nordik", "Kosu", "Atletika", "Pivot", "Sprinta")

    /** Kulübün ticari değeri (itibar, lig, başarı). */
    fun clubValue(g: Game, t: Team): Double {
        val lg = Leagues.of(t.division)
        val tierF = if (lg?.tier == 2) 0.45 else 1.0
        val wealth = sqrt(lg?.wealth ?: 1.0)
        var success = 1.0
        if (t.id in g.euroQualified || t.id in g.euroTeams) success += 0.2
        if (g.history.lastOrNull()?.champion == t.name) success += 0.15
        return (t.reputation / 70.0).pow(2) * wealth * tierF * success
    }

    private fun r25k(v: Double) = (v / 25_000).roundToLong() * 25_000
    private fun r5k(v: Double) = (v / 5_000).roundToLong().coerceAtLeast(1) * 5_000

    /** Sezonluk beklenen değer (alan bazında). */
    fun slotValue(g: Game, t: Team, slot: Int): Double {
        val stadF = if (slot == STADIUM) sqrt((t.stadium.coerceAtLeast(5_000) / 35_000.0)).coerceIn(0.5, 1.6) else 1.0
        return slotBase[slot] * clubValue(g, t) * stadF * Fans.sponsorFactor(g, t)
    }

    fun current(g: Game, slot: Int) = g.sponsors.firstOrNull { it.slot == slot }
    fun offers(g: Game, slot: Int) = g.sponsorOffers.filter { it.slot == slot }

    private fun makeOffer(g: Game, t: Team, slot: Int, style: Int, name: String, rnd: Random): Sponsor {
        val v = slotValue(g, t, slot) * rnd.nextDouble(0.9, 1.12)
        // Galibiyet primi: sezonda ~15 galibiyete göre ölçeklenir
        return when (style) {
            0 -> Sponsor(slot, name, 0, r25k(v), 0, 0, 3)
            1 -> Sponsor(slot, name, 1, r25k(v * 0.7), r5k(v * 0.35 / 15), r25k(v * 0.4), 2)
            else -> Sponsor(slot, name, 2, r25k(v * 0.4), r5k(v * 0.8 / 15), r25k(v * 1.0), 1)
        }
    }

    private fun name(slot: Int, rnd: Random, used: Set<String>): String {
        repeat(20) {
            val n = if (slot == KIT) kitBrands.random(rnd) else "${prefixes.random(rnd)} ${sectors.random(rnd)}"
            if (n !in used) return n
        }
        return "${prefixes.random(rnd)} ${sectors.random(rnd)}"
    }

    /** Boş alanlar için yeni teklifler oluşturur (eski teklifler silinir). */
    fun refreshOffers(g: Game, rnd: Random = Random.Default) {
        g.sponsorOffers.clear()
        if (g.fired || g.userTeamId < 0) return
        val us = g.userTeam
        Facilities.ensure(us)
        val used = g.sponsors.map { it.name }.toMutableSet()
        var any = false
        for (slot in 0..2) {
            if (current(g, slot) != null) continue
            for (style in 0..2) {
                val n = name(slot, rnd, used)
                used += n
                g.sponsorOffers += makeOffer(g, us, slot, style, n, rnd)
            }
            any = true
        }
        if (any) g.addNews("💼 Sponsor teklifleri", "Boş sponsorluk alanların için yeni teklifler geldi. Kulüp → Sponsorlar'dan değerlendirebilirsin.")
    }

    fun defaultStadiumName(t: Team) = t.name.substringBefore(" ") + " Arena"

    /** Teklifi imzalar. Hata mesajı döner. */
    fun sign(g: Game, offer: Sponsor): String? {
        if (offer !in g.sponsorOffers) return "Bu teklif artık geçerli değil."
        if (current(g, offer.slot) != null) return "Bu alanda zaten bir sponsorun var."
        g.sponsors += offer
        g.sponsorOffers.removeAll { it.slot == offer.slot }
        val us = g.userTeam
        if (offer.slot == STADIUM) us.stadiumName = "${offer.name} Arena"
        if (g.sponsors.size >= 3) Career.unlock(g, "sponsors")
        g.addNews(
            "🤝 Sponsorluk anlaşması",
            "${offer.name} ile ${offer.yearsLeft} sezonluk ${slotNames[offer.slot].lowercase()} anlaşması imzalandı. Sezonluk ${money(offer.base)}" +
                (if (offer.winBonus > 0) ", galibiyet başına ${money(offer.winBonus)}" else "") +
                (if (offer.titleBonus > 0) ", kupa başına ${money(offer.titleBonus)}" else "") + ".",
        )
        return null
    }

    fun seasonBase(g: Game) = g.sponsors.sumOf { it.base }

    /** Lig haftası: sabit ödemenin haftalık payı. */
    fun weekly(g: Game) {
        if (g.fired || g.sponsors.isEmpty()) return
        val weeks = g.calendar.count { it.type == 0 }.coerceAtLeast(1)
        Economy.earn(g.userTeam, Economy.SPONSOR, (g.sponsors.sumOf { it.base / weeks } * Perks.income(g)).toLong())
    }

    /** Lig galibiyeti primi. */
    fun matchResult(g: Game, f: Fixture) {
        if (g.fired || !f.played || !Comp.isLeague(f.comp)) return
        val us = g.userTeamId
        val won = (f.home == us && f.hg > f.ag) || (f.away == us && f.ag > f.hg)
        if (!won) return
        val bonus = g.sponsors.sumOf { it.winBonus }
        if (bonus > 0) Economy.earn(g.userTeam, Economy.SPONSOR, bonus)
    }

    /** Sezon sonu: kupa primleri (lig, ülke kupası, Avrupa). */
    fun seasonTitles(g: Game, titles: Int) {
        if (g.fired || titles <= 0) return
        val bonus = g.sponsors.sumOf { it.titleBonus } * titles
        if (bonus <= 0) return
        Economy.earn(g.userTeam, Economy.SPONSOR, bonus)
        g.addNews("🏆 Sponsor primi", "Sponsorlarımız $titles kupa için toplam ${money(bonus)} prim ödedi!")
    }

    /** Yeni sezon: sözleşme süreleri azalır, bitenler ayrılır. */
    fun newSeason(g: Game) {
        val ended = g.sponsors.filter { --it.yearsLeft <= 0 }
        if (ended.isEmpty()) return
        g.sponsors.removeAll(ended.toSet())
        if (ended.any { it.slot == STADIUM }) g.userTeam.stadiumName = defaultStadiumName(g.userTeam)
        g.addNews("Sponsorluk sona erdi", "${ended.joinToString { it.name }} ile sözleşme bitti. Yeni teklifler sezon başında geliyor.")
    }

    /** Takım değişince sözleşmeler eski kulüpte kalır. */
    fun leaveClub(g: Game, old: Team) {
        if (current(g, STADIUM) != null) old.stadiumName = defaultStadiumName(old)
        g.sponsors.clear()
        g.sponsorOffers.clear()
    }

    /** Yapay zekâ kulüplerinin tahmini sponsor geliri (adil rekabet için). */
    fun aiIncome(g: Game, t: Team): Long = r25k((0..2).sumOf { slotValue(g, t, it) } * 0.85)
}
