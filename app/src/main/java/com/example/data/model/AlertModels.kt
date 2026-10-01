package com.example.data.model

data class AlertRule(
    val id: Long = 0,
    val symbol: String,
    val stockName: String,
    val condition: AlertConditionType,
    val targetValue: Double,
    val currentLtp: Double,
    val smaValue: Double,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AlertConditionType(val title: String, val shortDesc: String) {
    PRICE_TOUCH_SMA44(
        "Price Touches 44 SMA",
        "Triggers when price tests within 0.3% of 44 SMA"
    ),
    PRICE_CROSS_ABOVE_SMA44(
        "Crosses Above 44 SMA",
        "Triggers when candle closes above rising 44 SMA"
    ),
    PRICE_CROSS_BELOW_SMA44(
        "Crosses Below 44 SMA",
        "Triggers when candle closes below falling 44 SMA"
    ),
    SMA44_BOUNCE_CONFIRMED(
        "44 SMA Bullish Bounce",
        "Triggers when high of candle touching 44 SMA is broken"
    ),
    SMA44_REJECT_CONFIRMED(
        "44 SMA Bearish Rejection",
        "Triggers when low of candle touching 44 SMA is broken"
    ),
    PRICE_TARGET_HIT(
        "Target Price Reached",
        "Triggers when LTP hits or crosses target price"
    ),
    STOP_LOSS_HIT(
        "Stop Loss Breached",
        "Triggers when LTP breaches stop loss level"
    )
}
