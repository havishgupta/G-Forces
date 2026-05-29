package com.havish.gforces

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sensorManagerWrapper = SensorManagerWrapper(this)
        
        setContent {
            val scope = rememberCoroutineScope()
            var showSettings by remember { mutableStateOf(false) }
            
            // Read settings
            var maxG by remember { mutableFloatStateOf(sensorManagerWrapper.prefs.maxG) }
            var isDebugMode by remember { mutableStateOf(sensorManagerWrapper.prefs.isDebugMode) }
            var useTrueGForceMode by remember { mutableStateOf(sensorManagerWrapper.prefs.useTrueGForceMode) }

            val gravityData by remember { sensorManagerWrapper.getGForceData() }.collectAsState(
                initial = GForceData(0f, 0f, 0f, 0f, 0f)
            )

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("G-Forces", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                        actions = {
                            Button(
                                onClick = { sensorManagerWrapper.calibrate() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Calibrate")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showSettings = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Settings")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDebugMode) {
                            Text(
                                text = "Debug Data\nRaw X: ${"%.2f".format(gravityData.rawX)}\nRaw Y: ${"%.2f".format(gravityData.rawY)}\nRaw Z: ${"%.2f".format(gravityData.rawZ)}\nLatG: ${"%.2f".format(gravityData.lateralG)} LonG: ${"%.2f".format(gravityData.longitudinalG)}",
                                color = Color.Green,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        GForceVisualizer(
                            xGs = gravityData.lateralG,
                            yGs = gravityData.longitudinalG,
                            maxG = maxG,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            }

            if (showSettings) {
                AlertDialog(
                    onDismissRequest = { showSettings = false },
                    title = { Text("Settings") },
                    text = {
                        Column {
                            Text("Max Scale (G)")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf(1.5f, 3.0f, 5.0f).forEach { scale ->
                                    FilterChip(
                                        selected = maxG == scale,
                                        onClick = {
                                            maxG = scale
                                            sensorManagerWrapper.prefs.maxG = scale
                                        },
                                        label = { Text("${scale}G") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("True G-Force Mode")
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = useTrueGForceMode,
                                    onCheckedChange = {
                                        useTrueGForceMode = it
                                        sensorManagerWrapper.prefs.useTrueGForceMode = it
                                    }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Debug Mode")
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = isDebugMode,
                                    onCheckedChange = {
                                        isDebugMode = it
                                        sensorManagerWrapper.prefs.isDebugMode = it
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettings = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
