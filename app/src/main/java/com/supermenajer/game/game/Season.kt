package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Day
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.IncomingOffer
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.SeasonRecord
import com.supermenajer.game.model.Team
import kotlin.math.roundToLong
import kotlin.random.Random

object Season {
    const val LEAGUE_WEEKS = 34
    private val cupAfterWeek = listOf(5, 11, 17, 24, 31)

    val trainingNames = listOf("Dengeli", "Hücum", "Savunma", "Fizik", "Gençler")
    val intensityNames = listOf("Hafif", "Normal", "Yoğun")

    fun buildCalendar(): MutableList<Day> {
        val c = mutableListOf<Day>()
        for (w in 1..LEAGUE_WEEKS) {
            c += Day(0, w)
            val r = cupAfterWeek.indexOf(w)
            if (r >= 0) c += Day(1, r)
            val e = Europe.afterWeek.indexOf(w)
            if (e >= 0) c += Day(2, e)
        }
        return c
    }

    private fun leagueDay(cal: List<Day>, week: Int): Int = cal.indexOfFirst { it.type == 0 && it.number == week } + 1
    private fun cupDay(cal: List<Day>, round: Int): Int = cal.indexOfFirst { it.type == 1 && it.number == round } + 1

    /** Takımı olan ligler (eski ülkesiz Avrupa kulüpleri hariç). */
    fun activeLeagues(g: Game): List<Int> = Leagues.all.map { it.id }.filter { id -> g.teams.count { it.division == id } >= 2 }

    /** Ligi olan ülkeler. */
    fun activeCountries(g: Game): List<String> = activeLeagues(g).mapNotNull { Leagues.of(it)?.country }.distinct()

    fun makeLeagueFixtures(teamIds: List<Int>, comp: Int, cal: List<Day>, rnd: Random): List<Fixture> {
        val ids = teamIds.shuffled(rnd).toMutableList()
        val n = ids.size
        val rounds = n - 1
        val pairs = mutableListOf<Triple<Int, Int, Int>>()
        for (r in 0 until rounds) {
            for (i in 0 until n / 2) {
                val a = ids[i]
                val b = ids[n - 1 - i]
                val swap = if (i == 0) r % 2 == 1 else i % 2 == 1
                pairs += if (swap) Triple(r + 1, b, a) else Triple(r + 1, a, b)
            }
            ids.add(1, ids.removeAt(n - 1))
        }
        val all = pairs + pairs.map { Triple(it.first + rounds, it.third, it.second) }
        return all.map { (w, h, a) -> Fixture(leagueDay(cal, w), h, a, comp = comp) }
    }

    fun start(g: Game, rnd: Random = Random.Default) {
        g.reindex()
        g.teams.forEach { it.resetTable() }
        g.calendar = buildCalendar()
        g.fixtures = activeLeagues(g).flatMap { id ->
            makeLeagueFixtures(g.divisionTeams(id).map { it.id }, id, g.calendar, rnd)
        }.toMutableList()
        g.week = 1
        g.date = GameCalendar.seasonStart(g)
        g.campUses = 0
        g.boardAsked.clear()
        g.campUntil = 0
        g.friendlies = 0
        g.secretWeek = -1
        g.seasonOver = false
        g.cupWinner = -1
        g.cupWinners = mutableMapOf()
        g.press = null
        Facilities.ensureAll(g)
        Roles.ensureAllNumbers(g)
        for (t in g.teams) { t.attTotal = 0; t.attGames = 0 }
        Economy.startSeason(g)
        Sponsors.refreshOffers(g, rnd)
        g.offers.clear()
        computeExpectations(g)
        g.players.forEach { it.startOvr = it.overall }
        for (t in g.teams) {
            if (t.id == g.userTeamId && t.lineup.size == 11) Lineups.validate(g, t) else Lineups.autoPick(g, t)
            if (t.id != g.userTeamId) aiTactics(g, t, rnd)
        }
        for (country in activeCountries(g)) startCup(g, country, rnd)
        Europe.start(g, rnd)
    }

    /** Yapay zekâ takımlarının oyun tarzı: büyükler topa sahip olur, küçükler kontra oynar. */
    fun aiTactics(g: Game, t: Team, rnd: Random) {
        val n = g.divisionTeams(t.division).size.coerceAtLeast(1)
        val rank = t.expected.toDouble() / n
        when {
            rank <= 0.3 -> {
                t.mentality = if (rnd.nextDouble() < 0.6) 2 else 1
                t.passing = 0; t.defLine = 2; t.width = rnd.nextInt(1, 3); t.pressing = 2
                t.tempo = rnd.nextInt(1, 3); t.counter = false; t.offsideTrap = rnd.nextDouble() < 0.4
            }
            rank >= 0.7 -> {
                t.mentality = if (rnd.nextDouble() < 0.6) 0 else 1
                t.passing = if (rnd.nextDouble() < 0.6) 2 else 1; t.defLine = 0; t.width = rnd.nextInt(0, 2)
                t.pressing = rnd.nextInt(0, 2); t.tempo = rnd.nextInt(1, 3); t.counter = true; t.offsideTrap = false
            }
            else -> {
                t.mentality = 1; t.passing = rnd.nextInt(0, 3); t.defLine = 1; t.width = rnd.nextInt(0, 3)
                t.pressing = 1; t.tempo = 1; t.counter = rnd.nextDouble() < 0.3; t.offsideTrap = rnd.nextDouble() < 0.2
            }
        }
        t.timeWasting = rnd.nextDouble() < 0.3
    }

    /** Ülke kupası: 2 ligli ülkelerde 32 takım (son 32), tek ligli ülkelerde 16 takım (son 16). */
    private fun startCup(g: Game, country: String, rnd: Random) {
        val top = Leagues.top(country)
        val second = Leagues.second(country)
        val first = g.divisionTeams(top.id)
        val lower = second?.let { g.divisionTeams(it.id) } ?: emptyList()
        if (lower.isNotEmpty()) {
            drawCup(g, top.cupComp, 0, (first + lower.sortedByDescending { it.reputation }).take(32), rnd)
        } else {
            drawCup(g, top.cupComp, 1, first.sortedByDescending { it.reputation }.take(16), rnd)
        }
    }

