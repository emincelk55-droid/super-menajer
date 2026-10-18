package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.TransferTalks
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game

/** Diyaloglu transfer masası: solda oyuncu, sağda konuşma ve teklif kontrolleri. */
@Composable
fun NegotiationDialog(g: Game, n: TransferTalks.Negotiation) {
    GameRepo.version
    val p = n.player
    val seller = if (p.teamId >= 0 && n.stage != TransferTalks.DONE) g.teamOrNull(p.teamId) else null
    WideDialog(onDismissRequest = { GameRepo.closeTalks() }, maxWidth = 900.dp) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight(0.97f).clip(RoundedCornerShape(18.dp)).background(Surface1),
        ) {
            // Sol: oyuncu
            Column(
                Modifier.width(190.dp).fillMaxHeight().background(Surface2).verticalScroll(rememberScrollState()).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val r = com.supermenajer.game.game.Scouting.range(g, p, p.overall)
                PlayerCard(
                    p, (r.first + r.last) / 2, p.pos.short, 96.dp,
                    seller?.let { Color(it.color1) } ?: Color(g.userTeam.color1), seller?.let { Color(it.color2) } ?: Color.White,
                    known = r.first == r.last,
                )
                Spacer(Modifier.height(6.dp))
                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("${Generator.flag(p.nation)} ${p.age} yaş · ${p.pos.label}", color = TextDim, fontSize = 11.sp)
                Text(seller?.name ?: if (p.teamId < 0) "Serbest oyuncu" else g.userTeam.name, color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Info("Değer", money(p.value))
                Info("Maaş isteği", "${money(p.demandWage)}/hf")
                Info("Bütçen", money(g.userTeam.budget))
                val room = g.userTeam.wageBudget - Economy.squadWages(g, g.userTeamId)
                Info("Maaş boşluğu", "${money(room)}/hf", if (room < p.demandWage) Red else Green)
                Spacer(Modifier.height(6.dp))
                if (p.teamId >= 0 && n.stage == TransferTalks.CLUB) Patience("Kulüp sabrı", n.clubPatience)
                if (n.stage == TransferTalks.AGENT) Patience("Menajer sabrı", n.agentPatience)
            }
            // Sağ: konuşma ve kontroller
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StageChip("1 · Kulüp", n.stage == TransferTalks.CLUB, done = n.stage > TransferTalks.CLUB && n.stage != TransferTalks.FAILED || (p.teamId < 0))
                    Text("  ›  ", color = TextDim)
                    StageChip("2 · Sözleşme", n.stage == TransferTalks.AGENT, done = n.stage == TransferTalks.DONE)
                    Text("  ›  ", color = TextDim)
                    StageChip("✍️ İmza", n.stage == TransferTalks.DONE, done = false)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { GameRepo.closeTalks() }) { Text("✕", color = TextDim, fontSize = 18.sp) }
                }
                val listState = rememberLazyListState()
                LaunchedEffect(n.log.size) { if (n.log.isNotEmpty()) listState.animateScrollToItem(n.log.size - 1) }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(n.log) { Bubble(it) }
                }
                Spacer(Modifier.height(6.dp))
                when (n.stage) {
                    TransferTalks.CLUB -> ClubControls(n)
                    TransferTalks.AGENT -> AgentControls(g, n)
                    TransferTalks.DONE -> Button(onClick = { GameRepo.closeTalks() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                        Text("🎉 Transfer tamamlandı · Kapat", fontWeight = FontWeight.Bold)
                    }
                    else -> OutlinedButton(onClick = { GameRepo.closeTalks() }, modifier = Modifier.fillMaxWidth()) { Text("Görüşmeler bitti · Kapat") }
                }
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String, color: Color = TextMain) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Patience(label: String, v: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
        repeat(3) { i ->
            Box(Modifier.padding(start = 3.dp).size(9.dp).clip(CircleShape).background(if (i < v) (if (v == 1) Red else Gold) else Line))
        }
    }
}

