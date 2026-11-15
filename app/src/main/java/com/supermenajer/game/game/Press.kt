package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.PressQuestion
import kotlin.random.Random

/** Maç sonrası basın toplantıları. Cevaplar takım morali ve yönetim güvenini etkiler. */
object Press {
    /** Cevap etkisi: (kadro morali, yönetim güveni, geri bildirim). */
    private class Effect(val morale: Double, val confidence: Double, val feedback: String)

    private class Template(val text: (String) -> String, val answers: List<Pair<String, Effect>>)

    private val templatesCache = LangCached { listOf(
        // 0: farklı galibiyet
        Template({ L("Hocam, $it karşısında farklı galibiyet! Bu takımın hedefi ne?", "Boss, a big win against $it! What's this team's goal?") }, listOf(
            "Ayaklarımız yere basmalı, maç maç gideceğiz." to Effect(2.0, 1.0, L("Olgun bir açıklama. Oyuncular odaklı kalıyor.", "A mature statement. The players stay focused.")),
            "Bu takım şampiyonluğa oynar!" to Effect(5.0, 3.0, L("Taraftar coştu, oyuncuların özgüveni tavan!", "The fans are ecstatic and the players' confidence is sky-high!")),
            "Rakip çok kötüydü, abartmayalım." to Effect(-1.0, -1.0, L("Oyuncular galibiyetin küçümsenmesinden pek hoşlanmadı.", "The players didn't like the win being played down.")),
        )),
        // 1: galibiyet
        Template({ L("$it galibiyetinin anahtarı neydi?", "What was the key to the win against $it?") }, listOf(
            "Oyuncularımın mücadelesi." to Effect(4.0, 0.0, L("Oyuncular övgüden çok memnun.", "The players are delighted with the praise.")),
            "Taktiğimiz tuttu, plan işledi." to Effect(1.0, 2.0, L("Yönetim taktik vizyonunu beğendi.", "The board liked your tactical vision.")),
            "Henüz iyi oynamıyoruz, daha çok çalışmalıyız." to Effect(-2.0, 1.0, L("Oyuncular biraz bozuldu ama mesaj alındı.", "The players were a little put out, but the message was received.")),
        )),
        // 2: beraberlik
        Template({ L("$it ile berabere kaldınız. Memnun musunuz?", "You drew with $it. Are you satisfied?") }, listOf(
            "Bir puan da değerli, devam ediyoruz." to Effect(1.0, 0.0, L("Sakin bir açıklama, ortam huzurlu.", "A calm statement, the mood is peaceful.")),
            "Hayır! Kazanmamız gerekiyordu, oyunculardan memnun değilim." to Effect(-4.0, 2.0, L("Yönetim sert tavrını beğendi, oyuncular gergin.", "The board liked your firm stance, the players are tense.")),
            "Hakem kararları sonucu belirledi." to Effect(2.0, -2.0, L("Oyuncular arkalarında durmanı sevdi, federasyon ise rahatsız.", "The players liked you standing behind them, the federation is less pleased.")),
        )),
        // 3: mağlubiyet
        Template({ L("$it yenilgisinin sorumlusu kim?", "Who's to blame for the defeat to $it?") }, listOf(
            "Sorumluluk tamamen bende." to Effect(3.0, -1.0, L("Oyuncular hocalarına daha çok bağlandı.", "The players grew closer to their manager.")),
            "Oyuncular sahada yoktu." to Effect(-6.0, 1.0, L("Soyunma odasında hava çok gergin!", "The atmosphere in the dressing room is very tense!")),
            "Şanssızdık, böyle maçlar olur." to Effect(1.0, 0.0, L("Kimse fazla üzerine düşmedi.", "Nobody dwelt on it too much.")),
        )),
        // 4: ağır mağlubiyet
        Template({ L("$it karşısında ağır yenilgi... Taraftar istifa diyor, ne diyeceksiniz?", "A heavy defeat to $it... The fans want you to resign, what do you say?") }, listOf(
            "Bu takımı ayağa kaldıracağım, söz veriyorum." to Effect(2.0, 2.0, L("Kararlı duruşun yönetimi biraz rahatlattı.", "Your determined stance reassured the board a little.")),
            "İstifa etmeyeceğim, hedeflerimiz değişmedi." to Effect(0.0, -2.0, L("Yönetim cevabını soğuk karşıladı.", "The board received your answer coldly.")),
            "Bu kadroyla ancak bu kadar." to Effect(-5.0, -5.0, L("Hem oyuncular hem yönetim çok kızgın!", "Both the players and the board are furious!")),
        )),
        // 5: Avrupa
        Template({ L("Avrupa'da $it maçı sonrası düşünceleriniz?", "Your thoughts after the European match against $it?") }, listOf(
            "Türk futbolunu en iyi şekilde temsil edeceğiz." to Effect(3.0, 1.0, L("Taraftar ve oyuncular gururlu.", "The fans and players are proud.")),
            "Ligi önceliğimiz yapacağız." to Effect(-1.0, 1.0, L("Bazı oyuncular Avrupa'da daha çok şans istiyordu.", "Some players wanted more chances in Europe.")),
            "Avrupa'da büyük işler yapacağız!" to Effect(4.0, 2.0, L("Heyecan dorukta!", "Excitement is at its peak!")),
        )),
    ) }
    private val templates get() = templatesCache.get()

    fun question(goalDiff: Int, opponent: String, euro: Boolean, cup: Boolean, rnd: Random): PressQuestion {
        val kind = when {
            euro -> 5
            goalDiff >= 3 -> 0
            goalDiff > 0 -> 1
            goalDiff == 0 && !cup -> 2
            goalDiff <= -3 -> 4
            else -> 3
        }
        val t = templates[kind]
        return PressQuestion(t.text(opponent), t.answers.map { it.first }, kind)
    }

    /** Cevabı uygular ve geri bildirim döner. */
    fun answer(g: Game, q: PressQuestion, index: Int): String {
        if (q.kind == Rivals.PRESS_KIND) return Rivals.answer(g, index)
        val e = templates.getOrNull(q.kind)?.answers?.getOrNull(index)?.second ?: return ""
        val f = StaffLogic.talkFactor(g)
        for (p in g.squad(g.userTeamId)) p.morale = (p.morale + e.morale * f).coerceIn(5.0, 100.0)
        g.confidence = (g.confidence + e.confidence).coerceIn(0.0, 100.0)
        g.press = null
        val m = when {
            e.morale > 0 -> L(" (Moral +${e.morale.toInt()})", " (Morale +${e.morale.toInt()})")
            e.morale < 0 -> L(" (Moral ${e.morale.toInt()})", " (Morale ${e.morale.toInt()})")
            else -> ""
        }
        return e.feedback + m
    }
}
