package com.supermenajer.game.engine

import com.supermenajer.game.model.Pos
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Sahadaki bir oyuncunun 2D görsel temsili (metre cinsinden). */
class Agent(val id: Int, val side: Int, val number: Int, val name: String, val gk: Boolean, val speed: Float) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    /** Diziliş konumu: kendi kalesinden uzaklık (d) ve yan konum (yanal). */
    var baseD = 0f
    var baseY = 0f
    var tx = 0f
    var ty = 0f
    var boost = 1f
}

/**
 * FM tarzı 2D maç görselleştirmesi. Maç motoru sonuçları belirler (gol, kurtarış, aut...);
 * bu sınıf aradaki oyunu kare kare oynatır: top oyuncudan oyuncuya pasla gider, oyuncular
 * ivmelenerek koşar, rakip pres yapar, top kapılır. Motorun olayları geldiğinde atak kurulur
 * ve şut sonucu sahada gerçekten yaşanır.
 */
class Pitch2D(val sim: MatchSim, seed: Long = 7L) {
    companion object {
        const val L = 105f
        const val W = 68f
        const val GOAL_HALF = 3.66f
    }

    private class Flight(
        val sx: Float, val sy: Float, val ex: Float, val ey: Float, val dur: Float, val peak: Float,
        val to: Agent?, val kind: Int, var t: Float = 0f,
    )

    private enum class Mode { NORMAL, ATTACK, SETPIECE, DEAD, CELEBRATE, KICKOFF }

    private val rnd = Random(seed)
    val agents = mutableListOf<Agent>()
    var ballX = L / 2
        private set
    var ballY = W / 2
        private set
    var ballZ = 0f
        private set
    var owner: Agent? = null
        private set
    private var flight: Flight? = null
    private var possSide = 0
    private var hold = 0f
    private var pressTime = 0f
    private var time = 0f
    private var mode = Mode.KICKOFF
    private var modeTime = 0f
    private var deadFor = 1.5f

    /** Ev sahibinin top hakimiyeti payı (motorun orta saha gücünden). */
    var bias = 0.5f

    /** Tamamlanan şut sonuçlarının sayısı: arayüz sonucu top ağlara/kaleciye ulaşınca açıklar. */
    var outcomes = 0
        private set

    /** Sahnede gösterilecek kısa etiket (ör. topu süren oyuncunun adı). */
    val busy: Boolean get() = mode == Mode.ATTACK || mode == Mode.SETPIECE || queue.isNotEmpty()

    // Atak senaryosu
    private val queue = ArrayDeque<MatchEvent>()
    private var cur: MatchEvent? = null
    private var steps = ArrayDeque<Agent>()
    private var shooter: Agent? = null
    private var spot = HashMap<Int, Pair<Float, Float>>()
    private var stepWait = 0f
    private var celebrator: Agent? = null
    private var kickoffSide = 0

    init {
        sync()
        kickoff(0, instant = true)
    }

    private fun d(side: Int, x: Float) = if (side == 0) x else L - x
    private fun fromD(side: Int, dd: Float) = if (side == 0) dd else L - dd
    private fun share(side: Int) = if (side == 0) bias else 1f - bias

    /** Sahadaki oyuncuları motorla eşitler (değişiklik, kırmızı kart, diziliş). */
    fun sync() {
        val ids = HashSet<Int>()
        for (s in sim.sides) {
            for (m in s.onPitch) {
                ids += m.p.id
                var a = agents.firstOrNull { it.id == m.p.id }
                if (a == null) {
                    a = Agent(m.p.id, s.index, m.p.number.takeIf { it > 0 } ?: 0, m.p.name.substringAfterLast(' '),
                        m.slot.pos == Pos.GK, 6.4f + m.p.pace / 100f * 2.6f)
                    // Yeni giren oyuncu kenar çizgisinden gelir
                    a.x = L / 2 + (if (s.index == 0) -3f else 3f)
                    a.y = -1.5f
                    agents += a
                }
                a.baseD = 3f + m.slot.y * 50f
                val lat = 5f + m.slot.x * (W - 10f)
                a.baseY = if (s.index == 0) lat else W - lat
            }
        }
        val gone = agents.filter { it.id !in ids }
        if (owner in gone) owner = null
        if (shooter in gone) shooter = null
        steps.removeAll(gone.toSet())
        agents.removeAll(gone.toSet())
    }

