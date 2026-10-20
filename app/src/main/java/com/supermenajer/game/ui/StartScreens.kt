package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Difficulty
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Leagues
import com.supermenajer.game.model.Team

/** Menajer uyrukları: bayrak kodu → ad. */
private val nations = listOf(
    "TR" to "Türk", "GB" to "İngiliz", "ES" to "İspanyol", "DE" to "Alman", "IT" to "İtalyan", "FR" to "Fransız",
    "PT" to "Portekizli", "NL" to "Hollandalı", "BR" to "Brezilyalı", "AR" to "Arjantinli", "AZ" to "Azerbaycanlı", "US" to "Amerikalı",
)

private val reps = listOf(
    "Amatör" to "Hiç profesyonel deneyimin yok. Yönetim seni yakından izler.",
    "Eski futbolcu" to "Futbol camiası seni tanıyor. Dengeli bir başlangıç.",
    "Ünlü" to "Tanınmış bir isimsin. Yönetim sana daha fazla güvenir.",
)

/** Kulübün mali durumu (bütçeye göre). */
fun financeText(t: Team): Pair<String, Color> = when {
    t.budget >= 60_000_000 -> "Zengin" to Green
    t.budget >= 15_000_000 -> "Güvende" to Green
    t.budget >= 3_000_000 -> "Dengeli" to Gold
    else -> "Kısıtlı" to Red
}

fun repStars(rep: Int): Int = when { rep >= 84 -> 5; rep >= 72 -> 4; rep >= 63 -> 3; rep >= 52 -> 2; else -> 1 }

/**
 * FM tarzı 4 adımlı yeni kariyer: 1 Takım seç (ülke → lig → kulüp, bilgi paneli),
 * 2 Menajer profili, 3 Ek seçenekler (zorluk, kovulmama), 4 Özet ve oyuna başla.
 */
@Composable
fun NewGameScreen() {
    var world by remember { mutableStateOf(GameRepo.takePreparedWorld()) }
    LaunchedEffect(Unit) {
        if (world == null) {
            world = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { GameRepo.awaitWorld() }
        }
    }
    var step by remember { mutableIntStateOf(1) }
    var country by remember { mutableStateOf("Türkiye") }
    var div by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var nation by remember { mutableStateOf("TR") }
    var rep by remember { mutableIntStateOf(1) }
    var difficulty by remember { mutableIntStateOf(1) }
    var noFiring by remember { mutableStateOf(false) }
    var national by remember { mutableStateOf("") }
    var starting by remember { mutableStateOf(false) }
    val hasSave = remember { GameRepo.slots().getOrNull(GameRepo.slot - 1)?.exists == true }
    BackHandler { if (step > 1) step-- else GameRepo.screen = Screen.TITLE }
    val w = world
    val name = listOf(first.trim(), last.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    if (starting && w != null) {
        // Kurulum ekranı: FM'deki gibi kısa adımlar, sonra kariyer başlar
        var msg by remember { mutableIntStateOf(0) }
        val msgs = listOf("Menajer ekleniyor", "Turnuvalar kuruluyor", "Fikstür çekiliyor", "Basın toplantısı hazırlanıyor")
        LaunchedEffect(Unit) {
            for (i in msgs.indices) { msg = i; kotlinx.coroutines.delay(450) }
            GameRepo.newCareer(w, name, selected, difficulty, noFiring, nation, rep, intro = true, national = national)
        }
        MenuBackground {
            Row(
                Modifier.align(Alignment.Center).fillMaxWidth(0.7f).clip(RoundedCornerShape(14.dp)).background(Color(0xEE10163A))
                    .border(1.dp, Teal.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameCrest(54.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Yeni Oyun Kurulumu", color = TextDim, fontSize = 12.sp)
                    Text(msgs[msg], fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                CircularProgressIndicator(color = Teal, modifier = Modifier.size(30.dp))
            }
        }
        return
    }

    MenuBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Başlık ve adım çubuğu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOf("Takım Seç", "Menajer Profili", "Ek Seçenekler", "Özet")[step - 1],
                        fontWeight = FontWeight.Black, fontSize = 20.sp,
                    )
                    Text(
                        listOf(
                            "Menajerlik yapacağın kulübü seç",
                            "Yeni profil oluştur",
                            "Oyun dünyası ve zorluk",
                            "Yeni kariyer · Yuva ${GameRepo.slot}" + if (hasSave) " (eski kayıt silinir)" else "",
                        )[step - 1],
                        color = TextDim, fontSize = 12.sp,
                    )
                }
                Text("ADIM $step/4", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDim)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(160.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                    Box(Modifier.fillMaxWidth(step / 4f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Purple, Teal))))
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    1 -> TeamStep(w, country, div, selected, onCountry = { country = it; div = Leagues.top(it).id; selected = -1 }, onDiv = { div = it; selected = -1 }) { selected = it }
                    2 -> ProfileStep(first, last, nation, rep, { first = it.take(16) }, { last = it.take(16) }, { nation = it }) { rep = it }
                    3 -> OptionsStep(w, difficulty, noFiring, national, { difficulty = it }, { national = it }) { noFiring = it }
                    else -> if (w != null) SummaryStep(w, w.team(selected), name, nation, rep, difficulty, noFiring, national)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Alt düğmeler
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                WizardButton(if (step == 1) "Ana Menü" else "‹ Geri", false, Modifier.width(150.dp)) { if (step == 1) GameRepo.screen = Screen.TITLE else step-- }
                Spacer(Modifier.weight(1f))
                if (step == 1 && w != null) {
                    WizardButton("🎲 Rastgele Takım", false, Modifier.width(180.dp)) {
                        val all = w.teams.filter { Leagues.of(it.division) != null }
                        all.randomOrNull()?.let { t ->
                            country = Leagues.of(t.division)!!.country; div = t.division; selected = t.id
                        }
                    }
                }
                val can = when (step) {
                    1 -> selected >= 0 && w != null
                    2 -> first.isNotBlank()
                    else -> w != null
                }
                WizardButton(if (step == 4) "Oyuna Başla ▶" else "Sonraki ›", true, Modifier.width(190.dp), enabled = can) {
                    if (step < 4) step++ else starting = true
                }
            }
        }
    }
}

