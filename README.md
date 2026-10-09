# My Lamp

Premium-Taschenlampe für Android von **Flondy** – LED-Licht, SOS, Stroboskop und Bildschirmlicht in einem dunklen, goldenen Design.

| | |
|---|---|
| Package | `dev.flondy.mylamp` |
| Version | 2.0 (versionCode 4) |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |
| UI | Kotlin, Jetpack Compose, Material 3 |
| Monetarisierung | AdMob-Banner (UMP-Consent) + Einmalkauf `remove_ads` (Google Play Billing 8) |
| Play Store | https://play.google.com/store/apps/details?id=dev.flondy.mylamp |
| Kontakt | flondy.dev@gmail.com |

## Funktionen

- **LED-Taschenlampe** mit großem animiertem Power-Button, auf Android 13+ (sofern vom Gerät unterstützt) mit regelbarer LED-Helligkeit
- **SOS** nach ITU-Morsezeiten (· · · – – – · · ·, Einheit 200 ms)
- **Stroboskop** von 1 bis 12 Hz mit Warnhinweis zur Lichtempfindlichkeit vor dem ersten Start
- **Bildschirmlicht** in fünf Farben (Neutral, Warm, Kalt, Bernstein, Rot/Nachtsicht) mit regelbarer Helligkeit. Die ursprüngliche Helligkeit wird beim Verlassen wiederhergestellt.
- **Schütteln zum Schalten** (optional) mit drei Empfindlichkeitsstufen
- **Schnelleinstellungs-Kachel** „My Lamp“, die die LED ohne Öffnen der App schaltet
- **Einstellungen** (DataStore): Vollbild, Navigationsleiste, Animationen, Haptik, Design, Lichtquelle des Power-Buttons, Verhalten beim Start, Strobe-Tempo, Schütteln, Premium-Hinweise, Datenschutzoptionen, Support, Play-Store-Link
- **Designs**: Klassisch und Mitternacht (kostenlos); Nebelwald, Waldstraße, Lichtung und Goldene Stunde (Premium)
- **Premium** (`remove_ads`): dauerhaft werbefrei, Premium-Designs, goldene Akzente, einmalige Freischaltanimation
- Sprachen: Deutsch und Englisch

## Projektstruktur

```
app/src/main/java/dev/flondy/mylamp/
├── MyLampApp.kt              Application, erzeugt den AppContainer
├── MainActivity.kt           Edge-to-Edge, Systemleisten, Shake-Lifecycle, Consent-Start
├── core/                     AppContainer (manuelle DI), DebugOverrides (nur Debug)
├── torch/                    TorchController (LED, Muster, Torch-Callback), LightPatterns (SOS/Strobe-Logik)
├── sensor/                   ShakeDetector + ShakeLogic (testbar)
├── data/                     AppSettings, SettingsRepository (DataStore)
├── billing/                  BillingRepository (PBL 8), EntitlementEvaluator, EntitlementStore, PurchaseVerifier
├── ads/                      AdsManager (UMP + Mobile Ads Init), AdaptiveAdBanner
├── tile/                     TorchTileService (Quick Settings)
└── ui/                       MainViewModel, LampApp (Navigation/Overlays), theme/, components/, screens/
```

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/BILLING.md](docs/BILLING.md) · [docs/MONETIZATION.md](docs/MONETIZATION.md) · [docs/TESTING.md](docs/TESTING.md) · [docs/RELEASE.md](docs/RELEASE.md) · [CHANGELOG.md](CHANGELOG.md)

## Konfiguration (vor dem Release)

In `gradle.properties` bzw. `local.properties` (wird nicht committet):

```properties
# Produktive Banner-ID – bereits in gradle.properties eingetragen (local.properties überschreibt sie)
mylamp.admobBannerId=ca-app-pub-6130152704553606/1689806050

# Empfohlen: öffentlicher Lizenzschlüssel (Play Console → Monetarisierung einrichten → Lizenzierung)
mylamp.playLicenseKey=MIIBIjANBgkq...

# Empfohlen: URL der Datenschutzerklärung (erscheint dann in den Einstellungen)
mylamp.privacyPolicyUrl=https://...
```

Debug-Builds verwenden automatisch die offizielle Google-Test-Banner-ID. Die AdMob-App-ID steht zentral in `app/build.gradle.kts`.

## Bauen und Testen

```bash
./gradlew testDebugUnitTest     # Unit-Tests (SOS-Timing, Strobe, Shake, Kauf-Logik, Debug-Trennung)
./gradlew lintDebug
./gradlew assembleDebug
./gradlew bundleRelease          # Signierung wie bisher über Android Studio („Generate Signed App Bundle“)
```

## Bekannte Einschränkungen

- **Keine serverseitige Kaufprüfung:** Die App prüft Käufe über Google Play und optional per RSA-Signatur. Eine Prüfung über die Play Developer API erfordert ein eigenes Backend (siehe docs/BILLING.md).
- **SOS/Taschenlampe im Hintergrund:** Das Licht läuft weiter, solange Android den Prozess nicht beendet. Ein Foreground-Service für garantierten Dauerbetrieb ist nicht eingebaut. Das Stroboskop stoppt bewusst beim Verlassen der App.
- **Schütteln** funktioniert nur bei geöffneter App, um den Akku zu schonen.
- Kauf-, Consent- und Werbetests brauchen ein echtes Gerät mit Google Play und einen Lizenztester-Account (siehe docs/TESTING.md).

## Sicherheit

My Lamp ersetzt keine professionelle Notfallausrüstung. Schnell blinkendes Licht kann bei fotosensitiver Epilepsie Anfälle auslösen. Die App zeigt deshalb vor dem ersten Stroboskop-Start einen Warnhinweis.
