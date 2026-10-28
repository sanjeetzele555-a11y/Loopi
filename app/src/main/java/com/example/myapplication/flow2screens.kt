package com.example.myapplication

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Style
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.ui.theme.LoopiColors
import android.content.Context
import androidx.room.withTransaction
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Flow2Blue = LoopiColors.Navy
private val Flow2BrightBlue = LoopiColors.Blue
private val Flow2Green = LoopiColors.Teal
private val Flow2Yellow = LoopiColors.Amber
private val Flow2Pink = LoopiColors.Pink
private val Flow2Purple = LoopiColors.Purple
private val Flow2Red = LoopiColors.Red
private val Flow2Background = LoopiColors.Background

data class StudyTopic(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val numberOfCards: Int,
    val targetCardCount: Int = 0,
    val progress: Int = 0,
    val lastUpdated: Long = 0L
)

private data class StarterTopic(
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String
)

internal fun countReviewedCards(availableCards: List<FlashcardEntity>, answers: List<ReviewResultEntity>): Int {
    val reviewedIds = answers.asSequence().map { it.flashcardId }.toSet()
    return availableCards.count { it.id in reviewedIds }
}

internal fun countCardsReviewedSince(availableCards: List<FlashcardEntity>, answers: List<ReviewResultEntity>, startMillis: Long): Int {
    val availableIds = availableCards.mapTo(mutableSetOf()) { it.id }
    return answers.asSequence().filter { it.reviewedAt >= startMillis }.map { it.flashcardId }.filter { it in availableIds }.distinct().count()
}

data class LoopiNotification(
    val icon: String,
    val title: String,
    val message: String,
    val time: String,
    val type: String
)

