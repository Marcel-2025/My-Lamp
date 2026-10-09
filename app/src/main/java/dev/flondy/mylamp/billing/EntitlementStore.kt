package dev.flondy.mylamp.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

private val Context.entitlementDataStore: DataStore<Preferences> by preferencesDataStore(name = "entitlement")

/**
 * Zwischenspeicher für eine VON GOOGLE PLAY bestätigte Premium-Berechtigung.
 *
 * Der Cache ist nie die alleinige Quelle der Wahrheit: Er wird nur genutzt, solange Google Play
 * nicht erreichbar ist, und verfällt nach [MAX_CACHE_AGE_MS]. Sobald Play antwortet, wird er
 * überschrieben (z. B. bei Rückerstattung gelöscht).
 */
class EntitlementStore(context: Context) {
    private val store = context.applicationContext.entitlementDataStore

    private object Keys {
        val ACTIVE = booleanPreferencesKey("premium_active")
        val TOKEN_HASH = stringPreferencesKey("token_hash")
        val VERIFIED_AT = longPreferencesKey("verified_at")
    }

    data class Cached(val active: Boolean, val verifiedAt: Long, val tokenHash: String?)

    suspend fun read(): Cached {
        val p = try {
            store.data.first()
        } catch (e: Exception) {
            return Cached(false, 0L, null)
        }
        return Cached(
            active = p[Keys.ACTIVE] ?: false,
            verifiedAt = p[Keys.VERIFIED_AT] ?: 0L,
            tokenHash = p[Keys.TOKEN_HASH],
        )
    }

    fun isCacheUsable(cached: Cached, now: Long = System.currentTimeMillis()): Boolean =
        cached.active && cached.tokenHash != null && now - cached.verifiedAt in 0..MAX_CACHE_AGE_MS

    suspend fun saveActive(purchaseToken: String) {
        runCatching {
            store.edit {
                it[Keys.ACTIVE] = true
                it[Keys.TOKEN_HASH] = sha256(purchaseToken)
                it[Keys.VERIFIED_AT] = System.currentTimeMillis()
            }
        }
    }

    suspend fun clear() {
        runCatching { store.edit { it.clear() } }
    }

    companion object {
        /** Offline-Kulanz für bereits bestätigte Käufer: 30 Tage. */
        const val MAX_CACHE_AGE_MS = 30L * 24 * 60 * 60 * 1000

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
