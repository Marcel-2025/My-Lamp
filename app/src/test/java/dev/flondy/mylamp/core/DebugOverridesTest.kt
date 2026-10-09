package dev.flondy.mylamp.core

import dev.flondy.mylamp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugOverridesTest {
    @Test
    fun realPurchaseState_isUsedWhenNoSimulation() {
        assertEquals(true, DebugOverrides.effectivePremium(true, PremiumSimulation.REAL))
        assertEquals(false, DebugOverrides.effectivePremium(false, PremiumSimulation.REAL))
    }

    @Test
    fun simulation_onlyAffectsDebugBuilds() {
        val simulated = DebugOverrides.effectivePremium(false, PremiumSimulation.SIMULATE_PREMIUM)
        assertEquals(BuildConfig.DEBUG, simulated)
        val simulatedFree = DebugOverrides.effectivePremium(true, PremiumSimulation.SIMULATE_FREE)
        assertEquals(!BuildConfig.DEBUG, simulatedFree)
    }
}
