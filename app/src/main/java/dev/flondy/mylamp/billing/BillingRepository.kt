package dev.flondy.mylamp.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class BillingState(
    val entitlement: Entitlement = Entitlement.UNKNOWN,
    /** true = aktueller Status stammt direkt von Google Play, false = aus dem Offline-Cache. */
    val verifiedWithPlay: Boolean = false,
    val connected: Boolean = false,
    val billingUnavailable: Boolean = false,
    val formattedPrice: String? = null,
    val productAvailable: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val signatureCheckEnabled: Boolean = false,
    val lastResponseCode: Int? = null,
) {
    val isPremium: Boolean get() = entitlement == Entitlement.ACTIVE
}

sealed interface BillingEvent {
    /** Neuer, bestätigter Kauf aus dem Kaufdialog → einmalige Erfolgsanimation. */
    data object PurchaseCompleted : BillingEvent
    data object PurchasePending : BillingEvent
    data object Restored : BillingEvent
    data object NothingToRestore : BillingEvent
    data object Unavailable : BillingEvent
    data class Error(val responseCode: Int) : BillingEvent
}

/**
 * Kapselt die Google Play Billing Library (v8) für den Einmalkauf [PRODUCT_REMOVE_ADS].
 *
 * Grundsätze:
 * - Freischaltung NUR für Käufe im Zustand PURCHASED (+ optional gültige Signatur).
 * - Jeder freigeschaltete Kauf wird acknowledged (auch beim App-Start gefundene).
 * - Ausstehende Käufe werden angezeigt, aber nicht freigeschaltet.
 * - Rückerstattete/widerrufene Käufe fehlen in queryPurchases → Premium wird entzogen.
 * - Netzwerkprobleme: Ein zuvor von Play bestätigter Status bleibt bis zu 30 Tage gültig.
 */
class BillingRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val entitlementStore: EntitlementStore,
    private val verifier: PurchaseVerifier,
) {
    private val _state = MutableStateFlow(BillingState(signatureCheckEnabled = verifier.isConfigured))
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private var productDetails: ProductDetails? = null
    private var restoreRequested = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        _state.update { it.copy(purchaseInProgress = false, lastResponseCode = result.responseCode) }
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch {
                handlePurchases(purchases.orEmpty(), fromPurchaseFlow = true)
            }
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                restoreRequested = true
                refreshPurchases()
            }
            else -> _events.tryEmit(BillingEvent.Error(result.responseCode))
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            val cached = entitlementStore.read()
            if (entitlementStore.isCacheUsable(cached) && _state.value.entitlement == Entitlement.UNKNOWN) {
                _state.update { it.copy(entitlement = Entitlement.ACTIVE, verifiedWithPlay = false) }
            }
            connect()
        }
    }

    // ------------------------------------------------------------------ Verbindung

    private fun connect() {
        if (billingClient.isReady) {
            onConnected()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                _state.update { it.copy(lastResponseCode = result.responseCode) }
                if (result.responseCode == BillingResponseCode.OK) {
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    _state.update {
                        it.copy(
                            connected = false,
                            billingUnavailable = result.responseCode == BillingResponseCode.BILLING_UNAVAILABLE,
                            // Ohne Play-Antwort und ohne Cache: kostenloser Modus
                            entitlement = if (it.entitlement == Entitlement.UNKNOWN) Entitlement.NONE else it.entitlement,
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() stellt die Verbindung beim nächsten Aufruf selbst wieder her.
                _state.update { it.copy(connected = false) }
            }
        })
    }

    private fun onConnected() {
        _state.update { it.copy(connected = true, billingUnavailable = false) }
        refreshPurchases()
        queryProductDetails()
    }

    // ------------------------------------------------------------------ Öffentliche API

    /** Gleicht den Kaufstatus mit Google Play ab (App-Start, Rückkehr in den Vordergrund, Wiederherstellen). */
    fun refreshPurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                scope.launch { handlePurchases(purchases, fromPurchaseFlow = false) }
            } else {
                Log.w(TAG, "queryPurchases failed: ${result.responseCode}")
                // Cache bleibt erhalten – keine Werbung für bestätigte Käufer bei Netzproblemen.
                _state.update {
                    it.copy(entitlement = if (it.entitlement == Entitlement.UNKNOWN) Entitlement.NONE else it.entitlement)
                }
                if (restoreRequested) {
                    restoreRequested = false
                    _events.tryEmit(BillingEvent.Error(result.responseCode))
                }
            }
        }
    }

    fun restorePurchases() {
        restoreRequested = true
        refreshPurchases()
    }

    fun queryProductDetails() {
        if (!billingClient.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            val details = detailsResult.productDetailsList.firstOrNull { it.productId == PRODUCT_REMOVE_ADS }
            if (result.responseCode == BillingResponseCode.OK && details != null) {
                productDetails = details
                _state.update {
                    it.copy(
                        productAvailable = true,
                        formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice,
                    )
                }
            } else {
                Log.w(TAG, "Product details unavailable: ${result.responseCode}")
                _state.update { it.copy(productAvailable = false) }
            }
        }
    }

    /** Startet den Google-Play-Kaufdialog. */
    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (!billingClient.isReady || details == null) {
            _events.tryEmit(BillingEvent.Unavailable)
            connect()
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode == BillingResponseCode.OK) {
            _state.update { it.copy(purchaseInProgress = true) }
        } else if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            restorePurchases()
        } else {
            _events.tryEmit(BillingEvent.Error(result.responseCode))
        }
    }

    // ------------------------------------------------------------------ Kaufverarbeitung

    private suspend fun handlePurchases(purchases: List<Purchase>, fromPurchaseFlow: Boolean) {
        val snapshots = purchases.map { it.toSnapshot() }
        val decision = EntitlementEvaluator.evaluate(snapshots, PRODUCT_REMOVE_ADS)
        val wasPremium = _state.value.isPremium && _state.value.verifiedWithPlay

        // Pflicht: Käufe bestätigen, sonst erstattet Google sie nach 3 Tagen automatisch.
        decision.needsAcknowledge.forEach { acknowledge(it.token) }

        when (decision.entitlement) {
            Entitlement.ACTIVE -> decision.activePurchase?.let { entitlementStore.saveActive(it.token) }
            Entitlement.NONE -> entitlementStore.clear() // Rückerstattung / Widerruf
            else -> Unit
        }

        _state.update { it.copy(entitlement = decision.entitlement, verifiedWithPlay = true) }

        if (fromPurchaseFlow) {
            when (decision.entitlement) {
                Entitlement.ACTIVE -> if (!wasPremium) _events.tryEmit(BillingEvent.PurchaseCompleted)
                Entitlement.PENDING -> _events.tryEmit(BillingEvent.PurchasePending)
                else -> Unit
            }
        }
        if (restoreRequested) {
            restoreRequested = false
            _events.tryEmit(
                if (decision.entitlement == Entitlement.ACTIVE) BillingEvent.Restored
                else if (decision.entitlement == Entitlement.PENDING) BillingEvent.PurchasePending
                else BillingEvent.NothingToRestore
            )
        }
    }

    private suspend fun acknowledge(token: String) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
        val result = suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(params) { cont.resume(it) }
        }
        if (result.responseCode != BillingResponseCode.OK) {
            // Wird beim nächsten refreshPurchases() (App-Start/Vordergrund) erneut versucht.
            Log.w(TAG, "Acknowledge failed: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private fun Purchase.toSnapshot() = PurchaseSnapshot(
        products = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseSnapshot.State.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseSnapshot.State.PENDING
            else -> PurchaseSnapshot.State.UNSPECIFIED
        },
        acknowledged = isAcknowledged,
        token = purchaseToken,
        signatureValid = verifier.isValid(originalJson, signature),
    )

    companion object {
        private const val TAG = "BillingRepository"

        /** Bestehende Produkt-ID – NICHT ändern, sonst verlieren Käufer ihre Rechte. */
        const val PRODUCT_REMOVE_ADS = "remove_ads"
    }
}
