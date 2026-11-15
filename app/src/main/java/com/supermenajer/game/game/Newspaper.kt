package com.supermenajer.game.game

import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Game
import kotlin.random.Random

/**
 * Haftalık spor gazetesi: manşetler oyunun gerçek durumundan yazılır (son maçın, lig tablosu, gol krallığı,
 * transferler, yönetimin sabrı) ve transfer söylentileri eklenir. Aynı hafta içinde aynı sayı çıkar
 * (rastgelelik sezon ve haftadan tohumlanır); kayıt dosyasına bir şey yazmaz.
 */
object Newspaper {
    /** [tag]: MAÇ, LİG, GOL KRALLIĞI, TRANSFER, SÖYLENTİ, KULÜP. */
    class Story(val tag: String, val headline: String, val body: String)

    class Edition(val masthead: String, val date: String, val number: Int, val main: Story, val side: List<Story>)

    private val papersCache = LangCached { listOf(L("SÜPER SPOR", "SUPER SPORT"), L("FUTBOL MANŞET", "FOOTBALL HEADLINES"), L("TRİBÜN GAZETESİ", "THE TERRACE GAZETTE")) }
    private val papers get() = papersCache.get()

    fun edition(g: Game): Edition {
        val rnd = Random(g.season * 1000 + g.week)
        val us = g.userTeam
        val stories = mutableListOf<Story>()
        // Derbi haftası manşete çıkar; son maç yan habere iner
        val derby = Talks.upcomingDerby(g)
        val derbyStory = derby?.let { (f, days) ->
            val opp = g.team(if (f.home == us.id) f.away else f.home)
            Story(
                L("DERBİ", "DERBY"), listOf(L("DERBİ ATEŞİ!", "DERBY FEVER!"), L("ŞEHİR İKİYE BÖLÜNDÜ", "A CITY DIVIDED"), L("BÜYÜK HESAPLAŞMA", "THE BIG SHOWDOWN")).random(rnd),
                L("${Talks.derbyName(g, f)}: ${us.name} ile ${opp.name} ${if (days == 0L) "bugün" else "$days gün sonra"} karşı karşıya. Biletler tükendi, tribünlerde dev koreografi hazırlanıyor.", "${Talks.derbyName(g, f)}: ${us.name} face ${opp.name} ${if (days == 0L) "today" else "in $days days"}. Tickets are sold out and a giant choreography is being prepared in the stands."),
            )
        }
        derbyStory?.let { lastMatchStory(g, rnd)?.let { m -> stories += m } }
        val main = derbyStory ?: lastMatchStory(g, rnd) ?: previewStory(g, rnd)
            ?: Story(L("KULÜP", "CLUB"), L("${us.name.uppercase()} YENİ SEZONA HAZIRLANIYOR", "${us.name.uppercase()} PREPARE FOR THE NEW SEASON"), L("Teknik direktör ${g.managerName} kampta takımı şekillendiriyor.", "Manager ${g.managerName} is shaping the team at the training camp."))

        // Lig: zirve ve küme hattı
        val table = g.table(us.division)
        if (table.isNotEmpty() && table.first().played > 0) {
            val top = table[0]
            val gap = top.points - (table.getOrNull(1)?.points ?: top.points)
            val gapText = if (gap == 0) L("ikinciyle puanlar eşit", "level on points with second place") else L("fark $gap puan", "a $gap-point gap")
            stories += Story(
                L("LİG", "LEAGUE"),
                if (top.id == us.id) L("ZİRVE BİZİM!", "WE'RE TOP!") else L("${top.name} zirvede", "${top.name} on top"),
                if (top.id == us.id) L("${us.name} ${top.points} puanla lider; $gapText.", "${us.name} lead with ${top.points} points; $gapText.")
                else L("${top.name} ${top.points} puanla lider ($gapText). ${us.name} ${g.position(us.id)}. sırada.", "${top.name} lead with ${top.points} points ($gapText). ${us.name} are ${g.position(us.id)}."),
            )
            val pos = g.position(us.id)
            if (pos >= table.size - 2) {
                stories += Story(L("KULÜP", "CLUB"), L("Küme hattında alarm!", "Relegation alarm!"), L("${us.name} ${pos}. sırada. Taraftar sonuç bekliyor, yönetim sessiz ama tedirgin.", "${us.name} are ${pos}. The fans want results; the board is quiet but uneasy."))
            }
        }

        // Gol krallığı
        val scorers = g.players.filter { it.teamId >= 0 && g.teamOrNull(it.teamId)?.division == us.division && it.goals > 0 }
            .sortedByDescending { it.goals }.take(3)
        if (scorers.isNotEmpty()) {
            val k = scorers[0]
            stories += Story(
                L("GOL KRALLIĞI", "GOLDEN BOOT"),
                L("${k.name.substringAfterLast(' ')} krallık yarışında önde", "${k.name.substringAfterLast(' ')} leads the scoring race"),
                scorers.joinToString(" · ") { "${it.name} (${g.teamOrNull(it.teamId)?.short ?: ""}) ${it.goals}" },
            )
        }

        // Son transfer
        g.transfers.firstOrNull()?.split("|")?.takeIf { it.size >= 4 }?.let { t ->
            val price = t[3].toLongOrNull() ?: 0
            stories += Story(L("TRANSFER", "TRANSFER"), L("${t[0]} ${t[2]} yolunda", "${t[0]} on his way to ${t[2]}"), if (price > 0) L("${t[1]} ile anlaşma ${money(price)} bedelle tamamlandı.", "The deal with ${t[1]} was completed for ${money(price)}.") else L("Oyuncu bedelsiz olarak imzayı attı.", "The player signed on a free."))
        }

        stories += rumors(g, rnd)

        // Yönetim
        when {
            g.confidence < 30 -> stories += Story(L("KULÜP", "CLUB"), L("${g.managerName}'ın koltuğu sallanıyor", "${g.managerName}'s job is at risk"), L("Yönetimin güveni %${g.confidence.toInt()}'e düştü. Önümüzdeki maçlar kritik.", "Board confidence has fallen to %${g.confidence.toInt()}. The next matches are crucial."))
            g.confidence > 85 -> stories += Story(L("KULÜP", "CLUB"), L("Yönetimden tam destek", "Full backing from the board"), L("Başkan: \"${g.managerName} ile uzun soluklu bir proje kuruyoruz.\"", "Chairman: \"We're building a long-term project with ${g.managerName}.\""))
        }
        return Edition(papers[(g.season + us.id) % papers.size], GameCalendar.format(g.date.coerceAtLeast(1)), g.week, main, stories.take(5))
    }

