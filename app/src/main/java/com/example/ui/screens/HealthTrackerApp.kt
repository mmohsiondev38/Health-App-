package com.example.ui.screens

import android.Manifest
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailySummary
import com.example.data.model.MealLog
import com.example.data.model.WeightLog
import com.example.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthTrackerApp(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Interactive custom state managers
    var showPermissionPromo by remember { mutableStateOf(!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) }
    var currentTab by remember { mutableStateOf("dashboard") } // "dashboard", "meals", "weight", "goals"

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailySummary by viewModel.currentDailySummary.collectAsStateWithLifecycle()
    val mealsToday by viewModel.currentMeals.collectAsStateWithLifecycle()
    val recentWeights by viewModel.recentWeights.collectAsStateWithLifecycle()
    val allSummaries by viewModel.allSummaries.collectAsStateWithLifecycle()
    val milestoneCelebration by viewModel.milestoneCelebration.collectAsStateWithLifecycle()

    // Automatic Permission Prompt Dialog
    if (showPermissionPromo) {
        Dialog(onDismissRequest = { showPermissionPromo = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121625)),
                border = BorderStroke(1.dp, Color(0xFF1B2C4F)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF0F254B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = Color(0xFF2979FF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enable Milestone Alerts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay motivated with instant push notifications when you hit 50% and 100% of your steps, caloric budget targets, and weight progress highlights.",
                        color = Color(0xFF9AA39F),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPermissionPromo = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9AA39F)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C3548)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }
                        Button(
                            onClick = {
                                showPermissionPromo = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }
    }

    // In-app floating celebration slide-down
    milestoneCelebration?.let { celebrationText ->
        LaunchedEffect(celebrationText) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearCelebration()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .zIndex(99f),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2443)),
                border = BorderStroke(2.dp, Color(0xFF2979FF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearCelebration() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2979FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Event Award",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val parts = celebrationText.split("\n", limit = 2)
                        Text(
                            text = parts.getOrNull(0) ?: "Goal Met!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = parts.getOrNull(1) ?: "",
                            color = Color(0xFF9AA39F),
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                    IconButton(onClick = { viewModel.clearCelebration() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Banner",
                            tint = Color(0xFF9AA39F)
                        )
                    }
                }
            }
        }
    }

    // Master Layout Scaffold
    Scaffold(
        containerColor = Color(0xFF090D1A), // Aura Active Premium Space Blue
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0E1322),
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .border(BorderStroke(0.5.dp, Color(0xFF1B2332)), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                listOf(
                    Triple("dashboard", "Aura", Icons.Default.Dashboard),
                    Triple("meals", "Fuel", Icons.Default.Restaurant),
                    Triple("goals", "Target", Icons.Default.Tune)
                ).forEach { item ->
                    val isSelected = currentTab == item.first
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = item.first },
                        icon = {
                            Icon(
                                imageVector = item.third,
                                contentDescription = item.second,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.second,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF2979FF),
                            indicatorColor = Color(0xFF2979FF),
                            unselectedIconColor = Color(0xFF5E6D66),
                            unselectedTextColor = Color(0xFF5E6D66)
                        ),
                        modifier = Modifier.testTag("nav_tab_${item.first}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (currentTab == "dashboard" || currentTab == "meals") {
                // --- TOP HEADER: Date Selector and Motivational Badge ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF121625), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Day",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = viewModel.formatDisplayDate(selectedDate),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) Color(0xFF2979FF) else Color(0xFF5E6D66),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = if (selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) "Active Tracker" else "Locked Record",
                                    color = Color(0xFF5E6D66),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.nextDay() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF121625), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Day",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Header Performance stats quick summary
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, Color(0xFF1B2A4A)), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0E1A2C), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable { showPermissionPromo = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.Notifications else Icons.Outlined.NotificationsOff,
                                contentDescription = "Channel Status",
                                tint = if (hasNotificationPermission) Color(0xFF2979FF) else Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (hasNotificationPermission) "Alert Live" else "Muted",
                                color = if (hasNotificationPermission) Color(0xFF2979FF) else Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF111725), thickness = 1.dp)
            }

            // --- MAIN SWITCH VIEWS BLOCK ---
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    "dashboard" -> DashboardView(
                        summary = dailySummary ?: DailySummary(selectedDate),
                        viewModel = viewModel
                    )
                    "meals" -> MealsView(
                        summary = dailySummary ?: DailySummary(selectedDate),
                        meals = mealsToday,
                        viewModel = viewModel
                    )
                    "goals" -> SettingsView(
                        summary = dailySummary ?: DailySummary(selectedDate),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD VIEW (DASHBOARD/ANALYTICS)
// ==========================================
@Composable
fun DashboardView(
    summary: DailySummary,
    viewModel: HealthViewModel
) {
    val context = LocalContext.current
    val isSensorTracking by viewModel.isSensorTracking.collectAsStateWithLifecycle()
    val isSensorHardwareAvailable = viewModel.isSensorHardwareAvailable

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasActivityPermission = isGranted
        if (isGranted) {
            viewModel.toggleSensorTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (isSensorTracking && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    var showStepQuickAdjust by remember { mutableStateOf(false) }
    var stepInputVal by remember { mutableStateOf("") }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    val consistencyMetric = viewModel.getWeekConsistencyMetrics()
    val weeklyPillData = viewModel.getWeekSummary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- SMART COACH COACHING ROW ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF1B2C44)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2979FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsGymnastics,
                        contentDescription = "Coach Avatar",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AURA ACTIVE COACH",
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = consistencyMetric.third,
                        color = Color.White,
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp)
                    )
                }
            }
        }

        // --- THE DUAL SPLIT CARDS (STEPS & CALORIES) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // STEP CARD (EMERALD RADIAL GAUGE)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121625)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF1C2D4A)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStepQuickAdjust = true }
                    .testTag("card_steps")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF11233D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Steps",
                                tint = Color(0xFF2979FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSensorTracking) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF2979FF), CircleShape)
                                )
                            }
                            Text(
                                text = if (isSensorTracking) "STEPS LIVE" else "STEPS",
                                color = if (isSensorTracking) Color(0xFF2979FF) else Color(0xFF5E6D66),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Gauge Drawing Area
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            // Backdrop ring
                            drawArc(
                                color = Color(0xFF1B233A),
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Filled ring
                            val ratio = if (summary.stepGoal > 0) summary.steps.toFloat() / summary.stepGoal.toFloat() else 0f
                            val sweepAngle = (ratio * 260f).coerceAtMost(260f)
                            drawArc(
                                color = Color(0xFF2979FF),
                                startAngle = -220f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = summary.steps.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Goal ${summary.stepGoal}",
                                color = Color(0xFF5E6D66),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val km = String.format(Locale.getDefault(), "%.2f", summary.steps * 0.00075)
                    Text(
                        text = "$km km walked",
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Tap to update",
                        color = Color(0xFF5E6D66),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // CALORIES CARD (ORANGE TEMPERATURE GAUGE)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121425)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF1C274A)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_calories")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFF2B1A24), CircleShape),
                                                    contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Calories",
                                tint = Color(0xFFFF7600),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "CALORIES",
                            color = Color(0xFF8C798A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Gauge Drawing Area
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            // Backdrop ring
                            drawArc(
                                color = Color(0xFF1F1B26),
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Filled ring
                            val ratio = if (summary.calorieGoal > 0) summary.caloriesConsumed.toFloat() / summary.calorieGoal.toFloat() else 0f
                            val sweepAngle = (ratio * 260f).coerceAtMost(260f)
                            drawArc(
                                color = Color(0xFFFF7600),
                                startAngle = -220f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = summary.caloriesConsumed.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Goal ${summary.calorieGoal}",
                                color = Color(0xFF6B584E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val leftCalories = (summary.calorieGoal - summary.caloriesConsumed).coerceAtLeast(0)
                    Text(
                        text = "$leftCalories kcal left",
                        color = Color(0xFFFF7600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Fuel recorded",
                        color = Color(0xFF6B584E),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- COHESIVE WEIGHT QUICKCARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13172B)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF1B264A)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAddWeightDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF15223A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = "Scale Weight",
                            tint = Color(0xFF2979FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WEIGHT MEASURE",
                            color = Color(0xFF5B698A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (summary.weight != null) "${summary.weight} kg" else "Unrecorded today",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Goal ${summary.weightGoal} kg",
                        color = Color(0xFF5B698A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    summary.weight?.let { current ->
                        val diff = current - summary.weightGoal
                        val text = when {
                            diff > 0 -> String.format(Locale.getDefault(), "+%.1f kg above", diff)
                            diff < 0 -> String.format(Locale.getDefault(), "%.1f kg to meet", -diff)
                            else -> "At exact weight goal!"
                        }
                        Text(
                            text = text,
                            color = if (diff <= 0) Color(0xFF2979FF) else Color(0xFFFFAB40),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } ?: Text(
                        text = "Tap scale to log",
                        color = Color(0xFF5B698A),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- WEEKLY CONSISTENCY HEATMAP PANEL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF171B29)), RoundedCornerShape(20.dp))
                .background(Color(0xFF0E1220), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "WEEKLY STEP STREAKS",
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Goal Met: ${consistencyMetric.second} of 7 days",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F2C4A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val score = if (weeklyPillData.isNotEmpty()) {
                        (weeklyPillData.count { it.second >= 1.0 } * 100 / weeklyPillData.size)
                    } else 0
                    Text(
                        text = "$score% SCORE",
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Capsule streak heatmaps row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyPillData.forEach { (day, ratio) ->
                    val isGoalMet = ratio >= 1.0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Capsule outline
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isGoalMet) Color(0xFF2979FF) else Color(0xFF171B29))
                                .border(
                                    BorderStroke(
                                        width = 1.dp,
                                        color = if (isGoalMet) Color.Transparent else Color(0xFF2C3245)
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (!isGoalMet && ratio > 0) {
                                // Draw a half fill background indicator
                                val fillHeight = (ratio * 56).coerceAtMost(56.0).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(fillHeight)
                                        .background(Color(0xFF0A2E5C))
                                )
                            }
                            if (isGoalMet) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success Complete",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                        Text(
                            text = day,
                            color = if (isGoalMet) Color.White else Color(0xFF5E6D66),
                            fontWeight = if (isGoalMet) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Modal quick adjusts Dialog for Steps
    if (showStepQuickAdjust) {
        Dialog(onDismissRequest = { showStepQuickAdjust = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121625)),
                border = BorderStroke(1.dp, Color(0xFF1B2C4F)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Steps Walked",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1000, 2500, 5000).forEach { inc ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.addSteps(inc)
                                    showStepQuickAdjust = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2979FF)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF2979FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+$inc")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = stepInputVal,
                        onValueChange = { stepInputVal = it },
                        label = { Text("Custom Amount", color = Color(0xFF5E6D66)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2979FF),
                            unfocusedBorderColor = Color(0xFF2C3245),
                            focusedLabelColor = Color(0xFF2979FF),
                            unfocusedLabelColor = Color(0xFF5E6D66),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("steps_input_field")
                    )

                    HorizontalDivider(color = Color(0xFF1B2A4A), thickness = 1.dp)

                    // --- SENSOR SYNC / GOOGLE FIT HUB ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1524), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1C325C)), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSensorTracking) Color(0xFF2979FF) else Color(0xFF1B2332),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSensorTracking) Icons.Default.DirectionsRun else Icons.Default.Sensors,
                                        contentDescription = "Sensor",
                                        tint = if (isSensorTracking) Color.Black else Color(0xFF5E6D66),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "PHONE STEPS HARDWARE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Active lighting badge status
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSensorTracking) Color(0xFF0A2443) else Color(0xFF22283A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSensorTracking) "ACTIVE" else "OFFLINE",
                                    color = if (isSensorTracking) Color(0xFF2979FF) else Color(0xFF9AA39F),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isSensorHardwareAvailable) {
                            Text(
                                text = "Listen to your dynamic hardware chip directly to automate real-time step increments whenever you are on a walk.",
                                color = Color(0xFF9AA39F),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isSensorTracking) "Disconnect Sensor Link" else "Connect Real-Time Sensor",
                                    color = if (isSensorTracking) Color(0xFF2979FF) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Switch(
                                    checked = isSensorTracking,
                                    onCheckedChange = { _ ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) {
                                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                        } else {
                                            viewModel.toggleSensorTracking()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(0xFF2979FF),
                                        uncheckedThumbColor = Color(0xFF5E6D66),
                                        uncheckedTrackColor = Color(0xFF1B2332)
                                    )
                                )
                            }
                        } else {
                            // Hardware chipset is missing (emulator sandbox fallback trigger)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Phone stride count chip is inactive inside this browser sandbox/emulator. Simulating live Google Fit active walking feeds instead.",
                                    color = Color(0xFF8B9691),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                Button(
                                    onClick = {
                                        viewModel.simulateSensorIncrement()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0E2547),
                                        contentColor = Color(0xFF2979FF)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1C3A6B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsWalk,
                                        contentDescription = "Simulate sensors walk steps",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Simulate Active Motion (+250 steps)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1B2A4A), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStepQuickAdjust = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9AA39F)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C3245)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                val value = stepInputVal.toIntOrNull()
                                if (value != null) {
                                    viewModel.addSteps(value)
                                    stepInputVal = ""
                                    showStepQuickAdjust = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("steps_submit_dialog_btn")
                        ) {
                            Text("Append")
                        }
                    }
                }
            }
        }
    }

    // Modal record Weight entry dialog inside Dashboard (Aura)
    if (showAddWeightDialog) {
        Dialog(onDismissRequest = { showAddWeightDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121625)),
                border = BorderStroke(1.dp, Color(0xFF1B2C4F)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Log Current Weight",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)", color = Color(0xFF5B698A)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2979FF),
                            unfocusedBorderColor = Color(0xFF2C3245),
                            focusedLabelColor = Color(0xFF2979FF),
                            unfocusedLabelColor = Color(0xFF5B698A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("weight_input_field")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddWeightDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5B698A)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C3245)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                val wt = weightInput.toFloatOrNull()
                                if (wt != null) {
                                    viewModel.recordWeight(wt)
                                    weightInput = ""
                                    showAddWeightDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("weight_submit_dialog_btn")
                        ) {
                            Text("Log Scale")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MEALS TRACKER VIEW (FUEL/CALORIES)
// ==========================================
@Composable
fun MealsView(
    summary: DailySummary,
    meals: List<MealLog>,
    viewModel: HealthViewModel
) {
    var showAddMealDialog by remember { mutableStateOf(false) }
    var foodNameInput by remember { mutableStateOf("") }
    var caloriesInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SUMMARY WATERFALL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1713)),
            border = BorderStroke(1.dp, Color(0xFF3B2618)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "CALORIC METRICS SUMMARY",
                    color = Color(0xFFFF7600),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Goal Budget", color = Color(0xFF8C776D), fontSize = 11.sp)
                        Text("${summary.calorieGoal} kcal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Consumed (-)", color = Color(0xFF8C776D), fontSize = 11.sp)
                        Text("${summary.caloriesConsumed} kcal", color = Color(0xFFFF7600), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining Budget", color = Color(0xFF8C776D), fontSize = 11.sp)
                        val remaining = (summary.calorieGoal - summary.caloriesConsumed).coerceAtLeast(0)
                        Text("$remaining kcal", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                val fraction = if (summary.calorieGoal > 0) summary.caloriesConsumed.toFloat() / summary.calorieGoal.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { fraction.coerceAtMost(1f) },
                    color = if (fraction > 1.0f) Color(0xFFFF3D00) else Color(0xFFFF7600),
                    trackColor = Color(0xFF2E1F16),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // --- SECTION HEADER: Logs list ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LOGGED MEALS TODAY",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Button(
                onClick = { showAddMealDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C190F), contentColor = Color(0xFFFF7600)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF55321B)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_meal_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Log", modifier = Modifier.size(16.dp))
                    Text("Add Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- MEALS LAZYCOLUMN ---
        if (meals.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF1E1410)), RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E0B09)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.NoFood,
                        contentDescription = "Empty Meals",
                        tint = Color(0xFF6B584E),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No fuel logs entered today yet.",
                        color = Color(0xFF6B584E),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(meals) { meal ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15110F)),
                        border = BorderStroke(1.dp, Color(0xFF281F1B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.foodName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.timestamp))
                                Text(
                                    text = "Logged at $timeStr",
                                    color = Color(0xFF6B584E),
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "+${meal.calories} kcal",
                                    color = Color(0xFFFF7600),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { viewModel.deleteMeal(meal.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFF9E2A2B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add meal items details
    if (showAddMealDialog) {
        Dialog(onDismissRequest = { showAddMealDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1713)),
                border = BorderStroke(1.dp, Color(0xFF3C2719)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Record Calories Entry",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        value = foodNameInput,
                        onValueChange = { foodNameInput = it },
                        label = { Text("What did you eat?", color = Color(0xFF8C776D)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7600),
                            unfocusedBorderColor = Color(0xFF423025),
                            focusedLabelColor = Color(0xFFFF7600),
                            unfocusedLabelColor = Color(0xFF8C776D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("meal_name_field")
                    )
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it },
                        label = { Text("Approx Calories (kcal)", color = Color(0xFF8C776D)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7600),
                            unfocusedBorderColor = Color(0xFF423025),
                            focusedLabelColor = Color(0xFFFF7600),
                            unfocusedLabelColor = Color(0xFF8C776D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("meal_cal_field")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddMealDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8C776D)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF423025)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                val cals = caloriesInput.toIntOrNull()
                                val name = foodNameInput.trim()
                                if (name.isNotEmpty() && cals != null) {
                                    viewModel.addMeal(name, cals)
                                    foodNameInput = ""
                                    caloriesInput = ""
                                    showAddMealDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7600), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("meal_submit_dialog_btn")
                        ) {
                            Text("Log Item")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. WEIGHT LOGS VIEW (SCALE/WEIGHT PROGRESS)
// ==========================================
@Composable
fun WeightView(
    summary: DailySummary,
    weights: List<WeightLog>,
    viewModel: HealthViewModel
) {
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SUMMARY WEIGHT STATS COMPANION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A22)),
            border = BorderStroke(1.dp, Color(0xFF1C2C3F)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "WEIGHT PROGRESS COMPANION",
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("Weight Goal", color = Color(0xFF6B7F96), fontSize = 11.sp)
                            Text("${summary.weightGoal} kg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Column {
                            Text("Latest Logged", color = Color(0xFF6B7F96), fontSize = 11.sp)
                            Text(
                                text = if (summary.weight != null) "${summary.weight} kg" else "--",
                                color = Color(0xFF2979FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddWeightDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152233), contentColor = Color(0xFF2979FF)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF233954)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_weight_fab")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Log", modifier = Modifier.size(16.dp))
                        Text("Log Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- HIGH-FIDELITY CUSTOM LINE CHART CANVAS ---
        Text(
            text = "PROGRESS TRACKING (RECENT WEIGHTS)",
            color = Color(0xFF6B7F96),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1317)),
            border = BorderStroke(1.dp, Color(0xFF1B232D)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (weights.size < 2) {
                // Empty state for weight chart
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Empty Chart",
                            tint = Color(0xFF4C5866),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "A minimum of 2 logs is required to plot progress charts.",
                            color = Color(0xFF4C5866),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Gorgeous Custom Canvas Weight Bezier Chart
                val reversedWeights = remember(weights) { weights.sortedBy { it.timestamp } }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val minWeight = reversedWeights.minOf { it.weight } * 0.98f
                    val maxWeight = reversedWeights.maxOf { it.weight } * 1.02f
                    val weightRange = if (maxWeight - minWeight == 0f) 1f else (maxWeight - minWeight)

                    val points = reversedWeights.mapIndexed { idx, log ->
                        val x = if (reversedWeights.size > 1) idx.toFloat() / (reversedWeights.size - 1) * width else width / 2
                        val y = height - ((log.weight - minWeight) / weightRange * height)
                        Offset(x, y)
                    }

                    // 1. Draw subtle background guidelines
                    val linesCount = 4
                    for (i in 0..linesCount) {
                        val yGrid = height / linesCount * i
                        drawLine(
                            color = Color(0xFF1B232D),
                            start = Offset(0f, yGrid),
                            end = Offset(width, yGrid),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }

                    // 2. Draw Bezier Line connects
                    val bezierPath = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val conPointX1 = (p1.x + p2.x) / 2
                                val conPointY1 = p1.y
                                val conPointX2 = (p1.x + p2.x) / 2
                                val conPointY2 = p2.y

                                cubicTo(
                                    conPointX1, conPointY1,
                                    conPointX2, conPointY2,
                                    p2.x, p2.y
                                )
                            }
                        }
                    }

                    // 3. Draw standard gradient fill underneath path
                    val fillPath = Path().apply {
                        addPath(bezierPath)
                        if (points.isNotEmpty()) {
                            lineTo(points.last().x, height)
                            lineTo(points.first().x, height)
                            close()
                        }
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x352979FF), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    drawPath(
                        path = bezierPath,
                        color = Color(0xFF2979FF),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 4. Highlight points
                    points.forEachIndexed { index, point ->
                        val item = reversedWeights[index]
                        
                        // Outer glowing overlay
                        drawCircle(
                            color = Color(0x602979FF),
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        // Inner ring core
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }

        // --- SUB SECTION: WEIGHT RECORDS HISTORY LIST ---
        Text(
            text = "HISTORICAL LOG ENTRIES",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (weights.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF14191F)), RoundedCornerShape(16.dp))
                    .background(Color(0xFF0C0E11)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = "Empty Weight",
                        tint = Color(0xFF4C5866),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Weight log is currently empty.",
                        color = Color(0xFF4C5866),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(weights) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111418)),
                        border = BorderStroke(1.dp, Color(0xFF21272E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val logDate = try {
                                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val dateStr = inputSdf.parse(entry.date) ?: Date()
                                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(dateStr)
                                } catch (e: Exception) {
                                    entry.date
                                }
                                Text(
                                    text = logDate,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                                Text(
                                    text = "Recorded at $timeStr",
                                    color = Color(0xFF6B7F96),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${entry.weight} kg",
                                    color = Color(0xFF2979FF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                IconButton(
                                    onClick = { viewModel.deleteWeightLog(entry.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF9E2A2B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal record Weight entry dialog
    if (showAddWeightDialog) {
        Dialog(onDismissRequest = { showAddWeightDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141922)),
                border = BorderStroke(1.dp, Color(0xFF1C2C3F)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Log Current Weight",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)", color = Color(0xFF6B7F96)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2979FF),
                            unfocusedBorderColor = Color(0xFF212E3C),
                            focusedLabelColor = Color(0xFF2979FF),
                            unfocusedLabelColor = Color(0xFF6B7F96),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("weight_input_field")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddWeightDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7F96)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF212E3C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                val wt = weightInput.toFloatOrNull()
                                if (wt != null) {
                                    viewModel.recordWeight(wt)
                                    weightInput = ""
                                    showAddWeightDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("weight_submit_dialog_btn")
                        ) {
                            Text("Log Scale")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. GOAL SETTINGS VIEW (TARGETS EDITOR)
// ==========================================
@Composable
fun SettingsView(
    summary: DailySummary,
    viewModel: HealthViewModel
) {
    var stepGoalInput by remember { mutableStateOf(summary.stepGoal.toString()) }
    var calorieGoalInput by remember { mutableStateOf(summary.calorieGoal.toString()) }
    var weightGoalInput by remember { mutableStateOf(summary.weightGoal.toString()) }

    var saveSuccessful by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = "PERSONALIZED GOALS CONFIGURATION",
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Customize your wellness standards. Changing targets will update your progress bar rings immediately.",
                color = Color(0xFF9AA39F),
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1210)),
            border = BorderStroke(1.dp, Color(0xFF1F3F31)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step Goal TextField
                OutlinedTextField(
                    value = stepGoalInput,
                    onValueChange = { stepGoalInput = it },
                    label = { Text("Daily Steps Goal", color = Color(0xFF5E6D66)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = Color(0xFF00E676)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E676),
                        unfocusedBorderColor = Color(0xFF1B2A22),
                        focusedLabelColor = Color(0xFF00E676),
                        unfocusedLabelColor = Color(0xFF5E6D66),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_step_goal")
                )

                // Calorie Goal TextField
                OutlinedTextField(
                    value = calorieGoalInput,
                    onValueChange = { calorieGoalInput = it },
                    label = { Text("Daily Calorie Budget (kcal)", color = Color(0xFF6B584E)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF7600)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF7600),
                        unfocusedBorderColor = Color(0xFF2E1F16),
                        focusedLabelColor = Color(0xFFFF7600),
                        unfocusedLabelColor = Color(0xFF6B584E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_calorie_goal")
                )

                // Weight Goal TextField
                OutlinedTextField(
                    value = weightGoalInput,
                    onValueChange = { weightGoalInput = it },
                    label = { Text("Target Weight Goal (kg)", color = Color(0xFF5B697A)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = Color(0xFF2979FF)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2979FF),
                        unfocusedBorderColor = Color(0xFF1C2835),
                        focusedLabelColor = Color(0xFF2979FF),
                        unfocusedLabelColor = Color(0xFF5B697A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_weight_goal")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val steps = stepGoalInput.toIntOrNull()
                        val calories = calorieGoalInput.toIntOrNull()
                        val weight = weightGoalInput.toFloatOrNull()
                        viewModel.updateGoals(steps, calories, weight)
                        saveSuccessful = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_goals_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Configuration"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE GOALS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = saveSuccessful,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E1C)),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF00E676)
                    )
                    Text(
                        text = "Your health standards have been calibrated and updated successfully!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    saveSuccessful = false
                }
            }
        }
    }
}
  