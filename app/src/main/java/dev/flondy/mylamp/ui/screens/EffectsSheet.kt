package dev.flondy.mylamp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.torch.LightEffect
import dev.flondy.mylamp.torch.LightMode
import dev.flondy.mylamp.torch.LightPatterns
import dev.flondy.mylamp.ui.MainUiState
import dev.flondy.mylamp.ui.components.GlassIconButton
import dev.flondy.mylamp.ui.components.RhythmDot
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.effectDescription
import dev.flondy.mylamp.ui.effectName
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import dev.flondy.mylamp.ui.theme.Skins

/**
 * Auswahl „Lichteffekte“: Morse-Nachricht (kostenlos) und feste LED-Effekte.
 * Gesperrte Premium-Effekte starten eine 10-Sekunden-Vorschau.
 */
@Composable
fun EffectsSheet(
    visible: Boolean,
    state: MainUiState,
    onMorse: (String) -> Unit,
    onStop: () -> Unit,
    onEffect: (LightEffect) -> Unit,
    onDismiss: () -> Unit,
) {
    val motion = LocalMotion.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(motion.spec(tween(200))), exit = fadeOut(motion.spec(tween(150)))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = if (motion.enabled) slideInVertically { it / 4 } + fadeIn() else fadeIn(tween(0)),
                exit = if (motion.enabled) slideOutVertically { it / 4 } + fadeOut() else fadeOut(tween(0)),
            ) {
                Column(
                    Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .heightIn(max = 720.dp)
                        .glass(RoundedCornerShape(28.dp), color = LampColors.NightRaised, borderColor = LampColors.Gold.copy(alpha = 0.3f))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { }
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.effects_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = LampColors.Text,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(stringResource(R.string.effects_subtitle), style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
                        }
                        GlassIconButton(
                            painter = rememberVectorPainter(Icons.Filled.Close),
                            contentDescription = stringResource(R.string.action_close),
                            onClick = onDismiss,
                            size = 40.dp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    MorseCard(state, onMorse, onStop)
                    Spacer(Modifier.height(16.dp))
                    LightEffect.entries.forEach { effect ->
                        EffectRow(
                            effect = effect,
                            locked = effect.isPremium && !state.isPremium,
                            active = state.mode == LightMode.EFFECT && state.effect == effect,
                            onClick = { onEffect(effect) },
                            accent = Skins.of(state.skin).accent,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MorseCard(state: MainUiState, onMorse: (String) -> Unit, onStop: () -> Unit) {
    var text by remember { mutableStateOf(state.settings.morseText) }
    val running = state.mode == LightMode.MORSE
    val code = LightPatterns.morseCode(text)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), color = LampColors.Card)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_morse), null, tint = LampColors.Gold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.morse_title), style = MaterialTheme.typography.titleSmall, color = LampColors.Text)
                Text(stringResource(R.string.morse_desc), style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
            }
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.shop_state_free), style = MaterialTheme.typography.labelSmall, color = LampColors.Success)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 60) text = it },
            singleLine = true,
            label = { Text(stringResource(R.string.morse_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onMorse(text) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LampColors.Gold,
                unfocusedBorderColor = LampColors.OutlineStrong,
                focusedLabelColor = LampColors.Gold,
                cursorColor = LampColors.Gold,
                focusedTextColor = LampColors.Text,
                unfocusedTextColor = LampColors.Text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (code.isEmpty()) stringResource(R.string.morse_preview_empty) else code,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (code.isEmpty()) LampColors.TextFaint else LampColors.Gold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = stringResource(R.string.morse_start),
                filled = true,
                enabled = code.isNotEmpty(),
                onClick = { onMorse(text) },
                modifier = Modifier.weight(1f),
            )
            if (running) {
                PillButton(text = stringResource(R.string.morse_stop), filled = false, onClick = onStop, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EffectRow(effect: LightEffect, locked: Boolean, active: Boolean, onClick: () -> Unit, accent: Color) {
    val haptics = LocalHaptics.current
    val motion = LocalMotion.current
    Row(
        Modifier
            .fillMaxWidth()
            .glass(
                RoundedCornerShape(20.dp),
                color = if (active) accent.copy(alpha = 0.14f) else LampColors.Card,
                borderColor = if (active) accent.copy(alpha = 0.6f) else LampColors.Outline,
            )
            .clickable(role = Role.Button) {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RhythmDot(effect.steps, if (locked) LampColors.Gold else accent, animate = motion.enabled, size = 14.dp)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(effectName(effect)), style = MaterialTheme.typography.titleSmall, color = LampColors.Text)
            Text(stringResource(effectDescription(effect)), style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
        }
        Spacer(Modifier.size(8.dp))
        when {
            active -> Icon(Icons.Filled.Check, null, tint = accent)
            locked -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, stringResource(R.string.cd_locked), tint = LampColors.Gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_preview), style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
            }
            !effect.isPremium -> Text(stringResource(R.string.shop_state_free), style = MaterialTheme.typography.labelSmall, color = LampColors.Success)
        }
    }
}

@Composable
fun PillButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = LocalHaptics.current
    Box(
        modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (filled) Modifier.background(if (enabled) LampColors.Gold else LampColors.Gold.copy(alpha = 0.25f))
                else Modifier.glass(RoundedCornerShape(50), borderColor = LampColors.Gold.copy(alpha = 0.5f))
            )
            .clickable(enabled = enabled, role = Role.Button) {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) LampColors.Night else LampColors.Gold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
