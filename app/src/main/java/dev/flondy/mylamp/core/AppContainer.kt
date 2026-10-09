package dev.flondy.mylamp.core

import android.content.Context
import dev.flondy.mylamp.BuildConfig
import dev.flondy.mylamp.ads.AdsManager
import dev.flondy.mylamp.billing.BillingRepository
import dev.flondy.mylamp.billing.EntitlementStore
import dev.flondy.mylamp.billing.PurchaseVerifier
import dev.flondy.mylamp.data.SettingsRepository
import dev.flondy.mylamp.torch.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Einfache manuelle Dependency Injection. Alle Objekte leben so lange wie der App-Prozess.
 * Teure Objekte (Billing, Ads) werden erst beim ersten Zugriff erzeugt.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val torchController: TorchController by lazy { TorchController(appContext, appScope) }

    val billingRepository: BillingRepository by lazy {
        BillingRepository(
            context = appContext,
            scope = appScope,
            entitlementStore = EntitlementStore(appContext),
            verifier = PurchaseVerifier(BuildConfig.PLAY_LICENSE_KEY),
        )
    }

    val adsManager: AdsManager by lazy { AdsManager(appContext, appScope) }
}