    private fun lastFixture(g: Game): Fixture? =
        g.fixtures.filter { it.hg >= 0 && it.involves(g.userTeamId) }.maxByOrNull { it.week }

    private fun lastMatchStory(g: Game, rnd: Random): Story? {
        val f = lastFixture(g) ?: return null
        val us = g.userTeam
        val home = f.home == us.id
        val opp = g.team(if (home) f.away else f.home)
        val gf = if (home) f.hg else f.ag
        val ga = if (home) f.ag else f.hg
        val score = "${g.team(f.home).short} ${f.hg}-${f.ag} ${g.team(f.away).short}"
        return when {
            gf - ga >= 3 -> Story(L("MAÇ", "MATCH"), listOf(L("GOL YAĞMURU!", "GOAL FEST!"), L("FARKA KOŞTU!", "A ROUT!"), L("ŞOV GİBİ MAÇ!", "WHAT A SHOW!")).random(rnd), L("${us.name}, ${opp.name} karşısında $score'lik skorla adeta şov yaptı.", "${us.name} put on a show against ${opp.name} with a $score scoreline."))
            gf > ga -> Story(L("MAÇ", "MATCH"), listOf(L("ZAFER!", "VICTORY!"), L("3 PUAN CEPTE!", "THREE POINTS IN THE BAG!"), L("GÜLEN TARAF ${us.short}!", "${us.short} ALL SMILES!")).random(rnd), L("${us.name}, ${opp.name} engelini $score ile geçti. ${g.managerName}: \"Oyuncularımla gurur duyuyorum.\"", "${us.name} got past ${opp.name} $score. ${g.managerName}: \"I'm proud of my players.\""))
            gf == ga -> Story(L("MAÇ", "MATCH"), listOf(L("PUANLAR PAYLAŞILDI", "POINTS SHARED"), L("KAZANAN YOK", "NO WINNER"), L("BERABERE", "STALEMATE")).random(rnd), L("${us.name} ile ${opp.name} yenişemedi: $score.", "${us.name} and ${opp.name} couldn't be separated: $score."))
            ga - gf >= 3 -> Story(L("MAÇ", "MATCH"), listOf(L("KABUS GİBİ GECE!", "A NIGHTMARE NIGHT!"), L("BOZGUN!", "ROUTED!"), L("TARAFTAR ÖFKELİ", "FANS FURIOUS")).random(rnd), L("${us.name}, ${opp.name} karşısında $score'lik ağır yenilgi aldı. Gözler ${g.managerName}'da.", "${us.name} suffered a heavy $score defeat to ${opp.name}. All eyes are on ${g.managerName}."))
            else -> Story(L("MAÇ", "MATCH"), listOf(L("HÜSRAN", "DISAPPOINTMENT"), L("KÖTÜ SÜRPRİZ", "NASTY SURPRISE"), L("PUAN YOK", "NO POINTS")).random(rnd), L("${us.name}, ${opp.name} ${if (home) "karşısında" else "deplasmanında"} $score ile kaybetti.", "${us.name} lost $score ${if (home) "to" else "away to"} ${opp.name}."))
        }
    }

