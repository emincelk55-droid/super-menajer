package com.supermenajer.game.game

import com.supermenajer.game.model.Game
import com.supermenajer.game.model.Player
import com.supermenajer.game.model.Pos
import com.supermenajer.game.model.Team
import kotlin.math.roundToInt
import kotlin.random.Random

object Generator {
    private val trFirst = listOf(
        "Ahmet", "Mehmet", "Mustafa", "Emre", "Burak", "Can", "Cenk", "Arda", "Kerem", "Ozan", "Selçuk", "Hakan",
        "Volkan", "Serkan", "Tolga", "Umut", "Yusuf", "Kaan", "Barış", "Berk", "Efe", "Eren", "Furkan", "Gökhan",
        "Halil", "İrfan", "İsmail", "Kenan", "Mert", "Murat", "Oğuz", "Okan", "Onur", "Orkun", "Ömer", "Rıdvan",
        "Sinan", "Taylan", "Uğur", "Yunus", "Zeki", "Alper", "Baran", "Batuhan", "Cengiz", "Deniz", "Doğukan",
        "Ferdi", "Hamza", "Ufuk", "Salih", "Sercan", "Tarık", "Yasin", "Enes", "Egemen", "Altay", "Çağlar",
        "Merih", "Abdullah", "Bora", "Emirhan", "Semih", "Kağan", "Tuna", "Yiğit", "Atakan", "Koray", "Erkan",
    )
    private val trLast = listOf(
        "Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir", "Arslan",
        "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek", "Polat", "Erdem", "Korkmaz",
        "Aksoy", "Güler", "Ünal", "Tekin", "Bulut", "Turan", "Akın", "Toprak", "Kaplan", "Bozkurt", "Uçar",
        "Ersoy", "Karaca", "Tosun", "Kahveci", "Akgün", "Sezer", "Ayhan", "Özer", "Duman", "Yavuz", "Keskin",
        "Gündoğdu", "Tufan", "Yazıcı", "Bayındır", "Karakaya", "Sarı", "Taş", "Ateş", "Erol", "Coşkun", "Işık",
        "Avcı", "Uysal", "Elmas", "Çınar", "Akbaş", "Soylu", "Altun", "Gürbüz", "Eren", "Tunç", "Baysal",
    )

    private class NameSet(val nation: String, val first: List<String>, val last: List<String>)

