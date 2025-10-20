package com.example.mylamp

// ... (alle anderen Imports bleiben gleich)
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
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.mylamp.ui.theme.MyLampTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enum zur Verwaltung der verschiedenen Licht-Modi
enum class LightMode {
    NORMAL,
    SOS,
    STROBE
}

// --- NEU: Hält den Zustand, ob Werbung entfernt wurde ---
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

        // AdMob & Billing Client initialisieren
        MobileAds.initialize(this) {}
        billingClientWrapper = BillingClientWrapper(this)

        enableEdgeToEdge()
        setContent {
            MyLampTheme {
                FlashlightRoot(billingClientWrapper)
            }
        }
    }
}

// --- Composable für das Werbebanner ---
@Composable
fun AdBanner() {
    // Test-ID für ein Banner. Ersetze sie vor dem Release durch deine eigene!
    val adUnitId = "ca-app-pub-3940256099942544/6300978111" // <-- DIESE TEST-ID VERWENDEN!
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                this.setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@SuppressLint("ServiceCast")
@Composable
fun FlashlightRoot(billingClientWrapper: BillingClientWrapper) { // Nimmt jetzt den BillingClientWrapper entgegen
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember { cameraManager.cameraIdList[0] }
    val coroutineScope = rememberCoroutineScope()

    var isTorchOn by remember { mutableStateOf(false) }
    var lightMode by remember { mutableStateOf(LightMode.NORMAL) }
    var isScreenLightOn by remember { mutableStateOf(false) }
    var blinkerJob by remember { mutableStateOf<Job?>(null) }
    var strobeSpeed by remember { mutableStateOf(50f) }

    // --- Premium-Status beobachten ---
    val isPremium by PremiumManager.isPremium.collectAsState()

    // Shake-to-Toggle Logik (hier nicht eingefügt, um Komplexität zu reduzieren,
    // kann aber leicht wieder hinzugefügt werden, falls gewünscht)

    // Aufräumen: Wenn die App verlassen wird, alles ausschalten
    DisposableEffect(Unit) {
        onDispose {
            blinkerJob?.cancel()
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: Exception) { /* Ignorieren */ }
        }
    }

    // --- HIER IST DIE FEHLENDE LOGIK ---

    // Funktion, um den Blitz zu steuern und Jobs zu verwalten
    val setTorch = { state: Boolean ->
        isTorchOn = state
        try {
            cameraManager.setTorchMode(cameraId, state)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Logik für den Klick auf den Haupt-Power-Button
    val onToggleClick = {
        blinkerJob?.cancel() // Immer zuerst laufende Jobs stoppen
        lightMode = LightMode.NORMAL
        setTorch(!isTorchOn)
    }

    // Logik für SOS
    val onSosClick = {
        blinkerJob?.cancel()
        if (lightMode == LightMode.SOS) {
            lightMode = LightMode.NORMAL
            setTorch(false)
        } else {
            lightMode = LightMode.SOS
            setTorch(true) // Startet im AN-Zustand
            blinkerJob = coroutineScope.launch {
                // S-O-S pattern: 3x kurz, 3x lang, 3x kurz
                val sosPattern = longArrayOf(200, 200, 200, 200, 200, 500, 500, 200, 500, 200, 500, 500, 200, 200, 200, 200, 200, 800)
                while (true) {
                    for (i in sosPattern.indices) {
                        setTorch(i % 2 == 0) // An, Aus, An, Aus...
                        kotlinx.coroutines.delay(sosPattern[i])
                    }
                }
            }
        }
    }

    // Logik für Stroboskop
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

    // Logik für Screen-Light Klick
    val onScreenLightClick = {
        blinkerJob?.cancel()
        setTorch(false)
        isScreenLightOn = true
    }

    // Logik für die Anzeige (UI)
    if (isScreenLightOn) {
        ScreenLight { isScreenLightOn = false }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                FlashlightScreen(
                    isTorchOn = isTorchOn,
                    isPremium = isPremium,
                    lightMode = lightMode,
                    strobeSpeed = strobeSpeed,
                    onStrobeSpeedChange = { strobeSpeed = it },
                    // --- JETZT MIT DER VOLLSTÄNDIGEN LOGIK VERKNÜPFT ---
                    onToggleClick = onToggleClick,
                    onSosClick = onSosClick,
                    onStrobeClick = onStrobeClick,
                    onScreenLightClick = onScreenLightClick,
                    onRemoveAdsClick = {
                        billingClientWrapper.launchPurchaseFlow()
                    },
                    isScreenLightOn = isScreenLightOn
                )
            }
            if (!isPremium) {
                AdBanner()
            }
        }
    }
}


