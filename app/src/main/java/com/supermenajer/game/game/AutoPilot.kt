package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.random.Random

/**
 * Test modu: yapay zekâ menajer kullanıcının yerine oynar. Gerçek oyuncunun kullandığı
 * [GameRepo] yollarını kullanır (olay kartları, basın, teklifler, sponsor, transfer,
 * sözleşme, maç), böylece arayüzün tetiklediği kodlar da sınanır. Her adımdan sonra
 * [Invariants] ile dünyanın tutarlılığı denetlenebilir.
 */
object AutoPilot {
    /** Otopilotun bir adımda yaptığı iş (kayıt için). */
    class Log {
        val actions = mutableListOf<String>()
        var matches = 0
        var wins = 0
        var draws = 0
        var losses = 0
        var bought = 0
        var sold = 0
        var renewed = 0
        var events = 0
        var fired = 0
        var seasons = 0
        fun add(s: String) { actions += s; if (actions.size > 400) actions.removeAt(0) }
    }

    /**
     * Bir karar ya da bir gün. Dönüş: sezon bittiyse ve yeni sezon başlatıldıysa true.
     * [maxSeasons] dolunca yeni sezona geçmez.
     */
    fun step(log: Log, rnd: Random): Boolean {
        val g = GameRepo.game ?: return false
        when {
            g.fired -> {
                val id = g.jobOffers.firstOrNull()
                log.fired++
                if (id != null) { GameRepo.takeJob(id); log.add("Kovuldu → yeni iş: ${g.userTeam.name}") }
                else { g.jobOffers += g.teams.filter { it.division == 1 || it.reputation < 55 }.random(rnd).id; log.add("Kovuldu, teklif bekleniyor") }
            }
            g.pendingEvent != null -> {
                val e = g.pendingEvent!!
                val c = rnd.nextInt(e.options.size.coerceAtLeast(1))
                GameRepo.resolveEvent(c)
                log.events++
                log.add("Olay: ${e.title} → ${e.options.getOrNull(c)}")
            }
            g.press != null -> {
                val q = g.press!!
                GameRepo.answerPress(q, rnd.nextInt(q.answers.size.coerceAtLeast(1)))
                g.press = null
            }
            g.careerOffer -> GameRepo.declineCareerOffer()
            g.medical.isNotEmpty() -> {
                val p = g.player(g.medical.first())
                if (p == null) g.medical.removeAt(0) else GameRepo.medicalDecision(p, rnd.nextInt(3))
            }
            g.offers.isNotEmpty() -> handleIncoming(g, log)
            g.natPending.isNotEmpty() -> GameRepo.playNational(live = false)
            g.seasonOver && Tournament.needed(g) -> { Tournament.start(g); GameRepo.bump() }
            g.seasonOver -> {
                log.seasons++
                val t = g.table(g.userTeam.division)
                log.add("Sezon ${g.season} bitti: ${g.userTeam.name} ${t.indexOf(g.userTeam) + 1}. (${g.userTeam.points} puan)")
                GameRepo.startNewSeason()
                return true
            }
            g.todayUserFixture() != null -> playMatch(g, log)
            else -> {
                manage(g, log, rnd)
                GameRepo.advanceDay()
            }
        }
        return false
    }

    private fun playMatch(g: Game, log: Log) {
        GameRepo.autoLineup()
        val f = g.todayUserFixture() ?: return
        GameRepo.playMatch(live = false)
        GameRepo.closeSummary()
        if (f.hg >= 0) {
            val us = if (f.home == g.userTeamId) f.hg else f.ag
            val them = if (f.home == g.userTeamId) f.ag else f.hg
            log.matches++
            when { us > them -> log.wins++; us == them -> log.draws++; else -> log.losses++ }
        }
    }

    /** Gelen teklifler: kilit oyuncu değilse ve fiyat değerinin üstündeyse sat, yoksa reddet. */
    private fun handleIncoming(g: Game, log: Log) {
        val o = g.offers.first()
        val p = g.player(o.playerId)
        val key = g.squad(g.userTeamId).sortedByDescending { it.overall }.take(13)
        if (p != null && p !in key && o.price >= p.value && g.squad(g.userTeamId).size > 22) {
            val err = GameRepo.acceptIncoming(o)
            if (err == null) { log.sold++; log.add("Satış: ${p.name} ${o.price / 1000}K") } else GameRepo.rejectIncoming(o)
        } else GameRepo.rejectIncoming(o)
        g.offers.remove(o)
    }

