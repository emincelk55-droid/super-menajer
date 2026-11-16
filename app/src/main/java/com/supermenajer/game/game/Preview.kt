package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import kotlin.math.exp

/**
 * Sezon önizlemesi: sezon başında TV yorumcularının lig tahmini (şampiyonluk ihtimalleri,
 * sürpriz ve küme adayı, kulübümüzün beklenen sırası). Sezon sonunda tahmin gerçekle karşılaştırılır.
 * Kayıt: g.preview = "takımId|tahminiSıra|şampiyonlukYüzdesi" (kulübümüzün liginde).
 */
object Preview {
    private val pundits = listOf("Selim Arslan", "Kaan Demirtaş", "Murat Eren", "Levent Uysal", "Okan Sevim")

    class Row(val teamId: Int, val pred: Int, val odds: Int)

    fun rows(g: Game): List<Row> = if (g.previewSeason != g.season) emptyList() else g.preview.mapNotNull { s ->
        val c = s.split("|")
        if (c.size < 3) null else Row(c[0].toInt(), c[1].toInt(), c[2].toInt())
    }

    /** Lig tahmini: güç sırası; şampiyonluk yüzdeleri güç farkından (softmax). */
    fun make(g: Game) {
        val us = g.userTeam
        val teams = g.divisionTeams(us.division)
        if (teams.size < 4) return
        val str = teams.associateWith { Lineups.strength(g, it) }
        val sorted = teams.sortedByDescending { str.getValue(it) }
        val top = str.values.max()
        val w = sorted.map { exp((str.getValue(it) - top) / 1.6) }
        val sum = w.sum()
        g.preview = sorted.mapIndexed { i, t -> "${t.id}|${i + 1}|${(w[i] / sum * 100).toInt()}" }.toMutableList()
        g.previewSeason = g.season
        val rows = rows(g)
        val fav = g.team(rows[0].teamId)
        val mine = rows.first { it.teamId == us.id }
        val n = rows.size
        val pundit = pundits[(g.season + us.id) % pundits.size]
        // Sürpriz takım: güç sırasına göre orta sıralarda, kadrosunda en çok genç yetenek olan
        val dark = rows.drop(3).take(n / 2).maxByOrNull { r -> g.squad(r.teamId).count { it.age <= 23 && it.potential >= 75 } }?.let { g.team(it.teamId) }
        val ourLine = when {
            mine.pred == 1 -> L("${us.name} bu sezon şampiyonluğun bir numaralı adayı; baskı da büyük olacak.", "${us.name} are the title favourites this season; the pressure will be huge.")
            mine.pred <= 4 -> L("${us.name} zirve yarışında, ${mine.pred}. sırayı bekliyorum.", "${us.name} will be in the title race; I expect them to finish ${mine.pred}.")
            mine.pred >= n - 2 -> L("${us.name} için zor bir sezon: küme hattında, ${mine.pred}. sıra tahminim.", "A tough season for ${us.name}: in the relegation zone, I predict ${mine.pred}.")
            else -> L("${us.name} orta sıralarda, ${mine.pred}. bitirir diye düşünüyorum.", "I think ${us.name} will be mid-table, finishing ${mine.pred}.")
        }
        g.addNews(
            L("📺 Sezon önizlemesi: favori ${fav.name}", "📺 Season preview: ${fav.name} are favourites"),
            L("Yorumcu $pundit: \"Şampiyonluk favorim ${fav.name} (%${rows[0].odds}). ", "Pundit $pundit: \"My title favourites are ${fav.name} (${rows[0].odds}%). ") +
                (dark?.let { L("Sürpriz takımım ${it.name}. ", "My dark horses are ${it.name}. ") } ?: "") +
                "$ourLine\"",
        )
    }

    /** Sezon sonu: tahminle gerçeği karşılaştır. */
    fun review(g: Game) {
        val mine = rows(g).firstOrNull { it.teamId == g.userTeamId } ?: return
        val pos = g.position(g.userTeamId)
        if (pos <= 0) return
        val diff = mine.pred - pos
        val (title, body) = when {
            diff >= 3 -> L("📺 Yorumcuları yanılttık!", "📺 We proved the pundits wrong!") to
                L("Sezon başında ${mine.pred}. sıra tahmin edilmişti, ${pos}. bitirdik. Stüdyoda herkes bizi konuşuyor.", "We were predicted to finish ${mine.pred}, and we finished ${pos}. Everyone in the studio is talking about us.")
            diff <= -3 -> L("📺 Beklentinin altında kaldık", "📺 We fell short of expectations") to
                L("Tahmin ${mine.pred}. sıraydı, ${pos}. bitirdik. Yorumcular sezonu sert eleştiriyor.", "We were predicted ${mine.pred} but finished ${pos}. The pundits are criticising our season.")
            else -> L("📺 Tahmin tuttu", "📺 The prediction came true") to
                L("Sezon başında ${mine.pred}. sıra beklenmişti, ${pos}. bitirdik.", "We were expected to finish ${mine.pred} and finished ${pos}.")
        }
        g.addNews(title, body)
        if (diff >= 3) Fans.change(g, 4.0, L("Beklentiyi aştık", "Beat expectations"))
        if (diff <= -3) Fans.change(g, -3.0, L("Beklentinin altında", "Below expectations"))
    }
}
