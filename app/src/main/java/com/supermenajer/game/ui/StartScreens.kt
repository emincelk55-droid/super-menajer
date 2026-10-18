package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.supermenajer.game.model.Leagues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Lineups
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.game.money

@Composable
fun TitleScreen() {
    val hasSave = remember { GameRepo.hasSave() }
    LaunchedEffect(Unit) { GameRepo.prepareWorldAsync() }
    var error by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0F3D2A), Bg, Bg))).padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚽", fontSize = 64.sp)
            Text("SÜPER", fontSize = 44.sp, fontWeight = FontWeight.Black, color = TextMain, letterSpacing = 6.sp)
            Text("MENAJER", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Green, letterSpacing = 6.sp)
            Text("Futbol Menajerlik Oyunu", color = TextDim, fontSize = 15.sp)
        }
        Column(Modifier.width(340.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasSave) {
                Button(
                    onClick = {
                        if (GameRepo.load()) GameRepo.screen = Screen.MAIN else error = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("KARİYERE DEVAM ET", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(12.dp))
            }
            val newBtn: @Composable () -> Unit = { Text("YENİ KARİYER", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (hasSave) {
                OutlinedButton(
                    onClick = { GameRepo.screen = Screen.NEW_GAME },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
                ) { newBtn() }
            } else {
                Button(
                    onClick = { GameRepo.screen = Screen.NEW_GAME },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
                ) { newBtn() }
            }
            Spacer(Modifier.height(12.dp))
            val ctx = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.material3.TextButton(onClick = { (ctx as? android.app.Activity)?.finishAndRemoveTask() }) {
                Text("Oyundan çık", color = TextDim)
            }
            if (error) {
                Spacer(Modifier.height(12.dp))
                Text("Kayıt dosyası okunamadı.", color = Red)
            }
            Spacer(Modifier.height(20.dp))
            Text("v1.1", color = TextDim, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewGameScreen() {
    var world by remember { mutableStateOf(GameRepo.takePreparedWorld()) }
    LaunchedEffect(Unit) {
        if (world == null) {
            world = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                Generator.newWorld().also { Season.computeExpectations(it) }
            }
        }
    }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(-1) }
    var country by remember { mutableStateOf("Türkiye") }
    var div by remember { mutableIntStateOf(0) }
    val hasSave = remember { GameRepo.hasSave() }
    BackHandler { GameRepo.screen = Screen.TITLE }
    val w = world

    Row(Modifier.fillMaxSize()) {
        // Sol panel: ad, ülke, lig ve başla düğmesi
        Column(Modifier.width(330.dp).fillMaxHeight().background(Surface1).padding(14.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = { GameRepo.screen = Screen.TITLE }) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                    Text("Yeni Kariyer", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(24) },
                    label = { Text("Menajer adın") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Ülke", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Leagues.countries.forEach { c ->
                        Chip("${Generator.flag(Leagues.flagOf(c))} $c", country == c) {
                            country = c
                            div = Leagues.top(c).id
                            selected = -1
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Lig", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Leagues.ofCountry(country).forEach { lg ->
                        Chip(lg.name + if (lg.tier == 2) " (zor mod)" else "", div == lg.id) { div = lg.id; selected = -1 }
                    }
                }
                if (hasSave) {
                    Spacer(Modifier.height(10.dp))
                    Text("Not: Yeni kariyer eski kaydının üzerine yazılır.", color = Gold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { w?.let { GameRepo.newCareer(it, name, selected) } },
                enabled = selected >= 0 && w != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
            ) { Text(if (selected >= 0) "KARİYERE BAŞLA" else "TAKIM SEÇ →", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        // Sağ: takım listesi
        if (w == null) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Green)
                Spacer(Modifier.height(12.dp))
                Text("Futbol dünyası oluşturuluyor...\n13 lig · 234 takım", color = TextDim, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(250.dp),
                Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(w.divisionTeams(div).sortedBy { it.expected }, key = { it.id }) { t ->
                    val sel = t.id == selected
                    val stars = when { t.reputation >= 84 -> 5; t.reputation >= 72 -> 4; t.reputation >= 63 -> 3; t.reputation >= 52 -> 2; else -> 1 }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1)
                            .border(2.dp, if (sel) Green else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { selected = t.id }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TeamBadge(t, 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text("★".repeat(stars) + "☆".repeat(5 - stars), color = Gold, fontSize = 12.sp)
                            Text("${Season.objectiveText(t)} · ${money(t.budget)}", color = TextDim, fontSize = 11.sp, maxLines = 1)
                        }
                        OvrBadge(Lineups.strength(w, t).toInt())
                    }
                }
            }
        }
    }
}
