package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Slot
import com.supermenajer.game.model.Team

/**
 * Detaylı mevkiler (KL, SLB, SĞB, STP, DOS, MO, OOS, SLK, SĞK, SF), tercih edilen ayak ve oyuncunun
 * doğal oyun özelliği (ör. kanat bek, topla oynayan stoper). Sahadaki her yerin de detaylı mevkisi vardır;
 * oyuncu kendi mevkisinde ve özelliğine uygun rolde daha verimli oynar.
 */
object Positions {
    enum class Det(val code: String, val label: String) {
        KL("KL", "Kaleci"),
        SLB("SLB", "Sol bek"),
        SGB("SĞB", "Sağ bek"),
        STP("STP", "Stoper"),
        DOS("DOS", "Defansif orta saha"),
        MO("MO", "Merkez orta saha"),
        OOS("OOS", "Ofansif orta saha"),
        SLK("SLK", "Sol kanat"),
        SGK("SĞK", "Sağ kanat"),
        SF("SF", "Santrfor"),
    }

    val feet = listOf("Sağ ayaklı", "Sol ayaklı", "İki ayaklı")

    private fun h(p: Player, salt: Int): Int {
        var x = (p.id.toLong() * 2246822519L + salt * 3266489917L) and 0x7fffffff
        x = (x xor (x shr 15)) * 668265263L and 0x7fffffff
        return (x % 1000).toInt()
    }

    fun det(p: Player): Det = Det.entries.getOrElse(p.detail) { assignDetail(p) }

    /** Sahadaki yerin detaylı mevkisi (koordinatlardan). */
    fun slotDet(s: Slot): Det = when (s.pos) {
        Pos.GK -> Det.KL
        Pos.DF -> when { s.x < 0.2f -> Det.SLB; s.x > 0.8f -> Det.SGB; else -> Det.STP }
        Pos.MF -> when {
            s.x <= 0.16f -> Det.SLK
            s.x >= 0.84f -> Det.SGK
            s.y <= 0.42f -> Det.DOS
            s.y >= 0.6f -> Det.OOS
            else -> Det.MO
        }
        Pos.FW -> when { s.x < 0.25f -> Det.SLK; s.x > 0.75f -> Det.SGK; else -> Det.SF }
    }

    /** Oyuncunun kendi detaylı mevkisi ve ayağı (özelliklerinden ve kimliğinden). */
    fun assignDetail(p: Player): Det {
        if (p.foot < 0) p.foot = h(p, 1).let { if (it < 600) 0 else if (it < 860) 1 else 2 }
        val left = p.foot == 1 || (p.foot == 2 && h(p, 2) < 500)
        val noise = h(p, 3) / 1000.0 * 16 - 8
        val d = when (p.pos) {
            Pos.GK -> Det.KL
            Pos.DF -> {
                val wide = (p.pace + p.passing) / 2.0 - p.defending + noise
                if (wide > -16) (if (left) Det.SLB else Det.SGB) else Det.STP
            }
            Pos.MF -> {
                val cand = listOf(
                    Det.DOS to p.defending + 2.0,
                    Det.MO to p.passing + 0.0,
                    Det.OOS to (p.shooting + p.dribbling) / 2.0,
                    (if (left) Det.SLK else Det.SGK) to (p.pace + p.dribbling) / 2.0 - 2,
                )
                cand.maxBy { it.second + (h(p, 4 + it.first.ordinal) / 1000.0 * 10) }.first
            }
            Pos.FW -> if ((p.pace + p.dribbling) / 2.0 > p.shooting + 2 + noise) (if (left) Det.SLK else Det.SGK) else Det.SF
        }
        p.detail = d.ordinal
        return d
    }

