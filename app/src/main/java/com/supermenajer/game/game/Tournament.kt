package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import kotlin.random.Random

/**
 * Canlı yaz turnuvası (Avrupa Şampiyonası / Dünya Kupası): 16 ulus, 4 grup, ilk iki çıkar;
 * çeyrek final, yarı final, final. Kullanıcının maçları bekleyen milli maç olarak canlı oynanır,
 * diğer maçlar aynı turda arka planda sonuçlanır.
 */
object Tournament {
    val groupNames = listOf("A", "B", "C", "D")
    private val koNames = listOf("Çeyrek Final", "Yarı Final", "Final")
    /** Grup içi eşleşmeler (grubun 0-3 sıraları): her tur iki maç. */
    private val rounds = listOf(listOf(0 to 1, 2 to 3), listOf(0 to 2, 1 to 3), listOf(0 to 3, 1 to 2))

    fun title(g: Game) = if (g.season % 2 == 0) "Avrupa Şampiyonası" else "Dünya Kupası"

    /** Sezon bitti, milli takım var ve bu sezonun turnuvası oynanmadı. */
    fun needed(g: Game) = g.nationalTeam.isNotEmpty() && !g.fired && g.tourSeason != g.season

    fun active(g: Game) = g.tourSeason == g.season && g.tourStage in 0..5

    fun group(g: Game, i: Int): List<String> = g.tourTeams.drop(i * 4).take(4)

    /** Grup sıralaması: puan, averaj, atılan gol. */
    fun standings(g: Game, i: Int): List<String> = group(g, i).sortedWith(
        compareByDescending<String> { g.tourPts[it] ?: 0 }
            .thenByDescending { (g.tourGf[it] ?: 0) - (g.tourGa[it] ?: 0) }
            .thenByDescending { g.tourGf[it] ?: 0 },
    )

    private fun str(g: Game, c: String) =
        if (c == g.nationalTeam) National.strengthOf(g, c, National.squad(g)) else National.strengthOf(g, c)

    /** Kura: kullanıcının ülkesi + en güçlü 15 ulus, 4 torbadan gruplara dağıtılır. */
    fun start(g: Game, rnd: Random = Random.Default) {
        val me = g.nationalTeam
        val field = (listOf(me) + National.nations(g).filter { it != me }).distinct().take(16)
        if (field.size < 16) { National.summerTournament(g, rnd); g.tourSeason = g.season; return }
        val pots = field.sortedByDescending { str(g, it) }.chunked(4).map { it.shuffled(rnd) }
        g.tourTeams = MutableList(16) { i -> pots[i % 4][i / 4] }
        g.tourSeason = g.season
        g.tourStage = 0
        g.tourPts.clear(); g.tourGf.clear(); g.tourGa.clear(); g.tourAlive.clear(); g.tourLog.clear()
        val gi = g.tourTeams.indexOf(me) / 4
        g.addNews(
            "🏆 ${title(g)} başlıyor!",
            "Kura çekildi: ${National.name(me)} ${groupNames[gi]} grubunda — " +
                group(g, gi).filter { it != me }.joinToString { National.name(it) } + " ile. İlk maç için DEVAM ET.",
        )
        startStage(g, rnd)
    }

    private fun addGroup(g: Game, a: String, b: String, x: Int, y: Int) {
        g.tourGf[a] = (g.tourGf[a] ?: 0) + x; g.tourGa[a] = (g.tourGa[a] ?: 0) + y
        g.tourGf[b] = (g.tourGf[b] ?: 0) + y; g.tourGa[b] = (g.tourGa[b] ?: 0) + x
        g.tourPts[a] = (g.tourPts[a] ?: 0) + when { x > y -> 3; x == y -> 1; else -> 0 }
        g.tourPts[b] = (g.tourPts[b] ?: 0) + when { y > x -> 3; x == y -> 1; else -> 0 }
    }

    private fun stageName(g: Game, gi: Int = -1) =
        if (g.tourStage <= 2) "${title(g)} · ${groupNames[gi]} Grubu ${g.tourStage + 1}. maç" else "${title(g)} · ${koNames[g.tourStage - 3]}"

    /** Aşamanın maçlarını kurar: diğerleri sonuçlanır, kullanıcının maçı bekletilir. */
    private fun startStage(g: Game, rnd: Random) {
        val me = g.nationalTeam
        if (g.tourStage <= 2) {
            for (gi in 0..3) {
                val grp = group(g, gi)
                for ((i, j) in rounds[g.tourStage]) {
                    val a = grp[i]
                    val b = grp[j]
                    if (a == me || b == me) {
                        val opp = if (a == me) b else a
                        g.natPending = "$opp|${stageName(g, gi)}|${if (a == me) 1 else 0}|0|1"
                    } else {
                        val (x, y) = National.play(str(g, a), str(g, b), rnd)
                        addGroup(g, a, b, x, y)
                        g.tourLog += "${g.tourStage}|${National.name(a)} $x-$y ${National.name(b)}"
                    }
                }
            }
        } else {
            val alive = g.tourAlive
            val next = mutableListOf<String>()
            var userIn = false
            for (k in alive.indices step 2) {
                val a = alive[k]
                val b = alive[k + 1]
                if (a == me || b == me) {
                    userIn = true
                    next += "?"
                    val opp = if (a == me) b else a
                    g.natPending = "$opp|${stageName(g)}|${if (a == me) 1 else 0}|1|1"
                } else {
                    next += koMatch(g, a, b, rnd)
                }
            }
            g.tourAlive = next
            if (!userIn) finishRest(g, rnd)
        }
    }

