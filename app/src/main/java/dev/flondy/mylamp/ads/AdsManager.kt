package dev.flondy.mylamp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dev.flondy.mylamp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class AdsState(
    /** Eine gültige Banner-ID ist konfiguriert (Debug: Google-Test-ID). */
    val configured: Boolean,
    /** Consent-Abfrage (UMP) abgeschlossen bzw. nicht erforderlich. */
    val consentResolved: Boolean = false,
    /** Laut UMP dürfen Anzeigen angefordert werden. */
    val canRequestAds: Boolean = false,
    /** Mobile Ads SDK ist initialisiert. */
    val sdkInitialized: Boolean = false,
    /** Nutzer muss Datenschutzoptionen jederzeit erreichen können (EWR/UK). */
    val privacyOptionsRequired: Boolean = false,
    val lastError: String? = null,
)

/**
 * Verwaltet Einwilligung (Google User Messaging Platform) und die Initialisierung des Mobile Ads SDK.
 *
 * - Das SDK wird erst initialisiert, wenn tatsächlich Werbung gezeigt werden soll (kostenlose
 *   Nutzer). Premium-Nutzer lösen weder Consent-Dialog noch SDK-Start noch Ad-Requests aus.
 * - Ist im Release keine Banner-ID hinterlegt, bleibt Werbung vollständig deaktiviert.
 */
class AdsManager(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    val bannerAdUnitId: String = BuildConfig.ADMOB_BANNER_ID

    private val _state = MutableStateFlow(
        AdsState(configured = bannerAdUnitId.startsWith("ca-app-pub-") && !bannerAdUnitId.contains("XXXX"))
    )
    val state: StateFlow<AdsState> = _state.asStateFlow()

    private val consentStarted = AtomicBoolean(false)
    private val sdkStarted = AtomicBoolean(false)

    /**
     * Startet die Consent-Abfrage (einmal pro Prozess). Muss mit einer Activity aufgerufen werden,
     * da UMP ggf. ein Formular anzeigt.
     */
    fun gatherConsentAndInit(activity: Activity) {
        if (!_state.value.configured) return
        if (!consentStarted.compareAndSet(false, true)) {
            if (consentInformation.canRequestAds()) initializeSdk()
            return
        }
        // Bereits erteilte Einwilligung aus vorheriger Sitzung → sofort starten.
        if (consentInformation.canRequestAds()) initializeSdk()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form: ${formError.errorCode} ${formError.message}")
                    onConsentResolved(formError?.message)
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                onConsentResolved(requestError.message)
            }
        )
    }

    private fun onConsentResolved(error: String?) {
        val canRequest = consentInformation.canRequestAds()
        _state.value = _state.value.copy(
            consentResolved = true,
            canRequestAds = canRequest,
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            lastError = error,
        )
        if (canRequest) initializeSdk()
    }

    private fun initializeSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        _state.value = _state.value.copy(canRequestAds = true)
        scope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(appContext) {
                    _state.value = _state.value.copy(sdkInitialized = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "MobileAds init failed", e)
                sdkStarted.set(false)
                _state.value = _state.value.copy(lastError = e.message)
            }
        }
    }

    /** Öffnet das UMP-Formular für Datenschutzoptionen (Pflicht-Einstieg in den Einstellungen). */
    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            _state.value = _state.value.copy(canRequestAds = consentInformation.canRequestAds())
            onDone(formError?.message)
        }
    }

    /** Nur Debug: Einwilligung zurücksetzen, um den Dialog erneut zu testen. */
    fun resetConsentForDebug() {
        if (!BuildConfig.DEBUG) return
        consentInformation.reset()
        consentStarted.set(false)
        _state.value = _state.value.copy(consentResolved = false, privacyOptionsRequired = false)
    }

    private companion object {
        const val TAG = "AdsManager"
    }
}