    /** Maç olmayan günlerde kulüp yönetimi (haftada bir kez kadar). */
    private fun manage(g: Game, log: Log, rnd: Random) {
        // Sponsor: en iyi teklifi imzala
        g.sponsorOffers.maxByOrNull { s -> s.base + s.winBonus * 15 }?.let { GameRepo.signSponsor(it) }
        // Yetenek puanı
        if (Perks.points(g) > 0) Perks.all.filter { Perks.lvl(g, it.id) < it.max }.randomOrNull(rnd)?.let { GameRepo.upgradePerk(it.id) }
        if (rnd.nextInt(7) != 0) return
        val sq = g.squad(g.userTeamId)
        // Sözleşmesi biten iyi oyuncuları uzat
        for (p in sq.filter { it.contractYears <= 1 }.sortedByDescending { it.overall }.take(3)) {
            if (p.overall >= sq.map { it.overall }.sorted().getOrElse(sq.size / 2) { 0 } && GameRepo.renewContract(p, 2 + rnd.nextInt(2)) == null) {
                log.renewed++
            }
        }
        // Transfer: pencere açıkken en zayıf mevkiye bütçeye uygun takviye
        if (Season.transferWindowOpen(g) && sq.size < 26) buy(g, log, rnd)
    }

    private fun buy(g: Game, log: Log, rnd: Random) {
        val us = g.userTeam
        val sq = g.squad(us.id)
        val weakest = Pos.entries.filter { it != Pos.GK || sq.count { p -> p.pos == Pos.GK } < 3 }
            .minByOrNull { pos -> sq.filter { it.pos == pos }.sortedByDescending { it.overall }.take(if (pos == Pos.GK) 1 else 3).map { it.overall }.average().takeIf { !it.isNaN() } ?: 0.0 } ?: return
        val bar = sq.filter { it.pos == weakest }.maxOfOrNull { it.overall } ?: 0
        val cands = g.players.filter { it.pos == weakest && it.teamId != us.id && it.teamId >= 0 && it.overall > bar - 3 && it.age <= 31 }
            .filter { GameRepo.transferBlock(it) == null }
            .map { it to GameRepo.askingPrice(it) }
            .filter { (_, price) -> price <= us.budget * 0.6 }
            .sortedByDescending { it.first.overall }
            .take(5)
        val (p, price) = cands.randomOrNull(rnd) ?: return
        when (val r = GameRepo.offerFor(p, price)) {
            is GameRepo.Reply.Accepted -> { log.bought++; log.add("Transfer: ${p.name} (${p.overall}) ${price / 1000}K"); GameRepo.afterTransfer() }
            is GameRepo.Reply.Counter -> if (r.price <= us.budget * 0.6 && GameRepo.offerFor(p, r.price) is GameRepo.Reply.Accepted) {
                log.bought++; log.add("Transfer (pazarlık): ${p.name} ${r.price / 1000}K"); GameRepo.afterTransfer()
            }
            is GameRepo.Reply.Rejected -> {}
        }
    }

    private fun <T> List<T>.randomOrNull(rnd: Random): T? = if (isEmpty()) null else this[rnd.nextInt(size)]
}

/** Dünyanın tutarlılık denetimleri: test modunda her gün çalışır, sorunları metin olarak döner. */
object Invariants {
    fun check(g: Game): List<String> {
        val out = mutableListOf<String>()
        val teamIds = g.teams.map { it.id }.toHashSet()
        val ids = HashSet<Int>()
        for (p in g.players) {
            if (!ids.add(p.id)) out += "Çift oyuncu kimliği: ${p.id} ${p.name}"
            if (p.teamId >= 0 && p.teamId !in teamIds) out += "Oyuncunun takımı yok: ${p.name} → ${p.teamId}"
            if (p.overall !in 1..99) out += "Güç aralık dışı: ${p.name} ${p.overall}"
            if (p.age !in 14..46) out += "Yaş aralık dışı: ${p.name} ${p.age}"
            if (p.fitness.isNaN() || p.fitness < -0.5 || p.fitness > 100.5) out += "Kondisyon bozuk: ${p.name} ${p.fitness}"
            if (p.morale.isNaN() || p.morale < -0.5 || p.morale > 100.5) out += "Moral bozuk: ${p.name} ${p.morale}"
            if (p.value < 0) out += "Negatif değer: ${p.name}"
        }
        for (t in g.teams) {
            if (t.division < 0) continue
            val n = g.squad(t.id).size
            if (n < 16 || n > 34) out += "Kadro boyutu: ${t.name} $n"
            if (t.lineup.size > 11) out += "İlk 11 fazla: ${t.name} ${t.lineup.size}"
            val lu = t.lineup.filter { it >= 0 }
            if (lu.size != lu.toSet().size) out += "İlk 11'de aynı oyuncu iki kez: ${t.name}"
            if (t.id == g.userTeamId && lu.any { id -> g.player(id)?.teamId != t.id }) out += "Kullanıcı ilk 11'inde başka takımın oyuncusu"
            if (t.played != t.won + t.drawn + t.lost) out += "Puan tablosu tutarsız: ${t.name}"
        }
        for (f in g.fixtures) {
            if (f.home !in teamIds || f.away !in teamIds) { out += "Fikstürde olmayan takım: ${f.home}-${f.away}"; break }
            if (f.hg > 15 || f.ag > 15) out += "Uçuk skor: ${f.hg}-${f.ag}"
        }
        if (g.userTeam.budget < -500_000_000L) out += "Bütçe uçtu: ${g.userTeam.budget}"
        if (g.news.size > 400) out += "Haber listesi şişiyor: ${g.news.size}"
        return out.distinct().take(30)
    }
}
