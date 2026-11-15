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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToLong
import kotlin.random.Random

enum class Screen { SPLASH, TITLE, NEW_GAME, MAIN, MATCH, SUMMARY }

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
    var screen by mutableStateOf(Screen.SPLASH)
    var match: MatchSim? = null
        private set
    var selectedPlayer by mutableStateOf<Player?>(null)
    /** Karşılaştırma: sabitlenen oyuncu ve açık karşılaştırma (iki oyuncu kimliği). */
    var comparePin by mutableStateOf<Int?>(null)
    var tacticWizard by mutableStateOf(false)
    var openTeam by mutableIntStateOf(-1)
    var editorOpen by mutableStateOf(false)
    var instrSub by mutableIntStateOf(0)
    var roleDialog by mutableIntStateOf(-1)
    var compare by mutableStateOf<Pair<Int, Int>?>(null)

    fun toggleWatch(p: Player) {
        val g = game ?: return
        if (p.id in g.watch) g.watch.remove(p.id) else g.watch += p.id
        changed()
    }

    /** ⚖: sabitli oyuncu varsa onunla, yoksa kadrodaki aynı mevkideki en iyi oyuncuyla karşılaştırır. */
    fun compareWith(p: Player) {
        val g = game ?: return
        val pin = comparePin
        if (pin != null && pin != p.id && g.player(pin) != null) {
            compare = pin to p.id
            return
        }
        comparePin = p.id
        if (p.teamId != g.userTeamId) {
            val d = Positions.det(p)
            val mine = g.squad(g.userTeamId).filter { Positions.canPlay(it, d) }.maxByOrNull { it.overall }
                ?: g.squad(g.userTeamId).filter { it.pos == p.pos }.maxByOrNull { it.overall }
            if (mine != null) compare = mine.id to p.id
        }
    }
    var tab by mutableIntStateOf(0)
    var tacticsMode by mutableIntStateOf(0)
    var leagueTab by mutableIntStateOf(-1)
    var leagueSel by mutableIntStateOf(-1)
    var reportOpen by mutableStateOf(false)
    var squadMode by mutableIntStateOf(0)
    /** Canlı maçın 2D sahnesi (ekran yeniden oluşsa da kaybolmasın). */
    var pitch: com.supermenajer.game.engine.Pitch2D? = null
    fun pitchFor(sim: com.supermenajer.game.engine.MatchSim) =
        pitch?.takeIf { it.sim === sim } ?: com.supermenajer.game.engine.Pitch2D(sim).also { pitch = it }
    /** Kulüp sekmesi: 0 teknik ekip, 1 tesisler, 2 sponsorlar, 3 finans, 4 taraftar. */
    var clubMode by mutableIntStateOf(0)
    var inboxCat by mutableIntStateOf(0)
    var inboxSel by mutableStateOf<com.supermenajer.game.model.News?>(null)
    var settingsOpen by mutableStateOf(false)

    /** Sezon sonu ödül gecesi açık mı. */
    var awardsOpen by mutableStateOf(false)

    /** Haftanın gazetesi açık mı. */
    var newspaperOpen by mutableStateOf(false)
    var tacticsPanel by mutableIntStateOf(0)

    // Ana menü sekmeleri
    const val TAB_HOME = 0
    const val TAB_SQUAD = 1
    const val TAB_TACTICS = 2
    const val TAB_LEAGUE = 3
    const val TAB_TRANSFER = 4
    const val TAB_INBOX = 5
    const val TAB_CLUB = 6
    const val TAB_NATIONAL = 7

    fun openNews(n: com.supermenajer.game.model.News) {
        inboxSel = n
        if (!n.read) {
            n.read = true
            changed()
        }
    }

    fun readAll() {
        val g = game ?: return
        g.news.forEach { it.read = true }
        changed()
    }

    /** Gün gün ilerlerken true; DEVAM düğmesi "DURDUR" olur. */
    var advancing by mutableStateOf(false)
    private val scope by lazy { kotlinx.coroutines.MainScope() }
    private var advanceJob: kotlinx.coroutines.Job? = null

    /** Ana menüye dönerken açık kariyeri kapatır: ilerleme durur, başka yuvanın üstüne yazılmaz. */
    fun closeCareer() {
        advancing = false
        advanceJob?.cancel()
        advanceJob = null
        screen = Screen.TITLE
        settingsOpen = false
        selectedPlayer = null
        negotiation = null
        match = null
        pitch = null
        game = null
        bump()
    }

    private val importantWords = listOf("teklif", "sakatl", "talep", "kovul", "kariyer", "inşaat tamamlandı", "altyapı seçmeleri", "sözleşme", "protesto", "ödül", "şampiyon",
        "offer", "injur", "request", "sacked", "job offer", "construction complete", "youth intake", "contract", "protest", "award", "champion")

    /**
     * Takvimde bir gün ilerler. Durulması gereken bir şey olduysa true döner:
     * maç günü, transfer teklifi, basın toplantısı, önemli mesaj, sezon sonu...
     */
    fun advanceDay(): Boolean {
        val g = game ?: return true
        if (g.fired || g.seasonOver) return true
        if (g.todayUserFixture() != null || g.natPending.isNotEmpty()) return true
        val offers = g.offers.size
        val newsBefore = g.news.firstOrNull()
        if (!g.isMatchDay) {
            g.date++
            Season.freeDay(g)
        } else {
            Season.simulateRest(g, g.week)
            Season.endDay(g)
            Lineups.validate(g, g.userTeam)
        }
        val fresh = g.news.takeWhile { it !== newsBefore }
        val important = fresh.any { n -> importantWords.any { it in n.title.lowercase() } }
        return g.offers.size > offers || g.press != null || g.careerOffer || g.fired || g.seasonOver || g.pendingEvent != null || g.natPending.isNotEmpty() ||
            important || g.todayUserFixture() != null
    }

    /** CM tarzı "Devam": maç günü maça çıkar, değilse günleri tek tek geçer ve önemli bir olayda durur. */
    fun continueGame() {
        val g = game ?: return
        when {
            autoRunning -> autoRunning = false
            advancing -> advancing = false
            g.fired -> tab = TAB_HOME
            g.pendingEvent != null -> tab = TAB_HOME
            g.natPending.isNotEmpty() -> playNational(live = true)
            g.seasonOver && Tournament.needed(g) -> { Tournament.start(g); save(force = true); bump() }
            g.seasonOver -> startNewSeason()
            g.todayUserFixture() != null -> playMatch(live = true)
            else -> {
                advancing = true
                advanceJob?.cancel()
                advanceJob = scope.launch {
                    while (advancing && game === g) {
                        // Günün simülasyonu arka planda: arayüz donmaz
                        val stop = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { advanceDay() }
                        bump()
                        if (stop) break
                        kotlinx.coroutines.delay(170)
                    }
                    if (game !== g) return@launch
                    advancing = false
                    if (g.pendingEvent != null) tab = TAB_HOME
                    save(force = true)
                    bump()
                }
            }
        }
    }

    /** Test modu (otopilot) çalışıyor mu; durum satırı. */
    var autoRunning by mutableStateOf(false)
    var autoStatus by mutableStateOf("")
    private var autoJob: kotlinx.coroutines.Job? = null

    /**
     * Test modu: yapay zekâ menajer [days] gün (≤0: sezon sonuna kadar) senin yerine oynar; her adımda
     * dünya tutarlılığını denetler, bitince gelen kutusuna rapor bırakır. Tekrar çağrılırsa durur.
     */
    fun autoPlay(days: Int) {
        val g = game ?: return
        if (autoRunning) { autoRunning = false; return }
        autoRunning = true
        advancing = false
        val log = AutoPilot.Log()
        val problems = LinkedHashSet<String>()
        val startDate = g.date
        val startSeason = g.season
        autoJob?.cancel()
        autoJob = scope.launch {
            var steps = 0
            try {
                while (autoRunning && game === g && steps++ < 3000) {
                    val newSeason = AutoPilot.step(log, Random)
                    if (steps % 4 == 0) problems += Invariants.check(g)
                    autoStatus = L("🤖 Otopilot · ${GameCalendar.format(g.date)} · ${log.wins}G ${log.draws}B ${log.losses}M", "🤖 Autopilot · ${GameCalendar.format(g.date)} · ${log.wins}W ${log.draws}D ${log.losses}L")
                    bump()
                    if (newSeason || (days > 0 && g.date - startDate >= days) || g.season != startSeason) break
                    kotlinx.coroutines.delay(15)
                }
            } catch (e: Throwable) {
                problems += L("ÇÖKME: ${e::class.simpleName}: ${e.message}", "CRASH: ${e::class.simpleName}: ${e.message}")
            }
            autoRunning = false
            autoStatus = ""
            if (game !== g) return@launch
            val body = buildString {
                append(L("Otopilot ${g.date - startDate} gün oynadı. Maçlar: ${log.matches} (${log.wins}G ${log.draws}B ${log.losses}M). ", "Autopilot played ${g.date - startDate} days. Matches: ${log.matches} (${log.wins}W ${log.draws}D ${log.losses}L). "))
                append(L("Alış ${log.bought}, satış ${log.sold}, sözleşme uzatma ${log.renewed}, olay kararı ${log.events}. ", "Bought ${log.bought}, sold ${log.sold}, contract extensions ${log.renewed}, event decisions ${log.events}. "))
                if (log.actions.isNotEmpty()) append(L("Son işler: ", "Latest actions: ") + log.actions.takeLast(5).joinToString(" · ") + ". ")
                append(if (problems.isEmpty()) L("Tutarlılık denetimi: sorun yok ✓", "Consistency check: no problems ✓") else L("Bulunan sorunlar (${problems.size}): ", "Problems found (${problems.size}): ") + problems.take(8).joinToString(" | "))
            }
            g.addNews(L("🤖 Test modu raporu", "🤖 Test mode report"), body)
            save(force = true)
            bump()
        }
    }

    fun continueLabel(g: Game): String = when {
        autoRunning -> L("🤖 DURDUR", "🤖 STOP")
        advancing -> L("⏸ DURDUR", "⏸ STOP")
        g.fired -> L("TEKLİFLER", "OFFERS")
        g.pendingEvent != null -> L("⚡ KARAR VER", "⚡ DECIDE")
        g.natPending.isNotEmpty() -> L("MİLLİ MAÇ ▶", "INTERNATIONAL ▶")
        g.seasonOver && Tournament.needed(g) -> L("TURNUVA ▶", "TOURNAMENT ▶")
        g.seasonOver -> L("YENİ SEZON ▶", "NEW SEASON ▶")
        g.todayUserFixture() != null -> L("MAÇA ÇIK ▶", "KICK OFF ▶")
        else -> L("DEVAM ▶", "CONTINUE ▶")
    }

    fun init(d: File) {
        dir = d
    }

    /** Başlık ekranındayken arka planda hazırlanan yeni dünya. */
    @Volatile
    var preparedWorld: Game? = null

    @Volatile
    private var preparing = false

    fun prepareWorldAsync() {
        if (preparedWorld != null || preparing) return
        preparing = true
        Thread {
            try {
                val w = Generator.newWorld().also { Season.computeExpectations(it) }
                if (preparedWorld == null) preparedWorld = w
            } finally {
                preparing = false
            }
        }.start()
    }

    /** Hazırlanan dünyayı alır; hazırlık sürüyorsa ikinci kez üretmek yerine onu bekler (arka planda çağrılmalı). */
    fun awaitWorld(): Game {
        while (preparedWorld == null && preparing) Thread.sleep(50)
        return takePreparedWorld() ?: Generator.newWorld().also { Season.computeExpectations(it) }
    }

    fun takePreparedWorld(): Game? = preparedWorld.also { preparedWorld = null }

    fun bump() {
        version++
    }

    /** Aktif kayıt yuvası (1-3). 1. yuva eski tek kayıt dosyasını kullanır. */
    var slot by mutableIntStateOf(1)

    private fun slotFile(n: Int) = File(dir, if (n == 1) "kariyer.json" else "kariyer$n.json")
    private fun metaFile(n: Int) = File(dir, "kariyer$n.meta")
    /** Döner yedekler: .bak1 en yeni, .bak3 en eski (en az 10 dakika arayla alınır). */
    private fun backupFile(n: Int, k: Int) = File(dir, "kariyer$n.bak$k")
    private const val BACKUPS = 3
    private const val BACKUP_EVERY_MS = 10 * 60 * 1000L

    /** Son yüklemede ana kayıt bozuktu ve yedekten dönüldüyse kullanıcıya gösterilecek not. */
    var restoredNote by mutableStateOf<String?>(null)

    private fun rotateBackups(n: Int, current: File) {
        if (!current.exists()) return
        val newest = backupFile(n, 1)
        if (newest.exists() && System.currentTimeMillis() - newest.lastModified() < BACKUP_EVERY_MS) return
        for (k in BACKUPS downTo 2) {
            val from = backupFile(n, k - 1)
            if (from.exists()) { val to = backupFile(n, k); to.delete(); from.renameTo(to) }
        }
        current.copyTo(newest, overwrite = true)
    }
    private val saveFile get() = slotFile(slot)
    fun hasSave(): Boolean = (1..3).any { slotFile(it).exists() }

    class SlotInfo(val n: Int, val exists: Boolean, val team: String, val manager: String, val detail: String, val color: Long)

    /** Başlık ekranı için kayıt yuvalarının özeti. */
    fun slots(): List<SlotInfo> = (1..3).map { n ->
        val f = slotFile(n)
        if (!f.exists()) return@map SlotInfo(n, false, "", "", "", 0)
        val m = runCatching { metaFile(n).readText().split("|") }.getOrNull()
        if (m == null || m.size < 5) SlotInfo(n, true, L("Kayıtlı kariyer", "Saved career"), "", "", 0xFF2E7D32)
        else SlotInfo(n, true, m[0], m[1], m[2] + " · " + m[3], m[4].toLongOrNull() ?: 0xFF2E7D32)
    }

    /** En son oynanan kayıt yuvası (dosya tarihine göre), yoksa null. */
    fun lastSlot(): Int? = (1..3).filter { slotFile(it).exists() }.maxByOrNull { slotFile(it).lastModified() }

    /** Yedek için yuvanın kayıt dosyası (gzip JSON); yoksa null. Açık kariyer önce diske yazılır. */
    fun exportSlot(n: Int): ByteArray? {
        if (n == slot && game != null) { save(force = true); flush() }
        return slotFile(n).takeIf { it.exists() }?.readBytes()
    }

    /** Yedek dosyasının önerilen adı: SuperMenajer_<kulüp>_<sezon>.smk */
    fun exportName(n: Int): String {
        val s = slots().getOrNull(n - 1)
        val team = (s?.team ?: "kariyer").filter { it.isLetterOrDigit() }.take(20)
        return "SuperMenajer_${team}_yuva$n.smk"
    }

    /**
     * Dışarıdan gelen yedeği yuvaya yazar. Dosya okunup oyun kaydı olarak çözülebiliyorsa yazılır,
     * yuvadaki eski kayıt önce yedeğe alınır. Hata varsa nedenini döner.
     */
    fun importSlot(n: Int, bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return L("Dosya okunamadı.", "The file couldn't be read.")
        val text = runCatching { readSaveBytes(bytes) }.getOrNull() ?: return L("Dosya bozuk.", "The file is corrupted.")
        val g = runCatching { json.decodeFromString(Game.serializer(), text).also { Season.migrate(it) } }.getOrNull()
            ?: return L("Bu dosya bir Süper Menajer kaydı değil.", "This file isn't a Super Manager save.")
        val f = slotFile(n)
        if (f.exists()) runCatching { f.copyTo(backupFile(n, 1), overwrite = true) }
        writeSave(f, text)
        metaFile(n).writeText(meta(g))
        if (n == slot && screen == Screen.TITLE) game = null
        bump()
        return null
    }

    fun deleteSlot(n: Int) {
        if (n == slot && screen == Screen.TITLE) game = null
        slotFile(n).delete()
        metaFile(n).delete()
        for (k in 1..BACKUPS) backupFile(n, k).delete()
        bump()
    }

    private fun meta(g: Game): String {
        val t = g.userTeam
        val date = if (g.date > 0) GameCalendar.format(g.date) else "${g.season}"
        val pos = if (t.played > 0) L(" · ${g.position(t.id)}. sıra", " · ${g.position(t.id)}. place") else ""
        return listOf(t.name, g.managerName, L("${g.season}-${(g.season + 1) % 100} sezonu$pos", "${g.season}-${(g.season + 1) % 100} season$pos"), date, t.color1.toString()).joinToString("|") { it.replace("|", " ") }
    }

    private var lastSave = 0L
    private var trailingSave = false

    /**
     * Kayıt dosyası gzip ile sıkıştırılmış JSON (~10 kat küçük, telefonda daha hızlı yazılır ve yüklenir).
     * Eski sürümlerin düz metin kayıtları da okunur (ilk iki bayt gzip imzasına bakılır).
     */
    internal fun writeSave(f: File, text: String) {
        java.util.zip.GZIPOutputStream(f.outputStream().buffered(), 64 * 1024).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    internal fun readSave(f: File): String = readSaveBytes(f.readBytes())

    internal fun readSaveBytes(bytes: ByteArray): String {
        val gz = bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        return if (gz) java.util.zip.GZIPInputStream(bytes.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
        else bytes.toString(Charsets.UTF_8)
    }

    /** Kaydı yazar. force=false iken sık değişikliklerde en fazla 4 saniyede bir yazar. */
    fun save(force: Boolean = false) {
        if (versus) return
        val g = game ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSave < 4_000) {
            // Atlanan kayıt kaybolmasın: kısa süre sonra bir kez daha yaz
            if (!trailingSave) {
                trailingSave = true
                // Testlerde ana iş parçacığı yoktur: o durumda gecikmeli kayıt atlanır
                runCatching { scope.launch { kotlinx.coroutines.delay(4_500); trailingSave = false; save() } }
                    .onFailure { trailingSave = false }
            }
            return
        }
        lastSave = now
        val text = runCatching { json.encodeToString(Game.serializer(), g) }.getOrNull() ?: return
        val target = saveFile
        val metaText = runCatching { meta(g) }.getOrNull()
        val n = slot
        writer.execute {
            runCatching {
                val tmp = File(dir, "kariyer.tmp")
                writeSave(tmp, text)
                rotateBackups(n, target)
                if (!tmp.renameTo(target)) {
                    target.delete()
                    tmp.renameTo(target)
                }
                metaText?.let { metaFile(n).writeText(it) }
            }
        }
    }

    /** Uygulama kapanırken yazmanın bitmesini bekle. */
    fun flush() {
        runCatching { writer.submit {}.get(3, java.util.concurrent.TimeUnit.SECONDS) }
    }

    /**
     * Kaydı açar. Ana dosya bozuksa (yarım yazılmış, elektrik kesintisi vb.) en yeni sağlam yedekten
     * döner ve [restoredNote] ile kullanıcıya söyler.
     */
    fun load(n: Int = slot): Boolean {
        advancing = false
        advanceJob?.cancel()
        fun read(f: File): Game? = runCatching {
            json.decodeFromString(Game.serializer(), readSave(f)).also { Season.migrate(it) }
        }.getOrNull()
        // Yuva ancak dosya sorunsuz okunursa değişir: bozuk kayıt başka kariyerle ezilmesin
        var g = read(slotFile(n))
        if (g == null) {
            for (k in 1..BACKUPS) {
                val b = backupFile(n, k)
                if (!b.exists()) continue
                g = read(b) ?: continue
                val date = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr")).format(java.util.Date(b.lastModified()))
                restoredNote = L("Kayıt dosyası bozulmuştu; $date tarihli yedekten geri yüklendi.", "The save file was corrupted; it was restored from the backup dated $date.")
                g.addNews(L("🛟 Kayıt kurtarıldı", "🛟 Save recovered"), restoredNote!!)
                break
            }
        }
        if (g == null) return false
        slot = n
        game = g
        save(force = true)
        bump()
        return true
    }

    fun newCareer(
        g: Game, manager: String, teamId: Int, difficulty: Int = 1, noFiring: Boolean = false,
        nation: String = "TR", rep: Int = 1, intro: Boolean = false, national: String = "",
    ) {
        g.difficulty = difficulty
        g.noFiring = noFiring
        g.nationalTeam = national
        g.managerNation = nation
        g.managerRep = listOf(25, 40, 60)[rep.coerceIn(0, 2)]
        g.introDone = !intro
        advancing = false
        advanceJob?.cancel()
        g.managerName = manager.ifBlank { L("Menajer", "Manager") }
        g.userTeamId = teamId
        Season.start(g)
        StaffLogic.hireDefaults(g)
        val t = g.userTeam
        t.budget = (t.budget * Difficulty.budgetFactor(g)).toLong()
        // İtibar: ünlü menajere yönetim daha çok güvenir
        g.confidence = (g.confidence + (rep.coerceIn(0, 2) - 1) * 6).coerceIn(0.0, 100.0)
        g.addNews(
            L("Hoş geldin hocam!", "Welcome, boss!"),
            L("${t.name} yönetimi seni yeni teknik direktör olarak açıkladı. Yönetimin bu sezonki hedefi: ${Season.objectiveText(t)}. Bütçen: ${money(t.budget)}.", "The ${t.name} board has announced you as the new manager. The board's objective this season: ${Season.objectiveText(t)}. Your budget: ${money(t.budget)}."),
        )
        if (intro) {
            g.addNews(L("Hazırlık maçları", "Friendlies"), L("Yardımcı antrenörün, kadroyu tanıman için hazırlık dönemi boyunca 3 hazırlık maçı oynayabileceğini hatırlatıyor. Ana sayfadaki maç hazırlığı kartından ayarlayabilirsin.", "Your assistant reminds you that you can play 3 friendlies during pre-season to get to know the squad. Set them up from the match preparation card on the home screen."))
            g.addNews(L("Kaptanını seçmek ister misin?", "Would you like to choose your captain?"), L("Takımın kaptanı ve duran top kullanıcıları şu an otomatik seçiliyor. Taktik → Talimatlar → Takım görevleri bölümünden kendin belirleyebilirsin. Kaptan, soyunma odasının morali ve uyumu üzerinde etkilidir.", "The team captain and set-piece takers are currently picked automatically. You can choose them yourself under Tactics → Instructions → Team duties. The captain influences dressing room morale and chemistry."))
            g.addNews(L("Takım raporu hazır", "Squad report ready"), teamReport(g))
        }
        game = g
        tab = 0
        save(force = true)
        screen = Screen.MAIN
        bump()
    }

    /** Yardımcı antrenörün yeni menajere hazırladığı kadro raporu. */
    fun teamReport(g: Game): String {
        val t = g.userTeam
        val sq = g.squad(t.id)
        val need = mapOf(com.supermenajer.game.model.Pos.GK to 1, com.supermenajer.game.model.Pos.DF to 4, com.supermenajer.game.model.Pos.MF to 4, com.supermenajer.game.model.Pos.FW to 2)
        val avg = need.mapValues { (p, n) -> sq.filter { it.pos == p }.sortedByDescending { it.overall }.take(n).map { it.overall }.average().takeIf { !it.isNaN() } ?: 0.0 }
        val best = avg.maxBy { it.value }
        val worst = avg.minBy { it.value }
        val stars = sq.sortedByDescending { it.overall }.take(3).joinToString { "${it.name} (${it.overall})" }
        val young = sq.filter { it.age <= 21 }.maxByOrNull { it.potential }
        val assistant = StaffLogic.of(g, StaffLogic.ASSISTANT)?.name ?: L("Yardımcı antrenörün", "Your assistant manager")
        return L("$assistant hazırladığı raporu sunuyor. Kadronun en güçlü bölgesi ${best.key.label.lowercase()} (ortalama ${best.value.toInt()}), ", "$assistant presents his report. The squad's strongest area is ${best.key.label.lowercase()} (average ${best.value.toInt()}), ") +
            L("en zayıf bölgesi ${worst.key.label.lowercase()} (ortalama ${worst.value.toInt()}). Öne çıkan oyuncular: $stars.", "the weakest is ${worst.key.label.lowercase()} (average ${worst.value.toInt()}). Standout players: $stars.") +
            (young?.let { L(" Geleceğin yıldızı olabilir: ${it.name} (${it.age} yaş).", " A possible future star: ${it.name} (${it.age} yrs).") } ?: "") +
            L(" Önerisi: ${worst.key.label.lowercase()} bölgesine takviye ve ${t.formation} dizilişiyle başlamak.", " His advice: strengthen ${worst.key.label.lowercase()} and start with a ${t.formation}.")
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

    /** Bekleyen milli maçı canlı ya da hızlı oynatır. */
    fun playNational(live: Boolean) {
        val g = game ?: return
        if (!live) {
            National.simulatePending(g)
            changed()
            bump()
            return
        }
        val sim = National.buildMatch(g, live = true) ?: return
        match = sim
        pitch = null
        screen = Screen.MATCH
        bump()
    }

    fun finishMatch() {
        val g = game ?: return
        val sim = match ?: return
        if (sim.twoPlayer) {
            sim.runToEnd()
            screen = Screen.SUMMARY
            bump()
            return
        }
        if (sim.nat != null) {
            if (g.natPending.isEmpty()) return
            sim.runToEnd()
            sim.applyNational()
            National.finish(g, sim)
            save(force = true)
            screen = Screen.SUMMARY
            bump()
            return
        }
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
        // Aradaki boş günler de yaşanır (dinlenme, gözlemci raporları, transferler)
        while (!g.isMatchDay) { g.date++; Season.freeDay(g) }
        if (g.fired || g.seasonOver || g.todayUserFixture() != null) return
        Season.simulateRest(g, g.week)
        Season.endDay(g)
        Lineups.validate(g, g.userTeam)
        changed()
    }

    /** Kullanıcının maçı olan güne kadar ilerler (yeni teklif gelirse durur). */
    /** Uzun işlem sürerken gösterilen mesaj (null = yok). Arayüz bu sırada dokunuşları engeller. */
    var busy by mutableStateOf<String?>(null)

    /** Ağır simülasyonu arka planda çalıştırır, bitince ana iş parçacığında devam eder. */
    private fun runBusy(msg: String, work: () -> Unit, then: () -> Unit = {}) {
        if (busy != null) return
        busy = msg
        val launched = runCatching {
            scope.launch {
                runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { work() } }
                busy = null
                then()
                save(force = true)
                bump()
            }
        }
        if (launched.isFailure) { busy = null; work(); then(); save(force = true); bump() }
    }

    fun skipToNextMatchAsync() = runBusy(L("Maç gününe ilerleniyor...", "Moving to match day..."), { skipCore() })

    fun quickMatchAsync() {
        val g = game ?: return
        val f = g.todayUserFixture() ?: return
        match = MatchSim(g, f, g.userTeamId, autoUserSubs = true)
        finishMatchAsync()
    }

    fun finishMatchAsync() {
        val sim = match ?: return
        if (sim.nat != null || sim.twoPlayer) { finishMatch(); return }
        val g = game ?: return
        if (sim.fixture.played) return
        runBusy(L("Diğer maçlar oynanıyor...", "Other matches being played..."), {
            sim.runToEnd()
            sim.applyToGame()
            Season.matchNews(g, sim)
            Season.simulateRest(g, sim.fixture.week)
            Season.endDay(g)
        }) {
            Lineups.validate(g, g.userTeam)
            screen = Screen.SUMMARY
        }
    }

    fun skipToNextMatch() {
        skipCore()
        save(force = true)
        bump()
    }

    private fun skipCore() {
        val g = game ?: return
        var guard = 0
        while (!g.fired && !g.seasonOver && g.todayUserFixture() == null && g.natPending.isEmpty() && guard++ < 30) {
            if (!g.isMatchDay) {
                // Aradaki boş günler de yaşanır (gözlemci raporları, transferler)
                while (!g.isMatchDay) { g.date++; Season.freeDay(g) }
                continue
            }
            val offersBefore = g.offers.size
            Season.simulateRest(g, g.week)
            Season.endDay(g)
            if (g.offers.size > offersBefore) break
        }
        Lineups.validate(g, g.userTeam)
    }

    fun answerPress(q: com.supermenajer.game.model.PressQuestion, index: Int): String {
        val g = game ?: return ""
        val msg = Press.answer(g, q, index)
        g.addNews(L("Basın toplantısı", "Press conference"), "\"${q.answers.getOrElse(index) { "" }}\" — $msg")
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
        if (!canTalk(p)) return L("${p.name} ile bu hafta zaten konuştun.", "You've already talked to ${p.name} this week.")
        p.lastTalk = talkStamp(g)
        val r = Random.nextDouble()
        val good = p.apps == 0 || p.avgRating >= 6.8
        val (delta, msg) = when (kind) {
            0 -> if (good) 6.0 to L("${p.name} övgülerin için teşekkür etti, çok motive.", "${p.name} thanked you for the praise, he's very motivated.") else 2.0 to L("${p.name} övgüne biraz şaşırdı ama memnun.", "${p.name} was a little surprised by the praise but pleased.")
            1 -> when {
                p.apps >= 3 && p.avgRating < 6.4 && r < 0.6 -> 5.0 to L("${p.name} mesajı aldı: \"Daha çok çalışacağım hocam.\"", "${p.name} got the message: \"I'll work harder, boss.\"")
                p.apps >= 3 && p.avgRating < 6.4 -> -6.0 to L("${p.name} eleştirine sert tepki gösterdi.", "${p.name} reacted angrily to your criticism.")
                else -> -8.0 to L("${p.name} eleştiriyi haksız buldu ve kırıldı.", "${p.name} found the criticism unfair and is hurt.")
            }
            else -> {
                p.promised = true
                10.0 to L("${p.name} sözüne güveniyor. Gelecek hafta ilk 11'de olmazsa çok kırılacak!", "${p.name} trusts your word. If he's not in the starting XI next week he'll be very hurt!")
            }
        }
        p.morale = (p.morale + delta).coerceIn(5.0, 100.0)
        if (p.wantsAway && p.morale > 45) p.wantsAway = false
        changed()
        return msg
    }

    /** Genç oyuncuyu kiralık gönder (oynayarak gelişir). Hata mesajı döner. */
    fun loanOut(p: Player): String? {
        val g = game ?: return L("Oyun yok", "No game")
        val us = g.userTeam
        if (p.teamId != us.id) return L("Oyuncu takımında değil.", "The player isn't in your team.")
        if (p.loanFrom >= 0) return L("Kiralık oyuncu tekrar kiralanamaz.", "A loan player can't be loaned out again.")
        if (p.age > 23) return L("Sadece 23 yaş ve altı oyuncular kiralık gönderilebilir.", "Only players aged 23 and under can be loaned out.")
        if (!Season.transferWindowOpen(g)) return L("Transfer dönemi kapalı.", "The transfer window is closed.")
        if (g.squad(us.id).size <= 18) return L("Kadro çok dar, kiralık gönderemezsin.", "The squad is too thin, you can't loan anyone out.")
        val country = g.userCountry
        val clubs = g.teams.filter { t ->
            t.id != us.id && t.division != com.supermenajer.game.model.Leagues.LEGACY_EURO &&
                g.squad(t.id).size < 28 && p.overall >= Lineups.strength(g, t) - 3 && t.reputation < us.reputation
        }
        val club = clubs.filter { g.countryOf(it) == country }.randomOrNull() ?: clubs.randomOrNull()
            ?: return L("Oyuncuyu düzenli oynatacak bir kulüp bulunamadı.", "No club was found that would play him regularly.")
        us.lineup.replaceAll { if (it == p.id) -1 else it }
        g.movePlayer(p, club.id)
        p.loanFrom = us.id
        Lineups.validate(g, us)
        g.addNews(L("Kiralık", "Loan"), L("${p.name} sezon sonuna kadar kiralık olarak ${club.name} (${g.leagueName(club)}) takımına gitti. Düzenli oynayıp gelişecek.", "${p.name} has gone on loan to ${club.name} (${g.leagueName(club)}) until the end of the season. He'll play regularly and develop."))
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
        g.addNews(L("Kiralıktan geri çağrıldı", "Recalled from loan"), L("${p.name} takıma geri döndü.", "${p.name} is back with the team."))
        changed()
    }

    fun scout(p: Player): String {
        val g = game ?: return ""
        val err = Scouting.request(g, p)
        changed()
        return err ?: L("Gözlemci yola çıktı. Rapor yarın gelecek (${money(Scouting.COST)}).", "The scout is on his way. The report arrives tomorrow (${money(Scouting.COST)}).")
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
        return err ?: L("${st.name} göreve başladı!", "${st.name} has started work!")
    }

    fun startProject(type: Int, seatOption: Int = 0): String {
        val g = game ?: return ""
        val err = Facilities.start(g, type, seatOption)
        changed()
        return err ?: L("İnşaat başladı!", "Construction started!")
    }

    fun signSponsor(s: com.supermenajer.game.model.Sponsor): String {
        val g = game ?: return ""
        val err = Sponsors.sign(g, s)
        changed()
        return err ?: L("Anlaşma imzalandı: ${s.name}!", "Deal signed: ${s.name}!")
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
        g.addNews(L("Teklif reddedildi", "Offer declined"), L("${g.userTeam.name} ile devam ediyorsun. Taraftar sadakatini alkışlıyor!", "You're staying with ${g.userTeam.name}. The fans applaud your loyalty!"))
        g.confidence = (g.confidence + 5).coerceAtMost(100.0)
        changed()
    }

    fun takeJob(teamId: Int) {
        val g = game ?: return
        Season.takeJob(g, teamId)
        StaffLogic.hireDefaults(g)
        g.addNews(L("Yeni teknik ekip", "New staff"), L("${g.userTeam.name}'nin teknik ekibiyle çalışacaksın. Kulüp → Teknik Ekip ekranından değişiklik yapabilirsin.", "You'll work with the ${g.userTeam.name} staff. You can make changes on the Club → Staff screen."))
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
        if (versus) {
            // İki kişilik maç bitti: dünya kariyere karışmadan ana menüye dönülür
            versus = false
            game = null
            pitch = null
            screen = Screen.TITLE
        } else screen = Screen.MAIN
        bump()
    }

    /** İki kişilik (aynı cihazda) dostluk maçı. */
    var versus = false
        private set

    fun startVersus(world: Game, homeId: Int, awayId: Int) {
        advancing = false
        versus = true
        world.userTeamId = homeId
        world.reindex()
        Lineups.autoPick(world, world.team(homeId))
        Lineups.autoPick(world, world.team(awayId))
        val f = com.supermenajer.game.model.Fixture(1, homeId, awayId, comp = com.supermenajer.game.model.Comp.FRIENDLY)
        game = world
        match = MatchSim(world, f, homeId, autoUserSubs = false, twoPlayer = true)
        pitch = null
        screen = Screen.MATCH
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
        g.userTeam.roles.clear()
        changed()
    }

    fun setMentality(v: Int) {
        game?.userTeam?.mentality = v
        changed()
    }

    /** Takım talimatı: tempo, width, passing, defLine (0-2) veya counter, offside, timewaste (0/1). */
    fun setInstruction(key: String, v: Int) {
        val t = game?.userTeam ?: return
        when (key) {
            "tempo" -> t.tempo = v
            "width" -> t.width = v
            "passing" -> t.passing = v
            "defLine" -> t.defLine = v
            "counter" -> t.counter = v == 1
            "offside" -> t.offsideTrap = v == 1
            "timewaste" -> t.timeWasting = v == 1
        }
        changed()
    }

    fun setRole(slot: Int, role: Int) {
        val t = game?.userTeam ?: return
        Roles.setRole(t, slot, role)
        changed()
    }

    /** kind: 0 kaptan, 1 penaltı, 2 frikik, 3 korner. */
    fun setDuty(kind: Int, playerId: Int) {
        val t = game?.userTeam ?: return
        when (kind) {
            0 -> t.captain = playerId
            1 -> t.penaltyTaker = playerId
            2 -> t.fkTaker = playerId
            else -> t.cornerTaker = playerId
        }
        changed()
    }

    fun setOppInstr(v: Int) { game?.let { it.oppInstr = if (it.oppInstr == v) -1 else v }; changed() }
    fun medicalDecision(p: Player, choice: Int): String { val g = game ?: return ""; val m = Medical.decide(g, p, choice); changed(); return m }

    fun payReleaseClause(p: Player): String {
        val g = game ?: return ""
        Contracts.clauseBlock(g, p)?.let { return it }
        val price = p.releaseClause
        completeBuy(p, price)
        p.releaseClause = 0
        g.addNews(L("💥 Serbest kalma bedeli ödendi", "💥 Release clause paid"), L("${p.name} için ${money(price)} tutarındaki serbest kalma bedeli yatırıldı.", "The ${money(price)} release clause for ${p.name} has been paid."))
        return L("${p.name} artık bizim oyuncumuz!", "${p.name} is now our player!")
    }

    fun signPreContract(p: Player): String { val g = game ?: return ""; val m = Contracts.signPre(g, p); changed(); return m }

    /** Veri editörü: oyuncu bilgilerini günceller. a: hız, şut, pas, teknik, savunma, fizik, kalecilik. */
    fun editPlayer(p: Player, name: String, age: Int, potential: Int, a: List<Int>) {
        p.name = name
        p.age = age.coerceIn(15, 45)
        p.pace = a[0].coerceIn(1, 99); p.shooting = a[1].coerceIn(1, 99); p.passing = a[2].coerceIn(1, 99)
        p.dribbling = a[3].coerceIn(1, 99); p.defending = a[4].coerceIn(1, 99); p.physical = a[5].coerceIn(1, 99)
        p.goalkeeping = a[6].coerceIn(1, 99)
        p.potential = potential.coerceIn(p.overall, 99)
        changed()
    }

    fun setViceCaptain(id: Int) { game?.userTeam?.viceCaptain = id; changed() }

    fun setMarking(slot: Int, v: Int) {
        val t = game?.userTeam ?: return
        while (t.marking.size < 11) t.marking += 0
        t.marking[slot] = v
        changed()
    }

    /** A2 takımına gönder / A takıma al. */
    fun setReserve(p: Player, on: Boolean): String? {
        val g = game ?: return null
        if (on) {
            Reserves.canSend(g, p)?.let { return it }
            removeFromLineup(p)
            p.reserve = true
            g.addNews(L("A2 takımı", "B team"), L("${p.name} A2 takımına gönderildi.", "${p.name} has been sent to the B team.") + if (p.age >= 24) L(" Oyuncu bu karardan pek hoşnut değil.", " The player isn't happy with this decision.") else "")
        } else {
            p.reserve = false
        }
        changed()
        return null
    }

    fun takeNationalJob(code: String): String? {
        val g = game ?: return null
        val need = National.requiredRep(g, code)
        if (g.managerRep < need) return L("${National.name(code)} federasyonu en az $need itibarlı bir menajer istiyor (senin: ${g.managerRep}).", "The ${National.name(code)} federation wants a manager with at least $need reputation (yours: ${g.managerRep}).")
        g.nationalTeam = code
        g.natSquad.clear()
        g.natPoints = 0
        g.addNews(L("🌍 Milli takım görevi", "🌍 National team job"), L("${National.name(code)} Futbol Federasyonu seni kulüp görevine ek olarak milli takımın başına getirdi.", "The ${National.name(code)} Football Federation has appointed you national team manager alongside your club job."))
        changed()
        return null
    }

    fun resignNational() {
        val g = game ?: return
        g.addNews(L("🌍 Milli takımdan ayrıldın", "🌍 You left the national team"), L("${National.name(g.nationalTeam)} milli takımındaki görevini bıraktın.", "You've stepped down from the ${National.name(g.nationalTeam)} national team."))
        g.nationalTeam = ""
        g.natPending = ""
        g.natSquad.clear()
        changed()
    }

    fun toggleCallUp(p: Player) {
        val g = game ?: return
        if (g.natSquad.isEmpty()) g.natSquad += National.squad(g).map { it.id }
        if (p.id in g.natSquad) g.natSquad.remove(p.id) else if (g.natSquad.size < 26) g.natSquad += p.id
        changed()
    }

    fun camp(): String { val g = game ?: return ""; val m = Matchday.camp(g); changed(); return m }
    fun secretTraining(): String { val g = game ?: return ""; val m = Matchday.secretTraining(g); changed(); return m }
    fun friendly(): String { val g = game ?: return ""; val m = Matchday.friendly(g); changed(); return m }
    fun setWinBonus(v: Int) { game?.userTeam?.winBonus = v; changed() }
    fun setFocus(p: Player, f: Int) { Development.setFocus(p, f); changed() }
    fun setLearn(p: Player, d: Int) { Development.setLearn(p, d); changed() }
    fun resolveEvent(choice: Int): String { val g = game ?: return ""; val m = Events.resolve(g, choice); changed(); return m }
    fun applyJob(teamId: Int): String { val g = game ?: return ""; val m = ManagerMarket.apply(g, teamId); changed(); return m }
    fun upgradePerk(id: String): String { val g = game ?: return ""; val m = Perks.upgrade(g, id); changed(); return m }
    /** Arma/forma editörü: kullanıcının kulübünde değişiklik. */
    fun editIdentity(edit: (com.supermenajer.game.model.Team) -> Unit) {
        val g = game ?: return
        edit(g.userTeam)
        changed()
    }

    /** Kulüp adını ve kısa adını değiştirir; kısa ad 3 harf ve benzersiz olmalı. */
    fun renameClub(name: String, short: String): String {
        val g = game ?: return ""
        val n = name.trim()
        val s = short.trim().uppercase()
        if (n.length < 3) return L("Kulüp adı en az 3 harf olmalı.", "The club name must be at least 3 letters.")
        if (s.length != 3 || !s.all { it.isLetter() }) return L("Kısa ad 3 harf olmalı.", "The short name must be 3 letters.")
        if (g.teams.any { it.id != g.userTeamId && (it.short == s || it.name.equals(n, ignoreCase = true)) }) return L("Bu ad ya da kısa ad başka bir kulübün.", "That name or short name belongs to another club.")
        val t = g.userTeam
        val old = t.name
        t.name = n; t.short = s
        if (old != n) g.addNews(L("Kulübün yeni adı: $n", "The club's new name: $n"), L("$old artık $n ($s) adıyla anılacak. Taraftarlar yeni kimliği konuşuyor.", "$old will now be known as $n ($s). The fans are talking about the new identity."))
        changed()
        return L("Kaydedildi: $n ($s)", "Saved: $n ($s)")
    }

    fun boardRequest(kind: Int): String { val g = game ?: return ""; val m = Career.request(g, kind); changed(); return m }

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
        val worst = candidates.minByOrNull { i -> g.player(t.lineup[i])?.let { Positions.rating(it, slots[i]) } ?: -1 } ?: return
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
            .maxByOrNull { Lineups.score(it, slots[i]) }
        if (best != null) t.lineup[i] = best.id else t.lineup[i] = p.id
        changed()
    }

    fun changed() {
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
        return r25k(p.value * (if (key) 1.35 else 1.1) * needsMoney * Perks.askingFactor(g))
    }

    fun counterFor(p: Player): Long? = talks[p.id]?.second?.takeIf { it > 0 }

    /** Açık transfer görüşmesi (diyalog penceresi). */
    var negotiation by mutableStateOf<TransferTalks.Negotiation?>(null)

    fun startTalks(p: Player): String? {
        val g = game ?: return null
        transferBlock(p)?.let { return it }
        negotiation = TransferTalks.start(g, p)
        bump()
        return null
    }

    fun talkClub() {
        val g = game ?: return
        negotiation?.let { TransferTalks.clubOffer(g, it) }
        bump()
    }

    fun talkAgent() {
        val g = game ?: return
        negotiation?.let { TransferTalks.agentOffer(g, it) }
        bump()
    }

    fun closeTalks() {
        negotiation = null
        bump()
    }

    fun blockTalks(p: Player) {
        talks[p.id] = 3 to 0L
    }

    fun afterTransfer() {
        val g = game ?: return
        Lineups.validate(g, g.userTeam)
        selectedPlayer = null
        changed()
    }

    fun toggleListed(p: Player): String {
        p.listed = !p.listed
        changed()
        return if (p.listed) L("${p.name} satış listesine kondu. Kulüplerden teklif gelince gelen kutuna düşer.", "${p.name} has been put on the transfer list. Offers from clubs will arrive in your inbox.") else L("${p.name} satış listesinden çıkarıldı.", "${p.name} has been taken off the transfer list.")
    }

    /** Transfer bütçesinden maaş bütçesine (veya tersine) aktarım. Mesaj döner. */
    fun shiftBudget(weekly: Long): String {
        val g = game ?: return ""
        val msg = Economy.shiftBudget(g, weekly)
        changed()
        return msg
    }

    /** Transfere engel bir durum varsa açıklamasını döner. */
    fun transferBlock(p: Player): String? {
        val g = game ?: return L("Oyun yok", "No game")
        val us = g.userTeam
        if (p.teamId == us.id) return L("Oyuncu zaten senin takımında.", "The player is already in your team.")
        if (p.loanFrom >= 0) return L("${p.name} şu an kiralık oynuyor.", "${p.name} is currently out on loan.")
        if (p.teamId >= 0 && !Season.transferWindowOpen(g)) return L("Transfer dönemi kapalı. Şu an sadece serbest oyuncu alabilirsin.", "The transfer window is closed. You can only sign free agents right now.")
        if (g.squad(us.id).size >= 34) return L("Kadro dolu (en fazla 34 oyuncu).", "The squad is full (max 34 players).")
        if (!Economy.fitsWages(g, us, p.demandWage)) {
            return L("Maaş bütçesi yetersiz: ${money(Economy.squadWages(g, us.id))} + ${money(p.demandWage)} > ${money(us.wageBudget)}/hafta. Önce oyuncu satmalısın.", "Not enough wage budget: ${money(Economy.squadWages(g, us.id))} + ${money(p.demandWage)} > ${money(us.wageBudget)}/week. You need to sell players first.")
        }
        if (p.teamId >= 0 && g.squad(p.teamId).size <= 18) return L("${g.team(p.teamId).name} kadrosu dar olduğu için satmıyor.", "${g.team(p.teamId).name} won't sell because their squad is too thin.")
        val level = Lineups.strength(g, us)
        if (p.teamId >= 0) {
            val seller = g.team(p.teamId)
            if (p.overall >= level + 6 && (us.reputation < seller.reputation || us.division > seller.division)) {
                return L("${p.name} daha büyük bir kulüpte oynamak istiyor, takımımıza gelmeye sıcak bakmıyor.", "${p.name} wants to play for a bigger club and isn't keen on joining us.")
            }
        } else if (p.overall >= level + 10) {
            return L("${p.name} daha iddialı bir takım arıyor.", "${p.name} is looking for a more ambitious team.")
        }
        val tries = talks[p.id]?.first ?: 0
        if (tries >= 3) return L("Kulüp görüşmeleri kesti. Bir süre bu oyuncu için teklif yapamazsın.", "The club broke off talks. You can't bid for this player for a while.")
        return null
    }

    sealed class Reply {
        data object Accepted : Reply()
        class Counter(val price: Long) : Reply()
        class Rejected(val msg: String) : Reply()
    }

    fun offerFor(p: Player, offer: Long): Reply {
        val g = game ?: return Reply.Rejected(L("Oyun yok", "No game"))
        transferBlock(p)?.let { return Reply.Rejected(it) }
        val us = g.userTeam
        if (us.budget < offer) return Reply.Rejected(L("Bütçe yetersiz.", "Not enough budget."))
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
            return Reply.Rejected(L("${p.name} bu imza parasını az buldu.", "${p.name} thought this signing bonus was too low."))
        }
        val seller = g.team(p.teamId).name
        if (offer >= asking * 0.8) {
            val c = r25k((offer + asking) / 2.0).coerceAtLeast(offer + 25_000)
            talks[p.id] = (tries + 1) to c
            return Reply.Counter(c)
        }
        talks[p.id] = (tries + 1) to (counter ?: 0L)
        return Reply.Rejected(L("$seller teklifi reddetti: \"Bu rakam ciddi değil.\"", "$seller rejected the offer: \"That's not a serious figure.\""))
    }

    private fun completeBuy(p: Player, price: Long) {
        val g = game ?: return
        val us = g.userTeam
        val sellerName = if (p.teamId >= 0) {
            val seller = g.team(p.teamId)
            Economy.earn(seller, Economy.SALES, price)
            seller.lineup.replaceAll { if (it == p.id) -1 else it }
            seller.name
        } else L("serbest oyuncu", "free agent")
        Economy.earn(us, Economy.BUYS, -price)
        g.logTransfer(p, g.teamOrNull(p.teamId), us, price)
        Missions.progress(g, "sign1", 1)
        Social.transfer(g, p.name, true, price)
        g.movePlayer(p, us.id)
        Fans.bought(g, p)
        p.morale = 80.0
        p.salary = p.demandWage
        p.contractYears = if (p.age >= 30) 2 else 4
        g.addNews(L("Transfer!", "Transfer!"), L("${p.name} (${p.age}, ${p.pos.label}, ${p.overall}) ${money(price)} karşılığında $sellerName → ${us.name}.", "${p.name} (${p.age}, ${p.pos.label}, ${p.overall}) $sellerName → ${us.name} for ${money(price)}."))
        changed()
    }

    fun makeOffer(p: Player): Offer? {
        val g = game ?: return null
        if (!Season.transferWindowOpen(g)) return null
        if (g.squad(g.userTeamId).size <= 16) return null
        val price = r25k(p.value * Random.nextDouble(0.85, 1.05))
        val buyer = g.teams.filter { it.id != g.userTeamId && it.budget >= price && g.squad(it.id).size < 34 }
            .randomOrNull() ?: return null
        return Offer(p, buyer, price)
    }

    fun acceptOffer(o: Offer): String? = sellPlayer(o.player, o.buyer, o.price)

    /** Satışı yapar; teklif bekletildiyse koşullar yeniden denetlenir. Olmazsa nedenini döner. */
    private fun sellPlayer(p: Player, buyer: Team, price: Long): String? {
        val g = game ?: return L("Oyun yok", "No game")
        val us = g.userTeam
        if (p.teamId != us.id) return L("Oyuncu artık takımında değil.", "The player is no longer in your team.")
        val stale = when {
            !Season.transferWindowOpen(g) -> L("Transfer dönemi kapandı, teklif geçersiz.", "The transfer window has closed, the offer is void.")
            buyer.budget < price -> L("${buyer.name} artık bu parayı ödeyemiyor, teklif geri çekildi.", "${buyer.name} can no longer afford it, the offer has been withdrawn.")
            g.squad(us.id).size <= 16 -> L("Kadro çok dar, satış yapılamaz.", "The squad is too thin, the sale can't go through.")
            else -> null
        }
        if (stale != null) {
            g.offers.removeAll { it.playerId == p.id && it.teamId == buyer.id }
            changed()
            return stale
        }
        Economy.earn(us, Economy.SALES, price)
        Economy.earn(buyer, Economy.BUYS, -price)
        TransferTalks.paySellOn(g, p, price, us.id)
        Career.sale(g, p.name, price)
        Dynamics.soldPlayer(g, p)
        g.logTransfer(p, us, buyer, price)
        Social.transfer(g, p.name, false, price)
        p.listed = false
        p.squadRole = -1
        g.movePlayer(p, buyer.id)
        Fans.sold(g, p)
        p.salary = p.demandWage
        p.contractYears = 4
        g.offers.removeAll { it.playerId == p.id }
        Lineups.validate(g, us)
        g.addNews(L("Satış", "Sale"), L("${p.name}, ${money(price)} karşılığında ${buyer.name} takımına satıldı.", "${p.name} has been sold to ${buyer.name} for ${money(price)}."))
        selectedPlayer = null
        changed()
        return null
    }

    /** Sözleşme uzatma. Hata mesajı döner; başarılıysa null. */
    fun renewContract(p: Player, years: Int): String? {
        val g = game ?: return L("Oyun yok", "No game")
        val us = g.userTeam
        if (p.teamId != us.id) return L("Oyuncu takımında değil.", "The player isn't in your team.")
        val demand = Economy.renewalDemand(g, p) ?: return L("${p.name} kulüpte mutlu değil ve sözleşme uzatmak istemiyor.", "${p.name} isn't happy at the club and doesn't want to extend his contract.")
        if (!Economy.fitsWages(g, us, demand, minusWage = p.wage)) {
            return L("Maaş bütçesi yetmiyor (${money(us.wageBudget)}/hafta). Önce başka bir oyuncu satmalısın.", "The wage budget isn't enough (${money(us.wageBudget)}/week). You need to sell another player first.")
        }
        p.salary = demand
        p.contractYears = years + 1
        p.morale = (p.morale + 8).coerceAtMost(100.0)
        g.addNews(L("Sözleşme uzatıldı", "Contract extended"), L("${p.name} ile $years yıllık yeni sözleşme imzalandı (${money(demand)}/hafta).", "A new $years-year contract has been signed with ${p.name} (${money(demand)}/week)."))
        changed()
        return null
    }

    fun acceptIncoming(o: IncomingOffer): String? {
        val g = game ?: return null
        val p = g.player(o.playerId) ?: return null
        val buyer = g.teamOrNull(o.teamId) ?: run { g.offers.remove(o); changed(); return L("Teklif yapan kulüp bulunamadı.", "No bidding club found.") }
        return sellPlayer(p, buyer, o.price)
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
            L("${buyer.name} yeni rakamı kabul etti: ${money(ask)}. Satışı onaylayabilirsin.", "${buyer.name} accepted the new figure: ${money(ask)}. You can approve the sale.")
        } else {
            g.offers.remove(o)
            L("${buyer.name} masadan kalktı.", "${buyer.name} walked away from the table.")
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