class LoopiAppState(
    context: Context,
    private val scope: CoroutineScope
) {

    var selectedSort by mutableStateOf("Name (A–Z)")
    var selectedCategory by mutableStateOf("All")
    var selectedDifficulty by mutableStateOf("All")

    val topics = mutableStateListOf<StudyTopic>()

    var message by mutableStateOf<String?>(null)

    private val appContext =
        context.applicationContext

    private val database =
        LoopiDatabase.getDatabase(
            appContext
        )

    private val topicDao =
        database.topicDao()

    private val flashcardDao =
        database.flashcardDao()

    private val resourceDao =
        database.resourceDao()

    private val reviewResultDao =
        database.reviewResultDao()

    private val preferences =
        appContext.getSharedPreferences(
            "loopi_preferences",
            Context.MODE_PRIVATE
        )

    var reviewStreak by mutableStateOf(
        preferences.getInt(
            "review_streak",
            0
        )
    )
        private set

    val notifications =
        listOf(
            LoopiNotification(
                icon = "📘",
                title = "Review due",
                message =
                    "You have cards due for review.",
                time = "5m ago",
                type = "Reminders"
            ),
            LoopiNotification(
                icon = "🔥",
                title =
                    "Keep your streak!",
                message =
                    "Complete a review session today.",
                time = "2h ago",
                type = "Reminders"
            ),
            LoopiNotification(
                icon = "📢",
                title = "New feature",
                message =
                    "Study packs are now available!",
                time = "1d ago",
                type = "Updates"
            ),
            LoopiNotification(
                icon = "📊",
                title = "Weekly summary",
                message =
                    "Check your learning progress.",
                time = "2d ago",
                type = "Updates"
            ),
            LoopiNotification(
                icon = "🎯",
                title =
                    "Weak topic detected",
                message =
                    "Review topics with lower progress.",
                time = "3d ago",
                type = "Reminders"
            )
        )

    init {

        scope.launch {

            val defaultTopicsCreated =
                preferences.getBoolean(
                    "default_topics_created",
                    false
                )

            val topicCount =
                topicDao.getTopicCount()

            if (
                !defaultTopicsCreated &&
                topicCount == 0
            ) {

                createDefaultTopics()

                preferences
                    .edit()
                    .putBoolean(
                        "default_topics_created",
                        true
                    )
                    .apply()
            }

            if (!preferences.getBoolean("starter_content_seeded_v1", false)) {
                runCatching { seedMissingStarterCards() }
                    .onSuccess {
                        preferences.edit().putBoolean("starter_content_seeded_v1", true).apply()
                    }
                    .onFailure {
                        message = "Starter study cards could not be prepared. You can still add your own cards."
                    }
            }

            topicDao
                .getAllTopics()
                .collectLatest {
                        databaseTopics ->

                    topics.clear()

                    topics.addAll(
                        databaseTopics.map {
                            it.toStudyTopic()
                        }
                    )
                }
        }
    }

    private suspend fun createDefaultTopics() {
        val topics = listOf(
            StarterTopic("Operating Systems", "Processes, memory management and scheduling", "Computer Science", "Medium"),
            StarterTopic("Databases", "SQL, tables and database design", "Information Technology", "Medium"),
            StarterTopic("Networks", "Protocols, OSI model and networking", "Information Technology", "Easy"),
            StarterTopic("Algorithms", "Algorithms and problem solving", "Computer Science", "Hard")
        )
        database.withTransaction {
            topics.forEach { seed ->
                val cards = DefaultStudyContent.byTopic[seed.title].orEmpty()
                val topicId = topicDao.insertTopic(
                    TopicEntity(
                        title = seed.title,
                        description = seed.description,
                        category = seed.category,
                        difficulty = seed.difficulty,
                        numberOfCards = cards.size,
                        targetCardCount = cards.size,
                        progress = 0
                    )
                ).toInt()
                cards.forEach { card ->
                    flashcardDao.insertFlashcard(
                        FlashcardEntity(topicId = topicId, question = card.question, answer = card.answer, explanation = card.explanation)
                    )
                }
            }
        }
    }

    private suspend fun seedMissingStarterCards() {
        DefaultStudyContent.byTopic.forEach { (title, cards) ->
            val topic = topicDao.getTopicByTitle(title) ?: return@forEach
            val existingCount = flashcardDao.getFlashcardCountForTopic(topic.id)
            if (existingCount == 0) {
                database.withTransaction {
                    cards.forEach { card ->
                        flashcardDao.insertFlashcard(
                            FlashcardEntity(topicId = topic.id, question = card.question, answer = card.answer, explanation = card.explanation)
                        )
                    }
                }
            }
            val actualCount = flashcardDao.getFlashcardCountForTopic(topic.id)
            val attempts = reviewResultDao.getAnswerCountForTopic(topic.id)
            val correct = reviewResultDao.getCorrectAnswerCountForTopic(topic.id)
            topicDao.updateTopic(topic.copy(
                numberOfCards = actualCount,
                targetCardCount = topic.targetCardCount.takeIf { it > 0 } ?: actualCount,
                progress = if (attempts == 0) 0 else (correct * 100 / attempts),
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun addTopic(
        title: String,
        description: String,
        category: String,
        difficulty: String,
        cards: Int
    ) {

        scope.launch {

            topicDao.insertTopic(
                TopicEntity(
                    title = title,
                    description = description,
                    category = category,
                    difficulty = difficulty,
                    numberOfCards = 0,
                    targetCardCount = cards,
                    progress = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            message =
                "Topic added successfully!"
        }
    }

    fun updateTopic(
        updatedTopic: StudyTopic
    ) {

        scope.launch {

            topicDao.updateTopic(
                TopicEntity(
                    id =
                        updatedTopic.id,
                    title =
                        updatedTopic.title,
                    description =
                        updatedTopic.description,
                    category =
                        updatedTopic.category,
                    difficulty =
                        updatedTopic.difficulty,
                    numberOfCards =
                        updatedTopic.numberOfCards,
                    targetCardCount = updatedTopic.targetCardCount,
                    progress =
                        updatedTopic.progress,
                    lastUpdated =
                        System.currentTimeMillis()
                )
            )

            message =
                "Topic updated successfully!"
        }
    }

    fun deleteTopic(
        id: Int
    ) {

        scope.launch {

            database.withTransaction {
                flashcardDao.deleteFlashcardsForTopic(id)
                resourceDao.deleteResourcesForTopic(id)
                reviewResultDao.clearTopicResults(id)
                topicDao.deleteTopicById(id)
            }

            message =
                "Topic deleted successfully!"
        }
    }

    fun findTopic(
        id: Int
    ): StudyTopic? {

        return topics.find {
            it.id == id
        }
    }

    fun recordReviewCompleted() {

        reviewStreak += 1

        preferences
            .edit()
            .putInt(
                "review_streak",
                reviewStreak
            )
            .apply()
    }
}

private fun TopicEntity
        .toStudyTopic(): StudyTopic {

    return StudyTopic(
        id = id,
        title = title,
        description =
            description,
        category =
            category,
        difficulty =
            difficulty,
        numberOfCards =
            numberOfCards,
        targetCardCount = targetCardCount,
        progress =
            progress,
        lastUpdated =
            lastUpdated
    )
}

/* =========================================================
   2A — HOME / DASHBOARD
   ========================================================= */

@Composable
fun Flow2HomeScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState
) {

    val context = LocalContext.current
    val settings = remember(context) { context.getSharedPreferences("loopi_settings", Context.MODE_PRIVATE) }
    val dailyGoal = settings.getInt("daily_goal", 20)

    val cards = flow4State.allFlashcards.toList()
    val answers = flow4State.allResults.toList()
    val totalCards = cards.size
    val reviewedCards = countReviewedCards(cards, answers)
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val dailyReviewed = countCardsReviewedSince(cards, answers, calendar.timeInMillis)

    val overallProgress =

        if (
            totalCards > 0
        ) {

            (
                    reviewedCards
                        .toFloat() /
                            totalCards
                                .toFloat() *
                            100f
                    )
                .roundToInt()

        } else {

            0
        }

    val topicsDue =
        appState.topics.filter {
            it.progress < 100
        }

    val currentDate =
        remember {

            SimpleDateFormat(
                "EEE, d MMM yyyy",
                Locale.getDefault()
            )
                .format(
                    Date()
                )
        }

    val greeting =
        remember {

            when (
                Calendar
                    .getInstance()
                    .get(
                        Calendar
                            .HOUR_OF_DAY
                    )
            ) {

                in 5..11 ->
                    "Good morning! 👋"

                in 12..16 ->
                    "Good afternoon! 👋"

                else ->
                    "Good evening! 👋"
            }
        }

    Scaffold(
        containerColor =
            Flow2Background,

        bottomBar = {

            LoopiBottomNavigation(
                navController =
                    navController,

                selected =
                    "home"
            )
        }
    ) {
            innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    horizontal =
                        18.dp
                ),

            contentPadding =
                PaddingValues(
                    top = 18.dp,
                    bottom = 24.dp
                )
        ) {

            item {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,

                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {

                    Column {

                        Text(
                            text =
                                "Loopi",

                            fontSize =
                                30.sp,

                            fontWeight =
                                FontWeight
                                    .ExtraBold,

                            color =
                                Flow2Blue
                        )

                        Text(
                            text =
                                greeting,

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight
                                    .Bold,

                            color =
                                Flow2Blue
                        )

                        Text(
                            text =
                                currentDate,

                            fontSize =
                                12.sp,

                            color =
                                Color.Gray
                        )
                    }

                    IconButton(
                        onClick = {

                            navController
                                .navigate(
                                    "notifications"
                                )
                        }
                    ) {

                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Flow2Blue
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    Color.White
                            )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .padding(
                                    18.dp
                                )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {

                            DailyGoalRing(
                                reviewed = dailyReviewed,
                                goal = dailyGoal
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        18.dp
                                    )
                            )

                            Column(
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                            ) {

                                DashboardStat(
                                    icon =
                                        Icons.Rounded.LocalFireDepartment,

                                    tint =
                                        LoopiColors.Amber,

                                    value =
                                        appState
                                            .reviewStreak
                                            .toString(),

                                    label =
                                        "day streak"
                                )

                                DashboardStat(
                                    icon =
                                        Icons.Rounded.EventAvailable,

                                    tint =
                                        LoopiColors.Teal,

                                    value =
                                        topicsDue
                                            .size
                                            .toString(),

                                    label =
                                        "topics due"
                                )

                                DashboardStat(
                                    icon =
                                        Icons.Rounded.Style,

                                    tint =
                                        LoopiColors.Blue,

                                    value =
                                        "$reviewedCards/$totalCards",

                                    label =
                                        "cards seen ($overallProgress%)"
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    18.dp
                                )
                        )

                        Button(
                            onClick = {
                                navController.navigate("review")
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Flow2Blue)
                        ) {
                            Text("Continue review →", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { navController.navigate("study_packs") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Open study packs", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            22.dp
                        )
                )

                Text(
                    text =
                        "Topics due today",

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Flow2Blue
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )
            }

            if (
                topicsDue.isEmpty()
            ) {

                item {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    20.dp
                                ),

                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "✅",

                                fontSize =
                                    30.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )

                            Text(
                                text =
                                    "You're all caught up!",

                                fontWeight =
                                    FontWeight
                                        .Bold,

                                color =
                                    Flow2Blue
                            )

                            Text(
                                text =
                                    "No topics are due right now.",

                                fontSize =
                                    12.sp,

                                color =
                                    Color.Gray
                            )
                        }
                    }
                }

            } else {

                items(
                    items =
                        topicsDue
                            .take(
                                3
                            ),

                    key = {
                        it.id
                    }
                ) {
                        topic ->

                    CompactTopicRow(
                        topic =
                            topic,

                        onClick = {

                            navController
                                .navigate(
                                    "topic_detail/${topic.id}"
                                )
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        navController
                            .navigate(
                                "topics"
                            )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =
                            "View all study topics"
                    )
                }
            }
        }
    }
}

/* =========================================================
   2B — NOTIFICATIONS
   ========================================================= */

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    appState: LoopiAppState
) {

    var selectedTab by
    remember {

        mutableStateOf(
            "All"
        )
    }

    val filteredNotifications =

        if (
            selectedTab ==
            "All"
        ) {

            appState
                .notifications

        } else {

            appState
                .notifications
                .filter {

                    it.type ==
                            selectedTab
                }
        }

    Scaffold(
        topBar = {

            LoopiTopBar(
                title =
                    "Notifications",

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

                horizontalArrangement =
                    Arrangement
                        .spacedBy(
                            8.dp
                        )
            ) {

                listOf(
                    "All",
                    "Reminders",
                    "Updates"
                )
                    .forEach {
                            tab ->

                        FilterChip(
                            selected =
                                selectedTab ==
                                        tab,

                            onClick = {

                                selectedTab =
                                    tab
                            },

                            label = {

                                Text(
                                    tab
                                )
                            }
                        )
                    }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
            ) {

                items(
                    filteredNotifications
                ) {
                        notification ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    18.dp,

                                vertical =
                                    13.dp
                            ),

                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Text(
                            text =
                                notification
                                    .icon,

                            fontSize =
                                26.sp
                        )

                        Spacer(
                            modifier =
                                Modifier
                                    .width(
                                        12.dp
                                    )
                        )

                        Column(
                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
                        ) {

                            Text(
                                text =
                                    notification
                                        .title,

                                fontWeight =
                                    FontWeight
                                        .Bold,

                                color =
                                    Flow2Blue
                            )

                            Text(
                                text =
                                    notification
                                        .message,

                                fontSize =
                                    13.sp,

                                color =
                                    Color.Gray
                            )
                        }

                        Text(
                            text =
                                notification
                                    .time,

                            fontSize =
                                11.sp,

                            color =
                                Color.Gray
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}

/* =========================================================
   2C + 2D — STUDY TOPICS / SEARCH
   ========================================================= */

@Composable
fun StudyTopicsScreen(
    navController: NavHostController,
    appState: LoopiAppState
) {

    var search by
    remember {

        mutableStateOf(
            ""
        )
    }

    val snackbarHostState =
        remember {

            SnackbarHostState()
        }

    val currentMessage =
        appState.message

    LaunchedEffect(
        currentMessage
    ) {

        if (
            currentMessage !=
            null
        ) {

            appState.message =
                null

            snackbarHostState
                .showSnackbar(
                    message =
                        currentMessage
                )
        }
    }

    val filteredTopics =
        appState
            .topics
            .filter {
                    topic ->

                val searchMatch =

                    search
                        .isBlank() ||

                            topic.title
                                .contains(
                                    search,
                                    ignoreCase =
                                        true
                                ) ||

                            topic.description
                                .contains(
                                    search,
                                    ignoreCase =
                                        true
                                ) ||

                            topic.category
                                .contains(
                                    search,
                                    ignoreCase =
                                        true
                                )

                val categoryMatch =

                    appState
                        .selectedCategory ==
                            "All" ||

                            topic.category ==
                            appState
                                .selectedCategory ||

                            (
                                    appState
                                        .selectedCategory ==
                                            "IT" &&

                                            topic.category ==
                                            "Information Technology"
                                    )

                val difficultyMatch =

                    appState
                        .selectedDifficulty ==
                            "All" ||

                            topic.difficulty ==
                            appState
                                .selectedDifficulty

                searchMatch &&
                        categoryMatch &&
                        difficultyMatch
            }
            .let {
                    list ->

                when (
                    appState
                        .selectedSort
                ) {

                    "Name (A–Z)" ->

                        list.sortedBy {

                            it.title
                                .lowercase()
                        }

                    "Name (Z–A)" ->

                        list.sortedByDescending {

                            it.title
                                .lowercase()
                        }

                    "Last updated" ->

                        list.sortedByDescending {

                            it.lastUpdated
                        }

                    "Most cards" ->

                        list.sortedByDescending {

                            it.numberOfCards
                        }

                    "Least cards" ->

                        list.sortedBy {

                            it.numberOfCards
                        }

                    else ->
                        list
                }
            }

    Scaffold(
        containerColor =
            Flow2Background,

        topBar = {

            LoopiTopBar(
                title =
                    "Study Topics",

                onBack =
                    null
            )
        },

        bottomBar = {

            LoopiBottomNavigation(
                navController =
                    navController,

                selected =
                    "topics"
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = {

                    navController
                        .navigate(
                            "add_topic"
                        )
                },

                containerColor =
                    Flow2BrightBlue,

                contentColor =
                    Color.White
            ) {

                Text(
                    text =
                        "+",

                    fontSize =
                        28.sp
                )
            }
        },

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        }
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    horizontal =
                        16.dp
                )
        ) {

            OutlinedTextField(
                value =
                    search,

                onValueChange = {

                    search =
                        it
                },

                modifier =
                    Modifier
                        .fillMaxWidth(),

                placeholder = {

                    Text(
                        text =
                            "Search topics..."
                    )
                },

                leadingIcon = {

                    Text(
                        "🔍"
                    )
                },

                trailingIcon = {

                    if (
                        search
                            .isNotEmpty()
                    ) {

                        Text(
                            text =
                                "×",

                            fontSize =
                                22.sp,

                            modifier =
                                Modifier
                                    .clickable {

                                        search =
                                            ""
                                    }
                        )
                    }
                },

                singleLine =
                    true
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically
            ) {

                Column {

                    Text(
                        text =

                            if (
                                search
                                    .isBlank()
                            ) {

                                "My Topics"

                            } else {

                                "Search Results"
                            },

                        fontWeight =
                            FontWeight
                                .Bold,

                        color =
                            Flow2Blue
                    )

                    Text(
                        text =
                            "${filteredTopics.size} topics",

                        fontSize =
                            12.sp,

                        color =
                            Color.Gray
                    )
                }

                TextButton(
                    onClick = {

                        navController
                            .navigate(
                                "topic_filter"
                            )
                    }
                ) {

                    Text(
                        text =
                            "Filter / Sort"
                    )
                }
            }

            if (
                filteredTopics
                    .isEmpty()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top =
                                80.dp
                        ),

                    horizontalAlignment =
                        Alignment
                            .CenterHorizontally
                ) {

                    Text(
                        text =
                            "🔎",

                        fontSize =
                            48.sp
                    )

                    Text(
                        text =
                            "No topics found",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Try another search or reset your filters.",

                        textAlign =
                            TextAlign.Center,

                        color =
                            Color.Gray
                    )
                }

            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                ) {

                    items(
                        items =
                            filteredTopics,

                        key = {
                            it.id
                        }
                    ) {
                            topic ->

                        FullTopicRow(
                            topic =
                                topic,

                            onClick = {

                                navController
                                    .navigate(
                                        "topic_detail/${topic.id}"
                                    )
                            }
                        )
                    }

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    90.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

/* =========================================================
   2E — FILTER / SORT
   ========================================================= */

@Composable
fun TopicFilterScreen(
    navController: NavHostController,
    appState: LoopiAppState
) {

    var sortBy by
    remember {

        mutableStateOf(
            appState
                .selectedSort
        )
    }

    var category by
    remember {

        mutableStateOf(
            appState
                .selectedCategory
        )
    }

    var difficulty by
    remember {

        mutableStateOf(
            appState
                .selectedDifficulty
        )
    }

    val sortOptions =
        listOf(
            "Name (A–Z)",
            "Name (Z–A)",
            "Last updated",
            "Most cards",
            "Least cards"
        )

    val categories =
        listOf(
            "All",
            "Computer Science",
            "IT",
            "Engineering",
            "Business",
            "Mathematics",
            "Other"
        )

    val difficulties =
        listOf(
            "All",
            "Easy",
            "Medium",
            "Hard"
        )

    Scaffold(
        containerColor =
            Flow2Background,

        topBar = {

            LoopiTopBar(
                title =
                    "Filter & Sort",

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    18.dp
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically
            ) {

                Text(
                    text =
                        "Sort by",

                    fontWeight =
                        FontWeight
                            .Bold,

                    color =
                        Flow2Blue
                )

                TextButton(
                    onClick = {

                        sortBy =
                            "Name (A–Z)"

                        category =
                            "All"

                        difficulty =
                            "All"
                    }
                ) {

                    Text(
                        text =
                            "Reset"
                    )
                }
            }

            sortOptions
                .forEach {
                        option ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                sortBy =
                                    option
                            },

                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                sortBy ==
                                        option,

                            onClick = {

                                sortBy =
                                    option
                            }
                        )

                        Text(
                            option
                        )
                    }
                }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "Filter by category",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            FlowChipRow(
                values =
                    categories,

                selected =
                    category,

                onSelected = {

                    category =
                        it
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "Filter by difficulty",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            FlowChipRow(
                values =
                    difficulties,

                selected =
                    difficulty,

                onSelected = {

                    difficulty =
                        it
                }
            )

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(
                onClick = {

                    appState
                        .selectedSort =
                        sortBy

                    appState
                        .selectedCategory =
                        category

                    appState
                        .selectedDifficulty =
                        difficulty

                    navController
                        .popBackStack()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Flow2BrightBlue
                        )
            ) {

                Text(
                    text =
                        "Apply Filters",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* =========================================================
   2F — ADD TOPIC
   ========================================================= */

@Composable
fun AddTopicScreen(
    navController: NavHostController,
    appState: LoopiAppState
) {

    TopicFormScreen(
        screenTitle =
            "Add New Topic",

        initialTopic =
            null,

        buttonText =
            "Save Topic",

        navController =
            navController,

        onSave = {
                title,
                description,
                category,
                difficulty,
                cards ->

            appState.addTopic(
                title =
                    title,

                description =
                    description,

                category =
                    category,

                difficulty =
                    difficulty,

                cards =
                    cards
            )

            navController
                .navigate(
                    "topics"
                ) {

                    popUpTo(
                        "topics"
                    ) {

                        inclusive =
                            true
                    }
                }
        }
    )
}

/* =========================================================
   2J–2M — EDIT / DELETE TOPIC
   ========================================================= */

@Composable
fun EditTopicScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    topicId: Int
) {

    val topic =
        appState
            .findTopic(
                topicId
            )

    if (
        topic == null
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize(),

            horizontalAlignment =
                Alignment
                    .CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =
                    "Topic not found"
            )

            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )

            Button(
                onClick = {

                    navController
                        .popBackStack()
                }
            ) {

                Text(
                    text =
                        "Back"
                )
            }
        }

        return
    }

    var showDeleteDialog by
    remember {

        mutableStateOf(
            false
        )
    }

    if (
        showDeleteDialog
    ) {

        AlertDialog(
            onDismissRequest = {

                showDeleteDialog =
                    false
            },

            icon = {

                Text(
                    text =
                        "🗑️",

                    fontSize =
                        34.sp
                )
            },

            title = {

                Text(
                    text =
                        "Delete Topic?"
                )
            },

            text = {

                Text(
                    text =
                        "Are you sure you want to delete “${topic.title}”? " +
                                "This action cannot be undone.",

                    textAlign =
                        TextAlign.Center
                )
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Cancel"
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        appState
                            .deleteTopic(
                                topic.id
                            )

                        navController
                            .navigate(
                                "topics"
                            ) {

                                popUpTo(
                                    "topics"
                                ) {

                                    inclusive =
                                        true
                                }
                            }
                    },

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    Flow2Red
                            )
                ) {

                    Text(
                        text =
                            "Delete"
                    )
                }
            }
        )
    }

    TopicFormScreen(
        screenTitle =
            "Edit Topic",

        initialTopic =
            topic,

        buttonText =
            "Save Changes",

        navController =
            navController,

        showDelete =
            true,

        onDelete = {

            showDeleteDialog =
                true
        },

        onSave = {
                title,
                description,
                category,
                difficulty,
                cards ->

            appState
                .updateTopic(
                    topic.copy(
                        title =
                            title,

                        description =
                            description,

                        category =
                            category,

                        difficulty =
                            difficulty,

                        targetCardCount =
                            cards
                    )
                )

            navController
                .navigate(
                    "topics"
                ) {

                    popUpTo(
                        "topics"
                    ) {

                        inclusive =
                            true
                    }
                }
        }
    )
}

/* =========================================================
   ADD / EDIT FORM
   ========================================================= */

@Composable
private fun TopicFormScreen(
    screenTitle: String,
    initialTopic: StudyTopic?,
    buttonText: String,
    navController: NavHostController,
    showDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onSave: (
        String,
        String,
        String,
        String,
        Int
    ) -> Unit
) {

    var topicTitle by
    remember {

        mutableStateOf(
            initialTopic
                ?.title
                ?: ""
        )
    }

    var description by
    remember {

        mutableStateOf(
            initialTopic
                ?.description
                ?: ""
        )
    }

    var category by
    remember {

        mutableStateOf(
            initialTopic
                ?.category
                ?: ""
        )
    }

    var difficulty by
    remember {

        mutableStateOf(
            initialTopic
                ?.difficulty
                ?: "Medium"
        )
    }

    var cardsText by
    remember {

        mutableStateOf(
            initialTopic
                ?.targetCardCount
                ?.toString()
                ?: ""
        )
    }

    var titleError by
    remember {

        mutableStateOf(
            false
        )
    }

    var descriptionError by
    remember {

        mutableStateOf(
            false
        )
    }

    var categoryError by
    remember {

        mutableStateOf(
            false
        )
    }

    var cardsError by
    remember {

        mutableStateOf(
            false
        )
    }

    var categoryExpanded by
    remember {

        mutableStateOf(
            false
        )
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

    Scaffold(
        containerColor =
            Flow2Background,

        topBar = {

            LoopiTopBar(
                title =
                    screenTitle,

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal =
                        18.dp
                )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "Topic title *",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            OutlinedTextField(
                value =
                    topicTitle,

                onValueChange = {

                    topicTitle =
                        it

                    titleError =
                        false
                },

                modifier =
                    Modifier
                        .fillMaxWidth(),

                placeholder = {

                    Text(
                        text =
                            "Enter topic title"
                    )
                },

                isError =
                    titleError,

                singleLine =
                    true
            )

            if (
                titleError
            ) {

                Flow2ErrorMessage(
                    text =
                        "Title is required"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Description *",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            OutlinedTextField(
                value =
                    description,

                onValueChange = {

                    description =
                        it

                    descriptionError =
                        false
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        110.dp
                    ),

                placeholder = {

                    Text(
                        text =
                            "Enter short description..."
                    )
                },

                isError =
                    descriptionError
            )

            if (
                descriptionError
            ) {

                Flow2ErrorMessage(
                    text =
                        "Description is required"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Category *",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {

                        categoryExpanded =
                            true
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =

                            if (
                                category
                                    .isBlank()
                            ) {

                                "Select category"

                            } else {

                                category
                            },

                        modifier =
                            Modifier
                                .weight(
                                    1f
                                ),

                        textAlign =
                            TextAlign.Start,

                        color =

                            if (
                                categoryError
                            ) {

                                Flow2Red

                            } else {

                                Flow2Blue
                            }
                    )

                    Text(
                        text =
                            "⌄"
                    )
                }

                DropdownMenu(
                    expanded =
                        categoryExpanded,

                    onDismissRequest = {

                        categoryExpanded =
                            false
                    }
                ) {

                    categories
                        .forEach {
                                item ->

                            DropdownMenuItem(
                                text = {

                                    Text(
                                        text =
                                            item
                                    )
                                },

                                onClick = {

                                    category =
                                        item

                                    categoryError =
                                        false

                                    categoryExpanded =
                                        false
                                }
                            )
                        }
                }
            }

            if (
                categoryError
            ) {

                Flow2ErrorMessage(
                    text =
                        "Please select a category"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Difficulty level",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .spacedBy(
                            8.dp
                        )
            ) {

                listOf(
                    "Easy",
                    "Medium",
                    "Hard"
                )
                    .forEach {
                            value ->

                        FilterChip(
                            selected =
                                difficulty ==
                                        value,

                            onClick = {

                                difficulty =
                                    value
                            },

                            label = {

                                Text(
                                    text =
                                        value
                                )
                            }
                        )
                    }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Number of cards *",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                verticalAlignment =
                    Alignment
                        .CenterVertically
            ) {

                OutlinedTextField(
                    value =
                        cardsText,

                    onValueChange = {
                            value ->

                        if (
                            value.all {
                                it.isDigit()
                            }
                        ) {

                            cardsText =
                                value

                            cardsError =
                                false
                        }
                    },

                    modifier =
                        Modifier
                            .weight(
                                1f
                            ),

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType
                                    .Number
                        ),

                    singleLine =
                        true,

                    placeholder = {

                        Text(
                            text =
                                "20"
                        )
                    },

                    isError =
                        cardsError
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        val current =
                            cardsText
                                .toIntOrNull()
                                ?: 1

                        cardsText =
                            (
                                    current -
                                            1
                                    )
                                .coerceAtLeast(
                                    1
                                )
                                .toString()

                        cardsError =
                            false
                    },

                    contentPadding =
                        PaddingValues(
                            horizontal =
                                12.dp
                        )
                ) {

                    Text(
                        text =
                            "−",

                        fontSize =
                            20.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        val current =
                            cardsText
                                .toIntOrNull()
                                ?: 0

                        cardsText =
                            (
                                    current +
                                            1
                                    )
                                .coerceAtMost(
                                    999
                                )
                                .toString()

                        cardsError =
                            false
                    },

                    contentPadding =
                        PaddingValues(
                            horizontal =
                                12.dp
                        )
                ) {

                    Text(
                        text =
                            "+",

                        fontSize =
                            20.sp
                    )
                }
            }

            if (
                cardsError
            ) {

                Flow2ErrorMessage(
                    text =
                        "Number of cards is required"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )

            if (
                showDelete
            ) {

                OutlinedButton(
                    onClick =
                        onDelete,

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    Flow2Red
                            )
                ) {

                    Text(
                        text =
                            "🗑 Delete Topic"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )
            }

            Button(
                onClick = {

                    titleError =
                        topicTitle
                            .trim()
                            .isEmpty()

                    descriptionError =
                        description
                            .trim()
                            .isEmpty()

                    categoryError =
                        category
                            .isBlank()

                    val cards =
                        cardsText
                            .toIntOrNull()

                    cardsError =
                        cards == null ||
                                cards <= 0

                    if (
                        !titleError &&
                        !descriptionError &&
                        !categoryError &&
                        !cardsError &&
                        cards != null
                    ) {

                        onSave(
                            topicTitle
                                .trim(),

                            description
                                .trim(),

                            category,

                            difficulty,

                            cards
                        )
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Flow2Blue
                        )
            ) {

                Text(
                    text =
                        buttonText,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )
        }
    }
}

/* =========================================================
   SMALL UI COMPONENTS
   ========================================================= */

@Composable
private fun DashboardStat(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String
) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(tint.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = value, fontWeight = FontWeight.Bold, color = Flow2Blue, fontSize = 16.sp)
            Text(text = label, fontSize = 11.sp, color = LoopiColors.Muted)
        }
    }
}

