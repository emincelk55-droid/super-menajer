package com.supermenajer.game

import com.supermenajer.game.game.Alias
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.NamePack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** İsim düzenleme ve isim paketleri. */
class NamePackTest {
    @Test
    fun renameExportImport() {
        val a = Generator.newWorld(Random(3))
        val t = a.divisionTeams(0)[0]
        val p = a.squad(t.id).first()
        val oldTeam = t.name
        val oldP = p.name
        // Kulüp: geçerli ad, kısa ad kontrolü, başka kulübün adı reddedilir
        assertNotNull(NamePack.renameTeam(a, t, "AB"))
        assertNotNull(NamePack.renameTeam(a, t, a.divisionTeams(0)[1].name))
        assertNotNull(NamePack.renameTeam(a, t, "Yeni Kulüp", "X1Y"))
        assertNull(NamePack.renameTeam(a, t, "Yıldız Kulübü", "YLD"))
        assertEquals("YLD", t.short)
        // Oyuncu: zincirlenen düzenleme tek kayıt olarak kalır
        assertNull(NamePack.renamePlayer(a, p, "Ara Ad"))
        assertNull(NamePack.renamePlayer(a, p, "Son Ad"))
        assertEquals(2, a.nameEdits.size)
        assertTrue(a.nameEdits.contains("O;$oldP;Son Ad"))
        assertTrue(a.nameEdits.contains("K;$oldTeam;Yıldız Kulübü;YLD"))
        // Aynı dünyanın temiz kopyasına paket uygulanır
        val pack = NamePack.export(a)
        val b = Generator.newWorld(Random(3))
        val r = NamePack.import(b, pack)
        assertEquals(1, r.teams)
        assertTrue(r.players >= 1)
        assertEquals("Yıldız Kulübü", b.team(t.id).name)
        assertEquals("Son Ad", b.player(p.id)!!.name)
        // İkinci kez uygulamak bir şey değiştirmez (eski adlar artık yok)
        val r2 = NamePack.import(b, pack)
        assertEquals(0, r2.teams)
        assertEquals("Yıldız Kulübü", b.team(t.id).name)
    }

    @Test
    fun realNamesMatchAliasedSave() {
        // Gerçek adlarla yazılmış paket, gerçeğe benzer adlı kayıtta da eşleşir
        val g = Generator.newWorld(Random(5))
        val p = g.players.first { it.teamId >= 0 }
        val real = "Zeki Testoğlu"
        p.name = Alias.person(real) // kayıtta "Zeki Testoğlu"nun gerçeğe benzer hâli
        val r = NamePack.import(g, "# test\nO;$real;$real\nO;Yok Böyle Biri;X Y\nbozuk satır\n")
        assertEquals(1, r.players)
        assertEquals(1, r.missing)
        assertEquals(1, r.errors)
        assertEquals(real, p.name)
    }

    @Test
    fun templateRoundTrip() {
        val g = Generator.newWorld(Random(7))
        val tpl = NamePack.template(g)
        assertTrue(tpl.lines().count { it.startsWith("O;") } > 1000)
        // Şablon değiştirilmeden yüklenirse hiçbir şey değişmez
        val before = g.players.map { it.name }
        val r = NamePack.import(g, tpl)
        assertEquals(0, r.players)
        assertEquals(before, g.players.map { it.name })
    }
}