    /** Eski kayıtları yeni yapıya taşır. */
    fun migrate(g: Game, rnd: Random = Random.Default) {
        if (g.saveVersion < 2) migrateV2(g, rnd)
        if (g.saveVersion < 3) g.saveVersion = 3
        if (g.saveVersion < 4) {
            for (p in g.players) {
                if (p.teamId >= 0 && p.contractYears <= 0) {
                    p.contractYears = rnd.nextInt(1, 5)
                    p.salary = p.demandWage
                }
            }
            for (t in g.teams) {
                // Geçiş sezonunda kimse kilitlenmesin: limit en az mevcut maaşlar kadar.
                t.wageBudget = maxOf(Economy.wageCap(t), Economy.squadWages(g, t.id))
            }
            g.saveVersion = 4
        }
        if (g.saveVersion < 5) {
            if (g.cupWinner >= 0) g.cupWinners[Comp.CUP] = g.cupWinner
            if (g.teams.none { it.division >= 10 }) g.worldUpgradePending = true
            g.saveVersion = 5
            g.addNews(
                "Güncelleme: v0.5",
                "Dünya büyüyor! İngiltere, İspanya, Almanya, İtalya, Fransa, Portekiz ve Hollanda ligleri yeni sezonla birlikte oyuna eklenecek. Ayrıca: devre arası konuşması, basın toplantıları ve 'Sonraki maça git'.",
            )
        }
        if (g.saveVersion < 6) migrateV6(g)
        if (g.saveVersion < 7) migrateV7(g)
        if (g.saveVersion < 8) migrateV8(g)
        if (g.saveVersion < 9) migrateV9(g)
        if (g.saveVersion < 11) {
            // Eski haberler okunmuş sayılır; kategoriler başlıktan çıkarılır
            g.news.forEachIndexed { i, n -> n.read = i >= 3 }
        }
        if (g.saveVersion < 10) {
            g.saveVersion = 10
            g.addNews(
                "Güncelleme: v1.0 · Taraftar ve yatay ekran",
                "Oyun artık yatay ekranda! Ana sayfadaki Taraftar kartından taraftar memnuniyetini takip et: galibiyetler, derbiler, yıldız transferleri ve kupalar taraftarı coşturur; yıldız satışı, ağır yenilgiler ve pahalı bilet kızdırır. Coşkulu taraftar evde 12. adam olur!",
            )
        }
        if (g.saveVersion < 11) {
            g.saveVersion = 11
            g.addNews(
                "Güncelleme: v1.1 · Gelen kutusu ve yeni ana sayfa",
                "Haberler artık Gelen Kutusu'nda; okunmamışlar kırmızı noktayla görünür. Üstteki DEVAM düğmesiyle oyunu ilerlet, ⚙️ ile ayarlara ulaş. Teknik ekip, tesisler, sponsorlar, finans ve taraftar yeni Kulüp menüsünde. Yardımcı antrenörün de birkaç haftada bir rapor gönderecek.",
            )
        }
        if (g.saveVersion < 12) {
            g.saveVersion = 12
            if (g.date == 0L) g.date = GameCalendar.dateOf(g, g.week)
            g.addNews(
                "Güncelleme: v1.2 · Büyük güncelleme",
                "Takvim artık gün gün ilerliyor; DEVAM önemli bir olayda durur. Taktikte sürükle-bırak ve yeni takım talimatları, oyuncu yüzleri ve kartları, diyaloglu transfer pazarlığı, bütçe kaydırma ve 3 kayıt yuvası geldi!",
            )
        }
        if (g.saveVersion < 13) {
            g.saveVersion = 13
            Roles.ensureAllNumbers(g)
            g.addNews(
                "Güncelleme: v1.3 · Menajerlik derinleşiyor",
                "Oyuncu kişilikleri, FM tarzı roller, kaptan ve duran top kullanıcıları, forma numaraları, maçta kenar çizgisi talimatları, antrenman kampı, gizli antrenman, galibiyet primi, hazırlık maçları, yönetimden istekler, başarımlar, kupa dolabı ve kulüp rekorları geldi!",
            )
        }
    }

    private fun migrateV9(g: Game) {
        g.saveVersion = 9
        g.addNews(
            "Güncelleme: v0.9 · Sponsorluk anlaşmaları",
            "Forma sponsoru, stadyum isim hakkı ve malzeme sponsoru geldi! Kulüp → Sponsorlar'dan garantili, dengeli ya da performans primli teklifleri seç.",
        )
        if (g.sponsors.isEmpty() && !g.fired && g.userTeamId >= 0) Sponsors.refreshOffers(g)
    }

    private fun migrateV8(g: Game) {
        g.saveVersion = 8
        Facilities.ensureAll(g)
        g.addNews(
            "Güncelleme: v0.8 · Stadyum ve tesisler",
            "Kulüp → Tesisler'den stadyumunu büyütebilir, bilet fiyatını belirleyebilir; antrenman tesisi, altyapı akademisi ve sağlık merkezini geliştirebilirsin. Seyirci sayısı artık formuna ve maçın önemine göre değişiyor!",
        )
    }

    private fun migrateV7(g: Game) {
        g.saveVersion = 7
        if (g.staff.isEmpty() && g.userTeamId >= 0) StaffLogic.hireDefaults(g)
        g.addNews(
            "Güncelleme: v0.7 · Teknik ekip ve takım konuşmaları",
            "Artık bir teknik ekibin var: yardımcı antrenör, antrenör, fizyoterapist ve gözlemci. Kulüp → Teknik Ekip'ten değiştirebilirsin. Maç öncesi, devre arası ve maç sonu takım konuşmaları da geldi!",
        )
    }

    private fun migrateV6(g: Game) {
        g.saveVersion = 6
        g.addNews(
            "Güncelleme: v0.6 · Championship Manager ruhu",
            "Gerilimli maç anlatımı, gizli özellikler ve gözlemci raporları, wonderkid avı, oyuncu mutluluğu ve transfer talepleri, kiralık gönderme, rakip raporu, sezon ödülleri ve kariyer teklifleri geldi!",
        )
    }

