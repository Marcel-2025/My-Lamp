# Release-Vorbereitung – My Lamp 2.0 (versionCode 4)

## Checkliste vor dem Upload

- [ ] `mylamp.admobBannerId` in `local.properties` eintragen (AdMob → Apps → My Lamp → Anzeigenblöcke → Banner)
- [ ] optional `mylamp.playLicenseKey` und `mylamp.privacyPolicyUrl` eintragen
- [ ] versionCode prüfen: 4 muss **höher** sein als die höchste bereits hochgeladene Version, sonst erhöhen
- [ ] Signiertes App Bundle wie gewohnt über Android Studio erstellen (Keystore unverändert)
- [ ] Interner Test-Track → manuelle Tests aus `docs/TESTING.md`
- [ ] Play Console → **Data Safety** aktualisieren (siehe unten)
- [ ] Play Console → Store-Eintrag: neue Screenshots (die Oberfläche hat sich komplett geändert)
- [ ] Mapping-Datei (`app/build/outputs/mapping/release/mapping.txt`) wird mit dem AAB hochgeladen, R8 ist jetzt aktiv

## Icon- und Namens-Konsistenz (frühere Beanstandung)

| Ort | Vorher | Jetzt |
|---|---|---|
| Legacy-Icon (`mipmap-*/ic_launcher.png`) | creme, Lampe auf gelbem Kegel | unverändert |
| Adaptives Icon (Android 8+, auf fast allen Geräten sichtbar) | **blauer** Hintergrund + cremefarbenes Quadrat darin | cremefarbener Hintergrund `#FEFAF4` + identisches Motiv, sieht wie das Store-Icon aus |
| Themed/Monochrome-Icon (Android 13+) | farbiges Vordergrundbild | eigene einfarbige Silhouette |
| App-Name | „My Lamp“ | „My Lamp“ (nicht übersetzbar, auch in der Kachel) |

Referenz für das Store-Icon (512 px): `docs/assets/play_store_icon_512.png`. **Bitte prüfen**, ob das in der Play Console hinterlegte Hoch-Icon genau dieses Motiv zeigt. Die Feature Graphic sollte zur neuen, dunklen Oberfläche passen. Empfehlung: dunkler Hintergrund `#050711`, goldener Power-Button und der Schriftzug „My Lamp“.

## Data Safety (Vorschlag – bitte mit den aktuellen Google-Angaben abgleichen)

Durch das Google Mobile Ads SDK (nur kostenlose Nutzer):
- **Geräte- oder andere IDs** (Werbe-ID) – erhoben, geteilt (Werbung, Analyse)
- **App-Aktivität / App-Interaktionen** – erhoben (Werbung, Analyse)
- **App-Info und Leistung** (Absturz- und Diagnosedaten) – erhoben
- **Ungefährer Standort** (aus IP) – erhoben (Werbung)

Weitere Angaben:
- Daten werden bei der Übertragung verschlüsselt: **Ja**
- Die App enthält Werbung: **Ja**
- Berechtigungen: `INTERNET`, `BILLING`, `AD_ID` (über das Ads SDK). Die bisherige `CAMERA`-Berechtigung entfällt.

## Store-Texte

### Kurzbeschreibung (max. 80 Zeichen)

- **de-DE** (75): `Taschenlampe mit SOS, Stroboskop & Bildschirmlicht – schnell, hell, elegant`
- **en-US** (69): `Flashlight with SOS, strobe & screen light – fast, bright and elegant`

### Vollständige Beschreibung – de-DE

