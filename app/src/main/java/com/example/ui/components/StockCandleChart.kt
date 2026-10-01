package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.SmaGolden
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ChartStyle {
    CANDLESTICK,
    AREA_LINE
}

@Composable
fun StockCandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(TerminalSurface),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading chart candles...", color = TextSecondary)
        }
        return
    }

    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }
    var chartStyle by remember { mutableStateOf(ChartStyle.CANDLESTICK) }
    var visibleBarsCount by remember { mutableIntStateOf(50) } // 30, 50, 80 bars

    val visibleCandles = candles.takeLast(visibleBarsCount.coerceAtMost(candles.size))
    val displayCandle = selectedCandleIndex?.let { if (it in visibleCandles.indices) visibleCandles[it] else null }
        ?: visibleCandles.last()

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalBackground)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Chart Controls & Toggles Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 44 SMA Legend badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SmaGolden)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "44 SMA: ₹%.2f".format(displayCandle.sma44),
                    color = SmaGolden,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                val diff = displayCandle.close - displayCandle.sma44
                val diffPct = if (displayCandle.sma44 > 0) (diff / displayCandle.sma44) * 100.0 else 0.0
                val diffColor = if (diff >= 0) BullGreen else BearRed
                val diffSign = if (diff >= 0) "+" else ""

                Text(
                    text = "%s%.2f%%".format(diffSign, diffPct),
                    color = diffColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Zoom (Bar count) & Chart Type Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bar counts (30, 50, All)
                listOf(30, 50, 80).forEach { count ->
                    val isSel = visibleBarsCount == count
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) SmaGolden.copy(alpha = 0.2f) else Color.Transparent)
                            .border(0.8.dp, if (isSel) SmaGolden else TerminalBorder, RoundedCornerShape(4.dp))
                            .clickable { visibleBarsCount = count }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${count}B",
                            color = if (isSel) SmaGolden else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Switch between Candlestick and Area/Line
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            chartStyle = if (chartStyle == ChartStyle.CANDLESTICK) ChartStyle.AREA_LINE else ChartStyle.CANDLESTICK
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (chartStyle == ChartStyle.CANDLESTICK) Icons.Default.BarChart else Icons.Default.ShowChart,
                        contentDescription = "Chart Style",
                        tint = SmaGolden,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // OHLCV Header HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalSurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OhlcItem(label = "O", value = displayCandle.open)
            OhlcItem(label = "H", value = displayCandle.high)
            OhlcItem(label = "L", value = displayCandle.low)
            OhlcItem(
                label = "C",
                value = displayCandle.close,
                color = if (displayCandle.isBullish) BullGreen else BearRed
            )
            OhlcItem(label = "Vol", valueStr = formatVolume(displayCandle.volume))
            Text(
                text = dateFormat.format(Date(displayCandle.timestamp)),
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Chart Canvas: Candlesticks + 44 SMA Curve + Current Price Line + Volume Sub-chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(visibleCandles) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val candleWidth = size.width / visibleCandles.size
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                                selectedCandleIndex = index
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val candleWidth = size.width / visibleCandles.size
                                val index = (change.position.x / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                                selectedCandleIndex = index
                            },
                            onDragEnd = { selectedCandleIndex = null },
                            onDragCancel = { selectedCandleIndex = null }
                        )
                    }
                    .pointerInput(visibleCandles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val candleWidth = size.width / visibleCandles.size
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                                selectedCandleIndex = if (selectedCandleIndex == index) null else index
                            }
                        )
                    }
            ) {
                drawTradingChart(
                    candles = visibleCandles,
                    chartStyle = chartStyle,
                    selectedIndex = selectedCandleIndex
                )
            }
        }
    }
}

