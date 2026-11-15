package com.supermenajer.game.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        val a2 = g.squad(g.userTeamId).count { it.reserve }
        FmTabs(listOf(L("Oyuncular", "Players"), L("Takım Dinamikleri · uyum ${com.supermenajer.game.game.Dynamics.pct(g.cohesion)}", "Team Dynamics · chemistry ${com.supermenajer.game.game.Dynamics.pct(g.cohesion)}"), L("A2 Takımı ($a2)", "B Team ($a2)"), L("🌱 Gelişim", "🌱 Development")), GameRepo.squadMode) { GameRepo.squadMode = it }
        when (GameRepo.squadMode) {
            1 -> DynamicsView(g)
            2 -> ReservesView(g)
            3 -> YouthView(g)
            else -> PlayersList(g)
        }
    }
}

@Composable
fun StaffView(g: Game) {
    var openRole by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf<String?>(null) }
    val us = g.userTeam
    LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(330.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = StaggeredGridItemSpan.FullLine) {
            val wages = com.supermenajer.game.game.Economy.squadWages(g, us.id)
            Text(
                L("Teknik ekip maaşları: ${money(StaffLogic.wages(g))}/hafta · Toplam maaş yükü ${money(wages)} / ${money(us.wageBudget)}", "Staff wages: ${money(StaffLogic.wages(g))}/week · Total wage bill ${money(wages)} / ${money(us.wageBudget)}"),
                color = TextDim, fontSize = 12.sp,
            )
            message?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
        for (role in 0..3) {
            item(key = "role$role") {
                val st = StaffLogic.of(g, role)
                SectionCard("${StaffLogic.roleIcons[role]} ${StaffLogic.roleNames[role]}") {
                    if (st == null) {
                        Text(L("Boş kadro! Kulübün gönüllüleri idare ediyor (kalite 6).", "Vacant post! Club volunteers are covering (quality 6)."), color = Red, fontSize = 13.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(L("${Generator.flag(st.nation)} ${st.age} yaş · ${money(st.wage)}/hafta", "${Generator.flag(st.nation)} ${st.age} yrs · ${money(st.wage)}/week"), color = TextDim, fontSize = 12.sp)
                            }
                            QualityBadge(st.quality)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(StaffLogic.roleInfo[role], color = TextDim, fontSize = 12.sp)
                    Text(effectText(g, role), color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { openRole = if (openRole == role) -1 else role; message = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (openRole == role) L("Adayları gizle", "Hide candidates") else L("Yeni ${StaffLogic.roleNames[role].lowercase()} ara", "Find a new ${StaffLogic.roleNames[role].lowercase()}"))
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
                                    Text(L("${Generator.flag(c.nation)} ${c.age} yaş · ${money(c.wage)}/hf (${if (diff >= 0) "+" else ""}${money(diff)})", "${Generator.flag(c.nation)} ${c.age} yrs · ${money(c.wage)}/wk (${if (diff >= 0) "+" else ""}${money(diff)})"), color = TextDim, fontSize = 12.sp)
                                }
                                Button(onClick = { message = GameRepo.hireStaff(c); openRole = -1 }) { Text(L("İşe al", "Hire")) }
                            }
                        }
                        if (st != null) Text(L("Not: Mevcut çalışana 4 haftalık maaşı (${money(st.wage * 4)}) tazminat ödenir.", "Note: the current employee receives 4 weeks' wages (${money(st.wage * 4)}) as compensation."), color = TextDim, fontSize = 11.sp)
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
    StaffLogic.ASSISTANT -> L("Konuşma etkisi: %${(StaffLogic.talkFactor(g) * 100).toInt()}", "Talk effect: %${(StaffLogic.talkFactor(g) * 100).toInt()}")
    StaffLogic.COACH -> L("Oyuncu gelişim hızı: %${(StaffLogic.coachFactor(g) * 100).toInt()}", "Player development speed: %${(StaffLogic.coachFactor(g) * 100).toInt()}")
    StaffLogic.PHYSIO -> L("Sakatlık riski: %${(StaffLogic.physioRisk(g) * 100).toInt()} · Hızlı iyileşme şansı: %${(StaffLogic.physioHeal(g) * 100).toInt()}", "Injury risk: %${(StaffLogic.physioRisk(g) * 100).toInt()} · Quick recovery chance: %${(StaffLogic.physioHeal(g) * 100).toInt()}")
    else -> L("Tahmin aralığı: kendi ligin ±${StaffLogic.scoutUncertainty(g, true)}, diğer ligler ±${StaffLogic.scoutUncertainty(g, false)} · Haftalık öneri: ${StaffLogic.scoutReports(g)}", "Estimate range: your league ±${StaffLogic.scoutUncertainty(g, true)}, other leagues ±${StaffLogic.scoutUncertainty(g, false)} · Weekly suggestions: ${StaffLogic.scoutReports(g)}")
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
                Text(L("Sırala:", "Sort:"), color = TextDim, fontSize = 12.sp)
                Chip(L("Mevki", "Position"), sort == 0) { sort = 0 }
                Chip(L("Güç", "Overall"), sort == 1) { sort = 1 }
                Chip(L("Yaş", "Age"), sort == 2) { sort = 2 }
                Chip(L("Gol", "Goals"), sort == 3) { sort = 3 }
                Chip(L("Puan", "Points"), sort == 4) { sort = 4 }
                Text(
                    L("${squad.size} oyuncu · Maaşlar: ${money(squad.sumOf { it.wage })} / ${money(t.wageBudget)}", "${squad.size} players · Wages: ${money(squad.sumOf { it.wage })} / ${money(t.wageBudget)}") +
                        squad.count { it.contractYears == 1 }.let { if (it > 0) L(" · $it sözleşme bitiyor", " · $it contracts ending") else "" },
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (sort == 0) {
            for (pos in Pos.entries) {
                val list = squad.filter { it.pos == pos }.sortedWith(compareBy<Player> { com.supermenajer.game.game.Positions.det(it).ordinal }.thenByDescending { it.overall })
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
                    L("KİRALIKTAKİLER (${loans.size})", "ON LOAN (${loans.size})"), color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                )
            }
            gridItems(loans, key = { "L" + it.id }) { p ->
                val club = g.teamOrNull(p.teamId)
                PlayerRow(g, p, false, extra = L("${club?.short ?: "-"} · ${p.apps}M ${p.goals}G · ${p.startOvr}→${p.overall}", "${club?.short ?: "-"} · ${p.apps}A ${p.goals}G · ${p.startOvr}→${p.overall}"))
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
        Spacer(Modifier.width(6.dp))
        val team = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
        Box {
            PlayerFace(p, 40.dp, team?.let { Color(it.color1) } ?: Color(0xFF455A64), team?.let { Color(it.color2) } ?: Color.White)
            Box(Modifier.align(Alignment.BottomEnd)) { DetChip(p, small = true) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.number > 0 && p.teamId == g.userTeamId) Text("${p.number} ", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                Text(L("${Generator.flag(p.nation)} ${p.age} yaş", "${Generator.flag(p.nation)} ${p.age} yrs"), color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Bar((p.fitness / 100).toFloat(), fitColor(p.fitness), Modifier.width(44.dp), 5.dp)
                Spacer(Modifier.width(8.dp))
                Text(extra ?: (if (p.apps > 0) L("${p.apps}M ${p.goals}G ${p.assists}A · ${"%.1f".format(p.avgRating)}", "${p.apps}A ${p.goals}G ${p.assists}As · ${"%.1f".format(p.avgRating)}") else ""), color = TextDim, fontSize = 12.sp, maxLines = 1)
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
                Text(L("Kiralıkta: ${team?.name} (${team?.let { g.leagueName(it) }})", "On loan: ${team?.name} (${team?.let { g.leagueName(it) }})"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { GameRepo.recallLoan(p); GameRepo.selectedPlayer = null }, modifier = Modifier.fillMaxWidth()) { Text(L("Kiralıktan geri çağır", "Recall from loan")) }
            } else if (own) {
                val t = g.userTeam
                if (p.wantsAway) Text(L("😠 Transfer talep etti. Forma ver, konuş ya da sat.", "😠 Has asked for a transfer. Play him, talk to him or sell him."), color = Red, fontSize = 13.sp)
                Text(L("BİREBİR KONUŞMA", "ONE-TO-ONE TALK") + if (!GameRepo.canTalk(p)) L(" (bu hafta konuşuldu)", " (talked this week)") else "", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(L("👏 Övgü", "👏 Praise"), L("🗣 Eleştiri", "🗣 Criticise"), L("🤝 Forma sözü", "🤝 Promise playing time")).forEachIndexed { i, n ->
                        Chip(n, false, Modifier.weight(1f)) { if (GameRepo.canTalk(p)) message = GameRepo.talkToPlayer(p, i) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (p.id in t.lineup) {
                    OutlinedButton(onClick = { GameRepo.removeFromLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text(L("Yedeğe Al", "Drop to bench")) }
                } else if (p.available) {
                    Button(onClick = { GameRepo.addToLineup(p) }, modifier = Modifier.fillMaxWidth()) { Text(L("İlk 11'e Al", "Start")) }
                }
                if (p.loanFrom < 0) {
                    OutlinedButton(
                        onClick = { message = GameRepo.setReserve(p, !p.reserve) ?: if (!p.reserve) L("A takıma alındı.", "Moved to the first team.") else L("A2 takımına gönderildi.", "Sent to the B team.") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (p.reserve) L("⬆ A takıma al", "⬆ Move to first team") else L("⬇ A2 takımına gönder", "⬇ Send to B team")) }
                }
                Spacer(Modifier.height(6.dp))
                if (p.loanFrom < 0) ContractBox(g, p) { message = it }
                else Text(L("Kiralık oyuncu · ${g.teamOrNull(p.loanFrom)?.name ?: ""} kulübünden", "Loan player · from ${g.teamOrNull(p.loanFrom)?.name ?: ""}"), color = Blue, fontSize = 12.sp)
                if (p.age <= 23 && p.loanFrom < 0 && p.id !in t.lineup) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { GameRepo.loanOut(p)?.let { message = it } }, modifier = Modifier.fillMaxWidth()) { Text(L("🔁 Kiralık gönder (gelişsin)", "🔁 Send on loan (to develop)")) }
                }
                Spacer(Modifier.height(6.dp))
                if (p.loanFrom < 0) {
                    OutlinedButton(onClick = { message = GameRepo.toggleListed(p) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (p.listed) L("🏷️ Satış listesinden çıkar", "🏷️ Remove from transfer list") else L("🏷️ Satış listesine koy", "🏷️ Put on transfer list"))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                val o = offer
                if (p.loanFrom >= 0) {
                    // Kiralık oyuncu satılamaz
                } else if (o == null) {
                    OutlinedButton(onClick = {
                        offer = GameRepo.makeOffer(p)
                        if (offer == null) message = if (!Season.transferWindowOpen(g)) L("Transfer dönemi kapalı.", "The transfer window is closed.") else L("Şu an bu oyuncu için teklif yapan kulüp yok (veya kadro çok dar).", "No club is bidding for this player right now (or the squad is too thin).")
                    }, modifier = Modifier.fillMaxWidth()) { Text(L("📣 Kulüplere hemen teklif ettir", "📣 Offer him to clubs now")) }
                } else {
                    Text(L("${o.buyer.name} teklif yaptı: ${money(o.price)}", "${o.buyer.name} bid: ${money(o.price)}"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { GameRepo.acceptOffer(o)?.let { message = it; offer = null } }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        ) { Text(L("Kabul Et", "Accept")) }
                        OutlinedButton(onClick = { offer = null }, modifier = Modifier.weight(1f)) { Text(L("Reddet", "Reject")) }
                    }
                }
            } else {
                if (!known) {
                    val requested = com.supermenajer.game.game.Scouting.requested(g, p)
                    OutlinedButton(
                        onClick = { message = GameRepo.scout(p) }, enabled = !requested, modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (requested) L("🔍 Gözlemci izliyor, rapor yarın", "🔍 Scout is watching, report tomorrow") else L("🔍 Gözlemci gönder (${money(com.supermenajer.game.game.Scouting.COST)})", "🔍 Send scout (${money(com.supermenajer.game.game.Scouting.COST)})")) }
                    Spacer(Modifier.height(8.dp))
                }
                val block = GameRepo.transferBlock(p)
                if (block != null) {
                    Text(block, color = Red, fontSize = 13.sp)
                } else {
                    Text(
                        if (p.teamId < 0) L("Serbest oyuncu · bonservis yok", "Free agent · no fee") else L("Tahmini bonservis: ~${money(GameRepo.askingPrice(p))} · ${team?.name}", "Estimated fee: ~${money(GameRepo.askingPrice(p))} · ${team?.name}"),
                        color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(L("Maaş beklentisi: ~${money(p.demandWage)}/hafta", "Wage expectation: ~${money(p.demandWage)}/week"), color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { GameRepo.startTalks(p)?.let { message = it } },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) { Text(L("🤝 Transfer görüşmesi başlat", "🤝 Start transfer talks"), fontWeight = FontWeight.Bold) }
                }
                if (p.teamId >= 0 && p.releaseClause > 0) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { message = GameRepo.payReleaseClause(p) }, modifier = Modifier.fillMaxWidth()) {
                        Text(L("💥 Serbest kalma bedelini öde: ${money(p.releaseClause)}", "💥 Pay the release clause: ${money(p.releaseClause)}"))
                    }
                }
                if (p.teamId >= 0 && p.contractYears <= 1) {
                    Spacer(Modifier.height(6.dp))
                    val pc = com.supermenajer.game.game.Contracts
                    OutlinedButton(onClick = { message = GameRepo.signPreContract(p) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (p.preContract == g.userTeamId) L("✍️ Ön sözleşme imzalandı · sezon sonu gelecek", "✍️ Pre-contract signed · joins at season end")
                            else L("✍️ Ön sözleşme teklif et (imza parası ${money(pc.signingBonus(p))})", "✍️ Offer a pre-contract (signing bonus ${money(pc.signingBonus(p))})"),
                        )
                    }
                    if (p.preContract != g.userTeamId) Text(L("Sözleşmesi bu sezon bitiyor: 17. haftadan sonra bedelsiz gelmesi için ön sözleşme yapabilirsin.", "His contract ends this season: after week 17 you can agree a pre-contract to sign him for free."), color = TextDim, fontSize = 11.sp)
                }
                Text(L("Bütçen: ${money(g.userTeam.budget)}", "Your budget: ${money(g.userTeam.budget)}"), color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    var ptab by remember(p.id) { mutableIntStateOf(0) }
    // Sekme anahtarları dilden bağımsız (Türkçe), görünen adlar çevrilir.
    val tabKeys = buildList {
        add("Nitelikler"); add("Bilgi"); add("Kariyer")
        if (own && p.loanFrom < 0) add("Antrenman")
        add(if (own || loaned) "İşlemler" else "Transfer")
    }
    val tabs = tabKeys.map {
        when (it) {
            "Nitelikler" -> L("Nitelikler", "Attributes"); "Bilgi" -> L("Bilgi", "Info"); "Kariyer" -> L("Kariyer", "Career")
            "Antrenman" -> L("Antrenman", "Training"); "İşlemler" -> L("İşlemler", "Actions"); else -> L("Transfer", "Transfers")
        }
    }
    WideDialog(onDismissRequest = { GameRepo.selectedPlayer = null }, maxWidth = 980.dp) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.97f).clip(RoundedCornerShape(16.dp)).background(Surface1)
                .border(1.dp, Line, RoundedCornerShape(16.dp)),
        ) {
            val tm = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
            val r = com.supermenajer.game.game.Scouting.range(g, p, p.overall)
            val vertical = portrait()
            // Başlık şeridi: kart, temel bilgiler, mevki haritası
            Row(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Purple.copy(alpha = 0.35f), Surface1)),
                ).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerCard(
                    p, (r.first + r.last) / 2, com.supermenajer.game.game.Positions.det(p).code, 66.dp,
                    tm?.let { Color(it.color1) } ?: Color(0xFF455A64), tm?.let { Color(it.color2) } ?: Color.White,
                    known = r.first == r.last,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1.3f)) {
                    Text(
                        (if (p.number > 0 && own) "${p.number}. " else "") + p.name,
                        fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DetChip(p)
                        Text(L("  ${Generator.flag(p.nation)} ${p.age} yaş · ${com.supermenajer.game.game.Positions.feet[p.foot.coerceIn(0, 2)]}", "  ${Generator.flag(p.nation)} ${p.age} yrs · ${com.supermenajer.game.game.Positions.feet[p.foot.coerceIn(0, 2)]}"), color = TextDim, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(
                        L("Özellik: ${com.supermenajer.game.game.Positions.natRoleName(p)}", "Trait: ${com.supermenajer.game.game.Positions.natRoleName(p)}") +
                            L(" · Mevkiler: ${com.supermenajer.game.game.Positions.codes(p)}", " · Positions: ${com.supermenajer.game.game.Positions.codes(p)}"),
                        color = Gold, fontSize = 12.sp, maxLines = 1,
                    )
                    Text(team?.let { "${Generator.flag(g.flagOf(it))} ${it.name} · ${g.leagueName(it)}" } ?: L("Serbest oyuncu", "Free agent"), color = TextDim, fontSize = 12.sp, maxLines = 1)
                    if (vertical) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        HeaderStat(money(p.value), L("Değer", "Value"))
                        if (p.teamId >= 0) HeaderStat(money(p.wage), L("Maaş/hf", "Wage/wk"))
                        HeaderStat(if (p.teamId < 0) "–" else L("${p.contractYears} sezon", "${p.contractYears} seas."), L("Sözleşme", "Contract"), if (p.contractYears == 1 && p.teamId >= 0) Red else TextMain)
                    }
                    if (own && p.promiseUntil > 0) {
                        val played = p.apps - p.promiseBaseApps
                        Text(
                            L("🤝 Forma sözü: $played/${p.promiseNeed} maç · ${(p.promiseUntil - g.week).coerceAtLeast(0)} hafta kaldı", "🤝 Playing-time promise: $played/${p.promiseNeed} matches · ${(p.promiseUntil - g.week).coerceAtLeast(0)} weeks left"),
                            color = if (played >= p.promiseNeed) Green else Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        )
                    }
                }
                if (!vertical) Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HeaderStat(money(p.value), L("Değer", "Value"))
                    if (p.teamId >= 0) HeaderStat(money(p.wage), L("Maaş/hafta", "Wage/week"))
                    HeaderStat(if (p.teamId < 0) "–" else L("${p.contractYears} sezon", "${p.contractYears} seasons"), L("Sözleşme", "Contract"), if (p.contractYears == 1 && p.teamId >= 0) Red else TextMain)
                    HeaderStat(if (known) moraleText(p.morale).substringBefore(' ') else "?", L("Moral", "Morale"))
                }
                Spacer(Modifier.width(8.dp))
                if (!vertical) PositionMap(p, Modifier.size(132.dp, 84.dp).clip(RoundedCornerShape(6.dp)))
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "✕", fontSize = 20.sp, color = TextDim,
                        modifier = Modifier.clip(CircleShape).clickable { GameRepo.selectedPlayer = null }.padding(8.dp),
                    )
                    if (!own) Text(
                        if (p.id in g.watch) "⭐" else "☆", fontSize = 20.sp, color = Gold,
                        modifier = Modifier.clip(CircleShape).clickable { GameRepo.toggleWatch(p) }.padding(6.dp),
                    )
                    Text(
                        "⚖", fontSize = 20.sp, color = Teal,
                        modifier = Modifier.clip(CircleShape).clickable {
                            GameRepo.compareWith(p)
                            if (GameRepo.compare == null) message = L("Karşılaştırma için sabitlendi. Başka bir oyuncunun profilinde ⚖'ye bas.", "Pinned for comparison. Tap ⚖ on another player's profile.")
                            else GameRepo.selectedPlayer = null
                        }.padding(6.dp),
                    )
                }
            }
            // Sekmeler
            Row(Modifier.fillMaxWidth().background(Bg.copy(alpha = 0.5f)).padding(horizontal = 10.dp)) {
                tabs.forEachIndexed { i, n ->
                    val sel = ptab == i
                    Column(
                        Modifier.weight(1f).clickable { ptab = i }.padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(n, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) TextMain else TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.6f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (sel) Green else Color.Transparent))
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                message?.takeIf { ptab == 0 }?.let { Text(it, color = Gold, fontSize = 13.sp); Spacer(Modifier.height(6.dp)) }
                when (tabKeys[ptab.coerceIn(0, tabKeys.lastIndex)]) {
                    "Nitelikler" -> {
                        if (!known) Text(L("🔍 Gözlemci raporu yok: değerler tahmini. Transfer sekmesinden gözlemci gönderebilirsin.", "🔍 No scout report: values are estimates. You can send a scout from the transfer tab."), color = Gold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        AttributeTable(
                            p, known,
                            extra = listOf(
                                "Kondisyon" to "${p.fitness.toInt()}%",
                                L("Moral", "Morale") to if (known) moraleText(p.morale) else "?",
                                L("Ort. Puan", "Avg. Rating") to if (p.apps > 0) "%.2f".format(p.avgRating) else "–",
                            ),
                        )
                    }
                    "Bilgi" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f)) {
                            InfoLine(L("Potansiyel", "Potential"), com.supermenajer.game.game.Scouting.potText(g, p))
                            if (!known) InfoLine(L("Bilgi", "Info"), L("Gözlemci raporu yok", "No scout report"), Gold)
                            else if (!own && !loaned && p.id in g.scouted) Text(com.supermenajer.game.game.Scouting.report(g, p), color = TextDim, fontSize = 12.sp)
                            InfoLine(L("Mevki", "Position"), com.supermenajer.game.game.Positions.det(p).label)
                            if (!own) InfoLine(L("Maaş talebi", "Wage demand"), L("${money(p.demandWage)}/hafta", "${money(p.demandWage)}/week"))
                            if (p.releaseClause > 0) InfoLine(L("Serbest kalma bedeli", "Release clause"), money(p.releaseClause))
                            if (p.injuredWeeks > 0 && p.injuryType.isNotEmpty()) InfoLine(L("Sakatlık türü", "Injury type"), p.injuryType, Red)
                            if (p.fragileWeeks > 0) InfoLine(L("Sağlık uyarısı", "Medical warning"), L("Yeniden sakatlanma riski (${p.fragileWeeks} hafta)", "Re-injury risk (${p.fragileWeeks} weeks)"), Gold)
                            InfoLine(L("Kişilik", "Personality"), if (known) com.supermenajer.game.game.Traits.name(p) else L("? (gözlemci raporu gerekli)", "? (scout report needed)"))
                            if (known) Text(com.supermenajer.game.game.Traits.info[com.supermenajer.game.game.Traits.of(p)], color = TextDim, fontSize = 11.sp)
                            if (p.squadRole >= 0 && p.teamId == g.userTeamId) InfoLine(L("Söz verilen rol", "Promised role"), com.supermenajer.game.game.TransferTalks.roleNames[p.squadRole])
                            if (p.injuredWeeks > 0) InfoLine(L("Sakatlık", "Injury"), L("${p.injuredWeeks} hafta", "${p.injuredWeeks} weeks"), Red)
                            if (p.suspended > 0) InfoLine(L("Ceza", "Suspension"), L("${p.suspended} maç", "${p.suspended} matches"), Red)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(L("BU SEZON", "THIS SEASON"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                Stat(L("Maç", "Apps"), "${p.apps}")
                                Stat(L("Gol", "Goals"), "${p.goals}")
                                Stat(L("Asist", "Assists"), "${p.assists}")
                                Stat(L("Ort. Puan", "Avg. Rating"), if (p.apps > 0) "%.2f".format(p.avgRating) else "-")
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(L("ANA ÖZELLİKLER (1-20)", "KEY ATTRIBUTES (1-20)"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            val rg = { v: Int -> com.supermenajer.game.game.Scouting.range(g, p, v) }
                            if (p.pos == Pos.GK) Attr(L("Kalecilik", "Goalkeeping"), p.goalkeeping, rg(p.goalkeeping))
                            Attr(L("Hız", "Pace"), p.pace, rg(p.pace))
                            Attr(L("Şut", "Shooting"), p.shooting, rg(p.shooting))
                            Attr(L("Pas", "Passing"), p.passing, rg(p.passing))
                            Attr(L("Teknik", "Technique"), p.dribbling, rg(p.dribbling))
                            Attr(L("Savunma", "Defending"), p.defending, rg(p.defending))
                            Attr(L("Fizik", "Physical"), p.physical, rg(p.physical))
                        }
                    }
                    "Kariyer" -> CareerHistory(g, p)
                    "Antrenman" -> TrainingBox(p)
                    else -> Box(Modifier.fillMaxWidth(0.6f)) { actions() }
                }
            }
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
internal fun Stat(label: String, v: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, fontSize = 11.sp, color = TextDim)
    }
}

@Composable
fun TransferTab(g: Game) {
    var detFilter by remember { mutableStateOf<com.supermenajer.game.game.Positions.Det?>(null) }
    var mode by remember { mutableIntStateOf(0) } // 0 tümü, 1 serbest, 2 bütçeme uygun, 3 genç, 4 kısa liste, 5 gözlemci önerileri
    var country by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) } // 0 güç, 1 potansiyel, 2 ucuz, 3 genç
    var ages by remember { mutableStateOf(15f..40f) }
    var minOvr by remember { mutableStateOf(40f) }
    var maxPriceStep by remember { mutableIntStateOf(priceSteps.lastIndex) }
    val budget = g.userTeam.budget
    // Gün gün ilerlerken liste her gün yeniden hesaplanmasın (6.500 oyuncu)
    val tick = if (GameRepo.advancing) -1 else GameRepo.version
    val maxPrice = priceSteps[maxPriceStep]
    val list = remember(tick, detFilter, mode, country, query, sort, ages, minOvr, maxPriceStep) {
        val q = query.trim().lowercase()
        val wagesNow = com.supermenajer.game.game.Economy.squadWages(g, g.userTeamId)
        val est = HashMap<Int, Int>()
        fun e(p: Player) = est.getOrPut(p.id) { com.supermenajer.game.game.Scouting.estimate(g, p) }
        g.players.asSequence()
            .filter { it.teamId != g.userTeamId && it.loanFrom != g.userTeamId }
            .filter { detFilter == null || com.supermenajer.game.game.Positions.canPlay(it, detFilter!!) }
            .filter { it.age.toFloat() in ages }
            .filter { country == null || (it.teamId >= 0 && g.teamOrNull(it.teamId)?.let { t -> g.countryOf(t) } == country) }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) }
            .filter { e(it) >= minOvr }
            .filter {
                when (mode) {
                    1 -> it.teamId < 0
                    2 -> GameRepo.askingPrice(it) <= budget && wagesNow + it.demandWage <= g.userTeam.wageBudget
                    3 -> it.age <= 21
                    4 -> it.id in g.watch
                    5 -> it.id in g.shortlist || it.id in g.scouted
                    else -> true
                }
            }
            .map { it to GameRepo.askingPrice(it) }
            .filter { (_, price) -> maxPriceStep == priceSteps.lastIndex || price <= maxPrice }
            .sortedWith(
                when (sort) {
                    // Gizli bilgiyi sızdırmamak için tanımadığımız oyuncularda tahmini güç kullanılır.
                    1 -> compareByDescending<Pair<Player, Long>> { if (com.supermenajer.game.game.Scouting.known(g, it.first)) it.first.potential else 0 }.thenByDescending { e(it.first) }
                    2 -> compareBy<Pair<Player, Long>> { it.second }.thenByDescending { e(it.first) }
                    3 -> compareBy<Pair<Player, Long>> { it.first.age }.thenByDescending { e(it.first) }
                    else -> compareByDescending<Pair<Player, Long>> { e(it.first) }
                },
            )
            .take(150).toList()
    }
    var showFilters by remember { mutableStateOf(false) }
    val vertical = portrait()
    SplitPane(
        Modifier.width(310.dp).fillMaxHeight(),
        Modifier.fillMaxWidth().heightIn(max = if (showFilters) 430.dp else 132.dp),
        first = { panelMod ->
        // Sol panel (dikeyde üstte, açılır): pencere durumu, gözlemci, arama ve filtreler
        Column(panelMod.verticalScroll(rememberScrollState()).padding(10.dp)) {
            val open = Season.transferWindowOpen(g)
            Text(
                Season.windowText(g), color = if (open) Green else Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface1).padding(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(30) }, singleLine = true,
                placeholder = { Text(L("🔍 Oyuncu ara", "🔍 Search player")) }, modifier = Modifier.fillMaxWidth(),
            )
            if (vertical) Text(
                if (showFilters) L("▴ Filtreleri gizle", "▴ Hide filters") else L("▾ Filtreler ve sıralama", "▾ Filters and sorting"),
                color = Teal, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showFilters = !showFilters }.padding(vertical = 8.dp),
            )
            FilterTitle(L("Liste", "List"))
            FlowChips {
                Chip(L("Tümü", "All"), mode == 0) { mode = 0 }
                Chip(L("Bütçeme uygun", "Affordable"), mode == 2) { mode = 2 }
                Chip(L("Serbest", "Free agent"), mode == 1) { mode = 1 }
                Chip(L("Genç (≤21)", "Young (≤21)"), mode == 3) { mode = 3 }
                Chip(L("⭐ Kısa liste (${g.watch.size})", "⭐ Shortlist (${g.watch.size})"), mode == 4) { mode = 4 }
                Chip(L("🔭 Gözlemci", "🔭 Scout"), mode == 5) { mode = 5 }
            }
            FilterTitle(L("Mevki", "Position"))
            FlowChips {
                Chip(L("Hepsi", "All"), detFilter == null) { detFilter = null }
                com.supermenajer.game.game.Positions.Det.entries.forEach { d -> Chip(d.code, detFilter == d) { detFilter = d } }
            }
            FilterTitle(L("Yaş: ${ages.start.toInt()}–${ages.endInclusive.toInt()}", "Age: ${ages.start.toInt()}–${ages.endInclusive.toInt()}"))
            androidx.compose.material3.RangeSlider(
                value = ages, onValueChange = { ages = it.start.roundToInt().toFloat()..it.endInclusive.roundToInt().toFloat() },
                valueRange = 15f..40f, steps = 24,
            )
            FilterTitle(L("En az güç (tahmini): ${minOvr.toInt()}", "Min. overall (estimated): ${minOvr.toInt()}"))
            androidx.compose.material3.Slider(value = minOvr, onValueChange = { minOvr = it.roundToInt().toFloat() }, valueRange = 40f..90f, steps = 49)
            FilterTitle(L("En fazla bonservis: ", "Max. fee: ") + if (maxPriceStep == priceSteps.lastIndex) L("sınırsız", "unlimited") else money(maxPrice))
            androidx.compose.material3.Slider(
                value = maxPriceStep.toFloat(), onValueChange = { maxPriceStep = it.roundToInt() },
                valueRange = 0f..priceSteps.lastIndex.toFloat(), steps = priceSteps.size - 2,
            )
            FilterTitle(L("Ülke", "Country"))
            FlowChips {
                Chip(L("🌍 Tümü", "🌍 All"), country == null) { country = null }
                Season.activeCountries(g).forEach { c -> Chip(Generator.flag(com.supermenajer.game.model.Leagues.flagOf(c)), country == c) { country = c } }
            }
            FilterTitle(L("Sırala", "Sort"))
            FlowChips {
                listOf(L("Güç", "Overall"), L("Potansiyel", "Potential"), L("En ucuz", "Cheapest"), L("En genç", "Youngest")).forEachIndexed { i, n -> Chip(n, sort == i) { sort = i } }
            }
            Spacer(Modifier.height(10.dp))
            ScoutCard(g)
        }
        },
        second = { listMod ->
        // Sağ (dikeyde altta): oyuncular
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            androidx.compose.foundation.lazy.grid.GridCells.Adaptive(340.dp),
            listMod,
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    L("${list.size}${if (list.size >= 150) "+" else ""} oyuncu · ⭐ kısa listeye ekle, ⚖ karşılaştır (oyuncu profilinden)", "${list.size}${if (list.size >= 150) "+" else ""} players · ⭐ add to shortlist, ⚖ compare (from the player profile)"),
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            if (list.isEmpty()) item { Text(L("Uygun oyuncu bulunamadı.", "No suitable players found."), color = TextDim, modifier = Modifier.padding(16.dp)) }
            items(list.size, key = { list[it].first.id }) { i ->
                val (p, price) = list[i]
                val t = if (p.teamId >= 0) g.teamOrNull(p.teamId) else null
                val team = t?.let { Generator.flag(g.flagOf(it)) + " " + it.short } ?: L("Serbest", "Free agent")
                val pot = if (com.supermenajer.game.game.Scouting.known(g, p) && (mode == 3 || sort == 1)) " · P${p.potential}" else ""
                val star = if (p.id in g.watch) "⭐ " else ""
                PlayerRow(g, p, false, extra = "$star$team · ${money(price)}$pot")
            }
        }
        },
    )
}

