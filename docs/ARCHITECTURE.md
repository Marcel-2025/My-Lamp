# Architektur

## Überblick

```
MainActivity ──► LampApp (Compose, Navigation per AnimatedContent)
     │                 │
     │                 └─► MainViewModel ──► SettingsRepository (DataStore „settings“)
     │                           │       ──► TorchController   (LED, SOS/Strobe, TorchCallback)
     │                           │       ──► BillingRepository (PBL 8, EntitlementStore „entitlement“)
     │                           │       ──► AdsManager        (UMP-Consent, MobileAds-Init)
     │                           └─► DebugOverrides (nur Debug, nur Darstellung)
     └─► ShakeDetector (nur STARTED + aktiviert)

TorchTileService ──► TorchController (gleiche Instanz wie die App)
```

- **AppContainer** (`core/`) ist eine einfache manuelle DI. Alle Abhängigkeiten leben im App-Prozess, Billing und Ads werden lazy erzeugt.
- **MVVM**: Der `MainViewModel` kombiniert Settings, Lichtmodus, Billing-, Debug- und Ads-Status zu einem `MainUiState` (StateFlow). Die Compose-UI sammelt ihn mit `collectAsStateWithLifecycle`.
- **Unidirektional**: Screens bekommen den Zustand und Callback-Objekte (`HomeActions`, `SettingsActions`) und kennen weder Repository noch Android-Services.

## Taschenlampe (`torch/`)

- `TorchController` sucht eine Kamera mit `FLASH_INFO_AVAILABLE` (bevorzugt Rückkamera) statt blind `cameraIdList[0]` zu nehmen. Ohne Blitz gibt es eine verständliche Meldung, und der Power-Button nutzt das Bildschirmlicht.
- Es läuft immer genau ein Muster-Job. Ein neuer Job beendet den alten per `cancelAndJoin()`, dadurch gibt es keine doppelten Blink-Jobs.
- Im `finally` jedes Musters wird die LED ausgeschaltet (`NonCancellable`).
- `TorchCallback` synchronisiert Änderungen von außen (System-Kachel, andere Apps). Eigene Schaltvorgänge werden 400 ms lang ignoriert, damit die Anzeige nicht flackert.
- Für `setTorchMode()` ist keine Kamera-Berechtigung nötig. Die `CAMERA`-Permission wurde deshalb entfernt.
- `LightPatterns` ist reine Kotlin-Logik (SOS nach ITU, Strobe-Grenzen 1–12 Hz) und per Unit-Test abgesichert.

### Lifecycle-Verhalten

| Ereignis | Verhalten |
|---|---|
| Drehung / Konfigurationswechsel | Licht bleibt an (Zustand liegt im app-weiten Controller) |
| Home / App-Wechsel | Dauerlicht und SOS laufen weiter, Stroboskop stoppt |
| App per Zurück beenden (`isFinishing`) | Alles aus |
| Kamera von anderer App belegt | Modus aus, Meldung „Blitz wird verwendet“ |

## Einstellungen (`data/`)

Jetpack DataStore (Preferences). Ein Lese-Fehler (`IOException`) führt zu Standardwerten statt zum Absturz. Gesichert werden per Auto-Backup nur die Einstellungen, nicht der Kauf-Cache (siehe `res/xml/*rules.xml`).

## Systemleisten / Insets

- `enableEdgeToEdge()` mit dunklen, transparenten Leisten (ab Android 15 ohnehin Pflicht).
- Inhalte nutzen `WindowInsets.safeDrawing` (Statusleiste, Navigationsleiste, Display-Cutout) statt fester 72-dp-Abstände.
- Der Werbebanner sitzt in einem eigenen Bereich über `WindowInsets.navigationBars` und überlappt so nie mit Navigationsleiste oder Bedienelementen.
- Vollbild: `WindowInsetsControllerCompat.hide(systemBars())` mit `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. Das wird in `onWindowFocusChanged` erneut angewendet, z. B. nach dem Kaufdialog.
- Dialoge werden im Compose-Baum gezeichnet (kein eigenes Fenster) und bleiben dadurch auch im Vollbild bedienbar.

## Tablets, Querformat und Fenstermodi

| Layout | Bedingung | Aufbau |
|---|---|---|
| Handy hoch | kürzere Seite < 600 dp, hoch | eine Spalte, Kacheln unten |
| Handy quer | kürzere Seite < 600 dp, quer | zwei Spalten, kompakte Kacheln, Strobe ohne Balkenanzeige |
| Tablet hoch | kürzere Seite ≥ 600 dp, hoch | eine Spalte, Button bis 320 dp, große Kacheln |
| Tablet quer | kürzere Seite ≥ 600 dp (oder ≥ 840 × 480 dp), quer | zwei Spalten, großer Button links, Modi rechts |

- Entschieden wird nach der tatsächlich verfügbaren Fläche (`BoxWithConstraints` in `HomeScreen`), nicht nach Gerätetyp.
- Einstellungen: ab 840 dp Breite zwei Spalten. Premium-Seite: ab 720 dp Breite zweispaltig.
- Hintergründe: `drawable-nodpi` (hoch, 1080 px) und `drawable-land-nodpi` (quer, bis 1920 × 1200 px). Android wählt automatisch.
- Rotation: `configChanges` im Manifest, Compose ordnet neu an, ohne dass die Activity neu startet.
- **Geteilter Bildschirm:** `resizeableActivity="false"` verhindert Splitscreen und Freiform-Fenster auf Handys.
  Auf Tablets (ab 600 dp) ignoriert Android 16 diese Angabe für targetSdk 36. Das offizielle Opt-out
  `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` ist gesetzt, **entfällt aber ab targetSdk 37**.
  Außerdem lässt Android ab Version 12 auf Tablets Splitscreen grundsätzlich zu und zeigt die App dann mit Rändern.
  Ganz verhindern lässt sich das also nicht. Die App öffnet aber immer im Vollbild, und das Layout passt sich an,
  falls der Nutzer sie trotzdem teilt.

## Animationen

- `LocalMotion` deaktiviert alle Animationen, wenn der Nutzer sie ausschaltet oder Android „Animationen entfernen“ aktiv ist.
- Daueranimationen (Puls des Power-Buttons, Statuspunkt) laufen nur, solange Licht bzw. Modus aktiv sind.
- Animierte Werte werden in der Draw-Phase gelesen (`Canvas`, `graphicsLayer`), dadurch gibt es keine unnötigen Recompositions.
