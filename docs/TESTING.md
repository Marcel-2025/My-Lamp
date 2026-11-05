# Tests & Qualitätssicherung

## Automatisiert (ausgeführt am 09.10.2026)

| Prüfung | Ergebnis |
|---|---|
| `assembleDebug` | ✅ erfolgreich |
| `bundleRelease` / `assembleRelease` (R8 + Resource-Shrinking) | ✅ erfolgreich, AAB 9,2 MB (vorher 19 MB) |
| `testDebugUnitTest` / `testReleaseUnitTest` | ✅ 23 Tests, 0 Fehler |
| `lintDebug` | ✅ 0 Fehler (verbleibende Warnungen: neuere Bibliotheksversionen verfügbar) |
| 16-KB-Page-Size-Alignment (`zipalign -c -P 16`) | ✅ alle nativen Bibliotheken OK |
| Screenshot-Renderings (Robolectric, nur temporär) | ✅ Start (aus/an/SOS/Strobe), Einstellungen, Paywall, Bildschirmlicht, Freischaltung, Warnhinweis |

Abgedeckt durch Unit-Tests:
- SOS-Muster: 3 kurz / 3 lang / 3 kurz, ITU-Pausen 1/3/7, Gesamtlänge 34 Einheiten
- Strobe: Frequenzgrenzen und Periodendauer
- Shake-Erkennung: Ruhe, Einzelstoß, echtes Schütteln, Sperrzeit, Empfindlichkeitsreihenfolge
- Kauflogik: kein Kauf, gekauft, nicht bestätigt (→ acknowledge), ausstehend, falsche Signatur, fremdes Produkt, Rückerstattung
- Debug-Simulation wirkt nur in Debug-Builds

### Tablet & Querformat (05.11.2026)

Gerendert und visuell geprüft (Robolectric): Handy quer (891 × 411 dp), Tablet quer (1280 × 800 dp) und Tablet hoch
(800 × 1280 dp). Geprüft wurden Start (aus/an/SOS/Strobe), Einstellungen, Paywall, Bildschirmlicht, Warn-Dialog und Freischaltung.
Build, Unit-Tests und Lint wurden danach erneut mit AGP 9.4.1 ausgeführt – alles grün.

Auf echten Geräten noch prüfen:
- [ ] Tablet quer und hoch, Drehen während Licht/SOS/Strobe läuft (Licht muss anbleiben)
- [ ] Handy quer mit aktivem Stroboskop: alle Kacheln sichtbar
- [ ] Splitscreen-Versuch auf dem Handy (sollte nicht angeboten werden) und auf dem Tablet
- [ ] Werbebanner im Querformat (neue Breite nach dem Drehen)

## Manuell auf echtem Gerät (noch durchzuführen)

Diese Punkte brauchen Hardware, Google Play oder einen Tester-Account und wurden **nicht** automatisiert geprüft:

- [ ] App startet ohne Absturz (Debug und signierter Release)
- [ ] LED ein/aus, LED-Helligkeit (Android 13+, Gerät mit Unterstützung)
- [ ] SOS starten/stoppen, Stroboskop starten/stoppen, Warnhinweis erscheint nur beim ersten Mal
- [ ] Strobe-Slider ändert das Tempo live
- [ ] Bildschirmlicht: Farben, Helligkeit, Rückkehr stellt die ursprüngliche Helligkeit wieder her
- [ ] Einstellungen bleiben nach App-Neustart erhalten
- [ ] Normalmodus mit Gesten- **und** Drei-Tasten-Navigation: kein Element unter der Navigationsleiste
- [ ] Vollbild: Leisten ausgeblendet, per Wisch einblendbar, nach Kaufdialog wieder ausgeblendet
- [ ] Quick-Settings-Kachel hinzufügen und schalten, Zustand synchron zur App
- [ ] Shake-to-Toggle (Empfindlichkeit Niedrig/Mittel/Hoch)
- [ ] Debug-Build zeigt Google-**Test**anzeigen („Test Ad“)
- [ ] EWR-Gerät: Consent-Dialog erscheint, „Datenschutzoptionen“ in den Einstellungen sichtbar
- [ ] Premium-Kauf mit Lizenztester (Play Console → Lizenztests), Freischaltanimation erscheint einmal
- [ ] App neu installieren → Kauf wird automatisch erkannt, keine Werbung
- [ ] „Käufe wiederherstellen“ meldet korrekt
- [ ] Ausstehender Kauf (Testkarte „Langsam, Zahlung ausstehend“) schaltet erst nach Abschluss frei
- [ ] Rückerstattung in der Play Console → nach App-Neustart wieder kostenlos
- [ ] Flugmodus nach bestätigtem Kauf → weiterhin keine Werbung

### Hilfen

- **Debug-Bereich** in den Einstellungen (nur Debug-Builds): Premium-/Free-Ansicht simulieren, Billing-/Ads-Status, Freischaltanimation, Hinweise und Consent zurücksetzen.
- Consent-Test außerhalb des EWR: In `AdsManager` vorübergehend `ConsentDebugSettings` mit `DEBUG_GEOGRAPHY_EEA` und der gehashten Test-Geräte-ID aus Logcat setzen.
- Kauf-Tests funktionieren nur mit einer **über Play installierten** Version (interner Test-Track) und einem Lizenztester-Konto.
