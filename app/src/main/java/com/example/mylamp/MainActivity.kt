package com.example.mylamp // Korrekter Package-Name, passend zu deiner build.gradle.kts

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylamp.ui.theme.MyLampTheme

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

    var isTorchOn by remember { mutableStateOf(false) }

    // --- NEW: State for managing backgrounds ---
    // List of all your background drawable resources
    val backgroundImages = remember {
        listOf(
            R.drawable.background_off, // Assuming this is your first image
            // Add your other images here
            R.drawable.background_1,
            R.drawable.background_2,
            R.drawable.background_3,
            R.drawable.background_4,
            R.drawable.background_5,
        )
    }
    var currentBackgroundIndex by remember { mutableStateOf(0) }
    // -----------------------------------------

    FlashlightScreen(
        isTorchOn = isTorchOn,
        onToggleClick = {
            isTorchOn = !isTorchOn
            try {
                cameraManager.setTorchMode(cameraId, isTorchOn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        // --- NEW: Pass background info and switcher logic down ---
        currentBackgroundResId = backgroundImages[currentBackgroundIndex],
        onBackgroundChangeClick = {
            // Cycle to the next image, or back to the start if at the end
            currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.size
        }
        // --------------------------------------------------------
    )
}

@Composable
fun FlashlightScreen(
    isTorchOn: Boolean,
    onToggleClick: () -> Unit,
    currentBackgroundResId: Int,
    onBackgroundChangeClick: () -> Unit
) {
    // --- THIS ANIMATION IS NOW REMOVED ---
    // val animatedBackgroundColor by animateColorAsState(...)

    Surface(
        modifier = Modifier.fillMaxSize(),
        // --- THIS COLOR IS NOW ALWAYS BLACK ---
        color = Color.Black
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Background Image now uses the dynamic resource ID
            Image(
                painter = painterResource(id = currentBackgroundResId),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // --- THIS ALPHA VALUE IS NOW THE ONLY THING THAT CREATES THE "ON" EFFECT ---
                alpha = if (isTorchOn) 0.5f else 0.4f
            )

            // Background Switcher Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, end = 24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = onBackgroundChangeClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_change_background),
                        contentDescription = "Change Background",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Main UI content (the power button and text)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PowerButton(
                    isTorchOn = isTorchOn,
                    onClick = onToggleClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isTorchOn) "LIGHT ON" else "LIGHT OFF",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f)
                    )
                )
            }
        }
    }
}



// --- NEW: A simple, reusable Icon Button Composable ---
@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
// ----------------------------------------------------


@Composable
fun PowerButton(
    isTorchOn: Boolean,
    onClick: () -> Unit
) {
    val buttonColor by animateColorAsState(
        if (isTorchOn) Color(0xFFFDEE51) else Color(0xFF303030),
        label = "buttonColor"
    )
    val iconColor by animateColorAsState(
        if (isTorchOn) Color.Black else Color.White.copy(alpha = 0.6f),
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .size(150.dp)
            .shadow(
                elevation = if (isTorchOn) 30.dp else 10.dp,
                shape = CircleShape,
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


@Preview(showBackground = true)
@Composable
fun FlashlightScreenPreviewOff() {
    MyLampTheme {
        // Pass a dummy background and empty lambda for the preview
        FlashlightScreen(isTorchOn = false, onToggleClick = {}, currentBackgroundResId = R.drawable.background_off, onBackgroundChangeClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun FlashlightScreenPreviewOn() {
    MyLampTheme {
        FlashlightScreen(isTorchOn = true, onToggleClick = {}, currentBackgroundResId = R.drawable.background_off, onBackgroundChangeClick = {})
    }
}
