package com.supermenajer.game.engine

/**
 * Yardımcı antrenörün maç içi önerileri: skor, dakika, kondisyon, kartlar ve istatistiklere bakar.
 * Her öneri tek dokunuşla uygulanabilir (değişiklik önerisi değişiklik penceresini açar).
 */
object MatchAdvice {
    /** [subs]: uygulamak için değişiklik penceresi açılmalı; [apply]: doğrudan uygulanır. */
    class Tip(val key: String, val text: String, val button: String, val subs: Boolean = false, val apply: (MatchSim, Side) -> Unit = { _, _ -> })

    fun suggest(sim: MatchSim, us: Side, skip: Set<String>): Tip? {
        val op = sim.other(us)
        val t = us.team
        val min = sim.minute
        val tips = buildList {
            if (op.reds > us.reds && t.pressing != 2) add(Tip("press", "Rakip ${11 - op.reds} kişi kaldı. Önde basıp boğalım!", "Pres: Yüksek") { _, s -> s.team.pressing = 2 })
            if (min >= 60 && us.goals < op.goals && t.mentality != 2) add(Tip("attack", "Gerideyiz hocam. Hücuma geçmenin zamanı.", "Hücum") { _, s -> s.team.mentality = 2 })
            if (min >= 75 && us.goals > op.goals && t.mentality != 0) add(Tip("defend", "Skoru koruyalım; takımı geriye çekelim.", "Defans") { _, s -> s.team.mentality = 0 })
            val tired = us.onPitch.filter { it.slot.pos != com.supermenajer.game.model.Pos.GK }.minByOrNull { it.fit }
            if (tired != null && tired.fit < 52 && us.subsLeft > 0 && min >= 50) add(Tip("tired${tired.p.id}", "${tired.p.name} çok yoruldu (kondisyon %${tired.fit.toInt()}). Değiştirelim.", "Değişiklik", subs = true))
            val booked = us.onPitch.firstOrNull { it.yellow > 0 && it.slot.pos == com.supermenajer.game.model.Pos.DF }
            if (booked != null && min in 55..85 && us.subsLeft > 0) add(Tip("card${booked.p.id}", "${booked.p.name} sarı kartlı, ikinci sarı riski var.", "Değişiklik", subs = true))
            if (min >= 30 && op.xg > us.xg + 0.9 && t.defLine != 0) add(Tip("deep", "Rakip arkaya çok koşu buluyor. Savunma hattını geri çekelim.", "Hat: Geride") { _, s -> s.team.defLine = 0 })
            val tp = (us.poss + op.poss).coerceAtLeast(1)
            if (min >= 25 && us.poss * 100 / tp < 38 && t.passing != 0) add(Tip("short", "Topu tutamıyoruz. Kısa paslarla oyunu kuralım.", "Pas: Kısa") { _, s -> s.team.passing = 0 })
            if (min >= 35 && us.shots <= 1 && t.tempo != 2 && us.goals <= op.goals) add(Tip("tempo", "Hiç şut atamıyoruz. Tempoyu artıralım.", "Tempo: Yüksek") { _, s -> s.team.tempo = 2 })
        }
        return tips.firstOrNull { it.key !in skip }
    }
}
