package com.example.myapplication

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ProgressNavy = Color(0xFF123B8F)
private val ProgressBlue = Color(0xFF0878F9)
private val ProgressTeal = Color(0xFF18B89A)
private val ProgressRed = Color(0xFFEF4444)
private val ProgressBackground = Color(0xFFF5F8FD)
private val ProgressMuted = Color(0xFF71809F)

internal enum class ProgressPeriod(val label: String, val days: Int?) {
    Week("Week", 7), Month("Month", 30), AllTime("All time", null)
}

internal fun calculateAccuracy(results: List<ReviewResultEntity>): Int? =
    if (results.isEmpty()) null else (results.count { it.isCorrect } * 100.0 / results.size).roundToInt()

private fun dayStart(now: Long, daysAgo: Int): Long = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, -daysAgo)
}.timeInMillis

internal data class AccuracyBucket(val label: String, val percent: Int, val answers: Int)

internal fun averageResponseSeconds(results: List<ReviewResultEntity>): Double? =
    results.map { it.responseTimeMillis }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.div(1000.0)

internal fun accuracyBuckets(results: List<ReviewResultEntity>, period: ProgressPeriod, now: Long): List<AccuracyBucket> {
    if (results.isEmpty()) return emptyList()
    if (period == ProgressPeriod.AllTime) {
        val recent = results.sortedBy { it.reviewedAt }.takeLast(60).chunked(10)
        return recent.mapIndexed { index, batch ->
            AccuracyBucket("${index * 10 + 1}–${index * 10 + batch.size}", calculateAccuracy(batch) ?: 0, batch.size)
        }
    }
    val days = period.days ?: 7
    val start = dayStart(now, days - 1)
    val relevant = results.filter { it.reviewedAt >= start }
    val bucketCount = if (period == ProgressPeriod.Week) 7 else 6
    val width = if (period == ProgressPeriod.Week) 1 else 5
    return (0 until bucketCount).map { index ->
        val from = start + index * width * 86_400_000L
        val until = from + width * 86_400_000L
        val batch = relevant.filter { it.reviewedAt >= from && it.reviewedAt < until }
        val label = if (period == ProgressPeriod.Week) {
            SimpleDateFormat("EE", Locale.getDefault()).format(Date(from))
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(from))
        }
        AccuracyBucket(label, calculateAccuracy(batch) ?: 0, batch.size)
    }
}