/** Bonservis üst sınırı adımları (son adım: sınırsız). */
private val priceSteps = listOf(
    0L, 250_000L, 500_000L, 1_000_000L, 2_000_000L, 3_000_000L, 5_000_000L, 7_500_000L, 10_000_000L, 15_000_000L,
    20_000_000L, 30_000_000L, 50_000_000L, 75_000_000L, 100_000_000L, Long.MAX_VALUE,
)

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

/** Bireysel antrenman + yeni mevki öğretme. */
@Composable
private fun TrainingBox(p: Player) {
    val dev = com.supermenajer.game.game.Development
    Spacer(Modifier.height(12.dp))
    Text(L("BİREYSEL ANTRENMAN", "INDIVIDUAL TRAINING"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text(L("Seçilen özellik haftalık antrenmanlarda öne çıkar.", "The chosen attribute gets priority in weekly training."), color = TextDim, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    FlowChips {
        dev.focusNames.forEachIndexed { i, n ->
            if (i == 6 && p.pos != Pos.GK) return@forEachIndexed
            Chip(n, p.trainFocus == i) { GameRepo.setFocus(p, i) }
        }
    }
    val opts = dev.learnable(p).filter { !com.supermenajer.game.game.Positions.canPlay(p, it) }
    if (opts.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text(L("YENİ MEVKİ ÖĞRET", "TEACH A NEW POSITION"), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        FlowChips {
            opts.forEach { d -> Chip(d.code + " · " + d.label, p.learnDet == d.ordinal) { GameRepo.setLearn(p, d.ordinal) } }
        }
        if (p.learnDet >= 0) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bar(p.learnProgress / 100f, Blue, Modifier.weight(1f), 7.dp)
                Text("  %${p.learnProgress}", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(L("Genç oyuncular yeni mevkiyi daha hızlı öğrenir.", "Young players learn a new position faster."), color = TextDim, fontSize = 11.sp)
        }
    }
}

/** Sözleşme uzatma kutusu. */
@Composable
private fun ContractBox(g: Game, p: Player, onMessage: (String) -> Unit) {
    var years by remember(p.id) { mutableIntStateOf(3) }
    var open by remember(p.id) { mutableStateOf(p.contractYears == 1) }
    if (!open) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(L("📝 Sözleşme Uzat", "📝 Extend Contract")) }
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp)) {
        val demand = com.supermenajer.game.game.Economy.renewalDemand(g, p)
        Text(L("Sözleşme yenileme", "Contract renewal"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (demand == null) {
            Text(L("${p.name} kulüpte mutlu değil, şu an uzatmak istemiyor. Moralini yükseltmeyi dene.", "${p.name} isn't happy at the club and doesn't want to extend right now. Try to raise his morale."), color = Red, fontSize = 12.sp)
            return@Column
        }
        Text(L("İstediği maaş: ${money(demand)}/hafta (şu an ${money(p.wage)})", "Wage requested: ${money(demand)}/week (currently ${money(p.wage)})"), color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { y -> Chip(L("$y yıl", "$y yrs"), years == y, Modifier.weight(1f)) { years = y } }
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val err = GameRepo.renewContract(p, years)
            onMessage(err ?: L("${p.name} ile $years yıllık sözleşme imzalandı! ✍️", "Signed a $years-year contract with ${p.name}! ✍️"))
        }, modifier = Modifier.fillMaxWidth()) { Text(L("İmzala", "Sign"), fontWeight = FontWeight.Bold) }
    }
}

/** Gözlemci ayarları: hangi ülkede, neyi arasın. */
@Composable
private fun ScoutCard(g: Game) {
    var open by remember { mutableStateOf(false) }
    val country = g.scoutCountry.ifEmpty { g.userCountry }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface1).padding(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text(L("🔍 Gözlemci: ${Generator.flag(com.supermenajer.game.model.Leagues.flagOf(country))} ${com.supermenajer.game.model.Leagues.countryName(country)} · ${com.supermenajer.game.game.Scouting.focusNames[g.scoutFocus]}", "🔍 Scout: ${Generator.flag(com.supermenajer.game.model.Leagues.flagOf(country))} ${com.supermenajer.game.model.Leagues.countryName(country)} · ${com.supermenajer.game.game.Scouting.focusNames[g.scoutFocus]}"), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (open) "▲" else "▼", color = TextDim)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(L("Gözlemcin her hafta seçtiğin ülkeden bir oyuncu önerir. Önerilenler Gözlem listesine eklenir ve tüm bilgileri açılır.", "Your scout suggests a player from the chosen country every week. Suggested players are added to the scouting list with full details."), color = TextDim, fontSize = 11.sp)
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


/** A2 (rezerv) takımı: haftalık maçlar, gençlerin gelişimi, oyuncu gönder/al. */
@Composable
private fun ReservesView(g: Game) {
    var msg by remember { mutableStateOf<String?>(null) }
    val res = g.squad(g.userTeamId).filter { it.reserve }.sortedByDescending { it.overall }
    val cand = g.squad(g.userTeamId).filter { !it.reserve && it.id !in g.userTeam.lineup && it.loanFrom < 0 }
        .sortedWith(compareBy<Player> { it.age > 21 }.thenBy { it.overall })
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                DashCard(L("A2 Kadrosu (${res.size})", "B Team Squad (${res.size})"), Modifier.fillMaxWidth()) {
                    Text(L("A2'deki oyuncular A takım maçlarına alınmaz. Her lig haftası A2 maçı oynarlar; 22 yaş altı oyuncular bu maçlarla gelişir, 24 yaş üstü oyuncuların morali düşer.", "Players in the B team aren't picked for first-team matches. They play a B-team match every league week; under-22s develop through these matches, over-24s lose morale."), color = TextDim, fontSize = 12.sp)
                    msg?.let { Text(it, color = Gold, fontSize = 12.sp) }
                    Spacer(Modifier.height(6.dp))
                    if (res.isEmpty()) Text(L("A2 takımında oyuncu yok.", "No players in the B team."), color = TextDim, fontSize = 13.sp)
                    res.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { PlayerRow(g, p, false, extra = L("${p.age} yaş · P${com.supermenajer.game.game.Scouting.potText(g, p)} · moral ${p.morale.toInt()}", "${p.age} yrs · P${com.supermenajer.game.game.Scouting.potText(g, p)} · morale ${p.morale.toInt()}")) }
                            OutlinedButton(onClick = { msg = GameRepo.setReserve(p, false) }) { Text(L("⬆ A takım", "⬆ First team")) }
                        }
                    }
                }
            }
            item {
                DashCard(L("A2 Sonuçları", "B Team Results"), Modifier.fillMaxWidth()) {
                    if (g.a2Results.isEmpty()) Text(L("Henüz A2 maçı oynanmadı.", "No B-team matches played yet."), color = TextDim, fontSize = 13.sp)
                    g.a2Results.take(8).forEach { r ->
                        val c = r.split("|")
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(c[0], color = TextDim, fontSize = 12.sp, modifier = Modifier.width(76.dp))
                            Text(c.getOrElse(1) { "" }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight()) {
            item {
                DashCard(L("A2'ye gönderilebilecekler", "Can be sent to the B team"), Modifier.fillMaxWidth()) {
                    Text(L("İlk 11 dışındaki oyuncular (önce gençler).", "Players outside the starting XI (youngsters first)."), color = TextDim, fontSize = 12.sp)
                    cand.take(20).forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { PlayerRow(g, p, false, extra = L("${p.age} yaş · güç ${p.overall}", "${p.age} yrs · overall ${p.overall}")) }
                            OutlinedButton(onClick = { msg = GameRepo.setReserve(p, true) }) { Text("⬇ A2") }
                        }
                    }
                }
            }
        }
    }
}

