package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.BullGreenBg
import com.example.ui.theme.SmaGolden
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun MarketHeader(
    niftyLtp: Double,
    niftyChange: Double,
    bankNiftyLtp: Double,
    bankNiftyChange: Double,
    isStreaming: Boolean,
    streamSpeed: Int,
    isNseApiLive: Boolean = true,
    isRefreshing: Boolean = false,
    lastSyncTime: String = "Live",
    onToggleStreaming: () -> Unit,
    onCycleSpeed: () -> Unit,
    onRefreshNse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder)
            .padding(12.dp)
            .testTag("market_header")
    ) {
        // Top status row: NSE Market Live & Stream Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(if (isStreaming) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isStreaming) BullGreen else BearRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isNseApiLive) "NSE LIVE API" else "NSE FEED",
                    color = if (isStreaming) BullGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BullGreenBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "IST 09:15-15:30",
                        color = BullGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Controls: Manual Refresh, Speed Pill, Play/Pause
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Refresh NSE quotes button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, CircleShape)
                        .clickable(onClick = onRefreshNse)
                        .testTag("refresh_nse_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Real NSE Quotes",
                        tint = SmaGolden,
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(if (isRefreshing) spinAngle else 0f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Speed pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, SmaGolden.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("stream_speed_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Stream Speed",
                            tint = SmaGolden,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${streamSpeed}x",
                            color = SmaGolden,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isStreaming) TerminalSurfaceElevated else BullGreen.copy(alpha = 0.2f))
                        .border(1.dp, if (isStreaming) TerminalBorder else BullGreen, CircleShape)
                        .clickable(onClick = onToggleStreaming)
                        .testTag("stream_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Live Streaming",
                        tint = if (isStreaming) TextSecondary else BullGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Index Tickers: Nifty 50 and Bank Nifty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Nifty 50 Card
            IndexMiniCard(
                name = "NIFTY 50",
                ltp = niftyLtp,
                change = niftyChange,
                modifier = Modifier.weight(1f)
            )

            // Bank Nifty Card
            IndexMiniCard(
                name = "BANK NIFTY",
                ltp = bankNiftyLtp,
                change = bankNiftyChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IndexMiniCard(
    name: String,
    ltp: Double,
    change: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val changeColor = if (isPositive) BullGreen else BearRed
    val sign = if (isPositive) "+" else ""
    val changePct = if (ltp - change != 0.0) (change / (ltp - change)) * 100.0 else 0.0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceElevated)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = name,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = String.format(Locale.US, "₹%,.2f", ltp),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%s%.2f (%s%.2f%%)", sign, change, sign, changePct),
                    color = changeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
