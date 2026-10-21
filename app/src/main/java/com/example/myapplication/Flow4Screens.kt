package com.example.myapplication

import android.content.Context
import android.os.SystemClock
import androidx.room.withTransaction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ReviewBlue =
    Color(0xFF123B8F)

private val ReviewBrightBlue =
    Color(0xFF0878F9)

private val ReviewGreen =
    Color(0xFF18B89A)

private val ReviewLightGreen =
    Color(0xFFE7F8F3)

private val ReviewRed =
    Color(0xFFEF4444)

private val ReviewLightRed =
    Color(0xFFFFEBEE)

private val ReviewYellow =
    Color(0xFFFFBC32)

private val ReviewLightYellow =
    Color(0xFFFFF6D9)

private val ReviewPurple =
    Color(0xFF8B5CF6)

private val ReviewBackground =
    Color(0xFFF8FAFF)

private val ReviewBorder =
    Color(0xFFE1E7F0)


/* =========================================================
   SESSION ANSWER MODEL
   ========================================================= */

data class ReviewSessionAnswer(

    val flashcardId: Int,

    val question: String,

    val correctAnswer: String,

    val userAnswer: String,

    val isCorrect: Boolean,

    val confidence: String
)


/* =========================================================
   FLOW 4 STATE
   ========================================================= */

