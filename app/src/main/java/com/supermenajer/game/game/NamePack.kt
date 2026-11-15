package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Team

/**
 * İsim düzenleme ve isim paketleri (PES "option file" mantığı): oyuncu her oyuncu ve kulübün adını
 * değiştirebilir, düzenlemeler bir metin dosyası olarak paylaşılır ve başka kayıtlara uygulanır.
 *
 * Dosya biçimi (UTF-8, satır başına bir kayıt; # ile başlayan satırlar yorum):
 *   K;Galatasarey;Galatasaray;GS      kulüp: eski ad; yeni ad; (isteğe bağlı) 3 harfli kısa ad
 *   O;Arda Gülar;Arda Güler           oyuncu: eski ad; yeni ad
 * Eşleştirme kayıttaki görünen adla yapılır; bulunamazsa eski ad "gerçeğe benzer" biçime çevrilip
 * yeniden denenir (gerçek adlarla yazılmış paketler de gerçeğe benzer kayıtlarda çalışır).
 */
object NamePack {
    const val HEADER = "# Süper Menajer isim paketi v1"

    private fun clean(s: String) = s.replace(";", " ").replace("\n", " ").replace("\r", " ").trim()

    private fun record(g: Game, kind: Char, old: String, new: String, short: String = "") {
        // Aynı hedefin önceki düzenlemesi zincirlenir: A→B, sonra B→C ise tek kayıt A→C kalır
        val prev = g.nameEdits.indexOfFirst { it.startsWith("$kind;") && it.split(";").getOrNull(2) == old }
        val origin = if (prev >= 0) g.nameEdits[prev].split(";")[1] else old
        if (prev >= 0) g.nameEdits.removeAt(prev)
        if (origin != new) g.nameEdits += listOf(kind.toString(), origin, new, short).filterIndexed { i, s -> i < 3 || s.isNotEmpty() }.joinToString(";")
    }

    /** Oyuncunun adını değiştirir; hata metni ya da null döner. */
    fun renamePlayer(g: Game, p: Player, name: String): String? {
        val n = clean(name).replace(Regex("\\s+"), " ")
        if (n.length < 2) return L("Ad en az 2 harf olmalı.", "The name must be at least 2 letters.")
        if (n.length > 32) return L("Ad en fazla 32 harf olabilir.", "The name can be at most 32 letters.")
        if (n == p.name) return null
        record(g, 'O', p.name, n)
        p.name = n
        return null
    }

    /** Kulübün adını (ve isteğe bağlı kısa adını) değiştirir; hata metni ya da null döner. */
    fun renameTeam(g: Game, t: Team, name: String, short: String = ""): String? {
        val n = clean(name).replace(Regex("\\s+"), " ")
        val s = clean(short).uppercase()
        if (n.length < 3) return L("Kulüp adı en az 3 harf olmalı.", "The club name must be at least 3 letters.")
        if (n.length > 32) return L("Kulüp adı en fazla 32 harf olabilir.", "The club name can be at most 32 letters.")
        if (s.isNotEmpty() && (s.length != 3 || !s.all { it.isLetter() })) return L("Kısa ad 3 harf olmalı.", "The short name must be 3 letters.")
        if (g.teams.any { it.id != t.id && (it.name.equals(n, ignoreCase = true) || (s.isNotEmpty() && it.short == s)) }) {
            return L("Bu ad ya da kısa ad başka bir kulübün.", "That name or short name belongs to another club.")
        }
        if (n == t.name && (s.isEmpty() || s == t.short)) return null
        record(g, 'K', t.name, n, s)
        t.name = n
        if (s.isNotEmpty()) t.short = s
        return null
    }

    /** Bu kayıtta yapılan düzenlemeler (paylaşılacak paket). */
    fun export(g: Game): String = buildString {
        appendLine(HEADER)
        appendLine("# ${g.nameEdits.size} " + L("düzenleme", "edits"))
        g.nameEdits.forEach { appendLine(it) }
    }

    /** Tüm kulüp ve oyuncu adlarının şablonu: metin düzenleyicide yeni adları yazıp içe aktarmak için. */
    fun template(g: Game): String = buildString {
        appendLine(HEADER)
        appendLine(L("# Şablon: her satırda ikinci ad yeni addır. Değiştirmek istemediğin satırları olduğu gibi bırak ya da sil.",
            "# Template: the second name on each line is the new name. Leave or delete lines you don't want to change."))
        val teams = g.teams.filter { g.squad(it.id).isNotEmpty() }.sortedWith(compareBy({ it.division }, { it.name }))
        for (t in teams) {
            appendLine()
            appendLine("# ${t.name} (${g.leagueName(t)})")
            appendLine("K;${t.name};${t.name};${t.short}")
            g.squad(t.id).sortedByDescending { it.overall }.forEach { appendLine("O;${it.name};${it.name}") }
        }
    }

    class Result(val teams: Int, val players: Int, val missing: Int, val errors: Int)

    /** Paketi uygular. Aynı satır iki kez uygulansa da sonuç değişmez. */
    fun import(g: Game, text: String): Result {
        var teams = 0; var players = 0; var missing = 0; var errors = 0
        val byTeam = g.teams.associateBy { it.name }.toMutableMap()
        val byPlayer = HashMap<String, MutableList<Player>>()
        g.players.forEach { byPlayer.getOrPut(it.name) { mutableListOf() } += it }
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val c = line.split(";").map { it.trim() }
            if (c.size < 3) { errors++; continue }
            val (old, new) = c[1] to c[2]
            when (c[0].uppercase()) {
                "K", "T", "C" -> {
                    val t = byTeam[old] ?: byTeam[Alias.club(old)]
                    if (t == null) { missing++; continue }
                    val before = t.name to t.short
                    if (renameTeam(g, t, new, c.getOrNull(3) ?: "") == null) {
                        if (before != (t.name to t.short)) { byTeam.remove(before.first); byTeam[t.name] = t; teams++ }
                    } else errors++
                }
                "O", "P" -> {
                    val list = byPlayer[old] ?: byPlayer[Alias.person(old)]
                    if (list.isNullOrEmpty()) { missing++; continue }
                    // Aynı adlı birden çok oyuncu varsa hepsi değişir (paket o adı hedefliyor)
                    for (p in list.toList()) {
                        val before = p.name
                        if (renamePlayer(g, p, new) == null) { if (p.name != before) players++ } else errors++
                    }
                }
                else -> errors++
            }
        }
        return Result(teams, players, missing, errors)
    }
}
