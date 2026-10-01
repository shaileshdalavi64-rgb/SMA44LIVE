package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmaSignal
import com.example.ui.theme.BearRed
import com.example.ui.theme.BearRedBg
import com.example.ui.theme.BullGreen
import com.example.ui.theme.BullGreenBg
import com.example.ui.theme.SmaGolden

@Composable
fun SignalBadge(
    signal: SmaSignal,
    modifier: Modifier = Modifier
) {
    val isBullish = signal.isBullish
    val bgColor = if (isBullish) BullGreenBg else BearRedBg
    val textColor = if (isBullish) BullGreen else BearRed
    val borderColor = if (isBullish) BullGreen.copy(alpha = 0.4f) else BearRed.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isBullish) Icons.Default.NorthEast else Icons.Default.SouthEast,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = signal.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SmaSlopeBadge(
    slope: Double,
    modifier: Modifier = Modifier
) {
    val isRising = slope > 0.03
    val isFalling = slope < -0.03
    val color = when {
        isRising -> BullGreen
        isFalling -> BearRed
        else -> SmaGolden
    }
    val text = when {
        isRising -> "44 SMA Rising ↗"
        isFalling -> "44 SMA Falling ↘"
        else -> "44 SMA Flat →"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
