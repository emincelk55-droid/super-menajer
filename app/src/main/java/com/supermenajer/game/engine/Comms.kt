package com.supermenajer.game.engine

import com.supermenajer.game.game.L
import com.supermenajer.game.model.Team
import kotlin.random.Random

/** Türkçe maç anlatımı. İsimlere ek gelmeyecek şekilde cümleler kuruldu. */
object Comms {
    private fun List<String>.r(rnd: Random) = this[rnd.nextInt(size)]

    fun kickoff(h: Team, a: Team, rnd: Random) = listOf(
        L("Hakem düdüğü çaldı, ${h.name} - ${a.name} maçı başladı!", "The referee blows the whistle, ${h.name} v ${a.name} is under way!"),
        L("Tribünler dolu, atmosfer müthiş! ${h.name} ile ${a.name} karşı karşıya.", "The stands are full and the atmosphere is electric! ${h.name} face ${a.name}."),
        L("Ve maç başlıyor! Ev sahibi ${h.name}, konuk ${a.name}.", "And we're off! ${h.name} at home, ${a.name} the visitors."),
    ).r(rnd)

    /**
     * Golün maçtaki anlamı: hat-trick, eşitlik, öne geçiş, fark, son dakika. Boşsa sıradan bir gol.
     * [scorerGoals] golcünün bu maçtaki gol sayısı (bu gol dahil), skorlar gol sonrası.
     */
    fun goalContext(minute: Int, scorerGoals: Int, forGoals: Int, againstGoals: Int, rnd: Random): String {
        val parts = mutableListOf<String>()
        when {
            scorerGoals == 3 -> parts += listOf(L("HAT-TRICK! 🎩 Maçın yıldızı o!", "HAT-TRICK! 🎩 He's the star of the show!"), L("Üçüncü golü! Hat-trick tamam! 🎩", "His third! Hat-trick complete! 🎩")).random(rnd)
            scorerGoals == 2 -> parts += listOf(L("Maçtaki ikinci golü!", "His second of the match!"), L("Bugün durdurulamıyor, ikinci gol!", "Unstoppable today, that's two!")).random(rnd)
            scorerGoals >= 4 -> parts += L("İnanılmaz! Bu maçtaki $scorerGoals. golü!", "Incredible! His $scorerGoals. goal of the match!")
        }
        val diff = forGoals - againstGoals
        when {
            diff == 0 -> parts += listOf(L("Skor eşitlendi!", "All square!"), L("Maç yeniden başa döndü!", "We're back to square one!"), L("Beraberlik golü!", "The equaliser!")).random(rnd)
            diff == 1 && forGoals == 1 -> parts += listOf(L("İlk gol geldi!", "The first goal arrives!"), L("Kilit açıldı!", "The deadlock is broken!")).random(rnd)
            diff == 1 -> parts += listOf(L("Yeniden öne geçtiler!", "They're back in front!"), L("Eşitlik bozuldu, öne geçtiler!", "The tie is broken, they lead!")).random(rnd)
            diff >= 3 -> parts += listOf(L("Fark $diff oldu, maç koptu!", "The gap is $diff, the game is gone!"), L("Gol yağmuru sürüyor!", "The goals keep coming!")).random(rnd)
            diff == 2 -> parts += listOf(L("Fark ikiye çıktı!", "The lead is two!"), L("Rahatladılar, fark iki!", "Some breathing room, two up!")).random(rnd)
            diff == -1 -> parts += listOf(L("Farkı bire indirdiler!", "They pull one back!"), L("Maça ortak oldular!", "They're back in it!")).random(rnd)
        }
        if (minute >= 88) parts.add(0, listOf(L("SON DAKİKA GOLÜ!", "LAST-MINUTE GOAL!"), L("Son nefeste!", "At the death!"), L("Uzatmalarda dram!", "Drama in stoppage time!")).random(rnd))
        else if (minute <= 3) parts.add(0, L("Daha ilk dakikalarda!", "In the opening minutes!"))
        return parts.joinToString(" ")
    }

    fun goal(shooter: String, assist: String?, penalty: Boolean, header: Boolean, rnd: Random): String {
        if (penalty) return listOf(
            L("GOOOL! $shooter penaltıyı soğukkanlılıkla gole çevirdi!", "GOAL! $shooter coolly converts the penalty!"),
            L("GOOOL! Penaltı noktasında hata yok: $shooter!", "GOAL! No mistake from the spot: $shooter!"),
        ).r(rnd)
        val base = if (header) listOf(
            L("GOOOL! $shooter kafayı vurdu, top ağlarda!", "GOAL! $shooter heads it home!"),
            L("GOOOL! Köşe vuruşunda en yükseğe $shooter çıktı ve fileleri havalandırdı!", "GOAL! $shooter rises highest from the corner and ripples the net!"),
        ) else listOf(
            L("GOOOL! $shooter topu ağlara gönderdi!", "GOAL! $shooter sends it into the net!"),
            L("GOOOL! Muhteşem bir vuruş! $shooter kaleciye şans tanımadı!", "GOAL! What a strike! $shooter gives the keeper no chance!"),
            L("GOOOL! $shooter ceza sahasında doğru yerdeydi ve affetmedi!", "GOAL! $shooter was in the right place in the box and made no mistake!"),
            L("GOOOL! Uzak köşeye nefis bir plase! İmza: $shooter!", "GOAL! A lovely finish into the far corner! Signed: $shooter!"),
            L("GOOOL! Stat yıkılıyor! $shooter sahneye çıktı!", "GOAL! The stadium erupts! $shooter takes centre stage!"),
            L("GOOOL! $shooter yerden sert vurdu, top filelerle buluştu!", "GOAL! $shooter drills it low and hard into the net!"),
        )
        val g = base.r(rnd)
        return if (assist != null) L("$g Asist: $assist.", "$g Assist: $assist.") else g
    }

