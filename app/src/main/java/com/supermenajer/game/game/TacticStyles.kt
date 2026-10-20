package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos

/**
 * Taktik Oluştur: hazır oyun tarzları. Her tarz takım talimatlarını ayarlar ve
 * kadronun niteliklerine göre ne kadar uygun olduğunu (artı/eksi yönleriyle) söyler.
 */
object TacticStyles {
    /** need: nitelik ağırlıkları (hız, şut, pas, teknik, savunma, fizik) ve bakılacak mevkiler. */
    class Style(
        val name: String, val desc: String,
        val mentality: Int, val tempo: Int, val width: Int, val passing: Int, val pressing: Int, val defLine: Int, val counter: Boolean,
        val formations: List<String>, val need: DoubleArray, val positions: Set<Pos>,
    )

    private val all3 = setOf(Pos.DF, Pos.MF, Pos.FW)

    val all = listOf(
        Style("Tiki-Taka", "Kısa paslarla topa sahip ol, rakibi yorarak boşluk bul. Top kaybedilince hemen baskı.",
            1, 0, 0, 0, 2, 2, false, listOf("4-3-3", "4-2-3-1"), doubleArrayOf(0.0, 0.0, 1.0, 1.0, 0.0, 0.0), setOf(Pos.MF, Pos.FW)),
        Style("Gegenpress", "Takımda her oyuncu top kaybedildiği an pres yapar; yüksek tempo, yüksek savunma hattı.",
            2, 2, 1, 1, 2, 2, false, listOf("4-2-3-1", "4-3-3"), doubleArrayOf(0.5, 0.0, 0.2, 0.0, 0.3, 1.0), all3),
        Style("Topa Sahip Ol", "Sabırlı oyun kurulumu, topu ayakta tut; top kaybında yerine dönen derli toplu bir takım.",
            1, 0, 1, 0, 1, 1, false, listOf("4-3-3", "4-2-3-1", "4-5-1"), doubleArrayOf(0.0, 0.0, 1.0, 0.6, 0.2, 0.0), setOf(Pos.DF, Pos.MF)),
        Style("Kanat Oyunu", "Oyunu genişlet; hızlı kanatlar ve bekler çizgiye inip ortalar yapar.",
            1, 1, 2, 1, 1, 1, false, listOf("4-4-2", "4-3-3"), doubleArrayOf(1.0, 0.0, 0.6, 0.6, 0.0, 0.0), setOf(Pos.MF, Pos.FW)),
        Style("Uzun Top", "Orta sahayı hızla geç; güçlü santrforlara uzun toplar.",
            1, 2, 1, 2, 0, 0, false, listOf("4-4-2", "3-5-2"), doubleArrayOf(0.3, 0.5, 0.0, 0.0, 0.0, 1.0), setOf(Pos.FW)),
        Style("Kontra Atak", "Geride kal, top kazanınca hızlı forvetlerle boşlukları cezalandır.",
            0, 2, 1, 2, 0, 0, true, listOf("4-2-3-1", "4-4-2", "5-3-2"), doubleArrayOf(1.0, 0.6, 0.0, 0.0, 0.4, 0.0), setOf(Pos.DF, Pos.FW)),
        Style("Catenaccio", "Önce savunma: derin ve dar blok, alan verme, fırsat buldukça vur.",
            0, 0, 0, 1, 0, 0, true, listOf("5-3-2", "3-5-2", "4-5-1"), doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.6), setOf(Pos.DF, Pos.MF)),
    )

    private val attrNames = listOf("hız", "şut", "pas", "teknik", "savunma", "fizik")

    private fun attrs(p: Player) = intArrayOf(p.pace, p.shooting, p.passing, p.dribbling, p.defending, p.physical)

    /** Kadronun en iyi 16 saha oyuncusu (kaleci hariç). */
    private fun core(g: Game): List<Player> = g.squad(g.userTeamId).filter { it.pos != Pos.GK }.sortedByDescending { it.overall }.take(16)

    /** Tarzın kadroya uygunluk puanı (ham). */
    fun fitness(g: Game, s: Style): Double {
        val ps = core(g).filter { it.pos in s.positions }.ifEmpty { core(g) }
        if (ps.isEmpty()) return 0.0
        val wsum = s.need.sum()
        return ps.sumOf { p -> attrs(p).withIndex().sumOf { (i, v) -> v * s.need[i] } / wsum } / ps.size
    }

    /** Tüm tarzlar için 1-5 arası uygunluk. */
    fun ratings(g: Game): Map<Style, Int> {
        val f = all.associateWith { fitness(g, it) }
        val lo = f.values.min()
        val hi = f.values.max()
        return f.mapValues { (_, v) -> if (hi - lo < 0.01) 3 else (1 + ((v - lo) / (hi - lo) * 4)).toInt().coerceIn(1, 5) }
    }

    /** Artı ve eksi yönler: kadronun ilgili nitelikleri takım ortalamasıyla karşılaştırılır. */
    fun prosCons(g: Game, s: Style): Pair<List<String>, List<String>> {
        val c = core(g)
        if (c.isEmpty()) return emptyList<String>() to emptyList()
        val ps = c.filter { it.pos in s.positions }.ifEmpty { c }
        val teamAvg = c.flatMap { attrs(it).toList() }.average()
        val pros = mutableListOf<String>()
        val cons = mutableListOf<String>()
        s.need.withIndex().filter { it.value >= 0.5 }.forEach { (i, _) ->
            val v = ps.map { attrs(it)[i] }.average()
            val who = s.positions.joinToString("/") { it.label.lowercase() }
            when {
                v >= teamAvg + 2 -> pros += "Kadromuzdaki $who oyuncularının ${attrNames[i]} gücü bu sisteme çok uygun (${v.toInt()})."
                v <= teamAvg - 2 -> cons += "$who oyuncularımızın ${attrNames[i]} seviyesi bu tarzın gerektirdiğinin altında (${v.toInt()})."
                else -> pros += "${attrNames[i].replaceFirstChar { it.uppercase() }} açısından kadromuz yeterli (${v.toInt()})."
            }
        }
        if (s.pressing == 2 && c.map { it.physical }.average() < teamAvg) cons += "Yoğun pres oyuncuları çabuk yorabilir; rotasyon gerekecek."
        if (s.defLine == 2 && g.squad(g.userTeamId).filter { it.pos == Pos.DF }.map { it.pace }.average() < 65) cons += "Yüksek savunma hattı yavaş stoperlerle arkaya top yiyebilir."
        return pros to cons
    }

    /** Tarzı uygula: talimatlar, diziliş ve en iyi 11. */
    fun apply(g: Game, s: Style, formation: String) {
        val t = g.userTeam
        t.mentality = s.mentality
        t.tempo = s.tempo
        t.width = s.width
        t.passing = s.passing
        t.pressing = s.pressing
        t.defLine = s.defLine
        t.counter = s.counter
        t.offsideTrap = s.defLine == 2
        GameRepo.setFormation(formation)
        GameRepo.autoLineup()
    }
}