class Flow4AppState(
    context: Context,
    private val scope: CoroutineScope
) {

    private val database =
        LoopiDatabase.getDatabase(
            context.applicationContext
        )

    private val flashcardDao =
        database.flashcardDao()

    private val reviewDao =
        database.reviewResultDao()

    private val topicDao =
        database.topicDao()

    val allFlashcards =
        mutableStateListOf<FlashcardEntity>()

    val allResults =
        mutableStateListOf<ReviewResultEntity>()

    val sessionAnswers =
        mutableStateListOf<ReviewSessionAnswer>()

    var selectedTopicId by
    mutableStateOf<Int?>(
        null
    )

    var questionCount by
    mutableStateOf(
        10
    )

    var shuffleQuestions by
    mutableStateOf(
        true
    )

    var weakCardsOnly by
    mutableStateOf(
        false
    )

    var sessionCards by
    mutableStateOf<
            List<FlashcardEntity>
            >(
        emptyList()
    )

        private set

    var currentQuestionIndex by
    mutableStateOf(
        0
    )

        private set

    var selectedAnswer by
    mutableStateOf<String?>(
        null
    )

        private set

    var selectedConfidence by
    mutableStateOf<String?>(
        null
    )

        private set

    var lastAnswer by
    mutableStateOf<
            ReviewSessionAnswer?
            >(
        null
    )

        private set

    var sessionStartTime by
    mutableStateOf(
        0L
    )

        private set

    var practiceMode by
    mutableStateOf(
        false
    )

        private set

    var completionRecorded by
    mutableStateOf(
        false
    )

        private set

    private var answerCommitted =
        false

    private var questionStartedRealtime = 0L


    init {

        scope.launch {

            flashcardDao
                .getAllFlashcards()
                .collectLatest {
                        cards ->

                    allFlashcards.clear()

                    allFlashcards.addAll(
                        cards
                    )
                }
        }

        scope.launch {

            reviewDao
                .getAllResults()
                .collectLatest {
                        results ->

                    allResults.clear()

                    allResults.addAll(
                        results
                    )
                }
        }
    }


    fun flashcardsForTopic(
        topicId: Int
    ): List<FlashcardEntity> {

        return allFlashcards
            .filter {

                it.topicId ==
                        topicId
            }
    }


    fun resultsForTopic(
        topicId: Int
    ): List<ReviewResultEntity> {

        return allResults
            .filter {

                it.topicId ==
                        topicId
            }
    }


    fun selectTopic(
        topicId: Int
    ) {

        selectedTopicId =
            topicId
    }


    fun increaseQuestionCount() {

        val topicId =
            selectedTopicId
                ?: return

        val maxQuestions =
            flashcardsForTopic(
                topicId
            )
                .size
                .coerceAtLeast(
                    1
                )

        questionCount =
            (
                    questionCount +
                            1
                    )
                .coerceAtMost(
                    maxQuestions
                )
    }


    fun decreaseQuestionCount() {

        questionCount =
            (
                    questionCount -
                            1
                    )
                .coerceAtLeast(
                    1
                )
    }


    fun setQuestionCountForTopic(
        topicId: Int
    ) {

        selectedTopicId =
            topicId

        val available =
            flashcardsForTopic(
                topicId
            )
                .size

        questionCount =

            if (
                available <= 0
            ) {

                1

            } else {

                minOf(
                    10,
                    available
                )
            }
    }


    fun startSession(
        topicId: Int
    ) {

        selectedTopicId =
            topicId

        practiceMode =
            false

        completionRecorded =
            false

        sessionAnswers.clear()

        currentQuestionIndex =
            0

        selectedAnswer =
            null

        selectedConfidence =
            null

        lastAnswer =
            null

        answerCommitted =
            false

        sessionStartTime =
            System.currentTimeMillis()

        questionStartedRealtime = SystemClock.elapsedRealtime()

        var cards =
            flashcardsForTopic(
                topicId
            )

        if (
            weakCardsOnly
        ) {

            val weakIds =
                resultsForTopic(
                    topicId
                )
                    .filter {
                        !it.isCorrect
                    }
                    .map {
                        it.flashcardId
                    }
                    .toSet()

            val weakCards =
                cards.filter {

                    weakIds.contains(
                        it.id
                    )
                }

            /*
             * If the user has no previous weak cards,
             * use all cards instead of showing
             * an empty review.
             */
            if (
                weakCards.isNotEmpty()
            ) {

                cards =
                    weakCards
            }
        }

        if (
            shuffleQuestions
        ) {

            cards =
                cards.shuffled()
        }

        sessionCards =
            cards.take(
                questionCount
                    .coerceAtLeast(
                        1
                    )
            )
    }


    fun startWeakPractice(
        topicId: Int
    ) {

        selectedTopicId =
            topicId

        practiceMode =
            true

        completionRecorded =
            false

        sessionAnswers.clear()

        currentQuestionIndex =
            0

        selectedAnswer =
            null

        selectedConfidence =
            null

        lastAnswer =
            null

        answerCommitted =
            false

        sessionStartTime =
            System.currentTimeMillis()

        questionStartedRealtime = SystemClock.elapsedRealtime()

        val topicCards =
            flashcardsForTopic(
                topicId
            )

        val weakIdsFromSession =
            sessionAnswers
                .filter {
                    !it.isCorrect
                }
                .map {
                    it.flashcardId
                }
                .toSet()

        val weakIdsFromHistory =
            resultsForTopic(
                topicId
            )
                .filter {
                    !it.isCorrect
                }
                .map {
                    it.flashcardId
                }
                .toSet()

        val weakIds =
            if (
                weakIdsFromSession
                    .isNotEmpty()
            ) {

                weakIdsFromSession

            } else {

                weakIdsFromHistory
            }

        val weakCards =
            topicCards.filter {

                weakIds.contains(
                    it.id
                )
            }

        sessionCards =

            if (
                weakCards.isNotEmpty()
            ) {

                weakCards

            } else {

                topicCards
            }
    }


    fun currentCard():
            FlashcardEntity? {

        return sessionCards
            .getOrNull(
                currentQuestionIndex
            )
    }


    fun selectAnswer(
        answer: String
    ) {

        selectedAnswer =
            answer
    }


    fun currentAnswerCorrect():
            Boolean {

        val card =
            currentCard()
                ?: return false

        val answer =
            selectedAnswer
                ?: return false

        return answer
            .trim()
            .equals(
                card.answer.trim(),
                ignoreCase = true
            )
    }


    fun buildAnswerOptions(
        card: FlashcardEntity
    ): List<String> {

        val otherAnswers =
            flashcardsForTopic(
                card.topicId
            )
                .filter {
                    it.id !=
                            card.id
                }
                .map {
                    it.answer
                        .trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()

        val options =
            mutableListOf<String>()

        options.add(
            card.answer
                .trim()
        )

        otherAnswers
            .take(
                3
            )
            .forEach {

                if (
                    !options.contains(
                        it
                    )
                ) {

                    options.add(
                        it
                    )
                }
            }

        val fallback =
            listOf(
                "None of the above",
                "I'm not sure",
                "Skip this question"
            )

        fallback.forEach {

            if (
                options.size <
                4 &&
                !options.contains(
                    it
                )
            ) {

                options.add(
                    it
                )
            }
        }

        /*
         * Stable order:
         * it does not move every time Compose
         * recomposes the screen.
         */
        return options
            .sortedBy {

                (
                        it +
                                card.id
                        )
                    .hashCode()
            }
            .take(
                4
            )
    }


    fun setConfidence(
        confidence: String
    ) {

        selectedConfidence =
            confidence
    }


    fun commitCurrentAnswer() {

        if (
            answerCommitted
        ) {

            return
        }

        val topicId =
            selectedTopicId
                ?: return

        val card =
            currentCard()
                ?: return

        val answer =
            selectedAnswer
                ?: return

        val confidence =
            selectedConfidence
                ?: return

        val correct =
            answer
                .trim()
                .equals(
                    card.answer.trim(),
                    ignoreCase = true
                )

        val sessionAnswer =
            ReviewSessionAnswer(
                flashcardId =
                    card.id,

                question =
                    card.question,

                correctAnswer =
                    card.answer,

                userAnswer =
                    answer,

                isCorrect =
                    correct,

                confidence =
                    confidence
            )

        sessionAnswers.add(
            sessionAnswer
        )

        lastAnswer =
            sessionAnswer

        answerCommitted =
            true

        scope.launch {
            database.withTransaction {
                reviewDao.insertResult(
                    ReviewResultEntity(
                        topicId = topicId,
                        flashcardId = card.id,
                        question = card.question,
                        correctAnswer = card.answer,
                        explanation = card.explanation,
                        userAnswer = answer,
                        isCorrect = correct,
                        confidence = confidence,
                        responseTimeMillis =
                            (SystemClock.elapsedRealtime() - questionStartedRealtime).coerceAtLeast(0L)
                    )
                )
                val attempts = reviewDao.getAnswerCountForTopic(topicId)
                val correctAnswers = reviewDao.getCorrectAnswerCountForTopic(topicId)
                topicDao.getTopicById(topicId)?.let { topic ->
                    topicDao.updateTopic(
                        topic.copy(
                            progress = if (attempts == 0) 0 else correctAnswers * 100 / attempts,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }


    fun moveToNextQuestion():
            Boolean {

        val hasNext =
            currentQuestionIndex <
                    sessionCards.lastIndex

        if (
            hasNext
        ) {

            currentQuestionIndex +=
                1

            questionStartedRealtime = SystemClock.elapsedRealtime()

            selectedAnswer =
                null

            selectedConfidence =
                null

            lastAnswer =
                null

            answerCommitted =
                false

            return true
        }

        return false
    }


    fun accuracy():
            Int {

        if (
            sessionAnswers
                .isEmpty()
        ) {

            return 0
        }

        val correct =
            sessionAnswers
                .count {
                    it.isCorrect
                }

        return (
                correct
                    .toFloat() /
                        sessionAnswers.size
                            .toFloat() *
                        100f
                )
            .roundToInt()
    }


    fun correctCount():
            Int {

        return sessionAnswers
            .count {
                it.isCorrect
            }
    }


    fun incorrectCount():
            Int {

        return sessionAnswers
            .count {
                !it.isCorrect
            }
    }


    fun weakAnswers():
            List<ReviewSessionAnswer> {

        return sessionAnswers
            .filter {
                !it.isCorrect
            }
    }


    fun sessionDurationMinutes():
            Int {

        if (
            sessionStartTime ==
            0L
        ) {

            return 0
        }

        val milliseconds =
            System.currentTimeMillis() -
                    sessionStartTime

        return (
                milliseconds /
                        60000L
                )
            .toInt()
            .coerceAtLeast(
                1
            )
    }


    fun finalizeNormalSession(
        appState: LoopiAppState
    ) {

        if (
            completionRecorded ||
            practiceMode
        ) {

            return
        }

        completionRecorded =
            true

        appState
            .recordReviewCompleted()

        val topicId =
            selectedTopicId
                ?: return

        val topic =
            appState.findTopic(
                topicId
            )
                ?: return

        val newProgress =
            maxOf(
                topic.progress,
                accuracy()
            )

        if (
            newProgress !=
            topic.progress
        ) {

            appState.updateTopic(
                topic.copy(
                    progress =
                        newProgress
                )
            )
        }
    }
}


/* =========================================================
   4B — CHOOSE TOPIC
   ========================================================= */

@Composable
fun ChooseReviewTopicScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState
) {

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Choose Topic",

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        },

        bottomBar = {

            LoopiBottomNavigation(
                navController =
                    navController,

                selected =
                    "review"
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
                        18.dp
                )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    "Choose a topic to review",

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Text(
                text =
                    "Select one of your study topics.",

                fontSize =
                    13.sp,

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            if (
                appState.topics
                    .isEmpty()
            ) {

                ReviewEmptyCard(
                    emoji =
                        "📚",

                    title =
                        "No topics yet",

                    description =
                        "Create a study topic before starting a review."
                )

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement
                            .spacedBy(
                                10.dp
                            ),

                    contentPadding =
                        PaddingValues(
                            bottom =
                                24.dp
                        )
                ) {

                    items(
                        items =
                            appState.topics,

                        key = {
                            it.id
                        }
                    ) {
                            topic ->

                        val cardCount =
                            flow4State
                                .flashcardsForTopic(
                                    topic.id
                                )
                                .size

                        val selected =
                            flow4State
                                .selectedTopicId ==
                                    topic.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled =
                                        cardCount >
                                                0
                                ) {

                                    flow4State
                                        .selectTopic(
                                            topic.id
                                        )

                                    flow4State
                                        .setQuestionCountForTopic(
                                            topic.id
                                        )

                                    navController
                                        .navigate(
                                            "review_settings/${topic.id}"
                                        )
                                },

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =

                                            if (
                                                selected
                                            ) {

                                                Color(
                                                    0xFFEAF3FF
                                                )

                                            } else {

                                                Color.White
                                            }
                                    )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        16.dp
                                    ),

                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(
                                            46.dp
                                        )
                                        .background(
                                            color =
                                                topicReviewColor(
                                                    topic.category
                                                )
                                                    .copy(
                                                        alpha =
                                                            0.16f
                                                    ),

                                            shape =
                                                RoundedCornerShape(
                                                    12.dp
                                                )
                                        ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text =
                                            topicReviewEmoji(
                                                topic.category
                                            ),

                                        fontSize =
                                            23.sp
                                    )
                                }

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
                                            FontWeight
                                                .Bold,

                                        color =
                                            ReviewBlue
                                    )

                                    Text(
                                        text =

                                            if (
                                                cardCount >
                                                0
                                            ) {

                                                "$cardCount flashcards available"

                                            } else {

                                                "No flashcards yet"
                                            },

                                        fontSize =
                                            12.sp,

                                        color =

                                            if (
                                                cardCount >
                                                0
                                            ) {

                                                Color.Gray

                                            } else {

                                                ReviewRed
                                            }
                                    )
                                }

                                Text(
                                    text =

                                        if (
                                            cardCount >
                                            0
                                        ) {

                                            "›"

                                        } else {

                                            "!"
                                        },

                                    fontSize =
                                        24.sp,

                                    color =

                                        if (
                                            cardCount >
                                            0
                                        ) {

                                            ReviewBlue

                                        } else {

                                            ReviewRed
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/* =========================================================
   4C — REVIEW SETTINGS
   ========================================================= */

@Composable
fun ReviewSettingsScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("loopi_settings", Context.MODE_PRIVATE) }

    val topic =
        appState.findTopic(
            topicId
        )

    val availableCards =
        flow4State
            .flashcardsForTopic(
                topicId
            )
            .size

    LaunchedEffect(
        topicId
    ) {

        flow4State
            .setQuestionCountForTopic(
                topicId
            )
        flow4State.weakCardsOnly = preferences.getBoolean("weak_only_default", false)
    }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Review Settings",

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
                    18.dp
                )
        ) {

            Text(
                text =
                    topic?.title
                        ?: "Review",

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Text(
                text =
                    "$availableCards flashcards available",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            ReviewSectionCard {

                Text(
                    text =
                        "Number of questions",

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ReviewBlue
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement
                            .Center,

                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {

                    OutlinedButton(
                        onClick = {

                            flow4State
                                .decreaseQuestionCount()
                        }
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
                                22.dp
                            )
                    )

                    Text(
                        text =
                            flow4State
                                .questionCount
                                .toString(),

                        fontSize =
                            30.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            ReviewBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                22.dp
                            )
                    )

                    OutlinedButton(
                        onClick = {

                            flow4State
                                .increaseQuestionCount()
                        }
                    ) {

                        Text(
                            text =
                                "+",

                            fontSize =
                                20.sp
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            ReviewSectionCard {

                Text(
                    text =
                        "Review options",

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ReviewBlue
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                ReviewSwitchRow(
                    title =
                        "Shuffle questions",

                    subtitle =
                        "Show flashcards in a random order.",

                    checked =
                        flow4State
                            .shuffleQuestions,

                    onCheckedChange = {

                        flow4State
                            .shuffleQuestions =
                            it
                    }
                )

                HorizontalDivider()

                ReviewSwitchRow(
                    title =
                        "Weak cards only",

                    subtitle =
                        "Focus on cards you answered incorrectly before.",

                    checked =
                        flow4State
                            .weakCardsOnly,

                    onCheckedChange = {

                        flow4State
                            .weakCardsOnly =
                            it
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f,
                        fill = false
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )

            Button(
                onClick = {

                    flow4State
                        .startSession(
                            topicId
                        )

                    navController
                        .navigate(
                            "review_loading/$topicId"
                        )
                },

                enabled =
                    availableCards >
                            0,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Start Quiz",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                availableCards ==
                0
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        "Add flashcards to this topic before starting the review.",

                    color =
                        ReviewRed,

                    fontSize =
                        12.sp,

                    textAlign =
                        TextAlign.Center,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                )
            }
        }
    }
}


/* =========================================================
   4D — LOADING QUESTIONS
   ========================================================= */

@Composable
fun ReviewLoadingScreen(
    navController: NavHostController,
    topicId: Int,
    flow4State: Flow4AppState
) {

    LaunchedEffect(
        topicId
    ) {

        delay(
            1100
        )

        if (
            flow4State
                .sessionCards
                .isNotEmpty()
        ) {

            navController
                .navigate(
                    "review_question/$topicId"
                ) {

                    popUpTo(
                        "review_loading/$topicId"
                    ) {

                        inclusive =
                            true
                    }
                }

        } else {

            navController
                .popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ReviewBackground
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment
                    .CenterHorizontally
        ) {

            Text(
                text =
                    "📚",

                fontSize =
                    72.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Text(
                text =
                    "Get ready!",

                fontSize =
                    25.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "Loading your questions...",

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            CircularProgressIndicator(
                color =
                    ReviewBrightBlue
            )
        }
    }
}


/* =========================================================
   4E — QUIZ QUESTION
   ========================================================= */

@Composable
fun ReviewQuestionScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val card =
        flow4State.currentCard()

    if (
        card == null
    ) {

        ReviewNoQuestionScreen(
            navController =
                navController
        )

        return
    }

    val topic =
        appState.findTopic(
            topicId
        )

    val options =
        remember(
            card.id,
            flow4State.sessionCards
        ) {

            flow4State
                .buildAnswerOptions(
                    card
                )
        }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    topic?.title
                        ?: "Review",

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
                    horizontal =
                        18.dp
                )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            ReviewProgressHeader(
                current =
                    flow4State
                        .currentQuestionIndex +
                            1,

                total =
                    flow4State
                        .sessionCards
                        .size
            )

            Spacer(
                modifier =
                    Modifier.height(
                        26.dp
                    )
            )

            Text(
                text =
                    "What is the answer?",

                fontSize =
                    13.sp,

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )

            Text(
                text =
                    card.question,

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            options
                .forEachIndexed {
                        index,
                        option ->

                    val selected =
                        flow4State
                            .selectedAnswer ==
                                option

                    AnswerOptionCard(
                        letter =
                            (
                                    'A'.code +
                                            index
                                    )
                                .toChar()
                                .toString(),

                        text =
                            option,

                        selected =
                            selected,

                        onClick = {

                            flow4State
                                .selectAnswer(
                                    option
                                )
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(
                onClick = {

                    if (
                        flow4State
                            .currentAnswerCorrect()
                    ) {

                        navController
                            .navigate(
                                "review_correct/$topicId"
                            )

                    } else {

                        navController
                            .navigate(
                                "review_incorrect/$topicId"
                            )
                    }
                },

                enabled =
                    flow4State
                        .selectedAnswer !=
                            null,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Submit Answer",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )
        }
    }
}


/* =========================================================
   4F — CORRECT
   ========================================================= */

@Composable
fun ReviewCorrectScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    ReviewFeedbackScreen(
        emoji =
            "✓",

        emojiBackground =
            ReviewGreen,

        title =
            "Correct!",

        titleColor =
            ReviewGreen,

        description =
            "Great job! You remembered this card correctly.",

        buttonText =
            "See Explanation →",

        onContinue = {

            navController
                .navigate(
                    "review_explanation/$topicId"
                )
        }
    )
}


/* =========================================================
   4G — INCORRECT
   ========================================================= */

@Composable
fun ReviewIncorrectScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val card =
        flow4State.currentCard()

    ReviewFeedbackScreen(
        emoji =
            "×",

        emojiBackground =
            ReviewRed,

        title =
            "Not quite!",

        titleColor =
            ReviewRed,

        description =
            "Correct answer:\n${card?.answer ?: ""}",

        buttonText =
            "See Explanation →",

        onContinue = {

            navController
                .navigate(
                    "review_explanation/$topicId"
                )
        }
    )
}


/* =========================================================
   4H — EXPLANATION
   ========================================================= */

@Composable
fun ReviewExplanationScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val card =
        flow4State.currentCard()

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Explanation",

                onBack =
                    null
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

            Text(
                text =
                    card?.question
                        ?: "",

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                Color.White
                        ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .padding(
                                18.dp
                            )
                ) {

                    Text(
                        text =
                            "Correct answer",

                        fontSize =
                            12.sp,

                        color =
                            Color.Gray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            card?.answer
                                ?: "",

                        fontSize =
                            17.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            ReviewGreen
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )

                    Text(
                        text =
                            "Why?",

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            ReviewBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    Text(
                        text = card?.explanation?.takeIf { it.isNotBlank() }
                            ?: "Review the question and answer together before continuing.",

                        color =
                            Color.DarkGray,

                        fontSize =
                            14.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(
                onClick = {

                    navController
                        .navigate(
                            "review_confidence/$topicId"
                        )
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
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Continue →",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )
        }
    }
}


/* =========================================================
   4I–4L — CONFIDENCE CHECK
   ========================================================= */

@Composable
fun ReviewConfidenceScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Confidence Check",

                onBack =
                    null
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
                ),

            horizontalAlignment =
                Alignment
                    .CenterHorizontally
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Text(
                text =
                    "How confident are you?",

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "Be honest — this helps Loopi identify weak areas.",

                color =
                    Color.Gray,

                textAlign =
                    TextAlign.Center,

                fontSize =
                    13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        32.dp
                    )
            )

            ConfidenceCard(
                emoji =
                    "😟",

                title =
                    "Not sure",

                subtitle =
                    "I mostly guessed",

                background =
                    ReviewLightRed,

                borderColor =
                    ReviewRed,

                selected =
                    flow4State
                        .selectedConfidence ==
                            "Not sure",

                onClick = {

                    flow4State
                        .setConfidence(
                            "Not sure"
                        )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            ConfidenceCard(
                emoji =
                    "🙂",

                title =
                    "Maybe",

                subtitle =
                    "I had an idea",

                background =
                    ReviewLightYellow,

                borderColor =
                    ReviewYellow,

                selected =
                    flow4State
                        .selectedConfidence ==
                            "Maybe",

                onClick = {

                    flow4State
                        .setConfidence(
                            "Maybe"
                        )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            ConfidenceCard(
                emoji =
                    "😎",

                title =
                    "I know it",

                subtitle =
                    "I was confident",

                background =
                    ReviewLightGreen,

                borderColor =
                    ReviewGreen,

                selected =
                    flow4State
                        .selectedConfidence ==
                            "I know it",

                onClick = {

                    flow4State
                        .setConfidence(
                            "I know it"
                        )
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

                    flow4State
                        .commitCurrentAnswer()

                    navController
                        .navigate(
                            "review_next/$topicId"
                        )
                },

                enabled =
                    flow4State
                        .selectedConfidence !=
                            null,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Next →",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )
        }
    }
}


/* =========================================================
   4M — NEXT QUESTION
   ========================================================= */

@Composable
fun ReviewNextQuestionScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val answer =
        flow4State
            .lastAnswer

    val nextNumber =
        flow4State
            .currentQuestionIndex +
                2

    val total =
        flow4State
            .sessionCards
            .size

    Scaffold(
        containerColor =
            ReviewBackground
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    22.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =

                    if (
                        answer?.isCorrect ==
                        true
                    ) {

                        "✓"

                    } else {

                        "↻"
                    },

                fontSize =
                    62.sp,

                color =

                    if (
                        answer?.isCorrect ==
                        true
                    ) {

                        ReviewGreen

                    } else {

                        ReviewRed
                    }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =

                    if (
                        nextNumber <=
                        total
                    ) {

                        "Next question"

                    } else {

                        "Review finished"
                    },

                fontSize =
                    25.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            if (
                nextNumber <=
                total
            ) {

                Text(
                    text =
                        "Question $nextNumber of $total",

                    color =
                        Color.Gray
                )

            } else {

                Text(
                    text =
                        "Let's see how you did.",

                    color =
                        Color.Gray
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )

            Button(
                onClick = {

                    val hasNext =
                        flow4State
                            .moveToNextQuestion()

                    if (
                        hasNext
                    ) {

                        navController
                            .navigate(
                                "review_mid_progress/$topicId"
                            )

                    } else {

                        navController
                            .navigate(
                                if (
                                    flow4State
                                        .practiceMode
                                ) {

                                    "review_practice_complete/$topicId"

                                } else {

                                    "review_complete/$topicId"
                                }
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
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =

                        if (
                            nextNumber <=
                            total
                        ) {

                            "Continue →"

                        } else {

                            "View Results →"
                        },

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/* =========================================================
   4N — MID PROGRESS
   ========================================================= */

@Composable
fun ReviewMidProgressScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val completed =
        flow4State
            .sessionAnswers
            .size

    val total =
        flow4State
            .sessionCards
            .size

    val percent =

        if (
            total >
            0
        ) {

            (
                    completed
                        .toFloat() /
                            total
                                .toFloat() *
                            100f
                    )
                .roundToInt()

        } else {

            0
        }

    LaunchedEffect(
        completed,
        total
    ) {

        delay(
            850
        )

        navController
            .navigate(
                "review_question/$topicId"
            ) {

                popUpTo(
                    "review_mid_progress/$topicId"
                ) {

                    inclusive =
                        true
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ReviewBackground
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(
                        125.dp
                    )
                    .background(
                        color =
                            Color.White,

                        shape =
                            CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment
                            .CenterHorizontally
                ) {

                    Text(
                        text =
                            "$completed/$total",

                        fontSize =
                            28.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            ReviewBlue
                    )

                    Text(
                        text =
                            "$percent%",

                        color =
                            ReviewGreen,

                        fontWeight =
                            FontWeight.Bold
                    )
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
                    "Nice progress!",

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Text(
                text =
                    "Keep going.",

                color =
                    Color.Gray
            )
        }
    }
}


/* =========================================================
   4O — REVIEW COMPLETED
   ========================================================= */

@Composable
fun ReviewCompletedScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    LaunchedEffect(
        topicId
    ) {

        flow4State
            .finalizeNormalSession(
                appState
            )
    }

    val accuracy =
        flow4State
            .accuracy()

    Scaffold(
        containerColor =
            ReviewBackground
    ) {
            innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    22.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =
                    "🎉",

                fontSize =
                    70.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(
                text =
                    "Review completed!",

                fontSize =
                    27.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )

            Text(
                text =
                    "You scored $accuracy%",

                color =
                    ReviewGreen,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )

            Button(
                onClick = {

                    navController
                        .navigate(
                            "review_summary/$topicId"
                        )
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
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "View Results",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/* =========================================================
   4P — SUMMARY OVERVIEW
   ========================================================= */

@Composable
fun ReviewSummaryScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(
            topicId
        )

    val accuracy =
        flow4State
            .accuracy()

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Review Summary",

                onBack = {

                    navController
                        .navigate(
                            "home"
                        )
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
                    18.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    topic?.title
                        ?: "Review",

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            Box(
                modifier = Modifier
                    .size(
                        140.dp
                    )
                    .background(
                        color =
                            Color.White,

                        shape =
                            CircleShape
                    )
                    .border(
                        width =
                            10.dp,

                        color =

                            if (
                                accuracy >=
                                70
                            ) {

                                ReviewGreen

                            } else {

                                ReviewYellow
                            },

                        shape =
                            CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "$accuracy%",

                        fontSize =
                            33.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            ReviewBlue
                    )

                    Text(
                        text =
                            "Score",

                        color =
                            Color.Gray
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .spacedBy(
                            10.dp
                        )
            ) {

                ReviewStatCard(
                    emoji =
                        "✓",

                    value =
                        flow4State
                            .correctCount()
                            .toString(),

                    label =
                        "Correct",

                    color =
                        ReviewGreen,

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                )

                ReviewStatCard(
                    emoji =
                        "×",

                    value =
                        flow4State
                            .incorrectCount()
                            .toString(),

                    label =
                        "Incorrect",

                    color =
                        ReviewRed,

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                )

                ReviewStatCard(
                    emoji =
                        "⏱",

                    value =
                        "${flow4State.sessionDurationMinutes()}m",

                    label =
                        "Time",

                    color =
                        ReviewPurple,

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Button(
                onClick = {

                    navController
                        .navigate(
                            "review_detailed/$topicId"
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth(),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "View Detailed Results"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            OutlinedButton(
                onClick = {

                    navController
                        .navigate(
                            "review_weak_spots/$topicId"
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
            ) {

                Text(
                    text =
                        "View Weak Spots"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            TextButton(
                onClick = {

                    navController
                        .navigate(
                            "home"
                        ) {

                            popUpTo(
                                "home"
                            ) {

                                inclusive =
                                    true
                            }
                        }
                }
            ) {

                Text(
                    text =
                        "Back to Home"
                )
            }
        }
    }
}


/* =========================================================
   4Q — DETAILED RESULTS
   ========================================================= */

@Composable
fun ReviewDetailedResultsScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Detailed Results",

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    ) {
            innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                ),

            contentPadding =
                PaddingValues(
                    16.dp
                ),

            verticalArrangement =
                Arrangement
                    .spacedBy(
                        10.dp
                    )
        ) {

            items(
                items =
                    flow4State
                        .sessionAnswers
                        .toList(),

                key = {
                    it.flashcardId
                }
            ) {
                    result ->

                ResultRowCard(
                    result =
                        result
                )
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        navController
                            .navigate(
                                "review_incorrect_questions/$topicId"
                            )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =
                            "Incorrect Questions"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        navController
                            .navigate(
                                "review_topic_performance/$topicId"
                            )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =
                            "Topic Performance"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                OutlinedButton(
                    onClick = {

                        navController
                            .navigate(
                                "review_confidence_analysis/$topicId"
                            )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =
                            "Confidence Analysis"
                    )
                }
            }
        }
    }
}


/* =========================================================
   4R — INCORRECT QUESTIONS
   ========================================================= */

@Composable
fun ReviewIncorrectQuestionsScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val wrongAnswers =
        flow4State
            .weakAnswers()

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Incorrect Questions",

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    ) {
            innerPadding ->

        if (
            wrongAnswers
                .isEmpty()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                ReviewEmptyCard(
                    emoji =
                        "🎉",

                    title =
                        "Perfect review!",

                    description =
                        "You answered every question correctly."
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    ),

                contentPadding =
                    PaddingValues(
                        16.dp
                    ),

                verticalArrangement =
                    Arrangement
                        .spacedBy(
                            10.dp
                        )
            ) {

                items(
                    items =
                        wrongAnswers,

                    key = {
                        it.flashcardId
                    }
                ) {
                        result ->

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
                            modifier =
                                Modifier
                                    .padding(
                                        16.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "✕ ${result.question}",

                                fontWeight =
                                    FontWeight
                                        .Bold,

                                color =
                                    ReviewRed
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Your answer",

                                fontSize =
                                    11.sp,

                                color =
                                    Color.Gray
                            )

                            Text(
                                text =
                                    result.userAnswer,

                                color =
                                    ReviewRed
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Correct answer",

                                fontSize =
                                    11.sp,

                                color =
                                    Color.Gray
                            )

                            Text(
                                text =
                                    result.correctAnswer,

                                color =
                                    ReviewGreen,

                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


/* =========================================================
   4S — TOPIC PERFORMANCE
   ========================================================= */

@Composable
fun ReviewTopicPerformanceScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(
            topicId
        )

    val history =
        flow4State
            .resultsForTopic(
                topicId
            )

    val grouped =
        history
            .groupBy {
                it.flashcardId
            }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Topic Performance",

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

            Text(
                text =
                    topic?.title
                        ?: "",

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            if (
                grouped.isEmpty()
            ) {

                ReviewEmptyCard(
                    emoji =
                        "📊",

                    title =
                        "No performance data",

                    description =
                        "Complete a review to see performance."
                )

            } else {

                grouped.entries
                    .take(
                        6
                    )
                    .forEach {
                            entry ->

                        val attempts =
                            entry.value

                        val correct =
                            attempts.count {
                                it.isCorrect
                            }

                        val percentage =
                            (
                                    correct
                                        .toFloat() /
                                            attempts.size
                                                .toFloat()
                                    )
                                .coerceIn(
                                    0f,
                                    1f
                                )

                        val question =
                            attempts
                                .firstOrNull()
                                ?.question
                                ?: "Flashcard"

                        Text(
                            text =
                                question,

                            maxLines =
                                1,

                            fontWeight =
                                FontWeight
                                    .SemiBold,

                            color =
                                ReviewBlue
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    5.dp
                                )
                        )

                        LinearProgressIndicator(
                            progress = {
                                percentage
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    9.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        10.dp
                                    )
                                ),

                            color =

                                if (
                                    percentage >=
                                    0.7f
                                ) {

                                    ReviewGreen

                                } else if (
                                    percentage >=
                                    0.4f
                                ) {

                                    ReviewYellow

                                } else {

                                    ReviewRed
                                },

                            trackColor =
                                Color(
                                    0xFFE8EDF5
                                )
                        )

                        Text(
                            text =
                                "${(percentage * 100).roundToInt()}% correct",

                            fontSize =
                                11.sp,

                            color =
                                Color.Gray,

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            textAlign =
                                TextAlign.End
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )
                    }
            }
        }
    }
}


/* =========================================================
   4T — CONFIDENCE ANALYSIS
   ========================================================= */

@Composable
fun ReviewConfidenceAnalysisScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val data =

        if (
            flow4State
                .sessionAnswers
                .isNotEmpty()
        ) {

            flow4State
                .sessionAnswers
                .toList()

        } else {

            flow4State
                .resultsForTopic(
                    topicId
                )
                .map {

                    ReviewSessionAnswer(
                        flashcardId =
                            it.flashcardId,

                        question =
                            it.question,

                        correctAnswer =
                            it.correctAnswer,

                        userAnswer =
                            it.userAnswer,

                        isCorrect =
                            it.isCorrect,

                        confidence =
                            it.confidence
                    )
                }
        }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Confidence Analysis",

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

            Text(
                text =
                    "How confident did you feel?",

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            ConfidenceAnalysisCard(
                emoji =
                    "😟",

                title =
                    "Not sure",

                results =
                    data.filter {
                        it.confidence ==
                                "Not sure"
                    },

                color =
                    ReviewRed
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            ConfidenceAnalysisCard(
                emoji =
                    "🙂",

                title =
                    "Maybe",

                results =
                    data.filter {
                        it.confidence ==
                                "Maybe"
                    },

                color =
                    ReviewYellow
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            ConfidenceAnalysisCard(
                emoji =
                    "😎",

                title =
                    "I know it",

                results =
                    data.filter {
                        it.confidence ==
                                "I know it"
                    },

                color =
                    ReviewGreen
            )

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                Color(
                                    0xFFEAF3FF
                                )
                        )
            ) {

                Text(
                    text =
                        "Tip: low confidence combined with incorrect answers is a strong sign that a flashcard needs more practice.",

                    modifier =
                        Modifier.padding(
                            15.dp
                        ),

                    fontSize =
                        13.sp,

                    color =
                        ReviewBlue
                )
            }
        }
    }
}


/* =========================================================
   4U — WEAK SPOTS
   ========================================================= */

@Composable
fun ReviewWeakSpotsScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val topic =
        appState.findTopic(
            topicId
        )

    val history =
        flow4State
            .resultsForTopic(
                topicId
            )

    val weakGroups =
        history
            .filter {
                !it.isCorrect
            }
            .groupBy {
                it.flashcardId
            }
            .entries
            .sortedByDescending {
                it.value.size
            }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Weak Spots",

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

            Text(
                text =
                    topic?.title
                        ?: "",

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Text(
                text =
                    "These cards need more practice.",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            if (
                weakGroups.isEmpty()
            ) {

                ReviewEmptyCard(
                    emoji =
                        "✓",

                    title =
                        "No weak spots",

                    description =
                        "Great work! No incorrect cards were found."
                )

            } else {

                weakGroups
                    .take(
                        6
                    )
                    .forEach {
                            entry ->

                        val result =
                            entry.value
                                .first()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical =
                                        5.dp
                                ),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            Color.White
                                    )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        14.dp
                                    ),

                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(
                                            38.dp
                                        )
                                        .background(
                                            ReviewLightRed,
                                            CircleShape
                                        ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text =
                                            "!",
                                        color =
                                            ReviewRed,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

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
                                            result.question,

                                        fontWeight =
                                            FontWeight
                                                .SemiBold,

                                        color =
                                            ReviewBlue
                                    )

                                    Text(
                                        text =
                                            "${entry.value.size} incorrect attempt(s)",

                                        fontSize =
                                            11.sp,

                                        color =
                                            ReviewRed
                                    )
                                }
                            }
                        }
                    }
            }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(
                onClick = {

                    flow4State
                        .startWeakPractice(
                            topicId
                        )

                    navController
                        .navigate(
                            "review_practice/$topicId"
                        )
                },

                enabled =
                    weakGroups
                        .isNotEmpty(),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Practice Weak Topic",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )
        }
    }
}


/* =========================================================
   4V — PRACTICE WEAK TOPIC
   ========================================================= */

@Composable
fun ReviewPracticeWeakTopicScreen(
    navController: NavHostController,
    appState: LoopiAppState,
    flow4State: Flow4AppState,
    topicId: Int
) {

    val card =
        flow4State.currentCard()

    if (
        card == null
    ) {

        ReviewNoQuestionScreen(
            navController =
                navController
        )

        return
    }

    val options =
        remember(
            card.id
        ) {

            flow4State
                .buildAnswerOptions(
                    card
                )
        }

    Scaffold(
        containerColor =
            ReviewBackground,

        topBar = {

            ReviewTopBar(
                title =
                    "Practice Weak Topic",

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

            ReviewProgressHeader(
                current =
                    flow4State
                        .currentQuestionIndex +
                            1,

                total =
                    flow4State
                        .sessionCards
                        .size
            )

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Text(
                text =
                    "Weak card",

                color =
                    ReviewRed,

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    card.question,

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            options
                .forEachIndexed {
                        index,
                        option ->

                    AnswerOptionCard(
                        letter =
                            (
                                    'A'.code +
                                            index
                                    )
                                .toChar()
                                .toString(),

                        text =
                            option,

                        selected =
                            flow4State
                                .selectedAnswer ==
                                    option,

                        onClick = {

                            flow4State
                                .selectAnswer(
                                    option
                                )
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(
                onClick = {

                    /*
                     * For weak-practice mode we use
                     * "Maybe" as the neutral confidence
                     * so the result can still be stored.
                     */
                    flow4State
                        .setConfidence(
                            "Maybe"
                        )

                    flow4State
                        .commitCurrentAnswer()

                    val hasNext =
                        flow4State
                            .moveToNextQuestion()

                    if (
                        hasNext
                    ) {

                        navController
                            .navigate(
                                "review_practice/$topicId"
                            ) {

                                popUpTo(
                                    "review_practice/$topicId"
                                ) {

                                    inclusive =
                                        true
                                }
                            }

                    } else {

                        navController
                            .navigate(
                                "review_practice_complete/$topicId"
                            )
                    }
                },

                enabled =
                    flow4State
                        .selectedAnswer !=
                            null,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    ),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Submit & Continue",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )
        }
    }
}


/* =========================================================
   4W — PRACTICE COMPLETED
   ========================================================= */

@Composable
fun ReviewPracticeCompletedScreen(
    navController: NavHostController,
    flow4State: Flow4AppState,
    topicId: Int
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ReviewBackground
            )
            .padding(
                24.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(
                        90.dp
                    )
                    .background(
                        ReviewLightGreen,
                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "✓",

                    fontSize =
                        48.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ReviewGreen
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            Text(
                text =
                    "Weak Topic Practice\nCompleted!",

                fontSize =
                    25.sp,

                textAlign =
                    TextAlign.Center,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "You practised the cards that needed extra attention.",

                textAlign =
                    TextAlign.Center,

                color =
                    Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )

            Button(
                onClick = {

                    navController
                        .navigate(
                            "review_summary/$topicId"
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth(),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        "Back to Review Results"
                )
            }
        }
    }
}


/* =========================================================
   SMALL COMPONENTS
   ========================================================= */

@Composable
private fun ReviewTopBar(
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
                    8.dp,

                vertical =
                    8.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        if (
            onBack != null
        ) {

            IconButton(
                onClick =
                    onBack
            ) {

                Text(
                    text =
                        "←",

                    fontSize =
                        24.sp,

                    color =
                        ReviewBlue
                )
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
                Modifier.weight(
                    1f
                ),

            textAlign =
                TextAlign.Center,

            fontWeight =
                FontWeight.Bold,

            fontSize =
                19.sp,

            color =
                ReviewBlue
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
private fun ReviewProgressHeader(
    current: Int,
    total: Int
) {

    Column {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),

            horizontalArrangement =
                Arrangement
                    .SpaceBetween
        ) {

            Text(
                text =
                    "Question $current of $total",

                fontSize =
                    12.sp,

                color =
                    Color.Gray
            )

            Text(
                text =

                    if (
                        total >
                        0
                    ) {

                        "${
                            (
                                    current
                                        .toFloat() /
                                            total
                                                .toFloat() *
                                            100f
                                    )
                                .roundToInt()
                        }%"

                    } else {

                        "0%"
                    },

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        LinearProgressIndicator(
            progress = {

                if (
                    total >
                    0
                ) {

                    (
                            current
                                .toFloat() /
                                    total
                                        .toFloat()
                            )
                        .coerceIn(
                            0f,
                            1f
                        )

                } else {

                    0f
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(
                    7.dp
                )
                .clip(
                    RoundedCornerShape(
                        20.dp
                    )
                ),

            color =
                ReviewYellow,

            trackColor =
                Color(
                    0xFFE8EDF5
                )
        )
    }
}


@Composable
private fun AnswerOptionCard(
    letter: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                onClick()
            }
            .then(

                if (
                    selected
                ) {

                    Modifier.border(
                        width =
                            2.dp,

                        color =
                            ReviewBrightBlue,

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    )

                } else {

                    Modifier
                }
            ),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =

                        if (
                            selected
                        ) {

                            Color(
                                0xFFEAF3FF
                            )

                        } else {

                            Color.White
                        }
                )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(
                        32.dp
                    )
                    .background(
                        color =

                            if (
                                selected
                            ) {

                                ReviewBrightBlue

                            } else {

                                Color(
                                    0xFFF0F3F8
                                )
                            },

                        shape =
                            CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        letter,

                    fontWeight =
                        FontWeight.Bold,

                    color =

                        if (
                            selected
                        ) {

                            Color.White

                        } else {

                            ReviewBlue
                        }
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Text(
                text =
                    text,

                modifier =
                    Modifier.weight(
                        1f
                    ),

                color =
                    Color(
                        0xFF20283D
                    )
            )
        }
    }
}


@Composable
private fun ReviewFeedbackScreen(
    emoji: String,
    emojiBackground: Color,
    title: String,
    titleColor: Color,
    description: String,
    buttonText: String,
    onContinue: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ReviewBackground
            )
            .padding(
                24.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(
                        94.dp
                    )
                    .background(
                        emojiBackground,
                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        emoji,

                    fontSize =
                        52.sp,

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Text(
                text =
                    title,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    titleColor
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    description,

                color =
                    Color.Gray,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        32.dp
                    )
            )

            Button(
                onClick =
                    onContinue,

                modifier =
                    Modifier
                        .fillMaxWidth(),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                ReviewBrightBlue
                        )
            ) {

                Text(
                    text =
                        buttonText
                )
            }
        }
    }
}


@Composable
private fun ConfidenceCard(
    emoji: String,
    title: String,
    subtitle: String,
    background: Color,
    borderColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                onClick()
            }
            .border(
                width =

                    if (
                        selected
                    ) {

                        2.dp

                    } else {

                        1.dp
                    },

                color =

                    if (
                        selected
                    ) {

                        borderColor

                    } else {

                        ReviewBorder
                    },

                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =

                        if (
                            selected
                        ) {

                            background

                        } else {

                            Color.White
                        }
                )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    16.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    emoji,

                fontSize =
                    30.sp
            )

            Spacer(
                modifier =
                    Modifier.width(
                        14.dp
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
                        title,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ReviewBlue
                )

                Text(
                    text =
                        subtitle,

                    fontSize =
                        12.sp,

                    color =
                        Color.Gray
                )
            }

            if (
                selected
            ) {

                Text(
                    text =
                        "✓",

                    color =
                        borderColor,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        20.sp
                )
            }
        }
    }
}


@Composable
private fun ReviewSectionCard(
    content:
    @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
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
                        16.dp
                    ),

            content =
                content
        )
    }
}


@Composable
private fun ReviewSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange:
        (Boolean) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,

                fontWeight =
                    FontWeight
                        .SemiBold,

                color =
                    ReviewBlue
            )

            Text(
                text =
                    subtitle,

                fontSize =
                    11.sp,

                color =
                    Color.Gray
            )
        }

        Switch(
            checked =
                checked,

            onCheckedChange =
                onCheckedChange
        )
    }
}


@Composable
private fun ReviewStatCard(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier =
        Modifier
) {

    Card(
        modifier =
            modifier,

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
                    12.dp
                ),

            horizontalAlignment =
                Alignment
                    .CenterHorizontally
        ) {

            Text(
                text =
                    emoji,

                fontSize =
                    22.sp,

                color =
                    color
            )

            Text(
                text =
                    value,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    color
            )

            Text(
                text =
                    label,

                fontSize =
                    10.sp,

                color =
                    Color.Gray
            )
        }
    }
}


@Composable
private fun ResultRowCard(
    result: ReviewSessionAnswer
) {

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),

            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(
                        34.dp
                    )
                    .background(
                        color =

                            if (
                                result
                                    .isCorrect
                            ) {

                                ReviewLightGreen

                            } else {

                                ReviewLightRed
                            },

                        shape =
                            CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =

                        if (
                            result
                                .isCorrect
                        ) {

                            "✓"

                        } else {

                            "×"
                        },

                    color =

                        if (
                            result
                                .isCorrect
                        ) {

                            ReviewGreen

                        } else {

                            ReviewRed
                        },

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        result.question,

                    fontWeight =
                        FontWeight
                            .SemiBold,

                    color =
                        ReviewBlue
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(
                    text =
                        "Confidence: ${result.confidence}",

                    fontSize =
                        11.sp,

                    color =
                        Color.Gray
                )
            }
        }
    }
}


@Composable
private fun ConfidenceAnalysisCard(
    emoji: String,
    title: String,
    results:
    List<ReviewSessionAnswer>,
    color: Color
) {

    val correct =
        results.count {
            it.isCorrect
        }

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

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment
                    .CenterVertically
        ) {

            Text(
                text =
                    emoji,

                fontSize =
                    28.sp
            )

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
                        title,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ReviewBlue
                )

                Text(
                    text =
                        "${results.size} question(s)",

                    fontSize =
                        11.sp,

                    color =
                        Color.Gray
                )
            }

            Text(
                text =

                    if (
                        results
                            .isEmpty()
                    ) {

                        "—"

                    } else {

                        "$correct/${results.size}"
                    },

                fontWeight =
                    FontWeight.Bold,

                color =
                    color
            )
        }
    }
}


@Composable
private fun ReviewEmptyCard(
    emoji: String,
    title: String,
    description: String
) {

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
                    24.dp
                ),

            horizontalAlignment =
                Alignment
                    .CenterHorizontally
        ) {

            Text(
                text =
                    emoji,

                fontSize =
                    42.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    title,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text =
                    description,

                color =
                    Color.Gray,

                fontSize =
                    12.sp,

                textAlign =
                    TextAlign.Center
            )
        }
    }
}


@Composable
private fun ReviewNoQuestionScreen(
    navController: NavHostController
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ReviewBackground
            )
            .padding(
                24.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "📭",

                fontSize =
                    55.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "No questions available",

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ReviewBlue
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "Add flashcards to this topic first.",

                color =
                    Color.Gray,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
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
    }
}


private fun topicReviewEmoji(
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


private fun topicReviewColor(
    category: String
): Color {

    return when (
        category
    ) {

        "Computer Science" ->
            ReviewGreen

        "Information Technology",
        "IT" ->
            Color(
                0xFF36B6C9
            )

        "Engineering" ->
            ReviewYellow

        "Business" ->
            ReviewPurple

        "Mathematics" ->
            Color(
                0xFFE85CB3
            )

        else ->
            ReviewBrightBlue
    }
}

