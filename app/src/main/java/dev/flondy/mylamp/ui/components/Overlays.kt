package dev.flondy.mylamp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.ui.UiMessage
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlinx.coroutines.delay

/** Dezente, selbst verschwindende Statusmeldung am oberen Rand. */
@Composable
fun MessageHost(message: UiMessage?, onShown: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf<UiMessage?>(null) }
    var current by remember { mutableStateOf<UiMessage?>(null) }
    LaunchedEffect(message?.id) {
        if (message != null) {
            current = message
            visible = message
            delay(2800)
            visible = null
            onShown()
        }
    }
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible != null,
        modifier = modifier,
        enter = if (motion.enabled) slideInVertically { -it } + fadeIn() else fadeIn(tween(0)),
        exit = if (motion.enabled) slideOutVertically { -it } + fadeOut() else fadeOut(tween(0)),
    ) {
        val msg = current ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 520.dp)
                .glass(
                    RoundedCornerShape(50),
                    color = Color(0xEE0C0F1C),
                    borderColor = if (msg.isError) LampColors.Sos.copy(alpha = 0.6f) else LampColors.Gold.copy(alpha = 0.4f),
                )
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (msg.isError) LampColors.Sos else LampColors.Gold)
            )
            Spacer(Modifier.size(10.dp))
            Text(stringResource(msg.text), style = MaterialTheme.typography.bodyMedium, color = LampColors.Text)
        }
    }
}

/**
 * In-App-Dialog im Premium-Design. Wird im Compose-Baum gezeichnet (kein eigenes Fenster) und
 * bleibt so auch im Vollbildmodus ohne eingeblendete Systemleisten bedienbar.
 */
@Composable
fun LampDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    icon: Painter? = null,
    accent: Color = LampColors.Gold,
) {
    val motion = LocalMotion.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.spec(tween(220))),
        exit = fadeOut(motion.spec(tween(180))),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .animateEnterExit(
                        enter = if (motion.enabled) scaleIn(initialScale = 0.9f) + fadeIn() else fadeIn(tween(0)),
                        exit = if (motion.enabled) scaleOut(targetScale = 0.95f) + fadeOut() else fadeOut(tween(0)),
                    )
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), color = LampColors.NightRaised, borderColor = accent.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.height(16.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, color = LampColors.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = LampColors.TextMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = LampColors.Night),
                ) { Text(confirmText, style = MaterialTheme.typography.labelLarge) }
                if (dismissText != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(dismissText, color = LampColors.TextMuted)
                    }
                }
            }
        }
    }
}

/** Schließbarer Premium-Hinweis (Entscheidung wird gespeichert). */
@Composable
fun PremiumHint(
    visible: Boolean,
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (motion.enabled) fadeIn(tween(400)) + slideInVertically { -it / 2 } else fadeIn(tween(0)),
        exit = if (motion.enabled) fadeOut(tween(250)) + slideOutVertically { -it / 2 } else fadeOut(tween(0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(50), color = Color(0xCC0C0F1C), borderColor = LampColors.Gold.copy(alpha = 0.35f))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(LampColors.GoldGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    androidx.compose.ui.res.painterResource(R.drawable.ic_crown),
                    contentDescription = null,
                    tint = LampColors.Night,
                    modifier = Modifier.size(13.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = LampColors.Text)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = stringResource(R.string.action_close), onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = LampColors.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