    private fun migrateV2(g: Game, rnd: Random) {
        if (g.calendar.isEmpty()) g.calendar = MutableList(LEAGUE_WEEKS) { Day(0, it + 1) }
        if (g.teams.none { it.division == 1 }) {
            Generator.addLowerDivision(g, rnd)
            g.fixtures.addAll(makeLeagueFixtures(g.divisionTeams(1).map { it.id }, Comp.FIRST, g.calendar, rnd))
            for (t in g.divisionTeams(1)) Lineups.autoPick(g, t)
            // Geçmiş haftaları 1. Lig için hızlıca oynat.
            for (d in 1 until g.week.coerceAtMost(g.calendar.size + 1)) {
                g.weekFixtures(d).filter { !it.played }.forEach {
                    MatchSim(g, it, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame() }
                }
            }
            computeExpectations(g)
        }
        g.players.forEach { if (it.startOvr == 0) it.startOvr = it.overall }
        g.saveVersion = 2
    }

    private fun drawCup(g: Game, comp: Int, round: Int, teams: List<Team>, rnd: Random) {
        val day = cupDay(g.calendar, round)
        if (day <= 0) return
        val list = teams.shuffled(rnd)
        for (i in 0 until list.size / 2) {
            val a = list[2 * i]
            val b = list[2 * i + 1]
            // Küçük takım ev sahibi olur.
            val (h, aw) = if (a.reputation <= b.reputation) a to b else b to a
            g.fixtures += Fixture(day, h.id, aw.id, comp = comp, round = round)
        }
        val us = g.userTeamId
        g.fixtures.firstOrNull { it.comp == comp && it.round == round && it.involves(us) }?.let {
            val opp = g.team(if (it.home == us) it.away else it.home)
            g.addNews("Kupa kurası", "${Comp.name(comp)} ${Comp.cupRounds[round]} turunda rakibimiz: ${opp.name}.")
        }
    }

    fun computeExpectations(g: Game) {
        for (id in activeLeagues(g)) {
            g.divisionTeams(id).sortedByDescending { Lineups.strength(g, it) }.forEachIndexed { i, t -> t.expected = i + 1 }
        }
    }

    fun objectiveTarget(expected: Int): Int = when {
        expected <= 1 -> 1
        expected <= 3 -> 3
        expected <= 6 -> 6
        expected <= 10 -> 10
        else -> 15
    }

    fun objectiveText(t: Team): String {
        val lg = Leagues.of(t.division)
        val tier = lg?.tier ?: 1
        val hasLower = lg != null && Leagues.second(lg.country) != null
        val topName = lg?.let { Leagues.top(it.country).name } ?: "üst lig"
        return when (objectiveTarget(t.expected)) {
            1 -> if (tier == 1) "Şampiyonluk" else "Şampiyon olarak yükselmek"
            3 -> if (tier == 1) "İlk 3" else "$topName'e yükselmek (ilk 3)"
            6 -> "İlk 6"
            10 -> "İlk 10"
            else -> if (tier == 1 && hasLower) "Ligde kalmak" else "Orta sıralar (ilk 15)"
        }
    }

    fun transferWindowOpen(g: Game): Boolean {
        if (g.seasonOver) return true
        val w = g.leagueWeekAt(g.week)
        return w < 4 || w in 16..18
    }

    fun windowText(g: Game): String = when {
        transferWindowOpen(g) -> "Transfer dönemi AÇIK" + if (!g.seasonOver) " (${if (g.leagueWeekAt(g.week) < 4) "4" else "19"}. haftaya kadar)" else ""
        g.leagueWeekAt(g.week) < 16 -> "Transfer dönemi kapalı · Ara transfer 17. haftada açılır"
        else -> "Transfer dönemi kapalı · Sezon sonunda açılır"
    }

    /** Günün kalan maçlarını hızlıca oynatır. */
    fun simulateRest(g: Game, day: Int, rnd: Random = Random.Default) {
        for (f in g.weekFixtures(day)) {
            if (f.played) continue
            MatchSim(g, f, g.userTeamId, autoUserSubs = true, rnd = rnd).apply { runToEnd(); applyToGame() }
        }
    }

