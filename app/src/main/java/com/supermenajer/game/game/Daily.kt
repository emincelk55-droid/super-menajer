package com.supermenajer.game.game

import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.Side
import com.supermenajer.game.model.Game
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Günlük giriş ödülü: gerçek takvime göre her gün bir ödül, 7 günlük seri.
 * Bir gün atlanırsa seri baştan başlar; 7. günden sonra döngü yeniden başlar (seri sayacı artmaya devam eder).
 */
object DailyReward {
    /** Bugünün tarihi (epoch günü). Testler sabit bir gün verebilir. */
    var today: () -> Long = { java.time.LocalDate.now().toEpochDay() }

    class Reward(val day: Int, val icon: String, val text: String)

    /** Para ödülleri kulübün büyüklüğüne göre ölçeklenir (alt ligde ekonomiyi bozmasın). */
    private fun scale(g: Game): Double = (g.userTeam.reputation / 70.0).coerceIn(0.35, 1.4)

    private fun cash(g: Game, base: Long): Long = ((base * scale(g)) / 25_000).roundToLong() * 25_000

    fun rewards(g: Game): List<Reward> = listOf(
        Reward(1, "💶", money(cash(g, 200_000))),
        Reward(2, "⭐", "+30 XP"),
        Reward(3, "💶", money(cash(g, 400_000))),
        Reward(4, "😊", L("Moral +5", "Morale +5")),
        Reward(5, "⭐", "+60 XP"),
        Reward(6, "💶", money(cash(g, 800_000))),
        Reward(7, "🎁", money(cash(g, 1_500_000)) + " + 100 XP"),
    )

    /** Bugünün ödülü alınabilir mi. */
    fun available(g: Game): Boolean = !g.fired && g.dailyLast < today()

    /** Ödül alınırsa serinin kaçıncı günü olacağı (1-7). */
    fun nextDay(g: Game): Int {
        val t = today()
        val streak = if (g.dailyLast == t - 1) g.dailyStreak + 1 else if (g.dailyLast == t) g.dailyStreak else 1
        return (streak - 1) % 7 + 1
    }

    /** Ödülü verir; açıklama döner (alınamıyorsa null). */
    fun claim(g: Game): String? {
        if (!available(g)) return null
        val t = today()
        g.dailyStreak = if (g.dailyLast == t - 1) g.dailyStreak + 1 else 1
        g.dailyLast = t
        g.dailyBest = maxOf(g.dailyBest, g.dailyStreak)
        val day = (g.dailyStreak - 1) % 7 + 1
        val us = g.userTeam
        when (day) {
            1 -> Economy.earn(us, Economy.BOARD, cash(g, 200_000))
            2 -> Perks.addXp(g, 30)
            3 -> Economy.earn(us, Economy.BOARD, cash(g, 400_000))
            4 -> g.squad(us.id).forEach { it.morale = (it.morale + 5).coerceAtMost(100.0) }
            5 -> Perks.addXp(g, 60)
            6 -> Economy.earn(us, Economy.BOARD, cash(g, 800_000))
            else -> { Economy.earn(us, Economy.BOARD, cash(g, 1_500_000)); Perks.addXp(g, 100) }
        }
        if (g.dailyStreak >= 7) Career.unlock(g, "daily7")
        if (g.dailyStreak >= 30) Career.unlock(g, "daily30")
        val r = rewards(g)[day - 1]
        return L("${day}. gün ödülü: ${r.icon} ${r.text}", "Day $day reward: ${r.icon} ${r.text}")
    }
}

/**
 * Maç görevleri: her maçtan önce 3 kısa görev. Maç bitince değerlendirilir, ödül verilir ve yenileri gelir.
 * Kayıt: "id|durum" (0 bekliyor, 1 başarıldı, 2 kaçtı).
 */
object Challenges {
    class C(val id: String, val icon: String, val title: String, val xp: Int, val check: (MatchSim, Side, Side) -> Boolean)

    private fun possPct(us: Side, them: Side): Int = us.poss * 100 / (us.poss + them.poss).coerceAtLeast(1)

