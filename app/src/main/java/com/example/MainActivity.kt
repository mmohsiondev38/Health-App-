package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.HealthTrackerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HealthViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: HealthViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        HealthTrackerApp(viewModel = viewModel, modifier = Modifier.fillMaxSize())
      }
    }
  }
}
