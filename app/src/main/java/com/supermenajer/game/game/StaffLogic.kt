package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Staff
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/** Teknik ekip: kalite (1-20) oyunun farklı bölümlerini etkiler. */
object StaffLogic {
    const val ASSISTANT = 0
    const val COACH = 1
    const val PHYSIO = 2
    const val SCOUT = 3
    val roleNames = listOf("Yardımcı antrenör", "Antrenör", "Fizyoterapist", "Gözlemci")
    val roleIcons = listOf("🧠", "🏋️", "🩺", "🔍")
    val roleInfo = listOf(
        "Takım konuşmalarının ve basın toplantılarının etkisini, rakip raporunun kalitesini belirler.",
        "Oyuncuların antrenmanda gelişme hızını belirler.",
        "Antrenman sakatlıklarını azaltır, sakat oyuncuları daha hızlı iyileştirir.",
        "Rapor doğruluğunu ve haftalık öneri sayısını belirler. İyi gözlemci tanımadığın oyuncuları daha net görür.",
    )

    fun wageFor(q: Int): Long = ((1_500 * 1.17.pow(q - 1)) / 500).roundToLong() * 500

    private fun make(g: Game, role: Int, q: Int, rnd: Random): Staff {
        val nation = listOf("TR", "TR", "TR", "GB", "ES", "DE", "IT", "PT", "NL", "FR").random(rnd)
        val (name, nat) = Generator.personName(rnd, nation)
        return Staff(g.nextStaffId++, name, role, q.coerceIn(1, 20), rnd.nextInt(32, 66), wageFor(q), nat)
    }

    /** Kullanıcının o roldeki çalışanı (yoksa null). */
    fun of(g: Game, role: Int): Staff? = g.staff.firstOrNull { it.role == role }

    /** Kalite (çalışan yoksa 6: kulübün gönüllü yardımcıları). */
    fun q(g: Game, role: Int): Int = of(g, role)?.quality ?: 6

    fun wages(g: Game): Long = g.staff.sumOf { it.wage }

    /** Kulübün itibarına uygun başlangıç ekibi. */
    fun hireDefaults(g: Game, rnd: Random = Random.Default) {
        val base = (g.userTeam.reputation / 7.5).toInt().coerceIn(4, 13)
        g.staff = (0..3).map { make(g, it, base + rnd.nextInt(-2, 3), rnd) }.toMutableList()
        g.staffMarket.clear()
        g.staffMarketSeason = 0
    }

    /** O rol için iş arayan adaylar (her sezon yenilenir). */
    fun market(g: Game, role: Int, rnd: Random = Random.Default): List<Staff> {
        if (g.staffMarketSeason != g.season) {
            g.staffMarket = (0..3).flatMap { r ->
                listOf(rnd.nextInt(5, 10), rnd.nextInt(8, 13), rnd.nextInt(11, 16), rnd.nextInt(14, 19), rnd.nextInt(17, 21))
                    .map { make(g, r, it, rnd) }
            }.toMutableList()
            g.staffMarketSeason = g.season
        }
        return g.staffMarket.filter { it.role == role }.sortedByDescending { it.quality }
    }

    /** Yeni çalışanı işe al. Eskisine 4 haftalık maaşı tazminat ödenir. Hata mesajı döner. */
    fun hire(g: Game, s: Staff): String? {
        val us = g.userTeam
        val old = of(g, s.role)
        val comp = (old?.wage ?: 0L) * 4
        if (us.budget < comp) return "Eski çalışanın tazminatı (${money(comp)}) için bütçe yetersiz."
        if (!Economy.fitsWages(g, us, s.wage, minusWage = old?.wage ?: 0L)) return "Maaş bütçesi bu maaşı kaldırmıyor (${money(s.wage)}/hafta)."
        if (comp > 0) Economy.earn(us, Economy.STAFF, -comp)
        g.staff.removeAll { it.role == s.role }
        g.staff += s
        g.staffMarket.remove(s)
        g.addNews(
            "Teknik ekip",
            "${s.name} ${roleNames[s.role].lowercase()} olarak göreve başladı (kalite ${s.quality}/20)." + (old?.let { " ${it.name} ile yollar ayrıldı." } ?: ""),
        )
        return null
    }

    // ---- Etkiler ----

    /** Antrenör: kullanıcının oyuncularının gelişim çarpanı (10 → 1.0). */
    fun coachFactor(g: Game) = 0.7 + q(g, COACH) * 0.03

    /** Fizyo: antrenman sakatlık riski çarpanı (10 → 1.0, 20 → 0.7). */
    fun physioRisk(g: Game) = (1.3 - q(g, PHYSIO) * 0.03).coerceAtLeast(0.6)

    /** Fizyo: sakat oyuncunun haftada bir ek hafta iyileşme ihtimali. */
    fun physioHeal(g: Game) = q(g, PHYSIO) * 0.025

    /** Yardımcı antrenör: konuşma etkisi çarpanı (10 → 0.9, 20 → 1.2). */
    fun talkFactor(g: Game) = 0.6 + q(g, ASSISTANT) * 0.03

    /** Gözlemci: bilinmeyen oyuncu belirsizliği. */
    fun scoutUncertainty(g: Game, ownLeague: Boolean): Int {
        val q = q(g, SCOUT)
        return if (ownLeague) (5 - q / 5).coerceAtLeast(1) else (10 - q / 3).coerceAtLeast(3)
    }

    /** Gözlemci: haftalık öneri sayısı. */
    fun scoutReports(g: Game) = if (q(g, SCOUT) >= 15) 2 else 1
}