    /** Kullanıcı maçından sonra haber, yönetim güveni ve basın toplantısı. */
    fun matchNews(g: Game, sim: MatchSim, rnd: Random = Random.Default) {
        val us = sim.userSide ?: return
        Matchday.afterMatch(g, us.goals > sim.other(us).goals || sim.fixture.winner == us.team.id)
        val them = sim.other(us)
        val h = sim.home
        val a = sim.away
        val score = "${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}" +
            if (sim.penHome >= 0) " (Penaltılar ${sim.penHome}-${sim.penAway})" else ""
        val won = if (sim.isCup) sim.cupWinner()?.id == us.team.id else us.goals > them.goals
        val euro = sim.fixture.comp == Comp.EURO
        if (sim.home === us) us.team.lastAttendance = sim.fixture.attendance
        Career.afterMatch(g, us.goals, them.goals, them.team.name, us.goals > them.goals, Talks.isDerby(g, sim.fixture), Comp.isLeague(sim.fixture.comp))
        val title = when {
            euro && sim.isCup && won -> "Avrupa'da tur atladık!"
            euro && sim.isCup -> "Avrupa'ya veda ettik"
            euro && us.goals > them.goals -> "Avrupa'da galibiyet!"
            euro && us.goals == them.goals -> "Avrupa'da beraberlik"
            euro -> "Avrupa'da mağlubiyet"
            sim.isCup && won -> "Kupada tur atladık!"
            sim.isCup -> "Kupaya veda ettik"
            us.goals - them.goals >= 3 -> "Farklı galibiyet!"
            us.goals > them.goals -> "Galibiyet!"
            us.goals == them.goals -> "Beraberlik"
            else -> "Mağlubiyet"
        }
        val scorers = us.used.filter { it.goals > 0 }.joinToString { if (it.goals > 1) "${it.p.name} (${it.goals})" else it.p.name }
        val motm = sim.manOfTheMatch()
        val body = buildString {
            append(score)
            if (scorers.isNotEmpty()) append(". Gollerimiz: $scorers")
            motm?.let { append(". Maçın oyuncusu: ${it.p.name} (${"%.1f".format(it.rating)})") }
        }
        g.addNews(title, body)
        for (m in us.used) {
            if (m.injured) g.addNews("Sakatlık", "${m.p.name} sakatlandı ve yaklaşık ${m.injWeeks - 1} hafta forma giyemeyecek.")
            if (m.red) g.addNews("Kırmızı kart", "${m.p.name} kırmızı kart nedeniyle cezalı duruma düştü.")
        }
        // Yönetim güveni
        val t = us.team
        var delta = if (euro) {
            if (sim.isCup) (if (won) 4.0 else 0.0) else if (us.goals > them.goals) 2.0 else if (us.goals < them.goals) -0.5 else 0.5
        } else if (sim.isCup) {
            if (won) 2.0 else if (t.expected <= 3 && sim.fixture.round <= 1) -6.0 else -1.0
        } else when {
            us.goals > them.goals -> 3.0
            us.goals == them.goals -> 0.0
            else -> -3.5
        }
        if (!sim.isCup && !euro && g.leagueWeekAt(g.week) >= 6) {
            val pos = g.position(t.id)
            val target = objectiveTarget(t.expected)
            delta += when {
                pos <= target -> 1.0
                pos <= target + 3 -> -0.5
                else -> -2.0
            }
        }
        g.confidence = (g.confidence + delta).coerceIn(0.0, 100.0)
        if (rnd.nextDouble() < 0.35) g.press = Press.question(us.goals - them.goals, them.team.name, euro, sim.isCup, rnd)
    }

    fun endDay(g: Game, rnd: Random = Random.Default) {
        g.reindex()
        val d = g.today ?: return
        val dayFixtures = g.weekFixtures(g.week)
        for (f in dayFixtures) {
            val h = g.team(f.home)
            if (f.attendance == 0) f.attendance = Facilities.attendance(g, f, rnd)
            Facilities.record(g, f)
            Economy.earn(h, Economy.TICKETS, Facilities.ticketIncome(g, f))
            if (f.comp == Comp.EURO) Economy.earn(h, Economy.EURO, 400_000L)
            Sponsors.matchResult(g, f)
            Fans.afterMatch(g, f)
            if (Comp.isCup(f.comp) && f.winner >= 0) {
                val prize = listOf(150_000L, 250_000L, 400_000L, 700_000L, 1_500_000L)[f.round]
                Economy.earn(g.team(f.winner), Economy.CUP, prize)
            }
        }
        when (d.type) {
            2 -> Europe.advance(g, d.number, rnd)
            0 -> {
                weeklyUpdate(g, rnd)
                Facilities.weekly(g)
                Sponsors.weekly(g)
                Fans.weekly(g, rnd)
                Assistant.weekly(g, d.number, rnd)
                if (d.number % 6 == 0) {
                    val leader = g.table(g.userTeam.division).first()
                    g.addNews("Lig lideri", "${d.number}. hafta sonunda ${leader.name} ${leader.points} puanla zirvede.")
                }
            }
            else -> {
                val comps = dayFixtures.filter { Comp.isCup(it.comp) }.map { it.comp }.distinct()
                for (c in comps) advanceCup(g, c, d.number, rnd)
            }
        }
        for (p in g.players) p.fitness = (p.fitness + recovery(g, p)).coerceAtMost(100.0)
        Scouting.processDay(g, d.type == 0, rnd)
        if (transferWindowOpen(g)) {
            aiTransfers(g, rnd)
            makeIncomingOffer(g, rnd)
        }
        g.offers.removeAll { g.week - it.day >= 4 || g.player(it.playerId)?.teamId != g.userTeamId || g.teamOrNull(it.teamId) == null }
        if (transferWindowOpen(g)) Roles.ensureAllNumbers(g)
        g.date = maxOf(g.date, GameCalendar.dateOf(g, g.week) + 1)
        g.week++
        if (g.week > g.totalDays) seasonEnd(g)
        if (!g.fired && g.confidence <= 5 && (g.seasonOver || g.leagueWeekAt(g.week) >= 8)) fire(g, rnd)
    }

    private fun recovery(g: Game, p: Player): Double {
        if (p.teamId < 0) return 30.0
        return when (g.teamOrNull(p.teamId)?.intensity) { 0 -> 28.0; 2 -> 16.0; else -> 22.0 }
    }

    private fun weeklyUpdate(g: Game, rnd: Random) {
        happiness(g)
        val coachF = StaffLogic.coachFactor(g) * (if (Matchday.campActive(g)) 1.4 else 1.0)
        val physioRisk = StaffLogic.physioRisk(g)
        val physioHeal = StaffLogic.physioHeal(g)
        for (p in g.players) {
            val mine = p.teamId == g.userTeamId
            p.morale += (70 - p.morale) * 0.08
            if (p.injuredWeeks > 0) p.injuredWeeks--
            // İyi fizyoterapist iyileşmeyi hızlandırır
            if (mine && p.injuredWeeks > 0 && rnd.nextDouble() < physioHeal) p.injuredWeeks--
            if (p.suspended > 0) p.suspended--
            val team = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
            // Sağlık merkezi tüm kulüplerde iyileşmeyi hızlandırır
            if (p.injuredWeeks > 0 && rnd.nextDouble() < Facilities.medicalHeal(team)) p.injuredWeeks--
            val focus = team?.training ?: 0
            val intensity = team?.intensity ?: 1
            val intF = when (intensity) { 0 -> 0.7; 2 -> 1.4; else -> 1.0 }
            val youthF = (if (focus == 4) (if (p.age <= 21) 1.8 else 0.7) else 1.0) * (if (p.loanFrom >= 0) 1.5 else 1.0) *
                (if (mine) coachF else 1.0) * Facilities.trainingFactor(team) * Traits.devFactor(p)
            val gap = p.potential - p.overall
            if (p.age <= 24 && gap > 0 && rnd.nextDouble() < (0.14 + gap * 0.01) * intF * youthF) {
                improve(p, 1, rnd, focus)
            } else if (p.age in 25..29 && gap > 0 && rnd.nextDouble() < 0.05 * intF * Traits.devFactor(p)) {
                improve(p, 1, rnd, focus)
            } else if (p.age >= 31 && rnd.nextDouble() < (0.04 + (p.age - 31) * 0.02) / intF.coerceAtMost(1.0)) {
                decline(p, 1, rnd)
            }
            // Antrenman sakatlığı
            if (team != null && p.injuredWeeks == 0) {
                val risk = when (intensity) { 0 -> 0.002; 2 -> 0.012; else -> 0.005 } * (if (mine) physioRisk else 1.0) * Facilities.medicalRisk(team) * Traits.injuryFactor(p)
                if (rnd.nextDouble() < risk) {
                    p.injuredWeeks = rnd.nextInt(1, 4)
                    if (team.id == g.userTeamId) g.addNews("Antrenmanda sakatlık", "${p.name} antrenmanda sakatlandı (${p.injuredWeeks} hafta).")
                }
            }
        }
    }

