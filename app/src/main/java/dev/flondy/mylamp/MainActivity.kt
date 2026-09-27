package dev.flondy.mylamp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dev.flondy.mylamp.ui.theme.MyLampTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs

import dev.flondy.mylamp.BuildConfig

// Enum zur Verwaltung der verschiedenen Licht-Modi
enum class LightMode {
    NORMAL,
    SOS,
    STROBE
}

// --- Premium-State ---
object PremiumManager {
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    fun setPremium(value: Boolean) {
        _isPremium.value = value
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var billingClientWrapper: BillingClientWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        billingClientWrapper = BillingClientWrapper(this)

        setContent {
            MyLampTheme {
                val view = LocalView.current
                var isImmersive by remember { mutableStateOf(false) }

                // Systemleisten je nach Modus steuern
                LaunchedEffect(isImmersive) {
                    val window = (view.context as Activity).window
                    val controller = WindowInsetsControllerCompat(window, window.decorView)

                    if (isImmersive) {
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        WindowCompat.setDecorFitsSystemWindows(window, true)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                FlashlightRoot(
                    billingClientWrapper = billingClientWrapper,
                    isImmersive = isImmersive,
                    onToggleImmersive = { isImmersive = !isImmersive }
                )
            }
        }
    }
}

// --- AdBanner ---
@Composable
fun AdBanner() {
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // Test
    } else {
        "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // TODO: Live-ID
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// --- Premium Banner oben ---
@Composable
fun PremiumBanner(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C0F1C))
            .border(
                width = 1.dp,
                color = Color(0x66FDEE51),
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDEE51)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    fontSize = 12.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Premium aktiviert",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Werbefrei · voller Fokus auf Licht",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Schließen",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClose)
            )
        }
    }
}

@SuppressLint("ServiceCast")
@Composable
fun FlashlightRoot(
    billingClientWrapper: BillingClientWrapper,
    isImmersive: Boolean,
    onToggleImmersive: () -> Unit
) {
    val context = LocalContext.current
    val cameraManager =
        remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember { cameraManager.cameraIdList[0] }
    val coroutineScope = rememberCoroutineScope()

    var isTorchOn by remember { mutableStateOf(false) }
    var lightMode by remember { mutableStateOf(LightMode.NORMAL) }
    var isScreenLightOn by remember { mutableStateOf(false) }
    var blinkerJob by remember { mutableStateOf<Job?>(null) }
    var strobeSpeed by remember { mutableStateOf(50f) }

    val isPremium by PremiumManager.isPremium.collectAsState()

    // Settings & Banner-States
    var showPremiumBanner by rememberSaveable { mutableStateOf(true) }
    var showPremiumThanks by rememberSaveable { mutableStateOf(true) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Aufräumen
    DisposableEffect(Unit) {
        onDispose {
            blinkerJob?.cancel()
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (_: Exception) {
            }
        }
    }

    val setTorch: (Boolean) -> Unit = { state ->
        isTorchOn = state
        try {
            cameraManager.setTorchMode(cameraId, state)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val onToggleClick = {
        blinkerJob?.cancel()
        lightMode = LightMode.NORMAL
        setTorch(!isTorchOn)
    }

    val onSosClick = {
        blinkerJob?.cancel()
        if (lightMode == LightMode.SOS) {
            lightMode = LightMode.NORMAL
            setTorch(false)
        } else {
            lightMode = LightMode.SOS
            setTorch(true)
            blinkerJob = coroutineScope.launch {
                val sosPattern = longArrayOf(
                    200, 200, 200, 200, 200, 500,
                    500, 200, 500, 200, 500, 500,
                    200, 200, 200, 200, 200, 800
                )
                while (true) {
                    for (i in sosPattern.indices) {
                        setTorch(i % 2 == 0)
                        kotlinx.coroutines.delay(sosPattern[i])
                    }
                }
            }
        }
    }

    val onStrobeClick = {
        blinkerJob?.cancel()
        if (lightMode == LightMode.STROBE) {
            lightMode = LightMode.NORMAL
            setTorch(false)
        } else {
            lightMode = LightMode.STROBE
            blinkerJob = coroutineScope.launch {
                var strobeState = true
                while (true) {
                    strobeState = !strobeState
                    setTorch(strobeState)
                    kotlinx.coroutines.delay(strobeSpeed.toLong())
                }
            }
        }
    }

    val onScreenLightClick = {
        blinkerJob?.cancel()
        setTorch(false)
        isScreenLightOn = true
    }

    if (isScreenLightOn) {
        ScreenLight { isScreenLightOn = false }
    } else {
        Box(Modifier.fillMaxSize()) {

            // Hauptinhalt (Flashlight + Banner/Ads)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    FlashlightScreen(
                        isTorchOn = isTorchOn,
                        isPremium = isPremium,
                        lightMode = lightMode,
                        strobeSpeed = strobeSpeed,
                        onStrobeSpeedChange = { strobeSpeed = it },
                        onToggleClick = onToggleClick,
                        onSosClick = onSosClick,
                        onStrobeClick = onStrobeClick,
                        onScreenLightClick = onScreenLightClick,
                        onRemoveAdsClick = { billingClientWrapper.launchPurchaseFlow() },
                        isScreenLightOn = isScreenLightOn,
                        showPremiumBanner = showPremiumBanner,
                        onChangeShowPremiumBanner = { showPremiumBanner = it },
                        onToggleImmersive = onToggleImmersive,
                        onOpenSettings = { showSettings = true }
                    )
                }

                if (!isPremium) {
                    AdBanner()
                } else if (showPremiumThanks) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C0F1C))
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Werbefrei dank Premium ✨ Danke für deinen Support!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Schließen",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showPremiumThanks = false }
                            )
                        }
                    }
                }
            }

            // Settings Overlay
            if (showSettings) {
                SettingsScreen(
                    isImmersive = isImmersive,
                    onToggleImmersive = onToggleImmersive,
                    showPremiumBanner = showPremiumBanner,
                    onChangeShowPremiumBanner = { showPremiumBanner = it },
                    showPremiumThanks = showPremiumThanks,
                    onChangeShowPremiumThanks = { showPremiumThanks = it },
                    onClose = { showSettings = false }
                )
            }
        }
    }
}

