package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.BacktestResult
import com.example.data.model.BacktestStrategy
import com.example.data.model.Timeframe
import com.example.data.model.TradeDirection
import com.example.data.model.TradeRecord
import com.example.ui.components.EquityCurveChart
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
import com.example.ui.viewmodel.BacktestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktesterScreen(
    viewModel: BacktestViewModel,
    preselectedSymbol: String? = null,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val savedBacktests by viewModel.savedBacktests.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveStatusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Results & Trades, 1 = Saved History
    var showConfigSheet by remember { mutableStateOf(false) }

    LaunchedEffect(preselectedSymbol) {
        if (!preselectedSymbol.isNullOrEmpty() && preselectedSymbol != config.symbol) {
            viewModel.updateSymbol(preselectedSymbol)
            viewModel.runBacktest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Backtester Header & Parameter Summary Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .border(1.dp, TerminalBorder)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "44 SMA BACKTEST ENGINE",
                            color = SmaGolden,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${config.symbol} • ${config.strategy.displayName}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showConfigSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalSurfaceElevated)
                                .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                                .testTag("backtest_config_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Config",
                                tint = SmaGolden,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.runBacktest() },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("run_backtest_button")
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Parameter Quick Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ParamChip(label = "TF: ${config.timeframe.label}")
                    ParamChip(label = "Side: ${config.direction.label}")
                    ParamChip(label = "Target: 1:${String.format(Locale.US, "%.1f", config.targetRatio)}")
                    ParamChip(label = "Risk: ${String.format(Locale.US, "%.0f", config.riskPerTradePercent)}%")
                    ParamChip(label = "Cap: ₹${String.format(Locale.US, "%,.0f", config.initialCapital)}")
                    if (config.useTrailingSma) {
                        ParamChip(label = "Trailing 44 SMA", color = SmaGolden)
                    }
                }
            }
        }

        // Sub Tabs: 0: Backtest Performance, 1: Saved History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TerminalSurface,
            contentColor = SmaGolden,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SmaGolden
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Strategy Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Saved Tests (${savedBacktests.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedTab == 0) {
            val res = result
            if (res == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmaGolden)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Primary Performance Card
                    item {
                        PerformanceSummaryCard(
                            result = res,
                            onSave = { viewModel.saveCurrentResult() },
                            saveMessage = saveMessage
                        )
                    }

                    // 2. Interactive Equity Growth Chart
                    item {
                        EquityCurveChart(
                            equityCurve = res.equityCurve,
                            initialCapital = res.config.initialCapital,
                            finalCapital = res.finalCapital
                        )
                    }

                    // 3. Trade Metrics Grid
                    item {
                        MetricsGrid(result = res)
                    }

                    // 4. Trade Log Journal Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRADE EXECUTION JOURNAL",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${res.trades.size} Trades Executed",
                                color = SmaGolden,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 5. List of individual trade items
                    if (res.trades.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No trades triggered for this setup", color = TextMuted)
                            }
                        }
                    } else {
                        items(res.trades, key = { it.id }) { trade ->
                            TradeJournalCard(trade = trade)
                        }
                    }
                }
            }
        } else {
            // Saved Backtests Tab
            if (savedBacktests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Saved Backtests Yet", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Run a backtest on any Indian stock and tap 'Save to Library' to compare historical performances here.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedBacktests, key = { it.id }) { item ->
                        val isProfitable = item.netProfit >= 0
                        val returnColor = if (isProfitable) BullGreen else BearRed
                        val sign = if (isProfitable) "+" else ""

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalSurface)
                                .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = item.symbol, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "${item.strategyName} • ${item.timeframe}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%s%.1f%% Return", sign, item.totalReturnPercent),
                                            color = returnColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s₹%,.0f Net", sign, item.netProfit),
                                            color = returnColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedBacktest(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TerminalSurfaceElevated)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Win Rate: ${String.format(Locale.US, "%.1f", item.winRate)}%", color = TextSecondary, fontSize = 10.sp)
                                    Text("Trades: ${item.totalTrades}", color = TextSecondary, fontSize = 10.sp)
                                    Text("Profit Factor: ${String.format(Locale.US, "%.2f", item.profitFactor)}", color = TextSecondary, fontSize = 10.sp)
                                    Text("Max DD: ${String.format(Locale.US, "%.1f", item.maxDrawdownPercent)}%", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Strategy Configuration
    if (showConfigSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false },
            sheetState = sheetState,
            containerColor = TerminalSurface
        ) {
            BacktestConfigSheet(
                config = config,
                onUpdateSymbol = { viewModel.updateSymbol(it) },
                onUpdateStrategy = { viewModel.updateStrategy(it) },
                onUpdateDirection = { viewModel.updateDirection(it) },
                onUpdateTimeframe = { viewModel.updateTimeframe(it) },
                onUpdateTargetRatio = { viewModel.updateTargetRatio(it) },
                onUpdateRisk = { viewModel.updateRiskPercent(it) },
                onUpdateTrailing = { viewModel.updateTrailingSma(it) },
                onDone = {
                    showConfigSheet = false
                    viewModel.runBacktest()
                }
            )
        }
    }
}

