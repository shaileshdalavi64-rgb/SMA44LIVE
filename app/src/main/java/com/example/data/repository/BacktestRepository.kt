package com.example.data.repository

import com.example.data.local.dao.BacktestDao
import com.example.data.local.entity.BacktestEntity
import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestResult
import com.example.data.model.BacktestStrategy
import com.example.data.model.Candle
import com.example.data.model.TradeDirection
import com.example.data.model.TradeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BacktestRepository(
    private val backtestDao: BacktestDao,
    private val stockRepository: StockRepository
) {
    val savedBacktests: Flow<List<BacktestEntity>> = backtestDao.getAllBacktests()

    suspend fun runBacktest(config: BacktestConfig): BacktestResult = withContext(Dispatchers.Default) {
        val candles = stockRepository.getCandlesForTimeframe(config.symbol, config.timeframe)
        if (candles.size < 48) {
            return@withContext emptyResult(config)
        }

        var capital = config.initialCapital
        var peakCapital = capital
        var maxDrawdown = 0.0
        val trades = mutableListOf<TradeRecord>()
        val equityCurve = mutableListOf<Pair<String, Double>>()

        val dateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
        equityCurve.add(dateFormat.format(Date(candles[44].timestamp)) to capital)

        var activeTrade: ActiveTrade? = null

        // Loop over candles after SMA warmup period
        for (i in 44 until candles.size) {
            val candle = candles[i]
            val dateStr = dateFormat.format(Date(candle.timestamp))

            // 1. Check if active trade reaches Target or Stop Loss
            if (activeTrade != null) {
                val trade = activeTrade
                var isExited = false
                var exitPrice = 0.0
                var exitReason = ""

                if (trade.isLong) {
                    if (config.useTrailingSma && candle.close < candle.sma44) {
                        // Trailing 44 SMA stop
                        exitPrice = candle.close
                        exitReason = "Trailed 44 SMA Exit"
                        isExited = true
                    } else if (candle.low <= trade.stopLoss) {
                        exitPrice = trade.stopLoss
                        exitReason = "Stop Loss Hit"
                        isExited = true
                    } else if (candle.high >= trade.targetPrice) {
                        exitPrice = trade.targetPrice
                        exitReason = "Target Hit (1:${String.format(Locale.US, "%.1f", config.targetRatio)})"
                        isExited = true
                    }
                } else {
                    // Short trade
                    if (config.useTrailingSma && candle.close > candle.sma44) {
                        exitPrice = candle.close
                        exitReason = "Trailed 44 SMA Exit"
                        isExited = true
                    } else if (candle.high >= trade.stopLoss) {
                        exitPrice = trade.stopLoss
                        exitReason = "Stop Loss Hit"
                        isExited = true
                    } else if (candle.low <= trade.targetPrice) {
                        exitPrice = trade.targetPrice
                        exitReason = "Target Hit (1:${String.format(Locale.US, "%.1f", config.targetRatio)})"
                        isExited = true
                    }
                }

                if (isExited) {
                    val grossPnl = if (trade.isLong) {
                        (exitPrice - trade.entryPrice) * trade.shares
                    } else {
                        (trade.entryPrice - exitPrice) * trade.shares
                    }
                    // Deduct ~0.06% exchange turnover & broker slippage
                    val turnoverCost = (trade.entryPrice + exitPrice) * trade.shares * 0.0006
                    val netPnl = grossPnl - turnoverCost
                    val pnlPercent = (netPnl / (trade.entryPrice * trade.shares)) * 100.0

                    capital += netPnl
                    if (capital > peakCapital) peakCapital = capital
                    val currentDrawdown = ((peakCapital - capital) / peakCapital) * 100.0
                    if (currentDrawdown > maxDrawdown) maxDrawdown = currentDrawdown

                    trades.add(
                        TradeRecord(
                            id = UUID.randomUUID().toString().take(8),
                            symbol = config.symbol,
                            isLong = trade.isLong,
                            entryDate = trade.entryDate,
                            entryPrice = trade.entryPrice,
                            exitDate = dateStr,
                            exitPrice = exitPrice,
                            shares = trade.shares,
                            stopLoss = trade.stopLoss,
                            targetPrice = trade.targetPrice,
                            pnl = netPnl,
                            pnlPercent = pnlPercent,
                            exitReason = exitReason,
                            durationDays = max(1, i - trade.entryIndex)
                        )
                    )
                    equityCurve.add(dateStr to capital)
                    activeTrade = null
                }
            }

            // 2. Look for new Entry Setup if no active position
            if (activeTrade == null && i < candles.size - 1) {
                val signal = detectEntrySignal(candles, i, config.strategy)

                if (signal != null) {
                    val canTrade = when (config.direction) {
                        TradeDirection.LONG_ONLY -> signal.isLong
                        TradeDirection.SHORT_ONLY -> !signal.isLong
                        TradeDirection.BOTH -> true
                    }

                    if (canTrade) {
                        val riskAmount = capital * (config.riskPerTradePercent / 100.0)
                        val perShareRisk = abs(signal.entryPrice - signal.stopLoss)

                        if (perShareRisk > 0.05) {
                            val maxSharesByRisk = (riskAmount / perShareRisk).toInt()
                            val maxSharesByCapital = (capital * 0.95 / signal.entryPrice).toInt()
                            val shares = min(maxSharesByRisk, maxSharesByCapital).coerceAtLeast(1)

                            if (shares * signal.entryPrice <= capital * 0.98) {
                                val targetPrice = if (signal.isLong) {
                                    signal.entryPrice + (perShareRisk * config.targetRatio)
                                } else {
                                    signal.entryPrice - (perShareRisk * config.targetRatio)
                                }

                                activeTrade = ActiveTrade(
                                    symbol = config.symbol,
                                    isLong = signal.isLong,
                                    entryIndex = i,
                                    entryDate = dateStr,
                                    entryPrice = signal.entryPrice,
                                    stopLoss = signal.stopLoss,
                                    targetPrice = targetPrice,
                                    shares = shares
                                )
                            }
                        }
                    }
                }
            }
        }

        // Close any lingering trade at last price
        if (activeTrade != null) {
            val trade = activeTrade
            val lastCandle = candles.last()
            val exitPrice = lastCandle.close
            val grossPnl = if (trade.isLong) {
                (exitPrice - trade.entryPrice) * trade.shares
            } else {
                (trade.entryPrice - exitPrice) * trade.shares
            }
            val netPnl = grossPnl * 0.999
            capital += netPnl
            trades.add(
                TradeRecord(
                    id = UUID.randomUUID().toString().take(8),
                    symbol = config.symbol,
                    isLong = trade.isLong,
                    entryDate = trade.entryDate,
                    entryPrice = trade.entryPrice,
                    exitDate = dateFormat.format(Date(lastCandle.timestamp)),
                    exitPrice = exitPrice,
                    shares = trade.shares,
                    stopLoss = trade.stopLoss,
                    targetPrice = trade.targetPrice,
                    pnl = netPnl,
                    pnlPercent = (netPnl / (trade.entryPrice * trade.shares)) * 100.0,
                    exitReason = "Lookback End (Mark to Market)",
                    durationDays = candles.size - trade.entryIndex
                )
            )
            equityCurve.add(dateFormat.format(Date(lastCandle.timestamp)) to capital)
        }

        // Calculate KPI Metrics
        val netProfit = capital - config.initialCapital
        val totalReturnPercent = (netProfit / config.initialCapital) * 100.0
        val winningTrades = trades.count { it.pnl > 0 }
        val losingTrades = trades.count { it.pnl <= 0 }
        val totalTrades = trades.size
        val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0

        val grossWins = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLosses = abs(trades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val profitFactor = if (grossLosses > 0) grossWins / grossLosses else if (grossWins > 0) 9.99 else 0.0

        val avgWin = if (winningTrades > 0) grossWins / winningTrades else 0.0
        val avgLoss = if (losingTrades > 0) grossLosses / losingTrades else 0.0

        // Sharpe calculation based on trade returns
        val tradeReturns = trades.map { it.pnlPercent / 100.0 }
        val meanReturn = if (tradeReturns.isNotEmpty()) tradeReturns.average() else 0.0
        val variance = if (tradeReturns.size > 1) {
            tradeReturns.map { (it - meanReturn) * (it - meanReturn) }.average()
        } else 0.0
        val stdDev = sqrt(variance)
        val sharpeRatio = if (stdDev > 0.001) (meanReturn / stdDev) * sqrt(12.0) else 0.0

        BacktestResult(
            config = config,
            totalReturnPercent = totalReturnPercent,
            netProfit = netProfit,
            finalCapital = capital,
            winRate = winRate,
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown,
            sharpeRatio = sharpeRatio,
            avgWinAmount = avgWin,
            avgLossAmount = avgLoss,
            trades = trades.reversed(), // Recent first
            equityCurve = equityCurve
        )
    }

    suspend fun saveBacktest(result: BacktestResult): Long {
        val entity = BacktestEntity(
            symbol = result.config.symbol,
            strategyName = result.config.strategy.displayName,
            timeframe = result.config.timeframe.label,
            periodMonths = result.config.periodMonths,
            initialCapital = result.config.initialCapital,
            finalCapital = result.finalCapital,
            totalReturnPercent = result.totalReturnPercent,
            netProfit = result.netProfit,
            winRate = result.winRate,
            totalTrades = result.totalTrades,
            winningTrades = result.winningTrades,
            losingTrades = result.losingTrades,
            profitFactor = result.profitFactor,
            maxDrawdownPercent = result.maxDrawdownPercent,
            sharpeRatio = result.sharpeRatio
        )
        return backtestDao.insertBacktest(entity)
    }

    suspend fun deleteBacktest(id: Long) {
        backtestDao.deleteBacktestById(id)
    }

    private data class ActiveTrade(
        val symbol: String,
        val isLong: Boolean,
        val entryIndex: Int,
        val entryDate: String,
        val entryPrice: Double,
        val stopLoss: Double,
        val targetPrice: Double,
        val shares: Int
    )

    private data class EntrySignal(
        val isLong: Boolean,
        val entryPrice: Double,
        val stopLoss: Double
    )

    private fun detectEntrySignal(
        candles: List<Candle>,
        idx: Int,
        strategy: BacktestStrategy
    ): EntrySignal? {
        val current = candles[idx]
        val prev = candles[idx - 1]
        val prev2 = candles[idx - 2]
        val smaSlope = (current.sma44 - candles[idx - 4].sma44) / 4.0

        return when (strategy) {
            BacktestStrategy.SMA44_BOUNCE -> {
                // Rising 44 SMA bounce
                if (smaSlope > 0.02) {
                    val touchedSma = current.low <= current.sma44 * 1.004 && current.close >= current.sma44 * 0.995
                    val isBullishReversal = current.isBullish && current.close > (current.high + current.low) / 2.0
                    if (touchedSma && isBullishReversal) {
                        val sl = min(current.low, prev.low) * 0.994
                        EntrySignal(isLong = true, entryPrice = current.close, stopLoss = sl)
                    } else null
                } else if (smaSlope < -0.02) {
                    // Falling 44 SMA rejection
                    val testedSma = current.high >= current.sma44 * 0.996 && current.close <= current.sma44 * 1.005
                    val isBearishRejection = !current.isBullish && current.close < (current.high + current.low) / 2.0
                    if (testedSma && isBearishRejection) {
                        val sl = max(current.high, prev.high) * 1.006
                        EntrySignal(isLong = false, entryPrice = current.close, stopLoss = sl)
                    } else null
                } else null
            }
            BacktestStrategy.SMA44_BREAKOUT -> {
                if (prev.close <= prev.sma44 && current.close > current.sma44 * 1.006 && current.isBullish) {
                    val sl = current.sma44 * 0.992
                    EntrySignal(isLong = true, entryPrice = current.close, stopLoss = sl)
                } else if (prev.close >= prev.sma44 && current.close < current.sma44 * 0.994 && !current.isBullish) {
                    val sl = current.sma44 * 1.008
                    EntrySignal(isLong = false, entryPrice = current.close, stopLoss = sl)
                } else null
            }
            BacktestStrategy.SMA44_SLOPE_CROSS -> {
                val prevSlope = (prev.sma44 - prev2.sma44)
                if (prevSlope <= 0 && smaSlope > 0.03 && current.close > current.sma44) {
                    EntrySignal(isLong = true, entryPrice = current.close, stopLoss = current.low * 0.99)
                } else if (prevSlope >= 0 && smaSlope < -0.03 && current.close < current.sma44) {
                    EntrySignal(isLong = false, entryPrice = current.close, stopLoss = current.high * 1.01)
                } else null
            }
        }
    }

    private fun emptyResult(config: BacktestConfig): BacktestResult {
        return BacktestResult(
            config = config,
            totalReturnPercent = 0.0,
            netProfit = 0.0,
            finalCapital = config.initialCapital,
            winRate = 0.0,
            totalTrades = 0,
            winningTrades = 0,
            losingTrades = 0,
            profitFactor = 0.0,
            maxDrawdownPercent = 0.0,
            sharpeRatio = 0.0,
            avgWinAmount = 0.0,
            avgLossAmount = 0.0,
            trades = emptyList(),
            equityCurve = emptyList()
        )
    }
}
