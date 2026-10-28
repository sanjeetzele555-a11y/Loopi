package com.example.myapplication

import com.example.myapplication.ui.theme.LoopiColors
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val PackNavy = LoopiColors.Navy
private val PackBlue = LoopiColors.Blue
private val PackTeal = LoopiColors.Teal
private val PackBackground = LoopiColors.Background
private val PackMuted = LoopiColors.Muted

class Flow6AppState(context: android.content.Context, private val scope: CoroutineScope) {
    private val dao = LoopiDatabase.getDatabase(context.applicationContext).studyPackDao()
    val packs = mutableStateListOf<StudyPackEntity>()
    val draftTopicNames: SnapshotStateList<String> = mutableStateListOf()
    var packsLoaded by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("")
        private set

    init {
        scope.launch {
            dao.getAllStudyPacks().collectLatest { latest ->
                packs.clear()
                packs.addAll(latest)
                packsLoaded = true
            }
        }
    }

    fun beginCreate() {
        draftTopicNames.clear()
        statusMessage = ""
    }

    fun toggleTopic(title: String) {
        if (draftTopicNames.contains(title)) draftTopicNames.remove(title)
        else draftTopicNames.add(title)
    }

    fun clearStatus() {
        statusMessage = ""
    }

    fun showStatus(message: String) {
        statusMessage = message
    }

    fun createPack(name: String, description: String, onCreated: (Int) -> Unit) {
        val cleanName = name.trim()
        val cleanDescription = description.trim()
        if (cleanName.isEmpty()) {
            statusMessage = "Enter a name for this study pack."
            return
        }
        if (draftTopicNames.isEmpty()) {
            statusMessage = "Select at least one topic."
            return
        }
        val pack = StudyPackEntity(
            name = cleanName,
            description = cleanDescription,
            topicNamesJson = JSONArray(draftTopicNames.toList()).toString()
        )
        scope.launch {
            runCatching { dao.insertStudyPack(pack).toInt() }
                .onSuccess { id ->
                    statusMessage = "Study pack created."
                    onCreated(id)
                }
                .onFailure { statusMessage = "Could not save this study pack. Please try again." }
        }
    }

    fun deletePack(pack: StudyPackEntity) {
        scope.launch {
            runCatching { dao.deleteStudyPackById(pack.id) }
                .onSuccess { statusMessage = "${pack.name} deleted." }
                .onFailure { statusMessage = "Could not delete this study pack." }
        }
    }

    fun importPack(json: String) {
        val imported = runCatching {
            val root = JSONObject(json)
            require(root.optString("type") == "loopi_study_pack") { "This file is not a Loopi study pack." }
            require(root.optInt("version", -1) == 1) { "This study pack version is not supported." }
            val name = root.optString("name").trim()
            require(name.isNotEmpty()) { "The imported pack has no name." }
            val sourceTopics = root.optJSONArray("topics") ?: JSONArray()
            val names = mutableListOf<String>()
            for (i in 0 until sourceTopics.length()) {
                val title = sourceTopics.optString(i).trim()
                if (title.isNotEmpty() && title !in names) names.add(title)
            }
            require(names.isNotEmpty()) { "The imported pack has no topics." }
            StudyPackEntity(
                name = name,
                description = root.optString("description").trim(),
                topicNamesJson = JSONArray(names).toString()
            )
        }.getOrElse {
            statusMessage = it.message ?: "This study pack file could not be read."
            return
        }
        scope.launch {
            runCatching { dao.insertStudyPack(imported).toInt() }
                .onSuccess { statusMessage = "${imported.name} imported." }
                .onFailure { statusMessage = "Could not save the imported study pack." }
        }
    }
}

private fun packTopicNames(pack: StudyPackEntity): List<String> = runCatching {
    val array = JSONArray(pack.topicNamesJson)
    List(array.length()) { array.getString(it) }
}.getOrDefault(emptyList())

