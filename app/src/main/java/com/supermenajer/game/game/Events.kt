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
    const val PLAYTIME = 6
    const val RAISE = 7
    const val WANTS_OUT = 8

    /** Forma sözü: önümüzdeki [window] haftada en az [need] maç. */
    fun promise(g: Game, p: com.supermenajer.game.model.Player, need: Int = 3, window: Int = 6) {
        p.promiseUntil = g.week + window
        p.promiseNeed = need
        p.promiseBaseApps = p.apps
    }

    /** Süresi dolan sözleri değerlendirir: tutulduysa moral artar, tutulmadıysa oyuncu kırılır, takım da görür. */
    fun checkPromises(g: Game) {
        for (p in g.squad(g.userTeamId)) {
            if (p.promiseUntil <= 0 || g.week < p.promiseUntil) continue
            val played = p.apps - p.promiseBaseApps
            if (played >= p.promiseNeed) {
                p.morale = (p.morale + 10).coerceAtMost(100.0)
                g.addNews("🤝 Söz tutuldu", "${p.name} sözünü tuttuğun için mutlu: son haftalarda $played maç oynadı.")
            } else {
                p.morale = (p.morale - 25).coerceAtLeast(5.0)
                g.cohesion = (g.cohesion - 3).coerceAtLeast(15.0)
                g.addNews("💔 Söz tutulmadı", "${p.name} verilen forma sözünün tutulmadığını düşünüyor (${played}/${p.promiseNeed} maç). Soyunma odası da bunu konuşuyor.")
            }
            p.promiseUntil = 0
        }
    }

    private fun r25k(v: Double) = (v / 25_000).roundToLong().coerceAtLeast(1) * 25_000

    /** Lig haftasında, bekleyen olay yoksa ~%9 ihtimalle yeni olay. */
    fun weekly(g: Game, rnd: Random) {
        checkPromises(g)
        if (g.fired || g.pendingEvent != null || rnd.nextDouble() > 0.11) return
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
        // Oyuncu talepleri: forma, zam, ayrılık
        val str = Lineups.strength(g, us)
        if (g.week >= 8) sq.filter { it.overall >= str - 4 && it.age in 21..31 && it.apps <= g.week / 4 && it.promiseUntil == 0 && it.injuredWeeks == 0 && !it.reserve }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(PLAYTIME, p.id, "🗣️ ${p.name} forma istiyor", "${p.name} odana geldi: \"Hocam, bu sezon sadece ${p.apps} maç oynadım. Daha fazla şans istiyorum.\"", listOf("Söz ver: 6 maçta en az 3 kez oynayacak", "Formayı antrenmanda hak etmeli", "Kiralık ya da satış listesine koy")) }
        }
        sq.filter { it.apps >= 8 && it.avgRating >= 7.05 && it.overall >= str && it.salary in 1 until (it.demandWage * 0.8).toLong() }.randomOrNull(rnd)?.let { p ->
            val raise = (p.demandWage - p.salary).coerceAtLeast(1_000)
            options += { GameEvent(RAISE, p.id, "💰 ${p.name} zam istiyor", "Harika bir sezon geçiren ${p.name} (ort. not %.2f) maaşının piyasa değerinin altında olduğunu söylüyor. Menajeri haftalık ${money(raise)} zam talep ediyor.".format(p.avgRating), listOf("Zammı ver (+${money(raise)}/hafta, sözleşme +1 yıl)", "Sezon sonunda konuşalım", "Mevcut sözleşmesine uymalı"), raise) }
        }
        sq.filter { it.overall >= str + 2 && (Traits.of(it) == Traits.AMBITIOUS || it.morale < 50) && !it.listed }.randomOrNull(rnd)?.let { p ->
            val club = g.teams.filter { it.reputation > us.reputation + 6 }.randomOrNull(rnd)?.name ?: return@let
            options += { GameEvent(WANTS_OUT, p.id, "✈️ ${p.name} gitmek istiyor", "$club'ün ilgisini duyan ${p.name} kariyerinde bir üst basamağa çıkmak istediğini söyledi. Kararı sana bıraktı.", listOf("Satış listesine koy", "Maaşını %20 artır ve projeye ikna et", "Kesinlikle gitmiyor")) }
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
            PLAYTIME -> when (choice) {
                0 -> { p?.let { promise(g, it); it.morale = (it.morale + 8).coerceAtMost(100.0) }; "${p?.name} sözünü aldı: önümüzdeki 6 maçın en az 3'ünde oynatmalısın. Söz tutulmazsa kırılır!" }
                1 -> { val pro = p != null && Traits.of(p) == Traits.PRO; p?.let { it.morale = (it.morale + if (pro) 2 else -8).coerceIn(5.0, 100.0) }; if (pro) "${p?.name} başını eğip çalışmaya döndü." else "${p?.name} cevabından memnun kalmadı." }
                else -> { p?.let { it.listed = true; it.morale = (it.morale + 4).coerceAtMost(100.0) }; "${p?.name} satış/kiralık listesine kondu; teklifler gelebilir." }
            }
            RAISE -> when (choice) {
                0 -> {
                    val wageOk = Economy.squadWages(g, us.id) + e.amount <= us.wageBudget
                    if (p != null && wageOk) {
                        p.salary += e.amount; p.contractYears += 1; p.morale = (p.morale + 12).coerceAtMost(100.0)
                        "${p.name} yeni maaşıyla imzayı attı. Mutlu ve motive!"
                    } else "Maaş bütçesi bu zammı kaldırmıyor. Önce maaş bütçesinde yer aç (Kulüp → Finans)."
                }
                1 -> { p?.let { it.morale = (it.morale - 5).coerceAtLeast(5.0) }; "${p?.name} bekleyecek ama sabırsız." }
                else -> { p?.let { it.morale = (it.morale - 12).coerceAtLeast(5.0); if (Traits.of(it) == Traits.AMBITIOUS) it.listed = true }; "${p?.name} kırıldı. Hırslı oyuncular bu durumda kulüpten ayrılmak isteyebilir." }
            }
            WANTS_OUT -> when (choice) {
                0 -> { p?.let { it.listed = true; it.morale = (it.morale + 6).coerceAtMost(100.0) }; Fans.change(g, -3.0, "Yıldız satış listesinde"); "${p?.name} satış listesinde. Büyük teklifler gelebilir." }
                1 -> {
                    val extra = ((p?.wage ?: 0) * 0.2).toLong()
                    if (p != null && Economy.squadWages(g, us.id) + extra <= us.wageBudget) {
                        p.salary = p.wage + extra; p.morale = (p.morale + 10).coerceAtMost(100.0); p.contractYears = maxOf(p.contractYears, 3)
                        "${p.name} ikna oldu: yeni maaş ve 3 yıllık sözleşmeyle kalıyor."
                    } else "Maaş bütçesi yetmedi; ${p?.name} hâlâ gitmek istiyor."
                }
                else -> { p?.let { it.morale = (it.morale - 15).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion - 2).coerceAtLeast(15.0); Fans.change(g, 3.0, "Yıldız satılmadı"); "${p?.name} kalıyor ama mutsuz. Taraftar ise kararını alkışladı." }
            }
            else -> if (choice == 0 && p != null) {
                promise(g, p, need = 4, window = 6)
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
