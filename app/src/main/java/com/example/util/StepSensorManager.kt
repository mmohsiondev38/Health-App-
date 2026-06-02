package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepSensorManager(
    private val context: Context,
    private val onStepsUpdated: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    
    private val sharedPrefs = context.getSharedPreferences("health_sensor_prefs", Context.MODE_PRIVATE)

    var isLiveTracking = false
        private set

    // Whether hardware step sensor exists on the physical phone
    fun isSensorAvailable(): Boolean {
        return stepCounterSensor != null || stepDetectorSensor != null
    }

    // Baseline reference logic:
    // Sensor values count from boot. We store the first read of the day in prefs.
    // Daily increment = currentSensorValue - baselineSensorValue.
    fun startNewSession() {
        if (!isSensorAvailable()) return

        try {
            isLiveTracking = true
            // Listen to step counter first (more persistent) or fallback to step detector (realtime pulses)
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("StepSensorManager", "Registered TYPE_STEP_COUNTER listener successfully")
            } else if (stepDetectorSensor != null) {
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("StepSensorManager", "Registered TYPE_STEP_DETECTOR listener successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isLiveTracking = false
        }
    }

    fun stopSession() {
        if (!isLiveTracking) return
        try {
            sensorManager.unregisterListener(this)
            isLiveTracking = false
            Log.d("StepSensorManager", "Unregistered step sensor listeners successfully")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsCaptured = event.values[0].toInt()
            Log.d("StepSensorManager", "Step counter changed value: $totalStepsCaptured")

            if (totalStepsCaptured <= 0) return

            val baselineKey = "sensor_baseline_$todayStr"
            val lastRecordedValueKey = "sensor_last_val_$todayStr"

            val baseline = sharedPrefs.getInt(baselineKey, -1)
            val lastRecordedValue = sharedPrefs.getInt(lastRecordedValueKey, -1)

            if (baseline == -1) {
                // First step reading of today: set baseline
                sharedPrefs.edit()
                    .putInt(baselineKey, totalStepsCaptured)
                    .putInt(lastRecordedValueKey, totalStepsCaptured)
                    .apply()
                Log.d("StepSensorManager", "New day baseline calibrated to: $totalStepsCaptured")
            } else {
                // Calculate the delta increment since last onSensorChanged read of today
                val delta = totalStepsCaptured - lastRecordedValue
                if (delta > 0) {
                    onStepsUpdated(delta)
                    sharedPrefs.edit()
                        .putInt(lastRecordedValueKey, totalStepsCaptured)
                        .apply()
                    Log.d("StepSensorManager", "Sensor tracked delta step batch of +$delta steps")
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector triggers on individual steps.
            val detectedSteps = event.values[0].toInt()
            if (detectedSteps > 0) {
                onStepsUpdated(detectedSteps)
                Log.d("StepSensorManager", "Step detector fired +$detectedSteps steps")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
