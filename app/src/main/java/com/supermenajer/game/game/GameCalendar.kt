package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Takvime gerçek tarihler verir: lig maçları cumartesi, kupa çarşamba, Avrupa perşembe.
 * 17. haftadan sonra 3 haftalık devre arası. Oyun günü [Game.date] (epoch günü) ile tutulur;
 * maç olmayan günler de tek tek geçilebilir.
 */
object GameCalendar {
    private var cacheKey = ""
    private var cache: List<Long> = emptyList()

    private val months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
    private val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    /** Takvimdeki her günün tarihi (epoch günü). */
    fun dates(g: Game): List<Long> {
        val key = "${g.season}:${g.calendar.size}:${g.calendar.firstOrNull()?.type}"
        if (key == cacheKey && cache.size == g.calendar.size) return cache
        var d = LocalDate.of(g.season, 8, 8).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val out = ArrayList<Long>(g.calendar.size)
        var first = true
        for (day in g.calendar) {
            if (!first) {
                val target = when (day.type) {
                    0 -> DayOfWeek.SATURDAY
                    1 -> DayOfWeek.WEDNESDAY
                    else -> DayOfWeek.THURSDAY
                }
                d = d.plusDays(1).with(TemporalAdjusters.nextOrSame(target))
            }
            first = false
            out += d.toEpochDay()
            if (day.type == 0 && day.number == 17) d = d.plusDays(21)
        }
        cache = out
        cacheKey = key
        return out
    }

    /** Takvim gününün (1 tabanlı) tarihi. */
    fun dateOf(g: Game, dayIdx: Int): Long {
        val ds = dates(g)
        if (ds.isEmpty()) return LocalDate.of(g.season, 8, 8).toEpochDay()
        return ds[(dayIdx - 1).coerceIn(0, ds.size - 1)]
    }

    /** Sezon başı: ilk maçtan 10 gün önce hazırlık dönemi. */
    fun seasonStart(g: Game): Long = dateOf(g, 1) - 10

    fun format(epochDay: Long, withYear: Boolean = true): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "${days[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${months[d.monthValue - 1]}" + if (withYear) " ${d.year}" else ""
    }

    /** Sıradaki maça kaç gün var. */
    fun daysUntil(g: Game, dayIdx: Int): Int = (dateOf(g, dayIdx) - g.date).toInt().coerceAtLeast(0)
}
