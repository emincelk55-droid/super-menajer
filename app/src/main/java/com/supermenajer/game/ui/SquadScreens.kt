package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Offer
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.math.roundToLong

@Composable
fun SquadTab(g: Game) {
    val t = g.userTeam
    val squad = g.squad(t.id)
    var sort by remember { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip("Mevki", sort == 0) { sort = 0 }
                Chip("Güç", sort == 1) { sort = 1 }
                Chip("Yaş", sort == 2) { sort = 2 }
                Chip("Gol", sort == 3) { sort = 3 }
                Chip("Puan", sort == 4) { sort = 4 }
            }
            Text(
                "${squad.size} oyuncu · Maaşlar: ${money(squad.sumOf { it.wage })} / ${money(t.wageBudget)} haftalık limit" +
                    squad.count { it.contractYears == 1 }.let { if (it > 0) " · $it sözleşme bitiyor" else "" },
                color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
        }
        if (sort == 0) {
            for (pos in Pos.entries) {
                val list = squad.filter { it.pos == pos }.sortedByDescending { it.overall }
                item(key = "h$pos") {
                    Text(
                        pos.label.uppercase() + " (${list.size})", color = posColor(pos), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 4.dp),
                    )
                }
                items(list, key = { it.id }) { p -> PlayerRow(g, p, p.id in t.lineup) }
            }
        } else {
            val list = when (sort) {
                1 -> squad.sortedByDescending { it.overall }
                2 -> squad.sortedBy { it.age }
                3 -> squad.sortedByDescending { it.goals * 100 + it.assists }
                else -> squad.sortedByDescending { it.avgRating }
            }
            items(list, key = { it.id }) { p -> PlayerRow(g, p, p.id in t.lineup) }
        }
    }
}

@Composable
fun PlayerRow(g: Game, p: Player, starting: Boolean, extra: String? = null, onClick: () -> Unit = { GameRepo.selectedPlayer = p }) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (starting) Green else Color.Transparent))
        Spacer(Modifier.width(8.dp))
        PosChip(p.pos)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                val dev = p.overall - p.startOvr
                if (p.startOvr > 0 && dev != 0) Text(if (dev > 0) "  ▲$dev" else "  ▼${-dev}", fontSize = 12.sp, color = if (dev > 0) Green else Red, fontWeight = FontWeight.Bold)
                if (p.injuredWeeks > 0) Text("  🚑${p.injuredWeeks}h", fontSize = 12.sp, color = Red)
                if (p.teamId == g.userTeamId && p.contractYears == 1) Text("  📝", fontSize = 12.sp)
                if (p.suspended > 0) Text("  🟥", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${Generator.flag(p.nation)} ${p.age} yaş", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Bar((p.fitness / 100).toFloat(), fitColor(p.fitness), Modifier.width(44.dp), 5.dp)
                Spacer(Modifier.width(8.dp))
                Text(extra ?: (if (p.apps > 0) "${p.apps}M ${p.goals}G ${p.assists}A · ${"%.1f".format(p.avgRating)}" else ""), color = TextDim, fontSize = 12.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        OvrBadge(p.overall)
    }
}