    /** Santra: oyuncular kendi yarı sahalarına koşar, top ortaya gelir. */
    private fun kickoff(side: Int, instant: Boolean = false) {
        kickoffSide = side
        flight = null
        ballX = L / 2; ballY = W / 2; ballZ = 0f
        possSide = side
        val taker = agents.filter { it.side == side && !it.gk }.maxByOrNull { it.baseD }
        owner = taker
        for (a in agents) {
            val dd = if (a === taker) L / 2 - 0.6f else min(a.baseD * 0.95f, L / 2 - 1.5f - (if (a.side != side && a.baseD > 35) 8f else 0f))
            a.tx = fromD(a.side, dd)
            a.ty = if (a === taker) W / 2 else a.baseY
            if (instant) { a.x = a.tx; a.y = a.ty; a.vx = 0f; a.vy = 0f }
        }
        setMode(Mode.KICKOFF)
        deadFor = if (instant) 0.8f else 2.4f
        hold = 0.5f
    }

    private fun setMode(m: Mode) { mode = m; modeTime = 0f }

    /** Motor olayını sahaya yansıt. */
    fun play(e: MatchEvent) {
        when (e.type) {
            Ev.GOAL, Ev.SAVE, Ev.MISS, Ev.POST, Ev.CORNER -> queue.addLast(e)
            Ev.YELLOW, Ev.RED, Ev.INJURY -> if (!busy) stoppage(e.side)
            Ev.HALF -> kickoff(1)
            Ev.SUB -> sync()
            else -> {}
        }
        if (mode == Mode.NORMAL) nextScene()
    }

    private fun stoppage(foulSide: Int) {
        sync()
        val o = 1 - foulSide
        val taker = agents.filter { it.side == o && !it.gk }.minByOrNull { dist(it.x, it.y, ballX, ballY) }
        flight = null
        if (taker != null) { owner = taker; possSide = o }
        setMode(Mode.DEAD)
        deadFor = 1.4f
        hold = 0.6f
    }

