package com.havish.gforces

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sensorManagerWrapper = SensorManagerWrapper(this)
        
        setContent {
            val gravityData by remember { sensorManagerWrapper.getGravityData() }.collectAsState(
                initial = GForceData(0f, 0f, 0f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                GForceVisualizer(
                    xGs = gravityData.x,
                    yGs = gravityData.y,
                    modifier = Modifier.fillMaxSize(0.8f)
                )
            }
        }
    }
}
