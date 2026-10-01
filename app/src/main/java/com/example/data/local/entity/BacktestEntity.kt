package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_backtests")
data class BacktestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val strategyName: String,
    val timeframe: String,
    val periodMonths: Int,
    val initialCapital: Double,
    val finalCapital: Double,
    val totalReturnPercent: Double,
    val netProfit: Double,
    val winRate: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double,
    val timestamp: Long = System.currentTimeMillis()
)
