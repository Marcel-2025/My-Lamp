package dev.flondy.mylamp.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementEvaluatorTest {
    private val id = "remove_ads"

    private fun purchase(
        state: PurchaseSnapshot.State = PurchaseSnapshot.State.PURCHASED,
        ack: Boolean = true,
        sig: Boolean = true,
        products: List<String> = listOf(id),
        token: String = "t1",
    ) = PurchaseSnapshot(products, state, ack, token, sig)

    @Test
    fun noPurchases_isNone() {
        assertEquals(Entitlement.NONE, EntitlementEvaluator.evaluate(emptyList(), id).entitlement)
    }

    @Test
    fun purchased_isActive() {
        val d = EntitlementEvaluator.evaluate(listOf(purchase()), id)
        assertEquals(Entitlement.ACTIVE, d.entitlement)
        assertTrue(d.needsAcknowledge.isEmpty())
    }

    @Test
    fun unacknowledgedPurchase_isActiveAndMustBeAcknowledged() {
        val d = EntitlementEvaluator.evaluate(listOf(purchase(ack = false)), id)
        assertEquals(Entitlement.ACTIVE, d.entitlement)
        assertEquals(1, d.needsAcknowledge.size)
    }

    @Test
    fun pending_isNotUnlocked() {
        val d = EntitlementEvaluator.evaluate(listOf(purchase(state = PurchaseSnapshot.State.PENDING, ack = false)), id)
        assertEquals(Entitlement.PENDING, d.entitlement)
        assertTrue(d.needsAcknowledge.isEmpty())
    }

    @Test
    fun invalidSignature_isNotUnlocked() {
        assertEquals(Entitlement.NONE, EntitlementEvaluator.evaluate(listOf(purchase(sig = false)), id).entitlement)
    }

    @Test
    fun otherProduct_isIgnored() {
        assertEquals(Entitlement.NONE, EntitlementEvaluator.evaluate(listOf(purchase(products = listOf("other"))), id).entitlement)
    }

    @Test
    fun unspecifiedState_isNotUnlocked() {
        assertEquals(Entitlement.NONE, EntitlementEvaluator.evaluate(listOf(purchase(state = PurchaseSnapshot.State.UNSPECIFIED)), id).entitlement)
    }

    @Test
    fun refundedPurchaseDisappears_entitlementRevoked() {
        // Nach Rückerstattung liefert Google Play den Kauf nicht mehr → NONE
        assertEquals(Entitlement.NONE, EntitlementEvaluator.evaluate(emptyList(), id).entitlement)
    }
}