    private fun previewStory(g: Game, rnd: Random): Story? {
        val f = g.nextUserFixture() ?: return null
        val opp = g.team(if (f.home == g.userTeamId) f.away else f.home)
        return Story(L("MAÇ", "MATCH"), listOf(L("DEV RANDEVU", "BIG DATE"), L("GÖZLER BU MAÇTA", "ALL EYES ON THIS MATCH"), L("HAZIRLIKLAR TAMAM", "PREPARATIONS COMPLETE")).random(rnd), L("${g.userTeam.name}, ${opp.name} karşısında sezonu iyi açmak istiyor.", "${g.userTeam.name} want to start the season well against ${opp.name}."))
    }

    /** Transfer söylentileri: yıldızımıza talip olanlar ve bizimle anılan oyuncular. */
    private fun rumors(g: Game, rnd: Random): List<Story> {
        val us = g.userTeam
        val out = mutableListOf<Story>()
        val mine = g.squad(us.id).filter { it.apps >= 3 }
        val star = mine.maxByOrNull { it.goals * 2 + it.assists + it.avgRating } ?: g.squad(us.id).maxByOrNull { it.overall }
        val giants = g.teams.filter { it.id != us.id && it.reputation >= us.reputation + 5 }
        if (star != null && giants.isNotEmpty()) {
            val suitor = giants.random(rnd)
            out += Story(
                L("SÖYLENTİ", "RUMOUR"),
                L("${suitor.name}, ${star.name.substringAfterLast(' ')} için harekete geçti", "${suitor.name} make a move for ${star.name.substringAfterLast(' ')}"),
                L("İddiaya göre ${suitor.short} gözlemcileri ${star.name}'i son maçlarda yakından izledi. Kulübümüz satışa kapalı.", "Reportedly ${suitor.short} scouts have watched ${star.name} closely in recent matches. Our club says he's not for sale."),
            )
        }
        val budget = us.budget
        val target = g.players.filter { it.teamId >= 0 && it.teamId != us.id && it.age <= 27 && it.value in (budget / 6)..(budget) }
            .sortedByDescending { it.overall }.take(8).randomOrNull(rnd)
        if (target != null) {
            out += Story(
                L("SÖYLENTİ", "RUMOUR"),
                L("${target.name.substringAfterLast(' ')} ${us.short}'a mı geliyor?", "Is ${target.name.substringAfterLast(' ')} heading to ${us.short}?"),
                L("${g.teamOrNull(target.teamId)?.name ?: ""} forması giyen ${target.age} yaşındaki ${target.name}'in menajeri ${us.name} ile temasta.", "The agent of ${target.age}-year-old ${target.name} of ${g.teamOrNull(target.teamId)?.name ?: ""} is in contact with ${us.name}."),
            )
        }
        return out
    }

    private fun <T> List<T>.randomOrNull(rnd: Random): T? = if (isEmpty()) null else this[rnd.nextInt(size)]
}
