package com.supermenajer.game.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var error by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F3D2A), Bg, Bg))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚽", fontSize = 84.sp)
            Spacer(Modifier.height(12.dp))
            Text("SÜPER", fontSize = 44.sp, fontWeight = FontWeight.Black, color = TextMain, letterSpacing = 6.sp)
            Text("MENAJER", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Green, letterSpacing = 6.sp)
            Spacer(Modifier.height(8.dp))
            Text("Futbol Menajerlik Oyunu", color = TextDim, fontSize = 16.sp)
            Spacer(Modifier.height(48.dp))
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
            if (error) {
                Spacer(Modifier.height(12.dp))
                Text("Kayıt dosyası okunamadı.", color = Red)
            }
            Spacer(Modifier.height(40.dp))
            Text("v0.4", color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
fun NewGameScreen() {
    val world = remember { Generator.newWorld().also { Season.computeExpectations(it) } }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(-1) }
    var div by remember { mutableIntStateOf(0) }
    val hasSave = remember { GameRepo.hasSave() }
    BackHandler { GameRepo.screen = Screen.TITLE }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Yeni Kariyer", fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(24) },
                label = { Text("Menajer adın") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("Takımını seç", color = TextDim, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Süper Lig", div == 0) { div = 0 }
                Chip("1. Lig (zor mod)", div == 1) { div = 1 }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(world.divisionTeams(div).sortedBy { it.expected }, key = { it.id }) { t ->
                val sel = t.id == selected
                val stars = when { t.reputation >= 84 -> 5; t.reputation >= 72 -> 4; t.reputation >= 63 -> 3; t.reputation >= 52 -> 2; else -> 1 }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1)
                        .border(2.dp, if (sel) Green else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { selected = t.id }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamBadge(t, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("★".repeat(stars) + "☆".repeat(5 - stars), color = Gold, fontSize = 13.sp)
                        Text("Hedef: ${Season.objectiveText(t)} · Bütçe: ${money(t.budget)}", color = TextDim, fontSize = 12.sp)
                    }
                    OvrBadge(Lineups.strength(world, t).toInt())
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            if (hasSave) {
                Text("Not: Yeni kariyer eski kaydının üzerine yazılır.", color = Gold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { GameRepo.newCareer(world, name, selected) },
                enabled = selected >= 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
            ) { Text("KARİYERE BAŞLA", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}
