package dev.flondy.mylamp.core

import dev.flondy.mylamp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PremiumSimulation { REAL, SIMULATE_PREMIUM, SIMULATE_FREE }

/**
 * Entwickler-Simulation – ausschließlich in Debug-Builds wirksam.
 *
 * Die Simulation verändert NUR die Darstellung (UI/Werbung). Sie wird weder gespeichert noch an
 * den Billing-/Entitlement-Speicher weitergegeben und kann echte Käufe daher nie ersetzen.
 * In Release-Builds ist [simulation] immer [PremiumSimulation.REAL].
 */
object DebugOverrides {
    private val _simulation = MutableStateFlow(PremiumSimulation.REAL)
    val simulation: StateFlow<PremiumSimulation> = _simulation.asStateFlow()

    fun setSimulation(value: PremiumSimulation) {
        if (BuildConfig.DEBUG) _simulation.value = value
    }

    fun effectivePremium(realPremium: Boolean, simulation: PremiumSimulation): Boolean {
        if (!BuildConfig.DEBUG) return realPremium
        return when (simulation) {
            PremiumSimulation.REAL -> realPremium
            PremiumSimulation.SIMULATE_PREMIUM -> true
            PremiumSimulation.SIMULATE_FREE -> false
        }
    }
}
