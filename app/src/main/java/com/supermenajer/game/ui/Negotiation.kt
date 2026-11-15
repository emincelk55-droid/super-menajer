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
                    p, (r.first + r.last) / 2, com.supermenajer.game.game.Positions.det(p).code, 96.dp,
                    seller?.let { Color(it.color1) } ?: Color(g.userTeam.color1), seller?.let { Color(it.color2) } ?: Color.White,
                    known = r.first == r.last,
                )
                Spacer(Modifier.height(6.dp))
                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text(L("${Generator.flag(p.nation)} ${p.age} yaş · ${com.supermenajer.game.game.Positions.det(p).label}", "${Generator.flag(p.nation)} ${p.age} yrs · ${com.supermenajer.game.game.Positions.det(p).label}"), color = TextDim, fontSize = 11.sp)
                Text(com.supermenajer.game.game.Positions.natRoleName(p), color = Gold, fontSize = 11.sp)
                Text(seller?.name ?: if (p.teamId < 0) L("Serbest oyuncu", "Free agent") else g.userTeam.name, color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Info(L("Değer", "Value"), money(p.value))
                Info(L("Maaş isteği", "Wage demand"), L("${money(p.demandWage)}/hf", "${money(p.demandWage)}/wk"))
                Info(L("Bütçen", "Your budget"), money(g.userTeam.budget))
                val room = g.userTeam.wageBudget - Economy.squadWages(g, g.userTeamId)
                Info(L("Maaş boşluğu", "Wage room"), L("${money(room)}/hf", "${money(room)}/wk"), if (room < p.demandWage) Red else Green)
                Spacer(Modifier.height(6.dp))
                if (p.teamId >= 0 && n.stage == TransferTalks.CLUB) Patience(L("Kulüp sabrı", "Club patience"), n.clubPatience)
                if (n.stage == TransferTalks.AGENT) Patience(L("Menajer sabrı", "Agent patience"), n.agentPatience)
            }
            // Sağ: konuşma ve kontroller
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StageChip(L("1 · Kulüp", "1 · Club"), n.stage == TransferTalks.CLUB, done = n.stage > TransferTalks.CLUB && n.stage != TransferTalks.FAILED || (p.teamId < 0))
                    Text("  ›  ", color = TextDim)
                    StageChip(L("2 · Sözleşme", "2 · Contract"), n.stage == TransferTalks.AGENT, done = n.stage == TransferTalks.DONE)
                    Text("  ›  ", color = TextDim)
                    StageChip(L("✍️ İmza", "✍️ Signing"), n.stage == TransferTalks.DONE, done = false)
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
                        Text(L("🎉 Transfer tamamlandı · Kapat", "🎉 Transfer complete · Close"), fontWeight = FontWeight.Bold)
                    }
                    else -> OutlinedButton(onClick = { GameRepo.closeTalks() }, modifier = Modifier.fillMaxWidth()) { Text(L("Görüşmeler bitti · Kapat", "Talks over · Close")) }
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
            if (!n.loan) Stepper(L("Bonservis", "Fee"), money(n.fee), { n.fee = (n.fee - step).coerceAtLeast(25_000); GameRepo.bump() }, { n.fee += step; GameRepo.bump() })
            else Text(L("Kiralama: bonservis yok, maaşı biz öderiz", "Loan: no fee, we pay the wages"), color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
            Button(onClick = { GameRepo.talkClub() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) { Text(L("Teklif yap", "Make offer"), fontWeight = FontWeight.Bold, maxLines = 1) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(L("Peşin", "Upfront"), !n.installments && !n.loan) { n.installments = false; n.loan = false; GameRepo.bump() }
            Chip(L("Taksitli", "Instalments"), n.installments) { n.installments = true; n.loan = false; GameRepo.bump() }
            Chip(L("Kiralık", "Loan"), n.loan) { n.loan = !n.loan; n.installments = false; GameRepo.bump() }
            Text(L(" Sonraki satış:", " Sell-on:"), color = TextDim, fontSize = 11.sp)
            listOf(0, 10, 20).forEach { v -> Chip("%$v", n.sellOn == v) { n.sellOn = v; GameRepo.bump() } }
            n.counterFee?.let { c ->
                OutlinedButton(onClick = { n.fee = c; n.loan = false; GameRepo.talkClub() }) { Text(L("${money(c)} kabul", "${money(c)} accepted"), fontSize = 12.sp) }
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
            Stepper(L("Maaş / hafta", "Wage / week"), money(n.wage), { n.wage = (n.wage - wStep).coerceAtLeast(500); GameRepo.bump() }, { n.wage += wStep; GameRepo.bump() })
            Spacer(Modifier.width(8.dp))
            Stepper(L("İmza parası", "Signing bonus"), money(n.bonus), { n.bonus = (n.bonus - bStep).coerceAtLeast(0); GameRepo.bump() }, { n.bonus += bStep; GameRepo.bump() })
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!n.loan) {
                Text(L("Süre:", "Length:"), color = TextDim, fontSize = 11.sp)
                (1..5).forEach { y -> Chip(L("$y yıl", "$y yrs"), n.years == y) { n.years = y; GameRepo.bump() } }
            }
            Text(L(" Rol:", " Role:"), color = TextDim, fontSize = 11.sp)
            TransferTalks.roleNames.forEachIndexed { i, r -> Chip(r, n.role == i) { n.role = i; GameRepo.bump() } }
            n.counterWage?.let { c -> OutlinedButton(onClick = { n.wage = c; GameRepo.talkAgent() }) { Text(L("${money(c)}/hf kabul", "${money(c)}/wk accepted"), fontSize = 12.sp) } }
        }
        val sc = TransferTalks.agentScore(g, n)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                L("Menajerin havası: ", "Agent's mood: ") + when { sc >= 1.0 -> L("😊 olumlu", "😊 positive"); sc >= 0.9 -> L("🙂 yakın", "🙂 close"); sc >= 0.82 -> L("😐 kararsız", "😐 undecided"); else -> L("😠 uzak", "😠 far apart") },
                color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            Button(onClick = { GameRepo.talkAgent() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)) { Text(L("Teklif yap", "Make offer"), fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}