    private fun nextScene() {
        val e = queue.removeFirstOrNull() ?: return
        sync()
        cur = e
        val atk = e.side
        spot.clear()
        steps.clear()
        if (e.type == Ev.CORNER) {
            // Top korner bayrağına, korneri kullanacak oyuncu oraya koşar
            val fx = fromD(atk, L - 0.6f)
            val fy = if (ballY < W / 2) 0.6f else W - 0.6f
            flight = Flight(ballX, ballY, fx, fy, 0.9f, 1.5f, null, 3)
            owner = null
            possSide = atk
            queue.firstOrNull()?.assistId?.takeIf { it >= 0 }?.let { spot[it] = fx to fy }
            cur = null
            setMode(Mode.DEAD)
            deadFor = 2.3f
            return
        }
        val shooterA = agents.firstOrNull { it.id == e.playerId }
            ?: agents.filter { it.side == atk && !it.gk }.maxByOrNull { it.baseD } ?: return
        shooter = shooterA
        val assister = agents.firstOrNull { it.id == e.assistId && it !== shooterA }
        flight = null
        // Topu atak yapan takıma ver (korner ise kornerci)
        if (owner?.side != atk) {
            owner = if (assister != null && dist(assister.x, assister.y, ballX, ballY) < 6f) assister
            else agents.filter { it.side == atk && !it.gk }.minByOrNull { dist(it.x, it.y, ballX, ballY) }
            possSide = atk
            ballZ = 0f
        }
        val o = owner
        val side = if (rnd.nextBoolean()) 1f else -1f
        when (e.style) {
            2, 3 -> {
                // Penaltı / serbest vuruş: top noktaya, oyuncular yerleşir
                val pd = if (e.style == 2) L - 11f else L - 22f - rnd.nextFloat() * 8f
                val py = if (e.style == 2) W / 2 else W / 2 + side * (4f + rnd.nextFloat() * 12f)
                flight = Flight(ballX, ballY, fromD(atk, pd), py, 0.7f, 1.2f, null, 3)
                owner = null
                spot[shooterA.id] = fromD(atk, pd - 1.6f) to py
                setMode(Mode.SETPIECE)
                deadFor = 1.9f
                return
            }
            1 -> {
                if (assister != null) {
                    spot[assister.id] = if (assister === o) assister.x to assister.y
                    else fromD(atk, L - 8f - rnd.nextFloat() * 6f) to (if (side > 0) 5f else W - 5f)
                }
                spot[shooterA.id] = fromD(atk, L - 7f - rnd.nextFloat() * 5f) to W / 2 + (rnd.nextFloat() - 0.5f) * 10f
            }
            else -> {
                if (assister != null) {
                    val wide = rnd.nextFloat() < 0.5f
                    spot[assister.id] = fromD(atk, L - 22f - rnd.nextFloat() * 12f) to
                        (if (wide) (if (side > 0) 9f else W - 9f) else W / 2 + (rnd.nextFloat() - 0.5f) * 16f)
                }
                spot[shooterA.id] = fromD(atk, L - 11f - rnd.nextFloat() * 13f) to W / 2 + (rnd.nextFloat() - 0.5f) * 20f
            }
        }
        if (assister != null && assister !== o && shooterA !== o) steps += assister
        if (shooterA !== o) steps += shooterA
        stepWait = 0f
        hold = 0f
        setMode(Mode.ATTACK)
    }

    private fun dist(ax: Float, ay: Float, bx: Float, by: Float) = hypot(ax - bx, ay - by)

    // ------------------------------------------------------------------ güncelleme

    fun update(dtIn: Float) {
        var left = dtIn.coerceAtMost(0.25f)
        while (left > 0f) {
            val dt = min(left, 1f / 60f)
            step(dt)
            left -= dt
        }
    }

    private fun step(dt: Float) {
        time += dt
        modeTime += dt
        hold += dt
        when (mode) {
            Mode.KICKOFF, Mode.DEAD -> if (modeTime > deadFor && flight == null) {
                if (queue.isNotEmpty()) nextScene() else setMode(Mode.NORMAL)
                if (owner == null) looseBall()
            }
            Mode.CELEBRATE -> if (modeTime > 2.6f) {
                val e = cur
                cur = null
                kickoff(if (e != null) 1 - e.side else 0)
            }
            Mode.SETPIECE -> if (modeTime > deadFor && flight == null) {
                val sh = shooter
                if (sh != null) { owner = sh; possSide = sh.side; shoot(sh) }
            }
            Mode.ATTACK -> attackLogic(dt)
            Mode.NORMAL -> {
                if (queue.isNotEmpty()) nextScene() else normalLogic(dt)
            }
        }
        targets()
        move(dt)
        moveBall(dt)
    }

    private fun normalLogic(dt: Float) {
        val o = owner
        if (o == null) { looseBall(); return }
        // Pres: rakip yakınsa top kapılabilir
        val presser = agents.filter { it.side != o.side && !it.gk }.minByOrNull { dist(it.x, it.y, o.x, o.y) }
        if (presser != null && dist(presser.x, presser.y, o.x, o.y) < 1.6f) {
            pressTime += dt
            if (pressTime > 0.6f && rnd.nextFloat() < dt * 1.6f * (1.15f - share(o.side))) {
                owner = presser
                possSide = presser.side
                hold = 0f
                pressTime = 0f
                return
            }
        } else pressTime = 0f
        val limit = if (o.gk) 1.4f else 0.7f + (o.id % 5) * 0.18f
        if (hold > limit) pass(o)
    }

