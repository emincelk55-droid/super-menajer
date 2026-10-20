package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import kotlin.random.Random

/**
 * Sakatlık türleri ve sağlık ekibi kararları: tam tedavi, uzman tedavisi (para karşılığı daha hızlı)
 * ya da riskli hızlı dönüş (süre yarıya iner ama birkaç hafta yeniden sakatlanma riski artar).
 */
object Medical {
    /** Tür ve hafta aralığı. */
    val types = listOf(
        "Kas zorlanması" to 1..2, "Hamstring sakatlığı" to 2..4, "Ayak bileği burkulması" to 2..4,
        "Kasık sakatlığı" to 2..5, "Diz bağı zedelenmesi" to 4..8, "Metatars kırığı" to 6..10,
    )

    fun specialistCost(p: Player): Long = (300_000L + p.value / 40).coerceAtMost(2_500_000L)

    /** Maçtan sonra: 3 hafta ve üzeri sakatlıklar için sağlık ekibi karar ister. */
    fun afterMatch(g: Game, injured: List<Player>) {
        for (p in injured) {
            if (p.teamId != g.userTeamId || p.injuredWeeks < 3 || p.id in g.medical) continue
            g.medical += p.id
            g.addNews(
                "🚑 Sağlık raporu: ${p.name}",
                "${p.name}: ${p.injuryType.ifEmpty { "sakatlık" }}, tahmini ${p.injuredWeeks} hafta. Sağlık ekibi tedavi yöntemi için kararını bekliyor (ana sayfa).",
            )
        }
    }

    /** 0 tam tedavi, 1 uzman tedavisi, 2 riskli hızlı dönüş. */
    fun decide(g: Game, p: Player, choice: Int, rnd: Random = Random.Default): String {
        g.medical.remove(p.id)
        return when (choice) {
            1 -> {
                val c = specialistCost(p)
                if (g.userTeam.budget < c) return "Bütçe yetmiyor: uzman tedavisi ${money(c)}.".also { g.medical += p.id }
                Economy.earn(g.userTeam, "Sağlık giderleri", -c)
                val cut = if (rnd.nextDouble() < 0.7) 2 else 1
                p.injuredWeeks = (p.injuredWeeks - cut).coerceAtLeast(1)
                "${p.name} yurtdışında uzman tedavisi görecek (${money(c)}). Dönüş: ${p.injuredWeeks} hafta."
            }
            2 -> {
                p.injuredWeeks = (p.injuredWeeks + 1) / 2
                p.fragileWeeks = 6
                "${p.name} hızlandırılmış programla ${p.injuredWeeks} haftada dönecek. Sağlık ekibi uyarıyor: 6 hafta yeniden sakatlanma riski yüksek!"
            }
            else -> "${p.name} için tam tedavi uygulanacak. Dönüş: ${p.injuredWeeks} hafta."
        }.also { g.addNews("🚑 Sağlık kararı", it) }
    }
}
