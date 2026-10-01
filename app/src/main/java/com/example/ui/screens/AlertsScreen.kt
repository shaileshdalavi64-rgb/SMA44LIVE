package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule
import com.example.data.model.Stock
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    alertViewModel: AlertViewModel,
    marketViewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by alertViewModel.alerts.collectAsStateWithLifecycle()
    val allStocks by marketViewModel.allStocks.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active Alerts, 1 = Triggered History
    var showCreateSheet by remember { mutableStateOf(false) }

    val activeAlerts = alerts.filter { !it.isTriggered }
    val triggeredAlerts = alerts.filter { it.isTriggered }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Alerts Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .border(1.dp, TerminalBorder)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REAL-TIME 44 SMA ALERTS",
                        color = SmaGolden,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${activeAlerts.count { it.isActive }} Active • ${triggeredAlerts.size} Triggered",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    // Quick add presets button
                    OutlinedButton(
                        onClick = {
                            // Seed preset alerts for top Indian stocks
                            val presets = listOf(
                                Triple("RELIANCE", "Reliance Industries Ltd", AlertConditionType.PRICE_TOUCH_SMA44),
                                Triple("TCS", "Tata Consultancy Services", AlertConditionType.SMA44_BOUNCE_CONFIRMED),
                                Triple("HDFCBANK", "HDFC Bank Ltd", AlertConditionType.PRICE_CROSS_ABOVE_SMA44),
                                Triple("TATAMOTORS", "Tata Motors Ltd", AlertConditionType.SMA44_BOUNCE_CONFIRMED)
                            )
                            presets.forEach { (sym, name, cond) ->
                                val stock = allStocks.find { it.symbol == sym }
                                alertViewModel.createAlert(
                                    symbol = sym,
                                    stockName = name,
                                    condition = cond,
                                    targetValue = stock?.sma44 ?: 2000.0,
                                    currentLtp = stock?.ltp ?: 2000.0,
                                    smaValue = stock?.sma44 ?: 2000.0
                                )
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SmaGolden),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SmaGolden)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SmaGolden, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showCreateSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("create_alert_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Alert", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tabs
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
                text = { Text("Active Rules (${activeAlerts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Triggered History (${triggeredAlerts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        // Active Alerts Tab
        if (selectedTab == 0) {
            if (activeAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Active Alerts", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ Presets' or 'New Alert' to get instant notifications and haptic alerts when price interacts with the 44 SMA.",
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
                    items(activeAlerts, key = { it.id }) { alert ->
                        ActiveAlertCard(
                            alert = alert,
                            onToggle = { alertViewModel.toggleAlert(alert) },
                            onDelete = { alertViewModel.deleteAlert(alert.id) }
                        )
                    }
                }
            }
        } else {
            // Triggered Alerts History Tab
            if (triggeredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Triggered Alerts Yet", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "As the live Indian market ticks stream in, triggered 44 SMA alerts will be logged here with exact trigger timestamps and execution prices.",
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
                    items(triggeredAlerts, key = { it.id }) { alert ->
                        TriggeredAlertCard(
                            alert = alert,
                            onDelete = { alertViewModel.deleteAlert(alert.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for New Alert
    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = TerminalSurface
        ) {
            NewAlertSheet(
                stocks = allStocks,
                onDismiss = { showCreateSheet = false },
                onConfirm = { symbol, name, condition, targetVal, currentLtp, sma ->
                    alertViewModel.createAlert(symbol, name, condition, targetVal, currentLtp, sma)
                    showCreateSheet = false
                }
            )
        }
    }
}

@Composable
private fun ActiveAlertCard(
    alert: AlertRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, if (alert.isActive) TerminalBorder else TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = alert.symbol, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (alert.isActive) BullGreenBg else TerminalSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (alert.isActive) "ARMED" else "PAUSED",
                            color = if (alert.isActive) BullGreen else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(text = alert.condition.title, color = SmaGolden, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ref 44 SMA: ₹%.2f | Current: ₹%.2f".format(alert.smaValue, alert.currentLtp),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = alert.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = SmaGolden
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TriggeredAlertCard(
    alert: AlertRule,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.ENGLISH) }
    val dateStr = alert.triggeredAt?.let { dateFormat.format(Date(it)) } ?: "Just Now"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, BullGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = BullGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = alert.symbol, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "• $dateStr", color = TextMuted, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(text = alert.condition.title, color = SmaGolden, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Triggered at LTP: ₹%.2f | 44 SMA: ₹%.2f".format(alert.currentLtp, alert.smaValue),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NewAlertSheet(
    stocks: List<Stock>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, AlertConditionType, Double, Double, Double) -> Unit
) {
    var selectedStock by remember { mutableStateOf(stocks.firstOrNull()) }
    var selectedCondition by remember { mutableStateOf(AlertConditionType.PRICE_TOUCH_SMA44) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text("Create Custom 44 SMA Alert", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        // Stock Selector
        Text("Select Indian Stock", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stocks.take(15).forEach { stock ->
                val isSelected = stock.symbol == selectedStock?.symbol
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStock = stock },
                    label = { Text(stock.symbol) },
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

        // Trigger condition
        Text("Alert Condition", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        AlertConditionType.entries.take(5).forEach { cond ->
            val isSelected = cond == selectedCondition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) TerminalSurfaceElevated else TerminalSurface)
                    .border(1.dp, if (isSelected) SmaGolden else TerminalBorder, RoundedCornerShape(6.dp))
                    .clickable { selectedCondition = cond }
                    .padding(8.dp)
            ) {
                Column {
                    Text(cond.title, color = if (isSelected) SmaGolden else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(cond.shortDesc, color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentStock = selectedStock
        Button(
            onClick = {
                if (currentStock != null) {
                    onConfirm(
                        currentStock.symbol,
                        currentStock.name,
                        selectedCondition,
                        currentStock.sma44,
                        currentStock.ltp,
                        currentStock.sma44
                    )
                }
            },
            enabled = currentStock != null,
            colors = ButtonDefaults.buttonColors(containerColor = SmaGolden),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_new_alert_btn")
        ) {
            Text("Create Alert", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
