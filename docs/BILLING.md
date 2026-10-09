# Premium & Google Play Billing

## Produkt

| Feld | Wert |
|---|---|
| Produkt-ID | `remove_ads` (**nicht ändern** – bestehende Käufer würden ihre Rechte verlieren) |
| Typ | Einmalkauf (INAPP), nicht verbrauchbar |
| Bibliothek | `com.android.billingclient:billing-ktx:8.3.0` |
| Preis | wird ausschließlich aus Google Play gelesen (`formattedPrice`) |

Bestehende Käufer von `remove_ads` erhalten automatisch alles, was Premium heute umfasst (werbefrei + Premium-Designs). Es wird nichts entzogen.

## Ablauf

1. **Start:** `EntitlementStore` liefert einen zuvor von Play bestätigten Status (max. 30 Tage alt). Danach verbindet sich der `BillingClient` (`enableAutoServiceReconnection`).
2. **`queryPurchasesAsync(INAPP)`** bei jedem Start und jeder Rückkehr in den Vordergrund:
   - `PURCHASED` (+ gültige Signatur, falls Schlüssel konfiguriert) → **ACTIVE**, Cache wird aktualisiert.
   - Nicht bestätigte Käufe werden **acknowledged**. Vorher passierte das nur im Kaufdialog-Callback, beim Start gefundene Käufe wären nach 3 Tagen automatisch erstattet worden.
   - `PENDING` → Status „ausstehend“, keine Freischaltung.
   - Kauf fehlt (Rückerstattung, Widerruf) → **NONE**, Cache wird gelöscht.
   - Fehler oder offline → letzter bestätigter Status bleibt. Ein Käufer sieht bei Netzproblemen keine Werbung.
3. **Kaufdialog:** Preis und Produkt kommen aus `queryProductDetailsAsync`. Nach erfolgreichem Kauf wird einmalig die Freischaltanimation angezeigt. `ITEM_ALREADY_OWNED` löst eine Wiederherstellung aus.
4. **Wiederherstellen:** Button auf der Premium-Seite und in den Einstellungen. Er gleicht erneut mit Play ab und meldet „wiederhergestellt“ oder „kein Kauf gefunden“.

Werbung wird nur geladen, wenn die Berechtigung sicher **NONE** (oder PENDING) ist. Im Zustand **UNKNOWN**, also ohne Play-Antwort und ohne Cache, wird keine Anzeige angefordert.

## Sicherheit

- Premium hängt **nicht** an einem frei setzbaren Boolean. Quelle ist immer die Kaufliste von Google Play. Der lokale Cache ist nur eine zeitlich begrenzte Offline-Kulanz und enthält einen SHA-256-Hash des Kauf-Tokens.
- **Signaturprüfung:** Wenn `mylamp.playLicenseKey` gesetzt ist, wird jede Kaufsignatur mit dem RSA-Schlüssel der App geprüft. Ungültige Signaturen schalten nicht frei.
- **Empfehlung für später:** Eine serverseitige Prüfung über die Google Play Developer API (`purchases.products.get`) plus Real-Time Developer Notifications (RTDN) für Erstattungen. Dafür braucht es ein Backend, zum Beispiel Firebase Cloud Functions.
- **Debug-Simulation** (`DebugOverrides`) wirkt nur in Debug-Builds, ändert nur die Darstellung und wird nie gespeichert oder an Billing weitergegeben.

## Hinweise zu Play-Anforderungen

- Google Play verlangt für neue Apps und Updates eine aktuelle Billing-Library. Version 8 erfüllt die Anforderungen zum Stand dieses Updates. Prüfe vor jedem Release die aktuelle Deadline in der Play Console. **Billing 9.x** ist bereits verfügbar und ein sinnvolles nächstes Upgrade.
- `enablePendingPurchases(PendingPurchasesParams…enableOneTimeProducts())` ist seit PBL 7 Pflicht und gesetzt.
