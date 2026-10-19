package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.GameEvent
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * FC tarzı beklenmedik olaylar: disiplin sorunu, ev özlemi, yatırımcı teklifi, sürpriz emeklilik,
 * transfer söylentisi, parlayan genç. Her olayda menajer bir seçim yapar; seçimin etkileri vardır.
 */
object Events {
    const val DISCIPLINE = 0
    const val HOMESICK = 1
    const val INVESTOR = 2
    const val RETIRE = 3
    const val RUMOUR = 4
    const val YOUNGSTAR = 5

    private fun r25k(v: Double) = (v / 25_000).roundToLong().coerceAtLeast(1) * 25_000

    /** Lig haftasında, bekleyen olay yoksa ~%9 ihtimalle yeni olay. */
    fun weekly(g: Game, rnd: Random) {
        if (g.fired || g.pendingEvent != null || rnd.nextDouble() > 0.09) return
        val us = g.userTeam
        val sq = g.squad(us.id)
        val home = g.flagOf(us)
        val options = mutableListOf<() -> GameEvent?>()
        sq.filter { Traits.of(it) == Traits.HOTHEAD || Traits.of(it) == Traits.LAZY }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(DISCIPLINE, p.id, "⚠️ Disiplin sorunu", "${p.name} antrenmana geç kaldı ve yardımcı antrenörle tartıştı. Soyunma odası tepkini bekliyor.", listOf("Para cezası kes", "Özel olarak uyar", "Görmezden gel")) }
        }
        sq.filter { it.nation != home && it.morale < 65 && it.age <= 26 }.randomOrNull(rnd)?.let { p ->
            val cost = r25k((p.wage * 4).toDouble().coerceAtLeast(50_000.0))
            options += { GameEvent(HOMESICK, p.id, "🏠 Ev özlemi", "${p.name} yeni ülkesine alışmakta zorlanıyor ve ailesini özlüyor.", listOf("Ailesinin taşınmasına destek ol (${money(cost)})", "Kaptanla konuşmasını iste", "Profesyonelliğini hatırlat"), cost) }
        }
        if (!g.investorDone && rnd.nextDouble() < 0.25) {
            val amount = r25k(rnd.nextDouble(5_000_000.0, 20_000_000.0) * (com.supermenajer.game.model.Leagues.of(us.division)?.wealth ?: 1.0))
            options += { GameEvent(INVESTOR, -1, "💼 Yatırımcı teklifi", "Bir yatırım grubu kulübe ${money(amount)} getirerek ortak olmak istiyor. Taraftarlar kulübün satılmasından endişeli.", listOf("Kabul et", "Reddet"), amount) }
        }
        sq.filter { it.age >= 33 && it.contractYears >= 1 }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(RETIRE, p.id, "👋 Sürpriz emeklilik", "${p.name} (${p.age}) sezon sonunda futbolu bırakacağını açıkladı.", listOf("Kararına saygı duyuyoruz", "Bir sezon daha oynaması için ikna et")) }
        }
        sq.filter { it.overall >= Lineups.strength(g, us) + 2 }.randomOrNull(rnd)?.let { p ->
            val club = g.teams.filter { it.reputation > us.reputation + 5 }.randomOrNull(rnd)?.name ?: "büyük bir kulüp"
            options += { GameEvent(RUMOUR, p.id, "📰 Transfer söylentisi", "Basın, $club'ün ${p.name} ile ilgilendiğini yazıyor. Ne açıklama yapacaksın?", listOf("\"Satılık değil!\"", "\"Yorum yapmayacağım.\"", "\"Doğru teklif gelirse konuşuruz.\"")) }
        }
        sq.filter { it.age <= 20 && it.id !in us.lineup && it.potential >= it.overall + 8 }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(YOUNGSTAR, p.id, "🌟 Genç yetenek parlıyor", "${p.name} (${p.age}) antrenmanlarda herkesi büyülüyor. Ona şans verecek misin?", listOf("İlk 11 sözü ver", "Sabırlı ol, zamanı gelecek")) }
        }
        g.pendingEvent = options.randomOrNull(rnd)?.invoke() ?: return
        g.addNews(g.pendingEvent!!.title, g.pendingEvent!!.text + " (Ana sayfadan karar ver.)")
    }

    /** Seçimi uygular ve sonucu döner. */
    fun resolve(g: Game, choice: Int, rnd: Random = Random.Default): String {
        val e = g.pendingEvent ?: return ""
        g.pendingEvent = null
        val p = g.player(e.playerId)
        val us = g.userTeam
        // Karar verilmeden oyuncu ayrıldıysa (satış, kiralık, fesih) olay düşer
        if (e.kind != INVESTOR && (p == null || p.teamId != us.id)) {
            return "Oyuncu artık kulüpte olmadığı için konu kapandı."
        }
        val msg = when (e.kind) {
            DISCIPLINE -> when (choice) {
                0 -> { p?.let { it.morale = (it.morale - 10).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion + 3).coerceAtMost(100.0); "Ceza kesildi. ${p?.name} kırgın ama takım disiplinin önemini anladı." }
                1 -> { p?.let { it.morale = (it.morale - 3).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion + 1).coerceAtMost(100.0); "${p?.name} hatasını kabul etti." }
                else -> { g.cohesion = (g.cohesion - 4).coerceAtLeast(15.0); "Diğer oyuncular bunun cezasız kalmasından rahatsız. Takım uyumu düştü." }
            }
            HOMESICK -> when (choice) {
                0 -> if (us.budget >= e.amount) { Economy.earn(us, "Oyuncu destekleri", -e.amount); p?.let { it.morale = (it.morale + 14).coerceAtMost(100.0) }; "Ailesi yanına taşındı, ${p?.name}'in yüzü gülüyor!" } else "Bütçe yetmedi."
                1 -> { val cap = Roles.captain(g, us); val good = cap != null && Traits.isLeader(cap); p?.let { it.morale = (it.morale + if (good) 8 else 3).coerceAtMost(100.0) }; if (good) "Kaptan ${cap!!.name} ona kol kanat gerdi." else "Kaptanla konuşması biraz iyi geldi." }
                else -> { val pro = p != null && Traits.of(p) == Traits.PRO; p?.let { it.morale = (it.morale + if (pro) 3 else -5).coerceIn(5.0, 100.0) }; if (pro) "${p?.name} mesajı aldı ve işine odaklandı." else "${p?.name} anlaşılmadığını düşünüyor." }
            }
            INVESTOR -> {
                g.investorDone = true
                if (choice == 0) {
                    Economy.earn(us, Economy.BOARD, e.amount)
                    Fans.change(g, -5.0, "Kulüp satışı tepkisi")
                    g.confidence = (g.confidence + 5).coerceAtMost(100.0)
                    "Yatırım geldi: transfer bütçesi +${money(e.amount)}. Taraftarlar biraz tedirgin."
                } else {
                    Fans.change(g, 4.0, "Kulübün bağımsızlığı korundu")
                    "Kulübün bağımsızlığı korundu, taraftarlar seni alkışlıyor."
                }
            }
            RETIRE -> if (choice == 1 && p != null && rnd.nextDouble() < 0.5) {
                p.contractYears = maxOf(p.contractYears, 2)
                p.morale = (p.morale + 5).coerceAtMost(100.0)
                "${p.name} ikna oldu, bir sezon daha oynayacak!"
            } else {
                p?.let { if (it.id !in g.retiring) g.retiring += it.id }
                "${p?.name} sezon sonunda futbolu bırakacak. Onu iyi uğurlayalım."
            }
            RUMOUR -> when (choice) {
                0 -> { Fans.change(g, 3.0, "\"Satılık değil\" açıklaması"); p?.let { it.morale = (it.morale + if (Traits.of(it) == Traits.AMBITIOUS) -6 else 4).coerceIn(5.0, 100.0) }; "Taraftar mesajını sevdi." }
                1 -> "Söylentiler bir süre sonra durulur."
                else -> { Fans.change(g, -3.0, "Yıldız satış sinyali"); p?.let { it.listed = true; it.morale = (it.morale + if (Traits.of(it) == Traits.AMBITIOUS) 5 else -4).coerceIn(5.0, 100.0) }; "${p?.name} satış listesine kondu; teklifler gelebilir." }
            }
            else -> if (choice == 0 && p != null) {
                p.squadRole = 1
                p.morale = (p.morale + 10).coerceAtMost(100.0)
                if (rnd.nextDouble() < 0.5) p.potential = (p.potential + 2).coerceAtMost(95)
                "${p.name}'e ilk 11 sözü verildi. Sözünü tutmazsan kırılabilir!"
            } else {
                p?.let { it.morale = (it.morale - 3).coerceAtLeast(5.0) }
                "${p?.name} biraz hayal kırıklığına uğradı ama çalışmaya devam ediyor."
            }
        }
        g.addNews(e.title + " · karar", msg)
        return msg
    }
}
