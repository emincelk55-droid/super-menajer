package com.supermenajer.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermenajer.game.game.Challenges
import com.supermenajer.game.game.DailyReward
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.model.Game

/* ---------- Günlük ödül ve maç görevleri kartları ---------- */

/** Günlük giriş ödülü: 7 günlük seri şeridi ve "ödülü al" düğmesi. Alındıktan sonra mesajla kapanır. */
@Composable
fun DailyRewardCard(g: Game) {
    val available = DailyReward.available(g)
    val msg = GameRepo.dailyMsg
    val day = if (available) DailyReward.nextDay(g) else ((g.dailyStreak - 1).coerceAtLeast(0) % 7 + 1)
    DashCard(L("🎁 Günlük Ödül", "🎁 Daily Reward"), Modifier.fillMaxWidth()) {
        Text(
            if (available) L("Her gün gel, seriyi büyüt: 7. gün büyük ödül! Seri: ${g.dailyStreak} gün", "Come back every day to grow your streak: big prize on day 7! Streak: ${g.dailyStreak} days")
            else L("Seri: ${g.dailyStreak} gün · yarın yeni ödül seni bekliyor.", "Streak: ${g.dailyStreak} days · a new reward is waiting tomorrow."),
            color = TextDim, fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in DailyReward.rewards(g)) {
                val claimed = r.day < day || (!available && r.day == day)
                val today = available && r.day == day
                val shape = RoundedCornerShape(8.dp)
                Column(
                    Modifier.weight(1f).clip(shape)
                        .background(if (today) Gold.copy(alpha = 0.18f) else if (claimed) Green.copy(alpha = 0.12f) else Surface1)
                        .border(if (today) 2.dp else 1.dp, if (today) Gold else CardLine, shape)
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(L("${r.day}. gün", "Day ${r.day}"), color = if (today) Gold else TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (claimed) "✅" else r.icon, fontSize = if (r.day == 7) 22.sp else 18.sp)
                    Text(r.text, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 11.sp, overflow = TextOverflow.Ellipsis, color = if (claimed) TextDim else TextMain)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (available) {
            SmActionButton(L("🎁  ÖDÜLÜ AL", "🎁  CLAIM REWARD"), Modifier.fillMaxWidth()) { GameRepo.claimDaily() }
        } else if (msg != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎉 $msg", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SideButton(L("Tamam", "OK"), modifier = Modifier.height(36.dp)) { GameRepo.dailyMsg = null }
            }
        }
    }
}

/** Maç hazırlığında: bu maçın 3 görevi. */
@Composable
fun ChallengeStrip(g: Game) {
    val list = Challenges.list(g).filter { it.state == 0 }
    if (list.isEmpty()) return
    Text(L("🎯 MAÇ GÖREVLERİ · üçü birden +30 XP bonus", "🎯 MATCH CHALLENGES · all three: +30 XP bonus"), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (v in list) ChallengeChip(v, Modifier.weight(1f))
    }
}

@Composable
private fun ChallengeChip(v: Challenges.View, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val (bg, line) = when (v.state) {
        1 -> Green.copy(alpha = 0.15f) to Green
        2 -> Red.copy(alpha = 0.10f) to Red.copy(alpha = 0.6f)
        else -> Surface1 to CardLine
    }
    Row(modifier.clip(shape).background(bg).border(1.dp, line, shape).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(when (v.state) { 1 -> "✅"; 2 -> "❌"; else -> v.c.icon }, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(v.c.title, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("+${v.c.xp}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Maç özeti: görev sonuçları ve kazanılan ödül. */
@Composable
fun ChallengeResultCard(list: List<Challenges.View>) {
    val ok = list.count { it.state == 1 }
    val xp = list.filter { it.state == 1 }.sumOf { it.c.xp } + if (ok == 3) 30 else 0
    DashCard(L("🎯 Maç Görevleri · $ok/3", "🎯 Match Challenges · $ok/3"), Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for (v in list) ChallengeChip(v, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            Text(
                when {
                    ok == 3 -> L("💯 Kusursuz! +$xp XP (bonus dahil) ve yönetim primi", "💯 Perfect! +$xp XP (bonus included) and a board bonus")
                    ok > 0 -> L("+$xp XP ve yönetim primi kazandın.", "You earned +$xp XP and a board bonus.")
                    else -> L("Bu kez olmadı; sıradaki maçın yeni görevleri hazır.", "Not this time; new challenges are ready for the next match.")
                },
                color = if (ok > 0) Gold else TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Ana sayfa ayrıntılı görünümü için kompakt sayaç (görev sayısı ve seri). */
@Composable
fun EngagementLine(g: Game) {
    Text(
        L("📅 Seri ${g.dailyStreak} gün (en iyi ${g.dailyBest}) · ✅ ${g.challengesDone} maç görevi · 💯 ${g.challengesPerfect} kusursuz maç",
            "📅 Streak ${g.dailyStreak} days (best ${g.dailyBest}) · ✅ ${g.challengesDone} challenges · 💯 ${g.challengesPerfect} perfect matches"),
        color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

/** Sezon önizlemesi: yorumcuların lig tahmini ve şampiyonluk ihtimalleri (sezonun ilk haftalarında). */
@Composable
fun SeasonPreviewCard(g: Game) {
    val rows = com.supermenajer.game.game.Preview.rows(g)
    if (rows.isEmpty()) return
    val n = rows.size
    DashCard(L("📺 Sezon Tahmini · ${com.supermenajer.game.model.Leagues.of(g.userTeam.division)?.name ?: ""}", "📺 Season Prediction · ${com.supermenajer.game.model.Leagues.of(g.userTeam.division)?.name ?: ""}"), Modifier.fillMaxWidth()) {
        Text(L("Yorumcuların güç sıralaması ve şampiyonluk ihtimalleri. Sezon sonunda tahminle karşılaştırılacak.", "The pundits' power ranking and title odds. It will be compared with the final table at the end of the season."), color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        val mineIdx = rows.indexOfFirst { it.teamId == g.userTeamId }
        val shown = (rows.take(5).map { it } + (if (mineIdx >= 5) listOf(rows[mineIdx]) else emptyList()) + rows.takeLast(3).filter { rows.indexOf(it) >= 5 && it.teamId != g.userTeamId }).distinct()
        var last = 0
        for (r in shown) {
            if (r.pred > last + 1) Text("⋯", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
            last = r.pred
            val t = g.team(r.teamId)
            val mine = r.teamId == g.userTeamId
            val zone = r.pred > n - 3
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (mine) Teal.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${r.pred}.", color = if (zone) Red else TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp))
                Text(t.name, fontSize = 12.sp, fontWeight = if (mine) FontWeight.Black else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (r.odds >= 1) {
                    Box(Modifier.width(70.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardLine)) {
                        Box(Modifier.fillMaxWidth((r.odds / 100f).coerceIn(0.03f, 1f)).height(6.dp).background(Gold))
                    }
                    Text("%${r.odds}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                } else if (zone) {
                    Text(L("küme hattı", "drop zone"), color = Red, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                } else Spacer(Modifier.width(110.dp))
            }
        }
    }
}