/** Today's reviews against the daily goal, drawn as a ring. */
@Composable
private fun DailyGoalRing(
    reviewed: Int,
    goal: Int
) {
    val fraction = if (goal > 0) (reviewed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(900), label = "goal")
    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            drawArc(
                color = LoopiColors.Track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(LoopiColors.Teal, LoopiColors.Blue, LoopiColors.Teal)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$reviewed/$goal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Flow2Blue)
            Text("today's goal", fontSize = 10.sp, color = LoopiColors.Muted)
            Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LoopiColors.Teal)
        }
    }
}

@Composable
private fun CompactTopicRow(
    topic: StudyTopic,
    onClick: () -> Unit
) {

    val reviewedCards =
        (
                topic.numberOfCards *
                        (
                                topic.progress
                                    .coerceIn(
                                        0,
                                        100
                                    ) /
                                        100f
                                )
                )
            .roundToInt()

    val cardsDue =
        (
                topic.numberOfCards -
                        reviewedCards
                )
            .coerceAtLeast(
                0
            )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                onClick()
            }
            .padding(
                vertical =
                    8.dp
            ),

        verticalAlignment =
            Alignment
                .CenterVertically
    ) {

        TopicIconTile(title = topic.title, category = topic.category, size = 40.dp)

        Spacer(
            modifier =
                Modifier.width(
                    12.dp
                )
        )

        Column(
            modifier =
                Modifier
                    .weight(
                        1f
                    )
        ) {

            Text(
                text =
                    topic.title,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Flow2Blue
            )

            Text(
                text =
                    "$cardsDue cards due",

                fontSize =
                    12.sp,

                color =
                    Color.Gray
            )
        }

        LoopiChevron()
    }
}

