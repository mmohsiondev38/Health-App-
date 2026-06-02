package com.example.data.dao

import androidx.room.*
import com.example.data.model.DailySummary
import com.example.data.model.MealLog
import com.example.data.model.WeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // --- DailySummary ---
    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getDailySummary(date: String): Flow<DailySummary?>

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getDailySummaryDirect(date: String): DailySummary?

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAllDailySummaries(): Flow<List<DailySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummary)

    // --- MealLog ---
    @Query("SELECT * FROM meal_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getMealsForDay(date: String): Flow<List<MealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealLog)

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteMealById(id: Int)

    // --- WeightLog ---
    @Query("SELECT * FROM weight_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getWeightLogsForDay(date: String): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWeightLogs(limit: Int): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightById(id: Int)
}
