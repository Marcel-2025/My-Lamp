package dev.flondy.mylamp.billing

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Lokale Signaturprüfung eines Google-Play-Kaufs mit dem öffentlichen RSA-Schlüssel der App
 * (Play Console → Monetarisierung einrichten → Lizenzierung).
 *
 * Hinweis: Eine lokale Prüfung erschwert Manipulationen, ersetzt aber keine serverseitige
 * Verifizierung über die Google Play Developer API. Siehe docs/BILLING.md.
 *
 * Ist kein Schlüssel konfiguriert ([base64PublicKey] leer), wird die Prüfung übersprungen und
 * der von Google Play gemeldete Kaufstatus gilt als maßgeblich.
 */
class PurchaseVerifier(private val base64PublicKey: String) {

    val isConfigured: Boolean get() = base64PublicKey.isNotBlank()

    private val publicKey: PublicKey? by lazy {
        if (!isConfigured) return@lazy null
        try {
            val decoded = Base64.decode(base64PublicKey, Base64.DEFAULT)
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decoded))
        } catch (e: Exception) {
            Log.e(TAG, "Invalid Play license key – signature verification disabled", e)
            null
        }
    }

    fun isValid(signedData: String, signature: String): Boolean {
        if (!isConfigured) return true
        val key = publicKey ?: return true // Fehlkonfiguration darf Käufer nicht aussperren
        if (signedData.isEmpty() || signature.isEmpty()) return false
        return try {
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initVerify(key)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            sig.verify(Base64.decode(signature, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.w(TAG, "Signature verification failed", e)
            false
        }
    }

    private companion object {
        const val TAG = "PurchaseVerifier"
    }
}
