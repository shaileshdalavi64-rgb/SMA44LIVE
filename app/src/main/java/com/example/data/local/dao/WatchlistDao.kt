package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlist(symbol: String): Flow<Boolean>

    @Query("SELECT symbol FROM watchlist")
    suspend fun getAllWatchlistSymbols(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)
}