@Composable
fun Flow5ProgressScreen(navController: NavHostController, appState: LoopiAppState, flow4State: Flow4AppState) {
    var period by remember { mutableStateOf(ProgressPeriod.Week) }
    var showWeakOnly by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val allResults = flow4State.allResults.toList()
    val filteredResults = period.days?.let { days -> allResults.filter { it.reviewedAt >= dayStart(now, days - 1) } } ?: allResults
    val buckets = accuracyBuckets(allResults, period, now)
    val correct = filteredResults.count { it.isCorrect }
    val incorrect = filteredResults.size - correct
    val overall = calculateAccuracy(filteredResults)
    val avgSeconds = averageResponseSeconds(filteredResults)
    val topicRows = appState.topics.map { topic ->
        val records = filteredResults.filter { it.topicId == topic.id }
        Triple(topic, records, calculateAccuracy(records))
    }
    val visibleRows = topicRows.filter { !showWeakOnly || (it.third?.let { value -> value < 70 } == true) }
        .sortedWith(compareBy<Triple<StudyTopic, List<ReviewResultEntity>, Int?>> { it.third ?: 101 }.thenBy { it.first.title })

    Scaffold(containerColor = ProgressBackground, bottomBar = { LoopiBottomNavigation(navController, selected = "progress") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ProgressHeader("Your Progress")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressPeriod.entries.forEach { option ->
                        FilterChip(selected = period == option, onClick = { period = option }, label = { Text(option.label) })
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Accuracy per answer", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(14.dp))
                        if (buckets.none { it.answers > 0 }) {
                            Text("Answer review questions to build your accuracy chart.", color = ProgressMuted, modifier = Modifier.padding(vertical = 20.dp))
                        } else {
                            Row(Modifier.fillMaxWidth().height(132.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                                buckets.forEach { bucket ->
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                        Text(if (bucket.answers > 0) "${bucket.percent}%" else "", color = ProgressMuted, fontSize = 10.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.fillMaxWidth().height((bucket.percent.coerceAtLeast(if (bucket.answers > 0) 8 else 2) * 0.85f).dp).background(if (bucket.answers > 0) ProgressBlue else Color(0xFFE6EDF8), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                        Spacer(Modifier.height(5.dp))
                                        Text(bucket.label, color = ProgressMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressMetric("Accuracy", overall?.let { "$it%" } ?: "—", Modifier.weight(1f))
                    ProgressMetric("Correct", correct.toString(), Modifier.weight(1f))
                    ProgressMetric("Incorrect", incorrect.toString(), Modifier.weight(1f))
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Average answer time", color = ProgressMuted, fontSize = 13.sp)
                            Text(avgSeconds?.let { "${it.roundToInt()} sec" } ?: "No timed answers yet", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Topics practiced", color = ProgressMuted, fontSize = 13.sp)
                            Text("${filteredResults.map { it.topicId }.distinct().size} / ${appState.topics.size}", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (showWeakOnly) "Weak Spots" else "Topic performance", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    TextButton(onClick = { showWeakOnly = !showWeakOnly }) { Text(if (showWeakOnly) "All topics" else "Weak spots") }
                }
            }
            if (visibleRows.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text(if (showWeakOnly) "No weak spots in this period. Keep reviewing to maintain your progress." else "No topics are available yet.", color = ProgressMuted, modifier = Modifier.padding(18.dp))
                    }
                }
            } else {
                items(visibleRows, key = { it.first.id }) { (topic, records, topicAccuracy) ->
                    ProgressTopicCard(topic.title, records.size, topicAccuracy, Modifier.fillMaxWidth()) {
                        navController.navigate("progress_topic/${topic.id}")
                    }
                }
            }
            item {
                Text("Progress updates when saved review answers arrive on this device. Previous answers without recorded timing are excluded from average answer time.", color = ProgressMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun Flow5TopicProgressScreen(navController: NavHostController, appState: LoopiAppState, flow4State: Flow4AppState, topicId: Int) {
    val topic = appState.topics.firstOrNull { it.id == topicId }
    val records = flow4State.resultsForTopic(topicId).sortedBy { it.reviewedAt }
    val percent = calculateAccuracy(records)
    val correct = records.count { it.isCorrect }
    val avg = averageResponseSeconds(records)
    Scaffold(containerColor = ProgressBackground, bottomBar = { LoopiBottomNavigation(navController, selected = "progress") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ProgressHeader(topic?.title ?: "Topic progress", onBack = { navController.popBackStack() })
                Text(topic?.description.orEmpty(), color = ProgressMuted)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressMetric("Accuracy", percent?.let { "$it%" } ?: "—", Modifier.weight(1f))
                    ProgressMetric("Correct", correct.toString(), Modifier.weight(1f))
                    ProgressMetric("Incorrect", (records.size - correct).toString(), Modifier.weight(1f))
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Accuracy per answer", fontWeight = FontWeight.Bold, color = ProgressNavy)
                        Spacer(Modifier.height(12.dp))
                        val recent = records.takeLast(10)
                        if (recent.isEmpty()) Text("Practice this topic to see answer-by-answer results.", color = ProgressMuted, modifier = Modifier.padding(vertical = 18.dp))
                        else Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            recent.forEachIndexed { index, result ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Box(Modifier.fillMaxWidth().height(if (result.isCorrect) 112.dp else 35.dp).background(if (result.isCorrect) ProgressTeal else ProgressRed, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                                    Text("${index + 1}", color = ProgressMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                ProgressMetric("Average answer time", avg?.let { "${it.roundToInt()} sec" } ?: "No timed answers yet", Modifier.fillMaxWidth())
            }
            item {
                Text("Recent answers", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
            items(records.takeLast(10).reversed(), key = { it.id }) { record ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(record.question, color = ProgressNavy, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (record.isCorrect) "Correct · ${record.confidence}" else "Incorrect · ${record.confidence}", color = if (record.isCorrect) ProgressTeal else ProgressRed, fontSize = 12.sp)
                    }
                }
            }
            item {
                Button(onClick = { navController.navigate("review_settings/$topicId") }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ProgressNavy)) { Text("Practice this topic") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { navController.navigate("review_practice/$topicId") }, modifier = Modifier.fillMaxWidth()) { Text("Practice weak cards") }
            }
        }
    }
}

@Composable
fun Flow5ProfileScreen(navController: NavHostController, appState: LoopiAppState, flow4State: Flow4AppState) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("loopi_auth", android.content.Context.MODE_PRIVATE) }
    val answers = flow4State.allResults.toList()
    val practiced = answers.map { it.topicId }.distinct().size
    var fullName by remember(preferences) { mutableStateOf(preferences.getString("full_name", "").orEmpty()) }
    var email by remember(preferences) { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var showEditName by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(fullName) }
    var draftEmail by remember { mutableStateOf(email) }
    var draftStudentId by remember { mutableStateOf(preferences.getString("student_id", "").orEmpty()) }
    var draftBio by remember { mutableStateOf(preferences.getString("profile_bio", "").orEmpty()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember(preferences) { mutableStateOf(preferences.getString("profile_image_uri", null)) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            preferences.edit().putString("profile_image_uri", uri.toString()).apply()
            profileImageUri = uri.toString()
        }
    }
    val profileBitmap = remember(profileImageUri) {
        profileImageUri?.let { savedUri ->
            runCatching { context.contentResolver.openInputStream(Uri.parse(savedUri))?.use(BitmapFactory::decodeStream)?.asImageBitmap() }.getOrNull()
        }
    }

    val distinctStudyDays = answers.map { dayStart(it.reviewedAt, 0) }.distinct().sortedDescending()
    val todayStart = dayStart(System.currentTimeMillis(), 0)
    var streak = 0
    var expectedDay = if (distinctStudyDays.firstOrNull() == todayStart) todayStart else todayStart - 86_400_000L
    for (studyDay in distinctStudyDays) {
        if (studyDay == expectedDay) { streak++; expectedDay -= 86_400_000L } else if (studyDay < expectedDay) break
    }

    Scaffold(containerColor = Color.White, bottomBar = { LoopiBottomNavigation(navController, selected = "profile") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Loopi", modifier = Modifier.weight(1f), color = ProgressNavy, fontWeight = FontWeight.ExtraBold, fontSize = 29.sp)
                TextButton(onClick = { showSignOutConfirmation = true }) { Text("⇥", color = ProgressNavy, fontSize = 23.sp) }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.size(106.dp).clickable { imagePicker.launch(arrayOf("image/*")) }) {
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFDCE8FF), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profileBitmap != null) {
                            androidx.compose.foundation.Image(profileBitmap, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                        } else {
                            Text(fullName.trim().firstOrNull()?.uppercase() ?: "L", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 42.sp)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = ProgressBlue, modifier = Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 15.sp) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(fullName.ifBlank { "Loopi learner" }, color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(email, color = ProgressMuted, fontSize = 14.sp)
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF2CF), modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)) {
                Text("$streak-day streak 🔥", modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp), color = Color(0xFF855400), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            ProfileMenuRow("✎", "Edit Profile", onClick = {
                draftName = fullName
                draftEmail = email
                draftStudentId = preferences.getString("student_id", "").orEmpty()
                draftBio = preferences.getString("profile_bio", "").orEmpty()
                nameError = null
                showEditName = true
            })
            ProfileMenuRow("▣", "My Study Packs", onClick = { navController.navigate("study_packs") })
            ProfileMenuRow("⚙", "Settings", onClick = { navController.navigate("settings") })
            ProfileMenuRow("?", "Help & Support", onClick = { navController.navigate("help_support") })
            ProfileMenuRow("⇥", "Log out", destructive = true, onClick = { showSignOutConfirmation = true })
            Spacer(Modifier.height(12.dp))
            Text("${answers.size} saved answers · $practiced of ${appState.topics.size} topics practiced · ${flow4State.allFlashcards.size} cards", color = ProgressMuted, fontSize = 12.sp)
        }
    }

    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Edit profile name") },
            text = {
                Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it; nameError = null },
                        label = { Text("Full name") },
                        singleLine = true,
                        isError = nameError != null
                    )
                    OutlinedTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it; nameError = null },
                        label = { Text("University email") },
                        singleLine = true,
                        isError = nameError != null
                    )
                    OutlinedTextField(
                        value = draftStudentId,
                        onValueChange = { draftStudentId = it },
                        label = { Text("Student ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftBio,
                        onValueChange = { draftBio = it },
                        label = { Text("Bio (optional)") },
                        minLines = 2,
                        maxLines = 4
                    )
                    nameError?.let { Text(it, color = ProgressRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedName = draftName.trim()
                    if (updatedName.isBlank()) {
                        nameError = "Name cannot be empty."
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(draftEmail.trim()).matches()) {
                        nameError = "Enter a valid university email."
                    } else {
                        val updatedEmail = draftEmail.trim().lowercase()
                        preferences.edit()
                            .putString("full_name", updatedName)
                            .putString("email", updatedEmail)
                            .putString("student_id", draftStudentId.trim())
                            .putString("profile_bio", draftBio.trim())
                            .apply()
                        fullName = updatedName
                        email = updatedEmail
                        showEditName = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditName = false }) { Text("Cancel") } }
        )
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign out?") },
            text = { Text("Your study progress will remain saved on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    preferences.edit().putBoolean("logged_in", false).apply()
                    showSignOutConfirmation = false
                    navController.navigate("welcome") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileActivityCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = ProgressMuted, fontSize = 13.sp)
            Text(value, color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text(caption, color = ProgressMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: String, title: String, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) ProgressRed else ProgressNavy
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = color, fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.width(48.dp))
        Text(title, modifier = Modifier.weight(1f), color = color, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        if (!destructive) Text("›", color = ProgressMuted, fontSize = 25.sp)
    }
    HorizontalDivider(color = Color(0xFFE8EDF5))
}

@Composable
fun Flow6SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("loopi_settings", android.content.Context.MODE_PRIVATE) }
    var reminders by remember { mutableStateOf(prefs.getBoolean("study_reminders", true)) }
    var dueAlerts by remember { mutableStateOf(prefs.getBoolean("review_due_alerts", true)) }
    var weakAlerts by remember { mutableStateOf(prefs.getBoolean("weak_topic_alerts", true)) }
    var offlineMode by remember { mutableStateOf(prefs.getBoolean("offline_mode", false)) }
    var dailyGoal by remember { mutableStateOf(prefs.getInt("daily_goal", 20)) }
    var weakOnly by remember { mutableStateOf(prefs.getBoolean("weak_only_default", false)) }

    Scaffold(containerColor = ProgressBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressHeader("Settings", onBack = { navController.popBackStack() })
            Text("Notifications", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            SettingsToggle("Study reminders", "Saved for this device. Reminder scheduling is not enabled in this demo.", reminders) {
                reminders = it; prefs.edit().putBoolean("study_reminders", it).apply()
            }
            SettingsToggle("Review due", "Preference saved locally. No notification scheduler is connected.", dueAlerts) {
                dueAlerts = it; prefs.edit().putBoolean("review_due_alerts", it).apply()
            }
            SettingsToggle("Weak topic alerts", "Preference saved locally. No notification scheduler is connected.", weakAlerts) {
                weakAlerts = it; prefs.edit().putBoolean("weak_topic_alerts", it).apply()
            }
            Text("Study preferences", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily card goal", color = ProgressNavy, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 50, 100).forEach { value ->
                            FilterChip(selected = dailyGoal == value, onClick = { dailyGoal = value; prefs.edit().putInt("daily_goal", value).apply() }, label = { Text("$value") })
                        }
                    }
                    HorizontalDivider()
                    Text("Default review mode", color = ProgressNavy, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !weakOnly, onClick = { weakOnly = false; prefs.edit().putBoolean("weak_only_default", false).apply() })
                        Text("All cards", modifier = Modifier.clickable { weakOnly = false; prefs.edit().putBoolean("weak_only_default", false).apply() })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = weakOnly, onClick = { weakOnly = true; prefs.edit().putBoolean("weak_only_default", true).apply() })
                        Text("Weak cards only", modifier = Modifier.clickable { weakOnly = true; prefs.edit().putBoolean("weak_only_default", true).apply() })
                    }
                }
            }
            Text("App", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            SettingsToggle("Offline mode", "Your study data is already stored on this device; this choice is saved locally.", offlineMode) {
                offlineMode = it; prefs.edit().putBoolean("offline_mode", it).apply()
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Data & storage", color = ProgressNavy, fontWeight = FontWeight.SemiBold)
                    Text("Loopi stores your profile and study data locally on this device. There is no cloud sync in this build.", color = ProgressMuted, fontSize = 13.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { navController.navigate("help_support") }, shape = RoundedCornerShape(16.dp)) {
                Text("Help & Support   ›", modifier = Modifier.padding(16.dp), color = ProgressNavy, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsToggle(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ProgressNavy, fontWeight = FontWeight.SemiBold)
                Text(detail, color = ProgressMuted, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun Flow6HelpSupportScreen(navController: NavHostController) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val questions = listOf(
        "Where is my study data saved?" to "Your profile, topics, resources, and review history are stored on this device. This version does not sync to a server.",
        "How are progress charts calculated?" to "Charts use saved review answers and their recorded response times. No answers means the chart has no accuracy value yet.",
        "Can I use Loopi offline?" to "Yes. The study library and saved progress are available offline on this device."
    )
    Scaffold(containerColor = ProgressBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressHeader("Help & Support", onBack = { navController.popBackStack() })
            Text("Frequently asked questions", color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            questions.forEach { (question, answer) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { expanded = if (expanded == question) null else question }, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(question, color = ProgressNavy, fontWeight = FontWeight.SemiBold)
                        if (expanded == question) Text(answer, color = ProgressMuted, fontSize = 13.sp)
                    }
                }
            }
            Button(onClick = { submitted = false; showFeedback = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ProgressNavy), shape = RoundedCornerShape(15.dp)) {
                Text("Send feedback")
            }
            Text("Feedback is not sent or retained after leaving this screen; no support server is connected.", color = ProgressMuted, fontSize = 12.sp)
        }
    }
    if (showFeedback) AlertDialog(
        onDismissRequest = { showFeedback = false },
        title = { Text(if (submitted) "Feedback saved" else "Send feedback") },
        text = {
            if (submitted) Text("Your feedback was saved for this session. This demo is not connected to a support service.")
            else OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("Your feedback") }, minLines = 3)
        },
        confirmButton = { TextButton(onClick = { if (submitted) showFeedback = false else if (feedback.isNotBlank()) submitted = true }) { Text(if (submitted) "Done" else "Save") } },
        dismissButton = { if (!submitted) TextButton(onClick = { showFeedback = false }) { Text("Cancel") } }
    )
}
@Composable
private fun ProgressHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹", color = ProgressNavy, fontSize = 30.sp) }
        Text(title, modifier = Modifier.weight(1f), color = ProgressNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Text(label, color = ProgressMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = ProgressNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProgressTopicCard(title: String, answers: Int, percent: Int?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = ProgressNavy, fontWeight = FontWeight.Bold)
                    Text(if (answers == 0) "Not practiced yet" else "$answers answers", color = ProgressMuted, fontSize = 12.sp)
                }
                Text(percent?.let { "$it%" } ?: "—", color = if (percent != null && percent < 70) ProgressRed else ProgressNavy, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { (percent ?: 0) / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = if (percent != null && percent < 70) ProgressRed else ProgressTeal, trackColor = Color(0xFFE6EDF8))
        }
    }
}

