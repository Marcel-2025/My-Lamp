package dev.flondy.mylamp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Reine Schüttel-Logik (testbar ohne Android).
 *
 * Ein Schütteln wird erst erkannt, wenn innerhalb von [windowMs] mindestens [requiredPeaks]
 * Beschleunigungsspitzen über [thresholdG] auftreten. Danach greift eine Sperrzeit
 * ([cooldownMs]), damit ein einzelnes Schütteln die Lampe nicht mehrfach umschaltet.
 * Einzelne Stöße (z. B. Handy auf den Tisch legen) lösen dadurch nicht aus.
 */
class ShakeLogic(
    var thresholdG: Float = 2.6f,
    private val requiredPeaks: Int = 3,
    private val windowMs: Long = 700L,
    private val minPeakSpacingMs: Long = 80L,
    private val cooldownMs: Long = 1500L,
) {
    private val peaks = ArrayDeque<Long>()
    private var lastTrigger = Long.MIN_VALUE / 2

    /** @return true, wenn mit diesem Messwert ein Schütteln erkannt wurde. */
    fun onSample(timestampMs: Long, x: Float, y: Float, z: Float): Boolean {
        if (timestampMs - lastTrigger < cooldownMs) return false
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (gForce < thresholdG) return false

        if (peaks.isNotEmpty() && timestampMs - peaks.last() < minPeakSpacingMs) return false
        peaks.addLast(timestampMs)
        while (peaks.isNotEmpty() && timestampMs - peaks.first() > windowMs) peaks.removeFirst()

        if (peaks.size >= requiredPeaks) {
            peaks.clear()
            lastTrigger = timestampMs
            return true
        }
        return false
    }

    fun reset() {
        peaks.clear()
    }
}

enum class ShakeSensitivity(val thresholdG: Float) {
    LOW(3.2f),
    MEDIUM(2.6f),
    HIGH(2.0f),
}

/**
 * Batterieschonender Sensor-Wrapper: Der Beschleunigungssensor wird nur registriert, solange
 * [start] aktiv ist (App im Vordergrund und Funktion eingeschaltet) und nutzt SENSOR_DELAY_UI.
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val logic = ShakeLogic()
    private var running = false

    val isAvailable: Boolean get() = accelerometer != null

    fun start(sensitivity: ShakeSensitivity) {
        logic.thresholdG = sensitivity.thresholdG
        if (running || accelerometer == null) return
        logic.reset()
        running = sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI) == true
    }

    fun stop() {
        if (!running) return
        sensorManager?.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ms = event.timestamp / 1_000_000L
        if (logic.onSample(ms, event.values[0], event.values[1], event.values[2])) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
