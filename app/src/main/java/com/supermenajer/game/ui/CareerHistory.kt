package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import java.util.Locale

/* ---------- Oyuncu kariyer geçmişi: sezon sezon istatistikler ve kulüp değişiklikleri ---------- */

/** Bir sezonun kariyer satırı. */
class CareerSeason(val season: Int, val club: String, val apps: Int, val goals: Int, val assists: Int, val rating: Double, val current: Boolean)

/** Bir kulüp değişikliği. */
class CareerMove(val season: Int, val week: Int, val from: String, val to: String, val fee: Long, val kind: Char)

object CareerData {
    fun seasons(g: Game, p: Player): List<CareerSeason> {
        val past = p.career.mapNotNull { line ->
            val c = line.split("|")
            if (c.size < 6) null
            else CareerSeason(c[0].toIntOrNull() ?: return@mapNotNull null, c[1], c[2].toIntOrNull() ?: 0, c[3].toIntOrNull() ?: 0, c[4].toIntOrNull() ?: 0, c[5].toDoubleOrNull() ?: 0.0, false)
        }
        val club = g.teamOrNull(p.teamId)?.name ?: L("Serbest", "Free agent")
        val now = CareerSeason(g.season, club, p.apps, p.goals, p.assists, p.avgRating, true)
        return past + now
    }

    fun moves(p: Player): List<CareerMove> = p.moves.mapNotNull { line ->
        val c = line.split("|")
        if (c.size < 6) null
        else CareerMove(c[0].toIntOrNull() ?: 0, c[1].toIntOrNull() ?: 0, c[2], c[3], c[4].toLongOrNull() ?: -1, c[5].firstOrNull() ?: 'T')
    }

    /** Kariyer toplamları: maç, gol, asist, maça göre ağırlıklı ortalama puan. */
    fun totals(list: List<CareerSeason>): CareerSeason {
        val apps = list.sumOf { it.apps }
        val rating = if (apps > 0) list.sumOf { it.rating * it.apps } / apps else 0.0
        return CareerSeason(0, "", apps, list.sumOf { it.goals }, list.sumOf { it.assists }, rating, false)
    }

    fun moveText(m: CareerMove): String = when (m.kind) {
        'L' -> L("Kiralık: ${m.from} → ${m.to}", "Loan: ${m.from} → ${m.to}")
        'R' -> L("Kiralıktan döndü: ${m.from} → ${m.to}", "Back from loan: ${m.from} → ${m.to}")
        'F' -> L("Serbest transfer: ${m.to}", "Free transfer: ${m.to}")
        'X' -> L("Serbest kaldı: ${m.from}", "Released by ${m.from}")
        else -> "${m.from} → ${m.to}"
    }

    fun feeText(m: CareerMove): String = when {
        m.kind == 'L' || m.kind == 'R' || m.kind == 'X' -> ""
        m.fee > 0 -> money(m.fee)
        m.fee == 0L || m.kind == 'F' -> L("bedelsiz", "free")
        else -> ""
    }
}

private val colW = 40.dp

@Composable
fun CareerHistory(g: Game, p: Player) {
    val seasons = CareerData.seasons(g, p)
    val moves = CareerData.moves(p)
    val tot = CareerData.totals(seasons)
    val clubs = (seasons.filter { it.apps > 0 }.map { it.club } + moves.flatMap { listOf(it.from, it.to) }).filter { it.isNotBlank() }.distinct().size
    val fees = moves.filter { it.kind == 'T' && it.fee > 0 }.sumOf { it.fee }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Stat(L("Maç", "Apps"), "${tot.apps}")
            Stat(L("Gol", "Goals"), "${tot.goals}")
            Stat(L("Asist", "Assists"), "${tot.assists}")
            Stat(L("Ort. Puan", "Avg. Rating"), if (tot.apps > 0) "%.2f".format(tot.rating) else "–")
            Stat(L("Kulüp", "Clubs"), "${clubs.coerceAtLeast(1)}")
            Stat(L("Bonservis", "Fees"), if (fees > 0) money(fees) else "–")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L("Sezon", "Season"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
            Text(L("Kulüp", "Club"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            for (h in listOf(L("Maç", "Apps"), L("Gol", "Gls"), L("Ast", "Ast"), L("Not", "Rtg"))) Head(h)
        }
        HorizontalDivider(color = CardLine)
        val best = seasons.filter { it.apps >= 10 }.takeIf { it.size >= 2 }?.maxByOrNull { it.rating }
        for (s in seasons.sortedByDescending { it.season }) {
            // Bu sezon içindeki transferler, sezon satırının üstünde (en yenisi önce).
            for (m in moves.filter { it.season == s.season }.sortedByDescending { it.week }) MoveRow(m)
            SeasonRow(s, s === best)
        }
        // Kayıtlı ilk sezondan önceki hareketler (eski kayıtlarda olabilir)
        val first = seasons.minOf { it.season }
        for (m in moves.filter { it.season < first }.sortedByDescending { it.season * 100 + it.week }) MoveRow(m)
        HorizontalDivider(color = CardLine, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L("Toplam", "Total"), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(62.dp))
            Text(L("${seasons.count { it.apps > 0 }} sezon", "${seasons.count { it.apps > 0 }} seasons"), color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Num("${tot.apps}", bold = true); Num("${tot.goals}", bold = true); Num("${tot.assists}", bold = true)
            Num(if (tot.apps > 0) "%.2f".format(Locale.US, tot.rating) else "–", ratingColor(tot.rating), bold = true)
        }
        if (seasons.size == 1 && moves.isEmpty()) {
            Text(L("Sezon sonunda istatistikler buraya eklenir; transferler de anında işlenir.", "Stats are added here at the end of each season; transfers are logged right away."), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Head(t: String) =
    Text(t, color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(colW))

@Composable
private fun Num(t: String, color: Color = TextMain, bold: Boolean = false) =
    Text(t, color = color, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.width(colW))

@Composable
private fun SeasonRow(s: CareerSeason, best: Boolean) {
    val bg = if (s.current) Teal.copy(alpha = 0.08f) else Color.Transparent
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(bg).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${s.season}-${(s.season + 1) % 100}".padEnd(7), color = if (s.current) Teal else TextDim, fontSize = 12.sp, modifier = Modifier.width(62.dp))
        Text(
            s.club + (if (s.current) L(" (bu sezon)", " (now)") else "") + (if (best) " ⭐" else ""),
            fontSize = 12.sp, fontWeight = if (s.current) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Num("${s.apps}"); Num("${s.goals}"); Num("${s.assists}")
        Num(if (s.apps > 0) "%.2f".format(Locale.US, s.rating) else "–", if (s.apps > 0) ratingColor(s.rating) else TextDim)
    }
}

@Composable
private fun MoveRow(m: CareerMove) {
    val (icon, col) = when (m.kind) {
        'L', 'R' -> "⇄" to Teal
        'X' -> "✕" to TextDim
        else -> "➜" to Gold
    }
    Row(Modifier.fillMaxWidth().padding(start = 62.dp, top = 1.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = col, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
        Text(CareerData.moveText(m), color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val fee = CareerData.feeText(m)
        if (fee.isNotEmpty()) Text(fee, color = col, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
