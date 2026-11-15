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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Inbox
import com.supermenajer.game.model.News

/** Kulüp menüsü: teknik ekip, tesisler, sponsorlar, finans ve taraftar. */
@Composable
fun ClubTab(g: Game) {
    // Telefonda tüm bölümler tek satırda görünsün diye kısa adlı sekmeler
    val modes = listOf(L("Ekip", "Staff"), L("Tesisler", "Facilities"), L("Sponsor", "Sponsors"), L("Finans", "Finances"), L("Taraftar", "Fans"), L("Kariyer", "Career"), L("Yetenek", "Skills"), L("Hocalar", "Managers"), L("Efsaneler", "Legends"), L("Vizyon", "Vision"), L("Kimlik", "Identity"))
    Column(Modifier.fillMaxSize()) {
        val dots = buildSet {
            if (g.sponsorOffers.isNotEmpty()) add(2)
            if (com.supermenajer.game.game.Perks.points(g) > 0) add(6)
            if (g.vacancies.any { !it.applied }) add(7)
        }
        FmTabs(modes, GameRepo.clubMode, dots = dots) { GameRepo.clubMode = it }
        when (GameRepo.clubMode) {
            0 -> StaffView(g)
            1 -> FacilitiesView(g)
            2 -> SponsorsView(g)
            3 -> TwoCards({ FinanceCard(g) }, { BudgetShiftCard(g); Spacer(Modifier.height(10.dp)); BoardRequestCard(g); Spacer(Modifier.height(10.dp)); BoardCard(g) })
            4 -> TwoCards({ FansCard(g) }, { FanInfoCard() })
            5 -> CareerView(g)
            6 -> PerksView(g)
            8 -> LegendsView(g)
            9 -> VisionView(g)
            10 -> IdentityView(g)
            else -> ManagerMarketView(g)
        }
    }
}

@Composable
private fun TwoCards(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    if (portrait()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { left(); right() }
        return
    }
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { left() }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { right() }
    }
}

