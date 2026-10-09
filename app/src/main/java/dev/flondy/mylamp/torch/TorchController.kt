package dev.flondy.mylamp.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class LightMode { OFF, TORCH, SOS, STROBE }

enum class TorchError { NO_FLASH, CAMERA_IN_USE, UNKNOWN }

/**
 * Zentrale, app-weite Steuerung des LED-Blitzes.
 *
 * - Genau eine Instanz (über AppContainer) → Activity, ViewModel und Quick-Settings-Tile
 *   teilen sich denselben Zustand.
 * - Es läuft immer höchstens EIN Muster-Job (SOS/Strobe). Ein neuer Job wartet, bis der alte
 *   vollständig beendet ist (cancelAndJoin) → keine überlappenden Blink-Jobs.
 * - Externe Änderungen (System-Schnelleinstellung, andere Apps) werden über den TorchCallback
 *   synchronisiert.
 * - Für setTorchMode() wird KEINE Kamera-Berechtigung benötigt.
 */
class TorchController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())

    /** ID der Kamera mit LED-Blitz (bevorzugt Rückkamera) oder null, wenn kein Blitz vorhanden ist. */
    val cameraId: String? = findFlashCamera()
    val hasFlash: Boolean get() = cameraId != null

    /** Maximale Helligkeitsstufe (Android 13+). 1 = nicht regelbar. */
    val maxStrengthLevel: Int = readMaxStrength()
    val supportsStrength: Boolean get() = maxStrengthLevel > 1

    private val _mode = MutableStateFlow(LightMode.OFF)
    val mode: StateFlow<LightMode> = _mode.asStateFlow()

    /** Tatsächlicher Hardware-Zustand der LED (flackert bei SOS/Strobe). */
    private val _ledOn = MutableStateFlow(false)
    val ledOn: StateFlow<Boolean> = _ledOn.asStateFlow()

    private val _strobeHz = MutableStateFlow(LightPatterns.STROBE_DEFAULT_HZ)
    val strobeHz: StateFlow<Float> = _strobeHz.asStateFlow()

    /** Gewünschte LED-Helligkeit 0..1 (nur wirksam, wenn [supportsStrength]). */
    @Volatile
    private var strengthFraction: Float = 1f

    private val _errors = MutableSharedFlow<TorchError>(extraBufferCapacity = 4)
    val errors: SharedFlow<TorchError> = _errors.asSharedFlow()

    private var patternJob: Job? = null
    private var lastInternalChange = 0L

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            _ledOn.value = enabled
            // Eigene Schaltvorgänge (und deren verspätete Callbacks) ignorieren.
            if (SystemClock.elapsedRealtime() - lastInternalChange < EXTERNAL_SYNC_GRACE_MS) return
            when (_mode.value) {
                LightMode.OFF -> if (enabled) _mode.value = LightMode.TORCH
                LightMode.TORCH -> if (!enabled) _mode.value = LightMode.OFF
                else -> Unit // Muster laufen selbst
            }
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id != cameraId) return
            _ledOn.value = false
            if (_mode.value != LightMode.OFF) {
                patternJob?.cancel()
                _mode.value = LightMode.OFF
                _errors.tryEmit(TorchError.CAMERA_IN_USE)
            }
        }
    }

    init {
        if (cameraId != null) {
            try {
                cameraManager?.registerTorchCallback(torchCallback, mainHandler)
            } catch (e: Exception) {
                Log.w(TAG, "registerTorchCallback failed", e)
            }
        }
    }

    // ------------------------------------------------------------------ Öffentliche API

    fun toggleTorch() {
        if (_mode.value == LightMode.TORCH) stop() else startTorch()
    }

    fun startTorch() {
        if (!ensureFlash()) return
        launchExclusive(LightMode.TORCH) {
            setLed(true)
        }
    }

    fun toggleSos() {
        if (_mode.value == LightMode.SOS) stop() else startSos()
    }

    fun startSos() {
        if (!ensureFlash()) return
        launchExclusive(LightMode.SOS) {
            while (isActive) {
                for (step in LightPatterns.SOS) {
                    if (!setLed(step.on)) return@launchExclusive
                    delay(step.durationMs)
                }
            }
        }
    }

    fun toggleStrobe() {
        if (_mode.value == LightMode.STROBE) stop() else startStrobe()
    }

    fun startStrobe() {
        if (!ensureFlash()) return
        launchExclusive(LightMode.STROBE) {
            var on = false
            while (isActive) {
                on = !on
                if (!setLed(on)) return@launchExclusive
                delay(LightPatterns.strobeHalfPeriodMs(_strobeHz.value))
            }
        }
    }

    fun setStrobeHz(hz: Float) {
        _strobeHz.value = LightPatterns.clampStrobeHz(hz)
    }

    fun setStrength(fraction: Float) {
        strengthFraction = fraction.coerceIn(0.05f, 1f)
        if (_mode.value == LightMode.TORCH) {
            scope.launch { setLed(true) }
        }
    }

    /** Schaltet jedes Muster und die LED sicher aus. */
    fun stop() {
        val previous = patternJob
        _mode.value = LightMode.OFF
        markInternal()
        patternJob = scope.launch {
            previous?.cancelAndJoin()
            setLed(false)
        }
    }

    // ------------------------------------------------------------------ Intern

    private fun ensureFlash(): Boolean {
        if (cameraId == null) {
            _errors.tryEmit(TorchError.NO_FLASH)
            return false
        }
        return true
    }

    private fun launchExclusive(mode: LightMode, block: suspend CoroutineScope.() -> Unit) {
        val previous = patternJob
        _mode.value = mode
        markInternal()
        patternJob = scope.launch {
            previous?.cancelAndJoin()
            try {
                block()
            } finally {
                // Bei Abbruch eines Musters LED immer ausschalten – außer im Dauerlicht-Modus.
                if (mode != LightMode.TORCH) {
                    withContext(NonCancellable) { setLed(false) }
                }
            }
        }
    }

    private fun markInternal() {
        lastInternalChange = SystemClock.elapsedRealtime()
    }

    /** @return false, wenn die Hardware nicht geschaltet werden konnte (Muster wird dann beendet). */
    private fun setLed(on: Boolean): Boolean {
        val id = cameraId ?: return false
        val manager = cameraManager ?: return false
        markInternal()
        return try {
            if (on && supportsStrength && strengthFraction < 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val level = (strengthFraction * maxStrengthLevel).roundToInt().coerceIn(1, maxStrengthLevel)
                manager.turnOnTorchWithStrengthLevel(id, level)
            } else {
                manager.setTorchMode(id, on)
            }
            _ledOn.value = on
            true
        } catch (e: CameraAccessException) {
            Log.w(TAG, "setTorchMode failed", e)
            failAndReset(TorchError.CAMERA_IN_USE)
            false
        } catch (e: Exception) {
            Log.w(TAG, "setTorchMode failed", e)
            failAndReset(TorchError.UNKNOWN)
            false
        }
    }

    private fun failAndReset(error: TorchError) {
        _ledOn.value = false
        _mode.value = LightMode.OFF
        _errors.tryEmit(error)
    }

    private fun findFlashCamera(): String? {
        val manager = cameraManager ?: return null
        return try {
            val withFlash = manager.cameraIdList.filter { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            withFlash.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: withFlash.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Camera enumeration failed", e)
            null
        }
    }

    private fun readMaxStrength(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1
        val id = cameraId ?: return 1
        return try {
            cameraManager?.getCameraCharacteristics(id)
                ?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } catch (e: Exception) {
            1
        }
    }

    companion object {
        private const val TAG = "TorchController"
        private const val EXTERNAL_SYNC_GRACE_MS = 400L
    }
}
