package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Kariyer katmanı (CM / FM / Soccer Manager): başarım rozetleri, menajer itibarı ve kupa dolabı,
 * kulüp rekorları, yönetimden istekler ve transfer döneminin son günü.
 */
object Career {
    class Achievement(val id: String, val icon: String, val name: String, val desc: String)

    val all = listOf(
        Achievement("first_win", "🥇", "İlk galibiyet", "Resmi maçta ilk galibiyetini al."),
        Achievement("big_win", "💥", "Gol yağmuru", "5 veya daha fazla farkla kazan."),
        Achievement("streak5", "🔥", "Seri", "Ligde üst üste 5 galibiyet."),
        Achievement("derby", "⚔️", "Derbinin galibi", "Bir derbi kazan."),
        Achievement("champion", "🏆", "Şampiyon", "Lig şampiyonu ol."),
        Achievement("cup", "🏅", "Kupa avcısı", "Ülke kupasını kazan."),
        Achievement("euro", "🌍", "Avrupa'nın zirvesi", "Avrupa Kupası'nı kazan."),
        Achievement("unbeaten", "🛡️", "Yenilmezler", "Ligde yenilgisiz bir sezon geçir."),
        Achievement("promotion", "⬆️", "Yükseliş", "Bir üst lige çık."),
        Achievement("objective", "🎯", "Sözünü tutan", "Yönetimin sezon hedefini tuttur."),
        Achievement("signing10", "💸", "Büyük transfer", "€10M veya üstüne oyuncu al."),
        Achievement("sale20", "💰", "Kasa doldu", "€20M veya üstüne oyuncu sat."),
        Achievement("wonderkid", "💎", "Altın çocuk", "Altyapından 84+ potansiyelli bir genç çıksın."),
        Achievement("stadium", "🏟️", "Yeni tribün", "Stadyumunu büyüt."),
        Achievement("fullhouse", "🎟️", "Kapalı gişe", "Stadını tamamen doldur."),
        Achievement("sponsors", "🤝", "Ticari deha", "Üç sponsorluk alanını da doldur."),
        Achievement("camp", "🏕️", "Kamp ateşi", "Takımı antrenman kampına götür."),
        Achievement("loyal", "💙", "Kulüp efsanesi", "Aynı kulüpte 5 sezon tamamla."),
    )
    private val byId = all.associateBy { it.id }

    fun unlock(g: Game, id: String) {
        if (g.fired || id in g.achievements) return
        val a = byId[id] ?: return
        g.achievements += id
        g.managerRep = (g.managerRep + 1).coerceAtMost(100)
        g.addNews("🏅 Başarım: ${a.name}", "${a.icon} ${a.desc} Menajer itibarın arttı.")
    }

    fun repText(rep: Int) = when {
        rep >= 90 -> "Dünya çapında efsane"
        rep >= 75 -> "Kıtasal üne sahip"
        rep >= 60 -> "Ulusal üne sahip"
        rep >= 45 -> "Bölgesel üne sahip"
        else -> "Yerel tanınırlık"
    }

    /** Kullanıcı maçı sonrası: rekorlar ve maç başarımları. */
    fun afterMatch(g: Game, ourGoals: Int, theirGoals: Int, opponent: String, won: Boolean, derby: Boolean, league: Boolean) {
        val diff = ourGoals - theirGoals
        val label = "$ourGoals-$theirGoals $opponent (${g.season})"
        if (diff > g.recBigWinDiff) { g.recBigWinDiff = diff; g.recBigWin = label }
        if (-diff > g.recLossDiff) { g.recLossDiff = -diff; g.recLoss = label }
        if (won) unlock(g, "first_win")
        if (diff >= 5) unlock(g, "big_win")
        if (won && derby) unlock(g, "derby")
        if (league && g.userTeam.form.takeLast(5) == "GGGGG") unlock(g, "streak5")
        val att = g.userTeam.lastAttendance
        if (att > g.recAttendance) g.recAttendance = att
        if (att > 0 && att >= g.userTeam.stadium) unlock(g, "fullhouse")
    }

    fun signing(g: Game, name: String, fee: Long) {
        if (fee > g.recSigningFee) { g.recSigningFee = fee; g.recSigning = "$name · ${money(fee)} (${g.season})" }
        if (fee >= 10_000_000) unlock(g, "signing10")
    }

    fun sale(g: Game, name: String, fee: Long) {
        if (fee > g.recSaleFee) { g.recSaleFee = fee; g.recSale = "$name · ${money(fee)} (${g.season})" }
        if (fee >= 20_000_000) unlock(g, "sale20")
    }

