package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun EquityCurveChart(
    equityCurve: List<Pair<String, Double>>,
    initialCapital: Double,
    finalCapital: Double,
    modifier: Modifier = Modifier
) {
    val isProfitable = finalCapital >= initialCapital
    val curveColor = if (isProfitable) BullGreen else BearRed
    val gradientColor = if (isProfitable) BullGreen.copy(alpha = 0.25f) else BearRed.copy(alpha = 0.25f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PORTFOLIO EQUITY GROWTH",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format(Locale.US, "₹%,.0f → ₹%,.0f", initialCapital, finalCapital),
                color = if (isProfitable) BullGreen else BearRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalBackground)
        ) {
            if (equityCurve.size < 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trades recorded for this configuration", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                    val values = equityCurve.map { it.second }
                    val minVal = min(values.minOrNull() ?: initialCapital, initialCapital) * 0.98
                    val maxVal = max(values.maxOrNull() ?: initialCapital, initialCapital) * 1.02
                    val range = (maxVal - minVal).coerceAtLeast(10.0)

                    val width = size.width
                    val height = size.height
                    val stepX = width / (values.size - 1)

                    fun toY(cap: Double): Float {
                        return (height - ((cap - minVal) / range) * height).toFloat()
                    }

                    // 1. Initial capital baseline (dashed)
                    val baseY = toY(initialCapital)
                    drawLine(
                        color = TextMuted.copy(alpha = 0.5f),
                        start = Offset(0f, baseY),
                        end = Offset(width, baseY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )

                    // 2. Build curve
                    val path = Path()
                    val fillPath = Path()

                    values.forEachIndexed { i, cap ->
                        val x = i * stepX
                        val y = toY(cap)
                        if (i == 0) {
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

                    // Fill gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientColor, Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Line stroke
                    drawPath(
                        path = path,
                        color = curveColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