@Composable
fun PlayerDialog(g: Game, p: Player) {
    val own = p.teamId == g.userTeamId
    var offer by remember(p.id) { mutableStateOf<Offer?>(null) }
    var message by remember(p.id) { mutableStateOf<String?>(null) }
    val team = if (p.teamId >= 0) g.team(p.teamId) else null
    val actions: @Composable () -> Unit = {
        Column {
            message?.let {
                Text(it, color = Gold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (own) {
                val t = g.userTeam
                if (p.id in t.lineup) {
                    OutlinedButton(onClick = { GameRepo.removeFromLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text("Yedeğe Al") }
                } else if (p.available) {
                    Button(onClick = { GameRepo.addToLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text("İlk 11'e Al") }
                }
                Spacer(Modifier.height(6.dp))
                ContractBox(g, p) { message = it }
                Spacer(Modifier.height(6.dp))
                val o = offer
                if (o == null) {
                    OutlinedButton(onClick = {
                        offer = GameRepo.makeOffer(p)
                        if (offer == null) message = if (!Season.transferWindowOpen(g)) "Transfer dönemi kapalı." else "Şu an bu oyuncu için teklif yapan kulüp yok (veya kadro çok dar)."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Satışa Çıkar") }
                } else {
                    Text("${o.buyer.name} teklif yaptı: ${money(o.price)}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { GameRepo.acceptOffer(o) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        ) { Text("Kabul Et") }
                        OutlinedButton(onClick = { offer = null }, modifier = Modifier.weight(1f)) { Text("Reddet") }
                    }
                }
            } else {
                val asking = GameRepo.askingPrice(p)
                val step = (asking / 20 / 25_000 * 25_000).coerceAtLeast(25_000)
                var bid by remember(p.id) {
                    mutableLongStateOf(GameRepo.counterFor(p) ?: if (p.teamId < 0) asking else (asking * 0.85 / 25_000).roundToLong() * 25_000)
                }
                val block = GameRepo.transferBlock(p)
                if (block != null) {
                    Text(block, color = Red, fontSize = 13.sp)
                } else {
                    Text(
                        if (p.teamId < 0) "Serbest oyuncu · imza parası pazarlığı" else "Bonservis pazarlığı · ${team?.name}",
                        color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    GameRepo.counterFor(p)?.let { Text("Son karşı teklif: ${money(it)}", color = Gold, fontSize = 13.sp) }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { bid = (bid - step).coerceAtLeast(25_000) }) { Text("−") }
                        Text(money(bid), fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { bid += step }) { Text("+") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            when (val r = GameRepo.offerFor(p, bid)) {
                                is GameRepo.Reply.Accepted -> GameRepo.selectedPlayer = null
                                is GameRepo.Reply.Counter -> { message = "${team?.name ?: "Kulüp"} karşı teklif yaptı: ${money(r.price)}. Kabul edersen teklifini bu rakama çıkar."; bid = r.price }
                                is GameRepo.Reply.Rejected -> message = r.msg
                            }
                        },
                        enabled = bid <= g.userTeam.budget,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Teklif Yap", fontWeight = FontWeight.Bold) }
                }
                Text("Bütçen: ${money(g.userTeam.budget)}", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Dialog(onDismissRequest = { GameRepo.selectedPlayer = null }) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 640.dp).clip(RoundedCornerShape(18.dp)).background(Surface1)
                .verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OvrBadge(p.overall, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PosChip(p.pos)
                        Text("  ${Generator.flag(p.nation)} ${p.age} yaş", color = TextDim, fontSize = 13.sp)
                    }
                    Text(team?.name ?: "Serbest oyuncu", color = TextDim, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (!own) {
                actions()
                Spacer(Modifier.height(14.dp))
            }
            val potText = if (own || p.age >= 27) "${p.potential}" else "${(p.potential - 4).coerceAtLeast(p.overall)}-${(p.potential + 4).coerceAtMost(99)}"
            InfoLine("Potansiyel", potText)
            InfoLine("Değer", money(p.value))
            if (p.teamId >= 0) {
                InfoLine("Maaş", "${money(p.wage)}/hafta")
                InfoLine("Sözleşme", if (p.contractYears == 1) "Sezon sonunda bitiyor" else "${p.contractYears} sezon", if (p.contractYears == 1) Red else TextMain)
            }
            if (!own) InfoLine("Maaş talebi", "${money(p.demandWage)}/hafta")
            InfoLine("Kondisyon", "${p.fitness.toInt()}%")
            InfoLine("Moral", moraleText(p.morale))
            if (p.injuredWeeks > 0) InfoLine("Sakatlık", "${p.injuredWeeks} hafta", Red)
            if (p.suspended > 0) InfoLine("Ceza", "${p.suspended} maç", Red)
            Spacer(Modifier.height(12.dp))
            Text("ÖZELLİKLER", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (p.pos == Pos.GK) Attr("Kalecilik", p.goalkeeping)
            Attr("Hız", p.pace)
            Attr("Şut", p.shooting)
            Attr("Pas", p.passing)
            Attr("Teknik", p.dribbling)
            Attr("Savunma", p.defending)
            Attr("Fizik", p.physical)
            Spacer(Modifier.height(12.dp))
            Text("BU SEZON", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Stat("Maç", "${p.apps}")
                Stat("Gol", "${p.goals}")
                Stat("Asist", "${p.assists}")
                Stat("Ort. Puan", if (p.apps > 0) "%.2f".format(p.avgRating) else "-")
            }
            Spacer(Modifier.height(16.dp))
            if (own) actions()
            TextButton(onClick = { GameRepo.selectedPlayer = null }, modifier = Modifier.align(Alignment.End)) { Text("Kapat") }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, color: Color = TextMain) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Attr(label: String, v: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(80.dp))
        Bar(v / 100f, ovrColor(v), Modifier.weight(1f), 8.dp)
        Text("$v", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp).padding(start = 8.dp))
    }
}

@Composable
private fun Stat(label: String, v: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, fontSize = 11.sp, color = TextDim)
    }
}

@Composable
fun TransferTab(g: Game) {
    var posFilter by remember { mutableStateOf<Pos?>(null) }
    var mode by remember { mutableIntStateOf(0) } // 0 tümü, 1 serbest, 2 bütçeme uygun, 3 genç yetenek
    var country by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) } // 0 güç, 1 potansiyel, 2 ucuz, 3 genç
    val budget = g.userTeam.budget
    val version = GameRepo.version
    val list = remember(version, posFilter, mode, country, query, sort) {
        val q = query.trim().lowercase()
        g.players.asSequence()
            .filter { it.teamId != g.userTeamId }
            .filter { posFilter == null || it.pos == posFilter }
            .filter { country == null || (it.teamId >= 0 && g.teamOrNull(it.teamId)?.let { t -> g.countryOf(t) } == country) }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) }
            .filter {
                when (mode) {
                    1 -> it.teamId < 0
                    2 -> GameRepo.askingPrice(it) <= budget && com.supermenajer.game.game.Economy.fitsWages(g, g.userTeam, it.demandWage)
                    3 -> it.age <= 21 && it.potential >= 70
                    else -> true
                }
            }
            .sortedWith(
                when (sort) {
                    1 -> compareByDescending<Player> { it.potential }
                    2 -> compareBy<Player> { GameRepo.askingPrice(it) }.thenByDescending { it.overall }
                    3 -> compareBy<Player> { it.age }.thenByDescending { it.potential }
                    else -> compareByDescending<Player> { it.overall }
                }
            )
            .take(150).toList()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Column(Modifier.padding(12.dp)) {
                val open = Season.transferWindowOpen(g)
                Text(
                    Season.windowText(g), color = if (open) Green else Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface1).padding(10.dp),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it.take(30) }, singleLine = true,
                    placeholder = { Text("🔍 Oyuncu ara") }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Tümü", mode == 0) { mode = 0 }
                    Chip("Bütçeme uygun", mode == 2) { mode = 2 }
                    Chip("Serbest", mode == 1) { mode = 1 }
                    Chip("Genç yetenek", mode == 3) { mode = 3 }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("🌍 Tüm ülkeler", country == null) { country = null }
                    Season.activeCountries(g).forEach { c -> Chip("${Generator.flag(com.supermenajer.game.model.Leagues.flagOf(c))} $c", country == c) { country = c } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Her mevki", posFilter == null) { posFilter = null }
                    Pos.entries.forEach { p -> Chip(p.label, posFilter == p) { posFilter = p } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sırala: Güç", "Potansiyel", "En ucuz", "En genç").forEachIndexed { i, n -> Chip(n, sort == i) { sort = i } }
                }
            }
        }
        if (list.isEmpty()) item { Text("Uygun oyuncu bulunamadı.", color = TextDim, modifier = Modifier.padding(16.dp)) }
        items(list, key = { it.id }) { p ->
            val t = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
            val team = t?.let { Generator.flag(g.flagOf(it)) + " " + it.short } ?: "Serbest"
            PlayerRow(g, p, false, extra = "$team · ${money(GameRepo.askingPrice(p))}" + if (mode == 3 || sort == 1) " · P${p.potential}" else "")
        }
    }
}

/** Sözleşme uzatma kutusu. */
@Composable
private fun ContractBox(g: Game, p: Player, onMessage: (String) -> Unit) {
    var years by remember(p.id) { mutableIntStateOf(3) }
    var open by remember(p.id) { mutableStateOf(p.contractYears == 1) }
    if (!open) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("📝 Sözleşme Uzat") }
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp)) {
        val demand = com.supermenajer.game.game.Economy.renewalDemand(g, p)
        Text("Sözleşme yenileme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (demand == null) {
            Text("${p.name} kulüpte mutlu değil, şu an uzatmak istemiyor. Moralini yükseltmeyi dene.", color = Red, fontSize = 12.sp)
            return@Column
        }
        Text("İstediği maaş: ${money(demand)}/hafta (şu an ${money(p.wage)})", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { y -> Chip("$y yıl", years == y, Modifier.weight(1f)) { years = y } }
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val err = GameRepo.renewContract(p, years)
            onMessage(err ?: "${p.name} ile $years yıllık sözleşme imzalandı! ✍️")
        }, modifier = Modifier.fillMaxWidth()) { Text("İmzala", fontWeight = FontWeight.Bold) }
    }
}
