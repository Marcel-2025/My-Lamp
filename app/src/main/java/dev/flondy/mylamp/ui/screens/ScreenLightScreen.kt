package dev.flondy.mylamp.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Bildschirmlicht:
 * - Farbe (Neutral/Warm/Kalt/Bernstein/Rot) und Helligkeit einstellbar
 * - Sanfte Übergänge, Display bleibt an
 * - Ursprüngliche Fensterhelligkeit wird beim Verlassen zuverlässig wiederhergestellt
 */
@Composable
fun ScreenLightScreen(
    color: ScreenLightColor,
    brightness: Float,
    onColorChange: (ScreenLightColor) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val view = LocalView.current
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    var localBrightness by remember { mutableFloatStateOf(brightness) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    val bg by animateColorAsState(Color(color.argb), motion.spec(tween(600)), label = "screenColor")
    val animatedBrightness by animateFloatAsState(localBrightness, motion.spec(tween(400)), label = "brightness")

    // Helligkeit anwenden + Bildschirm anlassen; beim Verlassen Originalwert wiederherstellen.
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val original = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.let {
                val params = it.attributes
                params.screenBrightness = original
                it.attributes = params
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    LaunchedEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        snapshotFlow { animatedBrightness }.collect { value ->
            val params = window.attributes
            params.screenBrightness = value.coerceIn(0.01f, 1f)
            window.attributes = params
        }
    }
    // Bedienelemente nach kurzer Zeit ausblenden
    LaunchedEffect(controlsVisible, interactionTick) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.screen_light_toggle_controls),
            ) {
                controlsVisible = !controlsVisible
                interactionTick++
            }
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (motion.enabled) slideInVertically { it / 2 } + fadeIn() else fadeIn(tween(0)),
            exit = if (motion.enabled) slideOutVertically { it / 2 } + fadeOut() else fadeOut(tween(0)),
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), color = Color(0xE60C0F1C))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { interactionTick++ }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.screen_light_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = LampColors.Text,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_close)) { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, stringResource(R.string.action_close), tint = LampColors.Text)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScreenLightColor.entries.forEach { c ->
                        val selected = c == color
                        val name = stringResource(colorName(c))
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(c.argb))
                                .border(if (selected) 3.dp else 1.dp, if (selected) LampColors.Gold else LampColors.OutlineStrong, CircleShape)
                                .semantics {
                                    this.selected = selected
                                    contentDescription = name
                                }
                                .clickable(role = Role.RadioButton) {
                                    haptics.click()
                                    interactionTick++
                                    onColorChange(c)
                                }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.screen_light_brightness), style = MaterialTheme.typography.labelMedium, color = LampColors.TextMuted)
                    Spacer(Modifier.size(12.dp))
                    Slider(
                        value = localBrightness,
                        onValueChange = {
                            localBrightness = it
                            interactionTick++
                        },
                        onValueChangeFinished = { onBrightnessChange(localBrightness) },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = LampColors.Gold,
                            activeTrackColor = LampColors.Gold,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text("${(localBrightness * 100).roundToInt()} %", style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
                }
                Text(
                    stringResource(R.string.screen_light_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LampColors.TextFaint,
                )
            }
        }
    }
}

private fun colorName(c: ScreenLightColor): Int = when (c) {
    ScreenLightColor.NEUTRAL -> R.string.color_neutral
    ScreenLightColor.WARM -> R.string.color_warm
    ScreenLightColor.COOL -> R.string.color_cool
    ScreenLightColor.AMBER -> R.string.color_amber
    ScreenLightColor.RED -> R.string.color_red
}