private fun packJson(pack: StudyPackEntity): String = JSONObject()
    .put("type", "loopi_study_pack")
    .put("version", 1)
    .put("name", pack.name)
    .put("description", pack.description)
    .put("topics", JSONArray(packTopicNames(pack)))
    .put("exportedAt", System.currentTimeMillis())
    .toString(2)

@Composable
fun StudyPacksScreen(navController: NavHostController, flow6State: Flow6AppState) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not open the selected file.")
            }
            result.onSuccess(flow6State::importPack)
                .onFailure { flow6State.showStatus(it.message ?: "Could not read that file.") }
        }
    }
    LaunchedEffect(flow6State.statusMessage) {
        val message = flow6State.statusMessage
        if (message.isNotBlank()) {
            snackbar.showSnackbar(message)
            flow6State.clearStatus()
        }
    }
    Scaffold(
        containerColor = PackBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { LoopiBottomNavigation(navController, selected = "home") }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Flow6Header("Study Packs", onBack = { navController.popBackStack() }, showBack = false)
            Text("Group topics into focused study sets.", color = PackMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f)) {
                    Text("Import pack")
                }
                Button(
                    onClick = {
                        flow6State.beginCreate()
                        navController.navigate("study_pack_create")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PackNavy)
                ) { Text("Create pack") }
            }
            Spacer(Modifier.height(12.dp))
            if (flow6State.packs.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 38.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No study packs yet", fontWeight = FontWeight.Bold, color = PackNavy)
                        Text("Create a pack from your topics or import one shared with you.", color = PackMuted)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(flow6State.packs, key = { it.id }) { pack ->
                        StudyPackCard(pack) { navController.navigate("study_pack_preview/${pack.id}") }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStudyPackScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow6State: Flow6AppState
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var topicsError by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(flow6State.statusMessage) {
        val message = flow6State.statusMessage
        if (message.isNotBlank()) {
            snackbar.showSnackbar(message)
            flow6State.clearStatus()
        }
    }
    Scaffold(containerColor = PackBackground, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Flow6Header("Create Study Pack", onBack = { navController.popBackStack() })
            Text("Choose a clear name, add a short note, then select the topics for this pack.", color = PackMuted)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Pack name *") },
                placeholder = { Text("e.g. Exam revision") },
                singleLine = true,
                isError = nameError,
                supportingText = { if (nameError) Text("Enter a pack name.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("What are you preparing for?") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable {
                topicsError = false
                navController.navigate("study_pack_topics")
            }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Select topics *", fontWeight = FontWeight.SemiBold, color = PackNavy)
                        Text(
                            if (flow6State.draftTopicNames.isEmpty()) "${appState.topics.size} topics available"
                            else flow6State.draftTopicNames.joinToString(", "),
                            color = PackMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (topicsError) Text("Choose at least one topic.", color = MaterialTheme.colorScheme.error)
                    }
                    Text("›", fontSize = 28.sp, color = PackBlue)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    nameError = name.isBlank()
                    topicsError = flow6State.draftTopicNames.isEmpty()
                    if (nameError || topicsError) return@Button
                    flow6State.createPack(name, description) { id ->
                        navController.navigate("study_pack_preview/$id") {
                            popUpTo("study_pack_create") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PackNavy)
            ) { Text("Create study pack", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun SelectStudyPackTopicsScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow6State: Flow6AppState
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleTopics = appState.topics.filter { it.title.contains(query, ignoreCase = true) }
    Scaffold(containerColor = PackBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Flow6Header("Select Topics", onBack = { navController.popBackStack() })
            Text("Select one or more topics. You can return and change your choices before creating the pack.", color = PackMuted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search topics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("${flow6State.draftTopicNames.size} selected", color = PackNavy, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 10.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 14.dp)) {
                items(visibleTopics, key = { it.id }) { topic ->
                    val checked = flow6State.draftTopicNames.contains(topic.title)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { flow6State.toggleTopic(topic.title) }) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = { flow6State.toggleTopic(topic.title) })
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(topic.title, color = PackNavy, fontWeight = FontWeight.SemiBold)
                                Text(topic.description.ifBlank { "${topic.numberOfCards} study cards" }, color = PackMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${topic.numberOfCards} cards", color = PackMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PackNavy)) {
                Text("Done (${flow6State.draftTopicNames.size} selected)")
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun StudyPackPreviewScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow6State: Flow6AppState,
    packId: Int
) {
    val context = LocalContext.current
    val pack = flow6State.packs.firstOrNull { it.id == packId }
    val snackbar = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(flow6State.statusMessage) {
        val message = flow6State.statusMessage
        if (message.isNotBlank()) {
            snackbar.showSnackbar(message)
            flow6State.clearStatus()
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pack != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(packJson(pack).toByteArray(Charsets.UTF_8)) }
                    ?: error("Could not write to the selected location.")
            }
            saved.onSuccess { flow6State.showStatus("${pack.name} exported.") }
                .onFailure { flow6State.showStatus(it.message ?: "Export failed.") }
        }
    }
    if (showDeleteDialog && pack != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete study pack?") },
            text = { Text("${pack.name} will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    flow6State.deletePack(pack)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
    Scaffold(containerColor = PackBackground, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (pack == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Flow6Header("Study Pack", onBack = { navController.popBackStack() })
                Text(if (flow6State.packsLoaded) "This pack is unavailable." else "Loading pack…", color = PackMuted)
            }
        } else {
            val topicNames = packTopicNames(pack)
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Flow6Header("Pack Preview", onBack = { navController.popBackStack() })
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        Text(pack.name, fontSize = 25.sp, color = PackNavy, fontWeight = FontWeight.ExtraBold)
                        if (pack.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(pack.description, color = PackMuted)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("${topicNames.size} topics", color = PackBlue, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                    }
                    items(topicNames) { title ->
                        val topic = appState.topics.firstOrNull { it.title.equals(title, ignoreCase = true) }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = PackNavy, fontWeight = FontWeight.SemiBold)
                                    Text(topic?.let { "${it.numberOfCards} cards · ${it.difficulty}" } ?: "This topic is not on this device yet", color = PackMuted)
                                }
                                if (topic != null) TextButton(onClick = { navController.navigate("topic_detail/${topic.id}") }) { Text("Open") }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Pack actions", color = PackNavy, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { exportLauncher.launch("${pack.name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "Loopi Study Pack" }}.json") }, modifier = Modifier.weight(1f)) { Text("Export") }
                            OutlinedButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, pack.name)
                                    putExtra(Intent.EXTRA_TEXT, packJson(pack))
                                }
                                context.startActivity(Intent.createChooser(send, "Share study pack"))
                            }, modifier = Modifier.weight(1f)) { Text("Share") }
                        }
                        TextButton(onClick = { showDeleteDialog = true }) { Text("Delete pack", color = MaterialTheme.colorScheme.error) }
                    }
                }
                val available = topicNames.mapNotNull { title -> appState.topics.firstOrNull { it.title.equals(title, ignoreCase = true) } }
                Button(
                    onClick = {
                        val topic = available.firstOrNull()
                        if (topic == null) flow6State.showStatus("None of this pack’s topics are available on this device.")
                        else navController.navigate("review_settings/${topic.id}")
                    },
                    enabled = available.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PackNavy)
                ) { Text("Study first available topic", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun StudyPackCard(pack: StudyPackEntity, onClick: () -> Unit) {
    val topicCount = packTopicNames(pack).size
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(LoopiColors.BlueSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text("📚", fontSize = 23.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.name, color = PackNavy, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$topicCount topics", color = PackMuted, fontSize = 12.sp)
                if (pack.description.isNotBlank()) Text(pack.description, color = PackMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
            Text("›", fontSize = 27.sp, color = PackMuted)
        }
    }
}

@Composable
private fun Flow6Header(title: String, onBack: () -> Unit, showBack: Boolean = true) {
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹", fontSize = 30.sp, color = PackNavy) }
        else Spacer(Modifier.width(8.dp))
        Text(title, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PackNavy)
        if (showBack) Spacer(Modifier.width(42.dp)) else Spacer(Modifier.width(8.dp))
    }
}