    /** Oyuncunun doğal rolü (Roles listesindeki indeks). */
    fun natRole(p: Player): Int {
        if (p.natRole >= 0) return p.natRole
        val d = det(p)
        val r = when (p.pos) {
            Pos.GK -> if (p.passing >= 45 && h(p, 7) < 420) 1 else 0
            Pos.DF -> when (d) {
                Det.STP -> if (p.passing >= p.defending - 20 && h(p, 8) < 550) 1 else 0
                else -> if ((p.pace + p.dribbling) / 2.0 >= p.defending - 9) 3 else 2
            }
            Pos.MF -> when (d) {
                Det.DOS -> 1
                Det.OOS -> if (p.passing > p.shooting + 14) 2 else 4
                Det.SLK, Det.SGK -> 3
                else -> if (p.passing >= p.physical + 2 && h(p, 9) < 650) 2 else 0
            }
            Pos.FW -> when (d) {
                Det.SLK, Det.SGK -> 3
                else -> when {
                    p.physical >= p.shooting - 6 && h(p, 10) < 600 -> 1
                    p.passing >= p.shooting - 18 && h(p, 18) < 380 -> 2
                    else -> 0
                }
            }
        }
        p.natRole = r
        return r
    }

    fun natRoleName(p: Player): String = Roles.byPos.getValue(p.pos).getOrNull(natRole(p))?.name ?: ""

    /** Oyuncunun o yerdeki uyumu (1.0 tam; kanat değişimi, bek ↔ kanat gibi yakın mevkiler biraz düşük). */
    fun fit(p: Player, slot: Det): Double {
        val d = det(p)
        if (d == slot) return 1.0
        val both = p.foot == 2
        fun pair(a: Det, b: Det) = (d == a && slot == b) || (d == b && slot == a)
        return when {
            pair(Det.SLB, Det.SGB) -> if (both) 0.99 else 0.95
            pair(Det.SLK, Det.SGK) -> if (both) 0.99 else 0.96
            pair(Det.MO, Det.DOS) || pair(Det.MO, Det.OOS) -> 0.97
            pair(Det.STP, Det.DOS) -> 0.94
            pair(Det.SLB, Det.SLK) || pair(Det.SGB, Det.SGK) -> 0.94
            pair(Det.OOS, Det.SF) || pair(Det.OOS, Det.SLK) || pair(Det.OOS, Det.SGK) -> 0.94
            pair(Det.SF, Det.SLK) || pair(Det.SF, Det.SGK) -> 0.93
            pair(Det.MO, Det.SLK) || pair(Det.MO, Det.SGK) -> 0.93
            pair(Det.DOS, Det.OOS) -> 0.92
            pair(Det.STP, Det.SLB) || pair(Det.STP, Det.SGB) -> 0.92
            else -> 0.88
        }
    }