// --- Einstellungen-Overlay ---
@Composable
fun SettingsScreen(
    isImmersive: Boolean,
    onToggleImmersive: () -> Unit,
    showPremiumBanner: Boolean,
    onChangeShowPremiumBanner: (Boolean) -> Unit,
    showPremiumThanks: Boolean,
    onChangeShowPremiumThanks: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101018))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Einstellungen",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Schließen",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsToggleRow(
                title = "Vollbildmodus (Immersive)",
                subtitle = "Blendet Status- und Navigationsleiste aus",
                checked = isImmersive,
                onCheckedChange = { onToggleImmersive() } // toggelt einfach
            )

            SettingsToggleRow(
                title = "Premium-Banner anzeigen",
                subtitle = "Goldenes 'Premium aktiviert'-Banner oben",
                checked = showPremiumBanner,
                onCheckedChange = onChangeShowPremiumBanner
            )

            SettingsToggleRow(
                title = "Premium-Hinweis unten anzeigen",
                subtitle = "„Werbefrei dank Premium“-Leiste",
                checked = showPremiumThanks,
                onCheckedChange = onChangeShowPremiumThanks
            )

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DEV-Hinweis: Premium-Status selbst per Debug-Button toggelbar.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// --- Flashlight Screen ---
@Composable
fun FlashlightScreen(
    isTorchOn: Boolean,
    isPremium: Boolean,
    lightMode: LightMode,
    strobeSpeed: Float,
    onStrobeSpeedChange: (Float) -> Unit,
    onToggleClick: () -> Unit,
    onSosClick: () -> Unit,
    onStrobeClick: () -> Unit,
    onScreenLightClick: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    isScreenLightOn: Boolean,
    showPremiumBanner: Boolean,
    onChangeShowPremiumBanner: (Boolean) -> Unit,
    onToggleImmersive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            Crossfade(
                targetState = isTorchOn,
                animationSpec = tween(1000),
                label = "backgroundCrossfade"
            ) { torchIsOn ->
                val backgroundImageRes = if (torchIsOn) {
                    R.drawable.background_1
                } else {
                    R.drawable.background_off
                }

                Image(
                    painter = painterResource(id = backgroundImageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Premium-Banner oben
            if (isPremium && showPremiumBanner) {
                PremiumBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp),
                    onClose = { onChangeShowPremiumBanner(false) }
                )
            }

            // DEV-Toggles oben links
            if (BuildConfig.DEBUG) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66000000))
                            .clickable {
                                PremiumManager.setPremium(!isPremium)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "DEV: Premium " + if (isPremium) "ON" else "OFF",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66000000))
                            .clickable { onToggleImmersive() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "DEV: Fullscreen TOGGLE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Buttons oben rechts (Remove Ads, Screen Light, Settings)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPremium) {
                    M3IconButton(onClick = onRemoveAdsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ads_off),
                            contentDescription = "Remove Ads",
                            tint = Color(0xFFFDD835)
                        )
                    }
                }
                M3IconButton(onClick = onScreenLightClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone_android),
                        contentDescription = "Screen Light",
                        tint = Color.White
                    )
                }
                M3IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color.White
                    )
                }
            }

            // Hauptlayout
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                PowerButton(
                    isTorchOn = isTorchOn,
                    isPremium = isPremium,
                    onClick = onToggleClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isTorchOn) lightMode.name else "OFF",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureButton(
                        onClick = onSosClick,
                        iconRes = R.drawable.ic_sos,
                        text = "SOS",
                        isSelected = lightMode == LightMode.SOS
                    )
                    FeatureButton(
                        onClick = onStrobeClick,
                        iconRes = R.drawable.ic_strobe,
                        text = "Strobe",
                        isSelected = lightMode == LightMode.STROBE
                    )
                }

                if (lightMode == LightMode.STROBE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = strobeSpeed,
                        onValueChange = onStrobeSpeedChange,
                        valueRange = 10f..200f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// --- Billing ---
class BillingClientWrapper(private val activity: Activity) {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        val acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                PremiumManager.setPremium(true)
                            }
                        }
                    }
                }
            }
        }

    private var billingClient: BillingClient =
        BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

    init {
        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                connectToGooglePlay()
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)

        billingClient.queryPurchasesAsync(params.build()) { _, purchases ->
            if (purchases.any { it.products.contains("remove_ads") }) {
                PremiumManager.setPremium(true)
            }
        }
    }

    fun launchPurchaseFlow() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }
}

