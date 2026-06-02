package com.example.data.repository

import com.example.data.dao.HealthDao
import com.example.data.model.DailySummary
import com.example.data.model.MealLog
import com.example.data.model.WeightLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

enum class HealthMilestone {
    STEPS_50,
    STEPS_100,
    CALORIES_100
}

class HealthRepository(private val healthDao: HealthDao) {

    val allSummaries: Flow<List<DailySummary>> = healthDao.getAllDailySummaries()

    fun getDailySummary(date: String): Flow<DailySummary?> = healthDao.getDailySummary(date)

    fun getMealsForDay(date: String): Flow<List<MealLog>> = healthDao.getMealsForDay(date)

    fun getRecentWeightLogs(limit: Int = 30): Flow<List<WeightLog>> = healthDao.getRecentWeightLogs(limit)

    suspend fun getOrCreateSummary(date: String): DailySummary = withContext(Dispatchers.IO) {
        val existing = healthDao.getDailySummaryDirect(date)
        if (existing != null) {
            existing
        } else {
            // Get previous summary to copy the goals forward if they exist
            val all = healthDao.getAllDailySummaries().firstOrNull() ?: emptyList()
            val latestGoals = all.firstOrNull()
            val newSummary = DailySummary(
                date = date,
                stepGoal = latestGoals?.stepGoal ?: 10000,
                calorieGoal = latestGoals?.calorieGoal ?: 2000,
                weightGoal = latestGoals?.weightGoal ?: 70.0f,
                weight = latestGoals?.weight // carry weight forward
            )
            healthDao.insertDailySummary(newSummary)
            newSummary
        }
    }

    suspend fun addSteps(date: String, increment: Int): List<HealthMilestone> = withContext(Dispatchers.IO) {
        val summary = getOrCreateSummary(date)
        val oldSteps = summary.steps
        val newSteps = oldSteps + increment
        val stepGoal = summary.stepGoal

        val triggeredMilestones = mutableListOf<HealthMilestone>()
        var notify50 = summary.milestoneNotifiedSteps50
        var notify100 = summary.milestoneNotifiedSteps100

        if (!notify50 && oldSteps < (stepGoal / 2) && newSteps >= (stepGoal / 2)) {
            triggeredMilestones.add(HealthMilestone.STEPS_50)
            notify50 = true
        }

        if (!notify100 && oldSteps < stepGoal && newSteps >= stepGoal) {
            triggeredMilestones.add(HealthMilestone.STEPS_100)
            notify100 = true
        }

        val updated = summary.copy(
            steps = newSteps,
            milestoneNotifiedSteps50 = notify50,
            milestoneNotifiedSteps100 = notify100
        )
        healthDao.insertDailySummary(updated)
        triggeredMilestones
    }

    suspend fun setSteps(date: String, total: Int): List<HealthMilestone> = withContext(Dispatchers.IO) {
        val summary = getOrCreateSummary(date)
        val oldSteps = summary.steps
        val newSteps = total.coerceAtLeast(0)
        val stepGoal = summary.stepGoal

        val triggeredMilestones = mutableListOf<HealthMilestone>()
        var notify50 = summary.milestoneNotifiedSteps50
        var notify100 = summary.milestoneNotifiedSteps100

        if (!notify50 && oldSteps < (stepGoal / 2) && newSteps >= (stepGoal / 2)) {
            triggeredMilestones.add(HealthMilestone.STEPS_50)
            notify50 = true
        }

        if (!notify100 && oldSteps < stepGoal && newSteps >= stepGoal) {
            triggeredMilestones.add(HealthMilestone.STEPS_100)
            notify100 = true
        }

        val updated = summary.copy(
            steps = newSteps,
            milestoneNotifiedSteps50 = notify50,
            milestoneNotifiedSteps100 = notify100
        )
        healthDao.insertDailySummary(updated)
        triggeredMilestones
    }

    suspend fun addMeal(date: String, foodName: String, calories: Int): List<HealthMilestone> = withContext(Dispatchers.IO) {
        val summary = getOrCreateSummary(date)
        val meal = MealLog(date = date, foodName = foodName, calories = calories)
        healthDao.insertMeal(meal)

        // Sum current day meal logs
        val meals = healthDao.getMealsForDay(date).firstOrNull() ?: emptyList()
        val totalCalories = meals.sumOf { it.calories } + calories // current meal is included manually because of Flow async

        val oldCalories = summary.caloriesConsumed
        val calorieGoal = summary.calorieGoal

        val triggeredMilestones = mutableListOf<HealthMilestone>()
        var notifyCal100 = summary.milestoneNotifiedCal100

        if (!notifyCal100 && oldCalories < calorieGoal && totalCalories >= calorieGoal) {
            triggeredMilestones.add(HealthMilestone.CALORIES_100)
            notifyCal100 = true
        }

        val updated = summary.copy(
            caloriesConsumed = totalCalories,
            milestoneNotifiedCal100 = notifyCal100
        )
        healthDao.insertDailySummary(updated)
        triggeredMilestones
    }

    suspend fun deleteMeal(mealId: Int, date: String) = withContext(Dispatchers.IO) {
        healthDao.deleteMealById(mealId)

        // Recalculate summary calories
        val meals = healthDao.getMealsForDay(date).firstOrNull() ?: emptyList()
        val totalCalories = meals.filter { it.id != mealId }.sumOf { it.calories }

        val summary = getOrCreateSummary(date)
        val updated = summary.copy(caloriesConsumed = totalCalories)
        healthDao.insertDailySummary(updated)
    }

    suspend fun addWeight(date: String, weight: Float) = withContext(Dispatchers.IO) {
        val log = WeightLog(date = date, weight = weight)
        healthDao.insertWeight(log)

        val summary = getOrCreateSummary(date)
        val updated = summary.copy(weight = weight)
        healthDao.insertDailySummary(updated)
    }

    suspend fun deleteWeightLog(weightId: Int) = withContext(Dispatchers.IO) {
        healthDao.deleteWeightById(weightId)
        // Note: we keep the latest weight in daily summaries as is
    }

    suspend fun updateGoals(date: String, stepGoal: Int?, calorieGoal: Int?, weightGoal: Float?) = withContext(Dispatchers.IO) {
        val summary = getOrCreateSummary(date)
        val updated = summary.copy(
            stepGoal = stepGoal ?: summary.stepGoal,
            calorieGoal = calorieGoal ?: summary.calorieGoal,
            weightGoal = weightGoal ?: summary.weightGoal
        )
        healthDao.insertDailySummary(updated)
    }
}
