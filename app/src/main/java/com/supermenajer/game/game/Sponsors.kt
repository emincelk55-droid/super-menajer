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
    private val slotNamesCache = LangCached { listOf(L("Forma sponsoru", "Shirt sponsor"), L("Stadyum isim hakkı", "Stadium naming rights"), L("Malzeme sponsoru", "Kit supplier")) }
    val slotNames get() = slotNamesCache.get()
    val slotIcons = listOf("👕", "🏟️", "👟")
    private val styleNamesCache = LangCached { listOf(L("Garantili", "Guaranteed"), L("Dengeli", "Balanced"), L("Performans", "Performance")) }
    val styleNames get() = styleNamesCache.get()
    private val styleInfoCache = LangCached { listOf(
        L("Yüksek sabit ödeme, prim yok. Uzun ve güvenli.", "High fixed payment, no bonuses. Long and safe."),
        L("Orta sabit ödeme, galibiyet ve kupa primi.", "Medium fixed payment, win and cup bonuses."),
        L("Düşük sabit ödeme, yüksek galibiyet ve kupa primi. Kazanırsan çok kazanırsın!", "Low fixed payment, high win and cup bonuses. Win and you earn big!"),
    ) }
    val styleInfo get() = styleInfoCache.get()
    private val slotBase = listOf(2_500_000.0, 1_500_000.0, 1_000_000.0)

    /** Gerçek markalara benzer (birebir aynı olmayan) sponsor isimleri. */
    private val brands = listOf(
        "Fly Emirats", "Turkish Airlinez", "Etihat Airways", "Qatar Airwais", "Pegasüs Air", "Spotifai", "Rakutan", "Vodafon",
        "Turkcel", "Türk Telekon", "Garanti BBVE", "Ziraat Bankasu", "Akbenk", "Yapı Kredy", "Allienz", "Siemenz", "Volkswegen",
        "Audy", "Samsunk", "Bekko", "Trendyolu", "Getirr", "Papera", "Sokar Enerji", "Coinbaze", "Standart Chartered", "Unibet Bank",
        "Hyundei", "Kia Motorz", "Toyuta", "Heinekan", "Mastercart", "Visaa", "Aramku", "Teleko Sigorta",
    )
    private val kitBrands = listOf("Adidaz", "Naik", "Pumma", "Under Armur", "Kappe", "Makron", "Humel", "New Balanse", "Jomma", "Loto", "Umbru", "Kastore")

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
            val n = if (slot == KIT) kitBrands.random(rnd) else brands.random(rnd)
            if (n !in used) return n
        }
        return brands.random(rnd)
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
        if (any) g.addNews(L("💼 Sponsor teklifleri", "💼 Sponsor offers"), L("Boş sponsorluk alanların için yeni teklifler geldi. Kulüp → Sponsorlar'dan değerlendirebilirsin.", "New offers have arrived for your empty sponsorship slots. You can review them in Club → Sponsors."))
    }

    fun defaultStadiumName(t: Team) = t.name.substringBefore(" ") + L(" Arena", " Arena")

    /** Teklifi imzalar. Hata mesajı döner. */
    fun sign(g: Game, offer: Sponsor): String? {
        if (offer !in g.sponsorOffers) return L("Bu teklif artık geçerli değil.", "This offer is no longer valid.")
        if (current(g, offer.slot) != null) return L("Bu alanda zaten bir sponsorun var.", "You already have a sponsor in this slot.")
        g.sponsors += offer
        g.sponsorOffers.removeAll { it.slot == offer.slot }
        val us = g.userTeam
        if (offer.slot == STADIUM) us.stadiumName = L("${offer.name} Arena", "${offer.name} Arena")
        if (g.sponsors.size >= 3) Career.unlock(g, "sponsors")
        g.addNews(
            L("🤝 Sponsorluk anlaşması", "🤝 Sponsorship deal"),
            L("${offer.name} ile ${offer.yearsLeft} sezonluk ${slotNames[offer.slot].lowercase()} anlaşması imzalandı. Sezonluk ${money(offer.base)}", "A ${offer.yearsLeft}-season ${slotNames[offer.slot].lowercase()} deal has been signed with ${offer.name}. ${money(offer.base)} per season") +
                (if (offer.winBonus > 0) L(", galibiyet başına ${money(offer.winBonus)}", ", ${money(offer.winBonus)} per win") else "") +
                (if (offer.titleBonus > 0) L(", kupa başına ${money(offer.titleBonus)}", ", ${money(offer.titleBonus)} per trophy") else "") + ".",
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
        g.addNews(L("🏆 Sponsor primi", "🏆 Sponsor bonus"), L("Sponsorlarımız $titles kupa için toplam ${money(bonus)} prim ödedi!", "Our sponsors paid a total bonus of ${money(bonus)} for $titles trophies!"))
    }

    /** Yeni sezon: sözleşme süreleri azalır, bitenler ayrılır. */
    fun newSeason(g: Game) {
        val ended = g.sponsors.filter { --it.yearsLeft <= 0 }
        if (ended.isEmpty()) return
        g.sponsors.removeAll(ended.toSet())
        if (ended.any { it.slot == STADIUM }) g.userTeam.stadiumName = defaultStadiumName(g.userTeam)
        g.addNews(L("Sponsorluk sona erdi", "Sponsorship ended"), L("${ended.joinToString { it.name }} ile sözleşme bitti. Yeni teklifler sezon başında geliyor.", "The contract with ${ended.joinToString { it.name }} has ended. New offers arrive at the start of the season."))
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
