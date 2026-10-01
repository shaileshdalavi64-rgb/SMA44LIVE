package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)