private fun DrawScope.drawTradingChart(
    candles: List<Candle>,
    chartStyle: ChartStyle,
    selectedIndex: Int?
) {
    if (candles.isEmpty()) return

    val minPrice = min(
        candles.minOf { it.low },
        candles.filter { it.sma44 > 0 }.minOfOrNull { it.sma44 } ?: candles.first().low
    ) * 0.995
    val maxPrice = max(
        candles.maxOf { it.high },
        candles.filter { it.sma44 > 0 }.maxOfOrNull { it.sma44 } ?: candles.first().high
    ) * 1.005

    val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)
    val width = size.width
    val fullHeight = size.height

    // Main candle pane takes top 80%, Volume pane takes bottom 20%
    val mainPaneHeight = fullHeight * 0.80f
    val volumePaneHeight = fullHeight * 0.18f
    val volumeTopY = fullHeight * 0.82f

    val candleStep = width / candles.size
    val candleBodyWidth = max(2.5f, candleStep * 0.65f)

    fun toY(price: Double): Float {
        return (mainPaneHeight - ((price - minPrice) / priceRange) * mainPaneHeight).toFloat()
    }

    // 1. Grid Lines
    val gridLevels = listOf(0.20, 0.45, 0.70)
    gridLevels.forEach { ratio ->
        val y = (mainPaneHeight * ratio).toFloat()
        drawLine(
            color = TerminalBorder.copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )
    }

    // 2. Volume Sub-Pane (Histogram at bottom)
    val maxVolume = candles.maxOfOrNull { it.volume }?.coerceAtLeast(1L) ?: 1L
    candles.forEachIndexed { i, candle ->
        val centerX = (i + 0.5f) * candleStep
        val barHeight = ((candle.volume.toFloat() / maxVolume) * volumePaneHeight).coerceAtLeast(1f)
        val barTop = fullHeight - barHeight
        val volColor = if (candle.isBullish) BullGreen.copy(alpha = 0.45f) else BearRed.copy(alpha = 0.45f)

        drawRect(
            color = volColor,
            topLeft = Offset(centerX - candleBodyWidth / 2f, barTop),
            size = Size(candleBodyWidth, barHeight)
        )
    }

    // 3. Main Chart Rendering: Candlesticks or Area Line
    if (chartStyle == ChartStyle.CANDLESTICK) {
        candles.forEachIndexed { i, candle ->
            val centerX = (i + 0.5f) * candleStep
            val openY = toY(candle.open)
            val closeY = toY(candle.close)
            val highY = toY(candle.high)
            val lowY = toY(candle.low)

            val isBullish = candle.isBullish
            val candleColor = if (isBullish) BullGreen else BearRed

            // Wick
            drawLine(
                color = candleColor,
                start = Offset(centerX, highY),
                end = Offset(centerX, lowY),
                strokeWidth = 1.2.dp.toPx()
            )

            // Body
            val topY = min(openY, closeY)
            val bottomY = max(openY, closeY)
            val bodyHeight = max(2.dp.toPx(), bottomY - topY)

            drawRect(
                color = candleColor,
                topLeft = Offset(centerX - candleBodyWidth / 2f, topY),
                size = Size(candleBodyWidth, bodyHeight)
            )

            // Support bounce triangular marker when candle low touches 44 SMA
            val touchesSma = abs(candle.low - candle.sma44) / candle.sma44 < 0.003
            if (touchesSma && candle.isBullish && candle.sma44 > 0) {
                val markerY = lowY + 6.dp.toPx()
                val path = Path().apply {
                    moveTo(centerX, markerY)
                    lineTo(centerX - 4.dp.toPx(), markerY + 6.dp.toPx())
                    lineTo(centerX + 4.dp.toPx(), markerY + 6.dp.toPx())
                    close()
                }
                drawPath(path, color = SmaGolden)
            }
        }
    } else {
        // Area Line style
        val pricePath = Path()
        val fillPath = Path()
        val isOverallBullish = candles.last().close >= candles.first().close
        val areaColor = if (isOverallBullish) BullGreen else BearRed

        candles.forEachIndexed { i, candle ->
            val x = (i + 0.5f) * candleStep
            val y = toY(candle.close)
            if (i == 0) {
                pricePath.moveTo(x, y)
                fillPath.moveTo(x, mainPaneHeight)
                fillPath.lineTo(x, y)
            } else {
                pricePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, mainPaneHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(areaColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = mainPaneHeight
            )
        )
        drawPath(
            path = pricePath,
            color = areaColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // 4. Draw 44 SMA Line (Golden Moving Average Curve Overlay)
    val smaPath = Path()
    var isSmaStarted = false

    candles.forEachIndexed { i, candle ->
        if (candle.sma44 > 0) {
            val x = (i + 0.5f) * candleStep
            val y = toY(candle.sma44)
            if (!isSmaStarted) {
                smaPath.moveTo(x, y)
                isSmaStarted = true
            } else {
                smaPath.lineTo(x, y)
            }
        }
    }

    // Ambient glow
    drawPath(
        path = smaPath,
        color = SmaGolden.copy(alpha = 0.35f),
        style = Stroke(width = 5.dp.toPx())
    )
    // Core sharp golden line
    drawPath(
        path = smaPath,
        color = SmaGolden,
        style = Stroke(width = 2.dp.toPx())
    )

    // 5. Current Live Price Line & 44 SMA Line
    val lastCandle = candles.last()
    val lastLtpY = toY(lastCandle.close)
    val lastSmaY = toY(lastCandle.sma44)

    // Horizontal dashed live price line
    val liveColor = if (lastCandle.isBullish) BullGreen else BearRed
    drawLine(
        color = liveColor.copy(alpha = 0.7f),
        start = Offset(0f, lastLtpY),
        end = Offset(width, lastLtpY),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    )

    // 6. Crosshair on Touch Drag
    if (selectedIndex != null && selectedIndex in candles.indices) {
        val selCandle = candles[selectedIndex]
        val selX = (selectedIndex + 0.5f) * candleStep
        val selY = toY(selCandle.close)

        // Vertical line
        drawLine(
            color = TextSecondary.copy(alpha = 0.6f),
            start = Offset(selX, 0f),
            end = Offset(selX, fullHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
        )
        // Horizontal line
        drawLine(
            color = TextSecondary.copy(alpha = 0.6f),
            start = Offset(0f, selY),
            end = Offset(width, selY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
        )
        // Crosshair intersection node
        drawCircle(
            color = SmaGolden,
            radius = 4.dp.toPx(),
            center = Offset(selX, selY)
        )
    }
}

@Composable
private fun OhlcItem(
    label: String,
    value: Double? = null,
    valueStr: String? = null,
    color: Color = TextPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valueStr ?: String.format(Locale.US, "%.1f", value ?: 0.0),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatVolume(volume: Long): String {
    return when {
        volume >= 10_000_000 -> String.format(Locale.US, "%.1fCr", volume / 10_000_000.0)
        volume >= 100_000 -> String.format(Locale.US, "%.1fL", volume / 100_000.0)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000.0)
        else -> volume.toString()
    }
}
