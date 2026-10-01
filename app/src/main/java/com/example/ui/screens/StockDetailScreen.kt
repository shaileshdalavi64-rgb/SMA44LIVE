package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlertConditionType
import com.example.data.model.Stock
import com.example.data.model.Timeframe
import com.example.ui.components.SignalBadge
import com.example.ui.components.SmaSlopeBadge
import com.example.ui.components.StockCandleChart
import com.example.ui.theme.BearRed
import com.example.ui.theme.BearRedBg
import com.example.ui.theme.BullGreen
import com.example.ui.theme.BullGreenBg
import com.example.ui.theme.SmaGolden
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AlertViewModel
import com.example.ui.viewmodel.MarketViewModel
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    marketViewModel: MarketViewModel,
    alertViewModel: AlertViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBacktest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allStocks by marketViewModel.allStocks.collectAsStateWithLifecycle()
    val stock = allStocks.find { it.symbol == symbol }
    val candles by marketViewModel.selectedStockCandles.collectAsStateWithLifecycle()
    val currentTimeframe by marketViewModel.timeframe.collectAsStateWithLifecycle()

    var showCreateAlertDialog by remember { mutableStateOf(false) }

    if (stock == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TerminalBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Stock not found", color = TextSecondary)
        }
        return
    }

    val isPositive = stock.change >= 0
    val changeColor = if (isPositive) BullGreen else BearRed
    val sign = if (isPositive) "+" else ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "NSE", color = SmaGolden, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = stock.name,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { marketViewModel.refreshRealNsePrices() },
                modifier = Modifier.testTag("detail_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh NSE quote",
                    tint = SmaGolden
                )
            }

            IconButton(
                onClick = { marketViewModel.toggleWatchlist(stock.symbol) }
            ) {
                Icon(
                    imageVector = if (stock.isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Watchlist",
                    tint = if (stock.isWatchlist) SmaGolden else TextMuted
                )
            }
        }

        // Live Price & 44 SMA Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "LAST TRADED PRICE",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", stock.ltp),
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%s%.2f (%s%.2f%%) Today", sign, stock.change, sign, stock.changePercent),
                            color = changeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        SignalBadge(signal = stock.signal)
                        Spacer(modifier = Modifier.height(6.dp))
                        SmaSlopeBadge(slope = stock.smaSlope)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 44 SMA Quick Metrics Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("44 SMA LEVEL", color = SmaGolden, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", stock.sma44),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DISTANCE FROM SMA", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        val dist = stock.distanceFromSmaPercent
                        val distColor = if (abs(dist) <= 0.6) SmaGolden else if (dist > 0) BullGreen else BearRed
                        Text(
                            text = String.format(Locale.US, "%s%.2f%%", if (dist >= 0) "+" else "", dist),
                            color = distColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("SLOPE ANGLE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (stock.smaSlope > 0) "+${String.format(Locale.US, "%.2f", stock.smaSlope)}" else String.format(Locale.US, "%.2f", stock.smaSlope),
                            color = if (stock.smaSlope > 0) BullGreen else BearRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Timeframe selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Timeframe.entries.forEach { tf ->
                val isSelected = tf == currentTimeframe
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) SmaGolden else TerminalSurface)
                        .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(6.dp))
                        .clickable { marketViewModel.setTimeframe(tf) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Candlestick Chart with 44 SMA Line
        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            StockCandleChart(candles = candles)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 44 SMA Technical Strategy Insight Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TerminalSurface)
                .border(1.dp, SmaGolden.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = SmaGolden,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "44 SMA TRADE SETUP ANALYSIS",
                        color = SmaGolden,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val setupText = when {
                    stock.smaSlope > 0.03 && abs(stock.distanceFromSmaPercent) <= 0.6 ->
                        "🟢 HIGH PROBABILITY BUY SETUP: 44 SMA is rising with strong upward momentum. Price is re-testing the 44 SMA as dynamic support. Look for entry above the high of the support candle with Stop Loss placed below the swing low."
                    stock.smaSlope > 0.03 && stock.distanceFromSmaPercent > 2.0 ->
                        "🟡 EXTENDED RISING TREND: Stock is in strong uptrend above 44 SMA, but currently stretched >2% away. Wait for a pullback towards 44 SMA (₹${String.format(Locale.US, "%.2f", stock.sma44)}) for optimal risk-reward entry."
                    stock.smaSlope < -0.03 && abs(stock.distanceFromSmaPercent) <= 0.6 ->
                        "🔴 SHORT SELL SETUP: 44 SMA is sloping downward. Price is testing 44 SMA as dynamic overhead resistance. Look for short entry on break of low of rejection candle with Stop Loss above swing high."
                    stock.smaSlope < -0.03 ->
                        "🔴 BEARISH DOWNTREND: Stock is trending consistently below falling 44 SMA. Avoid long swing positions until price breaks and establishes above 44 SMA."
                    else ->
                        "⚪ CONSOLIDATION / NEUTRAL: 44 SMA is sideways. Wait for a breakout candle or clear directional slope before placing directional swing trades."
                }

                Text(
                    text = setupText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Recommended SL & Target based on 44 SMA setup
                val estSl = if (stock.smaSlope > 0) stock.sma44 * 0.992 else stock.sma44 * 1.008
                val estRisk = abs(stock.ltp - estSl)
                val estTarget = if (stock.smaSlope > 0) stock.ltp + (estRisk * 2.0) else stock.ltp - (estRisk * 2.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalSurfaceElevated)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Suggested SL", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", estSl),
                            color = BearRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Risk : Reward", color = TextMuted, fontSize = 10.sp)
                        Text("1 : 2.0", color = SmaGolden, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target (1:2)", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", estTarget),
                            color = BullGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Key Market Statistics Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "KEY MARKET STATS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell("Day High", String.format(Locale.US, "₹%,.2f", stock.high), Modifier.weight(1f))
                    StatCell("Day Low", String.format(Locale.US, "₹%,.2f", stock.low), Modifier.weight(1f))
                    StatCell("Prev Close", String.format(Locale.US, "₹%,.2f", stock.prevClose), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell("52W High", String.format(Locale.US, "₹%,.2f", stock.week52High), Modifier.weight(1f))
                    StatCell("52W Low", String.format(Locale.US, "₹%,.2f", stock.week52Low), Modifier.weight(1f))
                    StatCell("P/E Ratio", String.format(Locale.US, "%.1f", stock.peRatio), Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Set Alert & Backtest 44 SMA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCreateAlertDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("set_alert_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set 44 SMA Alert", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { onNavigateToBacktest(stock.symbol) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SmaGolden)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("backtest_stock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    tint = SmaGolden,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Backtest 44 SMA", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Quick Alert Creator Bottom Sheet
    if (showCreateAlertDialog) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCreateAlertDialog = false },
            sheetState = sheetState,
            containerColor = TerminalSurface
        ) {
            CreateAlertContent(
                stock = stock,
                onDismiss = { showCreateAlertDialog = false },
                onConfirm = { condition, targetValue ->
                    alertViewModel.createAlert(
                        symbol = stock.symbol,
                        stockName = stock.name,
                        condition = condition,
                        targetValue = targetValue,
                        currentLtp = stock.ltp,
                        smaValue = stock.sma44
                    )
                    showCreateAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreateAlertContent(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (AlertConditionType, Double) -> Unit
) {
    var selectedCondition by remember { mutableStateOf(AlertConditionType.PRICE_TOUCH_SMA44) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Create Alert for ${stock.symbol}",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "LTP: ₹%.2f | 44 SMA: ₹%.2f".format(stock.ltp, stock.sma44),
            color = SmaGolden,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Trigger Condition",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        AlertConditionType.entries.forEach { condition ->
            val isSelected = condition == selectedCondition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) TerminalSurfaceElevated else TerminalSurface)
                    .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(8.dp))
                    .clickable { selectedCondition = condition }
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = condition.title,
                        color = if (isSelected) SmaGolden else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = condition.shortDesc,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onConfirm(selectedCondition, stock.sma44) },
            colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_create_alert_button")
        ) {
            Text("Activate Real-Time Alert", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
