package com.example.mylamp // Korrekter Package-Name, passend zu deiner build.gradle.kts

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylamp.ui.theme.MyLampTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enum zur Verwaltung der verschiedenen Licht-Modi
enum class LightMode {
    NORMAL,
    SOS,
    STROBE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLampTheme {
                FlashlightRoot()
            }
        }
    }
}

@Composable
fun FlashlightRoot() {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember { cameraManager.cameraIdList[0] }
    val coroutineScope = rememberCoroutineScope()

    // Zustandsvariablen für alle Funktionen
    var isTorchOn by remember { mutableStateOf(false) }
    var lightMode by remember { mutableStateOf(LightMode.NORMAL) }
    var isScreenLightOn by remember { mutableStateOf(false) }
    var blinkerJob by remember { mutableStateOf<Job?>(null) }
    var strobeSpeed by remember { mutableStateOf(50f) } // 50ms delay

    // Shake-to-Toggle Logik
    ShakeDetector {
        if (!isScreenLightOn) { // Deaktivieren, wenn Screenlight an ist
            val newTorchState = !isTorchOn
            isTorchOn = newTorchState
            lightMode = LightMode.NORMAL // Beim Schütteln immer in den Normalmodus
            blinkerJob?.cancel() // Alle Blink-Effekte stoppen
            try {
                cameraManager.setTorchMode(cameraId, newTorchState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Aufräumen: Wenn die App verlassen wird, alles ausschalten
    DisposableEffect(Unit) {
        onDispose {
            blinkerJob?.cancel()
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: Exception) { /* Ignorieren */ }
        }
    }

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
                val sosPattern = longArrayOf(100, 100, 100, 100, 100, 400, 400, 100, 400, 100, 400, 400, 100, 100, 100, 100, 100, 800)
                while (true) {
                    for (i in sosPattern.indices) {
                        setTorch(i % 2 == 0)
                        delay(sosPattern[i])
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
                    delay(strobeSpeed.toLong())
                }
            }
        }
    }

    // Logik für Screen-Light
    if (isScreenLightOn) {
        ScreenLight { isScreenLightOn = false }
    } else {
        FlashlightScreen(
            isTorchOn = isTorchOn,
            lightMode = lightMode,
            isScreenLightOn = isScreenLightOn, // <-- HIER HINZUFÜGEN
            strobeSpeed = strobeSpeed,
            onStrobeSpeedChange = { strobeSpeed = it },
            onToggleClick = onToggleClick,
            onSosClick = onSosClick,
            onStrobeClick = onStrobeClick,
            onScreenLightClick = {
                blinkerJob?.cancel()
                setTorch(false)
                isScreenLightOn = true
            }
        )
    }
}

// Die Haupt-UI, jetzt mit viel mehr Parametern
@Composable
fun FlashlightScreen(
    isTorchOn: Boolean,
    lightMode: LightMode,
    isScreenLightOn: Boolean, // <-- NEUER PARAMETER
    strobeSpeed: Float,
    onStrobeSpeedChange: (Float) -> Unit,
    onToggleClick: () -> Unit,
    onSosClick: () -> Unit,
    onStrobeClick: () -> Unit,
    onScreenLightClick: () -> Unit
) {
    // Hintergrund-Logik bleibt unverändert
    val backgroundImages = remember {
        listOf(
            R.drawable.background_off, R.drawable.background_1, R.drawable.background_2,
            R.drawable.background_3, R.drawable.background_4, R.drawable.background_5,
        )
    }
    var currentBackgroundIndex by remember { mutableStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            val alpha by animateFloatAsState(targetValue = if (isTorchOn) 0.5f else 0.4f, label = "")
            Image(
                painter = painterResource(id = backgroundImages[currentBackgroundIndex]),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = alpha
            )

            // Buttons oben rechts
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.size }) {
                    Icon(painter = painterResource(id = R.drawable.ic_change_background), contentDescription = "Change Background", tint = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = onScreenLightClick) {
                    Icon(painter = painterResource(id = R.drawable.ic_phone_android), contentDescription = "Screen Light", tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // Haupt-UI in der Mitte
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PowerButton(isTorchOn = isTorchOn, onClick = onToggleClick)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isTorchOn) lightMode.name else "LIGHT OFF",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f))
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Zusätzliche Funktionsbuttons (SOS, Strobe)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureButton(onClick = onSosClick, iconRes = R.drawable.ic_sos, text = "SOS", isSelected = lightMode == LightMode.SOS)
                    FeatureButton(onClick = onStrobeClick, iconRes = R.drawable.ic_strobe, text = "Strobe", isSelected = lightMode == LightMode.STROBE)
                }

                // Stroboskop-Slider (wird nur im STROBE-Modus sichtbar)
                AnimatedVisibility(visible = lightMode == LightMode.STROBE, enter = fadeIn(), exit = fadeOut()) {
                    Slider(
                        value = strobeSpeed,
                        onValueChange = onStrobeSpeedChange,
                        valueRange = 20f..500f, // 20ms (schnell) bis 500ms (langsam)
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

// Neue Composable für die Feature-Buttons (SOS, Strobe)
@Composable
fun FeatureButton(onClick: () -> Unit, iconRes: Int, text: String, isSelected: Boolean) {
    val buttonColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), label = "")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(buttonColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = text, tint = Color.White)
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
                params.screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = params
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.White).clickable(onClick = onClose))
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

                    val speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
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

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
}


// IconButton und PowerButton bleiben fast unverändert
@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun PowerButton(isTorchOn: Boolean, onClick: () -> Unit) {
    val buttonColor by animateColorAsState(if (isTorchOn) Color(0xFFFDEE51) else Color(0xFF303030), label = "")
    val iconColor by animateColorAsState(if (isTorchOn) Color.Black else Color.White.copy(alpha = 0.6f), label = "")
    Box(
        modifier = Modifier.size(150.dp)
            .shadow(
                elevation = if (isTorchOn) 30.dp else 10.dp, shape = CircleShape,
                ambientColor = if (isTorchOn) Color(0xFFFDEE51) else Color.Black,
                spotColor = if (isTorchOn) Color(0xFFFDEE51) else Color.Black
            )
            .clip(CircleShape).background(buttonColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_power), contentDescription = "Toggle Flashlight", tint = iconColor, modifier = Modifier.size(60.dp))
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
            isScreenLightOn = false
        )
    }
}
