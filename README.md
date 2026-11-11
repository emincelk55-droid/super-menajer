# Süper Menajer ⚽

Android için Türkçe futbol menajerlik oyunu (Kotlin + Jetpack Compose).

- 8 ülke, 13 lig, 234 kurgusal kulüp: Türkiye, İngiltere, İspanya, Almanya, İtalya (2'şer lig), Fransa, Portekiz, Hollanda
- İstediğin ülkeden, istediğin ligden kariyere başla; ülke kupaları, küme düşme / yükselme
- Ülke kupaları (tek maç eleme, penaltılar)
- Avrupa Kupası: 8 büyük ligin ilk 4'ü, 8 grup ve tek maç eleme turları
- Canlı maç motoru ve Türkçe anlatım, maç içi değişiklik ve taktik
- Transfer pazarlığı, transfer dönemleri, gelen teklifler
- Antrenman, oyuncu gelişimi, altyapı
- Ekonomi: transfer bütçesi + haftalık maaş limiti, başarıya göre sezon sonu ödeneği, sözleşmeler
- Yönetim güveni ve kovulma, basın toplantıları, devre arası konuşması
- Championship Manager ruhu: gerilimli metin anlatımı, gizli özellikler ve gözlemci raporları (1-20 ölçek), wonderkid avı, oyuncu mutluluğu ve transfer talepleri, birebir konuşma, kiralık gönderme, rakip raporu, sezon ödülleri, kariyer teklifleri
- Teknik ekip (yardımcı antrenör, antrenör, fizyoterapist, gözlemci; kalite 1-20) ve maç öncesi, devre arası, maç sonu takım konuşmaları
- Stadyum ve tesisler: seyirci sayısı (form, derbi, bilet fiyatı), stadyum büyütme, antrenman tesisi, altyapı akademisi ve sağlık merkezi (seviye 1-10), sezon başı altyapı seçmeleri
- Sponsorluk: forma, stadyum isim hakkı ve malzeme sponsoru; garantili, dengeli ya da performans primli sözleşmeler
- Taraftar memnuniyeti: sonuçlar, derbiler, transferler ve bilet fiyatıyla değişir; seyirciyi, sponsorları, yönetim güvenini etkiler, coşkulu taraftar evde 12. adam olur
- Yatay ekran tasarımı: CM tarzı DEVAM düğmesi, gelen kutusu (okunmamış rozeti), Kulüp menüsü (teknik ekip, tesis, sponsor, finans, taraftar), ayarlar ve yardımcı antrenör raporları

## Derleme

```
./gradlew :app:assembleRelease   # veya: gradle :app:assembleRelease
```

`local.properties` içinde `sdk.dir` tanımlı olmalı. Sürüm imzası için proje kökünde
`keystore.properties` ve imza anahtarı (`app/supermenajer.jks`) gerekir; bunlar güvenlik nedeniyle
depoda tutulmaz. Dosyalar yoksa APK debug anahtarıyla imzalanır.

## Testler

```
gradle :app:testDebugUnitTest          # sezon simülasyonu ve kayıt taşıma testleri
gradle :app:recordPaparazziDebug       # ekran görüntüsü testleri
```

### v1.2
- Gerçekçi (kurgusal) oyuncu isimleri: 35+ ülke, geniş isim havuzları
- Her oyuncuya özel çizilmiş yüz ve FUT tarzı oyuncu kartları, kalkan şeklinde armalar
- Taktik tahtası: sürükle-bırak kadro, tempo/genişlik/pas stili/savunma hattı, kontra, ofsayt tuzağı, zaman geçirme
- Gerçek tarihli takvim: DEVAM gün gün ilerler, önemli olaylarda durur
- Diyaloglu transfer: kulüple bonservis (taksit, sonraki satıştan pay, kiralık) ve menajerle sözleşme (maaş, süre, imza parası, rol); satış listesi
- Transfer bütçesi ↔ maaş bütçesi aktarımı, 3 kayıt yuvası

### v1.3
- Oyuncu kişilikleri (profesyonel, hırslı, tembel, lider, büyük maç oyuncusu, sakatlığa yatkın, sinirli, sadık)
- FM tarzı roller, kaptan, penaltı/frikik/korner kullanıcıları, forma numaraları
- Maçta kenar çizgisi talimatları, antrenman kampı, gizli antrenman, galibiyet primi, hazırlık maçları
- Yönetimden istekler, başarımlar, kupa dolabı, menajer itibarı, kulüp rekorları, transfer döneminin son günü

### v1.4
- Detaylı mevkiler (KL, SLB, SĞB, STP, DOS, MO, OOS, SLK, SĞK, SF) ve tercih edilen ayak
- Oyuncu özellikleri: kaleci / libero kaleci, stoper / topla oynayan stoper, bek / kanat bek, defansif / iki yönlü / ofansif orta saha, oyun kurucu, kanat, fırsatçı golcü, hedef santrfor, sahte 9, kanat forvet
- Mevki uyumu ve doğal rol maç performansını etkiler; otomatik 11 detaylı mevkiye göre dizer

### v1.5
- Bireysel antrenman (özellik odağı) ve oyunculara yeni mevki öğretme (2. mevki)
- Takım dinamikleri: soyunma odası hiyerarşisi, oyuncu grupları ve maça etki eden takım uyumu
- Beklenmedik olaylar: disiplin, ev özlemi, yatırımcı, emeklilik, transfer söylentisi, genç yetenek — kararları senin
- Hoca pazarı: rakip hocaların koltuk güvencesi, kovulmalar ve boş koltuklara başvuru
- Menajer seviyesi ve 8 dallı yetenek ağacı (pazarlık, gençler, taktik, motivasyon, ekonomi, gözlemci, disiplin, halkla ilişkiler)

### v1.6
- FM tarzı 2D maç motoru: top oyuncudan oyuncuya pasla gider, oyuncular ivmelenerek akıcı koşar, rakip pres yapar ve top kapar
- Goller, kurtarışlar, direkler, autlar, kornerler ve penaltılar sahada canlı oynanır; sonuç top ağlara/kaleciye ulaşınca açıklanır
- Gerçek ölçülü saha, forma numaralı oyuncular, havalanan top ve gölgesi, top sürenin adı, gol kartı
- FM tarzı oyuncu profili: başlık şeridi, mevki mini sahası, sekmeler ve 3 sütunlu detaylı nitelikler (1-20)
- Lacivert/neon yeni renk teması

### v1.7
- FM tarzı taktik ekranı: üstte sekmeler (Diziliş, Talimatlar, Roller & Görevler, Antrenman), gerçek ölçülü saha, forma şeklinde oyuncular ve üstlerinde rol kısaltmaları (uyuma göre yeşil/sarı/kırmızı)
- Sağda FM tarzı kadro listesi: mevki, kondisyon halkası, güç ve rol; hücum/orta saha/savunma gücü ve takım kimyası
- Yeni puan tablosu: atılan/yenilen gol, renkli averaj, son 5 maç formu, bölge renkleri ve kendi takımın vurgulu

### v1.8
- Gerçek dünya: 13 ligin 2026-27 kadroları (Süper Lig, 1. Lig, İngiltere, İspanya, Almanya, İtalya ve 2. ligleri, Fransa, Portekiz, Hollanda) — 234 kulüp, 6.500 oyuncu
- Güçler, mevkiler, ikinci mevkiler, ayak, yaş ve forma numaraları gerçeğe göre; teknik direktörler ve stadyumlar (kapasiteleriyle) gerçek
- İsimler gerçeğe benzer ama birebir aynı değil (ör. Arda Gülar, Kylian Mbapé, Galatasarai, Trabzunspor)
- Sponsorlar gerçek markalara benzer isimlerle; kadro sınırı 34
- Veri biçimi ve doğrulayıcı: tools/world/

### v1.9
- Çoklu mevki: her gerçek oyuncunun oynayabildiği tüm mevkiler (ör. Barış Alper SĞK/SLK/SF/SĞB/SLB, Arda Gülar OOS/SĞK/MO/SLK)
- Otomatik 11, maç gücü, oyuncu değişiklikleri ve taktik ekranı mevki listesine göre çalışır; kanat forvetleri kanat orta sahada da doğal oynar
- Talimatlar ve Roller & Görevler tam ekran; saha ve kadro listesi yalnızca Diziliş sekmesinde

### v2.0
- Mobil düzen: Kulüp menüsü tek satırda kısa sekmeler (Ekip, Tesisler, Sponsor, Finans, Taraftar, Kariyer, Yetenek, Hocalar; bildirimler kırmızı noktayla)
- Kadro ve maç anlatım/istatistik geçişleri sekme şeridi; maç kontrolleri eşit genişlikte, taşmıyor
- Talimatlar ve Roller iki sütun

### v2.1
- Çoklu ajan testi: oyun dengesi (3 sezon simülasyonu), kod hataları, arayüz ve gerçek veri kalitesi denetlendi.
- Zorluk seçimi (Kolay / Normal / Zor) ve "Kovulma yok" serbest modu.
- Oyuncu kariyer geçmişi: her sezonun maç/gol/asist/puan kaydı profilde.
- Dünya dengesi: AI sözleşme yenileme, gerçekçi emeklilik eğrisi, eksik mevkilerin dolması, gol oranı ayarı.
- Maç sonu ekranında golcüler ve maçın oyuncusu üstte; yan menü dokunma alanları büyüdü.
- Hata düzeltmeleri: ana menüye dönünce ilerlemenin sürmesi ve başka yuvaya kayıt, kiralık oyuncuyu tekrar kiralama,
  transfer son gününün hiç gelmemesi, 2D motorun oyuncu değişikliğinde takılması, saha çiziminde çökme,
  ayrılan oyuncuya ait olay kartları, bekletilen satış tekliflerinin denetimsiz kabulü, gün atlamada boş günlerin yaşanmaması,
  penaltı atışlarında sonsuz döngü, dünyanın iki kez üretilmesi.
- Veri: Bundesliga kaleci değerleri, yıldız güçleri, takım itibarları ve çift kayıtlı oyuncular düzeltildi.

### v2.2
- Açılış ekranı (logo, yükleme çubuğu, ipuçları, "başlamak için dokun") ve yeni ana menü: Devam Et, Yeni Kariyer, Kariyer Yükle, Nasıl Oynanır, Çıkış.
- SM tarzı ana sayfa: ☰ yan menü, bütçe/maaş/güven göstergeleri, tarihli büyük "Devam Et", görev kısayolları, kayan haber bandı;
  büyük maç kartı (armalar, takım güçleri, turnuva, tarih, stadyum), gelen kutusu, menajer seviyesi, müsabakalar, spot lambası,
  transferler, tesisler, takvim ve son maçlar.
- SM tarzı taktik ekranı: perspektif saha, yüzlü oyuncu kartları (güç, mevki, moral, kondisyon); Kadro (yedekler + kadro kartları),
  Diziliş (küçük saha resimleri), Talimatlar (Genel/Hücum/Savunma/Takım görevleri, açılır seçimler ve sahada görsel oklar), Roller.
- Transfer: detaylı mevki, yaş, en az güç ve en fazla bonservis filtreleri; ⭐ kısa liste; ⚖ oyuncu karşılaştırma; daha hızlı liste.

### v2.3
- SM tarzı maç ekranı: büyük skor tabelası (armalar, golcüler, skor kutuları, dakika, topa sahip olma), sahanın üstünde anlatım bandı,
  sağda Olaylar/İstatistik/Oyuncular, altta kontrol çubuğu (oynat, hız, mentalite, 📣 bağır, değişiklik, sonuca git),
  sahanın ortasında devre arası ve maç sonu kartları; yenilenen maç sonu özeti (maçın oyuncusu kartı).
- FM tarzı 4 adımlı yeni kariyer: Takım seç (ülke → lig → kulüp, kulüp bilgi paneli, rastgele takım), Menajer profili (ad, soyad, uyruk, itibar),
  Ek seçenekler (zorluk, kovulamaz ol), Özet ve kurulum ekranı.
- Kariyer açılışı: "Son dakika" TV haberi, 6 sayfalık hoş geldin rehberi, yardımcı antrenörün takım raporu ve ilk mesajlar.
- Taktik Oluştur: 7 oyun tarzı (Tiki-Taka, Gegenpress, Topa Sahip Ol, Kanat Oyunu, Uzun Top, Kontra Atak, Catenaccio),
  kadroya uygunluk çubukları, artı/eksi yönler ve önerilen dizilişler; tek dokunuşla uygulanır.

### v2.4
- Takım görevleri: FM tarzı duran top tablosu (penaltı/frikik/korner yıldızları, dokunarak ata) ve kaptan tablosu
  (durum, sosyal grup, yaş, liderlik; kaptan ve yardımcı kaptan seçimi — kaptan yoksa yardımcısı moral verir).
- Rol penceresi: oyuncuya dokununca roller (★ önerilen), açıklama, önemli özellikler ve markaj seçimi (alan / adam adama).
  Adam adama markaj maç motorunda savunmayı güçlendirir, oyun kurulumunu biraz zayıflatır.
- A2 takımı: oyuncuları A2'ye gönder/geri al; A2 her lig haftası maç oynar, 22 yaş altı oyuncular gelişir, 24 yaş üstü mutsuz olur.
- Milli takım kariyeri: kulüple birlikte milli takım çalıştır (yeni kariyerde ya da Milli Takım ekranından, itibara göre),
  kadro çağır/çıkar, 6/13/22/29. haftalarda milli maçlar, sezon sonunda Avrupa Şampiyonası / Dünya Kupası.

### v2.5
- Milli maçlar artık canlı: pencere haftasında "Milli maç günü" gelir, DEVAM ET seni maça götürür.
  Kulüp maçlarıyla aynı SM tarzı maç ekranı: 2D saha, soyunma odası konuşması, değişiklik, mentalite, bağırma, devre arası.
- İki ulusun gerçek oyuncu havuzundan otomatik ilk 11 ve yedekler; milli takım dizilişi Milli Takım ekranından seçilir.
- Milli maç kulüp istatistiklerine yazılmaz; yorgunluk, sakatlık ve moral kulübe yansır. "Hızlı sonuç" ile simüle de edilebilir.
- Milli takımlar skor tabelasında bayraklarıyla görünür.

### v2.6
- Yaz turnuvası (Avrupa Şampiyonası / Dünya Kupası) artık canlı: sezon bitince "Turnuva" ile kura çekilir.
  16 ulus, 4 grup (ilk iki çıkar), çeyrek final, yarı final, final. Senin maçların canlı (ya da hızlı sonuç),
  diğer maçlar aynı turda sonuçlanır; eleme maçlarında beraberlikte penaltılar.
- Milli Takım ekranında grup tabloları (puan, averaj) ve eleme turu sonuçları; ana sayfada turnuva kartı.
- Elenirsen turnuva arka planda tamamlanır ve şampiyon açıklanır; kazanırsan kupa dolabına girer, itibar ve XP kazanırsın.

### v2.7
- Sakatlık türleri (kas zorlanması, hamstring, diz bağı, kırık...) ve sağlık ekibi kararı: tam tedavi, uzman tedavisi (ücretli, daha hızlı)
  ya da riskli hızlı dönüş (süre yarıya iner, 6 hafta yeniden sakatlanma riski).
- Rakibe özel talimatlar (maç hazırlığı): yıldızını adam adama tut, kanatları kapat, önde baskı, arkaya koşu — maç motorunu etkiler.
- Serbest kalma bedelleri (İspanya'da tüm oyuncular, diğer liglerde bazıları): bedeli öde, kulübün onayı gerekmez.
- Ön sözleşme: sözleşmesinin son yılındaki oyuncuyla 17. haftadan sonra anlaş, sezon sonunda bedelsiz gelsin.
- Rakip takım profili: lig tablosundan ya da maç kartından takıma dokun; kadro (tahmini güçler), form, son maçlar, hoca, stadyum.
- Sezon görevleri (meydan okumalar) XP ve bütçe ödülüyle; taraftar sosyal medya akışı; Kulüp → Efsaneler (en çok forma giyenler,
  golcüler, şöhretler galerisi).
- Ana sayfada bekleyen kararlar (sağlık, milli maç, teklifler) üste taşındı; sık kayıtlarda atlanan kayıt artık kısa süre sonra yazılıyor.

### v2.8
- Donma iyileştirmesi: gün gün ilerleme, maça atlama, hızlı sonuç ve maç sonu diğer maçların oynatılması arka planda;
  uzun işlemlerde "ilerleniyor" katmanı.
- Rakip menajerler: büyük maçlardan bir gün önce rakip hoca basına laf atar, cevabın (sakin / sert / centilmen)
  takımının moralini, rakibin hırsını ve taraftarı etkiler. Rakip hocalarla kafa kafaya karne.
- Kulüp vizyonu: yönetimin 5 yıllık planı (lig hedefi, Avrupa, gençler, mali disiplin, tesis); her sezon sonu değerlendirilir.
- Takvim ekranı (Lig → Takvim, ana sayfadaki 📅): aylık görünüm, maç günleri, rakip, iç/dış saha ve skorlar.
- Veri editörü (Ayarlar): oyuncu adı, yaşı, potansiyeli ve niteliklerini düzenle.
- Çok oyunculu: ana menüden "İki Kişilik Maç" — aynı telefonda iki oyuncu birer takım seçer; ikisi de mentalite ve
  oyuncu değişikliğini yönetir.
- Düzeltmeler: rakibe özel talimat hazırlık maçında harcanmıyor; iyileşen oyuncunun sağlık kartı kalmıyor.

### v2.9
- Görsel yenileme: sekmeler arası yumuşak geçiş, sekme çubuklarında kayan parlak gösterge, kartlarda gölge ve
  ışıklı kenar, başlıklarda renkli şerit.
- Düğmeler dokununca yaylı şekilde küçülür; DEVAM ET ve ana menünün birincil düğmesinde periyodik parlama.
- Ana menü düğmeleri sırayla kayarak gelir; pencereler hafif büyüyerek açılır.
- İlerleme çubukları soldan dolar ve renk geçişli; güç rozetleri parlak degrade.
- Maçta gol olunca skor kutusu zıplar, takım renginde parlar ve rakam kayarak değişir.

### v2.10
- Gol kutlaması: gol anında sahada flaş ve genişleyen ışık halkaları, zıplayarak büyüyen dev "GOOOL!" yazısı ve
  iki köşeden fırlatılan, takım renklerinde dönen konfeti yağmuru.
- Rakip gol atınca konfeti yok; sönük bir "GOL" yazısı ve halka görünür. İki kişilik maçta ve izlenen maçlarda her gol kutlanır.

### v2.11
- Parlak oyuncu kartları: üstte cam parıltısı, ara ara kartın üzerinden geçen çapraz ışık; efsane (85+) kartlarda
  göz kırpan yıldızlar ve mor-altın kenar, altın (75+) kartlarda altın kenar ve parıltılı gölge, gümüş kartlarda gümüş kenar.
- Ana sayfada canlı gece stadyumu: salınan projektör ışıkları, takım renklerinde tribün, tribünde dolaşan Meksika dalgası,
  patlayan fotoğraf flaşları, yükselen ışık tozu ve perspektif saha (kartların arkasında, soluk).
- Ses: hakem düdükleri (başlama, devre, maç sonu, kart), maç boyunca tribün uğultusu (ataklarda kabarır), gol
  kükremesi, kaçan pozisyonda "ooo", top vuruşu, dokunma tıkı, yeni karar kartında bildirim sesi ve menü müziği.
  Ayarlar → efektler / müzik aç-kapa ve ses düzeyi. Tüm sesler `tools/gen_sounds.py` ile sentezlenir (lisans gerektirmez).
- Denge (15 sezonluk otomatik test, `BalanceReportTest`): ev sahibi avantajı gerçek liglere çekildi (ev %45, beraberlik
  %22, deplasman %33); yaşlanma gelişimin tersine işliyor, kaleciler sezonlar geçtikçe şişmiyor (gol/maç 2.6-2.8 sabit);
  kurtarış gücü kalecinin genel gücünden; sözleşmesi biten genç oyuncular silinmiyor; emeklilik 32'den başlıyor;
  yapay zekâ kadroyu ~26 kişide tutacak kadar genç alıyor, gençlerin potansiyeli kulüp seviyesine yakın.
- Kayıt güvenliği: her kayıt yuvası için en az 10 dakika arayla alınan 3 döner yedek; ana kayıt bozulursa (yarım yazılmış
  dosya, ani kapanma) en yeni sağlam yedekten açılır ve kullanıcıya söylenir.
- Çökme raporu: beklenmedik kapanmada ayrıntı cihaza yazılır, bir sonraki açılışta "Raporu paylaş" ile WhatsApp/Telegram/
  e-posta üzerinden gönderilebilir (kendiliğinden internete bir şey gönderilmez).
- Pil tasarrufu (Ayarlar): sürekli dönen efektleri durdurur. Kart ışığı ve stadyum animasyonları artık her karede yalnız
  yeniden çiziliyor (ekranı baştan oluşturmuyor), daha az işlemci harcıyor.

### v2.12
- Play Store hazırlığı: hedef Android sürümü API 35; yeni uygulama ikonu (oyun logosu kalkan); `docs/store/` altında
  512 px ikon, 1024x500 tanıtım görseli, 1920x1080 ekran görüntüleri ve Türkçe/İngilizce mağaza metinleri;
  `docs/gizlilik-politikasi.md`. Görseller `tools/store_assets.sh` ile yeniden üretilir. Mağaza paketi: `gradle :app:bundleRelease`.
- Gerçek sesler: tribün uğultusu ve "ooo" 58 bin kişilik bir maçtan, gol kükremesi Avusturya-İsveç maçından, hakem
  düdüğü gerçek kayıttan; menü müziği Kevin MacLeod "Carefree". Kaynaklar ve lisanslar: `docs/SES_KAYNAKLARI.md`.

### v2.13
- 2D maç: devre arası ve maç sonu düdüğünde oyuncular tokalaşıp tünelden soyunma odasına yürür ve sahne durur (önceden
  maç bittikten sonra da oynamaya devam ediyorlardı); ikinci yarıda takımlar tünelden sahaya çıkar.
- 2D maç: goller ağır çekim tekrar edilir (sinema bandı, "TEKRAR", sahaya dokununca geçilir; Ayarlar'dan kapatılabilir);
  atakta sarı kesik ofsayt çizgisi; serbest vuruşta 4 kişilik baraj.
- Maç sunumu: devre arası ve maç sonu kartlarında takım renklerinde istatistik çubukları; maç özetinde parlak kartlı
  "Maçın Oyuncusu"; gollerde bağlamlı anlatım (hat-trick, eşitlik, öne geçiş, fark, son dakika golü).
- Haftalık gazete (ana sayfada 📰): oyunun gerçek durumundan manşetler (son maçın, lig zirvesi, küme hattı, gol
  krallığı, son transfer, yönetimin sabrı) ve transfer söylentileri (yıldızımıza talip olanlar, bizimle anılanlar).
- Kurgusal isimler (yeni kariyerde varsayılan, ilk adımda kapatılabilir): gerçek veriden gelen kulüp, kısa kod, stadyum,
  teknik direktör ve oyuncu adları ülkeye uygun uydurma adlarla değişir (gerçek bir kulüp ya da futbolcuyla aynı ad
  çıkmaz); güçler, renkler, şehirler ve kadro yapısı aynı kalır. Mevcut kayıtlar etkilenmez.
- Maç içi taktik (📋 Taktik): maç durur; diziliş (oyuncular yeni mevkilere en uygun şekilde yerleşir) ve mentalite,
  pres, tempo, genişlik, pas, savunma hattı değişir. Yardımcı antrenör maç içinde durumu okuyup öneri verir (gerideyken
  hücum, öndeyken savunma, yorgun/sarı kartlı oyuncu, rakip eksikken pres...), tek dokunuşla uygulanır.
- Kadro → 🌱 Gelişim: 21 yaş altı oyuncular ve kiralıktakiler; sezon sezon güç grafiği, potansiyel bandı (antrenör
  kalitesine göre daralır), bu sezonki artış ve yardımcının önerisi (A takıma hazır / kiralık önerilir / geri çağır).
- Derbi atmosferi: aynı şehrin kulüpleri (İstanbul, Madrid, Milano, Londra...) ve aynı ligin iki devi derbi sayılır.
  Derbi haftası ana sayfada ateşli kart ve geri sayım, gazetede derbi manşeti; maç başında tribün koreografisi
  (takım renklerinde dalgalanan pankart, meşaleler), derbi adı skor bandında, tribün daha gür.
- Kupa töreni: kariyerde yeni bir kupa kazanılınca tam ekran tören (sahne ışıkları, yükselen altın kupa, sürekli
  konfeti, "KUPAYI KALDIR"); her kupa bir kez kutlanır. Sezon sonu kartında 🏆 Ödül Gecesi: yılın oyuncusu, gol kralı,
  asist kralı, yılın genci, yılın kalecisi ve yılın menajeri sırayla açılır; bizim oyuncumuz kazanırsa altın çerçeve.

### Geliştirmede (henüz APK yok)
- Denge: goller tek yıldız forvette fazla toplanmıyor (gol kralı tüm kupalarda ~25-50 gol, gerçek ligler gibi);
  denge raporuna gol kralları eklendi.
- Oyuncu talepleri: forma şansı isteyen yedek (6 maçta 3 kez oynatma sözü verebilirsin; tutulmazsa moral çöker,
  soyunma odası görür), zam isteyen yıldız (maaş bütçesine bağlı), büyük kulübe gitmek isteyen oyuncu. Verilen sözün
  durumu oyuncu profilinde görünür.
- Lig → 📈 Analiz: sezon özeti (G-B-M, gol, xG, topa sahip olma, gol yemeden), maç maç gol beklentisi grafiği,
  puan çizgisi (oyuna göre beklenen puanla), gollerin dakika dilimleri ve otomatik yorumlar (bitiricilik, geç gol
  yeme, topa sahip olma...).
- Yeni logo: altın çerçeveli zümrüt-lacivert kalkan, üç yıldızlı altın şerit, defne dalı içinde top ve parlama;
  uygulama ikonu, oyun içi logo ve mağaza görselleri tek tanımdan (`tools/gen_logo.py`). Marka yazısı altın/zümrüt degrade.
- Kayıt dosyası gzip ile sıkıştırılıyor (2,3 MB → ~340 KB; telefonda daha hızlı yazma/yükleme, daha az pil); eski düz
  metin kayıtlar da açılır.
- Ayarlar → Yazı boyutu (Normal / Büyük / Çok büyük): tüm metinler büyür, ana sayfa kartları da birlikte uzar. En küçük
  yazılar 9'dan 10 puntoya çıktı.
- İlk oynayış rehberi: her ana ekran ilk kez açıldığında kısa ipucu balonu (bir kez; "İpuçlarını kapat" ya da
  Ayarlar → "İpuçlarını yeniden göster"). Ana sayfa varsayılan olarak sade: maç, kulüp durumu, kararlar ve gazete;
  "Detaylı görünüm" ile finans, form, görevler ve sosyal medya kartları açılır.
- Taktik 2D sahada görünür: savunma hattı ve mentalite takımı ileri/geri iter (geride ~33 m, önde ~53 m ortalama),
  genişlik kanatları açar/daraltır, pres yoğunluğu topa basan oyuncu sayısını (1-3) ve hızını belirler.
- Uzun dönem dengesi: yapay zekâ kulüpleri altyapıdan genç alırken ve 30+ oyuncularla sözleşme uzatırken kadronun yaş
  yapısına bakar (20 sezonda ortalama yaş 24-26,5 arası; bütçeler kontrollü, lig medyanı 20 sezonda +%24).
- Aylık ödüller: her ay sonunda kendi liginde Ayın Oyuncusu, Ayın Genci ve Ayın Menajeri; ana sayfada ödül kartı.
  Kazanan bizden ise oyuncunun morali, menajerin itibarı ve yönetimin güveni artar.
- Menajer özellikleri: oyun tarzına göre kendiliğinden kazanılan kalıcı özellikler (Genç Dostu, Hücum/Savunma Ustası, Derbi Kralı, Geri Dönüş Ustası, Kupa Adamı); her birinin maçta ya da gelişimde küçük etkisi var, ilerleme Kulüp → Yetenek sekmesinde.
- 2,5D maç görünümü (TV kamerası): yan tribünden perspektifli saha, topu yumuşakça izleyen kamera, gölgeli ve koşarken bacakları hareket eden oyuncular, file ve direkli kaleler, tribünde seyirciler ve reklam panoları; gol tekrarı da bu açıdan. Maç ekranının sağ üstündeki düğmeyle klasik 2D görünüme geçilebilir (seçim hatırlanır).