    /** Kullanıcının kadrosunda forma süresi ve moral: yedekte kalan önemli oyuncular mutsuz olur. */
    private fun happiness(g: Game) {
        if (g.fired) return
        val us = g.userTeam
        val str = Lineups.strength(g, us)
        for (p in g.squad(us.id)) {
            val starting = p.id in us.lineup
            if (p.promised) {
                p.promised = false
                if (!starting) {
                    p.morale = (p.morale - 12).coerceAtLeast(5.0)
                    g.addNews("Söz tutulmadı", "${p.name} verdiğin forma sözünü tutmadığın için çok kırgın.")
                }
            }
            if (!p.available) continue
            when {
                starting -> p.morale = (p.morale + 1.5).coerceAtMost(100.0)
                p.overall >= str - 2 && p.age >= 21 -> p.morale = (p.morale - 5 * Traits.moraleSensitivity(p)).coerceAtLeast(5.0)
            }
            // Sözleşmede yıldız / ilk 11 rolü verilen oyuncu yedekte kalırsa kırılır
            if (!starting && p.squadRole in 0..1) p.morale = (p.morale - 2.5 * Traits.moraleSensitivity(p)).coerceAtLeast(5.0)
            if (!p.wantsAway && p.morale < 25 && p.overall >= str - 4) {
                p.wantsAway = true
                g.addNews("Transfer talebi!", "${p.name} forma bulamamaktan mutsuz ve kulüpten ayrılmak istiyor. Onunla konuşabilir, oynatabilir ya da satabilirsin.")
            } else if (p.wantsAway && p.morale > 45) {
                p.wantsAway = false
                g.addNews("Mutlu son", "${p.name} transfer talebini geri çekti.")
            }
        }
    }

    /** Kiralık oyuncular asıl kulüplerine döner. */
    private fun returnLoans(g: Game) {
        for (p in g.players.toList()) {
            if (p.loanFrom < 0) continue
            val home = g.teamOrNull(p.loanFrom)
            val club = g.teamOrNull(p.teamId)
            club?.lineup?.replaceAll { if (it == p.id) -1 else it }
            if (home != null) {
                if (home.id == g.userTeamId) g.addNews("Kiralıktan dönüş", "${p.name} kiralık dönemini tamamlayıp döndü (${p.apps} maç, ${p.goals} gol). Güç: ${p.startOvr} → ${p.overall}.")
                g.movePlayer(p, home.id)
            }
            p.loanFrom = -1
        }
    }

    private fun advanceCup(g: Game, comp: Int, round: Int, rnd: Random) {
        val winners = g.fixtures.filter { it.comp == comp && it.round == round }.mapNotNull { f -> f.winner.takeIf { it >= 0 } }
        if (round == Comp.cupRounds.lastIndex) {
            val w = winners.firstOrNull() ?: return
            g.cupWinners[comp] = w
            if (comp == Comp.CUP) g.cupWinner = w
            val t = g.team(w)
            Economy.earn(t, Economy.CUP, 3_000_000L)
            t.reputation = (t.reputation + 2).coerceAtMost(95)
            if (Leagues.cupCountry(comp) == g.userCountry) g.addNews("${Comp.name(comp)} sahibini buldu!", "${t.name} kupayı müzesine götürdü! 🏆")
            if (w == g.userTeamId) {
                g.confidence = (g.confidence + 15).coerceAtMost(100.0)
                g.addNews("KUPA BİZİM!", "Tebrikler hocam! ${Comp.name(comp)}'nı kazandık! Taraftar sokaklarda!")
            }
        } else {
            drawCup(g, comp, round + 1, winners.map { g.team(it) }, rnd)
        }
    }

    fun improve(p: Player, n: Int, rnd: Random, focus: Int = 0) {
        repeat(n) {
            if (p.pos == Pos.GK) {
                if (focus == 3 && rnd.nextInt(3) == 0) p.physical++ else p.goalkeeping++
                return@repeat
            }
            when (focus) {
                1 -> when (rnd.nextInt(3)) { 0 -> p.shooting++; 1 -> p.dribbling++; else -> p.passing++ }
                2 -> when (rnd.nextInt(3)) { 0 -> p.defending++; 1 -> p.defending++; else -> p.physical++ }
                3 -> when (rnd.nextInt(2)) { 0 -> p.pace++; else -> p.physical++ }
                else -> when (p.pos) {
                    Pos.DF -> when (rnd.nextInt(4)) { 0 -> p.defending++; 1 -> p.physical++; 2 -> p.pace++; else -> p.passing++ }
                    Pos.MF -> when (rnd.nextInt(5)) { 0 -> p.passing++; 1 -> p.dribbling++; 2 -> p.defending++; 3 -> p.shooting++; else -> p.physical++ }
                    else -> when (rnd.nextInt(4)) { 0 -> p.shooting++; 1 -> p.dribbling++; 2 -> p.pace++; else -> p.physical++ }
                }
            }
        }
        clamp(p)
    }

