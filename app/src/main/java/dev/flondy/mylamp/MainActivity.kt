package dev.flondy.mylamp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.flondy.mylamp.data.AppSettings
import dev.flondy.mylamp.sensor.ShakeDetector
import dev.flondy.mylamp.ui.LampApp
import dev.flondy.mylamp.ui.MainViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private lateinit var shakeDetector: ShakeDetector
    private var currentBars: Pair<Boolean, Boolean> = false to true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-Edge (ab Android 15 ohnehin erzwungen) mit hellen Symbolen auf dunklem Grund.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        shakeDetector = ShakeDetector(this) { viewModel.onShake() }
        if (savedInstanceState == null) viewModel.applyStartBehaviorOnce()

        setContent {
            LampApp(
                viewModel = viewModel,
                adUnitId = (application as MyLampApp).container.adsManager.bannerAdUnitId,
                shakeAvailable = shakeDetector.isAvailable,
                onExit = { finish() },
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Systemleisten gemäß Einstellungen
                launch {
                    viewModel.uiState
                        .filter { it.settingsLoaded }
                        .map { it.settings.fullscreen to it.settings.showNavigationBar }
                        .distinctUntilChanged()
                        .collect { (fullscreen, showNav) ->
                            currentBars = fullscreen to showNav
                            applySystemBars()
                        }
                }
                // Shake-Erkennung nur im Vordergrund und nur wenn aktiviert (batterieschonend)
                launch {
                    viewModel.uiState
                        .map { it.settings }
                        .map { s: AppSettings -> if (s.shakeEnabled) s.shakeSensitivity else null }
                        .distinctUntilChanged()
                        .collect { sensitivity ->
                            if (sensitivity != null) shakeDetector.start(sensitivity) else shakeDetector.stop()
                        }
                }
                // Consent + Mobile Ads SDK nur für Nutzer, die Werbung sehen
                launch {
                    viewModel.uiState
                        .map { it.showAds }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect { viewModel.gatherConsent(this@MainActivity) }
                }
                try {
                    awaitCancellation()
                } finally {
                    shakeDetector.stop()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        viewModel.onAppBackgrounded()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBars()
    }

    override fun onDestroy() {
        // Beim echten Beenden (nicht bei Drehung) Licht und Muster sicher ausschalten.
        if (isFinishing) viewModel.onAppExit()
        super.onDestroy()
    }

    private fun applySystemBars() {
        val (fullscreen, showNav) = currentBars
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        when {
            fullscreen -> controller.hide(WindowInsetsCompat.Type.systemBars())
            !showNav -> {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
            else -> controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
