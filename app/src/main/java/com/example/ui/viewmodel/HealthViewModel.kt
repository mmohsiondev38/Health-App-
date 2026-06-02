package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DailySummary
import com.example.data.model.MealLog
import com.example.data.model.WeightLog
import com.example.data.repository.HealthMilestone
import com.example.data.repository.HealthRepository
import com.example.util.HealthNotificationHelper
import com.example.util.StepSensorManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository
    private val notificationHelper: HealthNotificationHelper
    private val stepSensorManager: StepSensorManager

    // --- State Variables ---
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Sensor State Flows
    private val _isSensorTracking = MutableStateFlow(false)
    val isSensorTracking: StateFlow<Boolean> = _isSensorTracking.asStateFlow()

    val isSensorHardwareAvailable: Boolean by lazy {
        stepSensorManager.isSensorAvailable()
    }

    // Recent UI alerts/celebrations for in-app toasts
    private val _milestoneCelebration = MutableStateFlow<String?>(null)
    val milestoneCelebration: StateFlow<String?> = _milestoneCelebration.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = HealthRepository(database.healthDao())
        notificationHelper = HealthNotificationHelper(application)
        stepSensorManager = StepSensorManager(application) { increment ->
            addTodayPhysicalSteps(increment)
        }
        
        // Pre-create summary for today
        viewModelScope.launch {
            repository.getOrCreateSummary(getTodayDateString())
        }
    }

    // --- Flows ---
    val currentDailySummary: StateFlow<DailySummary?> = _selectedDate
        .flatMapLatest { date -> repository.getDailySummary(date) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentMeals: StateFlow<List<MealLog>> = _selectedDate
        .flatMapLatest { date -> repository.getMealsForDay(date) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentWeights: StateFlow<List<WeightLog>> = repository.getRecentWeightLogs(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allSummaries: StateFlow<List<DailySummary>> = repository.allSummaries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Date Helpers ---
    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            repository.getOrCreateSummary(date)
        }
    }

    fun nextDay() {
        val current = _selectedDate.value
        selectDate(getAdjacentDate(current, 1))
    }

    fun previousDay() {
        val current = _selectedDate.value
        selectDate(getAdjacentDate(current, -1))
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun getAdjacentDate(dateStr: String, offsetDays: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = sdf.parse(dateStr) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, offsetDays)
            }
            sdf.format(calendar.time)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = getTodayDateString()
        if (dateStr == todayStr) return "Today"
        
        return try {
            val date = inputSdf.parse(dateStr) ?: Date()
            val outputSdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            outputSdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    // --- Actions ---
    fun addSteps(count: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val milestones = repository.addSteps(date, count)
            handleMilestones(milestones)
        }
    }

    fun setStepsDirect(total: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val milestones = repository.setSteps(date, total)
            handleMilestones(milestones)
        }
    }

    fun addMeal(title: String, calories: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val milestones = repository.addMeal(date, title, calories)
            handleMilestones(milestones)
        }
    }

    fun deleteMeal(mealId: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.deleteMeal(mealId, date)
        }
    }

    fun recordWeight(weight: Float) {
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.addWeight(date, weight)
        }
    }

    fun deleteWeightLog(weightId: Int) {
        viewModelScope.launch {
            repository.deleteWeightLog(weightId)
        }
    }

    fun updateGoals(stepGoal: Int?, calorieGoal: Int?, weightGoal: Float?) {
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.updateGoals(date, stepGoal, calorieGoal, weightGoal)
        }
    }

    fun clearCelebration() {
        _milestoneCelebration.value = null
    }

    // --- On-Device Step Sensor Actions ---
    private fun addTodayPhysicalSteps(count: Int) {
        val today = getTodayDateString()
        viewModelScope.launch {
            val milestones = repository.addSteps(today, count)
            handleMilestones(milestones)
        }
    }

    fun toggleSensorTracking() {
        if (!isSensorHardwareAvailable) {
            return
        }
        if (_isSensorTracking.value) {
            stepSensorManager.stopSession()
            _isSensorTracking.value = false
        } else {
            stepSensorManager.startNewSession()
            _isSensorTracking.value = stepSensorManager.isLiveTracking
        }
    }

    fun simulateSensorIncrement() {
        val count = 250
        addTodayPhysicalSteps(count)
        
        // Trigger a friendly celebration toast/alert
        val text = "Captured step sync: +250 physical steps added to today's active metrics!"
        _milestoneCelebration.value = "👟 Sensor Synced\n$text"
        notificationHelper.postNotification("👟 Sensor Motion Tracked", text)
    }

    // --- Milestone Notifications and Animations Handler ---
    private fun handleMilestones(milestones: List<HealthMilestone>) {
        if (milestones.isEmpty()) return
        
        viewModelScope.launch {
            milestones.forEach { milestone ->
                val (title, text) = when (milestone) {
                    HealthMilestone.STEPS_50 -> Pair(
                        "🎉 Halfway There!",
                        "Fantastic effort! You've walked 50% of your daily step goal. Keep moving!"
                    )
                    HealthMilestone.STEPS_100 -> Pair(
                        "🏆 Step Goal Achieved!",
                        "Sensational! You hit your daily steps milestone. High-five to healthy habits!"
                    )
                    HealthMilestone.CALORIES_100 -> Pair(
                        "🍎 Fuel Completed!",
                        "Great tracking! You have hit your calorie goal budget to power through the day."
                    )
                }
                
                // 1. Post System Notification (Channel support, permissions verified)
                notificationHelper.postNotification(title, text)
                
                // 2. Trigger In-App Celebration Banner for real-time visual joy
                _milestoneCelebration.value = "$title\n$text"
            }
        }
    }

    // --- Week Progress Data ---
    fun getWeekSummary(): List<Pair<String, Double>> {
        val list = allSummaries.value
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        
        val weekData = mutableListOf<Pair<String, Double>>()
        
        // Fill details for the past 7 days
        for (i in 6 downTo 0) {
            val offsetCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = sdf.format(offsetCalendar.time)
            val dayLabel = dayFormat.format(offsetCalendar.time)
            
            val daySummary = list.find { it.date == dateStr }
            val completionRatio = if (daySummary != null && daySummary.stepGoal > 0) {
                daySummary.steps.toDouble() / daySummary.stepGoal.toDouble()
            } else {
                0.0
            }
            weekData.add(Pair(dayLabel, completionRatio))
        }
        return weekData
    }

    // --- Consistency Level Calculator ---
    fun getWeekConsistencyMetrics(): Triple<Int, Int, String> {
        val list = allSummaries.value
        val last7DaysSummaries = mutableListOf<DailySummary>()
        
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        for (i in 6 downTo 0) {
            val offsetCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = sdf.format(offsetCalendar.time)
            val summary = list.find { it.date == dateStr }
            if (summary != null) {
                last7DaysSummaries.add(summary)
            }
        }
        
        val daysActive = last7DaysSummaries.size
        val goalsMet = last7DaysSummaries.count { it.steps >= it.stepGoal }
        
        val progressMsg = when {
            goalsMet == 7 -> "🏆 Absolute Perfection! Clean sweep of your weekly step goals. Incredible work!"
            goalsMet >= 5 -> "⚡ Magnificent Consistency! Dynamic progress, you are actively leading a stellar active life."
            goalsMet >= 3 -> "🌟 Solid Momentum! Your consistency is forming excellent lifestyle habits."
            goalsMet >= 1 -> "🌱 Nice Beginnings! Consistent progress builds power. Log steps daily to unlock streaks!"
            else -> "💪 Ready for Action! Take a gentle 15-minute walk today to kickstart your step consistency!"
        }
        
        return Triple(daysActive, goalsMet, progressMsg)
    }

    override fun onCleared() {
        super.onCleared()
        if (_isSensorTracking.value) {
            stepSensorManager.stopSession()
        }
    }
}
