package com.supermenajer.game.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.supermenajer.game.game.StaffLogic
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun SquadTab(g: Game) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip("👥 Oyuncu", GameRepo.squadMode == 0) { GameRepo.squadMode = 0 }
            Chip("👔 Ekip", GameRepo.squadMode == 1) { GameRepo.squadMode = 1 }
            Chip("🏟️ Tesis", GameRepo.squadMode == 2) { GameRepo.squadMode = 2 }
            Chip("💼 Sponsor", GameRepo.squadMode == 3) { GameRepo.squadMode = 3 }
        }
        when (GameRepo.squadMode) {
            0 -> PlayersList(g)
            1 -> StaffView(g)
            2 -> FacilitiesView(g)
            else -> SponsorsView(g)
        }
    }
}

@Composable
private fun StaffView(g: Game) {
    var openRole by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf<String?>(null) }
    val us = g.userTeam
    LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(330.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = StaggeredGridItemSpan.FullLine) {
            val wages = com.supermenajer.game.game.Economy.squadWages(g, us.id)
            Text(
                "Teknik ekip maaşları: ${money(StaffLogic.wages(g))}/hafta · Toplam maaş yükü ${money(wages)} / ${money(us.wageBudget)}",
                color = TextDim, fontSize = 12.sp,
            )
            message?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
        for (role in 0..3) {
            item(key = "role$role") {
                val st = StaffLogic.of(g, role)
                SectionCard("${StaffLogic.roleIcons[role]} ${StaffLogic.roleNames[role]}") {
                    if (st == null) {
                        Text("Boş kadro! Kulübün gönüllüleri idare ediyor (kalite 6).", color = Red, fontSize = 13.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${Generator.flag(st.nation)} ${st.age} yaş · ${money(st.wage)}/hafta", color = TextDim, fontSize = 12.sp)
                            }
                            QualityBadge(st.quality)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(StaffLogic.roleInfo[role], color = TextDim, fontSize = 12.sp)
                    Text(effectText(g, role), color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { openRole = if (openRole == role) -1 else role; message = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (openRole == role) "Adayları gizle" else "Yeni ${StaffLogic.roleNames[role].lowercase()} ara")
                    }
                    if (openRole == role) {
                        val old = st?.wage ?: 0L
                        StaffLogic.market(g, role).forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                QualityBadge(c.quality)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val diff = c.wage - old
                                    Text("${Generator.flag(c.nation)} ${c.age} yaş · ${money(c.wage)}/hf (${if (diff >= 0) "+" else ""}${money(diff)})", color = TextDim, fontSize = 12.sp)
                                }
                                Button(onClick = { message = GameRepo.hireStaff(c); openRole = -1 }) { Text("İşe al") }
                            }
                        }
                        if (st != null) Text("Not: Mevcut çalışana 4 haftalık maaşı (${money(st.wage * 4)}) tazminat ödenir.", color = TextDim, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityBadge(q: Int) {
    val col = when {
        q >= 16 -> Gold
        q >= 12 -> Green
        q >= 8 -> Color(0xFF7CC4FA)
        else -> Color(0xFFB0B8C1)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(col), contentAlignment = Alignment.Center) {
            Text("$q", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Text("/20", color = TextDim, fontSize = 10.sp)
    }
}

private fun effectText(g: Game, role: Int): String = when (role) {
    StaffLogic.ASSISTANT -> "Konuşma etkisi: %${(StaffLogic.talkFactor(g) * 100).toInt()}"
    StaffLogic.COACH -> "Oyuncu gelişim hızı: %${(StaffLogic.coachFactor(g) * 100).toInt()}"
    StaffLogic.PHYSIO -> "Sakatlık riski: %${(StaffLogic.physioRisk(g) * 100).toInt()} · Hızlı iyileşme şansı: %${(StaffLogic.physioHeal(g) * 100).toInt()}"
    else -> "Tahmin aralığı: kendi ligin ±${StaffLogic.scoutUncertainty(g, true)}, diğer ligler ±${StaffLogic.scoutUncertainty(g, false)} · Haftalık öneri: ${StaffLogic.scoutReports(g)}"
}

@Composable
private fun PlayersList(g: Game) {
    val t = g.userTeam
    val squad = g.squad(t.id)
    var sort by remember { mutableIntStateOf(0) }
    val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope) -> androidx.compose.foundation.lazy.grid.GridItemSpan =
        { androidx.compose.foundation.lazy.grid.GridItemSpan(it.maxLineSpan) }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        androidx.compose.foundation.lazy.grid.GridCells.Adaptive(340.dp),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(span = full) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sırala:", color = TextDim, fontSize = 12.sp)
                Chip("Mevki", sort == 0) { sort = 0 }
                Chip("Güç", sort == 1) { sort = 1 }
                Chip("Yaş", sort == 2) { sort = 2 }
                Chip("Gol", sort == 3) { sort = 3 }
                Chip("Puan", sort == 4) { sort = 4 }
                Text(
                    "${squad.size} oyuncu · Maaşlar: ${money(squad.sumOf { it.wage })} / ${money(t.wageBudget)}" +
                        squad.count { it.contractYears == 1 }.let { if (it > 0) " · $it sözleşme bitiyor" else "" },
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (sort == 0) {
            for (pos in Pos.entries) {
                val list = squad.filter { it.pos == pos }.sortedByDescending { it.overall }
                item(key = "h$pos", span = full) {
                    Text(
                        pos.label.uppercase() + " (${list.size})", color = posColor(pos), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                gridItems(list, key = { it.id }) { p -> PlayerRow(g, p, p.id in t.lineup) }
            }
        } else {
            val list = when (sort) {
                1 -> squad.sortedByDescending { it.overall }
                2 -> squad.sortedBy { it.age }
                3 -> squad.sortedByDescending { it.goals * 100 + it.assists }
                else -> squad.sortedByDescending { it.avgRating }
            }
            gridItems(list, key = { it.id }) { p -> PlayerRow(g, p, p.id in t.lineup) }
        }
        val loans = g.players.filter { it.loanFrom == t.id }
        if (loans.isNotEmpty()) {
            item(key = "loans", span = full) {
                Text(
                    "KİRALIKTAKİLER (${loans.size})", color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                )
            }
            gridItems(loans, key = { "L" + it.id }) { p ->
                val club = g.teamOrNull(p.teamId)
                PlayerRow(g, p, false, extra = "${club?.short ?: "-"} · ${p.apps}M ${p.goals}G · ${p.startOvr}→${p.overall}")
            }
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
                if (p.teamId == g.userTeamId && p.wantsAway) Text("  😠", fontSize = 12.sp)
                if (!com.supermenajer.game.game.Scouting.known(g, p) && p.teamId != g.userTeamId) Text("  ?", fontSize = 12.sp, color = TextDim)
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
        PlayerOvrBadge(g, p)
    }
}

@Composable
fun PlayerDialog(g: Game, p: Player) {
    val own = p.teamId == g.userTeamId
    val loaned = p.loanFrom == g.userTeamId
    val known = com.supermenajer.game.game.Scouting.known(g, p)
    var offer by remember(p.id) { mutableStateOf<Offer?>(null) }
    var message by remember(p.id) { mutableStateOf<String?>(null) }
    val team = if (p.teamId >= 0) g.team(p.teamId) else null
    val actions: @Composable () -> Unit = {
        Column {
            message?.let {
                Text(it, color = Gold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (loaned) {
                Text("Kiralıkta: ${team?.name} (${team?.let { g.leagueName(it) }})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { GameRepo.recallLoan(p); GameRepo.selectedPlayer = null }, modifier = Modifier.fillMaxWidth()) { Text("Kiralıktan geri çağır") }
            } else if (own) {
                val t = g.userTeam
                if (p.wantsAway) Text("😠 Transfer talep etti. Forma ver, konuş ya da sat.", color = Red, fontSize = 13.sp)
                Text("BİREBİR KONUŞMA" + if (!GameRepo.canTalk(p)) " (bu hafta konuşuldu)" else "", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("👏 Övgü", "🗣 Eleştiri", "🤝 Forma sözü").forEachIndexed { i, n ->
                        Chip(n, false, Modifier.weight(1f)) { if (GameRepo.canTalk(p)) message = GameRepo.talkToPlayer(p, i) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (p.id in t.lineup) {
                    OutlinedButton(onClick = { GameRepo.removeFromLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text("Yedeğe Al") }
                } else if (p.available) {
                    Button(onClick = { GameRepo.addToLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text("İlk 11'e Al") }
                }
                Spacer(Modifier.height(6.dp))
                ContractBox(g, p) { message = it }
                if (p.age <= 23 && p.id !in t.lineup) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { GameRepo.loanOut(p)?.let { message = it } }, modifier = Modifier.fillMaxWidth()) { Text("🔁 Kiralık gönder (gelişsin)") }
                }
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
                if (!known) {
                    val requested = com.supermenajer.game.game.Scouting.requested(g, p)
                    OutlinedButton(
                        onClick = { message = GameRepo.scout(p) }, enabled = !requested, modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (requested) "🔍 Gözlemci izliyor, rapor yarın" else "🔍 Gözlemci gönder (${money(com.supermenajer.game.game.Scouting.COST)})") }
                    Spacer(Modifier.height(8.dp))
                }
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
    WideDialog(onDismissRequest = { GameRepo.selectedPlayer = null }, maxWidth = 760.dp) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 640.dp).clip(RoundedCornerShape(18.dp)).background(Surface1)
                .verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerOvrBadge(g, p, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PosChip(p.pos)
                        Text("  ${Generator.flag(p.nation)} ${p.age} yaş", color = TextDim, fontSize = 13.sp)
                    }
                    Text(team?.let { "${Generator.flag(g.flagOf(it))} ${it.name}" } ?: "Serbest oyuncu", color = TextDim, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f)) {
                    InfoLine("Potansiyel", com.supermenajer.game.game.Scouting.potText(g, p))
                    if (!known) InfoLine("Bilgi", "Gözlemci raporu yok", Gold)
                    else if (!own && !loaned && p.id in g.scouted) Text(com.supermenajer.game.game.Scouting.report(g, p), color = TextDim, fontSize = 12.sp)
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
                    Spacer(Modifier.height(10.dp))
                    Text("BU SEZON", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Stat("Maç", "${p.apps}")
                        Stat("Gol", "${p.goals}")
                        Stat("Asist", "${p.assists}")
                        Stat("Ort. Puan", if (p.apps > 0) "%.2f".format(p.avgRating) else "-")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("ÖZELLİKLER (1-20)", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val rg = { v: Int -> com.supermenajer.game.game.Scouting.range(g, p, v) }
                    if (p.pos == Pos.GK) Attr("Kalecilik", p.goalkeeping, rg(p.goalkeeping))
                    Attr("Hız", p.pace, rg(p.pace))
                    Attr("Şut", p.shooting, rg(p.shooting))
                    Attr("Pas", p.passing, rg(p.passing))
                    Attr("Teknik", p.dribbling, rg(p.dribbling))
                    Attr("Savunma", p.defending, rg(p.defending))
                    Attr("Fizik", p.physical, rg(p.physical))
                    Spacer(Modifier.height(10.dp))
                    actions()
                }
            }
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
/** CM tarzı 1-20 ölçek. Bilinmeyen oyuncuda aralık gösterilir. */
private fun to20(v: Int) = ((v / 5.0).roundToInt()).coerceIn(1, 20)

@Composable
private fun Attr(label: String, v: Int, range: IntRange = v..v) {
    val known = range.first == range.last
    val shown = if (known) to20(v) else to20((range.first + range.last) / 2)
    val col = when {
        shown >= 16 -> Green
        shown >= 13 -> Color(0xFF9BE9A8)
        shown >= 10 -> Gold
        else -> Color(0xFFB0B8C1)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(80.dp))
        Bar(shown / 20f, if (known) col else col.copy(alpha = 0.45f), Modifier.weight(1f), 8.dp)
        Text(
            if (known) "$shown" else "${to20(range.first)}-${to20(range.last)}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (known) col else TextDim,
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
        )
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
    var mode by remember { mutableIntStateOf(0) } // 0 tümü, 1 serbest, 2 bütçeme uygun, 3 genç, 4 gözlem listesi
    var country by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) } // 0 güç, 1 potansiyel, 2 ucuz, 3 genç
    val budget = g.userTeam.budget
    val version = GameRepo.version
    val list = remember(version, posFilter, mode, country, query, sort) {
        val q = query.trim().lowercase()
        g.players.asSequence()
            .filter { it.teamId != g.userTeamId && it.loanFrom != g.userTeamId }
            .filter { posFilter == null || it.pos == posFilter }
            .filter { country == null || (it.teamId >= 0 && g.teamOrNull(it.teamId)?.let { t -> g.countryOf(t) } == country) }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) }
            .filter {
                when (mode) {
                    1 -> it.teamId < 0
                    2 -> GameRepo.askingPrice(it) <= budget && com.supermenajer.game.game.Economy.fitsWages(g, g.userTeam, it.demandWage)
                    3 -> it.age <= 21
                    4 -> it.id in g.shortlist || it.id in g.scouted
                    else -> true
                }
            }
            .sortedWith(
                when (sort) {
                    // Gizli bilgiyi sızdırmamak için tanımadığımız oyuncularda tahmini güç kullanılır.
                    1 -> compareByDescending<Player> { if (com.supermenajer.game.game.Scouting.known(g, it)) it.potential else 0 }.thenByDescending { com.supermenajer.game.game.Scouting.estimate(g, it) }
                    2 -> compareBy<Player> { GameRepo.askingPrice(it) }.thenByDescending { com.supermenajer.game.game.Scouting.estimate(g, it) }
                    3 -> compareBy<Player> { it.age }.thenByDescending { com.supermenajer.game.game.Scouting.estimate(g, it) }
                    else -> compareByDescending<Player> { com.supermenajer.game.game.Scouting.estimate(g, it) }
                }
            )
            .take(150).toList()
    }
    Row(Modifier.fillMaxSize()) {
        // Sol panel: pencere durumu, gözlemci, arama ve filtreler
        Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(10.dp)) {
            val open = Season.transferWindowOpen(g)
            Text(
                Season.windowText(g), color = if (open) Green else Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface1).padding(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            ScoutCard(g)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(30) }, singleLine = true,
                placeholder = { Text("🔍 Oyuncu ara") }, modifier = Modifier.fillMaxWidth(),
            )
            FilterTitle("Liste")
            FlowChips {
                Chip("Tümü", mode == 0) { mode = 0 }
                Chip("Bütçeme uygun", mode == 2) { mode = 2 }
                Chip("Serbest", mode == 1) { mode = 1 }
                Chip("Genç (≤21)", mode == 3) { mode = 3 }
                Chip("📋 Gözlem (${g.shortlist.size})", mode == 4) { mode = 4 }
            }
            FilterTitle("Ülke")
            FlowChips {
                Chip("🌍 Tümü", country == null) { country = null }
                Season.activeCountries(g).forEach { c -> Chip(Generator.flag(com.supermenajer.game.model.Leagues.flagOf(c)), country == c) { country = c } }
            }
            FilterTitle("Mevki")
            FlowChips {
                Chip("Hepsi", posFilter == null) { posFilter = null }
                Pos.entries.forEach { p -> Chip(p.label, posFilter == p) { posFilter = p } }
            }
            FilterTitle("Sırala")
            FlowChips {
                listOf("Güç", "Potansiyel", "En ucuz", "En genç").forEachIndexed { i, n -> Chip(n, sort == i) { sort = i } }
            }
        }
        // Sağ: oyuncular
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            androidx.compose.foundation.lazy.grid.GridCells.Adaptive(340.dp),
            Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            if (list.isEmpty()) item { Text("Uygun oyuncu bulunamadı.", color = TextDim, modifier = Modifier.padding(16.dp)) }
            items(list.size, key = { list[it].id }) { i ->
                val p = list[i]
                val t = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
                val team = t?.let { Generator.flag(g.flagOf(it)) + " " + it.short } ?: "Serbest"
                val pot = if (com.supermenajer.game.game.Scouting.known(g, p) && (mode >= 3 || sort == 1)) " · P${p.potential}" else ""
                PlayerRow(g, p, false, extra = "$team · ${money(GameRepo.askingPrice(p))}$pot")
            }
        }
    }
}

@Composable
private fun FilterTitle(text: String) {
    Text(text.uppercase(), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
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

/** Gözlemci ayarları: hangi ülkede, neyi arasın. */
@Composable
private fun ScoutCard(g: Game) {
    var open by remember { mutableStateOf(false) }
    val country = g.scoutCountry.ifEmpty { g.userCountry }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface1).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("🔍 Gözlemci: ${Generator.flag(com.supermenajer.game.model.Leagues.flagOf(country))} $country · ${com.supermenajer.game.game.Scouting.focusNames[g.scoutFocus]}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (open) "▲" else "▼", color = TextDim)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text("Gözlemcin her hafta seçtiğin ülkeden bir oyuncu önerir. Önerilenler Gözlem listesine eklenir ve tüm bilgileri açılır.", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Season.activeCountries(g).forEach { c ->
                    Chip(Generator.flag(com.supermenajer.game.model.Leagues.flagOf(c)) + " " + c, c == country) { GameRepo.setScout(c, g.scoutFocus) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.supermenajer.game.game.Scouting.focusNames.forEachIndexed { i, n -> Chip(n, g.scoutFocus == i, Modifier.weight(1f)) { GameRepo.setScout(country, i) } }
            }
        }
    }
}
