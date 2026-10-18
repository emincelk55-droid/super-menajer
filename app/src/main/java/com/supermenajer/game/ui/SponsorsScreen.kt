package com.supermenajer.game.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Economy
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Sponsors
import com.supermenajer.game.game.money
import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Sponsor

private val styleColors = listOf(Blue, Green, Gold)

@Composable
fun SponsorsView(g: Game) {
    @Suppress("UNUSED_VARIABLE") val v = GameRepo.version
    var message by remember { mutableStateOf<String?>(null) }
    val us = g.userTeam
    LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(330.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = StaggeredGridItemSpan.FullLine) {
            SectionCard("💼 Sponsorluk özeti") {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Bu sezon kazanılan", color = TextDim, fontSize = 11.sp)
                        Text(money(us.income[Economy.SPONSOR] ?: 0L), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Green)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Sezonluk sabit", color = TextDim, fontSize = 11.sp)
                        Text(money(Sponsors.seasonBase(g)), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Galibiyet primi", color = TextDim, fontSize = 11.sp)
                        Text(money(g.sponsors.sumOf { it.winBonus }), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Teklifler kulübün itibarına, ligine, Avrupa'da oynamasına ve şampiyonluklara göre büyür.", color = TextDim, fontSize = 12.sp)
                message?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        for (slot in 0..2) {
            item(key = "slot$slot") {
                SectionCard("${Sponsors.slotIcons[slot]} ${Sponsors.slotNames[slot]}") {
                    val cur = Sponsors.current(g, slot)
                    if (cur != null) {
                        SponsorDeal(cur, showYears = true)
                        if (slot == Sponsors.STADIUM) Text("Stadının yeni adı: ${us.stadiumName}", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        val offers = Sponsors.offers(g, slot)
                        if (offers.isEmpty()) {
                            Text("Bu alan boş. Yeni teklifler sezon başında gelecek.", color = TextDim, fontSize = 13.sp)
                        } else {
                            Text("Boş! Gelen teklifler:", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            offers.forEach { o ->
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(Surface2).padding(10.dp),
                                ) {
                                    SponsorDeal(o, showYears = false)
                                    Text(Sponsors.styleInfo[o.style], color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                    Button(
                                        onClick = { message = GameRepo.signSponsor(o) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    ) { Text("İmzala · ${o.yearsLeft} sezon") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsorDeal(s: Sponsor, showYears: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(styleColors[s.style]).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(Sponsors.styleNames[s.style], color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth()) {
        DealStat("Sezonluk", money(s.base), Modifier.weight(1f))
        DealStat("Galibiyet", if (s.winBonus > 0) money(s.winBonus) else "-", Modifier.weight(1f))
        DealStat("Kupa", if (s.titleBonus > 0) money(s.titleBonus) else "-", Modifier.weight(1f))
    }
    if (showYears) Text("${s.yearsLeft} sezon kaldı", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun DealStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextDim, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** Ana sayfa: bekleyen sponsor teklifleri. */
@Composable
fun SponsorOffersCard(g: Game) {
    val slots = g.sponsorOffers.map { it.slot }.distinct()
    SectionCard("💼 Sponsor teklifleri") {
        Text(
            "${slots.joinToString { Sponsors.slotNames[it].lowercase() }} için teklifler bekliyor. Boş alanlar para kazandırmaz!",
            color = TextDim, fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { GameRepo.tab = 1; GameRepo.squadMode = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Teklifleri incele") }
    }
}
