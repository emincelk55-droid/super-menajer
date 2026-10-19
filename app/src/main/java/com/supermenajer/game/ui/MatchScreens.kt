package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supermenajer.game.engine.Ev
import com.supermenajer.game.engine.MatchEvent
import com.supermenajer.game.engine.MatchSim
import com.supermenajer.game.engine.PIM
import com.supermenajer.game.engine.Side
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.StaffLogic
import com.supermenajer.game.game.Talks
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Player
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Hız modları: dakika süresi, gerilim süresi, önemli olay sonrası bekleme (ms). */
private class Pace(val label: String, val minute: Long, val suspense: Long, val pause: Long, val scale: Float)

private val paces = listOf(
    Pace("▶ Normal", 650, 1500, 900, 1.0f),
    Pace("⏩ Hızlı", 220, 750, 350, 1.5f),
    Pace("⚡ Özet", 60, 1400, 900, 1.9f),
)

/** CM tarzı büyük anlatım satırı. kind: 0 normal, 1 gerilim, 2 gol */
private class Ticker(val minute: Int, val text: String, val side: Int, val kind: Int)

private fun colorDistance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

@Composable
fun MatchScreen() {
    val sim = GameRepo.match ?: return
    var tick by remember { mutableIntStateOf(0) }
    // Kullanıcı maçında önce maç öncesi konuşma yapılır
    var running by remember { mutableStateOf(sim.userSide == null || sim.minute > 0) }
    var preResult by remember { mutableStateOf<Talks.Result?>(null) }
    var speed by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableIntStateOf(sim.events.size) }
    var ticker by remember { mutableStateOf(sim.events.lastOrNull()?.let { Ticker(it.minute, it.text, it.side, 0) }) }
    var tab by remember { mutableIntStateOf(0) }
    var showSubs by remember { mutableStateOf(false) }
    var goalFlash by remember { mutableStateOf<MatchEvent?>(null) }
    var halfResult by remember { mutableStateOf<Talks.Result?>(null) }
    val haptic = LocalHapticFeedback.current
    BackHandler { running = false }

    val anim = remember(sim) { GameRepo.pitchFor(sim) }
    LaunchedEffect(running, speed) {
        val pace = paces[speed]
        // Duraklatılınca bekleyen olayları göster
        if (!running) { revealed = sim.events.size; return@LaunchedEffect }
        while (running && !sim.finished) {
            delay(pace.minute)
            val before = sim.events.size
            sim.step()
            val hm = sim.strength(sim.home).mid.pow(2)
            val am = sim.strength(sim.away).mid.pow(2)
            anim.bias = (hm / (hm + am)).toFloat()
            tick++
            val new = sim.events.subList(before, sim.events.size).toList()
            for ((k, e) in new.withIndex()) {
                val shotBefore = anim.outcomes
                anim.play(e)
                if (e.buildUp.isNotEmpty()) {
                    ticker = Ticker(e.minute, e.buildUp, e.side, 1)
                    // Atak sahada oynanır; sonuç top ağlara/kaleciye ulaşınca açıklanır
                    var waited = 0L
                    while (anim.outcomes == shotBefore && waited < 9000) { delay(40); waited += 40 }
                    delay(250)
                }
                revealed = before + k + 1
                val isGoal = e.type == Ev.GOAL
                ticker = Ticker(e.minute, e.text, e.side, if (isGoal) 2 else 0)
                if (isGoal) {
                    goalFlash = e
                    if (e.side == sim.userSide?.index) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                if (isGoal || e.buildUp.isNotEmpty() || e.type == Ev.RED || e.type == Ev.HALF || e.type == Ev.FULL) delay(pace.pause)
            }
            if (sim.minute == 45) running = false
        }
        revealed = sim.events.size
    }
    LaunchedEffect(goalFlash) {
        if (goalFlash != null) { delay(2600); goalFlash = null }
    }
    tick // yeniden çizim tetikleyicisi
    val shown = revealed.coerceAtMost(sim.events.size)
    val shownGoals = sim.events.take(shown).filter { it.type == Ev.GOAL }
    val scoreH = shownGoals.count { it.side == 0 }
    val scoreA = shownGoals.count { it.side == 1 }

    val homeC = Color(sim.home.team.color1)
    val awayC = Color(sim.away.team.color1).let { if (colorDistance(it, homeC) < 0.5f) Color(sim.away.team.color2) else it }

    Row(Modifier.fillMaxSize()) {
        // Sol: FM tarzı 2D saha, skor ve son olay
        Column(Modifier.weight(1.75f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF256F3B)), contentAlignment = Alignment.Center) {
                val gkH = keeperColor(homeC, awayC, Color(0xFF26C6DA))
                val gkA = keeperColor(homeC, awayC, Color(0xFFFF7043))
                MatchPitch(
                    anim, homeC, awayC, gkH, gkA, running, paces[speed].scale,
                    Modifier.fillMaxHeight().aspectRatio(PITCH_RATIO, matchHeightConstraintsFirst = true),
                )
                ScoreBug(sim, scoreH, scoreA, homeC, awayC, shown >= sim.events.size, Modifier.align(Alignment.TopStart).padding(8.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = goalFlash != null, enter = scaleIn() + fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                ) {
                    goalFlash?.let { GoalCard(sim, it, if (it.side == 0) homeC else awayC) }
                }
            }
            TickerBox(ticker, homeC, awayC)
        }
        // Sağ: kontroller, devre arası ve anlatım
        Column(Modifier.weight(1f).fillMaxHeight().background(Bg)) {
            LiveStrip(sim, homeC, awayC)
            if (!sim.finished) {
                Row(
                    Modifier.fillMaxWidth().background(Surface1).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Chip(if (running) "⏸" else "▶", false) { running = !running }
                    paces.forEachIndexed { i, pc -> Chip(pc.label, speed == i) { speed = i } }
                    val us = sim.userSide
                    Chip("🔁 ${us?.subsLeft ?: 0}", false) { running = false; showSubs = true }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val t = sim.userSide?.team
                    listOf("Defansif", "Dengeli", "Hücum").forEachIndexed { i, s ->
                        Chip(s, t?.mentality == i, Modifier.weight(1f)) { GameRepo.setMentality(i); tick++ }
                    }
                }
                if (sim.userSide != null) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (sim.canShout) "Bağır:" else "Bağır (${10 - (sim.minute - sim.lastShout)}')", color = TextDim, fontSize = 11.sp)
                        listOf("📣 Cesaret", "😠 Daha fazla", "🎯 Odaklan", "🧘 Sakin ol").forEachIndexed { i, s ->
                            Box(Modifier.alpha(if (sim.canShout) 1f else 0.4f)) {
                                Chip(s, false) { if (sim.canShout) { sim.shout(i); revealed = sim.events.size; tick++ } }
                            }
                        }
                    }
                }
            }
            if (sim.minute == 45 && !running) {
                Banner("DEVRE ARASI", halfResult?.message ?: "Takıma ne söyleyeceksin? Değişiklik ve taktik de yapabilirsin.") {
                    Button(onClick = { running = true }) { Text("2. YARI") }
                }
                halfResult?.let { r ->
                    Text(r.reactions.joinToString(" · "), color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                }
                if (!sim.talkDone && sim.userSide != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Talks.halfOptions.forEachIndexed { i, n ->
                            Chip(n, false, Modifier.weight(1f)) { halfResult = Talks.talk(sim, Talks.HALF, i); tick++ }
                        }
                    }
                }
            } else if (sim.finished) {
                Banner("MAÇ BİTTİ", null) {
                    Button(onClick = { GameRepo.finishMatch() }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                        Text("MAÇ SONUCU ▶", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!running) {
                TextButton(onClick = { sim.runToEnd(); revealed = sim.events.size; tick++ }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("⏭ Sonuca git", color = TextDim)
                }
            }
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Anlatım", tab == 0) { tab = 0 }
                Chip("İstatistik", tab == 1) { tab = 1 }
                Chip("Oyuncular", tab == 2) { tab = 2 }
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> Commentary(sim, homeC, awayC, shown)
                    1 -> Stats(sim)
                    else -> Ratings(sim)
                }
            }
        }
    }
    if (showSubs) {
        val us = sim.userSide
        if (us != null) SubDialog(sim, us, onClose = { showSubs = false; anim.sync(); tick++ })
    }
    if (sim.minute == 0 && !running && sim.userSide != null && !showSubs) {
        PreMatchDialog(sim, preResult, onTalk = { preResult = Talks.talk(sim, Talks.PRE, it); tick++ }, onStart = { running = true })
    }
}

