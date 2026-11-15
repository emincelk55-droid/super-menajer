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
                g.addNews(L("🤝 Söz tutuldu", "🤝 Promise kept"), L("${p.name} sözünü tuttuğun için mutlu: son haftalarda $played maç oynadı.", "${p.name} is happy you kept your word: he played $played matches in recent weeks."))
            } else {
                p.morale = (p.morale - 25).coerceAtLeast(5.0)
                g.cohesion = (g.cohesion - 3).coerceAtLeast(15.0)
                g.addNews(L("💔 Söz tutulmadı", "💔 Promise broken"), L("${p.name} verilen forma sözünün tutulmadığını düşünüyor (${played}/${p.promiseNeed} maç). Soyunma odası da bunu konuşuyor.", "${p.name} feels the playing-time promise wasn't kept (${played}/${p.promiseNeed} matches). The dressing room is talking about it too."))
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
            options += { GameEvent(DISCIPLINE, p.id, L("⚠️ Disiplin sorunu", "⚠️ Discipline problem"), L("${p.name} antrenmana geç kaldı ve yardımcı antrenörle tartıştı. Soyunma odası tepkini bekliyor.", "${p.name} was late for training and argued with the assistant manager. The dressing room awaits your reaction."), listOf(L("Para cezası kes", "Fine him"), L("Özel olarak uyar", "Warn him privately"), L("Görmezden gel", "Ignore it"))) }
        }
        sq.filter { it.nation != home && it.morale < 65 && it.age <= 26 }.randomOrNull(rnd)?.let { p ->
            val cost = r25k((p.wage * 4).toDouble().coerceAtLeast(50_000.0))
            options += { GameEvent(HOMESICK, p.id, L("🏠 Ev özlemi", "🏠 Homesick"), L("${p.name} yeni ülkesine alışmakta zorlanıyor ve ailesini özlüyor.", "${p.name} is struggling to settle in his new country and misses his family."), listOf(L("Ailesinin taşınmasına destek ol (${money(cost)})", "Help his family move here (${money(cost)})"), L("Kaptanla konuşmasını iste", "Ask the captain to talk to him"), L("Profesyonelliğini hatırlat", "Remind him to be professional")), cost) }
        }
        if (!g.investorDone && rnd.nextDouble() < 0.25) {
            val amount = r25k(rnd.nextDouble(5_000_000.0, 20_000_000.0) * (com.supermenajer.game.model.Leagues.of(us.division)?.wealth ?: 1.0))
            options += { GameEvent(INVESTOR, -1, L("💼 Yatırımcı teklifi", "💼 Investor offer"), L("Bir yatırım grubu kulübe ${money(amount)} getirerek ortak olmak istiyor. Taraftarlar kulübün satılmasından endişeli.", "An investment group wants to bring ${money(amount)} into the club as a partner. The fans are worried about the club being sold."), listOf(L("Kabul et", "Accept"), L("Reddet", "Reject")), amount) }
        }
        sq.filter { it.age >= 33 && it.contractYears >= 1 }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(RETIRE, p.id, L("👋 Sürpriz emeklilik", "👋 Surprise retirement"), L("${p.name} (${p.age}) sezon sonunda futbolu bırakacağını açıkladı.", "${p.name} (${p.age}) has announced he'll retire at the end of the season."), listOf(L("Kararına saygı duyuyoruz", "We respect his decision"), L("Bir sezon daha oynaması için ikna et", "Persuade him to play one more season"))) }
        }
        sq.filter { it.overall >= Lineups.strength(g, us) + 2 }.randomOrNull(rnd)?.let { p ->
            val club = g.teams.filter { it.reputation > us.reputation + 5 }.randomOrNull(rnd)?.name ?: L("büyük bir kulüp", "a big club")
            options += { GameEvent(RUMOUR, p.id, L("📰 Transfer söylentisi", "📰 Transfer rumour"), L("Basın, $club'ün ${p.name} ile ilgilendiğini yazıyor. Ne açıklama yapacaksın?", "The press report that $club are interested in ${p.name}. What will you say?"), listOf(L("\"Satılık değil!\"", "\"He's not for sale!\""), L("\"Yorum yapmayacağım.\"", "\"No comment.\""), L("\"Doğru teklif gelirse konuşuruz.\"", "\"If the right offer comes, we'll talk.\""))) }
        }
        sq.filter { it.age <= 20 && it.id !in us.lineup && it.potential >= it.overall + 8 }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(YOUNGSTAR, p.id, L("🌟 Genç yetenek parlıyor", "🌟 Young talent shining"), L("${p.name} (${p.age}) antrenmanlarda herkesi büyülüyor. Ona şans verecek misin?", "${p.name} (${p.age}) is dazzling everyone in training. Will you give him a chance?"), listOf(L("İlk 11 sözü ver", "Promise him a starting place"), L("Sabırlı ol, zamanı gelecek", "Be patient, his time will come"))) }
        }
        // Oyuncu talepleri: forma, zam, ayrılık
        val str = Lineups.strength(g, us)
        if (g.week >= 8) sq.filter { it.overall >= str - 4 && it.age in 21..31 && it.apps <= g.week / 4 && it.promiseUntil == 0 && it.injuredWeeks == 0 && !it.reserve }.randomOrNull(rnd)?.let { p ->
            options += { GameEvent(PLAYTIME, p.id, L("🗣️ ${p.name} forma istiyor", "🗣️ ${p.name} wants to play"), L("${p.name} odana geldi: \"Hocam, bu sezon sadece ${p.apps} maç oynadım. Daha fazla şans istiyorum.\"", "${p.name} came to your office: \"Boss, I've only played ${p.apps} matches this season. I want more chances.\""), listOf(L("Söz ver: 6 maçta en az 3 kez oynayacak", "Promise: he'll play at least 3 of the next 6 matches"), L("Formayı antrenmanda hak etmeli", "He has to earn it in training"), L("Kiralık ya da satış listesine koy", "Put him on the loan or transfer list"))) }
        }
        sq.filter { it.apps >= 8 && it.avgRating >= 7.05 && it.overall >= str && it.salary in 1 until (it.demandWage * 0.8).toLong() }.randomOrNull(rnd)?.let { p ->
            val raise = (p.demandWage - p.salary).coerceAtLeast(1_000)
            options += { GameEvent(RAISE, p.id, L("💰 ${p.name} zam istiyor", "💰 ${p.name} wants a raise"), L("Harika bir sezon geçiren ${p.name} (ort. not %.2f) maaşının piyasa değerinin altında olduğunu söylüyor. Menajeri haftalık ${money(raise)} zam talep ediyor.", "After a great season ${p.name} (avg. rating %.2f) says his wage is below his market value. His agent is asking for a ${money(raise)} weekly raise.").format(p.avgRating), listOf(L("Zammı ver (+${money(raise)}/hafta, sözleşme +1 yıl)", "Give the raise (+${money(raise)}/week, contract +1 year)"), L("Sezon sonunda konuşalım", "Let's talk at the end of the season"), L("Mevcut sözleşmesine uymalı", "He must honour his current contract")), raise) }
        }
        sq.filter { it.overall >= str + 2 && (Traits.of(it) == Traits.AMBITIOUS || it.morale < 50) && !it.listed }.randomOrNull(rnd)?.let { p ->
            val club = g.teams.filter { it.reputation > us.reputation + 6 }.randomOrNull(rnd)?.name ?: return@let
            options += { GameEvent(WANTS_OUT, p.id, L("✈️ ${p.name} gitmek istiyor", "✈️ ${p.name} wants to leave"), L("$club'ün ilgisini duyan ${p.name} kariyerinde bir üst basamağa çıkmak istediğini söyledi. Kararı sana bıraktı.", "Having heard about $club's interest, ${p.name} said he wants to take the next step in his career. He left the decision to you."), listOf(L("Satış listesine koy", "Put him on the transfer list"), L("Maaşını %20 artır ve projeye ikna et", "Raise his wage by 20% and convince him of the project"), L("Kesinlikle gitmiyor", "He's definitely not leaving"))) }
        }
        g.pendingEvent = options.randomOrNull(rnd)?.invoke() ?: return
        g.addNews(g.pendingEvent!!.title, g.pendingEvent!!.text + L(" (Ana sayfadan karar ver.)", " (Decide on the home screen.)"))
    }

    /** Seçimi uygular ve sonucu döner. */
    fun resolve(g: Game, choice: Int, rnd: Random = Random.Default): String {
        val e = g.pendingEvent ?: return ""
        g.pendingEvent = null
        val p = g.player(e.playerId)
        val us = g.userTeam
        // Karar verilmeden oyuncu ayrıldıysa (satış, kiralık, fesih) olay düşer
        if (e.kind != INVESTOR && (p == null || p.teamId != us.id)) {
            return L("Oyuncu artık kulüpte olmadığı için konu kapandı.", "The player is no longer at the club, so the matter is closed.")
        }
        val msg = when (e.kind) {
            DISCIPLINE -> when (choice) {
                0 -> { p?.let { it.morale = (it.morale - 10).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion + 3).coerceAtMost(100.0); L("Ceza kesildi. ${p?.name} kırgın ama takım disiplinin önemini anladı.", "He was fined. ${p?.name} is hurt but the team understood the importance of discipline.") }
                1 -> { p?.let { it.morale = (it.morale - 3).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion + 1).coerceAtMost(100.0); L("${p?.name} hatasını kabul etti.", "${p?.name} admitted his mistake.") }
                else -> { g.cohesion = (g.cohesion - 4).coerceAtLeast(15.0); L("Diğer oyuncular bunun cezasız kalmasından rahatsız. Takım uyumu düştü.", "The other players are unhappy that it went unpunished. Team chemistry dropped.") }
            }
            HOMESICK -> when (choice) {
                0 -> if (us.budget >= e.amount) { Economy.earn(us, L("Oyuncu destekleri", "Player support"), -e.amount); p?.let { it.morale = (it.morale + 14).coerceAtMost(100.0) }; L("Ailesi yanına taşındı, ${p?.name}'in yüzü gülüyor!", "His family moved in with him, ${p?.name} is all smiles!") } else L("Bütçe yetmedi.", "Not enough budget.")
                1 -> { val cap = Roles.captain(g, us); val good = cap != null && Traits.isLeader(cap); p?.let { it.morale = (it.morale + if (good) 8 else 3).coerceAtMost(100.0) }; if (good) L("Kaptan ${cap!!.name} ona kol kanat gerdi.", "Captain ${cap!!.name} took him under his wing.") else L("Kaptanla konuşması biraz iyi geldi.", "Talking to the captain helped a little.") }
                else -> { val pro = p != null && Traits.of(p) == Traits.PRO; p?.let { it.morale = (it.morale + if (pro) 3 else -5).coerceIn(5.0, 100.0) }; if (pro) L("${p?.name} mesajı aldı ve işine odaklandı.", "${p?.name} got the message and focused on his job.") else L("${p?.name} anlaşılmadığını düşünüyor.", "${p?.name} feels misunderstood.") }
            }
            INVESTOR -> {
                g.investorDone = true
                if (choice == 0) {
                    Economy.earn(us, Economy.BOARD, e.amount)
                    Fans.change(g, -5.0, L("Kulüp satışı tepkisi", "Backlash over club sale"))
                    g.confidence = (g.confidence + 5).coerceAtMost(100.0)
                    L("Yatırım geldi: transfer bütçesi +${money(e.amount)}. Taraftarlar biraz tedirgin.", "Investment arrived: transfer budget +${money(e.amount)}. The fans are a little uneasy.")
                } else {
                    Fans.change(g, 4.0, L("Kulübün bağımsızlığı korundu", "Club independence preserved"))
                    L("Kulübün bağımsızlığı korundu, taraftarlar seni alkışlıyor.", "The club's independence is preserved and the fans are applauding you.")
                }
            }
            RETIRE -> if (choice == 1 && p != null && rnd.nextDouble() < 0.5) {
                p.contractYears = maxOf(p.contractYears, 2)
                p.morale = (p.morale + 5).coerceAtMost(100.0)
                L("${p.name} ikna oldu, bir sezon daha oynayacak!", "${p.name} was persuaded and will play one more season!")
            } else {
                p?.let { if (it.id !in g.retiring) g.retiring += it.id }
                L("${p?.name} sezon sonunda futbolu bırakacak. Onu iyi uğurlayalım.", "${p?.name} will retire at the end of the season. Let's give him a proper send-off.")
            }
            RUMOUR -> when (choice) {
                0 -> { Fans.change(g, 3.0, L("\"Satılık değil\" açıklaması", "\"Not for sale\" statement")); p?.let { it.morale = (it.morale + if (Traits.of(it) == Traits.AMBITIOUS) -6 else 4).coerceIn(5.0, 100.0) }; L("Taraftar mesajını sevdi.", "The fans loved the message.") }
                1 -> L("Söylentiler bir süre sonra durulur.", "The rumours will die down after a while.")
                else -> { Fans.change(g, -3.0, L("Yıldız satış sinyali", "Star sale signal")); p?.let { it.listed = true; it.morale = (it.morale + if (Traits.of(it) == Traits.AMBITIOUS) 5 else -4).coerceIn(5.0, 100.0) }; L("${p?.name} satış listesine kondu; teklifler gelebilir.", "${p?.name} has been put on the transfer list; offers may come in.") }
            }
            PLAYTIME -> when (choice) {
                0 -> { p?.let { promise(g, it); it.morale = (it.morale + 8).coerceAtMost(100.0) }; L("${p?.name} sözünü aldı: önümüzdeki 6 maçın en az 3'ünde oynatmalısın. Söz tutulmazsa kırılır!", "${p?.name} got your promise: you must play him in at least 3 of the next 6 matches. If you break it he'll be hurt!") }
                1 -> { val pro = p != null && Traits.of(p) == Traits.PRO; p?.let { it.morale = (it.morale + if (pro) 2 else -8).coerceIn(5.0, 100.0) }; if (pro) L("${p?.name} başını eğip çalışmaya döndü.", "${p?.name} put his head down and went back to work.") else L("${p?.name} cevabından memnun kalmadı.", "${p?.name} wasn't happy with your answer.") }
                else -> { p?.let { it.listed = true; it.morale = (it.morale + 4).coerceAtMost(100.0) }; L("${p?.name} satış/kiralık listesine kondu; teklifler gelebilir.", "${p?.name} has been put on the transfer/loan list; offers may come in.") }
            }
            RAISE -> when (choice) {
                0 -> {
                    val wageOk = Economy.squadWages(g, us.id) + e.amount <= us.wageBudget
                    if (p != null && wageOk) {
                        p.salary += e.amount; p.contractYears += 1; p.morale = (p.morale + 12).coerceAtMost(100.0)
                        L("${p.name} yeni maaşıyla imzayı attı. Mutlu ve motive!", "${p.name} signed with his new wage. Happy and motivated!")
                    } else L("Maaş bütçesi bu zammı kaldırmıyor. Önce maaş bütçesinde yer aç (Kulüp → Finans).", "The wage budget can't cover this raise. Make room in the wage budget first (Club → Finances).")
                }
                1 -> { p?.let { it.morale = (it.morale - 5).coerceAtLeast(5.0) }; L("${p?.name} bekleyecek ama sabırsız.", "${p?.name} will wait, but he's impatient.") }
                else -> { p?.let { it.morale = (it.morale - 12).coerceAtLeast(5.0); if (Traits.of(it) == Traits.AMBITIOUS) it.listed = true }; L("${p?.name} kırıldı. Hırslı oyuncular bu durumda kulüpten ayrılmak isteyebilir.", "${p?.name} is hurt. Ambitious players may want to leave in this situation.") }
            }
            WANTS_OUT -> when (choice) {
                0 -> { p?.let { it.listed = true; it.morale = (it.morale + 6).coerceAtMost(100.0) }; Fans.change(g, -3.0, L("Yıldız satış listesinde", "Star on the transfer list")); L("${p?.name} satış listesinde. Büyük teklifler gelebilir.", "${p?.name} is on the transfer list. Big offers may come in.") }
                1 -> {
                    val extra = ((p?.wage ?: 0) * 0.2).toLong()
                    if (p != null && Economy.squadWages(g, us.id) + extra <= us.wageBudget) {
                        p.salary = p.wage + extra; p.morale = (p.morale + 10).coerceAtMost(100.0); p.contractYears = maxOf(p.contractYears, 3)
                        L("${p.name} ikna oldu: yeni maaş ve 3 yıllık sözleşmeyle kalıyor.", "${p.name} has been convinced: he stays with a new wage and a 3-year contract.")
                    } else L("Maaş bütçesi yetmedi; ${p?.name} hâlâ gitmek istiyor.", "The wage budget wasn't enough; ${p?.name} still wants to leave.")
                }
                else -> { p?.let { it.morale = (it.morale - 15).coerceAtLeast(5.0) }; g.cohesion = (g.cohesion - 2).coerceAtLeast(15.0); Fans.change(g, 3.0, L("Yıldız satılmadı", "Star not sold")); L("${p?.name} kalıyor ama mutsuz. Taraftar ise kararını alkışladı.", "${p?.name} stays but is unhappy. The fans applauded your decision.") }
            }
            else -> if (choice == 0 && p != null) {
                promise(g, p, need = 4, window = 6)
                p.squadRole = 1
                p.morale = (p.morale + 10).coerceAtMost(100.0)
                if (rnd.nextDouble() < 0.5) p.potential = (p.potential + 2).coerceAtMost(95)
                L("${p.name}'e ilk 11 sözü verildi. Sözünü tutmazsan kırılabilir!", "${p.name} has been promised a starting place. If you break your word he may be hurt!")
            } else {
                p?.let { it.morale = (it.morale - 3).coerceAtLeast(5.0) }
                L("${p?.name} biraz hayal kırıklığına uğradı ama çalışmaya devam ediyor.", "${p?.name} is a little disappointed but keeps working.")
            }
        }
        g.addNews(e.title + L(" · karar", " · decision"), msg)
        return msg
    }
}
