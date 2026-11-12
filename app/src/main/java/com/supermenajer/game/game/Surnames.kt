package com.supermenajer.game.game

/**
 * Uydurma oyuncu adları için ülkelere göre sıradan, yaygın soyadları. Bilinçli olarak tanınmış
 * futbolcu soyadları yok: üretilen (altyapı, kurgusal dünya, yeni ligler) oyuncular gerçek bir
 * yıldızı çağrıştırmasın diye. [Names] havuzlarındaki gerçek soyadlarının yerine kullanılır.
 */
object Surnames {
    private fun l(s: String) = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val byNation: Map<String, List<String>> = mapOf(
        "TR" to l("Yılmaz, Kaya, Demir, Şahin, Çelik, Yıldız, Yıldırım, Öztürk, Aydın, Özdemir, Arslan, Doğan, Kılıç, Aslan, Çetin, Kara, Koç, Kurt, Özkan, Şimşek, Polat, Erdem, Korkmaz, Aksoy, Güler, Ünal, Tekin, Bulut, Turan, Akın, Toprak, Kaplan, Bozkurt, Uçar, Ersoy, Karaca, Tosun, Akgün, Sezer, Özer, Duman, Yavuz, Keskin, Tufan, Karakaya, Sarı, Taş, Ateş, Erol, Coşkun, Işık, Avcı, Uysal, Çınar, Altun, Eren, Tunç, Özcan, Bayram, Kutlu, Akgül, Erkin, Sağlam, Dursun, Karadeniz, Zengin, Sevim, Cömert, Sönmez, Uzun, Okay, Gönül, Bolat, Akbulut, Ekinci, Güneş, Kalkan, Karataş, Özbek, Temiz, Yücel, Aktaş, Bilgin, Çakmak, Durmaz, Gezer, Kocaman, Oral, Seven, Tatlı, Yalçın"),
        "GB" to l("Smith, Taylor, Wright, Hughes, Clarke, Johnson, Brown, Evans, Wilson, Thompson, White, Robinson, Hall, Green, Wood, Barnes, Ward, Cooper, Harrison, Lewis, Morris, King, Baker, Carter, Mitchell, Phillips, Parker, Bennett, Lowe, Stewart, Murray, Campbell, Allen, Young, Scott, Adams, Hill, Moore, Jackson, Collins, Edwards, Price, Bell, Fletcher, Marshall, Holmes, Fox, Pearson, Lloyd, Shepherd, Gibson, Dixon, Kelly, Webb"),
        "ES" to l("García, Martínez, López, Sánchez, Ruiz, Navarro, Moreno, Jiménez, Romero, Torres, Ortega, Delgado, Castro, Herrera, Muñoz, Rubio, Vega, Molina, Gil, Serrano, Blanco, Medina, Domínguez, Vázquez, Ramírez, Flores, Prieto, Cano, Méndez, Peña, Cabrera, Calvo, Campos, Fuentes, Carrasco, Caballero, Nieto, Pascual, Santana, Lozano, Hidalgo, Márquez, Ibáñez, Ferrer"),
        "DE" to l("Müller, Schmidt, Weber, Wagner, Becker, Hoffmann, Schulz, Koch, Richter, Klein, Wolf, Neumann, Schwarz, Krüger, Hartmann, Zimmermann, Braun, Hofer, Fischer, Meyer, Schäfer, Bauer, Lehmann, Walter, Peters, König, Huber, Kaiser, Fuchs, Lange, Werner, Schmitt, Krause, Maier, Köhler, Herrmann, Vogel, Friedrich, Günther, Keller, Frank, Berger, Roth, Beck, Lorenz, Seidel"),
        "IT" to l("Rossi, Bianchi, Esposito, Ricci, Romano, Colombo, Marino, Greco, Bruno, Gallo, Costa, Fontana, Moretti, Barbieri, Lombardi, Giordano, Rinaldi, Caruso, Ferrara, Galli, Martini, Leone, Longo, Gentile, Martinelli, Vitale, Serra, Coppola, De Santis, D'Angelo, Marchetti, Parisi, Villa, Cattaneo, Fabbri, Bianco, Messina, Sala, Pellegrino, Palumbo, Sanna, Farina, Rizzo, Monti"),
        "FR" to l("Martin, Bernard, Dubois, Moreau, Laurent, Lefèvre, Girard, Roux, Fournier, Mercier, Blanc, Garnier, Faure, Chevalier, Lacroix, Samba, Bonnet, Leroy, Durand, Petit, Robert, Richard, Simon, Michel, Lefebvre, Rousseau, Vincent, Muller, André, Fontaine, Gauthier, Perrin, Morin, Clément, Gaillard, Barbier, Arnaud, Rolland, Picard, Renaud, Marchand, Aubert, Lemoine, Boyer, Duval, Brun, Dumont, Meunier"),
        "PT" to l("Ferreira, Gomes, Carvalho, Pinto, Sousa, Martins, Fonseca, Teixeira, Lopes, Moreira, Nunes, Ramos, Coelho, Marques, Cardoso, Almeida, Santos, Pereira, Rodrigues, Monteiro, Correia, Oliveira, Ribeiro, Barbosa, Rocha, Pires, Azevedo, Cunha, Tavares, Matos, Freitas, Magalhães, Batista, Morais, Sá, Antunes, Faria, Lourenço, Baptista, Simões"),
        "NL" to l("Bakker, Visser, Smit, Mulder, Meijer, Bos, Vos, Peters, Janssen, Hendriks, Dekker, de Boer, van den Berg, Dijkstra, Jacobs, van Leeuwen, Brouwer, de Graaf, van der Meer, Kok, Willems, Schouten, van Wijk, Hoekstra, Koster, Prins, Huisman, Kuipers, Postma, Veenstra, Wouters, Vermeulen, de Haan, van Dam, Boer, Groen, Verhoeven, Kramer, Timmermans"),
        "BR" to l("Silva, Santos, Oliveira, Souza, Costa, Pereira, Lima, Rocha, Almeida, Ribeiro, Nascimento, Carvalho, Ferreira, Araújo, Batista, Teixeira, Cardoso, Freitas, Gomes, Martins, Barros, Pinto, Moraes, Correia, Cavalcanti, Dias, Castro, Campos, Monteiro, Mendonça, Rezende, Duarte, Fonseca, Azevedo, Brito, Vasconcelos, Siqueira, Andrade"),
        "AR" to l("González, Fernández, Rodríguez, Gómez, Pérez, Sosa, Medina, Acosta, Benítez, Ríos, Herrera, Aguirre, Giménez, Ruiz, Torres, Flores, Ledesma, Vera, Ojeda, Cabrera, Figueroa, Godoy, Luna, Ponce, Quiroga, Villalba, Ferreyra, Juárez, Maidana, Navarro, Peralta, Rojas, Sánchez, Vega, Castillo, Bustos"),
        "SN" to l("Diallo, Ndiaye, Sow, Diop, Sarr, Gueye, Faye, Cissé, Dia, Camara, Seck, Ba, Fall, Dieng, Mbaye, Thiam, Niang, Ndao, Sy, Kane, Toure, Sall, Lo, Diouf, Wade, Gaye, Badji, Sène, Tall, Sène"),
        "NG" to l("Okafor, Adeyemi, Okonkwo, Balogun, Nwosu, Ige, Ajayi, Bassey, Onyeka, Okoro, Eze, Obi, Nwachukwu, Adebayo, Ogunleye, Chukwu, Okeke, Udoh, Effiong, Abiodun, Oladipo, Akinola, Babatunde, Olawale, Ibekwe, Nnamdi"),
        "CI" to l("Koné, Traoré, Bamba, Yao, Kouamé, Konan, Diallo, Koffi, Kouassi, N'Guessan, Ouattara, Coulibaly, Doumbia, Diomandé, Soro, Brou, Gnagne, Kra, Tanoh, Yapi, Aka, Ehui"),
        "GH" to l("Owusu, Mensah, Asante, Boateng, Acheampong, Amoah, Appiah, Adjei, Agyeman, Bonsu, Darko, Frimpong, Gyamfi, Kyei, Nkrumah, Ofori, Opoku, Osei, Sarpong, Tetteh, Quaye, Annan"),
        "CM" to l("Mbarga, Nkoulou, Fouda, Atangana, Essomba, Manga, Ndongo, Owona, Ekotto, Biyik, Tchami, Abega, Mbida, Nana, Kamga, Ngono, Tagne, Zambo"),
        "MA" to l("Alaoui, Bennani, El Amrani, Benjelloun, Tazi, Berrada, El Idrissi, Chraibi, Lahlou, Ouazzani, Bouzidi, El Fassi, Hajji, Belkadi, Naciri, Saidi, Zerouali, Amrani"),
        "DZ" to l("Benali, Haddad, Boudiaf, Mansouri, Rahmani, Belkacem, Cherif, Hamidi, Saadi, Meziane, Bouzid, Larbi, Khelifi, Ouali, Taleb, Ziani, Djebbar, Amrouche"),
        "HR" to l("Horvat, Kovač, Babić, Marić, Jurić, Perić, Vuković, Knežević, Matić, Tomić, Božić, Novak, Pavić, Blažević, Grgić, Šarić, Lovrić, Bošnjak, Mikulić, Radić, Kolar, Barišić"),
        "RS" to l("Petrović, Jovanović, Nikolić, Ilić, Stojanović, Pavlović, Marković, Đorđević, Stanković, Milošević, Popović, Todorović, Simić, Kostić, Radović, Obradović, Vasić, Stefanović, Mihajlović, Pešić, Ristić, Zorić"),
        "DK" to l("Jensen, Larsen, Nielsen, Hansen, Møller, Pedersen, Kristensen, Christensen, Andersen, Poulsen, Rasmussen, Sørensen, Thomsen, Madsen, Jørgensen, Petersen, Johansen, Mortensen, Knudsen, Holm, Lund, Bach"),
        "SE" to l("Karlsson, Olsen, Johansson, Andersson, Nilsson, Larsson, Gustafsson, Holm, Eriksson, Persson, Svensson, Jonsson, Pettersson, Lindberg, Lindqvist, Magnusson, Axelsson, Berglund, Sandberg, Lundgren, Engström, Hedlund"),
        "NO" to l("Hansen, Johansen, Olsen, Larsen, Andersen, Pedersen, Nilsen, Kristiansen, Jensen, Karlsen, Johnsen, Pettersen, Eriksen, Berg, Haugen, Hagen, Johannessen, Andreassen, Jacobsen, Dahl, Lie, Lund, Moen, Solberg, Bakken"),
        "PL" to l("Nowak, Kowalski, Wiśniewski, Wójcik, Kamiński, Kowalczyk, Zieliński, Szymański, Woźniak, Dąbrowski, Kozłowski, Jankowski, Mazur, Kwiatkowski, Krawczyk, Piotrowski, Grabowski, Pawłowski, Michalski, Król, Wieczorek, Jabłoński"),
        "BE" to l("Peeters, Janssens, Maes, Jacobs, Mertens, Willems, Claes, Goossens, Wouters, De Smet, Dubois, Lambert, Dupont, Martens, Vermeulen, Pauwels, Hermans, Aerts, Desmet, Michiels, Leclercq, Renard, Smets, Declercq"),
        "CO" to l("Rodríguez, Gómez, González, Martínez, García, López, Hernández, Sánchez, Ramírez, Pérez, Díaz, Moreno, Rojas, Vargas, Castro, Ortiz, Suárez, Jiménez, Gutiérrez, Ruiz, Mosquera, Valencia, Palacios, Cuesta"),
        "UY" to l("Rodríguez, González, Fernández, Pérez, Martínez, García, López, Sosa, Silva, Pereira, Méndez, Díaz, Álvarez, Romero, Acosta, Olivera, Castro, Benítez, Correa, Cabrera, Píriz, Techera"),
        "US" to l("Johnson, Williams, Brown, Jones, Miller, Davis, Wilson, Anderson, Taylor, Thomas, Moore, Martin, Jackson, Thompson, White, Harris, Clark, Lewis, Walker, Hall, Young, Allen, Nelson, Hill, Baker, Carter, Mitchell, Roberts"),
        "JP" to l("Sato, Suzuki, Takahashi, Tanaka, Watanabe, Ito, Yamamoto, Nakamura, Kobayashi, Kato, Yoshida, Yamada, Sasaki, Yamaguchi, Matsumoto, Inoue, Kimura, Hayashi, Shimizu, Yamazaki, Mori, Ikeda, Hashimoto, Ishikawa"),
        "KR" to l("Kim, Lee, Park, Choi, Jung, Kang, Cho, Yoon, Jang, Lim, Han, Oh, Seo, Shin, Kwon, Hwang, Ahn, Song, Yoo, Hong, Jeon, Ko, Moon, Yang"),
        "AT" to l("Gruber, Huber, Bauer, Wagner, Müller, Pichler, Steiner, Moser, Mayer, Hofer, Leitner, Berger, Fuchs, Eder, Fischer, Schmid, Winkler, Weber, Schwarz, Maier, Schneider, Reiter, Mayr, Brunner, Lang, Baumgartner, Auer"),
        "CH" to l("Müller, Meier, Schmid, Keller, Weber, Huber, Meyer, Schneider, Steiner, Fischer, Gerber, Brunner, Baumann, Frei, Zimmermann, Moser, Widmer, Wyss, Graf, Roth, Suter, Bachmann, Kunz, Bühler"),
        "GR" to l("Papadopoulos, Papadakis, Georgiou, Nikolaou, Ioannou, Dimitriou, Konstantinou, Christodoulou, Vasileiou, Athanasiou, Pappas, Karagiannis, Oikonomou, Makris, Antoniou, Alexiou, Petridis, Theodorou, Michailidis, Stavrou"),
        "UA" to l("Melnyk, Shevchuk, Boyko, Kovalenko, Bondarenko, Tkachenko, Kovalchuk, Kravchenko, Oliynyk, Shevchenko, Koval, Polishchuk, Bondar, Tkachuk, Moroz, Marchenko, Lysenko, Rudenko, Savchenko, Petrenko, Klymenko, Pavlenko"),
        "BA" to l("Hodžić, Hadžić, Čengić, Delić, Demirović, Begić, Mehmedović, Kovačević, Salihović, Hasanović, Ibrahimović, Alić, Mujić, Omerović, Smajić, Husić, Zukić, Karić"),
        "AL" to l("Hoxha, Shehu, Dervishi, Leka, Gjoka, Kola, Prifti, Çela, Hysa, Krasniqi, Berisha, Gashi, Morina, Kelmendi, Shala, Bytyqi, Rama, Basha, Hasani, Zeqiri"),
        "CZ" to l("Novák, Svoboda, Novotný, Dvořák, Černý, Procházka, Kučera, Veselý, Horák, Němec, Marek, Pospíšil, Pokorný, Hájek, Král, Jelínek, Růžička, Beneš, Fiala, Sedláček, Doležal, Zeman"),
    ).filterValues { it.isNotEmpty() }

    fun forNation(nation: String): List<String>? = byNation[nation]
}
