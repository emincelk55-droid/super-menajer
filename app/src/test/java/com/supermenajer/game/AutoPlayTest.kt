package com.supermenajer.game

import com.supermenajer.game.game.AutoPilot
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Invariants
import com.supermenajer.game.game.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

/**
 * Test modu: yapay zekâ menajer farklı kulüplerle (güçlü, zayıf, alt lig, yeni lig) birkaç sezon oynar.
 * Her adımda dünya tutarlılığı denetlenir; çökme ya da tutarsızlık testi düşürür. Rapor: build/reports/autoplay.txt
 */
class AutoPlayTest {
    private class Run(val name: String, val seed: Int, val pick: (com.supermenajer.game.model.Game) -> Int)

    @Test
    fun aiPlaysCareers() {
        val runs = listOf(
            Run("Süper Lig devi", 1) { w -> w.divisionTeams(0).maxBy { it.reputation }.id },
            Run("Süper Lig küçüğü", 2) { w -> w.divisionTeams(0).minBy { it.reputation }.id },
            Run("1. Lig", 3) { w -> w.divisionTeams(1).sortedBy { it.reputation }[5].id },
            Run("İngiltere orta", 4) { w -> w.divisionTeams(10).sortedBy { it.reputation }[9].id },
            Run("Brezilya (yeni lig)", 5) { w -> w.divisionTeams(23)[2].id },
        )
        val report = StringBuilder()
        val problems = mutableListOf<String>()
        for (r in runs) {
            GameRepo.init(Files.createTempDirectory("auto").toFile())
            val w = Generator.newWorld(Random(r.seed)).also { Season.computeExpectations(it) }
            GameRepo.newCareer(w, "Otopilot", r.pick(w))
            GameRepo.game!!.introDone = true
            val rnd = Random(r.seed * 31)
            val log = AutoPilot.Log()
            val start = System.currentTimeMillis()
            var steps = 0
            val seen = HashSet<String>()
            try {
                while (log.seasons < 2 && steps++ < 4000) {
                    if (AutoPilot.step(log, rnd)) {
                        // Sezon geçişinde kaydet → geri yükle: kayıt bozulmadan devam edebilmeli
                        val before = Triple(GameRepo.game!!.players.size, GameRepo.game!!.userTeam.name, GameRepo.game!!.season)
                        GameRepo.save(force = true); GameRepo.flush()
                        if (!GameRepo.load()) problems += "[${r.name}] Kayıt geri yüklenemedi"
                        val ng = GameRepo.game!!
                        val after = Triple(ng.players.size, ng.userTeam.name, ng.season)
                        if (before != after) problems += "[${r.name}] Kayıt farklı döndü: $before → $after"
                    }
                    val g = GameRepo.game!!
                    if (steps % 5 == 0) for (p in Invariants.check(g)) if (seen.add(p)) problems += "[${r.name} s${g.season} gün ${g.week}] $p"
                }
            } catch (e: Throwable) {
                problems += "[${r.name}] ÇÖKTÜ: ${e::class.simpleName}: ${e.message} @ ${e.stackTrace.take(4).joinToString(" < ")}"
            }
            val g = GameRepo.game!!
            val t = g.userTeam
            report.appendLine("== ${r.name}: ${t.name} (${com.supermenajer.game.model.Leagues.of(t.division)?.name}) · ${(System.currentTimeMillis() - start) / 1000}s, $steps adım")
            report.appendLine("   maç ${log.matches}: ${log.wins}G ${log.draws}B ${log.losses}M · alış ${log.bought} satış ${log.sold} uzatma ${log.renewed} olay ${log.events} kovulma ${log.fired}")
            report.appendLine("   bütçe ${t.budget / 1_000_000}M · kadro ${g.squad(t.id).size} · güven %${g.confidence.toInt()} · kupalar ${g.trophies.size}")
            log.actions.filter { it.startsWith("Sezon") || it.startsWith("Kovuldu") || it.startsWith("Transfer") }.takeLast(10).forEach { report.appendLine("   · $it") }
        }
        report.appendLine(if (problems.isEmpty()) "SORUN YOK" else "SORUNLAR (${problems.size}):\n" + problems.joinToString("\n"))
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/autoplay.txt").writeText(report.toString())
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
