package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String, // format "YYYY-MM-DD"
    val steps: Int = 0,
    val stepGoal: Int = 10000,
    val caloriesConsumed: Int = 0,
    val calorieGoal: Int = 2000,
    val weight: Float? = null,
    val weightGoal: Float = 70.0f,
    val milestoneNotifiedSteps50: Boolean = false,
    val milestoneNotifiedSteps100: Boolean = false,
    val milestoneNotifiedCal100: Boolean = false
)

@Entity(tableName = "meal_logs")
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "YYYY-MM-DD"
    val foodName: String,
    val calories: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "YYYY-MM-DD"
    val weight: Float,
    val timestamp: Long = System.currentTimeMillis()
)