    private val foreignNames = listOf(
        NameSet("BR", listOf("Lucas", "Thiago", "Gabriel", "Rafael", "Bruno", "Diego", "Felipe", "Vinicius", "Matheus", "Igor", "Caio", "Renan"), listOf("Silva", "Santos", "Oliveira", "Souza", "Costa", "Pereira", "Lima", "Rocha", "Almeida", "Barbosa", "Ribeiro", "Moura")),
        NameSet("AR", listOf("Matías", "Facundo", "Lautaro", "Nicolás", "Julián", "Santiago", "Emiliano", "Gonzalo", "Valentín"), listOf("González", "Fernández", "Romero", "Álvarez", "Sosa", "Medina", "Acosta", "Benítez", "Ríos")),
        NameSet("SN", listOf("Moussa", "Ibrahima", "Cheikh", "Sadio", "Mamadou", "Pape", "Idrissa", "Lamine"), listOf("Diallo", "Ndiaye", "Sow", "Diop", "Sarr", "Gueye", "Faye", "Cissé")),
        NameSet("NG", listOf("Samuel", "Victor", "Emmanuel", "Chidi", "Kelechi", "Ahmed", "Tunde", "Femi"), listOf("Okafor", "Mensah", "Adeyemi", "Okonkwo", "Eze", "Balogun", "Nwosu", "Ige")),
        NameSet("HR", listOf("Luka", "Marko", "Ivan", "Mateo", "Josip", "Ante", "Domagoj", "Filip"), listOf("Horvat", "Kovač", "Babić", "Marić", "Jurić", "Perić", "Vuković", "Knežević")),
        NameSet("RS", listOf("Nikola", "Dušan", "Stefan", "Miloš", "Filip", "Lazar", "Uroš"), listOf("Petrović", "Jovanović", "Nikolić", "Ilić", "Lazić", "Stojanović", "Pavlović")),
        NameSet("DK", listOf("Jonas", "Erik", "Mikkel", "Rasmus", "Andreas", "Mads", "Kasper"), listOf("Jensen", "Larsen", "Nielsen", "Hansen", "Møller", "Pedersen", "Kristensen")),
        NameSet("PT", listOf("João", "Rúben", "André", "Gonçalo", "Nuno", "Diogo", "Tiago", "Rafael", "Pedro", "Bernardo", "Vitinha", "Hugo"), listOf("Ferreira", "Gomes", "Carvalho", "Mendes", "Pinto", "Silva", "Sousa", "Martins", "Fonseca", "Teixeira", "Lopes", "Moreira")),
        NameSet("FR", listOf("Hugo", "Théo", "Lucas", "Mathis", "Yanis", "Antoine", "Kylian", "Adrien", "Bastien", "Clément", "Enzo", "Maxime", "Noah", "Rayan"), listOf("Martin", "Bernard", "Dubois", "Moreau", "Laurent", "Lefèvre", "Girard", "Roux", "Fournier", "Mercier", "Blanc", "Garnier", "Faure", "Chevalier")),
        NameSet("ES", listOf("Pablo", "Sergio", "Álvaro", "Iker", "Javier", "Raúl", "Marcos", "Adrián", "Dani", "Hugo", "Rodrigo", "Unai", "Mikel", "Carlos", "Jorge"), listOf("García", "Martínez", "López", "Sánchez", "Ruiz", "Navarro", "Moreno", "Jiménez", "Romero", "Torres", "Ortega", "Delgado", "Castro", "Vidal", "Herrera")),
        NameSet("IT", listOf("Marco", "Lorenzo", "Federico", "Matteo", "Andrea", "Davide", "Alessandro", "Simone", "Giacomo", "Riccardo", "Nicolò", "Tommaso", "Filippo", "Gianluca"), listOf("Rossi", "Bianchi", "Esposito", "Ricci", "Romano", "Conti", "Colombo", "Marino", "Greco", "Bruno", "Gallo", "Costa", "Fontana", "Moretti", "Barbieri")),
        NameSet("DE", listOf("Leon", "Jonas", "Lukas", "Felix", "Niklas", "Tim", "Julian", "Florian", "Kai", "Max", "Moritz", "Paul", "Jan", "Malte", "Timo"), listOf("Müller", "Schmidt", "Weber", "Wagner", "Becker", "Hoffmann", "Schulz", "Koch", "Richter", "Klein", "Wolf", "Neumann", "Schwarz", "Krüger", "Hartmann")),
        NameSet("GB", listOf("Harry", "Jack", "James", "Oliver", "Mason", "Callum", "Jordan", "Reece", "Kyle", "Declan", "Connor", "Ben", "Luke", "Aaron", "Jamie"), listOf("Smith", "Walker", "Taylor", "Wright", "Hughes", "Clarke", "Johnson", "Brown", "Evans", "Wilson", "Thompson", "White", "Robinson", "Hall", "Green")),
        NameSet("NL", listOf("Daan", "Sem", "Bram", "Luuk", "Thijs", "Jesse", "Stijn", "Ruben", "Bart", "Niels", "Joost"), listOf("de Jong", "Bakker", "Visser", "van Dijk", "Smit", "Mulder", "de Vries", "Meijer", "Bos", "Vos", "Peters")),
    )
    private val nameSetByNation = foreignNames.associateBy { it.nation }

    class TeamDef(val name: String, val short: String, val city: String, val c1: Long, val c2: Long, val rep: Int)

