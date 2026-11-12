package com.supermenajer.game

import com.supermenajer.game.game.AutoPilot
import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Invariants
import com.supermenajer.game.game.Season
import com.supermenajer.game.model.Leagues
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

/**
 * Uzun test modu: yapay zekâ 10 sezon oynar, her sezon ekonomi/denge ölçülür.
 * Uzun sürdüğü için yalnız LONG_TEST=1 ile çalışır. Rapor: build/reports/long_autoplay.txt
 */
class LongAutoPlayTest {
    @Test
    fun tenSeasons() {
        assumeTrue(System.getenv("LONG_TEST") == "1")
        val seasons = (System.getenv("LONG_SEASONS") ?: "10").toInt()
        val out = StringBuilder()
        val runs = listOf<Pair<String, (com.supermenajer.game.model.Game) -> Int>>(
            "Süper Lig orta" to { w -> w.divisionTeams(0).sortedByDescending { it.reputation }[6].id },
            "İngiltere zayıf" to { w -> w.divisionTeams(10).sortedBy { it.reputation }[2].id },
            "1. Lig" to { w -> w.divisionTeams(1).sortedByDescending { it.reputation }[8].id },
        )
        for ((ri, run) in runs.withIndex()) {
            GameRepo.init(Files.createTempDirectory("long").toFile())
            val w = Generator.newWorld(Random(100 + ri)).also { Season.computeExpectations(it) }
            com.supermenajer.game.game.Fictional.apply(w)
            GameRepo.newCareer(w, "Otopilot", run.second(w))
            GameRepo.game!!.introDone = true
            val log = AutoPilot.Log()
            val rnd = Random(7 + ri)
            out.appendLine("=== ${run.first}: ${GameRepo.game!!.userTeam.name}")
            out.appendLine("sez | kulüp(lig) sıra puan | bütçe M | maaş/limit K | kadro ort.güç | dünya: ort.güç 85+ 80+ oyuncu serbest | bütçe medyan/en çok M | maaş medyan K | ort.değer K | en pahalı transfer M | kayıt KB | süre s | sorun")
            var steps = 0
            var lastSeason = log.seasons
            var t0 = System.currentTimeMillis()
            val problems = LinkedHashSet<String>()
            var maxFee = 0L
            while (log.seasons < seasons && steps++ < seasons * 900) {
                val g0 = GameRepo.game!!
                val before = g0.news.firstOrNull()
                val sb = g0.userTeam.name to g0.userTeam.division
                val ended = try { AutoPilot.step(log, rnd) } catch (e: Throwable) { problems += "ÇÖKME ${e::class.simpleName}: ${e.message} @ ${e.stackTrace.take(3).joinToString(" < ")}"; break }
                val g = GameRepo.game!!
                // Dünyadaki en pahalı transfer (haberlerden değil, takip için değer üzerinden kaba ölçü)
                if (steps % 10 == 0) problems += Invariants.check(g)
                if (ended) {
                    val us = g.userTeam
                    val prevTable = sb
                    val act = g.players.filter { it.teamId >= 0 }
                    val tops = Leagues.topLeagues.map { it.id }.filter { g.divisionTeams(it).isNotEmpty() }
                    val budgets = tops.flatMap { d -> g.divisionTeams(d).map { it.budget } }.sorted()
                    val wages = tops.flatMap { d -> g.divisionTeams(d).map { Economy.squadWages(g, it.id) } }.sorted()
                    val save = runCatching { GameRepo.save(force = true); GameRepo.flush(); File(System.getProperty("java.io.tmpdir")).let { 0 } }.getOrDefault(0)
                    val json = kotlinx.serialization.json.Json.encodeToString(com.supermenajer.game.model.Game.serializer(), g)
                    val bytes = java.io.ByteArrayOutputStream().also { b -> java.util.zip.GZIPOutputStream(b).use { it.write(json.toByteArray()) } }.size()
                    val last = log.actions.lastOrNull { it.startsWith("Sezon") } ?: ""
                    val sq = g.squad(us.id)
                    out.appendLine(
                        "${g.season - 1} | ${last.removePrefix("Sezon ").substringAfter("bitti: ")} [${Leagues.of(prevTable.second)?.name}] | ${us.budget / 1_000_000} | ${Economy.squadWages(g, us.id) / 1000}/${us.wageBudget / 1000} | ${sq.size} ${"%.1f".format(sq.sortedByDescending { it.overall }.take(11).map { it.overall }.average())}" +
                            " | ${"%.1f".format(act.map { it.overall }.average())} ${act.count { it.overall >= 85 }} ${act.count { it.overall >= 80 }} ${act.size} ${g.squad(-1).size}" +
                            " | ${budgets[budgets.size / 2] / 1_000_000}/${budgets.last() / 1_000_000} | ${wages[wages.size / 2] / 1000} | ${act.map { it.value }.average().toLong() / 1000} | - | ${bytes / 1024} | ${(System.currentTimeMillis() - t0) / 1000} | ${problems.size}"
                    )
                    t0 = System.currentTimeMillis()
                }
            }
            out.appendLine("maç ${log.matches}: ${log.wins}G ${log.draws}B ${log.losses}M · alış ${log.bought} satış ${log.sold} uzatma ${log.renewed} kovulma ${log.fired} · kupalar ${GameRepo.game!!.trophies}")
            if (problems.isNotEmpty()) out.appendLine("SORUNLAR: " + problems.take(20).joinToString("\n  "))
        }
        println(out)
        File("build/reports").mkdirs()
        File("build/reports/long_autoplay.txt").writeText(out.toString())
    }
}
