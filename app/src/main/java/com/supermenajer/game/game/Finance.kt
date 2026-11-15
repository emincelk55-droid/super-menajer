package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Team
import java.time.LocalDate

/**
 * Gerçek finans: maaşlar her ay kasadan ödenir, TV yayın geliri her ay girer, kasa eksiye düşerse
 * yönetim baskısı artar, istenirse banka kredisi alınır. Kullanıcıya aylık mali rapor gelir.
 */
object Finance {
    const val WAGES = "Maaş ödemeleri"
    const val TV = "TV yayın geliri"
    const val LOAN_IN = "Banka kredisi"
    const val LOAN_PAY = "Kredi taksiti"
    const val INTEREST_PCT = 8
    const val LOAN_MONTHS = 12

    private fun key(epochDay: Long): Int = LocalDate.ofEpochDay(epochDay).let { it.year * 12 + it.monthValue - 1 }

    /** Aylık maaş gideri (haftalık × 52 / 12). */
    fun monthlyWages(g: Game, t: Team): Long = Economy.squadWages(g, t.id) * 52 / 12

    /** Aylık TV yayın geliri: ligin zenginliği ve kulübün itibarı. */
    fun monthlyTv(t: Team): Long {
        val lg = Leagues.of(t.division) ?: return 0L
        val base = if (lg.tier == 1) 170_000.0 else 60_000.0
        return (base * lg.wealth * (0.6 + t.reputation / 100.0)).toLong()
    }

    /** Kullanıcının alabileceği en yüksek kredi: yıllık TV + sponsor geliri ölçüsünde. */
    fun loanLimit(g: Game): Long = (monthlyTv(g.userTeam) * 12 + 2_000_000L).let { (it / 500_000L) * 500_000L }

    /** Her gün başında çağrılır: ay değiştiyse maaş, TV geliri, kredi taksiti ve rapor. */
    fun check(g: Game) {
        if (g.date <= 0) return
        val now = key(g.date)
        if (g.finMonth < 0) { g.finMonth = now; snap(g); return }
        if (now == g.finMonth) return
        g.finMonth = now
        monthEnd(g)
        snap(g)
    }

    private fun snap(g: Game) {
        g.finSnap = g.userTeam.income.toMutableMap()
    }

    private fun monthEnd(g: Game) {
        for (t in g.teams) {
            if (t.division < 0) continue
            Economy.earn(t, TV, monthlyTv(t))
            Economy.earn(t, WAGES, -monthlyWages(g, t))
        }
        val us = g.userTeam
        if (g.loanLeft > 0) {
            val pay = minOf(g.loanMonthly, g.loanLeft)
            Economy.earn(us, LOAN_PAY, -pay)
            g.loanLeft -= pay
            if (g.loanLeft <= 0) { g.loanLeft = 0; g.loanMonthly = 0; g.addNews(L("🏦 Kredi kapandı", "🏦 Loan repaid"), L("Banka kredisinin son taksiti ödendi.", "The last instalment of the bank loan has been paid.")) }
        }
        if (g.fired) return
        // Aylık rapor: bu ayın gelir ve giderleri (kategori farkları)
        var income = 0L
        var expense = 0L
        val lines = mutableListOf<String>()
        for ((k, v) in us.income) {
            val d = v - (g.finSnap[k] ?: 0L)
            if (d == 0L) continue
            if (d > 0) income += d else expense += -d
            lines += "$k ${if (d > 0) "+" else "−"}${money(kotlin.math.abs(d))}"
        }
        val net = income - expense
        g.monthLedger += "${g.finMonth - 1}|$income|$expense|$net|${us.budget}"
        if (g.monthLedger.size > 12) g.monthLedger.removeAt(0)
        val month = LocalDate.ofEpochDay(g.date).minusMonths(1)
        val monthName = MonthlyAwards.monthName(month.monthValue)
        g.addNews(
            L("📊 $monthName mali raporu: ${if (net >= 0) "+" else "−"}${money(kotlin.math.abs(net))}", "📊 $monthName financial report: ${if (net >= 0) "+" else "−"}${money(kotlin.math.abs(net))}"),
            L("Gelir ${money(income)}, gider ${money(expense)}. Kasa: ${money(us.budget)}. Ayrıntı: ", "Income ${money(income)}, expenses ${money(expense)}. Balance: ${money(us.budget)}. Details: ") + lines.joinToString(" · "),
        )
        if (us.budget < 0) {
            g.confidence = (g.confidence - 3).coerceAtLeast(0.0)
            g.addNews(
                L("⚠️ Kasa ekside", "⚠️ In the red"),
                L("Kulübün kasası ${money(us.budget)} ekside. Yönetim tedirgin (güven −3). Oyuncu satarak, maaş bütçesini kısarak ya da kredi alarak toparlanmalıyız.",
                    "The club is ${money(-us.budget)} in the red. The board is uneasy (confidence −3). We need to recover by selling players, cutting wages or taking a loan."),
            )
        }
    }

    /** Banka kredisi: tutar kasaya girer, faiziyle 12 ayda geri ödenir. Hata varsa nedeni döner. */
    fun takeLoan(g: Game, amount: Long): String? {
        if (g.loanLeft > 0) return L("Önce mevcut krediyi kapatmalısın.", "You must repay your current loan first.")
        if (amount <= 0 || amount > loanLimit(g)) return L("Banka en fazla ${money(loanLimit(g))} verebilir.", "The bank can lend at most ${money(loanLimit(g))}.")
        val total = amount + amount * INTEREST_PCT / 100
        g.loanLeft = total
        g.loanMonthly = (total + LOAN_MONTHS - 1) / LOAN_MONTHS
        Economy.earn(g.userTeam, LOAN_IN, amount)
        g.addNews(
            L("🏦 Banka kredisi", "🏦 Bank loan"),
            L("${money(amount)} kredi kasaya girdi. Faiziyle ${money(total)}, $LOAN_MONTHS ayda aylık ${money(g.loanMonthly)} taksitle ödenecek.",
                "A ${money(amount)} loan has been credited. With interest ${money(total)}, repaid in $LOAN_MONTHS monthly instalments of ${money(g.loanMonthly)}."),
        )
        return null
    }
}