@Composable
private fun WizardButton(label: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.height(44.dp).clip(shape)
            .background(
                if (primary && enabled) Brush.horizontalGradient(listOf(Color(0xFF7B4DFF), Purple))
                else Brush.horizontalGradient(listOf(Color(0xFF1A2150), Color(0xFF141A3A))),
            )
            .border(1.dp, if (primary && enabled) Color.White.copy(alpha = 0.4f) else CardLine, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (enabled) Color.White else TextDim) }
}

/** Seçilebilir kutu (ülke, lig, kulüp, seçenek). */
@Composable
private fun Tile(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(shape)
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF3A2C86), Color(0xFF1E1A55))) else Brush.verticalGradient(listOf(Color(0xE6161D42), Color(0xE60E1431))))
            .border(if (selected) 2.dp else 1.dp, if (selected) Teal else CardLine, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamStep(
    w: Game?, country: String, div: Int, selected: Int,
    onCountry: (String) -> Unit, onDiv: (Int) -> Unit, onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Sol: ülke ve lig
        Column(Modifier.width(250.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Text("ÜLKE", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Leagues.countries.forEach { c ->
                    Tile(country == c, Modifier.width(78.dp).height(54.dp), onClick = { onCountry(c) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Generator.flag(Leagues.flagOf(c)), fontSize = 20.sp)
                            Text(c, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("LİG", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Leagues.ofCountry(country).forEach { lg ->
                Tile(div == lg.id, Modifier.fillMaxWidth().height(40.dp).padding(bottom = 6.dp), onClick = { onDiv(lg.id) }) {
                    Text(lg.name + if (lg.tier == 2) " · zor mod" else "", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // Orta: kulüpler
        if (w == null) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Teal)
                Spacer(Modifier.height(12.dp))
                Text("Seçilebilir kulüpler yükleniyor...", color = TextDim)
            }
            return@Row
        }
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp), Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(w.divisionTeams(div).sortedBy { it.expected }, key = { it.id }) { t ->
                Tile(t.id == selected, Modifier.height(96.dp), onClick = { onSelect(t.id) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                        TeamBadge(t, 40.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(t.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 12.sp)
                        Text("★".repeat(repStars(t.reputation)), color = Gold, fontSize = 9.sp)
                    }
                }
            }
        }
        // Sağ: kulüp bilgi paneli
        Column(
            Modifier.width(230.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).verticalScroll(rememberScrollState()),
        ) {
            val t = w.teamOrNull(selected)
            if (t == null) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { Text("BİR TAKIM SEÇ", color = TextDim, fontWeight = FontWeight.Bold) }
                return@Column
            }
            Box(Modifier.fillMaxWidth().background(Color(t.color1)).padding(8.dp), contentAlignment = Alignment.Center) {
                Text(t.name, color = contentOn(Color(t.color1)), fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamBadge(t, 54.dp)
                Spacer(Modifier.height(6.dp))
                InfoPair("İTİBAR", "★".repeat(repStars(t.reputation)) + "☆".repeat(5 - repStars(t.reputation)), Gold)
                val (fin, fc) = financeText(t)
                InfoPair("MALİ DURUM", "$fin · ${money(t.budget)}", fc)
                w.squad(t.id).maxByOrNull { it.overall }?.let { InfoPair("YILDIZ OYUNCU", "${it.name} (${it.overall})") }
                InfoPair("STADYUM", "${t.stadiumName.ifBlank { t.name + " Stadı" }} · ${"%,d".format(t.stadium).replace(',', '.')}")
                InfoPair("TAKIM GÜCÜ", "${Lineups.strength(w, t).toInt()}")
                InfoPair("YÖNETİMİN HEDEFİ", Season.objectiveText(t), Teal)
                w.managers[t.id]?.let { InfoPair("ESKİ HOCA", it) }
            }
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String, color: Color = TextMain) {
    Text(label, color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep(
    first: String, last: String, nation: String, rep: Int,
    onFirst: (String) -> Unit, onLast: (String) -> Unit, onNation: (String) -> Unit, onRep: (Int) -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Profil oluştur", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(first, onFirst, label = { Text("Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(last, onLast, label = { Text("Soyadı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text("UYRUK", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                nations.forEach { (code, n) -> Chip("${Generator.flag(code)} $n", nation == code) { onNation(code) } }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("İtibar", fontWeight = FontWeight.Black, fontSize = 16.sp)
            reps.forEachIndexed { i, (n, d) ->
                Tile(rep == i, Modifier.fillMaxWidth(), onClick = { onRep(i) }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(n, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (rep == i) Teal else TextMain)
                        Text(d, color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsStep(
    w: Game?, difficulty: Int, noFiring: Boolean, national: String,
    onDifficulty: (Int) -> Unit, onNational: (String) -> Unit, onNoFiring: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("ZORLUK", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Difficulty.names.forEachIndexed { i, n ->
                Tile(difficulty == i, Modifier.fillMaxWidth(), onClick = { onDifficulty(i) }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(n, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (difficulty == i) Teal else TextMain)
                        Text(Difficulty.descriptions[i], color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("AYRICALIKLAR", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ToggleRow("🛡 Kovulamaz ol", "Yönetim seni asla kovmaz (serbest mod).", noFiring) { onNoFiring(!noFiring) }
            ToggleRow("🌍 Kulüp ve milli takım", "Kulübünle birlikte bir milli takımı da çalıştır.", national.isNotEmpty()) {
                onNational(if (national.isNotEmpty()) "" else (w?.let { com.supermenajer.game.game.National.nations(it).firstOrNull { c -> c == "TR" } } ?: "TR"))
            }
            if (national.isNotEmpty() && w != null) {
                val list = remember(w) { com.supermenajer.game.game.National.nations(w) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    list.take(16).forEach { c -> Chip("${Generator.flag(c)} ${com.supermenajer.game.game.National.name(c)}", national == c) { onNational(c) } }
                }
            }
            ToggleRow("🔍 Özellik maskeleme", "Gözlemci raporu olmayan oyuncuların değerleri tahmini görünür.", true, enabled = false) {}
            ToggleRow("🌍 13 lig aktif", "Türkiye, İngiltere, İspanya, Almanya, İtalya, Fransa, Portekiz ve Hollanda.", true, enabled = false) {}
        }
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, on: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (on) Purple else Color.Transparent).border(2.dp, if (on) Purple else TextDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (enabled) TextMain else TextDim)
            Text(desc, color = TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SummaryStep(w: Game, t: Team, name: String, nation: String, rep: Int, difficulty: Int, noFiring: Boolean, national: String) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("YÖNETİLEN TAKIM", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(t, 60.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("${Generator.flag(w.flagOf(t))} ${w.leagueName(t)}", color = TextDim, fontSize = 13.sp)
                    Text("★".repeat(repStars(t.reputation)), color = Gold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            InfoLineW("Yönetimin hedefi", Season.objectiveText(t))
            InfoLineW("Transfer bütçesi", money((t.budget * Difficulty.budgetFactor(difficulty)).toLong()))
            InfoLineW("Stadyum", "${t.stadiumName.ifBlank { t.name + " Stadı" }} (${"%,d".format(t.stadium).replace(',', '.')})")
            InfoLineW("Başlangıç", "Hazırlık dönemi · ${w.season}-${(w.season + 1) % 100} sezonu")
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xE60E1431))
                .border(1.dp, CardLine, RoundedCornerShape(12.dp)).padding(14.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("MENAJER AYRINTILARI", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFF3A4580), Color(0xFF1A2050)))), contentAlignment = Alignment.Center) {
                    Text(name.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name.uppercase(), fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("${Generator.flag(nation)} ${nations.firstOrNull { it.first == nation }?.second ?: ""} · ${reps[rep].first}", color = TextDim, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            InfoLineW("Zorluk", Difficulty.names[difficulty])
            InfoLineW("Kovulma", if (noFiring) "Kapalı (serbest mod)" else "Açık")
            if (national.isNotEmpty()) InfoLineW("Milli takım", "${Generator.flag(national)} ${com.supermenajer.game.game.National.name(national)}")
            InfoLineW("Menajerlik unvanları", "Unvansız başlıyor")
        }
    }
}

@Composable
private fun InfoLineW(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
    }
}