    /** Sezon sonu: kupalar ve itibar. */
    fun seasonEnd(g: Game, champion: Boolean, cup: Boolean, euro: Boolean, metObjective: Boolean, missedBadly: Boolean, leagueName: String, cupName: String) {
        if (g.fired) return
        val season = "${g.season}-${(g.season + 1) % 100}"
        if (champion) { g.trophies += "🏆 $season $leagueName şampiyonu"; unlock(g, "champion") }
        if (cup) { g.trophies += "🏅 $season $cupName"; unlock(g, "cup") }
        if (euro) { g.trophies += "🌍 $season Avrupa Kupası"; unlock(g, "euro") }
        if (metObjective) unlock(g, "objective")
        if (g.userTeam.lost == 0 && g.userTeam.played > 0) unlock(g, "unbeaten")
        g.managerRep = (g.managerRep + (if (champion) 6 else 0) + (if (cup) 3 else 0) + (if (euro) 8 else 0) +
            (if (metObjective) 2 else 0) - (if (missedBadly) 3 else 0)).coerceIn(10, 100)
        g.seasonsAtClub++
        if (g.seasonsAtClub >= 5) unlock(g, "loyal")
    }

    // ---- Yönetimden istekler ----

    val requestNames = listOf("💶 Transfer bütçesi artışı", "💼 Maaş bütçesi artışı", "🌱 Altyapı yatırımı", "🏋️ Antrenman tesisi yatırımı")

    private fun r50k(v: Double) = (v / 50_000).roundToLong().coerceAtLeast(1) * 50_000

    /** Yönetimden istek: sezonda her türden bir kez. Başarı yönetim güvenine ve itibarına bağlı. */
    fun request(g: Game, kind: Int, rnd: Random = Random.Default): String {
        if (g.fired) return ""
        if (kind in g.boardAsked) return "Bu sezon bu konuyu zaten yönetime açtın."
        g.boardAsked += kind
        val us = g.userTeam
        val chance = ((g.confidence - 40) / 60.0 + (g.managerRep - 40) / 150.0).coerceIn(0.05, 0.9)
        val ok = rnd.nextDouble() < chance
        if (!ok) {
            g.confidence = (g.confidence - 3).coerceAtLeast(0.0)
            return listOf(
                "Yönetim: \"Şu an bunun için uygun değiliz. Önce sonuçları görelim.\" (Güven -3)",
                "Başkan isteğini geri çevirdi: \"Elimizdekiyle yetinmeliyiz.\" (Güven -3)",
            ).random(rnd)
        }
        return when (kind) {
            0 -> {
                val add = r50k((us.budget.coerceAtLeast(0) * 0.15).coerceIn(1_000_000.0, 15_000_000.0))
                Economy.earn(us, Economy.BOARD, add)
                g.addNews("Yönetim onayladı", "Transfer bütçesine ${money(add)} ek ödenek verildi.")
                "Yönetim kabul etti! Transfer bütçesi +${money(add)}."
            }
            1 -> {
                val add = (us.wageBudget * 0.1 / 5_000).roundToLong() * 5_000
                us.wageBudget += add
                g.addNews("Yönetim onayladı", "Haftalık maaş bütçesi ${money(add)} artırıldı.")
                "Yönetim kabul etti! Maaş bütçesi +${money(add)}/hafta."
            }
            2 -> {
                if (us.youthFac >= 10) return "Akademi zaten en üst seviyede."
                us.youthFac++
                g.addNews("Yönetim onayladı", "Yönetim altyapı akademisine yatırım yaptı. Yeni seviye: ${us.youthFac}/10.")
                "Yönetim akademiye yatırım yaptı! Seviye ${us.youthFac}/10."
            }
            else -> {
                if (us.trainingFac >= 10) return "Antrenman tesisi zaten en üst seviyede."
                us.trainingFac++
                g.addNews("Yönetim onayladı", "Yönetim antrenman tesisine yatırım yaptı. Yeni seviye: ${us.trainingFac}/10.")
                "Yönetim tesise yatırım yaptı! Seviye ${us.trainingFac}/10."
            }
        }
    }

    /** Transfer döneminin son günü (maç olmayan son gün): piyasa hareketlenir. */
    fun isDeadlineDay(g: Game): Boolean {
        if (!Season.transferWindowOpen(g) || g.seasonOver || g.isMatchDay) return false
        val day = g.calendar.getOrNull(g.week - 1) ?: return false
        return day.type == 0 && (day.number == 4 || day.number == 19) && g.date == GameCalendar.dateOf(g, g.week) - 1
    }
}