    fun decline(p: Player, n: Int, rnd: Random) {
        repeat(n) {
            if (p.pos == Pos.GK) {
                if (rnd.nextBoolean()) p.goalkeeping-- else p.physical--
            } else when (rnd.nextInt(3)) {
                0 -> p.pace--
                1 -> p.physical--
                else -> when (p.pos) { Pos.DF -> p.defending--; Pos.MF -> p.passing--; else -> p.shooting-- }
            }
        }
        clamp(p)
    }

    private fun clamp(p: Player) {
        p.pace = p.pace.coerceIn(10, 99); p.shooting = p.shooting.coerceIn(10, 99)
        p.passing = p.passing.coerceIn(10, 99); p.dribbling = p.dribbling.coerceIn(10, 99)
        p.defending = p.defending.coerceIn(10, 99); p.physical = p.physical.coerceIn(10, 99)
        p.goalkeeping = p.goalkeeping.coerceIn(5, 99)
    }

    private fun r25k(v: Double): Long = (v / 25_000).roundToLong() * 25_000

    /** Yapay zekâ kulüplerinin kendi aralarındaki transferleri. */
    private fun aiTransfers(g: Game, rnd: Random) {
        val buyers = g.teams.filter { it.id != g.userTeamId && it.budget > 2_000_000 && it.division != Leagues.LEGACY_EURO }
        if (buyers.isEmpty()) return
        val freeAgents = g.squad(-1)
        repeat(8) {
            if (rnd.nextDouble() > 0.5) return@repeat
            val buyer = buyers[rnd.nextInt(buyers.size)]
            val squad = g.squad(buyer.id)
            if (squad.size >= 28) return@repeat
            // En zayıf mevkiyi bul
            val need = mapOf(Pos.GK to 1, Pos.DF to 4, Pos.MF to 4, Pos.FW to 2)
            val weakest = need.minBy { (pos, c) ->
                squad.filter { it.pos == pos }.sortedByDescending { it.overall }.take(c).minOfOrNull { it.overall } ?: 0
            }.key
            val bar = squad.filter { it.pos == weakest }.sortedByDescending { it.overall }.take(need.getValue(weakest)).minOfOrNull { it.overall } ?: 0
            val maxPrice = buyer.budget * 0.6
            // Aday havuzu: serbestler + aynı ülkeden ya da daha küçük kulüplerden oyuncular.
            val candidates = (freeAgents + g.players.filter { it.teamId >= 0 && it.pos == weakest && it.overall >= bar + 2 })
                .asSequence()
                .filter { p ->
                    p.pos == weakest && p.teamId != buyer.id && p.teamId != g.userTeamId && p.loanFrom < 0 && p.overall >= bar + 2 && p.age <= 31 &&
                        (p.teamId < 0 || g.teamOrNull(p.teamId)?.let { s -> s.reputation <= buyer.reputation + 4 && s.division != Leagues.LEGACY_EURO } == true) &&
                        p.value * (if (p.teamId < 0) 0.15 else 1.15) <= maxPrice &&
                        Economy.fitsWages(g, buyer, p.demandWage)
                }
                .sortedByDescending { it.overall }.take(5).toList()
            val p = candidates.randomOrNull(rnd) ?: return@repeat
            if (p.teamId >= 0 && g.squad(p.teamId).size <= 19) return@repeat
            val price = r25k(p.value * if (p.teamId < 0) 0.15 else rnd.nextDouble(1.0, 1.3))
            val from = if (p.teamId >= 0) g.team(p.teamId) else null
            from?.let { Economy.earn(it, Economy.SALES, price); it.lineup.replaceAll { id -> if (id == p.id) -1 else id } }
            Economy.earn(buyer, Economy.BUYS, -price)
            g.movePlayer(p, buyer.id)
            p.salary = p.demandWage
            p.contractYears = rnd.nextInt(2, 6)
            val local = g.countryOf(buyer) == g.userCountry || (from != null && g.countryOf(from) == g.userCountry)
            if ((local && (p.overall >= 66 || price >= 1_000_000)) || price >= 15_000_000) {
                g.addNews("Transfer", "${p.name} (${p.overall}) → ${buyer.name}" + (from?.let { " · ${it.name} kasasına ${money(price)} girdi" } ?: " · serbest oyuncu olarak imzaladı"))
            }
        }
    }

    /** Kullanıcının oyuncularına gelen teklifler. */
    /** Maç olmayan gün: transfer döneminde kulüpler hareketli olur. */
    fun freeDay(g: Game, rnd: Random = Random.Default) {
        if (g.fired) return
        if (Career.isDeadlineDay(g)) {
            g.addNews("📺 Transfer döneminin son günü!", "Kulüpler son dakika transferleri için kolları sıvadı. Gece yarısı pencere kapanıyor!")
            repeat(3) { aiTransfers(g, rnd) }
            repeat(2) { makeIncomingOffer(g, rnd) }
        }
        if (transferWindowOpen(g)) {
            if (rnd.nextDouble() < 0.35) aiTransfers(g, rnd)
            if (rnd.nextDouble() < 0.4) makeIncomingOffer(g, rnd)
        }
        for (p in g.players) if (p.teamId >= 0) p.fitness = (p.fitness + 4).coerceAtMost(100.0)
        Scouting.processDay(g, false, rnd)
        if (transferWindowOpen(g)) Roles.ensureNumbers(g, g.userTeam)
    }