    val teamDefs = listOf(
        TeamDef("İstanbul Yıldızspor", "IYS", "İstanbul", 0xFFC8102E, 0xFFFDB913, 88),
        TeamDef("Boğaziçi FK", "BOĞ", "İstanbul", 0xFF0B1F4D, 0xFFFFD100, 86),
        TeamDef("Haliç Gücü", "HAL", "İstanbul", 0xFF111111, 0xFFFFFFFF, 84),
        TeamDef("Karadeniz Fırtınası", "KFR", "Trabzon", 0xFF7A1E3A, 0xFF5BC2E7, 80),
        TeamDef("Ankara Başkent SK", "ANK", "Ankara", 0xFF1E5AA8, 0xFFFFFFFF, 73),
        TeamDef("İzmir Körfezspor", "İZK", "İzmir", 0xFF00897B, 0xFFFFFFFF, 71),
        TeamDef("Bursa Yeşilvadi", "BUR", "Bursa", 0xFF2E7D32, 0xFFFFFFFF, 70),
        TeamDef("Antalya Akdeniz FK", "ANT", "Antalya", 0xFFE53935, 0xFFFFFFFF, 68),
        TeamDef("Konya Selçukspor", "KON", "Konya", 0xFF43A047, 0xFF111111, 66),
        TeamDef("Adana Toroslar", "ADA", "Adana", 0xFFFB8C00, 0xFF1565C0, 65),
        TeamDef("Kayseri Erciyes Gençlik", "KAY", "Kayseri", 0xFFFDD835, 0xFFC62828, 63),
        TeamDef("Samsun Atakum SK", "SAM", "Samsun", 0xFFD32F2F, 0xFFFFFFFF, 62),
        TeamDef("Gaziantep Şahinbey FK", "GAZ", "Gaziantep", 0xFF8E1B1B, 0xFF212121, 61),
        TeamDef("Eskişehir Porsukspor", "ESK", "Eskişehir", 0xFF212121, 0xFFE53935, 60),
        TeamDef("Sivas Kızılırmak", "SİV", "Sivas", 0xFFB71C1C, 0xFFF5F5F5, 59),
        TeamDef("Diyarbakır Dicle SK", "DİY", "Diyarbakır", 0xFF388E3C, 0xFFD32F2F, 58),
        TeamDef("Rize Çay Gücü", "RİZ", "Rize", 0xFF1B5E20, 0xFF4FC3F7, 57),
        TeamDef("Malatya Kayısıspor", "MAL", "Malatya", 0xFFFFB300, 0xFF212121, 56),
    )

    val lowerDefs = listOf(
        TeamDef("Kocaeli Körfez Gücü", "KOC", "Kocaeli", 0xFF2E7D32, 0xFF111111, 55),
        TeamDef("Manisa Lale SK", "MAN", "Manisa", 0xFFE91E63, 0xFFFFFFFF, 54),
        TeamDef("Denizli Horozspor", "DEN", "Denizli", 0xFF1565C0, 0xFF111111, 53),
        TeamDef("Mersin Liman FK", "MER", "Mersin", 0xFF0277BD, 0xFFFFEB3B, 52),
        TeamDef("Hatay Defnespor", "HAT", "Hatay", 0xFF880E4F, 0xFFFFFFFF, 51),
        TeamDef("Erzurum Palandöken", "ERZ", "Erzurum", 0xFF0D47A1, 0xFFFFFFFF, 50),
        TeamDef("Aydın Efespor", "AYD", "Aydın", 0xFFF57C00, 0xFF111111, 49),
        TeamDef("Balıkesir Kazdağı", "BAL", "Balıkesir", 0xFFC62828, 0xFF111111, 48),
        TeamDef("Tekirdağ Trakya FK", "TEK", "Tekirdağ", 0xFF00838F, 0xFFFFFFFF, 47),
        TeamDef("Ordu Fındıkspor", "ORD", "Ordu", 0xFF5D4037, 0xFFFFC107, 46),
        TeamDef("Bolu Abant SK", "BOL", "Bolu", 0xFF558B2F, 0xFFFFFFFF, 45),
        TeamDef("Van Gölü SK", "VAN", "Van", 0xFF1E88E5, 0xFFFFEB3B, 44),
        TeamDef("Şanlıurfa Balıklıgöl", "URF", "Şanlıurfa", 0xFF43A047, 0xFFFFEB3B, 43),
        TeamDef("Çorum Hititspor", "ÇOR", "Çorum", 0xFFB71C1C, 0xFF111111, 42),
        TeamDef("Zonguldak Kömürspor", "ZON", "Zonguldak", 0xFF263238, 0xFFD32F2F, 41),
        TeamDef("Edirne Selimiye FK", "EDİ", "Edirne", 0xFF6A1B9A, 0xFFFFFFFF, 40),
        TeamDef("Afyon Termalspor", "AFY", "Afyon", 0xFF283593, 0xFFE53935, 39),
        TeamDef("Uşak Ulubey SK", "UŞA", "Uşak", 0xFF9E9D24, 0xFF111111, 38),
    )

