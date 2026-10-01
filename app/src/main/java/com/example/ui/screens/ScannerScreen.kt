package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.ScanType
import com.example.data.model.Stock
import com.example.data.model.Timeframe
import com.example.ui.components.MarketHeader
import com.example.ui.components.SignalBadge
import com.example.ui.components.SmaSlopeBadge
import com.example.ui.components.SparklineChart
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
import com.example.ui.viewmodel.MarketViewModel
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: MarketViewModel,
    onStockSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.filteredStocks.collectAsStateWithLifecycle()
    val scanType by viewModel.scanType.collectAsStateWithLifecycle()
    val timeframe by viewModel.timeframe.collectAsStateWithLifecycle()
    val selectedSector by viewModel.selectedSector.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val maxDistance by viewModel.maxDistanceFilter.collectAsStateWithLifecycle()

    val niftyLtp by viewModel.nifty50Ltp.collectAsStateWithLifecycle()
    val niftyChange by viewModel.nifty50Change.collectAsStateWithLifecycle()
    val bankNiftyLtp by viewModel.bankNiftyLtp.collectAsStateWithLifecycle()
    val bankNiftyChange by viewModel.bankNiftyChange.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isLiveStreaming.collectAsStateWithLifecycle()
    val isNseApiLive by viewModel.isNseApiLive.collectAsStateWithLifecycle()
    val isRefreshingNse by viewModel.isRefreshingNse.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastNseSyncTime.collectAsStateWithLifecycle()
    val streamSpeed by viewModel.streamSpeedMultiplier.collectAsStateWithLifecycle()

    val risingCount by viewModel.risingCount.collectAsStateWithLifecycle()
    val fallingCount by viewModel.fallingCount.collectAsStateWithLifecycle()
    val bounceCount by viewModel.bounceCount.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Market Ticker Header with NSE Live status & controls
        MarketHeader(
            niftyLtp = niftyLtp,
            niftyChange = niftyChange,
            bankNiftyLtp = bankNiftyLtp,
            bankNiftyChange = bankNiftyChange,
            isStreaming = isStreaming,
            streamSpeed = streamSpeed,
            isNseApiLive = isNseApiLive,
            isRefreshing = isRefreshingNse,
            lastSyncTime = lastSyncTime,
            onToggleStreaming = { viewModel.toggleLiveStreaming() },
            onRefreshNse = { viewModel.refreshRealNsePrices() },
            onCycleSpeed = {
                val next = when (streamSpeed) {
                    1 -> 2
                    2 -> 5
                    else -> 1
                }
                viewModel.setStreamSpeed(next)
            }
        )

        // KPI Summary Counters Bar (Rising 44 SMA vs Falling 44 SMA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiChip(
                label = "Rising 44 SMA",
                count = risingCount,
                color = BullGreen,
                bgColor = BullGreenBg,
                isSelected = scanType == ScanType.RISING_STOCKS,
                onClick = {
                    viewModel.setScanType(
                        if (scanType == ScanType.RISING_STOCKS) ScanType.ALL else ScanType.RISING_STOCKS
                    )
                },
                modifier = Modifier.weight(1f)
            )

            KpiChip(
                label = "Falling 44 SMA",
                count = fallingCount,
                color = BearRed,
                bgColor = BearRedBg,
                isSelected = scanType == ScanType.FALLING_STOCKS,
                onClick = {
                    viewModel.setScanType(
                        if (scanType == ScanType.FALLING_STOCKS) ScanType.ALL else ScanType.FALLING_STOCKS
                    )
                },
                modifier = Modifier.weight(1f)
            )

            KpiChip(
                label = "44 SMA Setups",
                count = bounceCount,
                color = SmaGolden,
                bgColor = SmaGolden.copy(alpha = 0.15f),
                isSelected = scanType == ScanType.BOUNCE_SETUPS,
                onClick = {
                    viewModel.setScanType(
                        if (scanType == ScanType.BOUNCE_SETUPS) ScanType.ALL else ScanType.BOUNCE_SETUPS
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Search & Filter Trigger Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stock_search_input"),
                placeholder = { Text("Search NSE stocks (e.g. RELIANCE, TCS)", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TerminalSurfaceElevated,
                    unfocusedContainerColor = TerminalSurface,
                    focusedBorderColor = SmaGolden,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (maxDistance < 5f || selectedSector != "All") SmaGolden.copy(alpha = 0.2f) else TerminalSurface)
                    .border(1.dp, if (maxDistance < 5f || selectedSector != "All") SmaGolden else TerminalBorder, RoundedCornerShape(8.dp))
                    .clickable { showFilterSheet = true }
                    .testTag("filter_dialog_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (maxDistance < 5f || selectedSector != "All") SmaGolden else TextSecondary
                )
            }
        }

        // Horizontal Quick Strategy Pills & Timeframe Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe toggles
            Timeframe.entries.forEach { tf ->
                val isSelected = tf == timeframe
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) SmaGolden else TerminalSurface)
                        .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.setTimeframe(tf) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("timeframe_${tf.label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Scan Type Chips
            ScanType.entries.forEach { st ->
                val isSelected = st == scanType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TerminalSurfaceElevated else TerminalSurface)
                        .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.setScanType(st) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("scan_type_${st.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = st.title,
                        color = if (isSelected) SmaGolden else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Active filter indicator if distance or sector filtered
        if (maxDistance < 5f || selectedSector != "All") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters: Sector: $selectedSector • Max Distance: ≤ ${String.format(Locale.US, "%.1f", maxDistance)}%",
                    color = SmaGolden,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Reset",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        viewModel.setSector("All")
                        viewModel.setMaxDistanceFilter(5.0f)
                    }
                )
            }
        }

        // Stock Results Count & Column Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stocks.size} Stocks Found",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "LTP / 44 SMA",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // List of Scanned Indian Stocks
        if (stocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No stocks matching scanner criteria",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try increasing the SMA distance tolerance or clearing sector filters",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("scanner_stock_list"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stocks, key = { it.symbol }) { stock ->
                    StockScannerCard(
                        stock = stock,
                        onClick = { onStockSelected(stock.symbol) },
                        onWatchlistToggle = { viewModel.toggleWatchlist(stock.symbol) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Filters
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = TerminalSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Scanner Advanced Filters",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Max Distance from 44 SMA Slider
                Text(
                    text = "Max Distance from 44 SMA: ${String.format(Locale.US, "%.1f", maxDistance)}%",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = maxDistance,
                    onValueChange = { viewModel.setMaxDistanceFilter(it) },
                    valueRange = 0.3f..5.0f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = SmaGolden,
                        activeTrackColor = SmaGolden,
                        inactiveTrackColor = TerminalBorder
                    ),
                    modifier = Modifier.testTag("distance_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sector Selector
                Text(
                    text = "Sector / Industry",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                val sectors = listOf("All", "Banking", "IT", "Auto", "Pharma", "Energy", "FMCG", "Metals")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sectors.forEach { sec ->
                        val isSelected = sec == selectedSector
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSector(sec) },
                            label = { Text(sec) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmaGolden,
                                selectedLabelColor = Color.Black,
                                containerColor = TerminalSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.setSector("All")
                            viewModel.setMaxDistanceFilter(5.0f)
                            showFilterSheet = false
                        }
                    ) {
                        Text("Reset All", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showFilterSheet = false }
                    ) {
                        Text("Done", color = SmaGolden, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockScannerCard(
    stock: Stock,
    onClick: () -> Unit,
    onWatchlistToggle: () -> Unit
) {
    val isPositive = stock.change >= 0
    val changeColor = if (isPositive) BullGreen else BearRed
    val sign = if (isPositive) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("stock_card_${stock.symbol}")
    ) {
        Column {
            // Header Row: Symbol, Name, Sector & Watchlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TerminalSurfaceElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stock.sector,
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = stock.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Price and Day % Change
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "₹%,.2f", stock.ltp),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%s%.2f (%s%.2f%%)", sign, stock.change, sign, stock.changePercent),
                        color = changeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Watchlist Star Icon
                IconButton(
                    onClick = onWatchlistToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (stock.isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (stock.isWatchlist) SmaGolden else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 44 SMA Metrics & Mini Sparkline Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 44 SMA Info
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "44 SMA: ",
                            color = SmaGolden,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", stock.sma44),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val dist = stock.distanceFromSmaPercent
                    val distSign = if (dist >= 0) "+" else ""
                    val distColor = if (abs(dist) <= 0.6) SmaGolden else if (dist > 0) BullGreen else BearRed

                    Text(
                        text = "Dist: %s%.2f%% %s".format(
                            distSign,
                            dist,
                            if (dist >= 0) "above SMA" else "below SMA"
                        ),
                        color = distColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Middle: Sparkline Preview with 44 SMA reference line
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(34.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    SparklineChart(
                        prices = stock.sparkline,
                        sma44 = stock.sma44,
                        isBullish = isPositive
                    )
                }

                // Right: Signal Badge
                Box(
                    modifier = Modifier.weight(1.1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    SignalBadge(signal = stock.signal)
                }
            }
        }
    }
}

@Composable
private fun KpiChip(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.25f) else bgColor)
            .border(1.dp, if (isSelected) color else color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = color.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