    /** Takım bazında kotalarla detaylı mevki ataması (dünya kurulurken): gerçekçi bir kadro dağılımı. */
    fun assignTeam(g: Game, t: Team) {
        val sq = g.squad(t.id)
        for (p in sq) if (p.foot < 0) p.foot = h(p, 1).let { if (it < 600) 0 else if (it < 860) 1 else 2 }
        fun noise(p: Player, salt: Int) = h(p, salt) / 1000.0 * 8
        fun leftBonus(p: Player) = when (p.foot) { 1 -> 14.0; 2 -> 6.0; else -> 0.0 }
        fun rightBonus(p: Player) = when (p.foot) { 0 -> 14.0; 2 -> 6.0; else -> 0.0 }
        fun take(pool: MutableList<Player>, n: Int, det: Det, score: (Player) -> Double) {
            repeat(n) {
                val best = pool.maxByOrNull(score) ?: return
                best.detail = det.ordinal
                best.natRole = -1
                pool.remove(best)
            }
        }
        sq.filter { it.pos == Pos.GK }.forEach { it.detail = Det.KL.ordinal; it.natRole = -1 }
        val df = sq.filter { it.pos == Pos.DF }.toMutableList()
        val backs = (df.size - maxOf(2, (df.size + 1) / 2)).coerceAtLeast(0)
        val wide = { p: Player -> (p.pace + p.passing) / 2.0 - p.defending / 3.0 + noise(p, 11) }
        take(df, (backs + 1) / 2, Det.SLB) { wide(it) + leftBonus(it) }
        take(df, backs / 2, Det.SGB) { wide(it) + rightBonus(it) }
        take(df, df.size, Det.STP) { 0.0 }
        val mf = sq.filter { it.pos == Pos.MF }.toMutableList()
        val n = mf.size
        val winger = { p: Player -> (p.pace + p.dribbling) / 2.0 - p.defending / 4.0 + noise(p, 12) }
        take(mf, maxOf(1, n / 4), Det.DOS) { it.defending + it.physical / 3.0 + noise(it, 13) }
        take(mf, maxOf(1, (n * 15 + 50) / 100), Det.SLK) { winger(it) + leftBonus(it) }
        take(mf, maxOf(1, (n * 15 + 50) / 100), Det.SGK) { winger(it) + rightBonus(it) }
        take(mf, maxOf(1, (n * 15 + 50) / 100), Det.OOS) { (it.shooting + it.dribbling) / 2.0 + noise(it, 14) }
        take(mf, mf.size, Det.MO) { 0.0 }
        val fw = sq.filter { it.pos == Pos.FW }.toMutableList()
        val wingers = (fw.size - maxOf(1, (fw.size * 6 + 9) / 10)).coerceAtLeast(0)
        val fwWide = { p: Player -> (p.pace + p.dribbling) / 2.0 - p.shooting / 2.0 + noise(p, 15) }
        take(fw, (wingers + 1) / 2, Det.SLK) { fwWide(it) + leftBonus(it) }
        take(fw, wingers / 2, Det.SGK) { fwWide(it) + rightBonus(it) }
        take(fw, fw.size, Det.SF) { 0.0 }
        // Kanada geçen oyuncunun ayağı o tarafa uygun olsun
        for (p in sq) {
            val d = det(p)
            if ((d == Det.SLB || d == Det.SLK) && p.foot == 0 && h(p, 16) < 700) p.foot = 2
            if ((d == Det.SGB || d == Det.SGK) && p.foot == 1 && h(p, 17) < 700) p.foot = 2
        }
    }

    /** Takım kadrosunda temel mevkilerin eksik kalmaması için en uygun oyuncuları kaydırır. */
    fun balance(g: Game, t: Team) {
        val sq = g.squad(t.id)
        fun ensure(pos: Pos, det: Det, n: Int) {
            var have = sq.count { it.pos == pos && det(it) == det }
            while (have < n) {
                val c = sq.filter { it.pos == pos && det(it) != det && sq.count { o -> o.pos == pos && det(o) == det(it) } > 1 }
                    .maxByOrNull { fitScore(it, det) } ?: return
                c.detail = det.ordinal
                c.natRole = -1
                have++
            }
        }
        ensure(Pos.DF, Det.STP, 3)
        ensure(Pos.DF, Det.SLB, 1)
        ensure(Pos.DF, Det.SGB, 1)
        ensure(Pos.MF, Det.DOS, 1)
        ensure(Pos.MF, Det.MO, 1)
        ensure(Pos.FW, Det.SF, 1)
    }

    private fun fitScore(p: Player, det: Det): Double = when (det) {
        Det.STP -> p.defending + p.physical / 2.0
        Det.SLB, Det.SGB -> p.pace + p.passing / 2.0
        Det.DOS -> p.defending + p.passing / 2.0
        Det.MO -> p.passing + p.physical / 2.0
        Det.SF -> p.shooting + p.physical / 2.0
        else -> p.pace + p.dribbling / 2.0
    }

    /** fresh: dünya kurulurken / eski kayıt taşınırken tüm kadrolara kotalı atama; değilse sadece eksikleri tamamla. */
    fun ensureAll(g: Game, fresh: Boolean = false) {
        if (fresh) for (t in g.teams) assignTeam(g, t)
        for (p in g.players) if (p.detail < 0) assignDetail(p)
        for (t in g.teams) balance(g, t)
        for (p in g.players) natRole(p)
    }
}
