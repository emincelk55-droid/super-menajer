package com.supermenajer.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Finance
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game

/** Aylık gelir-gider: son 12 ayın net sonucu çubuklarla, aylık maaş ve TV geliri. */
@Composable
fun MonthlyFinanceCard(g: Game) {
    val us = g.userTeam
    SectionCard(L("📊 Aylık gelir-gider", "📊 Monthly cash flow")) {
        val rows = g.monthLedger.mapNotNull { r -> r.split("|").takeIf { it.size >= 5 } }
        Row(Modifier.fillMaxWidth()) {
            Text(L("Aylık maaş gideri", "Monthly wage bill"), color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("−" + money(Finance.monthlyWages(g, us)), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(L("Aylık TV yayın geliri", "Monthly TV income"), color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("+" + money(Finance.monthlyTv(us)), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            Text(L("İlk ay bitince aylık sonuçlar burada görünür.", "Monthly results appear here once the first month ends."), color = TextDim, fontSize = 12.sp)
        } else {
            val nets = rows.map { it[3].toLongOrNull() ?: 0L }
            val maxAbs = nets.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1L).toFloat()
            Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                val w = size.width / 12f
                val mid = size.height / 2
                drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, mid), Offset(size.width, mid), 1.5f)
                nets.forEachIndexed { i, n ->
                    val h = (n / maxAbs) * (mid - 4)
                    val x = (12 - nets.size + i) * w + w * 0.2f
                    drawRect(if (n >= 0) Green else Red, Offset(x, if (n >= 0) mid - h else mid), Size(w * 0.6f, kotlin.math.abs(h).coerceAtLeast(2f)))
                }
            }
            val last = rows.last()
            Text(
                L("Son ay: gelir ${money(last[1].toLong())}, gider ${money(last[2].toLong())}, net ${money(last[3].toLong())}",
                    "Last month: income ${money(last[1].toLong())}, expenses ${money(last[2].toLong())}, net ${money(last[3].toLong())}"),
                color = TextDim, fontSize = 11.sp,
            )
        }
        if (us.budget < 0) Text(
            L("⚠️ Kasa ekside! Yönetim her ay güvenini düşürüyor. Satış yap, maaşları kıs ya da kredi al.", "⚠️ In the red! The board's confidence drops every month. Sell, cut wages or take a loan."),
            color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Banka kredisi: tutar seç, faiz ve taksit görünür. */
@Composable
fun LoanCard(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    SectionCard(L("🏦 Banka kredisi", "🏦 Bank loan")) {
        if (g.loanLeft > 0) {
            Text(L("Kalan borç: ${money(g.loanLeft)} · aylık taksit ${money(g.loanMonthly)}", "Remaining debt: ${money(g.loanLeft)} · monthly instalment ${money(g.loanMonthly)}"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(L("Yeni kredi için mevcut borcun bitmesi gerekir.", "Your current debt must be repaid before a new loan."), color = TextDim, fontSize = 11.sp)
        } else {
            val limit = Finance.loanLimit(g)
            Text(
                L("Faiz %${Finance.INTEREST_PCT}, ${Finance.LOAN_MONTHS} ayda geri ödeme. En fazla ${money(limit)}.", "${Finance.INTEREST_PCT}% interest, repaid over ${Finance.LOAN_MONTHS} months. Up to ${money(limit)}."),
                color = TextDim, fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(limit / 4, limit / 2, limit).distinct().filter { it > 0 }.forEach { a ->
                    Text(
                        money(a), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Surface2).clickable { msg = GameRepo.takeLoan(a) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        msg?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}