```
My Lamp ist die elegante Taschenlampe für dein Android-Gerät: ein Fingertipp, und es wird hell.

✦ LED-TASCHENLAMPE
Großer, gut erreichbarer Power-Button mit sanfter Lichtanimation. Auf unterstützten Geräten mit Android 13+ lässt sich die LED-Helligkeit regeln.

✦ SOS-SIGNAL
Das internationale Morsesignal · · · – – – · · · mit korrekten Zeiten, startet und stoppt zuverlässig.

✦ STROBOSKOP
Einstellbar von 1 bis 12 Hz. Vor dem ersten Start erscheint ein Hinweis zur Lichtempfindlichkeit.

✦ BILDSCHIRMLICHT
Neutral-, Warm- oder Kaltweiß, Bernstein oder Rot für die Nachtsicht, mit regelbarer Helligkeit. Ideal für Geräte ohne Blitz.

✦ PRAKTISCH
• Schnelleinstellungs-Kachel: Licht schalten, ohne die App zu öffnen
• Schütteln zum Ein- und Ausschalten (optional)
• Vollbildmodus und frei wählbares Startverhalten
• Dunkles, goldenes Design, das abends nicht blendet

✦ PREMIUM – EINMAL KAUFEN, FÜR IMMER WERBEFREI
Kein Abo, keine wiederkehrenden Kosten. Premium entfernt die Werbung dauerhaft und schaltet vier exklusive Designs frei. Alle Lichtfunktionen bleiben kostenlos.

Hinweis: Nicht direkt in die LED schauen. Schnell blinkendes Licht kann bei fotosensitiver Epilepsie Anfälle auslösen. My Lamp ersetzt keine professionelle Notfallausrüstung.

Fragen oder Feedback: flondy.dev@gmail.com
```

### Full description – en-US

```
My Lamp is the elegant flashlight for your Android device: one tap and there is light.

✦ LED FLASHLIGHT
A large, easy-to-reach power button with a smooth light animation. On supported devices running Android 13+, you can adjust the LED brightness.

✦ SOS SIGNAL
The international Morse signal · · · – – – · · · with correct timing. Starts and stops reliably.

✦ STROBE
Adjustable from 1 to 12 Hz. A photosensitivity notice is shown before the first start.

✦ SCREEN LIGHT
Neutral, warm or cool white, amber, or red for night vision, with adjustable brightness. Ideal for devices without a flash.

✦ CONVENIENT
• Quick Settings tile: toggle the light without opening the app
• Shake to toggle (optional)
• Fullscreen mode and configurable start behaviour
• Dark gold design that doesn't dazzle at night

✦ PREMIUM – BUY ONCE, AD-FREE FOREVER
No subscription, no recurring costs. Premium permanently removes ads and unlocks four exclusive designs. All light features stay free.

Note: Do not look directly into the LED. Rapidly flashing light can trigger seizures in people with photosensitive epilepsy. My Lamp is not a substitute for professional emergency equipment.

Questions or feedback: flondy.dev@gmail.com
```

### Versionshinweise

**de-DE** (max. 500 Zeichen)
```
My Lamp 2.0 – komplett neu gestaltet
• Neues dunkles Premium-Design mit goldenen Akzenten und sanften Animationen
• Korrektes SOS-Signal, Stroboskop 1–12 Hz mit Sicherheitshinweis
• Bildschirmlicht in 5 Farben mit Helligkeitsregler
• Neue Einstellungen, Vollbildmodus, Schütteln zum Schalten
• Schnelleinstellungs-Kachel
• Premium: 4 exklusive Designs, zuverlässigere Kauferkennung
• Keine Kamera-Berechtigung mehr nötig
• Stabilität und Akkuverbrauch verbessert
```

**en-US**
```
My Lamp 2.0 – completely redesigned
• New dark premium design with gold accents and smooth animations
• Correct SOS signal, strobe 1–12 Hz with safety notice
• Screen light in 5 colours with brightness control
• New settings, fullscreen mode, shake to toggle
• Quick Settings tile
• Premium: 4 exclusive designs, more reliable purchase detection
• Camera permission no longer required
• Improved stability and battery use
```

### Release-Name

`2.0 – Premium Redesign`

### Kategorie & Tags

- Kategorie: **Tools** (Tools)
- Tags (Play Console, bis zu 5): Taschenlampe / Flashlight, Werkzeuge / Tools, Nützliches / Utilities, Notfall / Emergency, Taschenlampen-App
- Keywords für Beschreibung: Taschenlampe, LED, SOS, Stroboskop, Bildschirmlicht, Flashlight, Torch, Strobe