    /** Pozisyon başına kadro planı. */
    val squadPlan = linkedMapOf(Pos.GK to 3, Pos.DF to 8, Pos.MF to 8, Pos.FW to 5)
    private val starters = mapOf(Pos.GK to 1, Pos.DF to 4, Pos.MF to 4, Pos.FW to 2)

    fun teamLevel(rep: Int): Double = 40 + rep * 0.4

    fun newWorld(rnd: Random = Random.Default): Game {
        val teams = teamDefs.mapIndexed { i, d ->
            Team(i, d.name, d.short, d.city, d.c1, d.c2, d.rep, budgetFor(d.rep))
        }.toMutableList()
        val g = Game(
            managerName = "", userTeamId = -1, teams = teams, players = mutableListOf(),
            fixtures = mutableListOf(), news = mutableListOf(), nextPlayerId = 1, saveVersion = 11,
        )
        for (t in teams) fillSquad(g, t, rnd, "TR")
        addLowerDivision(g, rnd)
        WorldGen.addForeignLeagues(g, rnd)
        topUpFreeAgents(g, rnd)
        return g
    }

    /** 1. Lig takımlarını ve kadrolarını ekler (eski kayıtların taşınmasında da kullanılır). */
    fun addLowerDivision(g: Game, rnd: Random) {
        val base = (g.teams.maxOfOrNull { it.id } ?: -1) + 1
        lowerDefs.forEachIndexed { i, d ->
            val t = Team(base + i, d.name, d.short, d.city, d.c1, d.c2, d.rep, budgetFor(d.rep), division = 1)
            g.addTeam(t)
            fillSquad(g, t, rnd, "TR")
        }
    }

    fun budgetFor(rep: Int): Long = ((rep - 45) * (rep - 45) * 15_000L).coerceAtLeast(1_500_000L)

    /** Takımın ülkesine göre baskın uyruk. */
    fun nationOf(g: Game, t: Team): String = g.flagOf(t)

    fun fillSquad(g: Game, t: Team, rnd: Random, nation: String) {
        val lvl = teamLevel(t.reputation)
        for ((pos, n) in squadPlan) for (i in 0 until n) {
            val target: Double
            val age: Int
            when {
                i < starters.getValue(pos) -> { target = lvl + rnd.nextInt(-3, 5); age = rnd.nextInt(22, 33) }
                i == n - 1 && pos != Pos.GK -> { target = lvl - rnd.nextInt(9, 16); age = rnd.nextInt(16, 20) }
                else -> { target = lvl - rnd.nextInt(2, 9); age = rnd.nextInt(19, 34) }
            }
            val pl = makePlayer(g.nextPlayerId++, pos, target.roundToInt(), age, t.id, rnd, nation)
            // Dünyaya dağılmış gizli wonderkid'ler
            if (age <= 19 && rnd.nextDouble() < 0.06) pl.potential = rnd.nextInt(83, 95)
            g.addPlayer(pl)
        }
    }

    fun topUpFreeAgents(g: Game, rnd: Random) {
        val pos = Pos.entries
        val nations = listOf("TR", "TR", "GB", "ES", "DE", "IT", "FR", "PT", "NL", "BR")
        var free = g.squad(-1).size
        while (free < 60) {
            g.addPlayer(makePlayer(g.nextPlayerId++, pos[rnd.nextInt(pos.size)], rnd.nextInt(50, 72), rnd.nextInt(19, 35), -1, rnd, nations.random(rnd)))
            free++
        }
    }