/** Gençler ve kiralıktakiler: güç grafiği, potansiyel tahmini ve yardımcının önerisi. */
@Composable
fun YouthView(g: Game) {
    val rows = com.supermenajer.game.game.Youth.rows(g)
    if (rows.isEmpty()) {
        Text(L("Kadroda 21 yaş altı ya da kiralıkta oyuncu yok. Altyapı seçmeleri ve genç transferleriyle başla.", "No under-21 or loaned-out players in the squad. Start with academy trials and young signings."), color = TextDim, modifier = Modifier.padding(16.dp))
        return
    }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        androidx.compose.foundation.lazy.grid.GridCells.Adaptive(400.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(L("Potansiyel aralığı antrenörünün kalitesine göre daralır. Kiralıktaki oyuncuların performansı da burada.", "The potential range narrows with the quality of your coach. Loaned-out players' performances are shown here too."), color = TextDim, fontSize = 12.sp)
        }
        items(rows.size) { i -> YouthCard(g, rows[i]) }
    }
}

@Composable
private fun YouthCard(g: Game, r: com.supermenajer.game.game.Youth.Row) {
    val p = r.p
    val t = g.teamOrNull(p.loanFrom.takeIf { it >= 0 } ?: p.teamId) ?: g.userTeam
    DashCard(null, Modifier.fillMaxWidth(), onClick = { GameRepo.selectedPlayer = p }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerFace(p, 52.dp, Color(t.color1), Color(t.color2))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    L("${p.age} yaş · ${com.supermenajer.game.game.Positions.det(p).code}", "${p.age} yrs · ${com.supermenajer.game.game.Positions.det(p).code}") + (r.loanClub?.let { L(" · 🔁 Kiralık: $it", " · 🔁 On loan: $it") } ?: ""),
                    color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(L("Potansiyel ${r.potLow}–${r.potHigh}", "Potential ${r.potLow}–${r.potHigh}"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OvrBadge(p.overall, 40.dp)
                if (r.growth != 0) Text((if (r.growth > 0) "+" else "") + r.growth, color = if (r.growth > 0) Green else Red, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        GrowthChart(r.history, r.potLow, r.potHigh, Modifier.fillMaxWidth().height(54.dp))
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(when (r.level) { 0 -> Green; 1 -> Gold; else -> Red }))
            Spacer(Modifier.width(6.dp))
            Text(r.advice, fontSize = 12.sp, lineHeight = 15.sp)
        }
    }
}

/** Sezon sonu güçlerinin çizgisi ve potansiyel bandı. */
@Composable
private fun GrowthChart(hist: List<Int>, potLow: Int, potHigh: Int, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val lo = (hist.minOrNull() ?: 50).coerceAtMost(potLow) - 3
        val hi = potHigh + 2
        fun y(v: Int) = size.height * (1f - (v - lo).toFloat() / (hi - lo).coerceAtLeast(1))
        // Potansiyel bandı
        drawRect(Gold.copy(alpha = 0.28f), androidx.compose.ui.geometry.Offset(0f, y(potHigh)), androidx.compose.ui.geometry.Size(size.width, y(potLow) - y(potHigh)))
        val n = hist.size
        if (n == 1) {
            drawCircle(Teal, 5f, androidx.compose.ui.geometry.Offset(size.width - 6f, y(hist[0])))
            return@Canvas
        }
        val step = (size.width - 12f) / (n - 1)
        val path = androidx.compose.ui.graphics.Path()
        hist.forEachIndexed { i, v ->
            val x = 6f + i * step
            if (i == 0) path.moveTo(x, y(v)) else path.lineTo(x, y(v))
        }
        drawPath(path, Teal, style = androidx.compose.ui.graphics.drawscope.Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        hist.forEachIndexed { i, v -> drawCircle(if (i == n - 1) Gold else Teal, if (i == n - 1) 6f else 4f, androidx.compose.ui.geometry.Offset(6f + i * step, y(v))) }
    }
}
