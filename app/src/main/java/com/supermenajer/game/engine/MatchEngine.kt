package com.supermenajer.game.engine

import com.supermenajer.game.game.Facilities
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Fixture
import com.supermenajer.game.model.Formations
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Slot
import com.supermenajer.game.model.Team
import kotlin.math.pow
import kotlin.random.Random

enum class Ev { KICKOFF, GOAL, SAVE, MISS, POST, CORNER, YELLOW, RED, INJURY, SUB, INFO, HALF, FULL }

/** buildUp: pozisyonun gerilimli girişi ("Sağdan orta... kafa..."); arayüz sonucu bundan sonra açıklar. */
class MatchEvent(val minute: Int, val type: Ev, val side: Int, val text: String, val playerId: Int = -1, val buildUp: String = "")

/** Maçtaki bir oyuncu. */
class PIM(val p: Player, var slot: Slot) {
    var fit = p.fitness
    var rating = 6.0
    var yellow = 0
    var red = false
    var goals = 0
    var assists = 0
    var injured = false
    var injWeeks = 0
    var subbedOn = false
    /** Takım konuşmasından gelen maç içi motivasyon (performans çarpanı, ör. 0.03 = %3). */
    var boost = 0.0
}

class Side(val team: Team, val index: Int) {
    val onPitch = mutableListOf<PIM>()
    val bench = mutableListOf<Player>()
    val used = mutableListOf<PIM>()
    var subsLeft = 5
    var goals = 0
    var shots = 0
    var onTarget = 0
    var poss = 0
    var fouls = 0
    var corners = 0
    var yellows = 0
    var reds = 0
    var xg = 0.0
    fun gk(): PIM? = onPitch.firstOrNull { it.slot.pos == Pos.GK }
}

