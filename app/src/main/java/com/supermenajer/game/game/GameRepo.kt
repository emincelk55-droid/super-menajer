package com.supermenajer.game.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.IncomingOffer
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Team
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToLong
import kotlin.random.Random

enum class Screen { TITLE, NEW_GAME, MAIN, MATCH, SUMMARY }

class Offer(val player: Player, val buyer: Team, val price: Long)

/** Oyunun tek durum kaynağı. Değişikliklerden sonra [bump] çağrılır ve ekranlar yeniden çizilir. */
object GameRepo {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    /** Kayıt dosyası arka planda, sırayla yazılır (arayüz donmasın). */
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()
    private lateinit var dir: File

    var game by mutableStateOf<Game?>(null)
        private set
    var version by mutableIntStateOf(0)
        private set
    var screen by mutableStateOf(Screen.TITLE)
    var match: MatchSim? = null
        private set
    var selectedPlayer by mutableStateOf<Player?>(null)
    var tab by mutableIntStateOf(0)
    var tacticsMode by mutableIntStateOf(0)
    var leagueTab by mutableIntStateOf(-1)
    var leagueSel by mutableIntStateOf(-1)
    var reportOpen by mutableStateOf(false)
    var squadMode by mutableIntStateOf(0)

    fun init(d: File) {
        dir = d
    }

    /** Başlık ekranındayken arka planda hazırlanan yeni dünya. */
    @Volatile
    var preparedWorld: Game? = null

    fun prepareWorldAsync() {
        if (preparedWorld != null) return
        Thread {
            val w = Generator.newWorld().also { Season.computeExpectations(it) }
            if (preparedWorld == null) preparedWorld = w
        }.start()
    }

    fun takePreparedWorld(): Game? = preparedWorld.also { preparedWorld = null }

    fun bump() {
        version++
    }

    private val saveFile get() = File(dir, "kariyer.json")
    fun hasSave(): Boolean = saveFile.exists()

    private var lastSave = 0L

