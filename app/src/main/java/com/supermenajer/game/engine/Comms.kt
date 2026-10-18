package com.supermenajer.game.engine

import com.supermenajer.game.model.Team
import kotlin.random.Random

/** Türkçe maç anlatımı. İsimlere ek gelmeyecek şekilde cümleler kuruldu. */
object Comms {
    private fun List<String>.r(rnd: Random) = this[rnd.nextInt(size)]

    fun kickoff(h: Team, a: Team, rnd: Random) = listOf(
        "Hakem düdüğü çaldı, ${h.name} - ${a.name} maçı başladı!",
        "Tribünler dolu, atmosfer müthiş! ${h.name} ile ${a.name} karşı karşıya.",
        "Ve maç başlıyor! Ev sahibi ${h.name}, konuk ${a.name}.",
    ).r(rnd)

    fun goal(shooter: String, assist: String?, penalty: Boolean, header: Boolean, rnd: Random): String {
        if (penalty) return listOf(
            "GOOOL! $shooter penaltıyı soğukkanlılıkla gole çevirdi!",
            "GOOOL! Penaltı noktasında hata yok: $shooter!",
        ).r(rnd)
        val base = if (header) listOf(
            "GOOOL! $shooter kafayı vurdu, top ağlarda!",
            "GOOOL! Köşe vuruşunda en yükseğe $shooter çıktı ve fileleri havalandırdı!",
        ) else listOf(
            "GOOOL! $shooter topu ağlara gönderdi!",
            "GOOOL! Muhteşem bir vuruş! $shooter kaleciye şans tanımadı!",
            "GOOOL! $shooter ceza sahasında doğru yerdeydi ve affetmedi!",
            "GOOOL! Uzak köşeye nefis bir plase! İmza: $shooter!",
            "GOOOL! Stat yıkılıyor! $shooter sahneye çıktı!",
            "GOOOL! $shooter yerden sert vurdu, top filelerle buluştu!",
        )
        val g = base.r(rnd)
        return if (assist != null) "$g Asist: $assist." else g
    }

    fun save(shooter: String, gk: String, penalty: Boolean, rnd: Random) = if (penalty) {
        "KURTARDI! $gk penaltıda $shooter karşısında gole izin vermedi!"
    } else listOf(
        "$shooter şutunu çekti, $gk iyi uzanarak kurtardı.",
        "Tehlikeli şut! $shooter denedi ama $gk yerinde.",
        "$gk müthiş bir refleksle golü önledi! Şutu atan: $shooter.",
        "$shooter vurdu, kaleci $gk topu kontrol etti.",
    ).r(rnd)

    fun post(shooter: String, rnd: Random) = listOf(
        "DİREK! $shooter çok yaklaştı, top direkten döndü!",
        "Üst direk! $shooter golü kıl payı kaçırdı!",
    ).r(rnd)

    fun miss(shooter: String, penalty: Boolean, rnd: Random) = if (penalty) {
        "Penaltı kaçtı! $shooter topu auta gönderdi!"
    } else listOf(
        "$shooter vurdu, top üstten auta gitti.",
        "$shooter şansını denedi ama isabet yok.",
        "Gol fırsatı! $shooter kaleyi bulamadı.",
        "$shooter uzaktan denedi, top yandan dışarıda.",
        "Savunma son anda araya girdi, $shooter şutu bloke edildi.",
    ).r(rnd)

    fun corner(t: Team, rnd: Random) = listOf(
        "${t.name} korner kazandı, ceza sahası kalabalık...",
        "Korner atışı ${t.name} lehine.",
    ).r(rnd)

    fun freeKick(t: Team, rnd: Random) = listOf(
        "Tehlikeli bölgede serbest vuruş! ${t.name} için fırsat.",
        "Ceza sahası yayına yakın frikik, ${t.name} hazırlanıyor.",
    ).r(rnd)

    fun penalty(t: Team, rnd: Random) = listOf(
        "PENALTI! Hakem beyaz noktayı gösterdi, ${t.name} lehine!",
        "Ceza sahasında düşürüldü... ve PENALTI! ${t.name} büyük fırsat yakaladı.",
    ).r(rnd)

    fun yellow(name: String, rnd: Random) = listOf(
        "Sarı kart: $name. Sert bir müdahale.",
        "Hakem cebine gitti, $name sarı kart gördü.",
        "$name taktik faul yaptı ve sarı kartla cezalandırıldı.",
    ).r(rnd)

    fun red(name: String, secondYellow: Boolean) =
        if (secondYellow) "İKİNCİ SARIDAN KIRMIZI! $name oyundan atıldı!"
        else "DİREKT KIRMIZI KART! $name çok sert girdi ve oyundan atıldı!"

    fun injury(name: String, rnd: Random) = listOf(
        "Kötü haber: $name sakatlandı ve oyuna devam edemeyecek.",
        "$name yerde kaldı, sağlık ekibi sahada... Devam edemeyecek gibi.",
    ).r(rnd)

    fun sub(t: Team, inName: String, outName: String) = "Oyuncu değişikliği (${t.short}): $inName girdi, $outName çıktı."

    fun filler(t: Team, name: String, rnd: Random) = listOf(
        "${t.name} topu ayağında tutuyor, sabırlı bir oyun.",
        "Orta sahada sert mücadeleler var.",
        "$name kanattan ilerliyor, orta açmak istiyor...",
        "$name ara pas denedi ama savunma araya girdi.",
        "${t.name} baskıyı artırdı, rakip kendi yarı sahasına çekildi.",
        "Tribünlerden büyük destek var!",
        "$name rakibini çalımladı, seyirciler ayakta!",
        "${t.name} hızlı bir kontra atağa çıktı ama ofsayt bayrağı kalktı.",
        "$name uzun bir topla oyunu kanat değiştirdi.",
        "Oyun biraz durgunlaştı, takımlar birbirini kolluyor.",
    ).r(rnd)

    fun half(h: Side, a: Side) = "İlk yarı sona erdi: ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}"
    fun full(h: Side, a: Side) = "Maç bitti! ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}"
}