    private fun looseBall() {
        if (flight != null) return
        val n = agents.filter { !it.gk || d(it.side, ballX) < 16f }.minByOrNull { dist(it.x, it.y, ballX, ballY) } ?: return
        if (dist(n.x, n.y, ballX, ballY) < 1.2f) {
            owner = n
            possSide = n.side
            hold = 0f
        }
    }

    /** Akan oyunda pas seçimi: ileri, boşta ve makul mesafedeki arkadaşa. */
    private fun pass(o: Agent) {
        val mates = agents.filter { it.side == o.side && it !== o && (!it.gk || d(o.side, o.x) < 25f) }
        if (mates.isEmpty()) return
        val od = d(o.side, o.x)
        val opp = agents.filter { it.side != o.side }
        fun open(a: Agent) = opp.minOf { dist(it.x, it.y, a.x, a.y) }
        val w = mates.map { m ->
            val fwd = d(o.side, m.x) - od
            val len = dist(o.x, o.y, m.x, m.y)
            // Son üçte birde şut yerine topu çevir (şutları motor belirler)
            val fwdW = if (od > 78f) -abs(fwd) * 0.03f else fwd * 0.045f
            exp(fwdW + min(open(m), 9f) * 0.22f - abs(len - 16f) * 0.07f).toDouble()
        }
        var r = rnd.nextDouble() * w.sum()
        var target = mates.last()
        for ((i, m) in mates.withIndex()) { r -= w[i]; if (r <= 0) { target = m; break } }
        val len = dist(o.x, o.y, target.x, target.y)
        val speed = 15f + rnd.nextFloat() * 6f + len * 0.08f
        var dur = len / speed
        var ex = target.x + target.vx * dur * 0.8f
        var ey = target.y + target.vy * dur * 0.8f
        val ok = (0.8f + (share(o.side) - 0.5f) * 0.5f + min(open(target), 8f) * 0.012f - max(len - 25f, 0f) * 0.004f)
            .coerceIn(0.55f, 0.96f)
        var to = target
        if (rnd.nextFloat() > ok) {
            // Araya girme: pas çizgisine en yakın rakip
            val inter = opp.filter { !it.gk }.minByOrNull { segDist(it.x, it.y, o.x, o.y, ex, ey) }
            if (inter != null) {
                val (px, py) = project(inter.x, inter.y, o.x, o.y, ex, ey)
                ex = px; ey = py
                dur = max(dist(o.x, o.y, ex, ey) / speed, 0.15f)
                to = inter
            }
        }
        launch(o, ex.coerceIn(0.5f, L - 0.5f), ey.coerceIn(0.5f, W - 0.5f), dur, if (len > 30f) 2.2f + len * 0.05f else 0.15f, to, 0)
    }

    private fun launch(o: Agent, ex: Float, ey: Float, dur: Float, peak: Float, to: Agent?, kind: Int) {
        flight = Flight(ballX, ballY, ex, ey, dur.coerceAtLeast(0.12f), peak, to, kind)
        owner = null
        hold = 0f
        if (to != null) possSide = to.side
    }

    private fun attackLogic(dt: Float) {
        stepWait += dt
        if (flight != null && owner == null) return
        val atk = cur?.side
        // Top sahipsiz kaldıysa (ör. topu süren oyuncu oyundan çıktı) ya da rakipteyse
        // atak takımının en yakın oyuncusu topu alır; yoksa sahne takılır ve şut hiç gelmez.
        if (atk != null && owner?.side != atk) {
            val n = agents.filter { it.side == atk && !it.gk }.minByOrNull { dist(it.x, it.y, ballX, ballY) }
            if (n == null) { looseBall(); return }
            owner = n
            possSide = atk
            ballZ = 0f
            hold = 0f
            steps.remove(n)
        }
        val o = owner ?: run {
            looseBall()
            return
        }
        val next = steps.firstOrNull()
        if (next == null) {
            if (hold > 0.35f) shoot(o)
            return
        }
        val sp = spot[next.id]
        val ready = sp == null || dist(next.x, next.y, sp.first, sp.second) < 3.5f
        if ((ready && hold > 0.45f) || stepWait > 2.4f) {
            steps.removeFirst()
            stepWait = 0f
            val len = dist(o.x, o.y, next.x, next.y)
            val cross = cur?.style == 1 && steps.isEmpty()
            val speed = if (cross) 19f else 17f + len * 0.1f
            val dur = len / speed
            val ex = next.x + next.vx * dur * 0.9f
            val ey = next.y + next.vy * dur * 0.9f
            launch(o, ex.coerceIn(1f, L - 1f), ey.coerceIn(1f, W - 1f), dur, if (cross) 3.5f else if (len > 28f) 2f else 0.1f, next, 0)
        }
    }

