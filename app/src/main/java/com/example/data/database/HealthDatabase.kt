package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.HealthDao
import com.example.data.model.DailySummary
import com.example.data.model.MealLog
import com.example.data.model.WeightLog

@Database(
    entities = [DailySummary::class, MealLog::class, WeightLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