/** Maç öncesi soyunma odası: bağlam, konuşma seçenekleri ve oyuncu tepkileri. */
@Composable
private fun PreMatchDialog(sim: MatchSim, result: Talks.Result?, onTalk: (Int) -> Unit, onStart: () -> Unit) {
    WideDialog(onDismissRequest = {}) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1)
                .verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Text("🗣 Soyunma odası", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("${sim.home.team.name} - ${sim.away.team.name}", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(Talks.contextText(sim), color = Gold, fontSize = 13.sp)
            Text("Yardımcı antrenör: ${StaffLogic.of(sim.game, StaffLogic.ASSISTANT)?.name ?: "-"} (${StaffLogic.q(sim.game, StaffLogic.ASSISTANT)}/20)", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (result == null) {
                Text("Maçtan önce takıma ne söyleyeceksin?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Talks.preOptions.withIndex().chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (i, o) ->
                            OutlinedButton(onClick = { onTalk(i) }, modifier = Modifier.weight(1f).padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp)) {
                                Text(o, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                TextButton(onClick = onStart) { Text("Konuşmadan maça başla", color = TextDim) }
            } else {
                Text(result.message, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                result.reactions.forEach { Text("• $it", fontSize = 13.sp, color = TextDim) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("MAÇA BAŞLA ▶", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TickerBox(t: Ticker?, homeC: Color, awayC: Color) {
    val c = when (t?.side) { 0 -> homeC; 1 -> awayC; else -> TextDim }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .background(if (t?.kind == 2) c.copy(alpha = 0.35f) else Surface2)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (t == null) return@Row
        Text("${t.minute}'", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(36.dp))
        Box(Modifier.width(4.dp).height(36.dp).background(c))
        Spacer(Modifier.width(10.dp))
        Text(
            if (t.kind == 1) "⚡ ${t.text}" else t.text,
            fontSize = if (t.kind == 2) 16.sp else 14.sp,
            fontWeight = if (t.kind == 0) FontWeight.Medium else FontWeight.Bold,
            fontStyle = if (t.kind == 1) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            color = if (t.kind == 1) Gold else TextMain,
            maxLines = 2,
        )
    }
}

@Composable
private fun Banner(title: String, sub: String?, action: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Surface2).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
            if (sub != null) Text(sub, color = TextDim, fontSize = 12.sp)
        }
        action()
    }
}

@Composable
private fun ScoreBoard(sim: MatchSim, hg: Int = sim.home.goals, ag: Int = sim.away.goals, revealedAll: Boolean = true) {
    Column(Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 12.dp, vertical = 6.dp)) {
        val f = sim.fixture
        Text(
            (if (Talks.isDerby(sim.game, f)) "🔥 DERBİ · " else "") + if (Comp.isCup(f.comp) || f.comp == Comp.EURO) "${Comp.name(f.comp).uppercase()} · ${Comp.roundName(f.comp, f.round).uppercase()}" else "${Comp.name(f.comp).uppercase()} · ${sim.game.leagueWeekAt(f.week)}. HAFTA",
            color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(sim.home.team, 30.dp)
                Spacer(Modifier.width(6.dp))
                Text(sim.home.team.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("$hg - $ag", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(if (sim.finished && revealedAll) "MS" else "${sim.minute}'", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (sim.penHome >= 0 && revealedAll) Text("Pen. ${sim.penHome}-${sim.penAway}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(sim.away.team.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                Spacer(Modifier.width(6.dp))
                TeamBadge(sim.away.team, 30.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Bar(sim.minute / 90f, Green, Modifier.fillMaxWidth(), 3.dp)
    }
}

/** Topa sahip olma ve şutlar: maç sırasında ince bir şerit. */
@Composable
private fun LiveStrip(sim: MatchSim, homeC: Color, awayC: Color) {
    val h = sim.home
    val a = sim.away
    val tot = (h.poss + a.poss).coerceAtLeast(1)
    val hp = h.poss.toFloat() / tot
    Row(Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${(hp * 100).roundToInt()}%", fontSize = 11.sp, color = TextDim, modifier = Modifier.width(34.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(awayC)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (h.poss + a.poss == 0) 0.5f else hp).background(homeC))
        }
        Text("${100 - (hp * 100).roundToInt()}%", fontSize = 11.sp, color = TextDim, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text("Şut ${h.shots}-${a.shots}", fontSize = 11.sp, color = TextDim)
    }
}

private fun evIcon(t: Ev) = when (t) {
    Ev.GOAL -> "⚽"
    Ev.SAVE -> "🧤"
    Ev.MISS -> "↗️"
    Ev.POST -> "🥅"
    Ev.CORNER -> "🚩"
    Ev.YELLOW -> "🟨"
    Ev.RED -> "🟥"
    Ev.INJURY -> "🚑"
    Ev.SUB -> "🔁"
    Ev.HALF, Ev.FULL, Ev.KICKOFF -> "⏱️"
    Ev.INFO -> "🎙️"
}

@Composable
private fun Commentary(sim: MatchSim, homeC: Color, awayC: Color, shown: Int = sim.events.size) {
    val list = sim.events.take(shown).asReversed()
    LazyColumn(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
        items(list) { e ->
            val isGoal = e.type == Ev.GOAL
            val c = when (e.side) { 0 -> homeC; 1 -> awayC; else -> Color.Transparent }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isGoal) c.copy(alpha = 0.25f) else if (e.type == Ev.HALF || e.type == Ev.FULL) Surface2 else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("${e.minute}'", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                Box(Modifier.width(3.dp).height(18.dp).background(c))
                Spacer(Modifier.width(6.dp))
                Text(evIcon(e.type), fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(e.text, fontSize = 14.sp, fontWeight = if (isGoal || e.type == Ev.RED) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Stats(sim: MatchSim) {
    val h = sim.home
    val a = sim.away
    val totalPoss = (h.poss + a.poss).coerceAtLeast(1)
    val rows = listOf(
        Triple("Topa sahip olma", "${(h.poss * 100f / totalPoss).roundToInt()}%", "${100 - (h.poss * 100f / totalPoss).roundToInt()}%") to (h.poss.toFloat() / totalPoss),
        Triple("Şut", "${h.shots}", "${a.shots}") to ratio(h.shots, a.shots),
        Triple("İsabetli şut", "${h.onTarget}", "${a.onTarget}") to ratio(h.onTarget, a.onTarget),
        Triple("Gol beklentisi (xG)", "%.2f".format(h.xg), "%.2f".format(a.xg)) to ratioD(h.xg, a.xg),
        Triple("Korner", "${h.corners}", "${a.corners}") to ratio(h.corners, a.corners),
        Triple("Faul", "${h.fouls}", "${a.fouls}") to ratio(h.fouls, a.fouls),
        Triple("Sarı kart", "${h.yellows}", "${a.yellows}") to ratio(h.yellows, a.yellows),
        Triple("Kırmızı kart", "${h.reds}", "${a.reds}") to ratio(h.reds, a.reds),
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { (t, frac) ->
            Column {
                Row {
                    Text(t.second, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                    Text(t.first, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(t.third, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(4.dp))
                Bar(frac, Green, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun ratio(a: Int, b: Int) = if (a + b == 0) 0.5f else a.toFloat() / (a + b)
private fun ratioD(a: Double, b: Double) = if (a + b <= 0) 0.5f else (a / (a + b)).toFloat()

@Composable
private fun Ratings(sim: MatchSim) {
    LazyColumn(contentPadding = PaddingValues(10.dp)) {
        for (s in sim.sides) {
            item {
                Text(s.team.name.uppercase(), color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(s.used) { m -> PimRow(m, onPitch = m in s.onPitch) }
        }
    }
}

@Composable
private fun PimRow(m: PIM, onPitch: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosChip(m.slot.pos)
        Spacer(Modifier.width(8.dp))
        Text(
            buildString {
                append(m.p.name)
                if (m.goals > 0) append(" " + "⚽".repeat(m.goals))
                if (m.assists > 0) append(" 🅰️")
                if (m.yellow > 0 && !m.red) append(" 🟨")
                if (m.red) append(" 🟥")
                if (m.injured) append(" 🚑")
                if (m.subbedOn) append(" ↑")
                if (!onPitch && !m.red && !m.injured) append(" ↓")
            },
            modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (onPitch) TextMain else TextDim,
        )
        Bar((m.fit / 100).toFloat(), fitColor(m.fit), Modifier.width(40.dp), 5.dp)
        Spacer(Modifier.width(8.dp))
        Text("%.1f".format(m.rating), color = ratingColor(m.rating), fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun SubDialog(sim: MatchSim, side: Side, onClose: () -> Unit) {
    var out by remember { mutableStateOf<PIM?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    refresh
    WideDialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).clip(RoundedCornerShape(18.dp)).background(Surface1)
                .verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text("Oyuncu Değişikliği", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Kalan hak: ${side.subsLeft}", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            val o = out
            if (side.subsLeft <= 0 || sim.finished) {
                Text("Değişiklik hakkın kalmadı.", color = Gold)
            } else if (o == null) {
                Text("ÇIKACAK OYUNCU", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                side.onPitch.forEach { m -> PimRow(m, true) { out = m } }
            } else {
                Text("${o.p.name} yerine girecek oyuncu (${o.slot.pos.label}):", color = Gold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                val bench = side.bench.sortedWith(compareByDescending<Player> { it.pos == o.slot.pos || com.supermenajer.game.game.Positions.canPlay(it, com.supermenajer.game.game.Positions.slotDet(o.slot)) }.thenByDescending { com.supermenajer.game.game.Positions.rating(it, o.slot) })
                if (bench.isEmpty()) Text("Yedek oyuncu kalmadı.", color = TextDim)
                bench.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            sim.substitute(side, o, p); out = null; refresh++
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetChip(p)
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("Bu mevkide ", color = TextDim, fontSize = 12.sp)
                        OvrBadge(com.supermenajer.game.game.Positions.rating(p, o.slot), 30.dp)
                    }
                }
                TextButton(onClick = { out = null }) { Text("← Geri") }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Tamam") }
        }
    }
}

@Composable
fun SummaryScreen() {
    GameRepo.version
    val sim = GameRepo.match
    val g = GameRepo.game
    if (sim == null || g == null) {
        GameRepo.closeSummary(); return
    }
    BackHandler { GameRepo.closeSummary() }
    val motm = sim.manOfTheMatch()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            ScoreBoard(sim)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sim.userSide != null) item { PostTalkCard(sim) }
                item {
                    SectionCard("Goller") {
                        val goals = sim.events.filter { it.type == Ev.GOAL }
                        if (goals.isEmpty()) Text("Gol yok.", color = TextDim)
                        goals.forEach { e ->
                            val name = (sim.home.used + sim.away.used).firstOrNull { it.p.id == e.playerId }?.p?.name ?: ""
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                if (e.side == 0) {
                                    Text("⚽ $name ${e.minute}'", modifier = Modifier.weight(1f), maxLines = 1)
                                } else {
                                    Text("${e.minute}' $name ⚽", modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                com.supermenajer.game.game.Facilities.crowdText(sim).takeIf { it.isNotEmpty() }?.let { crowd ->
                    item {
                        val us = sim.home.team.id == g.userTeamId
                        val income = if (us) " · Bilet geliri ${com.supermenajer.game.game.money(com.supermenajer.game.game.Facilities.ticketIncome(g, sim.fixture))}" else ""
                        Text("🏟️ $crowd$income", color = TextDim, fontSize = 12.sp)
                    }
                }
                if (motm != null) item {
                    SectionCard("Maçın oyuncusu") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(motm.p.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(g.team(if (motm in sim.home.used) sim.home.team.id else sim.away.team.id).name, color = TextDim, fontSize = 13.sp)
                            }
                            Text("%.1f".format(motm.rating), color = ratingColor(motm.rating), fontWeight = FontWeight.Black, fontSize = 24.sp)
                        }
                    }
                }
            }
            Button(
                onClick = { GameRepo.closeSummary() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).height(48.dp), shape = RoundedCornerShape(12.dp),
            ) { Text("DEVAM", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCard("Maç istatistikleri") { Box(Modifier.heightIn(max = 520.dp)) { Stats(sim) } }
            }
            sim.userSide?.let { us ->
                item {
                    SectionCard("Oyuncu puanları") {
                        us.used.sortedByDescending { it.rating }.forEach { PimRow(it, onPitch = true) }
                    }
                }
            }
            item {
                SectionCard("Diğer sonuçlar · ${Comp.name(sim.fixture.comp)}") {
                    g.weekFixtures(sim.fixture.week).filter { it !== sim.fixture && it.comp == sim.fixture.comp }.forEachIndexed { i, f ->
                        if (i > 0) HorizontalDivider(color = Line)
                        ResultRow(g, f)
                    }
                }
            }
        }
    }
}


@Composable
private fun PostTalkCard(sim: MatchSim) {
    var result by remember { mutableStateOf<Talks.Result?>(null) }
    SectionCard("🗣 Maç sonu konuşması") {
        val r = result
        if (r == null && !sim.talksDone[Talks.POST]) {
            Text("Soyunma odasında oyunculara ne diyeceksin?", fontSize = 13.sp, color = TextDim)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Talks.postOptions.forEachIndexed { i, o -> Chip(o, false, Modifier.weight(1f)) { result = Talks.talk(sim, Talks.POST, i); GameRepo.bump() } }
            }
        } else if (r != null) {
            Text(r.message, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            r.reactions.forEach { Text("• $it", fontSize = 12.sp, color = TextDim) }
        } else {
            Text("Konuşma yapıldı.", color = TextDim, fontSize = 13.sp)
        }
    }
}

private fun keeperColor(homeC: Color, awayC: Color, want: Color): Color =
    if (colorDistance(want, homeC) < 0.6f || colorDistance(want, awayC) < 0.6f) Color(0xFF212121) else want

/** FM tarzı sol üst skor rozeti. */
@Composable
private fun ScoreBug(sim: MatchSim, hg: Int, ag: Int, homeC: Color, awayC: Color, revealedAll: Boolean, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xE60E1426)), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (sim.finished && revealedAll) "MS" else "${sim.minute}:00".padStart(5, '0'),
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.background(Color(0xFF3B2A78)).padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Text(
            sim.home.team.short, color = contentOn(homeC), fontWeight = FontWeight.Black, fontSize = 13.sp,
            modifier = Modifier.background(homeC).padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Text("$hg - $ag", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Text(
            sim.away.team.short, color = contentOn(awayC), fontWeight = FontWeight.Black, fontSize = 13.sp,
            modifier = Modifier.background(awayC).padding(horizontal = 8.dp, vertical = 5.dp),
        )
        if (sim.penHome >= 0 && revealedAll) Text(" (P ${sim.penHome}-${sim.penAway}) ", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Gol kartı: atan oyuncu ve sezondaki gol sayısı. */
@Composable
private fun GoalCard(sim: MatchSim, e: MatchEvent, c: Color) {
    val side = sim.sides[e.side.coerceIn(0, 1)]
    val pim = side.used.firstOrNull { it.p.id == e.playerId }
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(58.dp).height(58.dp).background(c), contentAlignment = Alignment.Center) {
            Text("⚽\n${side.team.short}", color = contentOn(c), fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            Text(
                (pim?.p?.number?.takeIf { it > 0 }?.let { "$it " } ?: "") + (pim?.p?.name ?: "GOL!"),
                color = Color(0xFF1A1A2E), fontWeight = FontWeight.Black, fontSize = 17.sp,
            )
            val n = (pim?.p?.goals ?: 0) + (pim?.goals ?: 0)
            Text(if (pim != null) "Sezonun $n. golü" else "", color = Color(0xFF5A5A70), fontSize = 13.sp)
        }
    }
}
