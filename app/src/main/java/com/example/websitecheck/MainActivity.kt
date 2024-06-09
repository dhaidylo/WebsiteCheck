package com.example.websitecheck


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.websitecheck.ui.theme.WebsiteCheckTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            log("Notification permission granted")
        } else {
            log("Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) -> {
                    log("Notification permission already granted")
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            var settings by remember { mutableStateOf(Settings()) }

            LaunchedEffect(Unit) {
                settings = readSettings(this@MainActivity)
            }
            WebsiteCheckTheme {
                MainScreen(
                    settings,
                    onStartService = {
                        runBlocking {
                            setSettings(this@MainActivity, it)
                        }
                        actionOnService(Actions.START) },
                    onStopService = { actionOnService(Actions.STOP) }
                )
            }
        }
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, WebsiteCheckService::class.java).also {
            it.action = action.name
            startForegroundService(it)
        }
    }
}

@Composable
fun MainScreen(settings: Settings, onStartService: (Settings) -> Unit, onStopService: () -> Unit) {
    var minArea by remember { mutableStateOf(settings.minArea.toString()) }
    var maxArea by remember { mutableStateOf(settings.maxArea.toString()) }
    var maxPrice by remember { mutableStateOf(settings.maxPrice.toString()) }
    var checkInterval by remember { mutableStateOf(settings.checkInterval.toString()) }

    LaunchedEffect(settings) {
        minArea = settings.minArea.toString()
        maxArea = settings.maxArea.toString()
        maxPrice = settings.maxPrice.toString()
        checkInterval = settings.checkInterval.toString()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.LightGray)
                .fillMaxWidth()
        ) {
            Text("Immowelt", fontSize = 25.sp)
            OutlinedTextField(
                value = minArea,
                modifier = Modifier.padding(8.dp),
                label = { Text("Min area, qm") },
                onValueChange = { minArea = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = maxArea,
                modifier = Modifier.padding(8.dp),
                label = { Text("Max area, qm") },
                onValueChange = { maxArea = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = maxPrice,
                modifier = Modifier.padding(8.dp),
                label = { Text("Max price, Euro") },
                onValueChange = { maxPrice = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        OutlinedTextField(
            value = checkInterval,
            modifier = Modifier.padding(8.dp),
            label = { Text("Check interval, seconds") },
            onValueChange = { checkInterval = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(onClick = { onStartService(Settings(
            minArea.toIntOrNull() ?: settings.minArea,
            maxArea.toIntOrNull() ?: settings.maxArea,
            maxPrice.toIntOrNull() ?: settings.maxPrice,
            checkInterval.toIntOrNull() ?: settings.checkInterval
        )) }) {
            Text(text = "Start Service")
        }
        Button(onClick = onStopService) {
            Text(text = "Stop Service")
        }
    }
}