    fun youth(g: Game, t: Team, pos: Pos, rnd: Random): Player {
        Facilities.ensure(t)
        val bonus = Facilities.youthBonus(t)
        val target = (teamLevel(t.reputation) - rnd.nextInt(10, 20) + bonus).roundToInt()
        val p = makePlayer(g.nextPlayerId++, pos, target, rnd.nextInt(17, 19), t.id, rnd, nationOf(g, t))
        // Potansiyel takım seviyesinin etrafında; çok yüksek potansiyel nadir.
        val pot = teamLevel(t.reputation) - 3 + bonus + gauss(rnd) * 6
        p.potential = pot.roundToInt().coerceIn(p.overall + 4, 91)
        // Nadir "altın çocuk": küçük kulüpte bile dünya yıldızı potansiyeli
        if (rnd.nextDouble() < Facilities.wonderkidChance(t)) p.potential = rnd.nextInt(84, 95)
        p.contractYears = 3
        p.salary = p.demandWage
        return p
    }

    private val offsets = mapOf(
        Pos.GK to intArrayOf(-22, -35, -12, -28, -25, -6),
        Pos.DF to intArrayOf(-4, -22, -8, -12, 4, 3),
        Pos.MF to intArrayOf(-3, -8, 4, 1, -8, -4),
        Pos.FW to intArrayOf(3, 4, -10, 2, -35, -3),
    )

    fun makePlayer(id: Int, pos: Pos, target: Int, age: Int, teamId: Int, rnd: Random, nation: String = "TR"): Player {
        val o = offsets.getValue(pos)
        fun a(i: Int) = (target + o[i] + rnd.nextInt(-6, 7)).coerceIn(15, 95)
        val gk = if (pos == Pos.GK) (target + rnd.nextInt(-2, 4)).coerceIn(30, 95) else rnd.nextInt(8, 22)
        val (name, nat) = randomName(rnd, nation)
        val p = Player(id, name, age, nat, pos, a(0), a(1), a(2), a(3), a(4), a(5), gk, 0, teamId)
        repeat(4) {
            val diff = target - p.overall
            if (diff != 0) shift(p, diff)
        }
        p.potential = when {
            age <= 20 -> p.overall + rnd.nextInt(4, 19)
            age <= 23 -> p.overall + rnd.nextInt(2, 10)
            age <= 26 -> p.overall + rnd.nextInt(0, 5)
            else -> p.overall
        }.coerceIn(p.overall, 92)
        if (teamId >= 0) {
            p.contractYears = rnd.nextInt(1, 6)
            p.salary = p.demandWage
        }
        p.morale = rnd.nextInt(60, 80).toDouble()
        return p
    }

    private fun shift(p: Player, d: Int) {
        if (p.pos == Pos.GK) {
            p.goalkeeping = (p.goalkeeping + d).coerceIn(15, 97)
        } else {
            p.pace = (p.pace + d).coerceIn(15, 97)
            p.shooting = (p.shooting + d).coerceIn(15, 97)
            p.passing = (p.passing + d).coerceIn(15, 97)
            p.dribbling = (p.dribbling + d).coerceIn(15, 97)
            p.defending = (p.defending + d).coerceIn(15, 97)
            p.physical = (p.physical + d).coerceIn(15, 97)
        }
    }

    /** Teknik ekip vb. için isim üretir: (isim, uyruk). */
    fun personName(rnd: Random, nation: String): Pair<String, String> = randomName(rnd, nation)

    /** Baskın uyruktan isim (yerli oranı %70-78), kalanı karışık yabancı. */
    private fun randomName(rnd: Random, nation: String): Pair<String, String> {
        val r = rnd.nextDouble()
        if (nation == "TR" && r < 0.78) return "${trFirst.random(rnd)} ${trLast.random(rnd)}" to "TR"
        val local = nameSetByNation[nation]
        if (local != null && r < 0.7) return "${local.first.random(rnd)} ${local.last.random(rnd)}" to nation
        if (nation != "TR" && r < 0.74) return "${trFirst.random(rnd)} ${trLast.random(rnd)}" to "TR"
        val s = foreignNames.random(rnd)
        return "${s.first.random(rnd)} ${s.last.random(rnd)}" to s.nation
    }

    /** Standart normal dağılım (Box-Muller). */
    fun gauss(rnd: Random): Double {
        val u = rnd.nextDouble().coerceAtLeast(1e-9)
        val v = rnd.nextDouble()
        return kotlin.math.sqrt(-2 * kotlin.math.ln(u)) * kotlin.math.cos(2 * Math.PI * v)
    }

    fun flag(code: String): String =
        code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}
