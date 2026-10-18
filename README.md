# Süper Menajer ⚽

Android için Türkçe futbol menajerlik oyunu (Kotlin + Jetpack Compose).

- 18 takımlı Süper Lig ve 1. Lig, küme düşme / yükselme
- Türkiye Kupası (tek maç eleme, penaltılar)
- Canlı maç motoru ve Türkçe anlatım, maç içi değişiklik ve taktik
- Transfer pazarlığı, transfer dönemleri, gelen teklifler
- Antrenman, oyuncu gelişimi, altyapı
- Yönetim güveni ve kovulma

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