    private fun shoot(o: Agent) {
        val e = cur ?: return
        val atk = e.side
        val gx = fromD(atk, L)
        val r = rnd.nextFloat()
        val (ex, ey) = when (e.type) {
            Ev.GOAL -> fromD(atk, L + 1.3f) to W / 2 + (r - 0.5f) * 2f * (GOAL_HALF - 0.6f)
            Ev.SAVE -> fromD(atk, L - 1.2f) to W / 2 + (r - 0.5f) * 2f * (GOAL_HALF - 0.8f)
            Ev.POST -> gx to W / 2 + (if (r < 0.5f) -GOAL_HALF else GOAL_HALF)
            else -> fromD(atk, L + 3f) to W / 2 + (if (r < 0.5f) -1f else 1f) * (GOAL_HALF + 1.5f + rnd.nextFloat() * 5f)
        }
        val len = dist(o.x, o.y, ex, ey)
        val header = e.style == 1
        val speed = if (header) 15f else 24f + rnd.nextFloat() * 6f
        if (e.type == Ev.SAVE) {
            agents.firstOrNull { it.gk && it.side != atk }?.let { k -> k.boost = 2.2f }
        }
        launch(o, ex, ey, len / speed, if (header) 0.6f else 0.2f + rnd.nextFloat() * 1.2f, null, 1)
        possSide = atk
    }

    private fun shotArrived() {
        val e = cur ?: return
        outcomes++
        when (e.type) {
            Ev.GOAL -> {
                celebrator = shooter
                setMode(Mode.CELEBRATE)
            }
            Ev.SAVE -> {
                val k = agents.firstOrNull { it.gk && it.side != e.side }
                owner = k
                possSide = k?.side ?: (1 - e.side)
                cur = null
                setMode(Mode.DEAD)
                deadFor = 1.2f
                hold = -0.6f
            }
            Ev.POST -> {
                cur = null
                flight = Flight(ballX, ballY, fromD(e.side, L - 9f - rnd.nextFloat() * 6f), W / 2 + (rnd.nextFloat() - 0.5f) * 18f, 0.7f, 1.2f, null, 2)
                setMode(Mode.NORMAL)
            }
            else -> {
                // Aut: kale vuruşu
                cur = null
                val k = agents.firstOrNull { it.gk && it.side != e.side }
                setMode(Mode.DEAD)
                deadFor = 1.6f
                if (k != null) {
                    flight = Flight(ballX, ballY, fromD(k.side, 5.5f), W / 2 + (rnd.nextFloat() - 0.5f) * 10f, 0.9f, 0.4f, k, 2)
                }
                possSide = 1 - e.side
            }
        }
    }

    // ------------------------------------------------------------------ hedefler ve hareket