/** FM tarzı bütçe ayarı: transfer parasını haftalık maaş bütçesine kaydır (ya da tersi). */
@Composable
private fun BudgetShiftCard(g: Game) {
    var msg by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val t = g.userTeam
    val wages = com.supermenajer.game.game.Economy.squadWages(g, t.id)
    SectionCard(L("⚖️ Bütçe ayarı", "⚖️ Budget split")) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(L("Transfer bütçesi", "Transfer budget"), color = TextDim, fontSize = 11.sp)
                Text(com.supermenajer.game.game.money(t.budget), color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(L("Maaş bütçesi", "Wage budget"), color = TextDim, fontSize = 11.sp)
                Text(L("${com.supermenajer.game.game.money(t.wageBudget)}/hf", "${com.supermenajer.game.game.money(t.wageBudget)}/wk"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(L("Kullanılan: ${com.supermenajer.game.game.money(wages)}", "Used: ${com.supermenajer.game.game.money(wages)}"), color = TextDim, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(L("Transfer → Maaş", "Transfer → Wages"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10_000L, 25_000L, 50_000L).forEach { w ->
                OutlinedButton(onClick = { msg = GameRepo.shiftBudget(w) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Text(L("+${com.supermenajer.game.game.money(w)}/hf", "+${com.supermenajer.game.game.money(w)}/wk"), fontSize = 12.sp)
                }
            }
        }
        Text(L("Maaş → Transfer", "Wages → Transfer"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10_000L, 25_000L, 50_000L).forEach { w ->
                OutlinedButton(onClick = { msg = GameRepo.shiftBudget(-w) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Text("+${com.supermenajer.game.game.money(w * com.supermenajer.game.game.Economy.SHIFT_RATIO)}", fontSize = 12.sp)
                }
            }
        }
        msg?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Text(L("1 haftalık maaş = ${com.supermenajer.game.game.Economy.SHIFT_RATIO} katı transfer parası. Ayar bu sezon için geçerli; yeni sezonda yönetim maaş bütçesini yeniden belirler.", "1 week of wages = ${com.supermenajer.game.game.Economy.SHIFT_RATIO}x transfer money. Applies this season; the board sets a new wage budget next season."), color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Yönetimden istekler (FM tarzı). */
@Composable
private fun BoardRequestCard(g: Game) {
    var msg by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val C = com.supermenajer.game.game.Career
    SectionCard(L("🗣️ Yönetimden iste", "🗣️ Ask the board")) {
        Text(L("Her konu sezonda bir kez açılabilir. Yönetimin sana güveni ve itibarın ne kadar yüksekse kabul şansı o kadar artar; ret güveni biraz düşürür.", "Each topic can be raised once a season. The higher the board's confidence and your reputation, the better the chance; a refusal lowers confidence a little."), color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        C.requestNames.forEachIndexed { i, n ->
            OutlinedButton(
                onClick = { msg = GameRepo.boardRequest(i) }, enabled = i !in g.boardAsked,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) { Text(if (i in g.boardAsked) "$n ✓" else n, fontSize = 13.sp) }
        }
        msg?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Menajer profili, kupa dolabı, kulüp rekorları ve başarımlar. */
@Composable
private fun CareerView(g: Game) {
    val C = com.supermenajer.game.game.Career
    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard(L("👤 Menajer", "👤 Manager")) {
                Text(g.managerName, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(L("${g.userTeam.name} · ${g.seasonsAtClub + 1}. sezon", "${g.userTeam.name} · season ${g.seasonsAtClub + 1}"), color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("İtibar", "Reputation"), color = TextDim, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                    Bar(g.managerRep / 100f, Gold, Modifier.weight(1f), 8.dp)
                    Text(" ${g.managerRep}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(C.repText(g.managerRep), color = Gold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(L("KUPA DOLABI", "TROPHY CABINET"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (g.trophies.isEmpty()) Text(L("Henüz kupa yok. İlk kupa yakında!", "No trophies yet. The first one is coming!"), color = TextDim, fontSize = 12.sp)
                g.trophies.forEach { Text(it, fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp)) }
            }
            SectionCard(L("📊 Kulüp rekorları (senin dönemin)", "📊 Club records (your era)")) {
                RecordLine(L("En farklı galibiyet", "Biggest win"), g.recBigWin)
                RecordLine(L("En ağır yenilgi", "Heaviest defeat"), g.recLoss)
                RecordLine(L("Rekor transfer", "Record signing"), g.recSigning)
                RecordLine(L("Rekor satış", "Record sale"), g.recSale)
                RecordLine(L("Rekor seyirci", "Record attendance"), if (g.recAttendance > 0) "%,d".format(g.recAttendance).replace(',', '.') else "")
            }
        }
        Column(Modifier.weight(1.2f).fillMaxHeight()) {
            Text(L("BAŞARIMLAR · ${g.achievements.size}/${C.all.size}", "ACHIEVEMENTS · ${g.achievements.size}/${C.all.size}"), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                androidx.compose.foundation.lazy.grid.GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(C.all.size) { i ->
                    val a = C.all[i]
                    val on = a.id in g.achievements
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (on) Gold.copy(alpha = 0.18f) else Surface1).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (on) a.icon else "🔒", fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(a.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (on) Gold else TextMain, maxLines = 1)
                            Text(a.desc, color = TextDim, fontSize = 10.sp, maxLines = 2, lineHeight = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(120.dp))
        Text(value.ifEmpty { "–" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FanInfoCard() {
    SectionCard(L("Taraftarı ne etkiler?", "What affects the fans?")) {
        listOf(
            L("✅ Galibiyetler, özellikle derbi ve sürpriz galibiyetler", "✅ Wins, especially derbies and upsets"),
            L("✅ Yıldız transferleri, kupalar, Avrupa bileti", "✅ Star signings, trophies, European qualification"),
            L("✅ Ucuz bilet ve stadyum büyütme", "✅ Cheap tickets and stadium expansion"),
            L("❌ Ağır ve derbi yenilgileri", "❌ Heavy defeats and derby losses"),
            L("❌ En iyi oyuncularını satmak", "❌ Selling your best players"),
            L("❌ Pahalı bilet, küme düşme", "❌ Expensive tickets, relegation"),
        ).forEach { Text(it, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(L("Memnun taraftar statı doldurur, sponsorları çeker ve yönetimin güvenini artırır. %75'in üstünde evde 12. adam olur!", "Happy fans fill the stadium, attract sponsors and boost the board's confidence. Above 75% they become the 12th man at home!"), color = TextDim, fontSize = 12.sp)
    }
}

/** Gelen kutusu: solda mesaj listesi, sağda seçili mesaj (CM tarzı). */
@Composable
fun InboxTab(g: Game) {
    GameRepo.version
    val cat = GameRepo.inboxCat
    val list = g.news.filter { cat == Inbox.ALL || it.category == cat }
    val sel = GameRepo.inboxSel?.takeIf { it in g.news } ?: list.firstOrNull()
    val vertical = portrait()
    // Dikey: önce liste, dokunulan mesaj tam ekran açılır
    val opened = GameRepo.inboxSel?.takeIf { it in g.news }
    if (vertical && opened != null) {
        androidx.activity.compose.BackHandler { GameRepo.inboxSel = null }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(L("‹ Gelen kutusu", "‹ Inbox"), color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { GameRepo.inboxSel = null }.padding(vertical = 6.dp, horizontal = 2.dp))
            Spacer(Modifier.height(8.dp))
            MessageDetail(g, opened)
        }
        return
    }
    if (!vertical) androidx.compose.runtime.LaunchedEffect(sel) { if (sel != null && !sel.read) GameRepo.openNews(sel) }
    Row(Modifier.fillMaxSize()) {
        Column((if (vertical) Modifier.fillMaxWidth() else Modifier.width(330.dp)).fillMaxHeight().background(Surface1)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Inbox.names.forEachIndexed { i, n ->
                    val unread = g.news.count { !it.read && (i == Inbox.ALL || it.category == i) }
                    Chip(if (unread > 0) "$n ($unread)" else n, cat == i) { GameRepo.inboxCat = i; GameRepo.inboxSel = null }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(list) { n -> MessageRow(g, n, n === sel) }
            }
            if (g.unread > 0) {
                TextButton(onClick = { GameRepo.readAll() }, modifier = Modifier.fillMaxWidth()) { Text(L("Tümünü okundu say", "Mark all as read")) }
            }
        }
        if (!vertical) Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp)) {
            if (sel == null) {
                Text(L("Mesaj yok.", "No messages."), color = TextDim)
            } else {
                MessageDetail(g, sel)
            }
        }
    }
}

@Composable
private fun MessageDetail(g: Game, sel: News) {
    Text(Inbox.names[sel.category].uppercase(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(sel.title, fontWeight = FontWeight.Black, fontSize = 20.sp)
    Text("${sel.season} · ${g.dayLabel(sel.week).ifEmpty { L("Sezon arası", "Off-season") }}", color = TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(14.dp))
    Text(sel.body, fontSize = 15.sp, lineHeight = 22.sp)
    Spacer(Modifier.height(16.dp))
    MessageAction(g, sel)
}

@Composable
private fun MessageRow(g: Game, n: News, selected: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) Surface2 else Color.Transparent)
            .clickable { GameRepo.openNews(n) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (n.read) Color.Transparent else Red))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                n.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, color = if (n.read) TextDim else TextMain,
            )
            Text(n.body, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        Text(g.dayLabel(n.week).substringBefore(" ·").take(10), fontSize = 10.sp, color = TextDim, maxLines = 1)
    }
}

/** Mesaja göre ilgili ekrana kısayol. */
@Composable
private fun MessageAction(g: Game, n: News) {
    val t = n.title.lowercase()
    val (label, go) = when {
        "sponsor" in t -> L("Sponsorlara git", "Go to sponsors") to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 2 }
        "inşaat" in t || "altyapı" in t || "construction" in t || "youth intake" in t -> L("Tesislere git", "Go to facilities") to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 1 }
        "teknik ekip" in t || "staff" in t -> L("Teknik ekibe git", "Go to staff") to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 0 }
        "taraftar" in t || "fan" in t -> L("Taraftar durumuna git", "Go to fans") to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 4 }
        "başarım" in t || "achievement" in t -> L("Kariyerime git", "Go to my career") to { GameRepo.tab = GameRepo.TAB_CLUB; GameRepo.clubMode = 5 }
        ("teklif" in t && "kariyer" !in t) || ("offer" in t && "job" !in t) -> L("Ana sayfaya git", "Go to home") to { GameRepo.tab = GameRepo.TAB_HOME }
        "gözlemci" in t || "scout" in t -> L("Gözlem listesine git", "Go to scouting list") to { GameRepo.tab = GameRepo.TAB_TRANSFER }
        "yardımcı antrenör" in t || "assistant" in t -> L("Kadroya git", "Go to squad") to { GameRepo.tab = GameRepo.TAB_SQUAD }
        "kariyer" in t || "kovul" in t || "job" in t || "sacked" in t -> L("Ana sayfaya git", "Go to home") to { GameRepo.tab = GameRepo.TAB_HOME }
        else -> return
    }
    OutlinedButton(onClick = go) { Text("$label →") }
}


/** Kulüp efsaneleri: en çok forma giyenler, en golcüler ve futbolu bırakmış efsaneler. */
@Composable
private fun LegendsView(g: Game) {
    val rows = androidx.compose.runtime.remember(GameRepo.version / 10) { com.supermenajer.game.game.Legends.table(g) }
    TwoCards({
        SectionCard(L("🏟️ En Çok Forma Giyenler", "🏟️ Most Appearances")) {
            rows.sortedByDescending { it.apps }.take(10).forEachIndexed { i, r -> LegendRow(i, r.name, L("${r.apps} maç", "${r.apps} apps"), r.active) }
            if (rows.isEmpty()) Text(L("Henüz veri yok: sezonlar geçtikçe efsaneler oluşur.", "No data yet: legends emerge as seasons pass."), color = TextDim, fontSize = 13.sp)
        }
    }, {
        SectionCard(L("⚽ Kulübün Golcüleri", "⚽ Club Top Scorers")) {
            rows.filter { it.goals > 0 }.sortedByDescending { it.goals }.take(10).forEachIndexed { i, r -> LegendRow(i, r.name, L("${r.goals} gol", "${r.goals} goals"), r.active) }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(L("👑 Şöhretler Galerisi", "👑 Hall of Fame")) {
            if (g.hallOfFame.isEmpty()) Text(L("Kulübümüzde en az 40 maç oynayıp futbolu bırakanlar burada anılır.", "Players who retired after at least 40 matches for us are honoured here."), color = TextDim, fontSize = 13.sp)
            g.hallOfFame.forEach { s ->
                val c = s.split("|")
                if (c.size >= 4) Text(L("${c[0]} · ${c[1]} maç, ${c[2]} gol · ${c[3]}'te veda etti", "${c[0]} · ${c[1]} apps, ${c[2]} goals · retired in ${c[3]}"), fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    })
}

@Composable
private fun LegendRow(i: Int, name: String, value: String, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${i + 1}.", color = if (i < 3) Gold else TextDim, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(28.dp))
        Text(name + if (active) "" else L(" (ayrıldı)", " (left)"), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}


/** Kulüp vizyonu (5 yıllık plan) ve rakip menajer karnesi. */
@Composable
private fun VisionView(g: Game) {
    com.supermenajer.game.game.Vision.ensure(g)
    val goals = com.supermenajer.game.game.Vision.list(g)
    TwoCards({
        SectionCard(L("🏛 Yönetimin 5 Yıllık Vizyonu", "🏛 The Board's 5-Year Vision")) {
            Text(L("Her sezon sonunda değerlendirilir. Başarılan hedef: +10 güven, itibar ve XP. Kaçan hedef: -10 güven.", "Reviewed at the end of each season. Achieved goal: +10 confidence, reputation and XP. Missed goal: -10 confidence."), color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            goals.forEach { v ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when (v.status) { 1 -> "✅"; 2 -> "❌"; else -> "⏳" }, fontSize = 16.sp, modifier = Modifier.width(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = when (v.status) { 1 -> Green; 2 -> Red; else -> TextMain })
                        val left = v.deadline - g.season
                        Text(
                            when (v.status) {
                                1 -> L("Başarıldı", "Achieved")
                                2 -> L("Kaçırıldı", "Missed")
                                else -> if (left <= 0) L("Bu sezon sonu son tarih!", "Deadline is the end of this season!") else L("Son tarih: ${v.deadline}-${(v.deadline + 1) % 100} sezonu ($left sezon kaldı)", "Deadline: ${v.deadline}-${(v.deadline + 1) % 100} season ($left seasons left)")
                            },
                            color = TextDim, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }, {
        SectionCard(L("🎙️ Rakip Menajerlerle Karnen", "🎙️ Your Record vs Rival Managers")) {
            if (g.h2h.isEmpty()) Text(L("Henüz rakip hocalarla karşılaşmadın.", "You haven't faced rival managers yet."), color = TextDim, fontSize = 13.sp)
            g.h2h.entries.sortedByDescending { e -> e.value.split(",").sumOf { it.toIntOrNull() ?: 0 } }.take(15).forEach { (m, v) ->
                val c = v.split(",").map { it.toIntOrNull() ?: 0 }
                val team = g.managers.entries.firstOrNull { it.value == m }?.key?.let { g.teamOrNull(it)?.name } ?: ""
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (team.isNotEmpty()) Text(team, color = TextDim, fontSize = 11.sp)
                    }
                    Text("${c.getOrElse(0) { 0 }}G", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(34.dp))
                    Text("${c.getOrElse(1) { 0 }}B", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(34.dp))
                    Text("${c.getOrElse(2) { 0 }}M", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(34.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(L("Büyük maçlardan önce rakip hocalar basına laf atabilir; cevabın takımının moralini ve rakibin hırsını etkiler.", "Before big matches rival managers may take shots in the press; your answer affects your team's morale and the opponent's hunger."), color = TextDim, fontSize = 11.sp)
        }
    })
}
