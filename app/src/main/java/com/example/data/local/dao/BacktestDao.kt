package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BacktestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BacktestDao {
    @Query("SELECT * FROM saved_backtests ORDER BY timestamp DESC")
    fun getAllBacktests(): Flow<List<BacktestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBacktest(backtest: BacktestEntity): Long

    @Delete
    suspend fun deleteBacktest(backtest: BacktestEntity)

    @Query("DELETE FROM saved_backtests WHERE id = :id")
    suspend fun deleteBacktestById(id: Long)

    @Query("DELETE FROM saved_backtests")
    suspend fun clearAll()
}