    private fun targets() {
        val o = owner
        val fl = flight
        val bx = fl?.ex ?: ballX
        val by = fl?.ey ?: ballY
        val attackSide = if (mode == Mode.ATTACK || mode == Mode.SETPIECE) cur?.side ?: possSide else possSide
        if (mode == Mode.KICKOFF) {
            for (a in agents) if (a !== o) { a.tx = fromD(a.side, min(a.baseD * 0.95f, L / 2 - 2f)); a.ty = a.baseY }
            return
        }
        if (mode == Mode.CELEBRATE) {
            val c = celebrator
            val cx = fromD(c?.side ?: 0, L - 6f)
            val cy = if ((c?.y ?: 0f) < W / 2) 4f else W - 4f
            for (a in agents) {
                if (a.side == c?.side && !a.gk) { a.tx = cx - (if (a.side == 0) 1 else -1) * ((a.id % 5) * 1.3f); a.ty = cy + ((a.id % 3) - 1) * 1.6f }
                else { a.tx = a.x; a.ty = a.y }
            }
            return
        }
        // Sahipsiz top: iki takımdan en yakın oyuncular topa koşar
        val loose = o == null && (fl == null || fl.to == null) && fl?.kind != 1 && mode == Mode.NORMAL
        val chasers = if (loose) (0..1).mapNotNull { sd -> agents.filter { it.side == sd && !it.gk }.minByOrNull { dist(it.x, it.y, bx, by) } } else emptyList()
        // Savunmada topa en yakın iki oyuncu: biri basar, biri arkayı kapatır
        val defSide = 1 - attackSide
        val pressers = agents.filter { it.side == defSide && !it.gk }.sortedBy { dist(it.x, it.y, bx, by) }.take(2)
        for (a in agents) {
            if (a === o && mode != Mode.SETPIECE) {
                // Top sürme: ileri ve hafif merkeze
                val fwd = if (mode == Mode.ATTACK) 14f else if (d(a.side, a.x) > 80f) 2f else 8f
                a.tx = fromD(a.side, (d(a.side, a.x) + fwd).coerceAtMost(L - 6f))
                a.ty = a.y + (W / 2 - a.y) * 0.2f
                a.boost = if (mode == Mode.ATTACK) 0.95f else 0.75f
                continue
            }
            val prog = d(a.side, bx) / L
            val inPoss = a.side == attackSide
            if (a.gk) {
                if (fl != null && fl.kind == 1 && cur?.type == Ev.SAVE && !inPoss) { a.tx = fl.ex; a.ty = fl.ey; continue }
                val keepD = if (inPoss) 4f + prog * 14f else 2.5f + prog * 8f
                a.tx = fromD(a.side, keepD)
                a.ty = W / 2 + (by - W / 2) * (if (prog < 0.3f) 0.12f else 0.3f)
                if (mode == Mode.ATTACK && !inPoss) { a.tx = fromD(a.side, 1.5f); a.ty = W / 2 + (by - W / 2) * 0.1f }
                continue
            }
            if (fl != null && fl.to === a) { a.tx = fl.ex; a.ty = fl.ey; a.boost = 1.2f; continue }
            if (a in chasers) { a.tx = bx; a.ty = by; a.boost = 1.15f; continue }
            val sp = spot[a.id]
            if ((mode == Mode.ATTACK || mode == Mode.SETPIECE || mode == Mode.DEAD) && sp != null) {
                a.tx = sp.first; a.ty = sp.second; a.boost = 1.35f
                continue
            }
            if (!inPoss && a in pressers && mode == Mode.NORMAL || (!inPoss && mode == Mode.ATTACK && a === pressers.firstOrNull())) {
                if (a === pressers[0]) { a.tx = bx; a.ty = by }
                else {
                    val gx = fromD(a.side, 0f)
                    a.tx = bx + (gx - bx) * 0.12f; a.ty = by + (W / 2 - by) * 0.2f
                }
                a.boost = 1.05f
                continue
            }
            val atkPush = if (mode == Mode.ATTACK && inPoss) 10f else 0f
            val pr = if (mode == Mode.ATTACK && inPoss) max(prog, 0.72f) else prog
            val shift = if (inPoss) -6f + pr * 52f + atkPush else -10f + pr * 44f
            val dd = (a.baseD * (if (inPoss) 1f else 0.85f) + shift).coerceIn(3f, L - 4f)
            val lat = if (inPoss) a.baseY + (by - W / 2) * 0.28f
            else W / 2 + (a.baseY - W / 2) * 0.72f + (by - W / 2) * 0.38f
            val wob = sin(time * 0.6f + a.id * 1.7f) * 1.4f
            a.tx = fromD(a.side, dd + wob)
            a.ty = (lat + sin(time * 0.45f + a.id) * 1.2f).coerceIn(1.5f, W - 1.5f)
            a.boost = 1f
        }
    }

