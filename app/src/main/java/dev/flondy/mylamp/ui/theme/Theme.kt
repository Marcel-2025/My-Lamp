package dev.flondy.mylamp.ui.theme

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val LampColorScheme = darkColorScheme(
    primary = LampColors.Gold,
    onPrimary = LampColors.Night,
    primaryContainer = LampColors.GoldSubtle,
    onPrimaryContainer = LampColors.Gold,
    secondary = LampColors.GoldWarm,
    onSecondary = LampColors.Night,
    tertiary = LampColors.Strobe,
    background = LampColors.Night,
    onBackground = LampColors.Text,
    surface = LampColors.NightRaised,
    onSurface = LampColors.Text,
    surfaceVariant = LampColors.Card,
    onSurfaceVariant = LampColors.TextMuted,
    surfaceContainer = LampColors.Card,
    surfaceContainerHigh = LampColors.CardHigh,
    surfaceContainerHighest = LampColors.CardHigh,
    outline = LampColors.OutlineStrong,
    outlineVariant = LampColors.Outline,
    error = LampColors.Sos,
)

/** Steuert, ob Animationen laufen (Einstellung + System-„Animationen entfernen“). */
@Immutable
data class Motion(val enabled: Boolean) {
    fun <T> spec(animated: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (enabled) animated else snap()
    fun <T> anySpec(animated: AnimationSpec<T>): AnimationSpec<T> = if (enabled) animated else snap()
}

/** Haptisches Feedback, abschaltbar über die Einstellungen. */
class LampHaptics(private val view: View?, private val enabled: Boolean) {
    fun click() = perform(HapticFeedbackConstants.VIRTUAL_KEY)
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)
    fun toggle(on: Boolean) = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (on) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else HapticFeedbackConstants.LONG_PRESS
    )
    fun success() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.LONG_PRESS
    )

    private fun perform(constant: Int) {
        if (enabled) view?.performHapticFeedback(constant)
    }
}

val LocalMotion = staticCompositionLocalOf { Motion(enabled = true) }
val LocalHaptics = staticCompositionLocalOf { LampHaptics(null, false) }

@Composable
fun MyLampTheme(
    animationsEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val systemAnimationsOff = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val motion = remember(animationsEnabled, systemAnimationsOff) {
        Motion(enabled = animationsEnabled && !systemAnimationsOff)
    }
    val haptics = remember(view, hapticsEnabled) { LampHaptics(view, hapticsEnabled) }

    CompositionLocalProvider(LocalMotion provides motion, LocalHaptics provides haptics) {
        MaterialTheme(
            colorScheme = LampColorScheme,
            typography = LampTypography,
            content = content,
        )
    }
}