@Composable
private fun PerformanceSummaryCard(
    result: BacktestResult,
    onSave: () -> Unit,
    saveMessage: String?
) {
    val isPositive = result.netProfit >= 0
    val pnlColor = if (isPositive) BullGreen else BearRed
    val sign = if (isPositive) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "TOTAL STRATEGY RETURN",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f%%", sign, result.totalReturnPercent),
                        color = pnlColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%s₹%,.2f Net P&L", sign, result.netProfit),
                        color = pnlColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(
                        onClick = onSave,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SmaGolden),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SmaGolden)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("save_backtest_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = SmaGolden, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Library", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (saveMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = saveMessage, color = BullGreen, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Key Stats: Capital, Win Rate, Total Trades, Profit Factor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceElevated)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Final Capital", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = String.format(Locale.US, "₹%,.0f", result.finalCapital),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Win Rate", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = String.format(Locale.US, "%.1f%%", result.winRate),
                        color = if (result.winRate >= 50.0) BullGreen else BearRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trades (W/L)", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "${result.totalTrades} (${result.winningTrades}/${result.losingTrades})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Profit Factor", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f", result.profitFactor),
                        color = SmaGolden,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(result: BacktestResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text("RISK & DRAWDOWN METRICS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell("Max Drawdown", String.format(Locale.US, "%.2f%%", result.maxDrawdownPercent), BearRed, Modifier.weight(1f))
                MetricCell("Sharpe Ratio", String.format(Locale.US, "%.2f", result.sharpeRatio), SmaGolden, Modifier.weight(1f))
                MetricCell("Avg Win", String.format(Locale.US, "₹%,.0f", result.avgWinAmount), BullGreen, Modifier.weight(1f))
                MetricCell("Avg Loss", String.format(Locale.US, "₹%,.0f", result.avgLossAmount), BearRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TradeJournalCard(trade: TradeRecord) {
    val isWin = trade.pnl > 0
    val color = if (isWin) BullGreen else BearRed
    val sign = if (isWin) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (trade.isLong) BullGreenBg else BearRedBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (trade.isLong) "LONG" else "SHORT",
                            color = if (trade.isLong) BullGreen else BearRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${trade.entryDate} → ${trade.exitDate}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = String.format(Locale.US, "%s₹%,.1f (%s%.2f%%)", sign, trade.pnl, sign, trade.pnlPercent),
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entry: ₹%.2f | Exit: ₹%.2f (%d shares)".format(trade.entryPrice, trade.exitPrice, trade.shares),
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = trade.exitReason,
                    color = if (trade.exitReason.startsWith("Target")) BullGreen else if (trade.exitReason.startsWith("Stop")) BearRed else SmaGolden,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ParamChip(
    label: String,
    color: Color = TextSecondary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalSurfaceElevated)
            .border(0.8.dp, TerminalBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BacktestConfigSheet(
    config: com.example.data.model.BacktestConfig,
    onUpdateSymbol: (String) -> Unit,
    onUpdateStrategy: (BacktestStrategy) -> Unit,
    onUpdateDirection: (TradeDirection) -> Unit,
    onUpdateTimeframe: (Timeframe) -> Unit,
    onUpdateTargetRatio: (Double) -> Unit,
    onUpdateRisk: (Double) -> Unit,
    onUpdateTrailing: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Backtest Strategy Parameters", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(14.dp))

        // Stock Selector
        Text("Indian Market Stock", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val sampleStocks = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "TATAMOTORS", "SBIN", "SUNPHARMA", "BAJFINANCE", "LT", "TITAN", "NTPC", "BEL")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sampleStocks.forEach { sym ->
                val isSelected = sym == config.symbol
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateSymbol(sym) },
                    label = { Text(sym) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmaGolden,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Strategy Selector
        Text("44 SMA Strategy Logic", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        BacktestStrategy.entries.forEach { st ->
            val isSelected = st == config.strategy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) TerminalSurfaceElevated else TerminalSurface)
                    .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(8.dp))
                    .clickable { onUpdateStrategy(st) }
                    .padding(10.dp)
            ) {
                Column {
                    Text(st.displayName, color = if (isSelected) SmaGolden else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(st.shortDesc, color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trade Side (Long, Short, Both)
        Text("Trade Direction", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TradeDirection.entries.forEach { dir ->
                FilterChip(
                    selected = dir == config.direction,
                    onClick = { onUpdateDirection(dir) },
                    label = { Text(dir.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmaGolden,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Risk to Reward Ratio
        Text("Target Risk-to-Reward Ratio", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val rrRatios = listOf(1.5, 2.0, 3.0)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rrRatios.forEach { rr ->
                FilterChip(
                    selected = config.targetRatio == rr,
                    onClick = { onUpdateTargetRatio(rr) },
                    label = { Text("1:${String.format(Locale.US, "%.1f", rr)}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmaGolden,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trailing 44 SMA Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Trail Stop Loss Along 44 SMA", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Exits long trade when price closes below 44 SMA", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = config.useTrailingSma,
                onCheckedChange = { onUpdateTrailing(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = SmaGolden
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Apply & Run Backtest", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
