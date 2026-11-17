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
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.core.AutoOff
import dev.flondy.mylamp.core.ScreenLightLogic
import dev.flondy.mylamp.shop.PremiumPreview
import dev.flondy.mylamp.ui.AutoOffTimer
import dev.flondy.mylamp.ui.screenColorName
import dev.flondy.mylamp.ui.screenEffectName
import dev.flondy.mylamp.ui.components.rememberTicker
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Bildschirmlicht:
 * - Farbe (5 kostenlose, 4 Premium-Farben, eigene Farbe per Farbton) und Helligkeit einstellbar
 * - Premium-Effekte: Regenbogen, Kerzenlicht, Party, Warnlicht (alle langsam, siehe [ScreenLightLogic])
 * - Gesperrte Inhalte starten eine 30-Sekunden-Vorschau
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
    customHue: Float = 200f,
    effect: ScreenEffect = ScreenEffect.NONE,
    isPremium: Boolean = false,
    preview: PremiumPreview? = null,
    autoOff: AutoOffTimer? = null,
    onEffectChange: (ScreenEffect) -> Unit = {},
    onHueChange: (Float) -> Unit = {},
    onUnlock: () -> Unit = {},
) {
    val view = LocalView.current
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    var localBrightness by remember { mutableFloatStateOf(brightness) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    var localHue by remember(customHue) { mutableFloatStateOf(customHue) }
    val base = ScreenLightLogic.baseColor(color, localHue)
    val staticBg by animateColorAsState(Color(base), motion.spec(tween(600)), label = "screenColor")
    // Effekte laufen bildgenau über die Frame-Uhr; ohne Effekt bleibt die Farbe ruhig.
    var effectTime by remember { mutableIntStateOf(0) }
    LaunchedEffect(effect) {
        if (effect == ScreenEffect.NONE) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { effectTime = (it - start).toInt() }
    }
    val bg = if (effect == ScreenEffect.NONE) staticBg else Color(ScreenLightLogic.colorAt(effect, base, effectTime.toLong()))
    val ticking = preview != null || autoOff != null
    val now by rememberTicker(active = ticking && controlsVisible, periodMs = 500L)
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
                if (preview != null && !isPremium) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LampColors.GoldSubtle)
                            .clickable(onClick = onUnlock)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val seconds = ((preview.remainingMs(now) + 999) / 1000).toInt()
                        Text(
                            stringResource(R.string.preview_banner, stringResource(shopItemName(preview.item)), seconds),
                            style = MaterialTheme.typography.labelMedium, color = LampColors.Text, modifier = Modifier.weight(1f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Text(stringResource(R.string.action_unlock), style = MaterialTheme.typography.labelLarge, color = LampColors.Gold)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(ScreenLightColor.entries.toList(), key = { it.name }) { c ->
                        val selected = c == color
                        val locked = c.isPremium && !isPremium
                        val name = stringResource(screenColorName(c))
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .then(
                                    if (c == ScreenLightColor.CUSTOM) Modifier.background(
                                        Brush.sweepGradient((0..6).map { Color(ScreenLightLogic.hsvToArgb(it * 60f, 0.7f, 1f)) })
                                    ) else Modifier.background(Color(c.argb))
                                )
                                .border(if (selected) 3.dp else 1.dp, if (selected) LampColors.Gold else LampColors.OutlineStrong, CircleShape)
                                .semantics {
                                    this.selected = selected
                                    contentDescription = name
                                }
                                .clickable(role = Role.RadioButton) {
                                    haptics.click()
                                    interactionTick++
                                    onColorChange(c)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (locked) Icon(Icons.Filled.Lock, null, tint = Color(0xCC050711), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (color == ScreenLightColor.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.screen_light_hue), style = MaterialTheme.typography.labelMedium, color = LampColors.TextMuted)
                        Spacer(Modifier.size(12.dp))
                        Slider(
                            value = localHue,
                            onValueChange = {
                                localHue = it
                                interactionTick++
                            },
                            onValueChangeFinished = { onHueChange(localHue) },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(base),
                                activeTrackColor = Color(base),
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.screen_light_effects), style = MaterialTheme.typography.labelMedium, color = LampColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScreenEffect.entries.toList(), key = { it.name }) { e ->
                        val selected = e == effect
                        val locked = e.isPremium && !isPremium
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) LampColors.Gold else Color.White.copy(alpha = 0.08f))
                                .semantics { this.selected = selected }
                                .clickable(role = Role.RadioButton) {
                                    haptics.click()
                                    interactionTick++
                                    onEffectChange(e)
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (locked) {
                                Icon(Icons.Filled.Lock, stringResource(R.string.cd_locked), tint = if (selected) LampColors.Night else LampColors.Gold, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.size(5.dp))
                            }
                            Text(
                                stringResource(screenEffectName(e)),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) LampColors.Night else LampColors.Text,
                            )
                        }
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
                if (autoOff != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.auto_off_running, AutoOff.format((autoOff.endsAtMs - now).coerceAtLeast(0L))),
                        style = MaterialTheme.typography.bodySmall,
                        color = LampColors.Gold,
                    )
                }
            }
        }
    }
}
