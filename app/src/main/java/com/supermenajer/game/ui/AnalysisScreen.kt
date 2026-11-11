package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Analysis
import com.supermenajer.game.model.Game

/** Sezon analizi: özet, maç maç gol beklentisi, puan çizgisi ve gol dakikaları. */
@Composable
fun AnalysisView(g: Game) {
    val ms = Analysis.season(g)
    if (ms.isEmpty()) {
        Text("Bu sezon henüz maç oynanmadı. İlk maçtan sonra analiz burada.", color = TextDim, modifier = Modifier.padding(16.dp))
        return
    }
    LazyVerticalGrid(GridCells.Adaptive(420.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { SummaryRow(ms) }
        item { DashCard("Maç maç gol beklentisi (xG)", Modifier.fillMaxWidth()) { XgBars(ms.takeLast(14)) } }
        item { DashCard("Puan çizgisi", Modifier.fillMaxWidth()) { PointsLine(ms) } }
        item { DashCard("Gollerin dakikası", Modifier.fillMaxWidth()) { PeriodBars(g.goalsByPeriod) } }
        item { DashCard("Yorum", Modifier.fillMaxWidth()) { Insights(ms, g.goalsByPeriod) } }
    }
}

@Composable
private fun SummaryRow(ms: List<Analysis.Match>) {
    val w = ms.count { it.gf > it.ga }
    val d = ms.count { it.gf == it.ga }
    val l = ms.size - w - d
    val items = listOf(
        "Maç" to "${ms.size}", "G-B-M" to "$w-$d-$l", "Gol" to "${ms.sumOf { it.gf }}:${ms.sumOf { it.ga }}",
        "xG" to "%.1f : %.1f".format(ms.sumOf { it.xgf }, ms.sumOf { it.xga }),
        "Topa sahip" to "%${ms.map { it.poss }.average().toInt()}", "Gol yemeden" to "${ms.count { it.ga == 0 }}",
    )
    DashCard(null, Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for ((k, v) in items) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(k, color = TextDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun XgBars(ms: List<Analysis.Match>) {
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val mid = size.height / 2
        val maxV = (ms.maxOf { maxOf(it.xgf, it.xga, it.gf.toDouble(), it.ga.toDouble()) }).coerceAtLeast(2.0).toFloat()
        val slot = size.width / ms.size
        val bw = slot * 0.55f
        drawLine(Line, Offset(0f, mid), Offset(size.width, mid), 1.5f)
        ms.forEachIndexed { i, m ->
            val x = slot * i + (slot - bw) / 2
            val up = (m.xgf.toFloat() / maxV) * (mid - 6)
            val dn = (m.xga.toFloat() / maxV) * (mid - 6)
            drawRect(Teal, Offset(x, mid - up), Size(bw, up))
            drawRect(Red.copy(alpha = 0.85f), Offset(x, mid), Size(bw, dn))
            // Gerçek goller: nokta
            repeat(m.gf) { k -> drawCircle(Color.White, 3.2f, Offset(x + bw / 2, mid - 8f - k * 9f)) }
            repeat(m.ga) { k -> drawCircle(Color.White, 3.2f, Offset(x + bw / 2, mid + 8f + k * 9f)) }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        for (m in ms) Text(m.opp, color = if (m.gf > m.ga) Green else if (m.gf == m.ga) Gold else Red, fontSize = 10.sp,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
    }
    Text("Üstte bizim, altta rakibin gol beklentisi; noktalar atılan goller.", color = TextDim, fontSize = 10.sp)
}

@Composable
private fun PointsLine(ms: List<Analysis.Match>) {
    var acc = 0
    val pts = ms.map { acc += it.points; acc }
    var xacc = 0.0
    // Beklenen puan: xG farkına göre kabaca (fark > 0.4 galibiyet, < -0.4 yenilgi)
    val xpts = ms.map { m -> xacc += when { m.xgf - m.xga > 0.4 -> 2.3; m.xgf - m.xga < -0.4 -> 0.5; else -> 1.3 }; xacc }
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val maxP = (ms.size * 3).coerceAtLeast(3).toFloat()
        fun pt(i: Int, v: Float) = Offset(if (ms.size == 1) size.width else size.width * i / (ms.size - 1), size.height * (1f - v / maxP))
        drawLine(Line, Offset(0f, size.height), pt(ms.size - 1, maxP), 1.5f) // her maç galibiyet çizgisi
        fun line(vals: List<Float>, c: Color, w: Float) {
            val p = Path()
            vals.forEachIndexed { i, v -> val o = pt(i, v); if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
            drawPath(p, c, style = Stroke(w))
        }
        line(xpts.map { it.toFloat() }, Gold.copy(alpha = 0.7f), 3f)
        line(pts.map { it.toFloat() }, Teal, 5f)
    }
    Row {
        Text("━ Puan ${pts.last()}", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text("━ Oyuna göre beklenen %.0f".format(xpts.last()), color = Gold, fontSize = 11.sp)
    }
}

@Composable
private fun PeriodBars(p: List<Int>) {
    val labels = listOf("0-15", "16-30", "31-45", "46-60", "61-75", "76-90+")
    val maxV = (p.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val slot = size.width / 6
        val bw = slot * 0.32f
        for (i in 0 until 6) {
            val f = p.getOrElse(i) { 0 } / maxV * (size.height - 4)
            val a = p.getOrElse(6 + i) { 0 } / maxV * (size.height - 4)
            drawRect(Teal, Offset(slot * i + slot * 0.15f, size.height - f), Size(bw, f))
            drawRect(Red.copy(alpha = 0.85f), Offset(slot * i + slot * 0.15f + bw + 3f, size.height - a), Size(bw, a))
        }
    }
    Row(Modifier.fillMaxWidth()) { for (l in labels) Text(l, color = TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
    Text("Turkuaz attığımız, kırmızı yediğimiz goller.", color = TextDim, fontSize = 10.sp)
}

@Composable
private fun Insights(ms: List<Analysis.Match>, p: List<Int>) {
    val notes = buildList {
        val xgf = ms.sumOf { it.xgf }; val gf = ms.sumOf { it.gf }
        val xga = ms.sumOf { it.xga }; val ga = ms.sumOf { it.ga }
        if (gf > xgf + 3) add("🎯 Beklenenden ${"%.0f".format(gf - xgf)} gol fazla attık: bitiricilik çok iyi (ya da şanslıyız).")
        if (gf < xgf - 3) add("😬 Pozisyon üretiyoruz ama ${"%.0f".format(xgf - gf)} gol kaçırdık. Bitiricilik antrenmanı düşün.")
        if (ga > xga + 3) add("🧤 Beklenenden fazla gol yiyoruz: kaleci ya da savunma hata yapıyor.")
        if (ga < xga - 3) add("🛡️ Savunma ve kaleci beklenenin üstünde: ${"%.0f".format(xga - ga)} golü önledi.")
        val lateFor = p.getOrElse(5) { 0 }; val lateAg = p.getOrElse(11) { 0 }
        if (lateAg >= 3 && lateAg > lateFor) add("⏱️ Son 15 dakikada $lateAg gol yedik: kondisyon ya da oyuncu değişikliği zamanlaması.")
        if (lateFor >= 3 && lateFor > lateAg) add("💪 Son dakikalarda $lateFor gol attık: takım fiziksel olarak güçlü.")
        val poss = ms.map { it.poss }.average()
        if (poss < 42) add("⚽ Topa az sahip oluyoruz (%${poss.toInt()}). Kısa pas ve düşük tempo kontrolü artırır.")
        if (poss > 58 && gf < xgf) add("🔁 Topa sahibiz ama etkisiz kalıyoruz. Tempo ve genişliği artırmayı dene.")
        val last5 = ms.takeLast(5).sumOf { it.points }
        add("📈 Son ${minOf(5, ms.size)} maçta $last5 puan.")
    }
    for (n in notes) Text(n, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
}
