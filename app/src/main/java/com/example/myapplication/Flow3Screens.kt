package com.example.myapplication

import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Brush
import androidx.room.withTransaction
import com.example.myapplication.ui.theme.LoopiColors
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val F3Blue = LoopiColors.Navy
private val F3BrightBlue = LoopiColors.Blue
private val F3Green = LoopiColors.Teal
private val F3Red = LoopiColors.Red
private val F3Background = LoopiColors.Background

class Flow3AppState(
    context: Context,
    private val scope: CoroutineScope
) {

    private val database =
        LoopiDatabase.getDatabase(
            context.applicationContext
        )

    private val flashcardDao =
        database.flashcardDao()

    private val topicDao =
        database.topicDao()

    private val resourceDao =
        database.resourceDao()

    val flashcards =
        mutableStateListOf<FlashcardEntity>()

    val resources =
        mutableStateListOf<ResourceEntity>()

    init {

        scope.launch {

            flashcardDao
                .getAllFlashcards()
                .collectLatest {

                    flashcards.clear()

                    flashcards.addAll(it)
                }
        }

        scope.launch {

            resourceDao
                .getAllResources()
                .collectLatest {

                    resources.clear()

                    resources.addAll(it)
                }
        }
    }

    fun flashcardsForTopic(
        topicId: Int
    ): List<FlashcardEntity> {

        return flashcards.filter {
            it.topicId == topicId
        }
    }

    fun resourcesForTopic(
        topicId: Int
    ): List<ResourceEntity> {

        return resources.filter {
            it.topicId == topicId
        }
    }

    fun findFlashcard(
        id: Int
    ): FlashcardEntity? {

        return flashcards.find {
            it.id == id
        }
    }

    fun findResource(
        id: Int
    ): ResourceEntity? {

        return resources.find {
            it.id == id
        }
    }

    fun addFlashcard(
        topicId: Int,
        question: String,
        answer: String
    ) {

        scope.launch {

            flashcardDao.insertFlashcard(
                FlashcardEntity(
                    topicId = topicId,
                    question = question,
                    answer = answer
                )
            )
            refreshTopicCardCount(topicId)
        }
    }

    fun updateFlashcard(
        flashcard: FlashcardEntity
    ) {

        scope.launch {

            flashcardDao.updateFlashcard(
                flashcard.copy(
                    updatedAt =
                        System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteFlashcard(
        id: Int
    ) {

        scope.launch {
            val topicId = flashcardDao.getFlashcardById(id)?.topicId
            flashcardDao
                .deleteFlashcardById(id)
            if (topicId != null) refreshTopicCardCount(topicId)
        }
    }

    /** Saves reviewed PDF questions in one go and keeps the topic's card count in step. */
    fun addGeneratedFlashcards(
        topicId: Int,
        cards: List<GeneratedQuestion>,
        onSaved: () -> Unit
    ) {
        scope.launch {
            database.withTransaction {
                cards.forEach { card ->
                    flashcardDao.insertFlashcard(
                        FlashcardEntity(
                            topicId = topicId,
                            question = card.question.trim(),
                            answer = card.answer.trim(),
                            explanation = card.explanation.trim(),
                            wrongOptions = card.distractors.joinToString(separator = "\n")
                        )
                    )
                }
                refreshTopicCardCount(topicId)
            }
            onSaved()
        }
    }

    private suspend fun refreshTopicCardCount(topicId: Int) {
        val topic = topicDao.getTopicById(topicId) ?: return
        val count = flashcardDao.getFlashcardCountForTopic(topicId)
        topicDao.updateTopic(topic.copy(numberOfCards = count, lastUpdated = System.currentTimeMillis()))
    }

    fun addResource(
        topicId: Int,
        type: String,
        title: String,
        content: String,
        url: String,
        fileUri: String,
        description: String,
        tags: String
    ) {

        scope.launch {

            resourceDao.insertResource(
                ResourceEntity(
                    topicId = topicId,
                    type = type,
                    title = title,
                    content = content,
                    url = url,
                    fileUri = fileUri,
                    description = description,
                    tags = tags
                )
            )
        }
    }

    fun updateResource(
        resource: ResourceEntity
    ) {

        scope.launch {

            resourceDao.updateResource(
                resource.copy(
                    updatedAt =
                        System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteResource(
        id: Int
    ) {

        scope.launch {

            resourceDao
                .deleteResourceById(id)
        }
    }
}

/* =================================================
   3A — TOPIC DETAIL
   ================================================= */

@Composable
fun TopicDetailScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(topicId)

    if (topic == null) {

        Flow3MissingScreen(
            navController,
            "Topic not found"
        )

        return
    }

    val flashcardCount =
        flow3State
            .flashcardsForTopic(topicId)
            .size

    val resourceCount =
        flow3State
            .resourcesForTopic(topicId)
            .size

    Scaffold(
        containerColor =
            F3Background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Flow3BackHeader(
                title = "",
                onBack = {
                    navController
                        .popBackStack()
                }
            )

            TopicIconTile(title = topic.title, category = topic.category, size = 76.dp)

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
                text = topic.title,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold,
                color = F3Blue
            )

            Text(
                text =
                    "${topic.numberOfCards} cards • $resourceCount resources",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
                text =
                    topic.description,

                textAlign =
                    TextAlign.Center,

                color =
                    LoopiColors.Muted
            )

            Spacer(
                Modifier.height(18.dp)
            )

            // Quick facts about the topic, as in wireframe 3A.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TopicStatTile(flashcardCount.toString(), "Flashcards", Modifier.weight(1f))
                TopicStatTile(resourceCount.toString(), "Resources", Modifier.weight(1f))
                TopicStatTile(topic.difficulty.ifBlank { "Medium" }, "Difficulty", Modifier.weight(1f))
                TopicStatTile("${topic.progress}%", "Accuracy", Modifier.weight(1f))
            }

            Spacer(
                Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    navController.navigate(
                        "flashcard_study/$topicId"
                    )
                },
                enabled = flashcardCount > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = F3BrightBlue)
            ) {
                Text("Start Studying", fontWeight = FontWeight.Bold)
            }

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = {
                    navController.navigate(
                        "review_settings/$topicId"
                    )
                },
                enabled = flashcardCount > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Take a Quiz", fontWeight = FontWeight.Bold)
            }

            Spacer(
                Modifier.height(8.dp)
            )

            // Offline PDF -> multiple-choice questions for this topic.
            Surface(
                onClick = {
                    navController.navigate(
                        "pdf_quiz/$topicId"
                    )
                },
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(LoopiColors.Purple, LoopiColors.Blue)),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Create quiz from PDF", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Upload notes, get MCQs (works offline)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    LoopiChevron(Color.White)
                }
            }

            Spacer(
                Modifier.height(16.dp)
            )

            TopicDetailRow(
                icon = "▣",
                title = "Flashcards",
                value =
                    flashcardCount.toString()
            ) {

                navController.navigate(
                    "flashcards/$topicId"
                )
            }

            TopicDetailRow(
                icon = "▤",
                title = "Resources",
                value =
                    resourceCount.toString()
            ) {

                navController.navigate(
                    "resources/$topicId"
                )
            }

            TopicDetailRow(
                icon = "⚙",
                title = "Settings",
                value = ""
            ) {

                navController.navigate(
                    "topic_settings/$topicId"
                )
            }
        }
    }
}

