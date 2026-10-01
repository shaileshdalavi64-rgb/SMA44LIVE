package com.example.data.model

data class Stock(
    val symbol: String,
    val name: String,
    val sector: String,
    val exchange: String = "NSE",
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val prevClose: Double,
    val volume: Long,
    val sma44: Double,
    val smaSlope: Double, // +0.10 means rising rapidly, -0.10 means falling rapidly
    val distanceFromSmaPercent: Double, // positive = above SMA, negative = below SMA
    val signal: SmaSignal,
    val sparkline: List<Double>,
    val candles: List<Candle> = emptyList(),
    val isWatchlist: Boolean = false,
    val bidPrice: Double = ltp - 0.25,
    val askPrice: Double = ltp + 0.25,
    val dayRangePercent: Float = 0.5f,
    val peRatio: Double = 24.5,
    val week52High: Double = ltp * 1.22,
    val week52Low: Double = ltp * 0.78
)

enum class SmaSignal(val label: String, val isBullish: Boolean, val description: String) {
    RISING_BOUNCE(
        "44 SMA Bounce",
        true,
        "Price taking strong support on Rising 44 SMA with bullish candle"
    ),
    RISING_BREAKOUT(
        "44 SMA Breakout",
        true,
        "Strong bullish breakout above 44 SMA with surging volume"
    ),
    RISING_TREND(
        "Rising 44 SMA",
        true,
        "Consistently trending upwards above sloping 44 SMA"
    ),
    FALLING_REJECT(
        "44 SMA Rejection",
        false,
        "Price facing stiff resistance at Falling 44 SMA with bearish candle"
    ),
    FALLING_BREAKDOWN(
        "44 SMA Breakdown",
        false,
        "Bearish breakdown below 44 SMA with selling momentum"
    ),
    FALLING_TREND(
        "Falling 44 SMA",
        false,
        "Consistently trending downwards below falling 44 SMA"
    ),
    NEAR_SMA(
        "Near 44 SMA",
        true,
        "Trading within 0.75% of 44 SMA - High probability setup developing"
    ),
    NEUTRAL(
        "Consolidating",
        true,
        "Price oscillating sideways around flat 44 SMA"
    )
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val sma44: Double = 0.0
) {
    val isBullish: Boolean get() = close >= open
}

enum class Timeframe(val label: String, val periodStr: String) {
    FIVE_MIN("5M", "5 Min"),
    FIFTEEN_MIN("15M", "15 Min"),
    ONE_HOUR("1H", "1 Hour"),
    DAILY("1D", "Daily")
}

enum class ScanType(val title: String) {
    ALL("All Stocks"),
    RISING_STOCKS("Rising 44 SMA (Bullish)"),
    FALLING_STOCKS("Falling 44 SMA (Bearish)"),
    BOUNCE_SETUPS("44 SMA Bounce / Rejection"),
    BREAKOUTS("Breakout / Breakdown"),
    NEAR_SMA("Near 44 SMA (<1%)")
}
