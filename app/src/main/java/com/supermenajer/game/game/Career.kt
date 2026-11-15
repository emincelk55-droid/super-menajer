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

    private val allCache = LangCached { listOf(
        Achievement("first_win", "🥇", L("İlk galibiyet", "First win"), L("Resmi maçta ilk galibiyetini al.", "Win your first competitive match.")),
        Achievement("big_win", "💥", L("Gol yağmuru", "Goal fest"), L("5 veya daha fazla farkla kazan.", "Win by 5 or more goals.")),
        Achievement("streak5", "🔥", L("Seri", "Streak"), L("Ligde üst üste 5 galibiyet.", "Win 5 league matches in a row.")),
        Achievement("derby", "⚔️", L("Derbinin galibi", "Derby winner"), L("Bir derbi kazan.", "Win a derby.")),
        Achievement("champion", "🏆", L("Şampiyon", "Champions"), L("Lig şampiyonu ol.", "Win the league.")),
        Achievement("cup", "🏅", L("Kupa avcısı", "Cup hunter"), L("Ülke kupasını kazan.", "Win your national cup.")),
        Achievement("euro", "🌍", L("Avrupa'nın zirvesi", "Top of Europe"), L("Avrupa Kupası'nı kazan.", "Win the European Cup.")),
        Achievement("unbeaten", "🛡️", L("Yenilmezler", "Invincibles"), L("Ligde yenilgisiz bir sezon geçir.", "Go a whole league season unbeaten.")),
        Achievement("promotion", "⬆️", L("Yükseliş", "Promotion"), L("Bir üst lige çık.", "Win promotion to a higher league.")),
        Achievement("objective", "🎯", L("Sözünü tutan", "Man of his word"), L("Yönetimin sezon hedefini tuttur.", "Reach the board's season objective.")),
        Achievement("signing10", "💸", L("Büyük transfer", "Big signing"), L("€10M veya üstüne oyuncu al.", "Sign a player for €10M or more.")),
        Achievement("sale20", "💰", L("Kasa doldu", "Cash in"), L("€20M veya üstüne oyuncu sat.", "Sell a player for €20M or more.")),
        Achievement("wonderkid", "💎", L("Altın çocuk", "Golden boy"), L("Altyapından 84+ potansiyelli bir genç çıksın.", "Produce an academy youngster with 84+ potential.")),
        Achievement("stadium", "🏟️", L("Yeni tribün", "New stand"), L("Stadyumunu büyüt.", "Expand your stadium.")),
        Achievement("fullhouse", "🎟️", L("Kapalı gişe", "Sold out"), L("Stadını tamamen doldur.", "Fill your stadium completely.")),
        Achievement("sponsors", "🤝", L("Ticari deha", "Commercial genius"), L("Üç sponsorluk alanını da doldur.", "Fill all three sponsorship slots.")),
        Achievement("camp", "🏕️", L("Kamp ateşi", "Camp fire"), L("Takımı antrenman kampına götür.", "Take the team to a training camp.")),
        Achievement("daily7", "📅", L("Sadık menajer", "Loyal manager"), L("Günlük ödülü 7 gün üst üste al.", "Claim the daily reward 7 days in a row.")),
        Achievement("daily30", "🗓️", L("Her gün sahada", "Every single day"), L("Günlük ödülü 30 gün üst üste al.", "Claim the daily reward 30 days in a row.")),
        Achievement("challenges25", "✅", L("Görev adamı", "Task master"), L("25 maç görevi tamamla.", "Complete 25 match challenges.")),
        Achievement("perfect5", "💯", L("Kusursuz plan", "Perfect plan"), L("Bir maçın 3 görevini birden 5 kez tamamla.", "Complete all 3 match challenges 5 times.")),
        Achievement("hattrick", "🎩", L("Hat-trick", "Hat-trick"), L("Oyuncularından biri bir maçta 3 gol atsın.", "One of your players scores 3 goals in a match.")),
        Achievement("loyal", "💙", L("Kulüp efsanesi", "Club legend"), L("Aynı kulüpte 5 sezon tamamla.", "Complete 5 seasons at the same club.")),
    ) }
    val all get() = allCache.get()
    private val byId = all.associateBy { it.id }

    fun unlock(g: Game, id: String) {
        if (g.fired || id in g.achievements) return
        val a = byId[id] ?: return
        g.achievements += id
        g.managerRep = (g.managerRep + 1).coerceAtMost(100)
        Perks.addXp(g, 25)
        g.addNews(L("🏅 Başarım: ${a.name}", "🏅 Achievement: ${a.name}"), L("${a.icon} ${a.desc} Menajer itibarın arttı.", "${a.icon} ${a.desc} Your manager reputation increased."))
    }

    fun repText(rep: Int) = when {
        rep >= 90 -> L("Dünya çapında efsane", "World-class legend")
        rep >= 75 -> L("Kıtasal üne sahip", "Continental reputation")
        rep >= 60 -> L("Ulusal üne sahip", "National reputation")
        rep >= 45 -> L("Bölgesel üne sahip", "Regional reputation")
        else -> L("Yerel tanınırlık", "Local recognition")
    }

    /** Kullanıcı maçı sonrası: rekorlar ve maç başarımları. */
    fun afterMatch(g: Game, ourGoals: Int, theirGoals: Int, opponent: String, won: Boolean, derby: Boolean, league: Boolean) {
        val diff = ourGoals - theirGoals
        val label = "$ourGoals-$theirGoals $opponent (${g.season})"
        if (diff > g.recBigWinDiff) { g.recBigWinDiff = diff; g.recBigWin = label }
        if (-diff > g.recLossDiff) { g.recLossDiff = -diff; g.recLoss = label }
        Perks.addXp(g, if (won) 6 else if (diff == 0) 2 else 0)
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
        if (champion) { g.trophies += L("🏆 $season $leagueName şampiyonu", "🏆 $season $leagueName champions"); unlock(g, "champion") }
        if (cup) { g.trophies += "🏅 $season $cupName"; unlock(g, "cup") }
        if (euro) { g.trophies += L("🌍 $season Avrupa Kupası", "🌍 $season European Cup"); unlock(g, "euro") }
        if (metObjective) unlock(g, "objective")
        if (g.userTeam.lost == 0 && g.userTeam.played > 0) unlock(g, "unbeaten")
        g.managerRep = (g.managerRep + (if (champion) 6 else 0) + (if (cup) 3 else 0) + (if (euro) 8 else 0) +
            (if (metObjective) 2 else 0) - (if (missedBadly) 3 else 0)).coerceIn(10, 100)
        Perks.addXp(g, (if (champion) 120 else 0) + (if (cup) 80 else 0) + (if (euro) 150 else 0) + (if (metObjective) 60 else 0))
        g.seasonsAtClub++
        if (g.seasonsAtClub >= 5) unlock(g, "loyal")
    }

    // ---- Yönetimden istekler ----

    private val requestNamesCache = LangCached { listOf(L("💶 Transfer bütçesi artışı", "💶 Transfer budget increase"), L("💼 Maaş bütçesi artışı", "💼 Wage budget increase"), L("🌱 Altyapı yatırımı", "🌱 Youth academy investment"), L("🏋️ Antrenman tesisi yatırımı", "🏋️ Training facility investment")) }
    val requestNames get() = requestNamesCache.get()

    private fun r50k(v: Double) = (v / 50_000).roundToLong().coerceAtLeast(1) * 50_000

    /** Yönetimden istek: sezonda her türden bir kez. Başarı yönetim güvenine ve itibarına bağlı. */
    fun request(g: Game, kind: Int, rnd: Random = Random.Default): String {
        if (g.fired) return ""
        if (kind in g.boardAsked) return L("Bu sezon bu konuyu zaten yönetime açtın.", "You've already raised this topic with the board this season.")
        g.boardAsked += kind
        val us = g.userTeam
        val chance = ((g.confidence - 40) / 60.0 + (g.managerRep - 40) / 150.0).coerceIn(0.05, 0.9)
        val ok = rnd.nextDouble() < chance
        if (!ok) {
            g.confidence = (g.confidence - 3).coerceAtLeast(0.0)
            return listOf(
                L("Yönetim: \"Şu an bunun için uygun değiliz. Önce sonuçları görelim.\" (Güven -3)", "Board: \"We're not in a position to do that right now. Let's see results first.\" (Confidence -3)"),
                L("Başkan isteğini geri çevirdi: \"Elimizdekiyle yetinmeliyiz.\" (Güven -3)", "The chairman turned down your request: \"We have to make do with what we have.\" (Confidence -3)"),
            ).random(rnd)
        }
        return when (kind) {
            0 -> {
                val add = r50k((us.budget.coerceAtLeast(0) * 0.15).coerceIn(1_000_000.0, 15_000_000.0))
                Economy.earn(us, Economy.BOARD, add)
                g.addNews(L("Yönetim onayladı", "The board agreed"), L("Transfer bütçesine ${money(add)} ek ödenek verildi.", "An extra ${money(add)} has been added to the transfer budget."))
                L("Yönetim kabul etti! Transfer bütçesi +${money(add)}.", "The board agreed! Transfer budget +${money(add)}.")
            }
            1 -> {
                val add = (us.wageBudget * 0.1 / 5_000).roundToLong() * 5_000
                us.wageBudget += add
                g.addNews(L("Yönetim onayladı", "The board agreed"), L("Haftalık maaş bütçesi ${money(add)} artırıldı.", "The weekly wage budget has been increased by ${money(add)}."))
                L("Yönetim kabul etti! Maaş bütçesi +${money(add)}/hafta.", "The board agreed! Wage budget +${money(add)}/week.")
            }
            2 -> {
                if (us.youthFac >= 10) return L("Akademi zaten en üst seviyede.", "The academy is already at the top level.")
                us.youthFac++
                g.addNews(L("Yönetim onayladı", "The board agreed"), L("Yönetim altyapı akademisine yatırım yaptı. Yeni seviye: ${us.youthFac}/10.", "The board invested in the youth academy. New level: ${us.youthFac}/10."))
                L("Yönetim akademiye yatırım yaptı! Seviye ${us.youthFac}/10.", "The board invested in the academy! Level ${us.youthFac}/10.")
            }
            else -> {
                if (us.trainingFac >= 10) return L("Antrenman tesisi zaten en üst seviyede.", "The training facility is already at the top level.")
                us.trainingFac++
                g.addNews(L("Yönetim onayladı", "The board agreed"), L("Yönetim antrenman tesisine yatırım yaptı. Yeni seviye: ${us.trainingFac}/10.", "The board invested in the training facility. New level: ${us.trainingFac}/10."))
                L("Yönetim tesise yatırım yaptı! Seviye ${us.trainingFac}/10.", "The board invested in the facility! Level ${us.trainingFac}/10.")
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