@Composable
private fun TopicStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = F3Blue,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = label,
                color = LoopiColors.Muted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

/* =================================================
   3B — FLASHCARDS TAB
   ================================================= */

@Composable
fun FlashcardsScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(topicId)

    if (topic == null) {

        Flow3MissingScreen(
            navController,
            "Topic not found"
        )

        return
    }

    var search by remember {
        mutableStateOf("")
    }

    val flashcards =
        flow3State
            .flashcardsForTopic(topicId)
            .filter {

                search.isBlank() ||

                        it.question.contains(
                            search,
                            true
                        ) ||

                        it.answer.contains(
                            search,
                            true
                        )
            }

    Scaffold(
        containerColor =
            F3Background,

        floatingActionButton = {

            FloatingActionButton(
                onClick = {

                    navController.navigate(
                        "add_flashcard/$topicId"
                    )
                },

                containerColor =
                    F3BrightBlue,

                contentColor =
                    Color.White
            ) {

                Text(
                    "+",
                    fontSize = 28.sp
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            TopicWorkspaceHeader(
                title =
                    topic.title,

                selected =
                    "Flashcards",

                topicId =
                    topicId,

                navController =
                    navController
            )

            OutlinedTextField(
                value = search,

                onValueChange = {
                    search = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp
                    ),

                placeholder = {

                    Text(
                        "Search flashcards..."
                    )
                },

                leadingIcon = {

                    Text("🔍")
                },

                singleLine = true
            )

            if (
                flashcards.isEmpty()
            ) {

                EmptyState(
                    icon = "🗂️",

                    title =
                        "No flashcards yet",

                    text =
                        "Add flashcards to start reviewing this topic.",

                    buttonText =
                        "+ Add Flashcard"
                ) {

                    navController.navigate(
                        "add_flashcard/$topicId"
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                16.dp
                        ),

                    contentPadding =
                        PaddingValues(
                            top = 8.dp,
                            bottom = 90.dp
                        )
                ) {

                    items(
                        flashcards,
                        key = {
                            it.id
                        }
                    ) { flashcard ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical =
                                        5.dp
                                )
                                .clickable {

                                    navController.navigate(
                                        "edit_flashcard/$topicId/${flashcard.id}"
                                    )
                                },

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color.White
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color(
                                                0xFFFFF2DD
                                            ),
                                            RoundedCornerShape(
                                                10.dp
                                            )
                                        ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text("❓")
                                }

                                Spacer(
                                    Modifier.width(
                                        12.dp
                                    )
                                )

                                Column(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {

                                    Text(
                                        flashcard.question,
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            F3Blue
                                    )

                                    Text(
                                        flashcard.answer,
                                        maxLines = 1,
                                        color =
                                            Color.Gray,
                                        fontSize =
                                            12.sp
                                    )
                                }

                                LoopiChevron()
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =================================================
   3D — SETTINGS
   ================================================= */

@Composable
fun TopicSettingsScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(topicId)

    if (topic == null) {

        Flow3MissingScreen(
            navController,
            "Topic not found"
        )

        return
    }

    var title by remember(topic.id) {
        mutableStateOf(
            topic.title
        )
    }

    var description by remember(topic.id) {
        mutableStateOf(
            topic.description
        )
    }

    var category by remember(topic.id) {
        mutableStateOf(
            topic.category
        )
    }

    var difficulty by remember(topic.id) {
        mutableStateOf(
            topic.difficulty
        )
    }

    var cards by remember(topic.id) {

        mutableStateOf(
            topic.numberOfCards
                .toString()
        )
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    val categories =
        listOf(
            "Computer Science",
            "Information Technology",
            "Engineering",
            "Business",
            "Mathematics",
            "Other"
        )

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        TopicWorkspaceHeader(
            title =
                topic.title,

            selected =
                "Settings",

            topicId =
                topicId,

            navController =
                navController
        )

        Column(
            modifier = Modifier
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
        ) {

            FormLabel(
                "Topic title"
            )

            OutlinedTextField(
                value = title,

                onValueChange = {
                    title = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine =
                    true
            )

            Spacer(
                Modifier.height(12.dp)
            )

            FormLabel(
                "Description"
            )

            OutlinedTextField(
                value =
                    description,

                onValueChange = {
                    description = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(
                Modifier.height(12.dp)
            )

            FormLabel(
                "Category"
            )

            Box(
                Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {

                        categoryExpanded =
                            true
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            category,

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        textAlign =
                            TextAlign.Start
                    )

                    Text("⌄")
                }

                DropdownMenu(
                    expanded =
                        categoryExpanded,

                    onDismissRequest = {

                        categoryExpanded =
                            false
                    }
                ) {

                    categories.forEach {
                            item ->

                        DropdownMenuItem(
                            text = {

                                Text(item)
                            },

                            onClick = {

                                category =
                                    item

                                categoryExpanded =
                                    false
                            }
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            FormLabel(
                "Difficulty level"
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                listOf(
                    "Easy",
                    "Medium",
                    "Hard"
                ).forEach {

                    FilterChip(
                        selected =
                            difficulty == it,

                        onClick = {
                            difficulty = it
                        },

                        label = {

                            Text(it)
                        }
                    )
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            FormLabel(
                "Number of cards"
            )

            OutlinedTextField(
                value = cards,

                onValueChange = {
                        value ->

                    if (
                        value.all {
                            it.isDigit()
                        }
                    ) {
                        cards = value
                    }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                singleLine =
                    true
            )

            Spacer(
                Modifier.height(22.dp)
            )

            Button(
                onClick = {

                    val number =
                        cards.toIntOrNull()

                    if (
                        title.isNotBlank() &&
                        number != null &&
                        number > 0
                    ) {

                        appState.updateTopic(
                            topic.copy(
                                title =
                                    title.trim(),

                                description =
                                    description.trim(),

                                category =
                                    category,

                                difficulty =
                                    difficulty,

                                numberOfCards =
                                    number
                            )
                        )

                        navController.navigate(
                            "topic_detail/$topicId"
                        )
                    }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            F3Blue
                    )
            ) {

                Text(
                    "Save Changes"
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {

                    navController.navigate(
                        "edit_topic/$topicId"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            F3Red
                    )
            ) {

                Text(
                    "Edit / Delete Topic"
                )
            }
        }
    }
}

/* =================================================
   3E–3K FLASHCARD CRUD
   ================================================= */

@Composable
fun AddFlashcardScreen(
    navController: NavHostController,
    flow3State: Flow3AppState,
    topicId: Int
) {

    FlashcardFormScreen(
        title =
            "Add Flashcard",

        initial =
            null,

        buttonText =
            "Save Flashcard",

        onBack = {

            navController
                .popBackStack()
        },

        onSave = {
                question,
                answer ->

            flow3State.addFlashcard(
                topicId,
                question,
                answer
            )

            navController.navigate(
                "flashcard_added/$topicId"
            )
        }
    )
}

@Composable
fun EditFlashcardScreen(
    navController: NavHostController,
    flow3State: Flow3AppState,
    topicId: Int,
    flashcardId: Int
) {

    val flashcard =
        flow3State.findFlashcard(
            flashcardId
        )

    if (
        flashcard == null
    ) {

        Flow3MissingScreen(
            navController,
            "Flashcard not found"
        )

        return
    }

    var showDelete by remember {
        mutableStateOf(false)
    }

    if (showDelete) {

        AlertDialog(
            onDismissRequest = {
                showDelete = false
            },

            icon = {

                Text(
                    "🗑️",
                    fontSize = 34.sp
                )
            },

            title = {

                Text(
                    "Delete Flashcard?"
                )
            },

            text = {

                Text(
                    "Are you sure you want to delete this flashcard?"
                )
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDelete = false
                    }
                ) {

                    Text("Cancel")
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        flow3State
                            .deleteFlashcard(
                                flashcard.id
                            )

                        navController.navigate(
                            "flashcard_deleted/$topicId"
                        )
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                F3Red
                        )
                ) {

                    Text("Delete")
                }
            }
        )
    }

    FlashcardFormScreen(
        title =
            "Edit Flashcard",

        initial =
            flashcard,

        buttonText =
            "Save Changes",

        showDelete =
            true,

        onDelete = {
            showDelete = true
        },

        onBack = {

            navController
                .popBackStack()
        },

        onSave = {
                question,
                answer ->

            flow3State.updateFlashcard(
                flashcard.copy(
                    question =
                        question,

                    answer =
                        answer
                )
            )

            navController.navigate(
                "flashcards/$topicId"
            )
        }
    )
}

@Composable
private fun FlashcardFormScreen(
    title: String,
    initial: FlashcardEntity?,
    buttonText: String,
    showDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onBack: () -> Unit,
    onSave: (
        String,
        String
    ) -> Unit
) {

    var question by remember {

        mutableStateOf(
            initial?.question ?: ""
        )
    }

    var answer by remember {

        mutableStateOf(
            initial?.answer ?: ""
        )
    }

    var questionError by remember {
        mutableStateOf(false)
    }

    var answerError by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp)
    ) {

        Flow3BackHeader(
            title,
            onBack
        )

        FormLabel(
            "Question *"
        )

        OutlinedTextField(
            value = question,

            onValueChange = {

                question = it
                questionError = false
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter question"
                )
            },

            isError =
                questionError
        )

        if (questionError) {

            ErrorText(
                "Question is required"
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        FormLabel(
            "Answer *"
        )

        OutlinedTextField(
            value = answer,

            onValueChange = {

                answer = it
                answerError = false
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),

            placeholder = {

                Text(
                    "Enter answer"
                )
            },

            isError =
                answerError
        )

        if (answerError) {

            ErrorText(
                "Answer is required"
            )
        }

        Spacer(
            Modifier.height(28.dp)
        )

        Button(
            onClick = {

                questionError =
                    question
                        .trim()
                        .isEmpty()

                answerError =
                    answer
                        .trim()
                        .isEmpty()

                if (
                    !questionError &&
                    !answerError
                ) {

                    onSave(
                        question.trim(),
                        answer.trim()
                    )
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        F3BrightBlue
                )
        ) {

            Text(
                buttonText
            )
        }

        if (showDelete) {

            Spacer(
                Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick =
                    onDelete,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            F3Red
                    )
            ) {

                Text(
                    "🗑 Delete"
                )
            }
        }
    }
}

/* =================================================
   RESOURCES
   ================================================= */

@Composable
fun ResourcesScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(topicId)

    if (topic == null) {

        Flow3MissingScreen(
            navController,
            "Topic not found"
        )

        return
    }

    var search by remember {
        mutableStateOf("")
    }

    val resources =
        flow3State
            .resourcesForTopic(
                topicId
            )
            .filter {

                search.isBlank() ||

                        it.title.contains(
                            search,
                            true
                        ) ||

                        it.tags.contains(
                            search,
                            true
                        ) ||

                        it.description.contains(
                            search,
                            true
                        )
            }

    Scaffold(
        containerColor =
            F3Background,

        floatingActionButton = {

            FloatingActionButton(
                onClick = {

                    navController.navigate(
                        "add_resource/$topicId"
                    )
                },

                containerColor =
                    F3BrightBlue,

                contentColor =
                    Color.White
            ) {

                Text(
                    "+",
                    fontSize = 28.sp
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            TopicWorkspaceHeader(
                title =
                    topic.title,

                selected =
                    "Resources",

                topicId =
                    topicId,

                navController =
                    navController
            )

            OutlinedTextField(
                value = search,

                onValueChange = {
                    search = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            16.dp
                    ),

                placeholder = {

                    Text(
                        "Search resources..."
                    )
                },

                leadingIcon = {

                    Text("🔍")
                },

                singleLine = true
            )

            if (
                resources.isEmpty()
            ) {

                EmptyState(
                    icon = "📄",

                    title =
                        "No resources yet",

                    text =
                        "Add notes, links or files to keep your study materials in one place.",

                    buttonText =
                        "+ Add Resource"
                ) {

                    navController.navigate(
                        "add_resource/$topicId"
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                16.dp
                        ),

                    contentPadding =
                        PaddingValues(
                            top = 8.dp,
                            bottom = 90.dp
                        )
                ) {

                    items(
                        resources,
                        key = {
                            it.id
                        }
                    ) { resource ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical =
                                        5.dp
                                )
                                .clickable {

                                    navController.navigate(
                                        "resource_detail/$topicId/${resource.id}"
                                    )
                                },

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color.White
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    resourceIcon(
                                        resource.type
                                    ),
                                    fontSize =
                                        24.sp
                                )

                                Spacer(
                                    Modifier.width(
                                        12.dp
                                    )
                                )

                                Column(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {

                                    Text(
                                        resource.title,
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            F3Blue
                                    )

                                    Text(
                                        resourceSubtitle(
                                            resource
                                        ),
                                        fontSize =
                                            12.sp,
                                        color =
                                            Color.Gray,
                                        maxLines =
                                            1
                                    )

                                    if (
                                        resource.tags
                                            .isNotBlank()
                                    ) {

                                        Text(
                                            resource.tags,
                                            fontSize =
                                                10.sp,
                                            color =
                                                F3BrightBlue
                                        )
                                    }
                                }

                                LoopiChevron()
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =================================================
   ADD RESOURCE
   ================================================= */

@Composable
fun AddResourceScreen(
    navController: NavHostController,
    flow3State: Flow3AppState,
    topicId: Int
) {

    ResourceFormScreen(
        title =
            "Add Resource",

        initial =
            null,

        buttonText =
            "Save Resource",

        onBack = {

            navController
                .popBackStack()
        },

        onSave = {
                type,
                name,
                content,
                url,
                fileUri,
                description,
                tags ->

            flow3State.addResource(
                topicId =
                    topicId,

                type =
                    type,

                title =
                    name,

                content =
                    content,

                url =
                    url,

                fileUri =
                    fileUri,

                description =
                    description,

                tags =
                    tags
            )

            navController.navigate(
                "resource_added/$topicId"
            )
        }
    )
}

/* =================================================
   RESOURCE DETAIL
   ================================================= */

@Composable
fun ResourceDetailScreen(
    navController: NavHostController,
    flow3State: Flow3AppState,
    topicId: Int,
    resourceId: Int
) {

    val resource =
        flow3State.findResource(
            resourceId
        )

    if (
        resource == null
    ) {

        Flow3MissingScreen(
            navController,
            "Resource not found"
        )

        return
    }

    val context =
        LocalContext.current

    var showDelete by remember {
        mutableStateOf(false)
    }

    if (showDelete) {

        AlertDialog(
            onDismissRequest = {
                showDelete = false
            },

            icon = {

                Text(
                    "🗑️",
                    fontSize = 34.sp
                )
            },

            title = {

                Text(
                    "Delete Resource?"
                )
            },

            text = {

                Text(
                    "Are you sure you want to delete this resource?"
                )
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDelete = false
                    }
                ) {

                    Text("Cancel")
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        flow3State
                            .deleteResource(
                                resource.id
                            )

                        navController.navigate(
                            "resource_deleted/$topicId"
                        )
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                F3Red
                        )
                ) {

                    Text(
                        "Delete"
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Flow3BackHeader(
            "Resource Detail"
        ) {

            navController
                .popBackStack()
        }

        Text(
            resourceIcon(
                resource.type
            ),
            fontSize = 55.sp
        )

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            resource.title,
            fontSize = 23.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                F3Blue
        )

        Text(
            "${resource.type} • ${formatDate(resource.updatedAt)}",
            color =
                Color.Gray,
            fontSize =
                12.sp
        )

        Spacer(
            Modifier.height(24.dp)
        )

        when (
            resource.type
        ) {

            "Note" -> {

                DetailSection(
                    "Content",
                    resource.content
                )
            }

            "Link" -> {

                DetailSection(
                    "URL",
                    resource.url
                )

                DetailSection(
                    "Description",
                    resource.description
                )

                Button(
                    onClick = {

                        openUri(
                            context,
                            resource.url,
                            true
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Open Link"
                    )
                }
            }

            "File" -> {

                DetailSection(
                    "File",
                    fileDisplayName(
                        resource.fileUri
                    )
                )

                DetailSection(
                    "Description",
                    resource.description
                )

                Button(
                    onClick = {

                        openUri(
                            context,
                            resource.fileUri,
                            false
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Open File"
                    )
                }
            }
        }

        if (
            resource.tags
                .isNotBlank()
        ) {

            DetailSection(
                "Tags",
                resource.tags
            )
        }

        Spacer(
            Modifier.height(22.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            OutlinedButton(
                onClick = {

                    navController.navigate(
                        "edit_resource/$topicId/${resource.id}"
                    )
                },

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("Edit")
            }

            OutlinedButton(
                onClick = {

                    showDelete = true
                },

                modifier =
                    Modifier.weight(1f),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            F3Red
                    )
            ) {

                Text(
                    "🗑 Delete"
                )
            }
        }
    }
}

/* =================================================
   EDIT RESOURCE
   ================================================= */

@Composable
fun EditResourceScreen(
    navController: NavHostController,
    flow3State: Flow3AppState,
    topicId: Int,
    resourceId: Int
) {

    val resource =
        flow3State.findResource(
            resourceId
        )

    if (
        resource == null
    ) {

        Flow3MissingScreen(
            navController,
            "Resource not found"
        )

        return
    }

    ResourceFormScreen(
        title =
            "Edit Resource",

        initial =
            resource,

        buttonText =
            "Save Changes",

        onBack = {

            navController
                .popBackStack()
        },

        onSave = {
                type,
                name,
                content,
                url,
                fileUri,
                description,
                tags ->

            flow3State.updateResource(
                resource.copy(
                    type =
                        type,

                    title =
                        name,

                    content =
                        content,

                    url =
                        url,

                    fileUri =
                        fileUri,

                    description =
                        description,

                    tags =
                        tags
                )
            )

            navController.navigate(
                "resources/$topicId"
            )
        }
    )
}

@Composable
private fun ResourceFormScreen(
    title: String,
    initial: ResourceEntity?,
    buttonText: String,
    onBack: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {

    val context =
        LocalContext.current

    var type by remember {

        mutableStateOf(
            initial?.type ?: "Note"
        )
    }

    var name by remember {

        mutableStateOf(
            initial?.title ?: ""
        )
    }

    var content by remember {

        mutableStateOf(
            initial?.content ?: ""
        )
    }

    var url by remember {

        mutableStateOf(
            initial?.url ?: ""
        )
    }

    var fileUri by remember {

        mutableStateOf(
            initial?.fileUri ?: ""
        )
    }

    var description by remember {

        mutableStateOf(
            initial?.description ?: ""
        )
    }

    var tags by remember {

        mutableStateOf(
            initial?.tags ?: ""
        )
    }

    var titleError by remember {
        mutableStateOf(false)
    }

    var mainError by remember {
        mutableStateOf(false)
    }

    val fileLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) { uri ->

            if (uri != null) {

                runCatching {

                    context
                        .contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }

                fileUri =
                    uri.toString()

                mainError =
                    false
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp)
    ) {

        Flow3BackHeader(
            title,
            onBack
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            listOf(
                "Note",
                "Link",
                "File"
            ).forEach {

                FilterChip(
                    selected =
                        type == it,

                    onClick = {

                        type = it
                        mainError = false
                    },

                    label = {

                        Text(it)
                    }
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        FormLabel(
            "Title *"
        )

        OutlinedTextField(
            value = name,

            onValueChange = {

                name = it
                titleError = false
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter title"
                )
            },

            isError =
                titleError,

            singleLine =
                true
        )

        if (titleError) {

            ErrorText(
                "Title is required"
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        when (type) {

            "Note" -> {

                FormLabel(
                    "Content *"
                )

                OutlinedTextField(
                    value =
                        content,

                    onValueChange = {

                        content = it
                        mainError = false
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),

                    placeholder = {

                        Text(
                            "Enter your notes..."
                        )
                    },

                    isError =
                        mainError
                )

                if (mainError) {

                    ErrorText(
                        "Content is required"
                    )
                }
            }

            "Link" -> {

                FormLabel(
                    "URL *"
                )

                OutlinedTextField(
                    value = url,

                    onValueChange = {

                        url = it
                        mainError = false
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    placeholder = {

                        Text(
                            "https://..."
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Uri
                        ),

                    isError =
                        mainError,

                    singleLine =
                        true
                )

                if (mainError) {

                    ErrorText(
                        "URL is required"
                    )
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                FormLabel(
                    "Description (optional)"
                )

                OutlinedTextField(
                    value =
                        description,

                    onValueChange = {
                        description = it
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            "File" -> {

                FormLabel(
                    "File *"
                )

                OutlinedButton(
                    onClick = {

                        fileLauncher.launch(
                            arrayOf("*/*")
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        if (
                            fileUri.isBlank()
                        ) {
                            "Choose File"
                        } else {
                            fileDisplayName(
                                fileUri
                            )
                        }
                    )
                }

                if (mainError) {

                    ErrorText(
                        "Please select a file"
                    )
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                FormLabel(
                    "Description (optional)"
                )

                OutlinedTextField(
                    value =
                        description,

                    onValueChange = {
                        description = it
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        FormLabel(
            "Tags (optional)"
        )

        OutlinedTextField(
            value = tags,

            onValueChange = {
                tags = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "processes, scheduling"
                )
            },

            singleLine =
                true
        )

        Spacer(
            Modifier.height(28.dp)
        )

        Button(
            onClick = {

                titleError =
                    name.trim()
                        .isEmpty()

                mainError =
                    when (type) {

                        "Note" ->
                            content
                                .trim()
                                .isEmpty()

                        "Link" ->
                            url
                                .trim()
                                .isEmpty()

                        "File" ->
                            fileUri
                                .isBlank()

                        else ->
                            true
                    }

                if (
                    !titleError &&
                    !mainError
                ) {

                    onSave(
                        type,
                        name.trim(),
                        content.trim(),
                        url.trim(),
                        fileUri,
                        description.trim(),
                        tags.trim()
                    )
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        F3BrightBlue
                )
        ) {

            Text(
                buttonText
            )
        }
    }
}

/* =================================================
   SUCCESS SCREENS
   ================================================= */

@Composable
fun Flow3SuccessScreen(
    navController: NavHostController,
    topicId: Int,
    title: String,
    message: String,
    buttonText: String,
    destination: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            "✓",
            fontSize = 72.sp,
            color = F3Green
        )

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            title,
            fontSize = 26.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                F3Blue
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            message,
            color =
                Color.Gray,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            Modifier.height(28.dp)
        )

        Button(
            onClick = {

                navController.navigate(
                    "$destination/$topicId"
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                buttonText
            )
        }
    }
}

/* =================================================
   COMPONENTS
   ================================================= */

@Composable
private fun TopicWorkspaceHeader(
    title: String,
    selected: String,
    topicId: Int,
    navController: NavHostController
) {

    Flow3BackHeader(
        title = title,

        onBack = {

            navController.navigate(
                "topic_detail/$topicId"
            )
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White
            ),

        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        WorkspaceTab(
            "Flashcards",
            selected == "Flashcards"
        ) {

            navController.navigate(
                "flashcards/$topicId"
            )
        }

        WorkspaceTab(
            "Resources",
            selected == "Resources"
        ) {

            navController.navigate(
                "resources/$topicId"
            )
        }

        WorkspaceTab(
            "Settings",
            selected == "Settings"
        ) {

            navController.navigate(
                "topic_settings/$topicId"
            )
        }
    }

    Spacer(
        Modifier.height(10.dp)
    )
}

@Composable
private fun WorkspaceTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            title,

            color =
                if (selected)
                    F3Blue
                else
                    Color.Gray,

            fontWeight =
                if (selected)
                    FontWeight.Bold
                else
                    FontWeight.Normal,

            fontSize =
                13.sp
        )

        Spacer(
            Modifier.height(5.dp)
        )

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(2.dp)
                .background(
                    if (selected)
                        F3BrightBlue
                    else
                        Color.Transparent
                )
        )
    }
}

@Composable
private fun Flow3BackHeader(
    title: String,
    onBack: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 6.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack
        ) {

            LoopiBackIcon()
        }

        Text(
            text = title,

            modifier =
                Modifier.weight(1f),

            textAlign =
                TextAlign.Center,

            fontWeight =
                FontWeight.Bold,

            color = F3Blue,

            fontSize =
                18.sp
        )

        Spacer(
            Modifier.width(48.dp)
        )
    }
}

@Composable
private fun TopicDetailRow(
    icon: String,
    title: String,
    value: String,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 14.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            icon,
            fontSize = 22.sp,
            color = F3Blue
        )

        Spacer(
            Modifier.width(12.dp)
        )

        Text(
            title,

            modifier =
                Modifier.weight(1f),

            fontWeight =
                FontWeight.SemiBold,

            color =
                F3Blue
        )

        if (value.isNotBlank()) {

            Text(
                value,
                color = Color.Gray
            )
        }

        Spacer(
            Modifier.width(10.dp)
        )

        LoopiChevron()
    }

    HorizontalDivider()
}

@Composable
private fun EmptyState(
    icon: String,
    title: String,
    text: String,
    buttonText: String,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 30.dp,
                vertical = 70.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            icon,
            fontSize = 52.sp
        )

        Text(
            title,
            fontWeight =
                FontWeight.Bold,
            color =
                F3Blue
        )

        Text(
            text,
            color =
                Color.Gray,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Button(
            onClick = onClick
        ) {

            Text(
                buttonText
            )
        }
    }
}

@Composable
private fun Flow3MissingScreen(
    navController: NavHostController,
    text: String
) {

    Column(
        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(text)

        Button(
            onClick = {

                navController
                    .popBackStack()
            }
        ) {

            Text("Back")
        }
    }
}

@Composable
private fun FormLabel(
    text: String
) {

    Text(
        text,
        fontWeight =
            FontWeight.Bold,
        color =
            F3Blue
    )

    Spacer(
        Modifier.height(5.dp)
    )
}

@Composable
private fun ErrorText(
    text: String
) {

    Text(
        "●  $text",
        color = F3Red,
        fontSize = 12.sp
    )
}

@Composable
private fun DetailSection(
    label: String,
    value: String
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            label,
            fontWeight =
                FontWeight.Bold,
            color = F3Blue
        )

        Text(
            value.ifBlank {
                "—"
            }
        )

        Spacer(
            Modifier.height(16.dp)
        )
    }
}

private fun resourceIcon(
    type: String
): String {

    return when (type) {

        "Note" ->
            "📝"

        "Link" ->
            "🔗"

        "File" ->
            "📄"

        else ->
            "📚"
    }
}

private fun resourceSubtitle(
    resource: ResourceEntity
): String {

    return when (
        resource.type
    ) {

        "Note" ->
            resource.content

        "Link" ->
            resource.url

        "File" ->
            fileDisplayName(
                resource.fileUri
            )

        else ->
            ""
    }
}

private fun fileDisplayName(
    uriString: String
): String {

    if (
        uriString.isBlank()
    ) {

        return "No file selected"
    }

    return Uri
        .parse(uriString)
        .lastPathSegment
        ?.substringAfterLast("/")
        ?: "Selected file"
}

private fun formatDate(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
}

private fun openUri(
    context: Context,
    value: String,
    web: Boolean
) {

    if (
        value.isBlank()
    ) {
        return
    }

    val result =

        if (
            web &&
            !value.startsWith(
                "http://"
            ) &&
            !value.startsWith(
                "https://"
            )
        ) {

            "https://$value"

        } else {

            value
        }

    runCatching {

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(result)
            ).apply {

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(
            intent
        )
    }
}