    /** Pozisyonun girişi: sonucu söylemeden gerilim kurar. */
    fun buildUp(shooter: String, assist: String?, header: Boolean, penalty: Boolean, setPiece: Boolean, rnd: Random): String = when {
        penalty -> listOf(
            L("$shooter topu beyaz noktaya koydu... geriliyor... koşuyor...", "$shooter places the ball on the spot... steps back... runs up..."),
            L("Stat nefesini tuttu... $shooter penaltıyı kullanacak... vuruyor...", "The stadium holds its breath... $shooter to take the penalty... he strikes..."),
        ).r(rnd)
        header -> listOf(
            L("Korner kullanılıyor... top ceza sahasında... $shooter yükseldi...", "The corner comes in... the ball is in the box... $shooter rises..."),
            L("${assist ?: "Kanattan"} ortaladı... ceza sahası karışık... $shooter kafayı vurdu...", "${assist ?: "From the wing"} crosses... chaos in the box... $shooter heads..."),
        ).r(rnd)
        setPiece -> listOf(
            L("Serbest vuruşun başında $shooter... baraj kuruldu... vuruyor...", "$shooter stands over the free kick... the wall is set... he strikes..."),
            L("$shooter frikiği kullanacak... aldığı mesafe uzun... ve vuruş...", "$shooter to take the free kick... a long run-up... and the shot..."),
        ).r(rnd)
        assist != null -> listOf(
            L("$assist harika bir ara pas attı... $shooter kaleciyle karşı karşıya...", "$assist plays a superb through ball... $shooter is one on one with the keeper..."),
            L("$assist sağ kanattan ortaladı... $shooter ceza sahasında...", "$assist crosses from the right... $shooter in the box..."),
            L("$assist ile $shooter verkaç yaptı... $shooter vuruyor...", "$assist and $shooter play a one-two... $shooter shoots..."),
            L("$assist sol kanatta rakibini geçti... içeri çevirdi... $shooter...", "$assist beats his man on the left... cuts it back... $shooter..."),
            L("Hızlı kontra! $assist topu $shooter önüne bıraktı...", "Quick counter! $assist lays it into the path of $shooter..."),
        ).r(rnd)
        else -> listOf(
            L("$shooter topla ilerliyor... bir kişiyi geçti... ceza sahasında...", "$shooter drives forward... beats one... into the box..."),
            L("$shooter uzaktan şansını deneyecek gibi... vuruyor...", "$shooter looks like he'll try his luck from distance... he shoots..."),
            L("Savunmada hata! Top $shooter önünde kaldı...", "A defensive error! The ball falls to $shooter..."),
            L("$shooter rakibinin arkasına sarktı... sadece kaleci var...", "$shooter gets in behind... only the keeper to beat..."),
        ).r(rnd)
    }

    fun save(shooter: String, gk: String, penalty: Boolean, rnd: Random, header: Boolean = false) = if (penalty) {
        L("KURTARDI! $gk penaltıda $shooter karşısında gole izin vermedi!", "SAVED! $gk denies $shooter from the spot!")
    } else if (header) listOf(
        L("$shooter kafayla vurdu, $gk çizgide çıkardı!", "$shooter heads it, $gk clears it off the line!"),
        L("Kafa vuruşu kaleyi buldu ama $gk yerinde. Vuruşu yapan: $shooter.", "The header is on target but $gk is there. Header by $shooter."),
    ).r(rnd) else listOf(
        L("$shooter şutunu çekti, $gk iyi uzanarak kurtardı.", "$shooter lets fly, $gk stretches to save."),
        L("Tehlikeli şut! $shooter denedi ama $gk yerinde.", "Dangerous shot! $shooter tries but $gk is there."),
        L("$gk müthiş bir refleksle golü önledi! Şutu atan: $shooter.", "$gk with a stunning reflex save! The shot came from $shooter."),
        L("$shooter vurdu, kaleci $gk topu kontrol etti.", "$shooter shoots, keeper $gk gathers."),
    ).r(rnd)

    fun post(shooter: String, rnd: Random) = listOf(
        L("DİREK! $shooter çok yaklaştı, top direkten döndü!", "POST! $shooter so close, it comes back off the post!"),
        L("Üst direk! $shooter golü kıl payı kaçırdı!", "The crossbar! $shooter misses by a whisker!"),
    ).r(rnd)