    private fun makeIncomingOffer(g: Game, rnd: Random) {
        val us = g.userTeam
        val listed = g.squad(us.id).filter { it.listed && it.loanFrom < 0 && g.offers.none { o -> o.playerId == it.id } }
        if (g.offers.size >= 3 || rnd.nextDouble() > (if (listed.isNotEmpty()) 0.5 else 0.25)) return
        val sq = g.squad(us.id).filter { it.age >= 19 && it.loanFrom < 0 && g.offers.none { o -> o.playerId == it.id } }.sortedByDescending { it.overall }.take(16)
        val unhappy = sq.filter { it.wantsAway }
        val p = when {
            listed.isNotEmpty() && rnd.nextDouble() < 0.7 -> listed.random(rnd)
            unhappy.isNotEmpty() && rnd.nextDouble() < 0.6 -> unhappy.random(rnd)
            else -> sq.randomOrNull(rnd)
        } ?: return
        val price = r25k(p.value * if (p.listed) rnd.nextDouble(0.8, 1.15) else rnd.nextDouble(0.9, 1.35))
        val buyer = g.teams.filter {
            it.id != us.id && it.division != Leagues.LEGACY_EURO && it.budget >= price && it.reputation >= us.reputation - 12 &&
                g.squad(it.id).size < 28 && Economy.fitsWages(g, it, p.demandWage)
        }.randomOrNull(rnd) ?: return
        g.offers += IncomingOffer(p.id, buyer.id, price, g.week)
        g.addNews("Transfer teklifi!", "${buyer.name} (${g.countryOf(buyer)}), ${p.name} için ${money(price)} teklif etti. Ana sayfadan yanıt verebilirsin.")
    }

    private fun fire(g: Game, rnd: Random) {
        g.fired = true
        g.careerOffer = false
        val cur = g.userTeam
        val pool = g.teams.filter { it.division != Leagues.LEGACY_EURO && it.id != cur.id }
        g.jobOffers = pool.filter { it.reputation in (cur.reputation - 20)..(cur.reputation - 3) }
            .shuffled(rnd).take(4).map { it.id }.toMutableList()
        if (g.jobOffers.isEmpty()) g.jobOffers = pool.sortedBy { it.reputation }.take(4).map { it.id }.toMutableList()
        g.addNews("KOVULDUN!", "${cur.name} yönetimi kötü sonuçlar nedeniyle yollarını seninle ayırdı. Masada yeni teklifler var.")
    }

    fun takeJob(g: Game, teamId: Int) {
        val old = g.userTeam.name
        Sponsors.leaveClub(g, g.userTeam)
        g.userTeamId = teamId
        g.fired = false
        g.careerOffer = false
        g.jobOffers.clear()
        g.offers.clear()
        g.press = null
        g.confidence = 55.0
        g.projects.clear()
        g.seasonsAtClub = 0
        Fans.reset(g)
        Facilities.ensure(g.userTeam)
        Sponsors.refreshOffers(g)
        Lineups.validate(g, g.userTeam)
        g.addNews("Yeni görev", "$old macerası bitti. Artık ${g.userTeam.name} teknik direktörüsün! Hedef: ${objectiveText(g.userTeam)}.")
    }

    private fun seasonEnd(g: Game) {
        g.seasonOver = true
        g.press = null
        val us = g.userTeam
        val budgetBefore = us.budget
        val cupComp0 = Leagues.of(us.division)?.cupComp ?: Comp.CUP
        val titles = (if (g.table(us.division).first().id == us.id) 1 else 0) +
            (if (g.cupWinners[cupComp0] == us.id) 1 else 0) + (if (g.euroWinner == us.id) 1 else 0)
        Sponsors.seasonTitles(g, titles)
        Economy.seasonPayout(g)
        Facilities.aiUpgrades(g, Random.Default)
        for (id in activeLeagues(g)) {
            g.table(id).forEachIndexed { i, t ->
                val champ = if (i == 0) 2 else 0
                t.reputation = (t.reputation + (t.expected - (i + 1)) / 2 + champ).coerceIn(30, 95)
            }
        }
        g.addNews(
            "Yeni sezon bütçesi",
            "Yönetim sezon sonu ödeneği olarak ${money(us.budget - budgetBefore)} ayırdı. Transfer bütçen: ${money(us.budget)}. Ayrıntılar ana sayfadaki mali raporda.",
        )
        val myLeague = g.table(us.division)
        val userPos = g.position(us.id)
        val leagueTeams = myLeague.map { it.id }.toSet()
        val top = g.players.filter { it.teamId in leagueTeams }.maxByOrNull { it.goals }
        val topText = top?.let { "${it.name} (${it.goals} gol)" } ?: "-"
        val country = g.userCountry
        val lower = Leagues.second(country)?.let { g.table(it.id) }
        val cupComp = Leagues.of(us.division)?.cupComp ?: Comp.CUP
        val awards = seasonAwards(g, us.division)
        g.history += SeasonRecord(
            g.season, myLeague.first().name, us.name, userPos, topText,
            cupWinner = g.cupWinners[cupComp]?.let { g.team(it).name } ?: "",
            firstChampion = lower?.firstOrNull()?.name ?: "", userDivision = us.division,
            euroWinner = if (g.euroWinner >= 0) g.team(g.euroWinner).name else "",
            leagueName = Leagues.of(us.division)?.name ?: "",
            awards = awards,
        )
        if (awards.isNotEmpty()) g.addNews("🏅 Sezonun ödülleri", awards)
        g.euroQualified = Leagues.topLeagues.flatMap { lg -> g.table(lg.id).take(4).map { it.id } }.toMutableList()
        if (us.id in g.euroQualified) g.addNews("AVRUPA'DAYIZ!", "${us.name} gelecek sezon Avrupa Kupası'nda mücadele edecek!")
        val topLeague = g.table(Leagues.top(country).id)
        val summary = buildString {
            append("${g.season} ${Leagues.top(country).name} şampiyonu ${topLeague.first().name}! Gol kralı: $topText.")
            if (lower != null) append(" Düşenler: ${topLeague.takeLast(3).joinToString { it.name }}. Yükselenler: ${lower.take(3).joinToString { it.name }}.")
        }
        g.addNews("Sezon sona erdi", summary)
        if (myLeague.first().id == us.id) g.addNews("ŞAMPİYONUZ!", "Tebrikler hocam! ${us.name} ${Leagues.of(us.division)?.name} şampiyonu!")
        val target = objectiveTarget(us.expected)
        g.confidence = (g.confidence + when {
            userPos <= target -> 25.0
            userPos <= target + 3 -> -10.0
            else -> -30.0
        }).coerceIn(0.0, 100.0)
        Fans.seasonEnd(g, titles, userPos <= target, userPos > target + 3)
        Career.seasonEnd(
            g, myLeague.first().id == us.id, g.cupWinners[cupComp] == us.id, g.euroWinner == us.id,
            userPos <= target, userPos > target + 3, Leagues.of(us.division)?.name ?: "Lig", Comp.name(cupComp),
        )
        careerOffers(g, userPos, target)
    }

