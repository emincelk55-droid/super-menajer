package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.supermenajer.game.model.Comp
import com.supermenajer.game.model.Player
import kotlinx.coroutines.delay
import kotlin.math.abs

private val speeds = listOf(650L, 280L, 90L)

private fun colorDistance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

@Composable
fun MatchScreen() {
    val sim = GameRepo.match ?: return
    var tick by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var speed by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var showSubs by remember { mutableStateOf(false) }
    var goalFlash by remember { mutableStateOf<MatchEvent?>(null) }
    val haptic = LocalHapticFeedback.current
    BackHandler { running = false }

    LaunchedEffect(running, speed) {
        while (running && !sim.finished) {
            delay(speeds[speed])
            val before = sim.events.size
            sim.step()
            tick++
            val goal = sim.events.drop(before).lastOrNull { it.type == Ev.GOAL }
            if (goal != null) {
                goalFlash = goal
                if (goal.side == sim.userSide?.index) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (sim.minute == 45) running = false
        }
    }
    LaunchedEffect(goalFlash) {
        if (goalFlash != null) { delay(1800); goalFlash = null }
    }
    tick // yeniden çizim tetikleyicisi

    val homeC = Color(sim.home.team.color1)
    val awayC = Color(sim.away.team.color1).let { if (colorDistance(it, homeC) < 0.5f) Color(sim.away.team.color2) else it }

    Column(Modifier.fillMaxSize()) {
        ScoreBoard(sim)
        Box(Modifier.fillMaxWidth()) {
            LivePitch(sim, homeC, awayC, speeds[speed].toInt())
            androidx.compose.animation.AnimatedVisibility(
                visible = goalFlash != null, enter = scaleIn() + fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                val side = goalFlash?.side ?: 0
                val c = if (side == 0) homeC else awayC
                Text(
                    "GOOOL!", fontSize = 46.sp, fontWeight = FontWeight.Black, color = contentOn(c),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(c.copy(alpha = 0.92f)).padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
        // Kontroller
        Row(
            Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!sim.finished) {
                Chip(if (running) "⏸" else "▶", false) { running = !running }
                listOf("1x", "2x", "4x").forEachIndexed { i, s -> Chip(s, speed == i) { speed = i } }
                Spacer(Modifier.weight(1f))
                val us = sim.userSide
                Chip("🔁 ${us?.subsLeft ?: 0}", false) { running = false; showSubs = true }
            }
        }
        if (!sim.finished) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val t = sim.userSide?.team
                listOf("Defansif", "Dengeli", "Hücum").forEachIndexed { i, s ->
                    Chip(s, t?.mentality == i, Modifier.weight(1f)) { GameRepo.setMentality(i); tick++ }
                }
            }
        }
        if (sim.minute == 45 && !running) {
            Banner("DEVRE ARASI", "Değişiklik yapabilir veya taktiği ayarlayabilirsin.") {
                Button(onClick = { running = true }) { Text("2. YARIYI BAŞLAT") }
            }
        } else if (sim.finished) {
            Banner("MAÇ BİTTİ", null) {
                Button(onClick = { GameRepo.finishMatch() }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                    Text("MAÇ SONUCU ▶", fontWeight = FontWeight.Bold)
                }
            }
        } else if (!running) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                TextButton(onClick = { sim.runToEnd(); tick++ }) { Text("⏭ Sonuca git", color = TextDim) }
            }
        }
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Anlatım", tab == 0) { tab = 0 }
            Chip("İstatistik", tab == 1) { tab = 1 }
            Chip("Oyuncular", tab == 2) { tab = 2 }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> Commentary(sim, homeC, awayC)
                1 -> Stats(sim)
                else -> Ratings(sim)
            }
        }
    }
    if (showSubs) {
        val us = sim.userSide
        if (us != null) SubDialog(sim, us, onClose = { showSubs = false; tick++ })
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
private fun ScoreBoard(sim: MatchSim) {
    Column(Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val f = sim.fixture
        Text(
            if (f.comp == Comp.CUP) "TÜRKİYE KUPASI · ${Comp.cupRounds[f.round].uppercase()}" else "${Comp.name(f.comp).uppercase()} · ${sim.game.leagueWeekAt(f.week)}. HAFTA",
            color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(sim.home.team, 34.dp)
                Spacer(Modifier.width(6.dp))
                Text(sim.home.team.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("${sim.home.goals} - ${sim.away.goals}", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(if (sim.finished) "MS" else "${sim.minute}'", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (sim.penHome >= 0) Text("Pen. ${sim.penHome}-${sim.penAway}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(sim.away.team.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                Spacer(Modifier.width(6.dp))
                TeamBadge(sim.away.team, 34.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Bar(sim.minute / 90f, Green, Modifier.fillMaxWidth(), 3.dp)
    }
}

@Composable
private fun LivePitch(sim: MatchSim, homeC: Color, awayC: Color, stepMs: Int) {
    val dur = (stepMs * 0.9f).toInt()
    val bx by animateFloatAsState(sim.ballX, tween(dur), label = "bx")
    val by by animateFloatAsState(sim.ballY, tween(dur), label = "by")
    val shift by animateFloatAsState((sim.ballX - 0.5f) * 0.25f, tween(dur), label = "shift")
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.7f)) {
        drawPitch(vertical = false)
        val w = size.width
        val h = size.height
        val r = 6.dp.toPx()
        fun drawSide(s: Side, c: Color, home: Boolean) {
            val border = if (home) Color(s.team.color2) else Color.White
            for (m in s.onPitch) {
                val depth = m.slot.y * 0.46f
                val x = ((if (home) 0.04f + depth else 0.96f - depth) + shift).coerceIn(0.03f, 0.97f)
                val y = 0.08f + (if (home) m.slot.x else 1f - m.slot.x) * 0.84f
                drawCircle(c, r, Offset(x * w, y * h))
                drawCircle(border, r, Offset(x * w, y * h), style = Stroke(1.5.dp.toPx()))
            }
        }
        drawSide(sim.home, homeC, true)
        drawSide(sim.away, awayC, false)
        drawCircle(Color.Black.copy(alpha = 0.35f), 4.5.dp.toPx(), Offset(bx * w + 2, by * h + 2))
        drawCircle(Color.White, 4.dp.toPx(), Offset(bx * w, by * h))
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
private fun Commentary(sim: MatchSim, homeC: Color, awayC: Color) {
    val list = sim.events.asReversed().toList()
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
private fun Stats(sim: MatchSim) {
    val h = sim.home
    val a = sim.away
    val totalPoss = (h.poss + a.poss).coerceAtLeast(1)
    val rows = listOf(
        Triple("Topa sahip olma", "${h.poss * 100 / totalPoss}%", "${a.poss * 100 / totalPoss}%") to (h.poss.toFloat() / totalPoss),
        Triple("Şut", "${h.shots}", "${a.shots}") to ratio(h.shots, a.shots),
        Triple("İsabetli şut", "${h.onTarget}", "${a.onTarget}") to ratio(h.onTarget, a.onTarget),
        Triple("Gol beklentisi (xG)", "%.2f".format(h.xg), "%.2f".format(a.xg)) to ratioD(h.xg, a.xg),
        Triple("Korner", "${h.corners}", "${a.corners}") to ratio(h.corners, a.corners),
        Triple("Faul", "${h.fouls}", "${a.fouls}") to ratio(h.fouls, a.fouls),
        Triple("Sarı kart", "${h.yellows}", "${a.yellows}") to ratio(h.yellows, a.yellows),
        Triple("Kırmızı kart", "${h.reds}", "${a.reds}") to ratio(h.reds, a.reds),
    )
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(rows) { (t, frac) ->
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
    Dialog(onDismissRequest = onClose) {
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
                val bench = side.bench.sortedWith(compareByDescending<Player> { it.pos == o.slot.pos }.thenByDescending { it.ovrAt(o.slot.pos) })
                if (bench.isEmpty()) Text("Yedek oyuncu kalmadı.", color = TextDim)
                bench.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            sim.substitute(side, o, p); out = null; refresh++
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PosChip(p.pos)
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("Bu mevkide ", color = TextDim, fontSize = 12.sp)
                        OvrBadge(p.ovrAt(o.slot.pos), 30.dp)
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
    Column(Modifier.fillMaxSize()) {
        ScoreBoard(sim)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard("Goller") {
                    val goals = sim.events.filter { it.type == Ev.GOAL }
                    if (goals.isEmpty()) Text("Gol yok.", color = TextDim)
                    goals.forEach { e ->
                        val name = (sim.home.used + sim.away.used).firstOrNull { it.p.id == e.playerId }?.p?.name ?: ""
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            if (e.side == 0) {
                                Text("⚽ $name ${e.minute}'", modifier = Modifier.weight(1f))
                                Spacer(Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                                Text("${e.minute}' $name ⚽", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }
                    }
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
        Button(
            onClick = { GameRepo.closeSummary() },
            modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp), shape = RoundedCornerShape(12.dp),
        ) { Text("DEVAM", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

