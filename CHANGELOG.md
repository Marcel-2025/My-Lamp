# Changelog

## 2.0 (versionCode 4) – Premium-Redesign

### Mehrsprachigkeit (Ergänzung 06.11.2026)
- 12 Sprachen: Deutsch, Englisch (Standard), Spanisch, Französisch, Italienisch, Niederländisch, Polnisch,
  Portugiesisch, Türkisch, Russisch, Japanisch, Chinesisch (vereinfacht)
- Sprachauswahl in den Einstellungen (Systemsprache oder feste Sprache); ab Android 13 auch in den
  Android-Einstellungen unter Apps → My Lamp → Sprache
- Alle Sprachen werden mit dem App Bundle ausgeliefert (kein Sprach-Split), damit die Auswahl sofort wirkt
- Auswahl-Knöpfe in den Einstellungen erlauben zwei Zeilen für längere Übersetzungen

### Tablet & Querformat (Ergänzung 05.11.2026)
- Eigene Layouts für Handy hoch, Handy quer, Tablet hoch und Tablet quer (nach verfügbarer Fläche, nicht nach Gerätetyp)
- Querformat: zwei Spalten – Power-Button links, Modi und Stroboskop rechts; Premium-Hinweis in der Kopfzeile
- Tablets: größerer Power-Button (bis 320 dp), große Moduskacheln, mehr Randabstand
- Einstellungen ab 840 dp Breite zweispaltig; Premium-Seite im Querformat mit Kauf links und Vorteilen rechts
- Dialoge und Freischaltanimation scrollen bei geringer Höhe (Handy quer)
- Eigene Querformat-Hintergrundbilder (`drawable-land-nodpi`, bis 1920 × 1200 px)
- Flüssige Rotation ohne Neustart der Activity (`configChanges`): Licht, Banner und Zustand bleiben erhalten
- Kein geteilter Bildschirm: `resizeableActivity="false"` plus Android-16-Opt-out
  `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` für Tablets
- Inaktive Moduskacheln mit dunklerem Glasgrund (besser lesbar auf hellen Designs)

### Neu
- Komplett neues Dark-Premium-Design (#050711 / #0C0F1C / Gold #FDEE51) mit Glas-Karten, Glow und Microinteractions
- Animierter Power-Button (Farbwechsel, Glow, Druckeffekt, Puls nur bei aktivem Licht)
- Moduskacheln mit aktivem Status, Stroboskop-Karte mit Frequenzanzeige
- Vollständige Einstellungsseite (DataStore), alle Optionen funktionieren
- Premium-Seite mit echtem Play-Preis, Wiederherstellen-Button und Einmalkauf-Hinweis
- Einmalige Freischaltanimation nach bestätigtem Kauf
- 4 Premium-Designs aus den vorhandenen Hintergrundbildern, 2 kostenlose Designs
- Bildschirmlicht mit 5 Farben und Helligkeitsregler
- LED-Helligkeit auf Android 13+ (sofern unterstützt)
- Schütteln zum Schalten (aktiviert, mit Empfindlichkeit)
- Schnelleinstellungs-Kachel
- UMP-Consent (EWR/UK) und Datenschutzoptionen
- Englische Übersetzung (Deutsch bleibt vollständig erhalten)
- Monochromes Icon für Android-13-Themed-Icons

### Behoben
- Adaptives Launcher-Icon zeigte einen blauen Hintergrund, der nicht zum Play-Store-Icon passte
- Beim Start gefundene, nicht bestätigte Käufe wurden nie acknowledged (automatische Erstattung nach 3 Tagen)
- Rückerstattungen und ausstehende Käufe wurden nicht behandelt
- Premium-Status ging bei Offline-Start verloren (Käufer sahen Werbung)
- Release-Build nutzte eine Platzhalter-Banner-ID → jetzt zentral konfigurierbar, ohne ID keine Requests
- Absturzrisiko durch `cameraIdList[0]` auf Geräten ohne (Rück-)Kamera
- SOS-Morsezeiten korrigiert (Strich = 3 Einheiten, Pausen 1/3/7)
- Strobe bis 10 ms war zu schnell für die Hardware → 1–12 Hz
- Mehrfach gestartete Blink-Jobs möglich → jetzt exklusiver Job mit `cancelAndJoin`
- Licht ging bei Bildschirmdrehung aus
- Einstellungen und Premium-Hinweise wurden nicht gespeichert
- UI-Elemente konnten unter der Navigationsleiste liegen → WindowInsets statt fester Abstände
- Riesige Hintergrundbilder (bis 5000×6200 px) in `drawable/` → 1080 px in `drawable-nodpi/` (deutlich weniger RAM)
- Instrumented Test prüfte den alten Paketnamen

### Technik
- compileSdk/targetSdk 36, Kotlin 2.2.21, Java 17, Compose BOM 2025.10.01
- Billing 6.2.1 → 8.3.0, Play Services Ads 23.1 → 24.9, UMP 3.2
- MVVM: ViewModel + StateFlow, Repositories, manuelle DI, MainActivity von 981 auf ~130 Zeilen
- `CAMERA`-Berechtigung entfernt (für die Taschenlampe nicht nötig)
- R8 + Resource-Shrinking im Release, AAB 19 MB → 9 MB
- 23 Unit-Tests

## 1.2 (versionCode 3)
- Stand vor dem Redesign (Branch `backup/my-lamp-before-premium-redesign`)
