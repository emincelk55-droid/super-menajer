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

    private val papers = listOf("SÜPER SPOR", "FUTBOL MANŞET", "TRİBÜN GAZETESİ")

    fun edition(g: Game): Edition {
        val rnd = Random(g.season * 1000 + g.week)
        val us = g.userTeam
        val stories = mutableListOf<Story>()
        val main = lastMatchStory(g, rnd) ?: previewStory(g, rnd)
            ?: Story("KULÜP", "${us.name.uppercase()} YENİ SEZONA HAZIRLANIYOR", "Teknik direktör ${g.managerName} kampta takımı şekillendiriyor.")

        // Lig: zirve ve küme hattı
        val table = g.table(us.division)
        if (table.isNotEmpty() && table.first().played > 0) {
            val top = table[0]
            val gap = top.points - (table.getOrNull(1)?.points ?: top.points)
            val gapText = if (gap == 0) "ikinciyle puanlar eşit" else "fark $gap puan"
            stories += Story(
                "LİG",
                if (top.id == us.id) "ZİRVE BİZİM!" else "${top.name} zirvede",
                if (top.id == us.id) "${us.name} ${top.points} puanla lider; $gapText."
                else "${top.name} ${top.points} puanla lider ($gapText). ${us.name} ${g.position(us.id)}. sırada.",
            )
            val pos = g.position(us.id)
            if (pos >= table.size - 2) {
                stories += Story("KULÜP", "Küme hattında alarm!", "${us.name} ${pos}. sırada. Taraftar sonuç bekliyor, yönetim sessiz ama tedirgin.")
            }
        }

        // Gol krallığı
        val scorers = g.players.filter { it.teamId >= 0 && g.teamOrNull(it.teamId)?.division == us.division && it.goals > 0 }
            .sortedByDescending { it.goals }.take(3)
        if (scorers.isNotEmpty()) {
            val k = scorers[0]
            stories += Story(
                "GOL KRALLIĞI",
                "${k.name.substringAfterLast(' ')} krallık yarışında önde",
                scorers.joinToString(" · ") { "${it.name} (${g.teamOrNull(it.teamId)?.short ?: ""}) ${it.goals}" },
            )
        }

        // Son transfer
        g.transfers.firstOrNull()?.split("|")?.takeIf { it.size >= 4 }?.let { t ->
            val price = t[3].toLongOrNull() ?: 0
            stories += Story("TRANSFER", "${t[0]} ${t[2]} yolunda", if (price > 0) "${t[1]} ile anlaşma ${money(price)} bedelle tamamlandı." else "Oyuncu bedelsiz olarak imzayı attı.")
        }

        stories += rumors(g, rnd)

        // Yönetim
        when {
            g.confidence < 30 -> stories += Story("KULÜP", "${g.managerName}'ın koltuğu sallanıyor", "Yönetimin güveni %${g.confidence.toInt()}'e düştü. Önümüzdeki maçlar kritik.")
            g.confidence > 85 -> stories += Story("KULÜP", "Yönetimden tam destek", "Başkan: \"${g.managerName} ile uzun soluklu bir proje kuruyoruz.\"")
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
            gf - ga >= 3 -> Story("MAÇ", listOf("GOL YAĞMURU!", "FARKA KOŞTU!", "ŞOV GİBİ MAÇ!").random(rnd), "${us.name}, ${opp.name} karşısında $score'lik skorla adeta şov yaptı.")
            gf > ga -> Story("MAÇ", listOf("ZAFER!", "3 PUAN CEPTE!", "GÜLEN TARAF ${us.short}!").random(rnd), "${us.name}, ${opp.name} engelini $score ile geçti. ${g.managerName}: \"Oyuncularımla gurur duyuyorum.\"")
            gf == ga -> Story("MAÇ", listOf("PUANLAR PAYLAŞILDI", "KAZANAN YOK", "BERABERE").random(rnd), "${us.name} ile ${opp.name} yenişemedi: $score.")
            ga - gf >= 3 -> Story("MAÇ", listOf("KABUS GİBİ GECE!", "BOZGUN!", "TARAFTAR ÖFKELİ").random(rnd), "${us.name}, ${opp.name} karşısında $score'lik ağır yenilgi aldı. Gözler ${g.managerName}'da.")
            else -> Story("MAÇ", listOf("HÜSRAN", "KÖTÜ SÜRPRİZ", "PUAN YOK").random(rnd), "${us.name}, ${opp.name} ${if (home) "karşısında" else "deplasmanında"} $score ile kaybetti.")
        }
    }

    private fun previewStory(g: Game, rnd: Random): Story? {
        val f = g.nextUserFixture() ?: return null
        val opp = g.team(if (f.home == g.userTeamId) f.away else f.home)
        return Story("MAÇ", listOf("DEV RANDEVU", "GÖZLER BU MAÇTA", "HAZIRLIKLAR TAMAM").random(rnd), "${g.userTeam.name}, ${opp.name} karşısında sezonu iyi açmak istiyor.")
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
                "SÖYLENTİ",
                "${suitor.name}, ${star.name.substringAfterLast(' ')} için harekete geçti",
                "İddiaya göre ${suitor.short} gözlemcileri ${star.name}'i son maçlarda yakından izledi. Kulübümüz satışa kapalı.",
            )
        }
        val budget = us.budget
        val target = g.players.filter { it.teamId >= 0 && it.teamId != us.id && it.age <= 27 && it.value in (budget / 6)..(budget) }
            .sortedByDescending { it.overall }.take(8).randomOrNull(rnd)
        if (target != null) {
            out += Story(
                "SÖYLENTİ",
                "${target.name.substringAfterLast(' ')} ${us.short}'a mı geliyor?",
                "${g.teamOrNull(target.teamId)?.name ?: ""} forması giyen ${target.age} yaşındaki ${target.name}'in menajeri ${us.name} ile temasta.",
            )
        }
        return out
    }

    private fun <T> List<T>.randomOrNull(rnd: Random): T? = if (isEmpty()) null else this[rnd.nextInt(size)]
}
