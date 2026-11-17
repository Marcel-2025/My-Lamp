package dev.flondy.mylamp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.billing.BillingState
import dev.flondy.mylamp.billing.Entitlement
import dev.flondy.mylamp.shop.ShopCatalog
import dev.flondy.mylamp.shop.ShopCategory
import dev.flondy.mylamp.ui.components.GlassIconButton
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalMotion

/**
 * Transparente Premium-Seite: Einmalkauf, Preis direkt aus Google Play, Kauf wiederherstellen.
 * Keine erfundenen Preise, kein Countdown, kein künstlicher Druck.
 */
@Composable
fun PaywallScreen(
    billing: BillingState,
    isPremium: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val motion = LocalMotion.current
    val entrance = remember { Animatable(if (motion.enabled) 0f else 1f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Box(Modifier.fillMaxSize().background(LampColors.Night)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(LampColors.Gold.copy(alpha = 0.22f * entrance.value), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f,
                )
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                GlassIconButton(
                    painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Tablet/Handy quer: Titel + Kauf links, Vorteile rechts
                val wide = maxWidth >= 720.dp
                val anim = Modifier.graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 40.dp.toPx()
                }
                if (wide) {
                    Row(
                        anim.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f, fill = false).widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PaywallHero()
                            Spacer(Modifier.height(24.dp))
                            PaywallPurchase(billing, isPremium, onBuy, onRestore, onRetry)
                        }
                        Column(Modifier.weight(1f, fill = false).widthIn(max = 520.dp)) {
                            PaywallBenefits()
                        }
                    }
                } else {
                    Column(
                        anim.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PaywallHero()
                            Spacer(Modifier.height(28.dp))
                            PaywallBenefits()
                            Spacer(Modifier.height(24.dp))
                            PaywallPurchase(billing, isPremium, onBuy, onRestore, onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallHero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(LampColors.GoldGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_crown), null, tint = LampColors.Night, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.paywall_title),
            style = MaterialTheme.typography.headlineMedium,
            color = LampColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.paywall_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = LampColors.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PaywallBenefits() {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), color = LampColors.Card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Benefit(stringResource(R.string.paywall_benefit_ads_title), stringResource(R.string.paywall_benefit_ads_text))
        Benefit(
            stringResource(R.string.paywall_benefit_themes_title, ShopCatalog.premiumCount(ShopCategory.THEMES)),
            stringResource(R.string.paywall_benefit_themes_text),
        )
        Benefit(
            stringResource(R.string.paywall_benefit_studio_title),
            stringResource(
                R.string.paywall_benefit_studio_text,
                ShopCatalog.premiumCount(ShopCategory.SKINS),
                ShopCatalog.premiumCount(ShopCategory.LIGHT_EFFECTS),
                ShopCatalog.premiumCount(ShopCategory.SCREEN),
            ),
        )
        Benefit(stringResource(R.string.paywall_benefit_look_title), stringResource(R.string.paywall_benefit_look_text))
        Benefit(stringResource(R.string.paywall_benefit_support_title), stringResource(R.string.paywall_benefit_support_text))
    }
}

@Composable
private fun PaywallPurchase(
    billing: BillingState,
    isPremium: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            isPremium -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(50), color = LampColors.GoldSubtle, borderColor = LampColors.Gold.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = LampColors.Gold)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.paywall_already_premium), color = LampColors.Gold, style = MaterialTheme.typography.labelLarge)
                }
            }
            billing.entitlement == Entitlement.PENDING -> {
                Text(
                    stringResource(R.string.paywall_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LampColors.Gold,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                val price = billing.formattedPrice
                Text(
                    if (price != null) stringResource(R.string.paywall_price_once, price)
                    else if (billing.billingUnavailable) stringResource(R.string.paywall_play_unavailable)
                    else stringResource(R.string.paywall_price_loading),
                    style = MaterialTheme.typography.titleMedium,
                    color = LampColors.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.paywall_no_subscription),
                    style = MaterialTheme.typography.bodySmall,
                    color = LampColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBuy,
                    enabled = price != null && !billing.purchaseInProgress,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LampColors.Gold,
                        contentColor = LampColors.Night,
                        disabledContainerColor = LampColors.Gold.copy(alpha = 0.25f),
                        disabledContentColor = LampColors.Night.copy(alpha = 0.6f),
                    ),
                ) {
                    if (billing.purchaseInProgress) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = LampColors.Night, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (price != null) stringResource(R.string.paywall_buy_with_price, price)
                            else stringResource(R.string.paywall_buy),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (price == null) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry), color = LampColors.TextMuted) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.action_restore_purchases), color = LampColors.Gold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.paywall_legal),
            style = MaterialTheme.typography.bodySmall,
            color = LampColors.TextFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Benefit(title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(LampColors.GoldSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = LampColors.Gold, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LampColors.Text)
            Text(text, style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
        }
    }
}