    private fun move(dt: Float) {
        for (a in agents) {
            val dx = a.tx - a.x
            val dy = a.ty - a.y
            val dist = hypot(dx, dy)
            val maxS = a.speed * a.boost
            val want = min(maxS, dist * 1.6f)
            val dvx = if (dist > 0.01f) dx / dist * want - a.vx else -a.vx
            val dvy = if (dist > 0.01f) dy / dist * want - a.vy else -a.vy
            val acc = 11f * dt
            val dl = hypot(dvx, dvy)
            val k = if (dl > acc) acc / dl else 1f
            a.vx += dvx * k
            a.vy += dvy * k
            // Takım arkadaşlarıyla çarpışmayı önle
            for (b in agents) {
                if (b === a || b.side != a.side) continue
                val sx = a.x - b.x
                val sy = a.y - b.y
                val sd = hypot(sx, sy)
                if (sd in 0.01f..2.4f) { a.vx += sx / sd * (2.4f - sd) * 2f * dt * 10f; a.vy += sy / sd * (2.4f - sd) * 2f * dt * 10f }
            }
            a.x = (a.x + a.vx * dt).coerceIn(-3f, L + 3f)
            a.y = (a.y + a.vy * dt).coerceIn(-3f, W + 3f)
            if (a.boost > 1.5f) a.boost = (a.boost - dt * 0.6f).coerceAtLeast(1f)
        }
    }

    private fun moveBall(dt: Float) {
        val fl = flight
        if (fl != null) {
            fl.t += dt
            val u = (fl.t / fl.dur).coerceAtMost(1f)
            val e = 1f - (1f - u).pow(1.4f)
            ballX = fl.sx + (fl.ex - fl.sx) * e
            ballY = fl.sy + (fl.ey - fl.sy) * e
            ballZ = 4f * fl.peak * u * (1f - u)
            if (u >= 1f) {
                flight = null
                ballZ = 0f
                when (fl.kind) {
                    1 -> shotArrived()
                    else -> if (fl.to != null) {
                        owner = fl.to
                        possSide = fl.to.side
                        hold = 0f
                    }
                }
            }
            return
        }
        val o = owner ?: return
        val sp = hypot(o.vx, o.vy)
        val fx = if (sp > 0.3f) o.vx / sp else if (o.side == 0) 1f else -1f
        val fy = if (sp > 0.3f) o.vy / sp else 0f
        val gx = o.x + fx * 0.8f
        val gy = o.y + fy * 0.8f
        val gd = dist(ballX, ballY, gx, gy)
        val maxStep = 24f * dt
        if (gd <= maxStep || gd < 0.3f) { ballX = gx; ballY = gy } else { ballX += (gx - ballX) / gd * maxStep; ballY += (gy - ballY) / gd * maxStep }
        ballZ = 0f
    }

    /** Topu süren ya da topa hareket eden oyuncu (ekranda adı yazılır). */
    fun focus(): Agent? = owner ?: flight?.to

    private fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val (qx, qy) = project(px, py, ax, ay, bx, by)
        return dist(px, py, qx, qy)
    }

    private fun project(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Pair<Float, Float> {
        val vx = bx - ax
        val vy = by - ay
        val l2 = vx * vx + vy * vy
        if (l2 < 1e-4f) return ax to ay
        val t = (((px - ax) * vx + (py - ay) * vy) / l2).coerceIn(0.25f, 1f)
        return ax + vx * t to ay + vy * t
    }
}
