package com.example.data.model

data class BacktestConfig(
    val symbol: String,
    val strategy: BacktestStrategy = BacktestStrategy.SMA44_BOUNCE,
    val direction: TradeDirection = TradeDirection.BOTH,
    val timeframe: Timeframe = Timeframe.DAILY,
    val periodMonths: Int = 12,
    val initialCapital: Double = 100000.0,
    val riskPerTradePercent: Double = 2.0,
    val targetRatio: Double = 2.0, // 1:2 Risk to Reward
    val useTrailingSma: Boolean = false
)

enum class BacktestStrategy(val displayName: String, val shortDesc: String) {
    SMA44_BOUNCE(
        "44 SMA Support/Resistance Bounce",
        "Buys when price touches rising 44 SMA & rebounds; Shorts when price tests falling 44 SMA & rejects"
    ),
    SMA44_BREAKOUT(
        "44 SMA Breakout Momentum",
        "Enters on decisive close crossing the 44 SMA in slope direction with volume confirmation"
    ),
    SMA44_SLOPE_CROSS(
        "44 SMA Slope Flip System",
        "Enters when 44 SMA curvature flips from falling to rising (Long) or rising to falling (Short)"
    )
}

enum class TradeDirection(val label: String) {
    LONG_ONLY("Long Only"),
    SHORT_ONLY("Short Only"),
    BOTH("Long & Short")
}

data class TradeRecord(
    val id: String,
    val symbol: String,
    val isLong: Boolean,
    val entryDate: String,
    val entryPrice: Double,
    val exitDate: String,
    val exitPrice: Double,
    val shares: Int,
    val stopLoss: Double,
    val targetPrice: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val exitReason: String,
    val durationDays: Int
)

data class BacktestResult(
    val config: BacktestConfig,
    val totalReturnPercent: Double,
    val netProfit: Double,
    val finalCapital: Double,
    val winRate: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double,
    val avgWinAmount: Double,
    val avgLossAmount: Double,
    val trades: List<TradeRecord>,
    val equityCurve: List<Pair<String, Double>> // Date string to Capital
)
