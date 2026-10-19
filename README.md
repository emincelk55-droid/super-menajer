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