    /** Lig ödülleri: yılın oyuncusu, gol kralı, genç oyuncu, teknik direktör. */
    private fun seasonAwards(g: Game, div: Int): String {
        val teams = g.divisionTeams(div)
        val players = teams.flatMap { g.squad(it.id) }
        val potY = players.filter { it.apps >= 20 }.maxByOrNull { it.avgRating }
        val young = players.filter { it.apps >= 15 && it.age <= 21 }.maxByOrNull { it.avgRating }
        val top = players.maxByOrNull { it.goals }
        val table = g.table(div)
        val coach = table.withIndex().maxByOrNull { (i, t) -> (t.expected - (i + 1)) * 10 + (if (i == 0) 15 else 0) }?.value
        val parts = mutableListOf<String>()
        potY?.let { parts += "Yılın oyuncusu: ${it.name} (${g.teamOrNull(it.teamId)?.short ?: "-"}, ${"%.2f".format(it.avgRating)})" }
        top?.let { parts += "Gol kralı: ${it.name} (${it.goals})" }
        young?.let { parts += "Yılın genç oyuncusu: ${it.name} (${it.age})" }
        coach?.let { parts += "Yılın teknik direktörü: " + if (it.id == g.userTeamId) "${g.managerName} (${it.name}) 🎉" else "${it.name} hocası" }
        return parts.joinToString(" · ")
    }

    /** Başarılı sezon sonrası daha büyük kulüplerden teklif. */
    private fun careerOffers(g: Game, pos: Int, target: Int) {
        if (g.fired || pos > target) return
        val us = g.userTeam
        val bigger = g.teams.filter { it.division != Leagues.LEGACY_EURO && it.reputation >= us.reputation + 4 && it.id != us.id }
        if (bigger.isEmpty() || Random.nextDouble() > 0.6) return
        g.jobOffers = bigger.shuffled().take(2).map { it.id }.toMutableList()
        g.careerOffer = true
        g.addNews("Kariyer teklifi!", "Başarın göz kamaştırdı: ${g.jobOffers.joinToString { g.team(it).name }} seni teknik direktör olarak istiyor. Ana sayfadan karar ver.")
    }

    fun newSeason(g: Game, rnd: Random = Random.Default) {
        val us = g.userTeam
        Sponsors.newSeason(g)
        // Küme düşme / yükselme (2 ligli ülkelerde)
        for (country in activeCountries(g)) {
            val top = Leagues.top(country)
            val second = Leagues.second(country) ?: continue
            val down = g.table(top.id).takeLast(3)
            val up = g.table(second.id).take(3)
            down.forEach { it.division = second.id; it.reputation = (it.reputation - 4).coerceAtLeast(30) }
            up.forEach { it.division = top.id; it.reputation = (it.reputation + 4).coerceAtMost(95) }
            if (us in down) Fans.change(g, -15.0, "Küme düşme")
            if (us in up) { Fans.change(g, 15.0, "Şampiyonluk yükselişi"); Career.unlock(g, "promotion") }
            if (us in down) g.addNews("Küme düştük", "${us.name} gelecek sezon ${second.name}'nde mücadele edecek. Hemen geri dönmeliyiz!")
            if (us in up) g.addNews("YÜKSELDİK!", "${us.name} artık ${top.name}'nde! Tarihi bir başarı hocam!")
        }
        returnLoans(g)
        Economy.processContracts(g, rnd)

        val retired = mutableListOf<Player>()
        for (p in g.players.toList()) {
            p.age++
            val gap = p.potential - p.overall
            when {
                p.age <= 23 && gap > 0 -> improve(p, (gap / 3).coerceIn(1, 6) + rnd.nextInt(0, 2), rnd)
                p.age <= 27 && gap > 0 -> improve(p, rnd.nextInt(0, 3), rnd)
                p.age >= 31 -> decline(p, rnd.nextInt(1, 2 + (p.age - 30)), rnd)
            }
            if (p.age >= 36 || (p.age >= 33 && rnd.nextDouble() < 0.25 + (p.age - 33) * 0.15)) {
                retired += p
                continue
            }
            p.apps = 0; p.goals = 0; p.assists = 0; p.ratingSum = 0.0
            p.yellowCards = 0; p.suspended = 0; p.fitness = 100.0
        }
        for (p in retired) if (p.teamId == g.userTeamId) g.addNews("Jübile", "${p.name} ${p.age} yaşında futbolu bıraktı.")
        g.removePlayers(retired)
        val retiredIds = retired.map { it.id }.toSet()
        g.teams.forEach { t -> t.lineup.replaceAll { if (it in retiredIds) -1 else it } }
        for (t in g.teams) {
            var sq = g.squad(t.id)
            while (sq.size < 22) {
                val pos = Generator.squadPlan.maxBy { (pos, n) -> n - sq.count { it.pos == pos } }.key
                val y = Generator.youth(g, t, pos, rnd)
                g.addPlayer(y)
                if (t.id == g.userTeamId) g.addNews("Altyapıdan yeni yetenek", "${y.name} (${y.age}, ${pos.label}) A takıma yükseldi. Potansiyeli: ${y.potential}.")
                sq = g.squad(t.id)
            }
        }
        Facilities.youthIntake(g, rnd)
        if (g.worldUpgradePending) WorldGen.upgradeWorld(g, rnd)
        Generator.topUpFreeAgents(g, rnd)
        g.season++
        start(g, rnd)
        g.addNews("Yeni sezon", "${g.season} sezonu başlıyor! Yönetimin hedefi: ${objectiveText(g.userTeam)}. Transfer dönemi açık.")
    }
}
