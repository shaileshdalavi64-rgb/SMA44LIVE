package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AlertDao
import com.example.data.local.dao.BacktestDao
import com.example.data.local.dao.WatchlistDao
import com.example.data.local.entity.AlertEntity
import com.example.data.local.entity.BacktestEntity
import com.example.data.local.entity.WatchlistEntity

@Database(
    entities = [
        AlertEntity::class,
        BacktestEntity::class,
        WatchlistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun backtestDao(): BacktestDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sma44_scanner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
