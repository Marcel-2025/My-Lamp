package dev.flondy.mylamp.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Adaptiver Anker-Banner.
 *
 * - Die Höhe wird vorab reserviert → kein Springen des Layouts beim Laden.
 * - Genau ein Ad-Request pro Banner-Instanz (kein Neuladen bei Recomposition).
 * - Lifecycle: pause/resume mit der Activity, destroy beim Verlassen der Komposition.
 */
@Composable
fun AdaptiveAdBanner(adUnitId: String, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val context = LocalContext.current
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val adView = remember(adSize, adUnitId) {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdBanner", "Ad failed: ${error.code} ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(factory = { adView })
        }
    }
}