    /** Tek maç eleme sonucu (beraberlikte penaltı). */
    private fun koMatch(g: Game, a: String, b: String, rnd: Random): String {
        var (x, y) = National.play(str(g, a), str(g, b), rnd)
        var pen = ""
        if (x == y) {
            val aWins = rnd.nextDouble() < 0.5 + (str(g, a) - str(g, b)) / 40.0
            pen = if (aWins) " (P 5-4)" else " (P 4-5)"
        }
        val w = if (x > y || (x == y && pen == " (P 5-4)")) a else b
        g.tourLog += "${g.tourStage}|${National.name(a)} $x-$y ${National.name(b)}$pen"
        return w
    }

    /** Kullanıcının turnuva maçı oynandı. */
    fun userResult(g: Game, p: National.Pending, x: Int, y: Int, won: Boolean, pens: String, scorers: List<String>) {
        val me = g.nationalTeam
        val (a, b, ga, gb) = if (p.home) listOf(me, p.opp, "$x", "$y") else listOf(p.opp, me, "$y", "$x")
        val penTxt = if (pens.isNotEmpty()) " (P ${if (p.home) pens else pens.split("-").reversed().joinToString("-")})" else ""
        val line = "${National.name(a)} $ga-$gb ${National.name(b)}$penTxt"
        g.tourLog += "${g.tourStage}|$line"
        g.natResults.add(0, "${g.season}|${p.kind}|$line|${when { won && (x != y || p.ko) -> 3; x == y -> 1; else -> 0 }}")
        while (g.natResults.size > 30) g.natResults.removeAt(g.natResults.lastIndex)
        val who = if (scorers.isNotEmpty()) " Golcüler: ${scorers.joinToString()}." else ""
        val rnd = Random.Default
        if (g.tourStage <= 2) {
            addGroup(g, a, b, ga.toInt(), gb.toInt())
            g.addNews("🏆 $line", "${p.kind}.$who")
            g.tourStage++
            if (g.tourStage <= 2) startStage(g, rnd) else qualify(g, rnd)
        } else {
            val i = g.tourAlive.indexOf("?")
            if (i >= 0) g.tourAlive[i] = if (won) me else p.opp
            if (won) Perks.addXp(g, 25)
            g.addNews("🏆 $line", "${p.kind} · ${if (won) "Tur atladık!" else "Turnuvaya veda ettik."}$who")
            if (won && g.tourStage == 5) { champion(g, me); return }
            if (!won) { finishRest(g, rnd); return }
            g.tourStage++
            startStage(g, rnd)
        }
    }

    /** Grup bitti: ilk ikiler çeyrek finale (A1-B2, B1-A2, C1-D2, D1-C2). */
    private fun qualify(g: Game, rnd: Random) {
        val s = (0..3).map { standings(g, it) }
        g.tourAlive = mutableListOf(s[0][0], s[1][1], s[1][0], s[0][1], s[2][0], s[3][1], s[3][0], s[2][1])
        g.tourStage = 3
        val me = g.nationalTeam
        if (me in g.tourAlive) {
            g.addNews("🏆 Çeyrek finaldeyiz!", "${National.name(me)} gruptan çıktı. ${title(g)} çeyrek finali için DEVAM ET.")
            startStage(g, rnd)
        } else {
            g.addNews("🏆 Gruptan çıkamadık", "${National.name(me)} ${title(g)}'na grup aşamasında veda etti.")
            finishRest(g, rnd)
        }
    }

    /** Kullanıcı elendiyse kalan turları arka planda oynatır. */
    private fun finishRest(g: Game, rnd: Random) {
        g.natPending = ""
        while (g.tourStage < 6 && g.tourAlive.size > 1) {
            if (g.tourStage < 3) g.tourStage = 3
            val next = mutableListOf<String>()
            for (k in g.tourAlive.indices step 2) next += koMatch(g, g.tourAlive[k], g.tourAlive[k + 1], rnd)
            g.tourAlive = next
            g.tourStage++
        }
        val champ = g.tourAlive.firstOrNull() ?: return
        g.tourStage = 6
        g.addNews("🏆 ${title(g)} sona erdi", "Şampiyon: ${National.name(champ)}. Yeni sezona hazır olduğunda DEVAM ET.")
    }

    private fun champion(g: Game, me: String) {
        g.tourAlive = mutableListOf(me)
        g.tourStage = 6
        g.natPending = ""
        g.trophies += "${g.season} ${title(g)} (${National.name(me)})"
        g.managerRep = (g.managerRep + 12).coerceAtMost(100)
        Perks.addXp(g, 150)
        g.addNews("🏆 ŞAMPİYONUZ!", "${National.name(me)}, ${title(g)}'nı kazandı! Menajer olarak tarihe geçtin.")
    }

    /** Aşama adı (ekran için). */
    fun stageText(g: Game): String = when (g.tourStage) {
        in 0..2 -> "Grup aşaması · ${g.tourStage + 1}. maç"
        in 3..5 -> koNames[g.tourStage - 3]
        else -> "Tamamlandı · Şampiyon: ${g.tourAlive.firstOrNull()?.let { National.name(it) } ?: "-"}"
    }
}
