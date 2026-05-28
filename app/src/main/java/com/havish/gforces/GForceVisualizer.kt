package com.havish.gforces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun GForceVisualizer(
    xGs: Float,
    yGs: Float,
    maxG: Float = 1.5f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw concentric rings (e.g., 0.5G, 1.0G, 1.5G)
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringRadius = radius * (i / ringCount.toFloat())
                drawCircle(
                    color = Color.DarkGray,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = if (i < ringCount) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                    )
                )
            }

            // Crosshairs
            drawLine(
                color = Color.DarkGray,
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Calculate dot position
            val normalizedX = (xGs / maxG).coerceIn(-1f, 1f)
            val normalizedY = (yGs / maxG).coerceIn(-1f, 1f)
            
            // Invert Y so positive G (braking/forward tilt) goes UP in the circle if preferred, 
            // but standard Android Y is positive downwards. Let's invert Y for a car dashboard feel 
            // where acceleration (backward tilt) makes the dot go down, braking makes it go up.
            val dotX = center.x + (normalizedX * radius)
            val dotY = center.y - (normalizedY * radius)

            // Draw Dot
            drawCircle(
                color = Color.Red,
                radius = 12.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }

        // Display current Gs
        val totalG = sqrt(xGs * xGs + yGs * yGs)
        Text(
            text = String.format("%.2f G", totalG),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
