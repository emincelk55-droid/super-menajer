package com.supermenajer.game.game

import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Team

/**
 * FM tarzı oyuncu rolleri (mevkiye göre), kaptan, duran top kullanıcıları ve forma numaraları.
 * Rol, maç motorunda oyuncunun hücum / orta saha / savunma katkısını ve şut-asist ağırlığını değiştirir.
 */
object Roles {
    class Role(val name: String, val info: String, val atk: Double, val mid: Double, val def: Double, val shot: Double, val assist: Double, val header: Double = 1.0)

    val byPos: Map<Pos, List<Role>> = mapOf(
        Pos.GK to listOf(
            Role(L("Kaleci", "Goalkeeper"), L("Kalesini korur, riske girmez.", "Guards his goal, takes no risks."), 0.0, 0.0, 0.0, 1.0, 1.0),
            Role(L("Libero kaleci", "Sweeper keeper"), L("Oyun kurulumuna katılır, çizgiden çıkar.", "Joins the build-up, comes off his line."), 0.0, 0.05, -0.03, 1.0, 1.2),
        ),
        Pos.DF to listOf(
            Role(L("Stoper", "Centre back"), L("Savunmaya odaklanır, top kapar.", "Focuses on defending, wins the ball."), -0.05, 0.0, 0.1, 0.8, 0.8, 1.2),
            Role(L("Topla oynayan stoper", "Ball-playing defender"), L("Arkadan pasla oyun kurar.", "Builds play with passes from the back."), 0.0, 0.12, -0.04, 0.8, 1.3),
            Role(L("Bek", "Full back"), L("Önce savunma, fırsatta bindirme.", "Defence first, overlaps when possible."), 0.05, 0.02, 0.04, 0.9, 1.1),
            Role(L("Kanat bek", "Wing back"), L("Sürekli hücuma bindirir, orta yapar.", "Constantly overlaps and crosses."), 0.15, 0.0, -0.1, 1.0, 1.4),
        ),
        Pos.MF to listOf(
            Role(L("İki yönlü orta saha", "Box-to-box midfielder"), L("Ceza sahasından ceza sahasına koşar.", "Runs from box to box."), 0.03, 0.03, 0.03, 1.0, 1.0),
            Role(L("Defansif orta saha", "Defensive midfielder"), L("Savunmanın önünde kalkan.", "A shield in front of the defence."), -0.15, 0.0, 0.2, 0.6, 0.8),
            Role(L("Oyun kurucu", "Playmaker"), L("Pasın merkezi; asist arar.", "The hub of the passing; looks for assists."), 0.0, 0.15, -0.1, 0.8, 1.6),
            Role(L("Kanat", "Winger"), L("Çizgiye iner, orta ve şut.", "Goes to the byline, crosses and shoots."), 0.15, -0.1, -0.1, 1.3, 1.3),
            Role(L("Ofansif orta saha", "Attacking midfielder"), L("Forvet arkasında 10 numara.", "The number 10 behind the striker."), 0.12, 0.05, -0.15, 1.2, 1.4),
        ),
        Pos.FW to listOf(
            Role(L("Fırsatçı golcü", "Poacher"), L("Ceza sahasında golcü.", "Goalscorer in the box."), 0.05, 0.0, 0.0, 1.3, 0.8),
            Role(L("Hedef santrfor", "Target man"), L("Fizik ve kafa vuruşu.", "Physical presence and headers."), 0.0, 0.05, 0.0, 1.0, 1.0, 1.7),
            Role(L("Sahte 9", "False 9"), L("Geriye gelip oyunu bağlar.", "Drops deep and links play."), -0.1, 0.2, 0.0, 0.7, 1.5),
            Role(L("Kanat forvet", "Inside forward"), L("Kanattan içeri kat eder.", "Cuts inside from the wing."), 0.1, -0.05, -0.05, 1.15, 1.2),
        ),
    )

    /** Yerin detaylı mevkisine göre varsayılan rol. */
    fun defaultRole(slot: com.supermenajer.game.model.Slot): Int = when (Positions.slotDet(slot)) {
        Positions.Det.SLB, Positions.Det.SGB -> 2
        Positions.Det.DOS -> 1
        Positions.Det.OOS -> 4
        Positions.Det.SLK, Positions.Det.SGK -> 3
        else -> 0
    }

    fun roleIndex(t: Team, slot: Int): Int {
        val r = t.roles.getOrNull(slot) ?: -1
        if (r >= 0) return r
        return Formations.slots(t.formation).getOrNull(slot)?.let { defaultRole(it) } ?: 0
    }

    fun role(t: Team, slot: Int): Role {
        val pos = Formations.slots(t.formation).getOrNull(slot)?.pos ?: Pos.MF
        val list = byPos.getValue(pos)
        return list[roleIndex(t, slot).coerceIn(0, list.size - 1)]
    }

    fun setRole(t: Team, slot: Int, role: Int) {
        while (t.roles.size < 11) t.roles += -1
        t.roles[slot] = role
    }

    /** Kaptan ve duran top kullanıcıları; boşsa otomatik seçilir. */
    fun captain(g: Game, t: Team): Player? = g.player(t.captain)?.takeIf { it.teamId == t.id } ?: autoCaptain(g, t)

    private fun autoCaptain(g: Game, t: Team): Player? =
        t.lineup.mapNotNull { g.player(it) }.maxByOrNull { (if (Traits.isLeader(it)) 20 else 0) + it.age.coerceAtMost(32) + it.overall / 4 }

    fun penaltyTaker(g: Game, t: Team, onPitch: List<Player>): Player? =
        onPitch.firstOrNull { it.id == t.penaltyTaker } ?: onPitch.filter { it.pos != Pos.GK }.maxByOrNull { it.shooting }

    fun freeKickTaker(g: Game, t: Team, onPitch: List<Player>): Player? =
        onPitch.firstOrNull { it.id == t.fkTaker } ?: onPitch.filter { it.pos != Pos.GK }.maxByOrNull { it.shooting + it.passing }

    fun cornerTaker(g: Game, t: Team, onPitch: List<Player>): Player? =
        onPitch.firstOrNull { it.id == t.cornerTaker } ?: onPitch.filter { it.pos != Pos.GK }.maxByOrNull { it.passing }

    // ---- Forma numaraları ----

    private val preferred = mapOf(
        Pos.GK to listOf(1, 12, 23, 13, 31),
        Pos.DF to listOf(4, 5, 3, 2, 6, 15, 14, 22, 24, 26, 33),
        Pos.MF to listOf(8, 6, 10, 7, 16, 18, 20, 21, 25, 28, 30),
        Pos.FW to listOf(9, 11, 7, 10, 19, 17, 27, 29, 99, 77),
    )

    /** Takımda numarası olmayan oyunculara mevkisine uygun boş numara verir. */
    fun ensureNumbers(g: Game, t: Team) {
        val squad = g.squad(t.id)
        val used = squad.map { it.number }.filter { it > 0 }.toMutableSet()
        // Aynı numarayı taşıyan iki oyuncu varsa birini sıfırla
        val seen = HashSet<Int>()
        for (p in squad.sortedByDescending { it.overall }) {
            if (p.number > 0 && !seen.add(p.number)) p.number = 0
        }
        for (p in squad.filter { it.number <= 0 }.sortedByDescending { it.overall }) {
            val n = preferred.getValue(p.pos).firstOrNull { it !in used } ?: (2..99).first { it !in used }
            p.number = n
            used += n
        }
    }

    fun ensureAllNumbers(g: Game) = g.teams.forEach { ensureNumbers(g, it) }
}