class MatchSim(
    val game: Game,
    val fixture: Fixture,
    private val userTeamId: Int,
    private val autoUserSubs: Boolean,
    private val rnd: Random = Random.Default,
) {
    private val big = com.supermenajer.game.game.Talks.bigMatch(game, fixture)
    val home = Side(game.team(fixture.home), 0)
    val away = Side(game.team(fixture.away), 1)
    val sides: List<Side> get() = listOf(home, away)
    var minute = 0
        private set
    val events = mutableListOf<MatchEvent>()
    var ballX = 0.5f
        private set
    var ballY = 0.5f
        private set
    val finished: Boolean get() = minute >= 90
    var penHome = -1
        private set
    var penAway = -1
        private set
    /** Tek maç eleme (Türkiye Kupası veya Avrupa eleme turları): beraberlikte penaltı. */
    val isCup: Boolean get() = Comp.isKnockout(fixture.comp, fixture.round)
    private val isLeague: Boolean get() = Comp.isLeague(fixture.comp)
    val userSide: Side? get() = sides.firstOrNull { it.team.id == userTeamId }

    init {
        setup(home)
        setup(away)
        ev(Ev.KICKOFF, -1, Comms.kickoff(home.team, away.team, rnd))
        if (fixture.attendance == 0) fixture.attendance = Facilities.attendance(game, fixture, rnd)
        Facilities.crowdText(this).takeIf { it.isNotEmpty() }?.let { ev(Ev.INFO, -1, "🏟️ $it") }
        userSide?.let { us ->
            // Gizli antrenman: rakibe özel hazırlık
            if (game.secretWeek == fixture.week) {
                us.onPitch.forEach { it.boost += 0.025 }
                ev(Ev.INFO, us.index, "🕵️ Gizli antrenmanda çalışılan plan sahada! Oyuncular rakibi ezbere biliyor.")
            }
            // Galibiyet primi motivasyonu
            val wb = us.team.winBonus
            if (wb > 0) us.onPitch.forEach { it.boost += 0.01 * wb }
        }
        val fanBoost = com.supermenajer.game.game.Fans.homeBoost(game, fixture)
        if (fanBoost > 0) {
            home.onPitch.forEach { it.boost += fanBoost }
            ev(Ev.INFO, 0, "📣 Tribünler coşkulu! Taraftar takımını 12. adam gibi destekliyor.")
        }
    }

    private fun setup(s: Side) {
        if (s.team.id == userTeamId) Lineups.validate(game, s.team) else Lineups.autoPick(game, s.team)
        val slots = Formations.slots(s.team.formation)
        s.team.lineup.forEachIndexed { i, id -> game.player(id)?.let { s.onPitch += PIM(it, slots[i]) } }
        s.used += s.onPitch
        s.bench += Lineups.bench(game, s.team)
    }

    private fun ev(type: Ev, side: Int, text: String, pid: Int = -1, buildUp: String = "") {
        events += MatchEvent(minute, type, side, text, pid, buildUp)
    }

    fun other(s: Side): Side = if (s === home) away else home

    /** Oyuncunun ilk 11'deki mevki indeksine göre rolü. */
    private fun roleOf(s: Side, m: PIM): com.supermenajer.game.game.Roles.Role {
        val idx = Formations.slots(s.team.formation).indexOf(m.slot)
        return if (idx < 0) com.supermenajer.game.game.Roles.byPos.getValue(m.slot.pos)[0] else com.supermenajer.game.game.Roles.role(s.team, idx)
    }

    private fun eff(m: PIM): Double =
        m.p.ovrAt(m.slot.pos) * (0.62 + 0.38 * m.fit / 100.0) * (0.93 + 0.14 * m.p.morale / 100.0) * (1.0 + m.boost) *
            com.supermenajer.game.game.Traits.matchFactor(m.p, big) *
            com.supermenajer.game.game.Positions.fit(m.p, com.supermenajer.game.game.Positions.slotDet(m.slot))

    class Str(val atk: Double, val mid: Double, val def: Double)

    fun strength(s: Side): Str {
        var a = 0.0; var m = 0.0; var d = 0.0
        for (x in s.onPitch) {
            val e = eff(x)
            when (x.slot.pos) {
                Pos.GK -> { m += e * 0.1; d += e * 0.3 }
                Pos.DF -> { a += e * 0.12; m += e * 0.3; d += e }
                Pos.MF -> { a += e * 0.45; m += e; d += e * 0.4 }
                Pos.FW -> { a += e; m += e * 0.2; d += e * 0.1 }
            }
            val r = roleOf(s, x)
            // Oyuncunun doğal özelliğine uygun rol: küçük bir verim artışı
            val idx = Formations.slots(s.team.formation).indexOf(x.slot)
            val natural = idx >= 0 && x.p.pos == x.slot.pos &&
                com.supermenajer.game.game.Roles.roleIndex(s.team, idx) == com.supermenajer.game.game.Positions.natRole(x.p)
            val nb = if (natural) e * 0.03 else 0.0
            a += e * r.atk + nb * 0.3; m += e * r.mid + nb * 0.4; d += e * r.def + nb * 0.3
        }
        a /= 4.3; m /= 5.4; d /= 5.9
        when (s.team.mentality) { 0 -> { a *= 0.9; d *= 1.07 }; 2 -> { a *= 1.08; d *= 0.94 } }
        when (s.team.pressing) { 0 -> { m *= 0.96; d *= 1.02 }; 2 -> { m *= 1.05; d *= 0.99 } }
        val o = other(s).team
        val t = s.team
        // Tempo: hızlı oyun daha az top hakimiyeti ama daha direkt hücum
        when (t.tempo) { 0 -> { m *= 1.04; a *= 0.97 }; 2 -> { m *= 0.98; a *= 1.04 } }
        // Genişlik: geniş oyun dar savunan rakibe karşı etkili
        when (t.width) {
            0 -> { m *= 1.03; a *= 0.98; d *= 1.02 }
            2 -> { a *= if (o.width == 0) 1.06 else 1.03; d *= 0.98 }
        }
        // Pas stili: kısa pas orta sahayı, uzun top yüksek savunma hattını cezalandırır
        when (t.passing) {
            0 -> m *= 1.04
            2 -> { m *= 0.95; a *= if (o.defLine == 2) 1.08 else 1.02 }
        }
        // Savunma hattı: yüksek hat orta sahayı sıkıştırır ama hızlı forvetlere açık
        when (t.defLine) {
            0 -> { d *= 1.05; m *= 0.96 }
            2 -> {
                m *= 1.04
                val oppPace = other(s).onPitch.filter { it.slot.pos == Pos.FW }.map { it.p.pace }.average().takeIf { !it.isNaN() } ?: 60.0
                d *= if (oppPace >= 76) 0.92 else 0.98
            }
        }
        if (t.counter) m *= 0.97
        // Sahadaki lider karakterler takımı yukarı çeker
        val leaders = s.onPitch.count { com.supermenajer.game.game.Traits.isLeader(it.p) }
        val lf = 1.0 + (leaders * 0.01).coerceAtMost(0.02)
        a *= lf; m *= lf; d *= lf
        // Kaptan sahadaysa küçük bir moral desteği
        val cap = com.supermenajer.game.game.Roles.captain(game, s.team)
        if (cap != null && s.onPitch.any { it.p.id == cap.id }) { a *= 1.01; m *= 1.01; d *= 1.01 }
        if (s === home) { a *= 1.03; m *= 1.03; d *= 1.03 }
        return Str(a.coerceAtLeast(5.0), m.coerceAtLeast(5.0), d.coerceAtLeast(5.0))
    }

    fun step() {
        if (finished) return
        minute++
        val sh = strength(home)
        val sa = strength(away)
        val mh = sh.mid.pow(2)
        val ma = sa.mid.pow(2)
        val atk = if (rnd.nextDouble() < mh / (mh + ma)) home else away
        val def = other(atk)
        val sA = if (atk === home) sh else sa
        val sD = if (atk === home) sa else sh
        atk.poss++
        val dir = if (atk === home) 1f else -1f
        ballX = 0.5f + dir * (0.05f + rnd.nextFloat() * 0.3f)
        ballY = 0.2f + rnd.nextFloat() * 0.6f

        var pShot = (0.2 * (sA.atk / sD.def).pow(1.6)).coerceIn(0.05, 0.45)
        when (atk.team.tempo) { 0 -> pShot *= 0.93; 2 -> pShot *= 1.08 }
        // Önde olan takım zaman geçiriyorsa oyun yavaşlar
        val leader = if (home.goals > away.goals) home else if (away.goals > home.goals) away else null
        if (minute >= 70 && leader != null && leader.team.timeWasting) pShot *= 0.85
        val r = rnd.nextDouble()
        var handled = false
        // Kontra atak: hücuma çıkan rakibin arkasına
        if (def.team.counter && atk.team.mentality == 2 && rnd.nextDouble() < 0.03) {
            val counter = pick(def.onPitch.filter { it.slot.pos == Pos.FW || it.slot.pos == Pos.MF })
            if (counter != null) ev(Ev.INFO, def.index, "⚡ Hızlı kontra! ${counter.p.name} boş alana koşuyor...")
            shot(def, atk, if (def === home) sh else sa, sA, forcedQ = 0.12 + rnd.nextDouble() * 0.12)
            handled = true
        }
        // Ofsayt tuzağı: kimi zaman atağı keser, kimi zaman bozulur
        if (!handled && r < pShot && def.team.offsideTrap) {
            val t = rnd.nextDouble()
            if (t < 0.15) {
                pick(atk.onPitch.filter { it.slot.pos == Pos.FW })?.let { ev(Ev.INFO, def.index, "🚩 Ofsayt! ${it.p.name} tuzağa düştü.") }
                handled = true
            } else if (t < 0.21) {
                ev(Ev.INFO, atk.index, "😱 Ofsayt tuzağı bozuldu, forvet kaleciyle karşı karşıya!")
                shot(atk, def, sA, sD, forcedQ = 0.28 + rnd.nextDouble() * 0.12)
                handled = true
            }
        }
        if (!handled) when {
            r < pShot -> shot(atk, def, sA, sD)
            r < pShot + 0.2 -> foul(def, atk)
            r < pShot + 0.3 -> pick(atk.onPitch)?.let { ev(Ev.INFO, atk.index, Comms.filler(atk.team, it.p.name, rnd)) }
        }
        fatigue(home); fatigue(away)
        for (s in sides) if (rnd.nextDouble() < 0.0012) injury(s)
        if (minute == 58 || minute == 67 || minute == 76 || minute == 84) {
            for (s in sides) if (s.team.id != userTeamId || autoUserSubs) aiSub(s)
        }
        if (minute == 45) ev(Ev.HALF, -1, Comms.half(home, away))
        if (minute == 90) finish()
    }

    fun runToEnd() {
        while (!finished) step()
    }

    private fun <T> pick(list: List<T>): T? = if (list.isEmpty()) null else list[rnd.nextInt(list.size)]

    private fun pickW(list: List<PIM>, w: (PIM) -> Double): PIM? {
        if (list.isEmpty()) return null
        val total = list.sumOf(w)
        if (total <= 0) return list[rnd.nextInt(list.size)]
        var x = rnd.nextDouble() * total
        for (m in list) {
            x -= w(m)
            if (x <= 0) return m
        }
        return list.last()
    }

    private fun shotW(p: Pos) = when (p) { Pos.FW -> 6.0; Pos.MF -> 2.5; Pos.DF -> 0.6; Pos.GK -> 0.0 }
    private fun astW(p: Pos) = when (p) { Pos.FW -> 1.5; Pos.MF -> 3.0; Pos.DF -> 1.0; Pos.GK -> 0.05 }

    private fun shot(atk: Side, def: Side, sA: Str, sD: Str, forcedQ: Double? = null, header: Boolean = false) {
        val outfield = atk.onPitch.filter { it.slot.pos != Pos.GK }
        val setPiece = forcedQ != null
        val penalty = !setPiece && rnd.nextDouble() < 0.02
        val onPitchPlayers = atk.onPitch.map { it.p }
        val R = com.supermenajer.game.game.Roles
        // Duran toplarda belirlenen kullanıcılar
        val designated = when {
            penalty -> R.penaltyTaker(game, atk.team, onPitchPlayers)
            setPiece && !header -> R.freeKickTaker(game, atk.team, onPitchPlayers)
            else -> null
        }
        val shooter = designated?.let { d -> atk.onPitch.firstOrNull { it.p.id == d.id } } ?: pickW(outfield) {
            val r = roleOf(atk, it)
            shotW(it.slot.pos) * (if (header) r.header else r.shot) * ((if (header) it.p.physical else it.p.shooting) / 70.0).pow(2)
        } ?: return
        val assister = if (!penalty && (header || rnd.nextDouble() < 0.72)) {
            val corner = if (header) R.cornerTaker(game, atk.team, onPitchPlayers)?.let { c -> atk.onPitch.firstOrNull { it.p.id == c.id && it !== shooter } } else null
            corner ?: pickW(outfield.filter { it !== shooter }) { astW(it.slot.pos) * roleOf(atk, it).assist * (it.p.passing / 70.0).pow(2) }
        } else null
        val q = when {
            penalty -> 0.76
            forcedQ != null -> forcedQ
            else -> (0.015 + rnd.nextDouble().pow(2.6) * 0.38) * (sA.atk / sD.def).pow(0.3)
        }
        atk.shots++
        atk.xg += q
        val skill = if (header) shooter.p.physical else shooter.p.shooting
        val shootF = (skill * (0.7 + 0.3 * shooter.fit / 100) / 70.0).pow(1.1)
        val keeper = def.gk()
        val gkF = (keeper?.let { it.p.goalkeeping * (0.85 + 0.15 * it.fit / 100) } ?: 20.0) / 70.0
        val pGoal = (q * shootF / gkF).coerceIn(0.01, 0.92)
        if (penalty) ev(Ev.INFO, atk.index, Comms.penalty(atk.team, rnd))
        val build = Comms.buildUp(shooter.p.name, assister?.p?.name, header, penalty, setPiece, rnd)
        ballX = if (atk === home) 0.93f else 0.07f
        ballY = 0.38f + rnd.nextFloat() * 0.24f

        if (rnd.nextDouble() < pGoal) {
            atk.goals++
            atk.onTarget++
            shooter.goals++
            shooter.rating += 1.1
            assister?.let { it.assists++; it.rating += 0.7 }
            def.onPitch.filter { it.slot.pos == Pos.GK || it.slot.pos == Pos.DF }.forEach { it.rating -= 0.25 }
            ev(
                Ev.GOAL, atk.index,
                Comms.goal(shooter.p.name, assister?.p?.name, penalty, header, rnd) + "  (${home.goals}-${away.goals})",
                shooter.p.id, build,
            )
            ballX = 0.5f; ballY = 0.5f
            return
        }
        val r2 = rnd.nextDouble()
        when {
            r2 < 0.42 -> {
                atk.onTarget++
                shooter.rating += 0.1
                keeper?.let { it.rating += 0.3 }
                ev(Ev.SAVE, atk.index, Comms.save(shooter.p.name, keeper?.p?.name ?: "savunma", penalty, rnd, header), shooter.p.id, build)
            }
            r2 < 0.48 -> {
                shooter.rating += 0.05
                ev(Ev.POST, atk.index, Comms.post(shooter.p.name, rnd), shooter.p.id, build)
            }
            else -> {
                shooter.rating -= 0.05
                ev(Ev.MISS, atk.index, Comms.miss(shooter.p.name, penalty, rnd), shooter.p.id, build)
            }
        }
        if (!setPiece && rnd.nextDouble() < 0.3) {
            atk.corners++
            if (rnd.nextDouble() < (if (atk.team.width == 2) 0.3 else 0.22)) {
                ev(Ev.CORNER, atk.index, Comms.corner(atk.team, rnd))
                shot(atk, def, sA, sD, forcedQ = 0.08 + rnd.nextDouble() * 0.1, header = true)
            }
        }
    }

    private fun foul(def: Side, atk: Side) {
        val fouler = pickW(def.onPitch) {
            when (it.slot.pos) { Pos.GK -> 0.1; Pos.DF -> 3.0; Pos.MF -> 2.0; Pos.FW -> 1.0 } * (1.4 - it.p.defending / 100.0) *
                com.supermenajer.game.game.Traits.foulFactor(it.p) * (if (calm && def === userSide) 0.7 else 1.0)
        } ?: return
        def.fouls++
        val r = rnd.nextDouble()
        when {
            r < 0.004 -> sendOff(def, fouler, secondYellow = false)
            r < 0.13 -> {
                fouler.yellow++
                def.yellows++
                fouler.rating -= 0.3
                if (fouler.yellow >= 2) sendOff(def, fouler, secondYellow = true)
                else ev(Ev.YELLOW, def.index, Comms.yellow(fouler.p.name, rnd), fouler.p.id)
            }
            rnd.nextDouble() < 0.05 -> {
                ev(Ev.INFO, atk.index, Comms.freeKick(atk.team, rnd))
                shot(atk, def, strength(atk), strength(def), forcedQ = 0.05 + rnd.nextDouble() * 0.06)
            }
        }
    }

    private fun sendOff(s: Side, m: PIM, secondYellow: Boolean) {
        m.red = true
        s.reds++
        m.rating -= 1.5
        ev(Ev.RED, s.index, Comms.red(m.p.name, secondYellow), m.p.id)
        val wasGk = m.slot.pos == Pos.GK
        val gkSlot = m.slot
        s.onPitch.remove(m)
        if (wasGk) {
            val gkSub = s.bench.firstOrNull { it.pos == Pos.GK }
            val sacrifice = s.onPitch.filter { it.slot.pos == Pos.FW || it.slot.pos == Pos.MF }.minByOrNull { eff(it) }
                ?: s.onPitch.minByOrNull { eff(it) }
            if (gkSub != null && s.subsLeft > 0 && sacrifice != null) {
                s.onPitch.remove(sacrifice)
                val nm = PIM(gkSub, gkSlot).apply { subbedOn = true }
                s.onPitch += nm
                s.bench.remove(gkSub)
                s.used += nm
                s.subsLeft--
                ev(Ev.SUB, s.index, Comms.sub(s.team, gkSub.name, sacrifice.p.name))
            } else {
                s.onPitch.filter { it.slot.pos != Pos.GK }.minByOrNull { eff(it) }?.slot = gkSlot
            }
        }
    }

    private fun injury(s: Side) {
        val v = pickW(s.onPitch) { com.supermenajer.game.game.Traits.injuryFactor(it.p) } ?: return
        v.injured = true
        v.injWeeks = rnd.nextInt(2, 7)
        v.rating -= 0.2
        ev(Ev.INJURY, s.index, Comms.injury(v.p.name, rnd), v.p.id)
        val rep = s.bench.filter { it.pos == v.slot.pos }.maxByOrNull { it.overall }
            ?: s.bench.filter { it.pos != Pos.GK }.maxByOrNull { it.ovrAt(v.slot.pos) }
        if (rep == null || !substitute(s, v, rep)) s.onPitch.remove(v)
    }

    /** Oyuncu değişikliği. Kullanıcı arayüzünden de çağrılır. */
    fun substitute(s: Side, out: PIM, inP: Player): Boolean {
        if (s.subsLeft <= 0 || finished) return false
        val idx = s.onPitch.indexOf(out)
        if (idx < 0 || inP !in s.bench) return false
        val m = PIM(inP, out.slot).apply { subbedOn = true }
        s.onPitch[idx] = m
        s.bench.remove(inP)
        s.used += m
        s.subsLeft--
        ev(Ev.SUB, s.index, Comms.sub(s.team, inP.name, out.p.name))
        return true
    }

    private fun aiSub(s: Side) {
        if (s.subsLeft <= 0) return
        val tired = s.onPitch.filter { it.slot.pos != Pos.GK }
            .minByOrNull { it.fit - if (it.yellow > 0) 8 else 0 } ?: return
        if (tired.fit > 70 && tired.yellow == 0) return
        val rep = s.bench.filter { it.pos == tired.slot.pos }.maxByOrNull { it.overall }
            ?: s.bench.filter { it.pos != Pos.GK }.maxByOrNull { it.ovrAt(tired.slot.pos) } ?: return
        substitute(s, tired, rep)
    }

    private fun fatigue(s: Side) {
        val pressF = when (s.team.pressing) { 0 -> 0.8; 2 -> 1.3; else -> 1.0 } *
            when (s.team.tempo) { 0 -> 0.92; 2 -> 1.1; else -> 1.0 }
        for (m in s.onPitch) {
            val drop = 0.42 * (1.32 - m.p.physical / 100.0) * pressF * (0.8 + rnd.nextDouble() * 0.4)
            m.fit = (m.fit - drop).coerceAtLeast(5.0)
        }
    }

    private fun finish() {
        for (s in sides) {
            val o = other(s)
            val win = s.goals > o.goals
            val loss = s.goals < o.goals
            val cleanSheet = o.goals == 0
            for (m in s.used) {
                if (win) m.rating += 0.4 else if (loss) m.rating -= 0.3
                if (cleanSheet && (m.slot.pos == Pos.GK || m.slot.pos == Pos.DF)) m.rating += 0.5
                m.rating = m.rating.coerceIn(3.0, 10.0)
            }
        }
        ev(Ev.FULL, -1, Comms.full(home, away))
        if (isCup && home.goals == away.goals) shootout()
    }

    private fun shootout() {
        ev(Ev.INFO, -1, "Eşitlik bozulmadı... Kazananı PENALTILAR belirleyecek!")
        val takers = sides.associateWith { s -> s.onPitch.filter { it.slot.pos != Pos.GK }.sortedByDescending { it.p.shooting } }
        fun kick(s: Side, i: Int): Boolean {
            val list = takers.getValue(s)
            if (list.isEmpty()) return false
            val taker = list[i % list.size]
            val gk = other(s).gk()?.p?.goalkeeping ?: 40
            val p = (0.76 * (taker.p.shooting / 70.0).pow(0.5) / (gk / 70.0).pow(0.5)).coerceIn(0.5, 0.92)
            val scored = rnd.nextDouble() < p
            ev(Ev.INFO, s.index, (if (scored) "✅ " else "❌ ") + "Penaltı: ${taker.p.name}" + if (scored) " gol!" else " kaçırdı!")
            return scored
        }
        var h = 0
        var a = 0
        for (i in 0 until 5) {
            if (kick(home, i)) h++
            if (h > a + (5 - i)) break
            if (kick(away, i)) a++
            if (a > h + (4 - i) || h > a + (4 - i)) break
        }
        var i = 5
        while (h == a) {
            if (kick(home, i)) h++
            if (kick(away, i)) a++
            i++
        }
        penHome = h
        penAway = a
        val w = if (h > a) home else away
        ev(Ev.FULL, w.index, "Penaltılar: $h-$a. ${w.team.name} turu geçti!")
    }

    /** Kupa maçında turu geçen takım. */
    fun cupWinner(): Team? = when {
        !finished -> null
        home.goals > away.goals -> home.team
        away.goals > home.goals -> away.team
        penHome > penAway -> home.team
        else -> away.team
    }

    /** 0 maç öncesi, 1 devre arası, 2 maç sonu konuşması yapıldı mı. */
    /** Kenar çizgisi talimatı: son kullanılan dakika ve "sakin ol" (kart riskini azaltır). */
    var lastShout = -100
        private set
    private var calm = false

    val canShout: Boolean get() = !finished && minute - lastShout >= 10 && userSide != null

    /** FM tarzı kenar çizgisi talimatı. 0 cesaretlendir, 1 daha fazlasını iste, 2 odaklan, 3 sakin ol. */
    fun shout(kind: Int): String {
        val us = userSide ?: return ""
        if (!canShout) return "Oyuncular az önce seni duydu, biraz bekle."
        lastShout = minute
        val diff = us.goals - other(us).goals
        val (delta, msg) = when (kind) {
            0 -> if (diff < 0) 0.025 to "Oyuncular alkışını duydu, yeniden inandılar!" else 0.01 to "Takım cesaretlendi."
            1 -> if (diff <= 0) (if (rnd.nextDouble() < 0.7) 0.03 to "Takım tempoyu artırdı!" else -0.01 to "Baskı ters tepti, bazı oyuncular gerildi.") else -0.01 to "Öndeyken bağırman oyuncuları şaşırttı."
            2 -> 0.015 to "Oyuncular konsantre oldu."
            else -> { calm = true; 0.0 to "Oyuncular sakinleşti, daha kontrollü oynayacaklar." }
        }
        us.onPitch.forEach { it.boost = (it.boost + delta).coerceIn(-0.06, 0.08) }
        ev(Ev.INFO, us.index, "📣 Kenar çizgisinden: " + listOf("\"Hadi çocuklar, inanın!\"", "\"Daha fazlasını istiyorum!\"", "\"Odaklanın!\"", "\"Sakin olun!\"")[kind] + " · " + msg)
        return msg
    }

    val talksDone = BooleanArray(3)
    val talkDone: Boolean get() = talksDone[1]
    val random: Random get() = rnd

    /** Konuşma etkisini uygular; oyuncu başına değişimi döner. */
    fun applyTalk(kind: Int, base: Double, factor: Double): List<Pair<PIM, Double>> {
        val us = userSide ?: return emptyList()
        talksDone[kind] = true
        val targets = if (kind == 2) us.used else us.onPitch
        return targets.map { m ->
            val d = base * factor + rnd.nextDouble(-2.0, 2.0)
            if (kind == 2) {
                m.p.morale = (m.p.morale + d).coerceIn(5.0, 100.0)
            } else {
                m.boost = (m.boost + d * 0.006).coerceIn(-0.06, 0.06)
                m.p.morale = (m.p.morale + d * 0.5).coerceIn(5.0, 100.0)
            }
            m to d
        }
    }

    fun note(text: String) {
        ev(Ev.INFO, userSide?.index ?: -1, text)
    }

    fun manOfTheMatch(): PIM? = (home.used + away.used).maxByOrNull { it.rating }

    /** Maç sonucunu oyun dünyasına işler. */
    fun applyToGame() {
        check(finished)
        fixture.hg = home.goals
        fixture.ag = away.goals
        if (isCup) {
            if (penHome >= 0) fixture.pen = "$penHome-$penAway"
            fixture.winner = cupWinner()?.id ?: -1
        } else if (isLeague) {
            record(home.team, home.goals, away.goals)
            record(away.team, away.goals, home.goals)
        }
        for (s in sides) {
            val res = s.goals.compareTo(other(s).goals)
            for (m in s.used) {
                val p = m.p
                p.apps++
                p.goals += m.goals
                p.assists += m.assists
                p.ratingSum += m.rating
                p.fitness = m.fit
                val perf = when { m.rating >= 7.5 -> 3; m.rating < 5.5 -> -3; else -> 0 }
                p.morale = (p.morale + res * 4 + perf).coerceIn(5.0, 100.0)
                if (m.red) {
                    p.suspended = rnd.nextInt(2, 4)
                } else if (m.yellow > 0) {
                    p.yellowCards++
                    if (p.yellowCards >= 4) { p.yellowCards = 0; p.suspended = 2 }
                }
                if (m.injured) p.injuredWeeks = m.injWeeks
            }
            val playedIds = s.used.map { it.p.id }.toSet()
            game.squad(s.team.id).filter { it.id !in playedIds }.forEach {
                it.morale = (it.morale + res * 1.5 - if (it.overall >= 70 && it.age >= 22) 1.0 else 0.0).coerceIn(5.0, 100.0)
            }
        }
    }

    private fun record(t: Team, f: Int, a: Int) {
        t.gf += f
        t.ga += a
        val c = when {
            f > a -> { t.won++; 'G' }
            f == a -> { t.drawn++; 'B' }
            else -> { t.lost++; 'M' }
        }
        t.form = (t.form + c).takeLast(5)
    }
}
