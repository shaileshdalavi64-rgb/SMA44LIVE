package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.model.AlertRule
import com.example.data.repository.AlertRepository
import com.example.data.repository.BacktestRepository
import com.example.data.repository.StockRepository
import com.example.service.NotificationHelper
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BacktesterScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StockDetailScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.BullGreen
import com.example.ui.theme.Sma44ScannerTheme
import com.example.ui.theme.SmaGolden
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AlertViewModel
import com.example.ui.viewmodel.BacktestViewModel
import com.example.ui.viewmodel.MarketViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        val watchlistDao = database.watchlistDao()
        val alertDao = database.alertDao()
        val backtestDao = database.backtestDao()

        val stockRepository = StockRepository(watchlistDao, lifecycleScope)
        val alertRepository = AlertRepository(alertDao, notificationHelper, lifecycleScope)
        val backtestRepository = BacktestRepository(backtestDao, stockRepository)

        val marketViewModel: MarketViewModel by viewModels {
            MarketViewModel.Factory(stockRepository, alertRepository)
        }
        val backtestViewModel: BacktestViewModel by viewModels {
            BacktestViewModel.Factory(backtestRepository)
        }
        val alertViewModel: AlertViewModel by viewModels {
            AlertViewModel.Factory(alertRepository)
        }

        setContent {
            Sma44ScannerTheme {
                MainAppScreen(
                    marketViewModel = marketViewModel,
                    backtestViewModel = backtestViewModel,
                    alertViewModel = alertViewModel
                )
            }
        }
    }
}

sealed class NavTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val tag: String) {
    data object Scanner : NavTab("Scanner", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "tab_scanner")
    data object Watchlist : NavTab("Watchlist", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_watchlist")
    data object Backtest : NavTab("Backtest", Icons.Filled.HistoryEdu, Icons.Outlined.HistoryEdu, "tab_backtest")
    data object Alerts : NavTab("Alerts", Icons.Filled.NotificationsActive, Icons.Outlined.Notifications, "tab_alerts")
}

@Composable
fun MainAppScreen(
    marketViewModel: MarketViewModel,
    backtestViewModel: BacktestViewModel,
    alertViewModel: AlertViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeDetailSymbol by remember { mutableStateOf<String?>(null) }
    var backtestPreselectedSymbol by remember { mutableStateOf<String?>(null) }

    val bannerAlert by alertViewModel.bannerAlert.collectAsStateWithLifecycle()

    val tabs = listOf(NavTab.Scanner, NavTab.Watchlist, NavTab.Backtest, NavTab.Alerts)

    // Handle back button when on StockDetailScreen
    BackHandler(enabled = activeDetailSymbol != null) {
        activeDetailSymbol = null
    }

    Box(modifier = Modifier.fillMaxSize().background(TerminalBackground)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = TerminalBackground,
            bottomBar = {
                if (activeDetailSymbol == null) {
                    NavigationBar(
                        containerColor = TerminalSurface,
                        contentColor = SmaGolden
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = index
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = SmaGolden,
                                    indicatorColor = SmaGolden,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeDetailSymbol != null) {
                    StockDetailScreen(
                        symbol = activeDetailSymbol!!,
                        marketViewModel = marketViewModel,
                        alertViewModel = alertViewModel,
                        onNavigateBack = { activeDetailSymbol = null },
                        onNavigateToBacktest = { sym ->
                            activeDetailSymbol = null
                            backtestPreselectedSymbol = sym
                            selectedTab = 2 // Switch to Backtester tab
                        }
                    )
                } else {
                    when (selectedTab) {
                        0 -> ScannerScreen(
                            viewModel = marketViewModel,
                            onStockSelected = { symbol ->
                                marketViewModel.selectStock(symbol)
                                activeDetailSymbol = symbol
                            }
                        )
                        1 -> WatchlistScreen(
                            viewModel = marketViewModel,
                            onStockSelected = { symbol ->
                                marketViewModel.selectStock(symbol)
                                activeDetailSymbol = symbol
                            }
                        )
                        2 -> BacktesterScreen(
                            viewModel = backtestViewModel,
                            preselectedSymbol = backtestPreselectedSymbol
                        )
                        3 -> AlertsScreen(
                            alertViewModel = alertViewModel,
                            marketViewModel = marketViewModel
                        )
                    }
                }
            }
        }

        // Real-Time Alert Floating Banner
        AnimatedVisibility(
            visible = bannerAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            bannerAlert?.let { alert ->
                AlertBanner(
                    alert = alert,
                    onViewStock = {
                        marketViewModel.selectStock(alert.symbol)
                        activeDetailSymbol = alert.symbol
                        alertViewModel.dismissBanner()
                    },
                    onDismiss = {
                        alertViewModel.dismissBanner()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlertBanner(
    alert: AlertRule,
    onViewStock: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.5.dp, SmaGolden, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("in_app_alert_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = SmaGolden,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚡ 44 SMA ALERT: ${alert.symbol}",
                        color = SmaGolden,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${alert.condition.title} • LTP: ₹%.2f".format(alert.currentLtp),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onViewStock,
                colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("View", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
