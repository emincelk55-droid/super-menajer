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
- Yatay ekran tasarımı: soldan menü, iki sütunlu ekranlar, saha + anlatım yan yana maç ekranı

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
