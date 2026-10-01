package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.SmaGolden

@Composable
fun SparklineChart(
    prices: List<Double>,
    sma44: Double,
    isBullish: Boolean,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val lineColor = if (isBullish) BullGreen else BearRed
    val gradientColor = if (isBullish) BullGreen.copy(alpha = 0.25f) else BearRed.copy(alpha = 0.25f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val minPrice = (prices.minOrNull() ?: 0.0).coerceAtMost(sma44)
        val maxPrice = (prices.maxOrNull() ?: 1.0).coerceAtLeast(sma44)
        val range = (maxPrice - minPrice).coerceAtLeast(0.01)

        val width = size.width
        val height = size.height
        val stepX = width / (prices.size - 1)

        // 1. Draw 44 SMA dashed reference line
        val smaY = (height - ((sma44 - minPrice) / range * (height * 0.8f) + height * 0.1f)).toFloat()
        drawLine(
            color = SmaGolden.copy(alpha = 0.85f),
            start = Offset(0f, smaY),
            end = Offset(width, smaY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        )

        // 2. Build price curve path & fill path
        val path = Path()
        val fillPath = Path()

        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val y = (height - ((price - minPrice) / range * (height * 0.8f) + height * 0.1f)).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Gradient below price curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Main price stroke
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
