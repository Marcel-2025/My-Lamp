package dev.flondy.mylamp.billing

/** Premium-Berechtigung, wie sie die App nach außen kommuniziert. */
enum class Entitlement {
    /** Noch keine Antwort von Google Play und kein gültiger Cache. Es wird KEINE Werbung geladen. */
    UNKNOWN,
    /** Kein (gültiger) Kauf vorhanden. */
    NONE,
    /** Kauf ist ausstehend (z. B. Barzahlung) – noch nicht freigeschaltet. */
    PENDING,
    /** Kauf ist abgeschlossen und geprüft. */
    ACTIVE,
}

/** Vereinfachte, Android-unabhängige Sicht auf einen Kauf (testbar). */
data class PurchaseSnapshot(
    val products: List<String>,
    val state: State,
    val acknowledged: Boolean,
    val token: String,
    val signatureValid: Boolean,
) {
    enum class State { PURCHASED, PENDING, UNSPECIFIED }
}

data class EntitlementDecision(
    val entitlement: Entitlement,
    /** Kauf, der freigeschaltet wurde (für Cache/Acknowledge). */
    val activePurchase: PurchaseSnapshot? = null,
    /** Käufe, die noch bestätigt (acknowledged) werden müssen – sonst erstattet Google nach 3 Tagen. */
    val needsAcknowledge: List<PurchaseSnapshot> = emptyList(),
)

object EntitlementEvaluator {

    /**
     * Ermittelt die Berechtigung ausschließlich aus der von Google Play gelieferten Kaufliste.
     * Erstattete oder widerrufene Käufe tauchen dort nicht mehr auf → Ergebnis NONE.
     */
    fun evaluate(purchases: List<PurchaseSnapshot>, productId: String): EntitlementDecision {
        val relevant = purchases.filter { productId in it.products }
        val purchased = relevant.firstOrNull {
            it.state == PurchaseSnapshot.State.PURCHASED && it.signatureValid
        }
        if (purchased != null) {
            return EntitlementDecision(
                entitlement = Entitlement.ACTIVE,
                activePurchase = purchased,
                needsAcknowledge = relevant.filter {
                    it.state == PurchaseSnapshot.State.PURCHASED && it.signatureValid && !it.acknowledged
                },
            )
        }
        if (relevant.any { it.state == PurchaseSnapshot.State.PENDING }) {
            return EntitlementDecision(Entitlement.PENDING)
        }
        return EntitlementDecision(Entitlement.NONE)
    }
}