// --- Feature-Button ---
@Composable
fun FeatureButton(onClick: () -> Unit, iconRes: Int, text: String, isSelected: Boolean) {
    val buttonColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
        label = ""
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(buttonColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

// --- ScreenLight ---
@Composable
fun ScreenLight(onClose: () -> Unit) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    DisposableEffect(Unit) {
        val originalBrightness = window?.attributes?.screenBrightness
        window?.let {
            val params = it.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            it.attributes = params
        }
        onDispose {
            window?.let {
                val params = it.attributes
                params.screenBrightness =
                    originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = params
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onClose)
    )
}

// --- ShakeDetector (falls genutzt) ---
@Composable
fun ShakeDetector(onShake: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastUpdate: Long = 0
        var last_x = 0f
        var last_y = 0f
        var last_z = 0f
        val SHAKE_THRESHOLD = 800

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 100) {
                    val diffTime = (curTime - lastUpdate)
                    lastUpdate = curTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed =
                        abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                    if (speed > SHAKE_THRESHOLD) {
                        onShake()
                    }
                    last_x = x
                    last_y = y
                    last_z = z
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
}

// --- Power Button ---
@Composable
fun PowerButton(
    isTorchOn: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val buttonColor by animateColorAsState(
        if (isTorchOn) Color(0xFFFDEE51)
        else if (isPremium) Color(0xFF555020)
        else Color(0xFF303030),
        label = ""
    )
    val iconColor by animateColorAsState(
        if (isTorchOn) Color.Black else Color.White.copy(alpha = 0.6f),
        label = ""
    )
    Box(
        modifier = Modifier
            .size(150.dp)
            .shadow(
                elevation = if (isTorchOn || isPremium) 30.dp else 10.dp,
                shape = CircleShape,
                ambientColor = if (isPremium) Color(0xFFFDEE51) else Color.Black,
                spotColor = if (isPremium) Color(0xFFFDEE51) else Color.Black
            )
            .clip(CircleShape)
            .background(buttonColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_power),
            contentDescription = "Toggle Flashlight",
            tint = iconColor,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlashlightScreenPreviewOff() {
    MyLampTheme {
        FlashlightScreen(
            isTorchOn = false,
            isPremium = false,
            lightMode = LightMode.NORMAL,
            strobeSpeed = 50f,
            onStrobeSpeedChange = {},
            onToggleClick = {},
            onSosClick = {},
            onStrobeClick = {},
            onScreenLightClick = {},
            onRemoveAdsClick = {},
            isScreenLightOn = false,
            showPremiumBanner = true,
            onChangeShowPremiumBanner = {},
            onToggleImmersive = {},
            onOpenSettings = {}
        )
    }
}
