# My Lamp – Ideenliste für den Ausbau

Stand: 17.11.2026 (Version 2.2 „Lichtstudio“).

Status:
- **eingebaut**: im Code der Version 2.2
- **teilweise**: Grundlage ist eingebaut, ein Teil fehlt noch
- **offen**: noch nicht gebaut
- **braucht Entscheidung**: Marcel muss vorher etwas festlegen oder in der Play Console anlegen

Regeln für den Ausbau:
- Die Grundfunktionen bleiben kostenlos: LED, SOS, Stroboskop, Bildschirmlicht in fünf Farben, Morse, Warnblinker, Auto-Aus, Akku-Anzeige, Widget und Kachel.
- Premium läuft weiter nur über den Einmalkauf `remove_ads` bei Google Play. Es gibt kein Abo, keine Lootboxen und keine Zufallsinhalte.

## 1. Funktionen

| Idee | Status | Hinweis |
|---|---|---|
| Morse-Nachricht: eigenen Text als Morsecode blinken (A–Z, 0–9, Satzzeichen) | eingebaut | kostenlos, mit Code-Vorschau vor dem Start |
| Lichteffekt „Warnblinker“ (1 Hz) | eingebaut | kostenlos |
| Lichteffekte „Herzschlag“, „Leuchtfeuer“, „Signal“ | eingebaut | Premium, alle unter 3 Blitzen pro Sekunde |
| Auto-Aus-Timer (1, 5, 15, 30, 60 Minuten) mit Countdown am Power-Button | eingebaut | gilt für LED-Modi und Bildschirmlicht |
| Lautstärketasten schalten das Licht | eingebaut | Einstellung, standardmäßig aus |
| Akku-Anzeige auf dem Startbildschirm, Hinweis ab 15 % | eingebaut | |
| Startbildschirm-Widget „Licht an/aus“ (1×1) | eingebaut | zusätzlich zur Schnelleinstellungs-Kachel |
| Kachel und Widget zeigen auch Morse und Effekte als Status | eingebaut | |
| Tasker-/Shortcut-Aktionen (App-Shortcuts lange drücken: SOS, Bildschirmlicht) | offen | |
| Kompass bzw. Notfall-Infokarte (Standort als Text) | offen | Standort braucht eine neue Berechtigung, deshalb erst nach Entscheidung |
| Licht per Sprachbefehl / Google Assistant | offen | |
| Wear-OS-Fernbedienung | offen | eigenes Modul nötig |
| Statistik „Leuchtdauer“ | offen | wenig Nutzen, vorerst nicht |

## 2. Designs, Optik und Skins

| Idee | Status | Hinweis |
|---|---|---|
| Vier animierte Designs: Polarlicht, Sternennacht, Glut, Ozean | eingebaut | Premium, laufen nur bei aktivierten Animationen, sonst Standbild |
| Sechs Button-Skins für den Power-Button: Chrom, Neon, Rosé, Smaragd, Lava, Polarlicht | eingebaut | Premium, „Gold“ bleibt kostenlos |
| Polarlicht-Skin mit drehendem Farbring | eingebaut | |
| Bildschirmlicht-Farben Grün (Nachtsicht), Violett, Cyan, Rosa | eingebaut | Premium |
| Eigene Bildschirmfarbe über einen Farbton-Regler | eingebaut | Premium |
| Bildschirm-Effekte: Regenbogen, Kerzenlicht, Party, Warnlicht | eingebaut | Premium, langsam und ohne Flackern (Warnlicht 1 Hz) |
| Weitere Foto-Designs (Strand, Berge, Stadt bei Nacht) | offen | braucht echte, lizenzfreie Fotos |
| Saisonale Designs (Halloween, Winter) | offen | |
| Eigenes Foto als Hintergrund | offen | braucht Bildauswahl, keine Berechtigung nötig (Photo Picker) |

## 3. Shop und Premium-Inhalte

| Idee | Status | Hinweis |
|---|---|---|
| „Studio & Shop“: alle Designs, Skins, Lichteffekte und Bildschirm-Inhalte auf einer Seite, nach Kategorien | eingebaut | für Premium-Nutzer zugleich der Ort zum Auswählen |
| Live-Vorschau jedes Premium-Inhalts für 30 Sekunden (Design, Skin, Bildschirmlicht) bzw. 10 Sekunden (LED-Effekt) | eingebaut | danach geht alles automatisch zurück |
| Vorschau-Leiste mit Restzeit und Knopf „Freischalten“ | eingebaut | |
| Premium-Seite listet die neuen Inhalte mit echter Anzahl aus dem Katalog | eingebaut | |
| Einzelkäufe (z. B. nur das Skin-Paket) als weitere Produkte | braucht Entscheidung | neue Produkte müssen in der Play Console angelegt werden; bisher gibt es nur `remove_ads` |
| „Unterstützer“-Trinkgeld als freiwilliges Produkt | braucht Entscheidung | ebenfalls ein neues Produkt in der Play Console |
| Rabatt-Aktionen über die Play Console (Preisaktion für `remove_ads`) | braucht Entscheidung | geht ohne App-Update, nur in der Play Console |

## 4. Monetarisierung

| Idee | Status | Hinweis |
|---|---|---|
| Mehr Gründe für den Einmalkauf durch Shop und Vorschau | eingebaut | |
| Banner bleibt unten, keine Vollbild-Werbung | eingebaut | wie bisher, für eine Notfall-App angemessen |
| Belohnte Werbung („Video ansehen = 24 h Premium-Design“) | braucht Entscheidung | wäre Werbung statt Kauf, deshalb noch nicht eingebaut |
| Interstitial-Werbung | nicht geplant | stört in Notfällen |

## 5. Vorschau und Animationen

| Idee | Status | Hinweis |
|---|---|---|
| Animierte Mini-Vorschau auf jeder Shop-Karte (Design läuft, Skin leuchtet, LED-Effekt blinkt als Punkt im echten Takt) | eingebaut | |
| Shop-Karten erscheinen nacheinander | eingebaut | |
| Countdown-Ring des Auto-Aus um den Power-Button | eingebaut | |
| Morse-Vorschau mit Punkten und Strichen | eingebaut | |
| Power-Button erscheint beim Start mit einer kurzen Einblendung | eingebaut | |
| Alle neuen Animationen respektieren die Einstellung „Animationen“ und die System-Einstellung | eingebaut | |

## 6. Verbesserungen

| Idee | Status | Hinweis |
|---|---|---|
| Rundes App-Icon an das neue Motiv anpassen, blauer Rand im adaptiven Icon | offen | aus dem Stand vom 05.11. |
| App-Name vereinheitlichen („My Lamp“ / „LED Taschenlampe Pro“) | braucht Entscheidung | |
| Datenschutz-URL eintragen | braucht Entscheidung | URL fehlt noch |
| Test auf echtem Gerät (LED-Effekte, Widget, Lautstärketasten) | offen | im Container nicht prüfbar |
| Windows-Version | offen | My Lamp hat keine Desktop-Version; eine LED gibt es am PC nicht, sinnvoll wäre nur das Bildschirmlicht |
