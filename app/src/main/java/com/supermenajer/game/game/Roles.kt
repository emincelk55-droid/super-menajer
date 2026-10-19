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
            Role("Kaleci", "Kalesini korur, riske girmez.", 0.0, 0.0, 0.0, 1.0, 1.0),
            Role("Libero kaleci", "Oyun kurulumuna katılır; biraz risk alır.", 0.0, 0.05, -0.03, 1.0, 1.2),
        ),
        Pos.DF to listOf(
            Role("Stoper", "Savunmaya odaklanır.", -0.05, 0.0, 0.1, 0.8, 0.8, 1.2),
            Role("Libero", "Arkadan oyun kurar.", 0.0, 0.12, -0.05, 0.8, 1.3),
            Role("Kanat beki", "Hücuma bindirir, orta yapar.", 0.15, 0.0, -0.1, 1.0, 1.4),
        ),
        Pos.MF to listOf(
            Role("Box-to-box", "Her iki yönde de çalışır.", 0.0, 0.0, 0.0, 1.0, 1.0),
            Role("Ön libero", "Savunmanın önünde kalkan.", -0.15, 0.0, 0.2, 0.6, 0.8),
            Role("Oyun kurucu", "Pasın merkezi; asist arar.", 0.0, 0.15, -0.1, 0.8, 1.6),
            Role("Kanat", "Çizgiye iner, orta ve şut.", 0.15, -0.1, -0.1, 1.3, 1.3),
        ),
        Pos.FW to listOf(
            Role("Fırsatçı", "Ceza sahasında golcü.", 0.05, 0.0, 0.0, 1.3, 0.8),
            Role("Hedef santrfor", "Fizik ve kafa vuruşu.", 0.0, 0.05, 0.0, 1.0, 1.0, 1.7),
            Role("Sahte 9", "Geriye gelip oyunu bağlar.", -0.1, 0.2, 0.0, 0.7, 1.5),
        ),
    )

    fun roleIndex(t: Team, slot: Int): Int = t.roles.getOrNull(slot)?.coerceAtLeast(0) ?: 0

    fun role(t: Team, slot: Int): Role {
        val pos = Formations.slots(t.formation).getOrNull(slot)?.pos ?: Pos.MF
        val list = byPos.getValue(pos)
        return list[roleIndex(t, slot).coerceIn(0, list.size - 1)]
    }

    fun setRole(t: Team, slot: Int, role: Int) {
        while (t.roles.size < 11) t.roles += 0
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
