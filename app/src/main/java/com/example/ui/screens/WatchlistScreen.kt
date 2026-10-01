package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Stock
import com.example.ui.components.MarketHeader
import com.example.ui.components.SignalBadge
import com.example.ui.components.SparklineChart
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
import com.example.ui.viewmodel.MarketViewModel
import java.util.Locale

@Composable
fun WatchlistScreen(
    viewModel: MarketViewModel,
    onStockSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlistStocks by viewModel.watchlistStocks.collectAsStateWithLifecycle()
    val niftyLtp by viewModel.nifty50Ltp.collectAsStateWithLifecycle()
    val niftyChange by viewModel.nifty50Change.collectAsStateWithLifecycle()
    val bankNiftyLtp by viewModel.bankNiftyLtp.collectAsStateWithLifecycle()
    val bankNiftyChange by viewModel.bankNiftyChange.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isLiveStreaming.collectAsStateWithLifecycle()
    val isNseApiLive by viewModel.isNseApiLive.collectAsStateWithLifecycle()
    val isRefreshingNse by viewModel.isRefreshingNse.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastNseSyncTime.collectAsStateWithLifecycle()
    val streamSpeed by viewModel.streamSpeedMultiplier.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY 44 SMA WATCHLIST",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${watchlistStocks.size} Tracked",
                color = SmaGolden,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (watchlistStocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the bookmark icon on any stock card in the Scanner to track it here with live real-time price updates.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlistStocks, key = { it.symbol }) { stock ->
                    WatchlistStockCard(
                        stock = stock,
                        onClick = { onStockSelected(stock.symbol) },
                        onRemove = { viewModel.toggleWatchlist(stock.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistStockCard(
    stock: Stock,
    onClick: () -> Unit,
    onRemove: () -> Unit
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
    ) {
        Column {
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
                        Text(
                            text = stock.sector,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = stock.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

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

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove from watchlist",
                        tint = SmaGolden,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "44 SMA: ₹%.2f".format(stock.sma44),
                        color = SmaGolden,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dist: %s%.2f%%".format(if (stock.distanceFromSmaPercent >= 0) "+" else "", stock.distanceFromSmaPercent),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(30.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    SparklineChart(
                        prices = stock.sparkline,
                        sma44 = stock.sma44,
                        isBullish = isPositive
                    )
                }

                Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
                    SignalBadge(signal = stock.signal)
                }
            }
        }
    }
}
