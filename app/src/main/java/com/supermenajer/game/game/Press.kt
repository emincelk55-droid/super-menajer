package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.PressQuestion
import kotlin.random.Random

/** Maç sonrası basın toplantıları. Cevaplar takım morali ve yönetim güvenini etkiler. */
object Press {
    /** Cevap etkisi: (kadro morali, yönetim güveni, geri bildirim). */
    private class Effect(val morale: Double, val confidence: Double, val feedback: String)

    private class Template(val text: (String) -> String, val answers: List<Pair<String, Effect>>)

    private val templates = listOf(
        // 0: farklı galibiyet
        Template({ "Hocam, $it karşısında farklı galibiyet! Bu takımın hedefi ne?" }, listOf(
            "Ayaklarımız yere basmalı, maç maç gideceğiz." to Effect(2.0, 1.0, "Olgun bir açıklama. Oyuncular odaklı kalıyor."),
            "Bu takım şampiyonluğa oynar!" to Effect(5.0, 3.0, "Taraftar coştu, oyuncuların özgüveni tavan!"),
            "Rakip çok kötüydü, abartmayalım." to Effect(-1.0, -1.0, "Oyuncular galibiyetin küçümsenmesinden pek hoşlanmadı."),
        )),
        // 1: galibiyet
        Template({ "$it galibiyetinin anahtarı neydi?" }, listOf(
            "Oyuncularımın mücadelesi." to Effect(4.0, 0.0, "Oyuncular övgüden çok memnun."),
            "Taktiğimiz tuttu, plan işledi." to Effect(1.0, 2.0, "Yönetim taktik vizyonunu beğendi."),
            "Henüz iyi oynamıyoruz, daha çok çalışmalıyız." to Effect(-2.0, 1.0, "Oyuncular biraz bozuldu ama mesaj alındı."),
        )),
        // 2: beraberlik
        Template({ "$it ile berabere kaldınız. Memnun musunuz?" }, listOf(
            "Bir puan da değerli, devam ediyoruz." to Effect(1.0, 0.0, "Sakin bir açıklama, ortam huzurlu."),
            "Hayır! Kazanmamız gerekiyordu, oyunculardan memnun değilim." to Effect(-4.0, 2.0, "Yönetim sert tavrını beğendi, oyuncular gergin."),
            "Hakem kararları sonucu belirledi." to Effect(2.0, -2.0, "Oyuncular arkalarında durmanı sevdi, federasyon ise rahatsız."),
        )),
        // 3: mağlubiyet
        Template({ "$it yenilgisinin sorumlusu kim?" }, listOf(
            "Sorumluluk tamamen bende." to Effect(3.0, -1.0, "Oyuncular hocalarına daha çok bağlandı."),
            "Oyuncular sahada yoktu." to Effect(-6.0, 1.0, "Soyunma odasında hava çok gergin!"),
            "Şanssızdık, böyle maçlar olur." to Effect(1.0, 0.0, "Kimse fazla üzerine düşmedi."),
        )),
        // 4: ağır mağlubiyet
        Template({ "$it karşısında ağır yenilgi... Taraftar istifa diyor, ne diyeceksiniz?" }, listOf(
            "Bu takımı ayağa kaldıracağım, söz veriyorum." to Effect(2.0, 2.0, "Kararlı duruşun yönetimi biraz rahatlattı."),
            "İstifa etmeyeceğim, hedeflerimiz değişmedi." to Effect(0.0, -2.0, "Yönetim cevabını soğuk karşıladı."),
            "Bu kadroyla ancak bu kadar." to Effect(-5.0, -5.0, "Hem oyuncular hem yönetim çok kızgın!"),
        )),
        // 5: Avrupa
        Template({ "Avrupa'da $it maçı sonrası düşünceleriniz?" }, listOf(
            "Türk futbolunu en iyi şekilde temsil edeceğiz." to Effect(3.0, 1.0, "Taraftar ve oyuncular gururlu."),
            "Ligi önceliğimiz yapacağız." to Effect(-1.0, 1.0, "Bazı oyuncular Avrupa'da daha çok şans istiyordu."),
            "Avrupa'da büyük işler yapacağız!" to Effect(4.0, 2.0, "Heyecan dorukta!"),
        )),
    )

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
        val e = templates.getOrNull(q.kind)?.answers?.getOrNull(index)?.second ?: return ""
        for (p in g.squad(g.userTeamId)) p.morale = (p.morale + e.morale).coerceIn(5.0, 100.0)
        g.confidence = (g.confidence + e.confidence).coerceIn(0.0, 100.0)
        g.press = null
        val m = when {
            e.morale > 0 -> " (Moral +${e.morale.toInt()})"
            e.morale < 0 -> " (Moral ${e.morale.toInt()})"
            else -> ""
        }
        return e.feedback + m
    }
}
