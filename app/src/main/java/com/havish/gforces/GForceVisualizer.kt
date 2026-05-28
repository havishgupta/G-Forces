package com.havish.gforces

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    // Animate the dot for smoothness
    val animatedX by animateFloatAsState(
        targetValue = xGs,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "xGs"
    )
    val animatedY by animateFloatAsState(
        targetValue = yGs,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "yGs"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(100)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Draw concentric rings
                val rings = listOf(maxG * 0.33f, maxG * 0.66f, maxG)
                
                rings.forEachIndexed { index, gValue ->
                    val ringRadius = radius * (gValue / maxG)
                    drawCircle(
                        color = Color.DarkGray.copy(alpha = 0.7f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(
                            width = if (index == rings.lastIndex) 3.dp.toPx() else 1.dp.toPx(),
                            pathEffect = if (index < rings.lastIndex) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
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
                val normalizedX = (animatedX / maxG).coerceIn(-1f, 1f)
                val normalizedY = (animatedY / maxG).coerceIn(-1f, 1f)
                
                val dotX = center.x + (normalizedX * radius)
                val dotY = center.y - (normalizedY * radius) // -Y so forward accel goes UP

                // Draw Dot Halo
                drawCircle(
                    color = Color.Red.copy(alpha = 0.3f),
                    radius = 18.dp.toPx(),
                    center = Offset(dotX, dotY)
                )

                // Draw Dot
                drawCircle(
                    color = Color.Red,
                    radius = 12.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }

        // Display current Gs
        val totalG = sqrt(animatedX * animatedX + animatedY * animatedY)
        Text(
            text = String.format("%.2f G", totalG),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