@Composable
private fun FullTopicRow(
    topic: StudyTopic,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical =
                    6.dp
            )
            .clickable {

                onClick()
            },

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                ),

        shape =
            RoundedCornerShape(
                14.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),

            verticalAlignment =
                Alignment
                    .CenterVertically
        ) {

            TopicIconTile(title = topic.title, category = topic.category, size = 46.dp)

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
            ) {

                Text(
                    text =
                        topic.title,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Flow2Blue
                )

                Text(
                    text =
                        "${topic.numberOfCards} cards${if (topic.targetCardCount > 0) " · goal ${topic.targetCardCount}" else ""}",

                    fontSize =
                        12.sp,

                    color =
                        Color.Gray
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                LoopiProgressBar(
                    progress = {

                        topic.progress
                            .coerceIn(
                                0,
                                100
                            ) /
                                100f
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            6.dp
                        ),

                    color =
                        topicColor(
                            topic.category
                        ),

                    trackColor =
                        Color(
                            0xFFE8EDF5
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {

                Text(
                    text =
                        "${topic.progress.coerceIn(0, 100)}%",

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Flow2Blue
                )

                LoopiChevron()
            }
        }
    }
}

@Composable
private fun LoopiTopBar(
    title: String,
    onBack: (() -> Unit)?
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White
            ).statusBarsPadding()
            .padding(
                horizontal =
                    10.dp,

                vertical =
                    10.dp
            ),

        verticalAlignment =
            Alignment
                .CenterVertically
    ) {

        if (
            onBack != null
        ) {

            IconButton(
                onClick =
                    onBack
            ) {

                LoopiBackIcon()
            }

        } else {

            Spacer(
                modifier =
                    Modifier.width(
                        48.dp
                    )
            )
        }

        Text(
            text =
                title,

            modifier =
                Modifier
                    .weight(
                        1f
                    ),

            textAlign =
                TextAlign.Center,

            fontSize =
                20.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                Flow2Blue
        )

        Spacer(
            modifier =
                Modifier.width(
                    48.dp
                )
        )
    }
}

