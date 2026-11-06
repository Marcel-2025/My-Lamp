package dev.flondy.mylamp.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.core.net.toUri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.flondy.mylamp.BuildConfig
import dev.flondy.mylamp.R
import dev.flondy.mylamp.core.AppLanguage
import dev.flondy.mylamp.ui.components.LampDialog
import dev.flondy.mylamp.ui.components.MessageHost
import dev.flondy.mylamp.ui.screens.CelebrationOverlay
import dev.flondy.mylamp.ui.screens.HomeActions
import dev.flondy.mylamp.ui.screens.HomeScreen
import dev.flondy.mylamp.ui.screens.PaywallScreen
import dev.flondy.mylamp.ui.screens.ScreenLightScreen
import dev.flondy.mylamp.ui.screens.SettingsActions
import dev.flondy.mylamp.ui.screens.SettingsScreen
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalMotion
import dev.flondy.mylamp.ui.theme.MyLampTheme

@Composable
fun LampApp(
    viewModel: MainViewModel,
    adUnitId: String,
    shakeAvailable: Boolean,
    onExit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val showCelebration by viewModel.showCelebration.collectAsStateWithLifecycle()
    val showStrobeWarning by viewModel.showStrobeWarning.collectAsStateWithLifecycle()

    var message by remember { mutableStateOf<UiMessage?>(null) }
    LaunchedEffect(Unit) { viewModel.messages.collect { message = it } }

    val context = LocalContext.current
    val activity = context as? Activity

    MyLampTheme(
        animationsEnabled = state.settings.animationsEnabled,
        hapticsEnabled = state.settings.hapticsEnabled,
    ) {
        BackHandler(enabled = route != Route.HOME) { viewModel.navigateBack() }

        val motion = LocalMotion.current
        Box(Modifier.fillMaxSize().background(LampColors.Night)) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    if (!motion.enabled) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else when {
                        targetState == Route.SCREEN_LIGHT || initialState == Route.SCREEN_LIGHT ->
                            fadeIn(tween(450)) togetherWith fadeOut(tween(350))
                        targetState == Route.PAYWALL ->
                            (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.96f)) togetherWith fadeOut(tween(250))
                        initialState == Route.PAYWALL ->
                            fadeIn(tween(300)) togetherWith (fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.96f))
                        targetState.ordinal > initialState.ordinal ->
                            (slideInHorizontally(tween(380)) { it / 3 } + fadeIn(tween(380))) togetherWith
                                (slideOutHorizontally(tween(380)) { -it / 6 } + fadeOut(tween(250)))
                        else ->
                            (slideInHorizontally(tween(380)) { -it / 6 } + fadeIn(tween(380))) togetherWith
                                (slideOutHorizontally(tween(380)) { it / 3 } + fadeOut(tween(250)))
                    }
                },
                label = "routes",
            ) { r ->
                when (r) {
                    Route.HOME -> HomeScreen(
                        state = state,
                        adUnitId = adUnitId,
                        actions = remember(viewModel) {
                            HomeActions(
                                onPower = viewModel::onPowerClick,
                                onSos = viewModel::onSosClick,
                                onStrobe = viewModel::onStrobeClick,
                                onScreenLight = viewModel::onScreenLightClick,
                                onSettings = { viewModel.navigate(Route.SETTINGS) },
                                onPremium = { viewModel.navigate(Route.PAYWALL) },
                                onStrobeHz = viewModel::onStrobeHzChange,
                                onStrobeHzDone = viewModel::onStrobeHzCommit,
                                onStrength = viewModel::onTorchStrengthChange,
                                onStrengthDone = viewModel::onTorchStrengthCommit,
                                onCloseTopHint = { viewModel.setShowPremiumTopHint(false) },
                                onCloseBottomHint = { viewModel.setShowPremiumBottomHint(false) },
                            )
                        },
                    )
                    Route.SETTINGS -> SettingsScreen(
                        state = state,
                        shakeAvailable = shakeAvailable,
                        languageTag = remember(context) { AppLanguage.current(context) },
                        onSelectLanguage = { tag -> activity?.let { AppLanguage.set(it, tag) } },
                        actions = remember(viewModel, activity) {
                            SettingsActions(
                                onBack = { viewModel.navigateBack() },
                                setFullscreen = viewModel::setFullscreen,
                                setShowNavigationBar = viewModel::setShowNavigationBar,
                                setAnimations = viewModel::setAnimationsEnabled,
                                setHaptics = viewModel::setHapticsEnabled,
                                selectTheme = viewModel::selectTheme,
                                setDefaultSource = viewModel::setDefaultLightSource,
                                setStartBehavior = viewModel::setStartBehavior,
                                setStrobeHz = { hz ->
                                    viewModel.onStrobeHzChange(hz)
                                    viewModel.onStrobeHzCommit()
                                },
                                setShakeEnabled = viewModel::setShakeEnabled,
                                setShakeSensitivity = viewModel::setShakeSensitivity,
                                openPaywall = { viewModel.navigate(Route.PAYWALL) },
                                restorePurchases = viewModel::restorePurchases,
                                setTopHint = viewModel::setShowPremiumTopHint,
                                setBottomHint = viewModel::setShowPremiumBottomHint,
                                openPrivacyOptions = { activity?.let(viewModel::showPrivacyOptions) },
                                openPrivacyPolicy = BuildConfig.PRIVACY_POLICY_URL.takeIf { it.startsWith("https://") }?.let { url ->
                                    { openUri(context, url) { viewModel.postMessage(R.string.error_no_app, true) } }
                                },
                                contactSupport = {
                                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:${BuildConfig.SUPPORT_EMAIL}".toUri())
                                        .putExtra(Intent.EXTRA_SUBJECT, "My Lamp ${BuildConfig.VERSION_NAME} – Support")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        viewModel.postMessage(R.string.error_no_mail_app, true)
                                    }
                                },
                                openPlayStore = {
                                    openUri(context, "market://details?id=${context.packageName}") {
                                        openUri(context, BuildConfig.PLAY_STORE_URL) { viewModel.postMessage(R.string.error_no_app, true) }
                                    }
                                },
                                setSimulation = viewModel::setSimulation,
                                debugCelebration = viewModel::debugShowCelebration,
                                debugResetHints = viewModel::debugResetHints,
                                debugResetConsent = viewModel::debugResetConsent,
                            )
                        },
                    )
                    Route.PAYWALL -> PaywallScreen(
                        billing = state.billing,
                        isPremium = state.isPremium,
                        onBuy = { activity?.let(viewModel::purchase) },
                        onRestore = viewModel::restorePurchases,
                        onRetry = viewModel::reloadProduct,
                        onBack = { viewModel.navigateBack() },
                    )
                    Route.SCREEN_LIGHT -> ScreenLightScreen(
                        color = state.settings.screenLightColor,
                        brightness = state.settings.screenLightBrightness,
                        onColorChange = viewModel::setScreenLightColor,
                        onBrightnessChange = viewModel::setScreenLightBrightness,
                        onClose = { if (!viewModel.navigateBack()) onExit() },
                    )
                }
            }

            LampDialog(
                visible = showStrobeWarning,
                title = stringResource(R.string.strobe_warning_title),
                text = stringResource(R.string.strobe_warning_text),
                confirmText = stringResource(R.string.strobe_warning_confirm),
                dismissText = stringResource(R.string.action_cancel),
                onConfirm = { viewModel.onStrobeWarningResult(true) },
                onDismiss = { viewModel.onStrobeWarningResult(false) },
                icon = painterResource(R.drawable.ic_strobe),
                accent = LampColors.Strobe,
            )

            CelebrationOverlay(visible = showCelebration, onDismiss = viewModel::dismissCelebration)

            MessageHost(
                message = message,
                onShown = { message = null },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

private fun openUri(context: android.content.Context, uri: String, onFail: () -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        onFail()
    }
}
