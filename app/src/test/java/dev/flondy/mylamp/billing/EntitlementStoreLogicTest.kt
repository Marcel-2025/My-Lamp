package dev.flondy.mylamp.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EntitlementStoreLogicTest {
    @Test
    fun tokenHashIsStableAndNotPlainText() {
        val h = EntitlementStore.sha256("token-123")
        assertEquals(64, h.length)
        assertEquals(h, EntitlementStore.sha256("token-123"))
        assertNotEquals("token-123", h)
    }

    @Test
    fun cacheGraceIsThirtyDays() {
        assertEquals(30L * 24 * 60 * 60 * 1000, EntitlementStore.MAX_CACHE_AGE_MS)
    }
}