    fun miss(shooter: String, penalty: Boolean, rnd: Random) = if (penalty) {
        L("Penaltı kaçtı! $shooter topu auta gönderdi!", "Penalty missed! $shooter puts it wide!")
    } else listOf(
        L("$shooter vurdu, top üstten auta gitti.", "$shooter shoots, it goes over the bar."),
        L("$shooter şansını denedi ama isabet yok.", "$shooter tries his luck but it's off target."),
        L("Gol fırsatı! $shooter kaleyi bulamadı.", "Chance! $shooter can't find the target."),
        L("$shooter uzaktan denedi, top yandan dışarıda.", "$shooter tries from distance, wide of the post."),
        L("Savunma son anda araya girdi, $shooter şutu bloke edildi.", "The defence gets a block in at the last moment, $shooter's shot is blocked."),
    ).r(rnd)

    fun corner(t: Team, rnd: Random) = listOf(
        L("${t.name} korner kazandı, ceza sahası kalabalık...", "${t.name} win a corner, the box is crowded..."),
        L("Korner atışı ${t.name} lehine.", "Corner to ${t.name}."),
    ).r(rnd)

    fun freeKick(t: Team, rnd: Random) = listOf(
        L("Tehlikeli bölgede serbest vuruş! ${t.name} için fırsat.", "Free kick in a dangerous area! A chance for ${t.name}."),
        L("Ceza sahası yayına yakın frikik, ${t.name} hazırlanıyor.", "Free kick near the edge of the box, ${t.name} are lining up."),
    ).r(rnd)

    fun penalty(t: Team, rnd: Random) = listOf(
        L("PENALTI! Hakem beyaz noktayı gösterdi, ${t.name} lehine!", "PENALTY! The referee points to the spot for ${t.name}!"),
        L("Ceza sahasında düşürüldü... ve PENALTI! ${t.name} büyük fırsat yakaladı.", "Brought down in the box... and it's a PENALTY! A big chance for ${t.name}."),
    ).r(rnd)

    fun yellow(name: String, rnd: Random) = listOf(
        L("Sarı kart: $name. Sert bir müdahale.", "Yellow card: $name. A strong challenge."),
        L("Hakem cebine gitti, $name sarı kart gördü.", "The referee reaches for his pocket, $name is booked."),
        L("$name taktik faul yaptı ve sarı kartla cezalandırıldı.", "$name commits a tactical foul and is shown a yellow card."),
    ).r(rnd)

    fun red(name: String, secondYellow: Boolean) =
        if (secondYellow) L("İKİNCİ SARIDAN KIRMIZI! $name oyundan atıldı!", "SECOND YELLOW, RED CARD! $name is sent off!")
        else L("DİREKT KIRMIZI KART! $name çok sert girdi ve oyundan atıldı!", "STRAIGHT RED CARD! $name went in far too hard and is sent off!")

    fun injury(name: String, rnd: Random) = listOf(
        L("Kötü haber: $name sakatlandı ve oyuna devam edemeyecek.", "Bad news: $name is injured and can't continue."),
        L("$name yerde kaldı, sağlık ekibi sahada... Devam edemeyecek gibi.", "$name stays down, the medics are on... He doesn't look able to continue."),
    ).r(rnd)

    fun sub(t: Team, inName: String, outName: String) = L("Oyuncu değişikliği (${t.short}): $inName girdi, $outName çıktı.", "Substitution (${t.short}): $inName on, $outName off.")

    fun filler(t: Team, name: String, rnd: Random) = listOf(
        L("${t.name} topu ayağında tutuyor, sabırlı bir oyun.", "${t.name} keep the ball, patient play."),
        L("Orta sahada sert mücadeleler var.", "Fierce battles in midfield."),
        L("$name kanattan ilerliyor, orta açmak istiyor...", "$name advances down the wing, looking to cross..."),
        L("$name ara pas denedi ama savunma araya girdi.", "$name tries a through ball but the defence cuts it out."),
        L("${t.name} baskıyı artırdı, rakip kendi yarı sahasına çekildi.", "${t.name} turn up the pressure, the opponents retreat into their own half."),
        L("Tribünlerden büyük destek var!", "Huge support from the stands!"),
        L("$name rakibini çalımladı, seyirciler ayakta!", "$name dribbles past his man, the fans are on their feet!"),
        L("${t.name} hızlı bir kontra atağa çıktı ama ofsayt bayrağı kalktı.", "${t.name} break quickly but the offside flag goes up."),
        L("$name uzun bir topla oyunu kanat değiştirdi.", "$name switches play with a long ball."),
        L("Oyun biraz durgunlaştı, takımlar birbirini kolluyor.", "The game has slowed down, the teams are cancelling each other out."),
    ).r(rnd)

    fun half(h: Side, a: Side) = L("İlk yarı sona erdi: ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}", "Half time: ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}")
    fun full(h: Side, a: Side) = L("Maç bitti! ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}", "Full time! ${h.team.name} ${h.goals} - ${a.goals} ${a.team.name}")
}