    private val poolCache = LangCached { listOf(
        C("win", "🏆", L("Maçı kazan", "Win the match"), 25) { s, us, them -> us.goals > them.goals || s.fixture.winner == us.team.id },
        C("score2", "⚽", L("En az 2 gol at", "Score at least 2 goals"), 20) { _, us, _ -> us.goals >= 2 },
        C("clean", "🧤", L("Gol yeme", "Keep a clean sheet"), 25) { _, _, them -> them.goals == 0 },
        C("shots12", "🎯", L("En az 12 şut çek", "Take at least 12 shots"), 15) { _, us, _ -> us.shots >= 12 },
        C("poss55", "🔄", L("Topa %55+ sahip ol", "Have 55%+ possession"), 15) { _, us, them -> possPct(us, them) >= 55 },
        C("youth", "🌱", L("21 yaş altı bir oyuncu ilk 11'de başlasın", "Start an under-21 player"), 10) { _, us, _ -> us.used.any { !it.subbedOn && it.p.age <= 21 } },
        C("star", "🌟", L("Bir oyuncun 7.5+ not alsın", "Get a 7.5+ player rating"), 20) { _, us, _ -> us.used.any { it.rating >= 7.5 } },
        C("fair", "🟨", L("En fazla 1 sarı kart, kırmızı yok", "At most 1 yellow card, no reds"), 15) { _, us, _ -> us.yellows <= 1 && us.reds == 0 },
        C("scorers2", "👥", L("İki farklı oyuncu gol atsın", "Two different players score"), 20) { _, us, _ -> us.used.count { it.goals > 0 } >= 2 },
        C("ontarget5", "🥅", L("En az 5 isabetli şut", "At least 5 shots on target"), 15) { _, us, _ -> us.onTarget >= 5 },
        C("fewshots", "🛡️", L("Rakibe en fazla 8 şut izni ver", "Allow the opponent 8 shots or fewer"), 15) { _, _, them -> them.shots <= 8 },
    ) }
    val pool get() = poolCache.get()
    private val byId get() = pool.associateBy { it.id }

    class View(val c: C, val state: Int)

    fun list(g: Game): List<View> = g.challenges.mapNotNull { s ->
        val c = s.split("|")
        byId[c[0]]?.let { View(it, c.getOrNull(1)?.toIntOrNull() ?: 0) }
    }

    /** Yeni maç görevleri (en az biri sonuçla ilgili, en az biri oyun içi). */
    fun refresh(g: Game, rnd: Random = Random.Default) {
        val result = listOf("win", "score2", "clean").shuffled(rnd).first()
        val rest = pool.map { it.id }.filter { it != result && it !in listOf("win", "score2", "clean") }.shuffled(rnd).take(2)
        g.challenges = (listOf(result) + rest).map { "$it|0" }.toMutableList()
    }

    fun ensure(g: Game, rnd: Random = Random.Default) {
        if (g.challenges.isEmpty() || g.challenges.none { it.endsWith("|0") }) refresh(g, rnd)
    }

    /** Maç sonu: görevleri değerlendirir, ödülleri verir, sonucu döner ve yeni görevleri hazırlar. */
    fun afterMatch(g: Game, sim: MatchSim, us: Side, rnd: Random = Random.Default): List<View> {
        ensure(g, rnd)
        val them = sim.other(us)
        val done = list(g).map { v -> View(v.c, if (v.c.check(sim, us, them)) 1 else 2) }
        val ok = done.filter { it.state == 1 }
        var xp = ok.sumOf { it.c.xp }
        if (ok.size == 3) xp += 30
        if (xp > 0) Perks.addXp(g, xp)
        val cashPer = ((40_000 * (g.userTeam.reputation / 70.0).coerceIn(0.35, 1.4)) / 5_000).roundToLong() * 5_000
        if (ok.isNotEmpty()) Economy.earn(g.userTeam, Economy.BOARD, cashPer * ok.size)
        g.challengesDone += ok.size
        if (ok.size == 3) g.challengesPerfect++
        if (g.challengesDone >= 25) Career.unlock(g, "challenges25")
        if (g.challengesPerfect >= 5) Career.unlock(g, "perfect5")
        g.lastChallenges = done.map { "${it.c.id}|${it.state}" }.toMutableList()
        refresh(g, rnd)
        GameRepo.challengeResult = done
        return done
    }
}
