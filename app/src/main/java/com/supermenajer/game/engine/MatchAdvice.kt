package com.supermenajer.game.engine

import com.supermenajer.game.game.L

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
            if (op.reds > us.reds && t.pressing != 2) add(Tip("press", L("Rakip ${11 - op.reds} kişi kaldı. Önde basıp boğalım!", "They're down to ${11 - op.reds} men. Let's press high and suffocate them!"), L("Pres: Yüksek", "Pressing: High")) { _, s -> s.team.pressing = 2 })
            if (min >= 60 && us.goals < op.goals && t.mentality != 2) add(Tip("attack", L("Gerideyiz hocam. Hücuma geçmenin zamanı.", "We're behind, boss. Time to attack."), L("Hücum", "Attack")) { _, s -> s.team.mentality = 2 })
            if (min >= 75 && us.goals > op.goals && t.mentality != 0) add(Tip("defend", L("Skoru koruyalım; takımı geriye çekelim.", "Let's protect the score; pull the team back."), L("Defans", "Defend")) { _, s -> s.team.mentality = 0 })
            val tired = us.onPitch.filter { it.slot.pos != com.supermenajer.game.model.Pos.GK }.minByOrNull { it.fit }
            if (tired != null && tired.fit < 52 && us.subsLeft > 0 && min >= 50) add(Tip("tired${tired.p.id}", L("${tired.p.name} çok yoruldu (kondisyon %${tired.fit.toInt()}). Değiştirelim.", "${tired.p.name} is exhausted (fitness %${tired.fit.toInt()}). Let's take him off."), L("Değişiklik", "Substitution"), subs = true))
            val booked = us.onPitch.firstOrNull { it.yellow > 0 && it.slot.pos == com.supermenajer.game.model.Pos.DF }
            if (booked != null && min in 55..85 && us.subsLeft > 0) add(Tip("card${booked.p.id}", L("${booked.p.name} sarı kartlı, ikinci sarı riski var.", "${booked.p.name} is on a yellow, there's a risk of a second."), L("Değişiklik", "Substitution"), subs = true))
            if (min >= 30 && op.xg > us.xg + 0.9 && t.defLine != 0) add(Tip("deep", L("Rakip arkaya çok koşu buluyor. Savunma hattını geri çekelim.", "They keep finding runs in behind. Let's drop the defensive line."), L("Hat: Geride", "Line: Deep")) { _, s -> s.team.defLine = 0 })
            val tp = (us.poss + op.poss).coerceAtLeast(1)
            if (min >= 25 && us.poss * 100 / tp < 38 && t.passing != 0) add(Tip("short", L("Topu tutamıyoruz. Kısa paslarla oyunu kuralım.", "We can't keep the ball. Let's build with short passes."), L("Pas: Kısa", "Passing: Short")) { _, s -> s.team.passing = 0 })
            if (min >= 35 && us.shots <= 1 && t.tempo != 2 && us.goals <= op.goals) add(Tip("tempo", L("Hiç şut atamıyoruz. Tempoyu artıralım.", "We can't get a shot off. Let's raise the tempo."), L("Tempo: Yüksek", "Tempo: High")) { _, s -> s.team.tempo = 2 })
        }
        return tips.firstOrNull { it.key !in skip }
    }
}