// FlashlightScreen erhält neue Parameter
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
    isScreenLightOn: Boolean
) {
    // animatedAlpha wird hier nicht mehr gebraucht, wir animieren nicht mehr die Deckkraft.

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- HIER IST DIE LOGIK FÜR DEINE HINTERGRUNDBILDER ---
            // Wählt das richtige Bild basierend auf dem isTorchOn-Zustand.
            // Der Übergang wird weich animiert.
            Crossfade(targetState = isTorchOn, animationSpec = tween(1000), label = "backgroundCrossfade") { torchIsOn ->
                val backgroundImageRes = if (torchIsOn) {
                    R.drawable.background_1 // DEIN "AN"-BILD
                } else {
                    R.drawable.background_off // DEIN "AUS"-BILD
                }

                Image(
                    painter = painterResource(id = backgroundImageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Stellt sicher, dass das Bild den ganzen Schirm füllt
                )
            }

            // --- Die restliche UI wird einfach über das Hintergrundbild gelegt ---

            // --- Buttons oben rechts ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPremium) {
                    IconButton(onClick = onRemoveAdsClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ads_off),
                            contentDescription = "Remove Ads",
                            tint = Color(0xFFFDD835) // Gold
                        )
                    }
                }
                IconButton(onClick = onScreenLightClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone_android),
                        contentDescription = "Screen Light",
                        tint = Color.White
                    )
                }
            }

            // --- UI in der Mitte und unten ---
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Power-Button
                PowerButton(isTorchOn = isTorchOn, onClick = onToggleClick)

                Spacer(modifier = Modifier.height(32.dp))

                // Text, der den Modus anzeigt
                Text(
                    text = if (isTorchOn) lightMode.name else "OFF",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // SOS und Strobe Buttons...
                // ... (der Rest deines Codes hier bleibt unverändert)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
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

                // Stroboskop-Slider (nur sichtbar im Strobe-Modus)
                if (lightMode == LightMode.STROBE) {
                    Slider(
                        value = strobeSpeed,
                        onValueChange = onStrobeSpeedChange,
                        valueRange = 10f..200f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}





// --- NEU: Wrapper für die Google Play Billing Library ---
class BillingClientWrapper(private val activity: Activity) {

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    // Kauf bestätigen, damit er nicht rückerstattet wird
                    val acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Nutzer ist jetzt Premium!
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
        billingClient.startConnection(object :
            BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Verbindung erfolgreich, prüfe bisherige Käufe
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Versuche, die Verbindung erneut herzustellen
                connectToGooglePlay()
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)

        billingClient.queryPurchasesAsync(params.build()) { _, purchases ->
            // Wenn die Liste der Käufe das Premium-Produkt enthält, setze den Status
            if (purchases.any { it.products.contains("remove_ads") }) { // "remove_ads" ist deine Produkt-ID
                PremiumManager.setPremium(true)
            }
        }
    }

    fun launchPurchaseFlow() {
        // Diese Produkt-ID musst du im Google Play Store erstellen!
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads") // Your product ID from Google Play Console
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            // Check billingResult and proceed if OK
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0] // Assuming you have one product
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                // Launch the billing flow
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
            // You might want to handle errors here (e.g., product not found)
        }
    }
}

// Der restliche Code (FeatureButton, ScreenLight, ShakeDetector, PowerButton, Previews etc.) bleibt unverändert.


    // Neue Composable für die Feature-Buttons (SOS, Strobe)
    @Composable
    fun FeatureButton(onClick: () -> Unit, iconRes: Int, text: String, isSelected: Boolean) {
        val buttonColor by animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                alpha = 0.2f
            ), label = ""
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

    // Composable für das Bildschirmlicht
    @Composable
    fun ScreenLight(onClose: () -> Unit) {
        val view = LocalView.current
        val window = (view.context as? Activity)?.window

        // Helligkeit beim Anzeigen maximieren und beim Verlassen zurücksetzen
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
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onClose))
    }

    // Composable für die Shake-Erkennung
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
                            Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
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


    // IconButton und PowerButton bleiben fast unverändert
    @Composable
    fun IconButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { content() }
    }

    @Composable
    fun PowerButton(isTorchOn: Boolean, onClick: () -> Unit) {
        val buttonColor by animateColorAsState(
            if (isTorchOn) Color(0xFFFDEE51) else Color(
                0xFF303030
            ), label = ""
        )
        val iconColor by animateColorAsState(
            if (isTorchOn) Color.Black else Color.White.copy(alpha = 0.6f),
            label = ""
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = if (isTorchOn) 30.dp else 10.dp, shape = CircleShape,
                    ambientColor = if (isTorchOn) Color(0xFFFDEE51) else Color.Black,
                    spotColor = if (isTorchOn) Color(0xFFFDEE51) else Color.Black
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

    // Previews müssen angepasst werden, um die neuen Parameter zu berücksichtigen
    @Preview(showBackground = true)
    @Composable
    fun FlashlightScreenPreviewOff() {
        MyLampTheme {
            FlashlightScreen(
                isTorchOn = false,
                lightMode = LightMode.NORMAL,
                strobeSpeed = 50f,
                onStrobeSpeedChange = {},
                onToggleClick = {},
                onSosClick = {},
                onStrobeClick = {},
                onScreenLightClick = {},
                isScreenLightOn = false,
                isPremium = false,
                onRemoveAdsClick = {}
            )
        }
    }
