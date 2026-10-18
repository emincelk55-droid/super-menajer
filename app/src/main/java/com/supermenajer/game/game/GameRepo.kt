package com.supermenajer.game.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Game
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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
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

    fun init(d: File) {
        dir = d
    }

    fun bump() {
        version++
    }

    private val saveFile get() = File(dir, "kariyer.json")
    fun hasSave(): Boolean = saveFile.exists()

    fun save() {
        val g = game ?: return
        runCatching {
            val tmp = File(dir, "kariyer.tmp")
            tmp.writeText(json.encodeToString(Game.serializer(), g))
            tmp.renameTo(saveFile)
        }
    }

    fun load(): Boolean = runCatching {
        game = json.decodeFromString(Game.serializer(), saveFile.readText())
        bump()
        true
    }.getOrDefault(false)

    fun newCareer(g: Game, manager: String, teamId: Int) {
        g.managerName = manager.ifBlank { "Menajer" }
        g.userTeamId = teamId
        Season.start(g)
        val t = g.userTeam
        g.addNews(
            "Hoş geldin hocam!",
            "${t.name} yönetimi seni yeni teknik direktör olarak açıkladı. Yönetimin bu sezonki hedefi: ${Season.objectiveText(t.expected)}. Bütçen: ${money(t.budget)}.",
        )
        game = g
        tab = 0
        save()
        screen = Screen.MAIN
        bump()
    }

    fun playMatch(live: Boolean) {
        val g = game ?: return
        val f = g.nextUserFixture() ?: return
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
        Season.endWeek(g)
        Lineups.validate(g, g.userTeam)
        save()
        screen = Screen.SUMMARY
        bump()
    }

    fun closeSummary() {
        match = null
        screen = Screen.MAIN
        bump()
    }

    fun startNewSeason() {
        val g = game ?: return
        Season.newSeason(g)
        save()
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

    fun askingPrice(p: Player): Long {
        val g = game ?: return p.value
        if (p.teamId < 0) return (p.value * 0.15 / 25_000).roundToLong() * 25_000
        val seller = g.team(p.teamId)
        val key = p.id in seller.lineup
        return ((p.value * if (key) 1.35 else 1.1) / 25_000).roundToLong() * 25_000
    }

    /** Hata mesajı döner; başarılıysa null. */
    fun buy(p: Player): String? {
        val g = game ?: return "Oyun yok"
        val us = g.userTeam
        if (p.teamId == us.id) return "Oyuncu zaten senin takımında."
        val price = askingPrice(p)
        if (us.budget < price) return "Bütçe yetersiz. Gereken: ${money(price)}"
        if (g.squad(us.id).size >= 30) return "Kadro dolu (en fazla 30 oyuncu)."
        val sellerName: String
        if (p.teamId >= 0) {
            val seller = g.team(p.teamId)
            if (g.squad(seller.id).size <= 18) return "${seller.name} kadrosu dar olduğu için satmıyor."
            seller.budget += price
            seller.lineup.replaceAll { if (it == p.id) -1 else it }
            sellerName = seller.name
        } else sellerName = "serbest oyuncu"
        us.budget -= price
        p.teamId = us.id
        p.morale = 80.0
        g.addNews("Transfer!", "${p.name} (${p.age}, ${p.pos.label}, ${p.overall}) ${money(price)} karşılığında $sellerName → ${us.name}.")
        changed()
        return null
    }

    fun makeOffer(p: Player): Offer? {
        val g = game ?: return null
        if (g.squad(g.userTeamId).size <= 16) return null
        val price = (p.value * Random.nextDouble(0.85, 1.05) / 25_000).roundToLong() * 25_000
        val buyer = g.teams.filter { it.id != g.userTeamId && it.budget >= price && g.squad(it.id).size < 30 }
            .randomOrNull() ?: return null
        return Offer(p, buyer, price)
    }

    fun acceptOffer(o: Offer) {
        val g = game ?: return
        val us = g.userTeam
        if (o.player.teamId != us.id) return
        us.budget += o.price
        o.buyer.budget -= o.price
        o.player.teamId = o.buyer.id
        Lineups.validate(g, us)
        g.addNews("Satış", "${o.player.name}, ${money(o.price)} karşılığında ${o.buyer.name} takımına satıldı.")
        selectedPlayer = null
        changed()
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