@Composable
private fun StageChip(text: String, active: Boolean, done: Boolean) {
    Text(
        (if (done) "✓ " else "") + text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = if (active) Color.Black else if (done) Green else TextDim,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (active) Green else Surface2).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Bubble(l: TransferTalks.Line) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (l.ours) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 460.dp).clip(RoundedCornerShape(12.dp))
                .background(if (l.ours) Green.copy(alpha = 0.22f) else Surface2).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(l.who, color = if (l.ours) Green else Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(l.text, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.width(62.dp), maxLines = 2, lineHeight = 12.sp)
        OutlinedButton(onClick = onMinus, modifier = Modifier.size(34.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−") }
        Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.width(80.dp))
        OutlinedButton(onClick = onPlus, modifier = Modifier.size(34.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+") }
    }
}

@Composable
private fun ClubControls(n: TransferTalks.Negotiation) {
    val step = (GameRepo.askingPrice(n.player) / 20 / 25_000 * 25_000).coerceAtLeast(25_000)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!n.loan) Stepper("Bonservis", money(n.fee), { n.fee = (n.fee - step).coerceAtLeast(25_000); GameRepo.bump() }, { n.fee += step; GameRepo.bump() })
            else Text("Kiralama: bonservis yok, maaşı biz öderiz", color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
            Button(onClick = { GameRepo.talkClub() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) { Text("Teklif yap", fontWeight = FontWeight.Bold, maxLines = 1) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Peşin", !n.installments && !n.loan) { n.installments = false; n.loan = false; GameRepo.bump() }
            Chip("Taksitli", n.installments) { n.installments = true; n.loan = false; GameRepo.bump() }
            Chip("Kiralık", n.loan) { n.loan = !n.loan; n.installments = false; GameRepo.bump() }
            Text(" Sonraki satış:", color = TextDim, fontSize = 11.sp)
            listOf(0, 10, 20).forEach { v -> Chip("%$v", n.sellOn == v) { n.sellOn = v; GameRepo.bump() } }
            n.counterFee?.let { c ->
                OutlinedButton(onClick = { n.fee = c; n.loan = false; GameRepo.talkClub() }) { Text("${money(c)} kabul", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun AgentControls(g: Game, n: TransferTalks.Negotiation) {
    val p = n.player
    val wStep = (p.demandWage / 20 / 500 * 500).coerceAtLeast(500)
    val bStep = (p.demandWage * 2 / 25_000 * 25_000).coerceAtLeast(25_000)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stepper("Maaş / hafta", money(n.wage), { n.wage = (n.wage - wStep).coerceAtLeast(500); GameRepo.bump() }, { n.wage += wStep; GameRepo.bump() })
            Spacer(Modifier.width(8.dp))
            Stepper("İmza parası", money(n.bonus), { n.bonus = (n.bonus - bStep).coerceAtLeast(0); GameRepo.bump() }, { n.bonus += bStep; GameRepo.bump() })
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!n.loan) {
                Text("Süre:", color = TextDim, fontSize = 11.sp)
                (1..5).forEach { y -> Chip("$y yıl", n.years == y) { n.years = y; GameRepo.bump() } }
            }
            Text(" Rol:", color = TextDim, fontSize = 11.sp)
            TransferTalks.roleNames.forEachIndexed { i, r -> Chip(r, n.role == i) { n.role = i; GameRepo.bump() } }
            n.counterWage?.let { c -> OutlinedButton(onClick = { n.wage = c; GameRepo.talkAgent() }) { Text("${money(c)}/hf kabul", fontSize = 12.sp) } }
        }
        val sc = TransferTalks.agentScore(g, n)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                "Menajerin havası: " + when { sc >= 1.0 -> "😊 olumlu"; sc >= 0.9 -> "🙂 yakın"; sc >= 0.82 -> "😐 kararsız"; else -> "😠 uzak" },
                color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            Button(onClick = { GameRepo.talkAgent() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)) { Text("Teklif yap", fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}
