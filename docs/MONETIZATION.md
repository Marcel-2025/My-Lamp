# Monetarisierung

## Ist-Zustand (nach diesem Update)

| Baustein | Umsetzung |
|---|---|
| Werbung | Ein adaptiver Anker-Banner unten, nur für kostenlose Nutzer, nur nach UMP-Consent bzw. wenn Consent nicht nötig ist |
| Einmalkauf | `remove_ads` → Premium: werbefrei, 4 Premium-Designs, goldene Optik |
| Paywall | Eigene Seite mit echtem Play-Preis, „Einmalkauf · kein Abo“, Wiederherstellen-Button, kein Zeitdruck |
| Einstiegspunkte | Dezente „Premium“-Pille oben rechts (nur kostenlos), Premium-Karte in den Einstellungen, gesperrte Designs |

Bewusst **nicht** eingebaut: Interstitials, Rewarded Ads, App-Open-Ads und Abos. Bei einer Taschenlampe, die im Notfall sofort funktionieren muss, würden sie Bewertungen und Bindung stärker schaden als Umsatz bringen.

## Vorschläge (nicht umgesetzt, Entscheidung beim Entwickler)

1. **Preis-Check:** Bei einem einzigen Produkt hängt der Umsatz stark am Preis. Ein A/B-Test über die Play-Console-Preisexperimente ist mit wenig Aufwand möglich.
2. **„Supporter“-Spende (optional, Einmalkauf):** z. B. `support_small` / `support_large` als reine Danke-Käufe ohne Zusatzfunktion. Das ist transparent und wird oft gut angenommen.
3. **Design-Pakete als eigene Einmalkäufe:** Nur sinnvoll, wenn regelmäßig neue Designs erscheinen. Bestehende `remove_ads`-Käufer sollten alle Pakete erhalten, die zum Kaufzeitpunkt beworben wurden.
4. **Premium-Widget** (Homescreen-Widget mit Ein/Aus und Akzentfarbe): echter Mehrwert, technisch mit Glance umsetzbar. Empfohlen als nächstes Premium-Feature.
5. **Kein Abo** für Grundfunktionen. Taschenlampe, SOS, Stroboskop und Bildschirmlicht bleiben dauerhaft kostenlos.

## Kennzahlen zum Beobachten

- Conversion „Paywall geöffnet → Kauf“ (Play Console: Käufe / Firebase Analytics, falls später ergänzt)
- Ad-Fill-Rate und eCPM in AdMob
- Bewertungen mit Stichwort „Werbung“
