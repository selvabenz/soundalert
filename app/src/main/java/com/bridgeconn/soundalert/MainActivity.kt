package com.bridgeconn.soundalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bridgeconn.soundalert.ui.SoundAlertTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundAlertTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoundAlertApp()
                }
            }
        }
    }
}

@Composable
private fun SoundAlertApp() {
    val context = LocalContext.current
    val serviceState by AppBus.serviceState.collectAsState()
    val lastAlert by AppBus.lastAlert.collectAsState()

    var mode by rememberSaveable { mutableStateOf(AlertMode.ROAD) }
    var sensitivity by rememberSaveable { mutableFloatStateOf(0.70f) }
    var cooldownMs by rememberSaveable { mutableLongStateOf(2500L) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (audioGranted) {
            permissionMessage = null
            SoundDetectionService.start(context, mode, sensitivity, cooldownMs)
        } else {
            permissionMessage = "Microphone permission is required to detect horns and door sounds."
        }
    }

    fun requestStart() {
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isEmpty()) {
            SoundDetectionService.start(context, mode, sensitivity, cooldownMs)
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(mode, sensitivity, cooldownMs, serviceState.running) {
        if (serviceState.running) {
            SoundDetectionService.update(context, mode, sensitivity, cooldownMs)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SoundAlert", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Sound awareness through distinctive vibration and large visual alerts.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatusCard(serviceState, lastAlert)

            Text("Choose mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeButton(
                    modifier = Modifier.weight(1f),
                    selected = mode == AlertMode.ROAD,
                    title = "ROAD",
                    subtitle = "Vehicle horns",
                    symbol = "H",
                    onClick = { mode = AlertMode.ROAD }
                )
                ModeButton(
                    modifier = Modifier.weight(1f),
                    selected = mode == AlertMode.HOME,
                    title = "HOME",
                    subtitle = "Doorbell / knocks",
                    symbol = "D",
                    onClick = { mode = AlertMode.HOME }
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Detection sensitivity", fontWeight = FontWeight.SemiBold)
                        Text("${(sensitivity * 100).roundToInt()}%")
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.30f..0.95f
                    )
                    Text(
                        "Raise this if real alerts are missed. Lower it if background noise causes false alerts.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Repeat cooldown", fontWeight = FontWeight.SemiBold)
                        Text(String.format("%.1f s", cooldownMs / 1000f))
                    }
                    Slider(
                        value = cooldownMs.toFloat(),
                        onValueChange = { cooldownMs = it.roundToInt().toLong() },
                        valueRange = 1000f..6000f
                    )
                }
            }

            if (permissionMessage != null) {
                Text(
                    permissionMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    if (serviceState.running) SoundDetectionService.stop(context) else requestStart()
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = if (serviceState.running) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (serviceState.running) "STOP LISTENING" else "START LISTENING", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    Haptics.vibrate(context, mode)
                    AppBus.publishAlert(
                        DetectedAlert(
                            label = if (mode == AlertMode.ROAD) SoundLabel.HORN else SoundLabel.DOORBELL_OR_KNOCK,
                            confidence = 1f,
                            detail = "manual vibration test"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (mode == AlertMode.ROAD) "TEST HORN VIBRATION" else "TEST DOORBELL VIBRATION", fontWeight = FontWeight.Bold)
            }

            VibrationPatternCard(mode)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy", fontWeight = FontWeight.Bold)
                    Text("Microphone audio is analyzed on the phone. This version does not save recordings and has no Internet permission.")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Road-safety note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        "Use Road Alert only as a supplementary awareness aid. A phone microphone can miss horns because of wind, distance, traffic noise, clothing, phone placement, or hardware limits. Never rely on this app as the sole warning system while travelling.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusCard(state: ServiceState, alert: DetectedAlert?) {
    val activeAlert = alert?.takeIf { System.currentTimeMillis() - it.timestampMillis < 15_000 }
    val hornAlert = activeAlert?.label == SoundLabel.HORN
    val container = when {
        activeAlert != null -> if (hornAlert) Color(0xFFFFE0B2) else Color(0xFFDDEBFF)
        state.running -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(34.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(34.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        activeAlert != null && hornAlert -> "H!"
                        activeAlert != null -> "D!"
                        state.running -> "ON"
                        else -> "OFF"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
            Text(
                when {
                    activeAlert != null && hornAlert -> "HORN DETECTED"
                    activeAlert != null -> "DOOR SOUND DETECTED"
                    else -> state.status
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            if (activeAlert != null) {
                Text(
                    "Confidence ${(activeAlert.confidence * 100).roundToInt()}% • ${activeAlert.detail}",
                    textAlign = TextAlign.Center
                )
            } else if (state.running) {
                Text("Keep the phone microphone unobstructed.", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ModeButton(
    modifier: Modifier,
    selected: Boolean,
    title: String,
    subtitle: String,
    symbol: String,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(96.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(title, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(96.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(title, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun VibrationPatternCard(mode: AlertMode) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Distinct vibration", fontWeight = FontWeight.Bold)
            if (mode == AlertMode.ROAD) {
                Text("HORN:  long  •  short  •  long", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("700 ms vibration → short pause → 190 ms → short pause → 700 ms")
            } else {
                Text("DOOR:  short • short  — pause —  short • short", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Two quick pulses → longer pause → two quick pulses")
            }
            Text("These patterns are intentionally different from a continuous call-style vibration.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