    /** Kaydı yazar. force=false iken sık değişikliklerde en fazla 4 saniyede bir yazar. */
    fun save(force: Boolean = false) {
        val g = game ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSave < 4_000) return
        lastSave = now
        val text = runCatching { json.encodeToString(Game.serializer(), g) }.getOrNull() ?: return
        writer.execute {
            runCatching {
                val tmp = File(dir, "kariyer.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(saveFile)) {
                    saveFile.delete()
                    tmp.renameTo(saveFile)
                }
            }
        }
    }

    /** Uygulama kapanırken yazmanın bitmesini bekle. */
    fun flush() {
        runCatching { writer.submit {}.get(3, java.util.concurrent.TimeUnit.SECONDS) }
    }

    fun load(): Boolean = runCatching {
        val g = json.decodeFromString(Game.serializer(), saveFile.readText())
        Season.migrate(g)
        game = g
        save(force = true)
        bump()
        true
    }.getOrDefault(false)

    fun newCareer(g: Game, manager: String, teamId: Int) {
        g.managerName = manager.ifBlank { "Menajer" }
        g.userTeamId = teamId
        Season.start(g)
        StaffLogic.hireDefaults(g)
        val t = g.userTeam
        g.addNews(
            "Hoş geldin hocam!",
            "${t.name} yönetimi seni yeni teknik direktör olarak açıkladı. Yönetimin bu sezonki hedefi: ${Season.objectiveText(t)}. Bütçen: ${money(t.budget)}.",
        )
        game = g
        tab = 0
        save(force = true)
        screen = Screen.MAIN
        bump()
    }

    fun playMatch(live: Boolean) {
        val g = game ?: return
        if (g.fired) return
        val f = g.todayUserFixture() ?: return
        val sim = MatchSim(g, f, g.userTeamId, autoUserSubs = !live)
        match = sim
        if (live) {
            screen = Screen.MATCH
            bump()
        } else {
            sim.runToEnd()
            finishMatch()
        }
    }

    fun finishMatch() {
        val g = game ?: return
        val sim = match ?: return
        if (sim.fixture.played) return
        sim.runToEnd()
        sim.applyToGame()
        Season.matchNews(g, sim)
        Season.simulateRest(g, sim.fixture.week)
        Season.endDay(g)
        Lineups.validate(g, g.userTeam)
        save(force = true)
        screen = Screen.SUMMARY
        bump()
    }

    /** Kullanıcının maçı olmayan günü (ör. kupadan elendiyse) geçer. */
    fun skipDay() {
        val g = game ?: return
        if (g.fired || g.seasonOver || g.todayUserFixture() != null) return
        Season.simulateRest(g, g.week)
        Season.endDay(g)
        Lineups.validate(g, g.userTeam)
        changed()
    }

    /** Kullanıcının maçı olan güne kadar ilerler (yeni teklif gelirse durur). */
    fun skipToNextMatch() {
        val g = game ?: return
        var guard = 0
        while (!g.fired && !g.seasonOver && g.todayUserFixture() == null && guard++ < 30) {
            val offersBefore = g.offers.size
            Season.simulateRest(g, g.week)
            Season.endDay(g)
            if (g.offers.size > offersBefore) break
        }
        Lineups.validate(g, g.userTeam)
        save(force = true)
        bump()
    }

    fun answerPress(q: com.supermenajer.game.model.PressQuestion, index: Int): String {
        val g = game ?: return ""
        val msg = Press.answer(g, q, index)
        g.addNews("Basın toplantısı", "\"${q.answers.getOrElse(index) { "" }}\" — $msg")
        changed()
        return msg
    }

    fun skipPress() {
        game?.press = null
        changed()
    }

    // ---- Oyuncu yönetimi (CM tarzı) ----

    private fun talkStamp(g: Game) = g.season * 1000 + g.leagueWeekAt(g.week)

    fun canTalk(p: Player): Boolean {
        val g = game ?: return false
        return p.teamId == g.userTeamId && p.lastTalk != talkStamp(g)
    }

    /** Birebir konuşma: 0 övgü, 1 eleştiri, 2 forma sözü. Haftada bir. */
    fun talkToPlayer(p: Player, kind: Int): String {
        val g = game ?: return ""
        if (!canTalk(p)) return "${p.name} ile bu hafta zaten konuştun."
        p.lastTalk = talkStamp(g)
        val r = Random.nextDouble()
        val good = p.apps == 0 || p.avgRating >= 6.8
        val (delta, msg) = when (kind) {
            0 -> if (good) 6.0 to "${p.name} övgülerin için teşekkür etti, çok motive." else 2.0 to "${p.name} övgüne biraz şaşırdı ama memnun."
            1 -> when {
                p.apps >= 3 && p.avgRating < 6.4 && r < 0.6 -> 5.0 to "${p.name} mesajı aldı: \"Daha çok çalışacağım hocam.\""
                p.apps >= 3 && p.avgRating < 6.4 -> -6.0 to "${p.name} eleştirine sert tepki gösterdi."
                else -> -8.0 to "${p.name} eleştiriyi haksız buldu ve kırıldı."
            }
            else -> {
                p.promised = true
                10.0 to "${p.name} sözüne güveniyor. Gelecek hafta ilk 11'de olmazsa çok kırılacak!"
            }
        }
        p.morale = (p.morale + delta).coerceIn(5.0, 100.0)
        if (p.wantsAway && p.morale > 45) p.wantsAway = false
        changed()
        return msg
    }

    /** Genç oyuncuyu kiralık gönder (oynayarak gelişir). Hata mesajı döner. */
    fun loanOut(p: Player): String? {
        val g = game ?: return "Oyun yok"
        val us = g.userTeam
        if (p.teamId != us.id) return "Oyuncu takımında değil."
        if (p.age > 23) return "Sadece 23 yaş ve altı oyuncular kiralık gönderilebilir."
        if (!Season.transferWindowOpen(g)) return "Transfer dönemi kapalı."
        if (g.squad(us.id).size <= 18) return "Kadro çok dar, kiralık gönderemezsin."
        val country = g.userCountry
        val clubs = g.teams.filter { t ->
            t.id != us.id && t.division != com.supermenajer.game.model.Leagues.LEGACY_EURO &&
                g.squad(t.id).size < 28 && p.overall >= Lineups.strength(g, t) - 3 && t.reputation < us.reputation
        }
        val club = clubs.filter { g.countryOf(it) == country }.randomOrNull() ?: clubs.randomOrNull()
            ?: return "Oyuncuyu düzenli oynatacak bir kulüp bulunamadı."
        us.lineup.replaceAll { if (it == p.id) -1 else it }
        g.movePlayer(p, club.id)
        p.loanFrom = us.id
        Lineups.validate(g, us)
        g.addNews("Kiralık", "${p.name} sezon sonuna kadar kiralık olarak ${club.name} (${g.leagueName(club)}) takımına gitti. Düzenli oynayıp gelişecek.")
        selectedPlayer = null
        changed()
        return null
    }

    fun recallLoan(p: Player) {
        val g = game ?: return
        if (p.loanFrom != g.userTeamId) return
        g.teamOrNull(p.teamId)?.lineup?.replaceAll { if (it == p.id) -1 else it }
        g.movePlayer(p, g.userTeamId)
        p.loanFrom = -1
        g.addNews("Kiralıktan geri çağrıldı", "${p.name} takıma geri döndü.")
        changed()
    }

    fun scout(p: Player): String {
        val g = game ?: return ""
        val err = Scouting.request(g, p)
        changed()
        return err ?: "Gözlemci yola çıktı. Rapor yarın gelecek (${money(Scouting.COST)})."
    }

    fun setScout(country: String, focus: Int) {
        val g = game ?: return
        g.scoutCountry = country
        g.scoutFocus = focus
        changed()
    }

    fun hireStaff(st: com.supermenajer.game.model.Staff): String {
        val g = game ?: return ""
        val err = StaffLogic.hire(g, st)
        changed()
        return err ?: "${st.name} göreve başladı!"
    }

    fun startProject(type: Int, seatOption: Int = 0): String {
        val g = game ?: return ""
        val err = Facilities.start(g, type, seatOption)
        changed()
        return err ?: "İnşaat başladı!"
    }

    fun setTicketPrice(i: Int) {
        val g = game ?: return
        g.userTeam.ticketPrice = i.coerceIn(0, 2)
        changed()
    }

    fun declineCareerOffer() {
        val g = game ?: return
        g.jobOffers.clear()
        g.careerOffer = false
        g.addNews("Teklif reddedildi", "${g.userTeam.name} ile devam ediyorsun. Taraftar sadakatini alkışlıyor!")
        g.confidence = (g.confidence + 5).coerceAtMost(100.0)
        changed()
    }

    fun takeJob(teamId: Int) {
        val g = game ?: return
        Season.takeJob(g, teamId)
        StaffLogic.hireDefaults(g)
        g.addNews("Yeni teknik ekip", "${g.userTeam.name}'nin teknik ekibiyle çalışacaksın. Kadro → Teknik Ekip ekranından değişiklik yapabilirsin.")
        tab = 0
        changed()
    }

    fun setTraining(v: Int) {
        game?.userTeam?.training = v
        changed()
    }

    fun setIntensity(v: Int) {
        game?.userTeam?.intensity = v
        changed()
    }

    fun closeSummary() {
        match = null
        screen = Screen.MAIN
        bump()
    }

    fun startNewSeason() {
        val g = game ?: return
        Season.newSeason(g)
        save(force = true)
        bump()
    }

    // ---- Kadro / taktik ----

    fun setFormation(name: String) {
        val g = game ?: return
        Lineups.changeFormation(g, g.userTeam, name)
        changed()
    }

    fun setMentality(v: Int) {
        game?.userTeam?.mentality = v
        changed()
    }

    fun setPressing(v: Int) {
        game?.userTeam?.pressing = v
        changed()
    }

    fun autoLineup() {
        val g = game ?: return
        Lineups.autoPick(g, g.userTeam)
        changed()
    }

    /** Bir oyuncuyu ilk 11'deki bir mevkiye koyar; zaten 11'deyse yer değiştirir. */
    fun putInSlot(slot: Int, playerId: Int) {
        val t = game?.userTeam ?: return
        if (slot !in 0..10) return
        val existing = t.lineup.indexOf(playerId)
        if (existing >= 0) {
            t.lineup[existing] = t.lineup[slot]
        }
        t.lineup[slot] = playerId
        changed()
    }

    fun swapSlots(a: Int, b: Int) {
        val t = game?.userTeam ?: return
        val tmp = t.lineup[a]
        t.lineup[a] = t.lineup[b]
        t.lineup[b] = tmp
        changed()
    }

    /** Oyuncuyu kendi mevkiindeki en zayıf ilk 11 oyuncusunun yerine koyar. */
    fun addToLineup(p: Player) {
        val g = game ?: return
        val t = g.userTeam
        if (p.id in t.lineup || !p.available) return
        val slots = com.supermenajer.game.model.Formations.slots(t.formation)
        val candidates = slots.indices.filter { slots[it].pos == p.pos }.ifEmpty { slots.indices.filter { it != 0 } }
        val worst = candidates.minByOrNull { i -> g.player(t.lineup[i])?.ovrAt(slots[i].pos) ?: -1 } ?: return
        t.lineup[worst] = p.id
        changed()
    }

    fun removeFromLineup(p: Player) {
        val g = game ?: return
        val t = g.userTeam
        val i = t.lineup.indexOf(p.id)
        if (i < 0) return
        t.lineup[i] = -1
        val slots = com.supermenajer.game.model.Formations.slots(t.formation)
        val best = g.squad(t.id).filter { it.available && it.id !in t.lineup && it.id != p.id }
            .maxByOrNull { Lineups.score(it, slots[i].pos) }
        if (best != null) t.lineup[i] = best.id else t.lineup[i] = p.id
        changed()
    }

    private fun changed() {
        save()
        bump()
    }

    // ---- Transfer ----

    /** Oturum boyunca pazarlık hafızası: oyuncu id -> (deneme sayısı, karşı teklif). */
    private val talks = HashMap<Int, Pair<Int, Long>>()

    private fun r25k(v: Double): Long = (v / 25_000).roundToLong() * 25_000

    fun askingPrice(p: Player): Long {
        val g = game ?: return p.value
        if (p.teamId < 0) return r25k(p.value * 0.15)
        val seller = g.team(p.teamId)
        val key = p.id in seller.lineup
        val needsMoney = if (seller.budget < 0) 0.9 else 1.0
        return r25k(p.value * (if (key) 1.35 else 1.1) * needsMoney)
    }

    fun counterFor(p: Player): Long? = talks[p.id]?.second?.takeIf { it > 0 }

    /** Transfere engel bir durum varsa açıklamasını döner. */
    fun transferBlock(p: Player): String? {
        val g = game ?: return "Oyun yok"
        val us = g.userTeam
        if (p.teamId == us.id) return "Oyuncu zaten senin takımında."
        if (p.teamId >= 0 && !Season.transferWindowOpen(g)) return "Transfer dönemi kapalı. Şu an sadece serbest oyuncu alabilirsin."
        if (g.squad(us.id).size >= 30) return "Kadro dolu (en fazla 30 oyuncu)."
        if (!Economy.fitsWages(g, us, p.demandWage)) {
            return "Maaş bütçesi yetersiz: ${money(Economy.squadWages(g, us.id))} + ${money(p.demandWage)} > ${money(us.wageBudget)}/hafta. Önce oyuncu satmalısın."
        }
        if (p.teamId >= 0 && g.squad(p.teamId).size <= 18) return "${g.team(p.teamId).name} kadrosu dar olduğu için satmıyor."
        val level = Lineups.strength(g, us)
        if (p.teamId >= 0) {
            val seller = g.team(p.teamId)
            if (p.overall >= level + 6 && (us.reputation < seller.reputation || us.division > seller.division)) {
                return "${p.name} daha büyük bir kulüpte oynamak istiyor, takımımıza gelmeye sıcak bakmıyor."
            }
        } else if (p.overall >= level + 10) {
            return "${p.name} daha iddialı bir takım arıyor."
        }
        val tries = talks[p.id]?.first ?: 0
        if (tries >= 3) return "Kulüp görüşmeleri kesti. Bir süre bu oyuncu için teklif yapamazsın."
        return null
    }

    sealed class Reply {
        data object Accepted : Reply()
        class Counter(val price: Long) : Reply()
        class Rejected(val msg: String) : Reply()
    }

    fun offerFor(p: Player, offer: Long): Reply {
        val g = game ?: return Reply.Rejected("Oyun yok")
        transferBlock(p)?.let { return Reply.Rejected(it) }
        val us = g.userTeam
        if (us.budget < offer) return Reply.Rejected("Bütçe yetersiz.")
        val asking = askingPrice(p)
        val counter = counterFor(p)
        val (tries, _) = talks[p.id] ?: (0 to 0L)
        if (offer >= asking || (counter != null && offer >= counter)) {
            completeBuy(p, offer)
            talks.remove(p.id)
            return Reply.Accepted
        }
        if (p.teamId < 0) {
            talks[p.id] = (tries + 1) to 0L
            return Reply.Rejected("${p.name} bu imza parasını az buldu.")
        }
        val seller = g.team(p.teamId).name
        if (offer >= asking * 0.8) {
            val c = r25k((offer + asking) / 2.0).coerceAtLeast(offer + 25_000)
            talks[p.id] = (tries + 1) to c
            return Reply.Counter(c)
        }
        talks[p.id] = (tries + 1) to (counter ?: 0L)
        return Reply.Rejected("$seller teklifi reddetti: \"Bu rakam ciddi değil.\"")
    }

    private fun completeBuy(p: Player, price: Long) {
        val g = game ?: return
        val us = g.userTeam
        val sellerName = if (p.teamId >= 0) {
            val seller = g.team(p.teamId)
            Economy.earn(seller, Economy.SALES, price)
            seller.lineup.replaceAll { if (it == p.id) -1 else it }
            seller.name
        } else "serbest oyuncu"
        Economy.earn(us, Economy.BUYS, -price)
        g.movePlayer(p, us.id)
        p.morale = 80.0
        p.salary = p.demandWage
        p.contractYears = if (p.age >= 30) 2 else 4
        g.addNews("Transfer!", "${p.name} (${p.age}, ${p.pos.label}, ${p.overall}) ${money(price)} karşılığında $sellerName → ${us.name}.")
        changed()
    }

    fun makeOffer(p: Player): Offer? {
        val g = game ?: return null
        if (!Season.transferWindowOpen(g)) return null
        if (g.squad(g.userTeamId).size <= 16) return null
        val price = r25k(p.value * Random.nextDouble(0.85, 1.05))
        val buyer = g.teams.filter { it.id != g.userTeamId && it.budget >= price && g.squad(it.id).size < 30 }
            .randomOrNull() ?: return null
        return Offer(p, buyer, price)
    }

    fun acceptOffer(o: Offer) {
        sellPlayer(o.player, o.buyer, o.price)
    }

    private fun sellPlayer(p: Player, buyer: Team, price: Long) {
        val g = game ?: return
        val us = g.userTeam
        if (p.teamId != us.id) return
        Economy.earn(us, Economy.SALES, price)
        Economy.earn(buyer, Economy.BUYS, -price)
        g.movePlayer(p, buyer.id)
        p.salary = p.demandWage
        p.contractYears = 4
        g.offers.removeAll { it.playerId == p.id }
        Lineups.validate(g, us)
        g.addNews("Satış", "${p.name}, ${money(price)} karşılığında ${buyer.name} takımına satıldı.")
        selectedPlayer = null
        changed()
    }

    /** Sözleşme uzatma. Hata mesajı döner; başarılıysa null. */
    fun renewContract(p: Player, years: Int): String? {
        val g = game ?: return "Oyun yok"
        val us = g.userTeam
        if (p.teamId != us.id) return "Oyuncu takımında değil."
        val demand = Economy.renewalDemand(g, p) ?: return "${p.name} kulüpte mutlu değil ve sözleşme uzatmak istemiyor."
        if (!Economy.fitsWages(g, us, demand, minusWage = p.wage)) {
            return "Maaş bütçesi yetmiyor (${money(us.wageBudget)}/hafta). Önce başka bir oyuncu satmalısın."
        }
        p.salary = demand
        p.contractYears = years + 1
        p.morale = (p.morale + 8).coerceAtMost(100.0)
        g.addNews("Sözleşme uzatıldı", "${p.name} ile $years yıllık yeni sözleşme imzalandı (${money(demand)}/hafta).")
        changed()
        return null
    }

    fun acceptIncoming(o: IncomingOffer) {
        val g = game ?: return
        val p = g.player(o.playerId) ?: return
        sellPlayer(p, g.team(o.teamId), o.price)
    }

    fun rejectIncoming(o: IncomingOffer) {
        val g = game ?: return
        g.offers.remove(o)
        g.player(o.playerId)?.let { if (o.price > it.value * 1.2) it.morale = (it.morale - 8).coerceAtLeast(5.0) }
        changed()
    }

    /** Daha yüksek bedel ister; kulüp %50 ihtimalle kabul eder. Mesaj döner. */
    fun counterIncoming(o: IncomingOffer): String {
        val g = game ?: return ""
        val buyer = g.team(o.teamId)
        val ask = r25k(o.price * 1.2)
        val msg = if (buyer.budget >= ask && Random.nextDouble() < 0.5) {
            o.price = ask
            "${buyer.name} yeni rakamı kabul etti: ${money(ask)}. Satışı onaylayabilirsin."
        } else {
            g.offers.remove(o)
            "${buyer.name} masadan kalktı."
        }
        changed()
        return msg
    }
}

fun money(v: Long): String {
    val a = kotlin.math.abs(v)
    val s = when {
        a >= 1_000_000 -> String.format("%.1fM", a / 1_000_000.0)
        a >= 1_000 -> "${a / 1_000}K"
        else -> "$a"
    }
    return (if (v < 0) "-€" else "€") + s
}