@Composable
private fun FlowChipRow(
    values: List<String>,
    selected: String,
    onSelected:
        (String) -> Unit
) {

    Column {

        values
            .chunked(
                3
            )
            .forEach {
                    rowValues ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement
                            .spacedBy(
                                7.dp
                            )
                ) {

                    rowValues
                        .forEach {
                                value ->

                            FilterChip(
                                selected =
                                    value ==
                                            selected,

                                onClick = {

                                    onSelected(
                                        value
                                    )
                                },

                                label = {

                                    Text(
                                        text =
                                            value,

                                        fontSize =
                                            11.sp
                                    )
                                }
                            )
                        }
                }
            }
    }
}

@Composable
private fun Flow2ErrorMessage(
    text: String
) {

    Text(
        text =
            "●  $text",

        color =
            Flow2Red,

        fontSize =
            12.sp,

        modifier =
            Modifier.padding(
                top =
                    4.dp
            )
    )
}

private fun topicEmoji(
    category: String
): String {

    return when (
        category
    ) {

        "Computer Science" ->

            "💻"

        "Information Technology",
        "IT" ->

            "🌐"

        "Engineering" ->

            "⚙️"

        "Business" ->

            "💼"

        "Mathematics" ->

            "➗"

        else ->

            "📚"
    }
}

private fun topicColor(
    category: String
): Color {

    return when (
        category
    ) {

        "Computer Science" ->

            Flow2Green

        "Information Technology",
        "IT" ->

            Color(
                0xFF36B6C9
            )

        "Engineering" ->

            Flow2Yellow

        "Business" ->

            Flow2Purple

        "Mathematics" ->

            Flow2Pink

        else ->

            Flow2BrightBlue
    }
}



