package dev.flondy.mylamp.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/** Aktuelle [SystemClock.elapsedRealtime]-Zeit, die nur tickt, solange [active] ist (spart Akku). */
@Composable
fun rememberTicker(active: Boolean, periodMs: Long = 500L): State<Long> {
    val now = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(active, periodMs) {
        now.longValue = SystemClock.elapsedRealtime()
        while (active) {
            delay(periodMs)
            now.longValue = SystemClock.elapsedRealtime()
        }
    }
    return now
}

data class BatteryInfo(val percent: Int, val charging: Boolean) {
    val isLow: Boolean get() = percent in 0..LOW_PERCENT && !charging

    companion object {
        const val LOW_PERCENT = 15
    }
}

/** Akkustand über den „sticky“ Broadcast ACTION_BATTERY_CHANGED – ohne Berechtigung. */
@Composable
fun rememberBattery(): State<BatteryInfo?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<BatteryInfo?>(null) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                state.value = parseBattery(intent)
            }
        }
        val sticky = runCatching {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.getOrNull()
        sticky?.let { state.value = parseBattery(it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}

private fun parseBattery(intent: Intent): BatteryInfo? {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo((level * 100 / scale).coerceIn(0, 100), charging)
}
