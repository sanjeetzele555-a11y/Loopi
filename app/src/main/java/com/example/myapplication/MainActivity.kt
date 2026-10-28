package com.example.myapplication

import com.example.myapplication.ui.theme.LoopiColors
import com.example.myapplication.ui.theme.LoopiTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {

            LoopiTheme {

                val navController =
                    rememberNavController()

                val coroutineScope =
                    rememberCoroutineScope()

                val appState =
                    remember {

                        LoopiAppState(
                            applicationContext,
                            coroutineScope
                        )
                    }

                val flow3State =
                    remember {

                        Flow3AppState(
                            applicationContext,
                            coroutineScope
                        )
                    }

                val flow4State =
                    remember {

                        Flow4AppState(
                            applicationContext,
                            coroutineScope
                        )
                    }

                val flow6State =
                    remember {
                        Flow6AppState(
                            applicationContext,
                            coroutineScope
                        )
                    }

                LoopiNavigation(
                    navController = navController,
                    appState = appState,
                    flow3State = flow3State,
                    flow4State = flow4State,
                    flow6State = flow6State
                )
            }
        }
    }
}


@Composable
fun LoopiNavigation(
    navController: NavHostController,
    appState: LoopiAppState,
    flow3State: Flow3AppState,
    flow4State: Flow4AppState,
    flow6State: Flow6AppState
) {

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        /* =====================================================
           FLOW 1
           ===================================================== */

        composable("splash") {

            SplashScreen(
                navController = navController
            )
        }


        composable("welcome") {

            WelcomeScreen(
                navController = navController
            )
        }


        composable("signin") {

            SignInScreen(
                navController = navController
            )
        }


        composable("signup") {

            SignUpScreen(
                navController = navController
            )
        }


        composable("account_created") {

            AccountCreatedScreen(
                navController = navController
            )
        }


        composable("forgot_password") {

            ForgotPasswordScreen(
                navController = navController
            )
        }


        composable("reset_password") {

            ResetPasswordScreen(
                navController = navController
            )
        }


        composable("first_time_setup") {

            FirstTimeSetupScreen(
                navController = navController
            )
        }


        composable("setup_complete") {

            SetupCompletedScreen(
                navController = navController
            )
        }


        /* =====================================================
           FLOW 2
           ===================================================== */

        composable("home") {

            Flow2HomeScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State
            )
        }


        composable("study_packs") {
            StudyPacksScreen(
                navController = navController,
                flow6State = flow6State
            )
        }

        composable("study_pack_create") {
            CreateStudyPackScreen(
                navController = navController,
                appState = appState,
                flow6State = flow6State
            )
        }

        composable("study_pack_topics") {
            SelectStudyPackTopicsScreen(
                navController = navController,
                appState = appState,
                flow6State = flow6State
            )
        }

        composable(
            route = "study_pack_preview/{packId}",
            arguments = listOf(navArgument("packId") { type = NavType.IntType })
        ) { entry ->
            StudyPackPreviewScreen(
                navController = navController,
                appState = appState,
                flow6State = flow6State,
                packId = entry.arguments?.getInt("packId") ?: -1
            )
        }

        composable("notifications") {

            NotificationsScreen(
                navController = navController,
                appState = appState
            )
        }


        composable("topics") {

            StudyTopicsScreen(
                navController = navController,
                appState = appState
            )
        }


        composable("topic_filter") {

            TopicFilterScreen(
                navController = navController,
                appState = appState
            )
        }


        composable("add_topic") {

            AddTopicScreen(
                navController = navController,
                appState = appState
            )
        }


        composable(
            route = "edit_topic/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            EditTopicScreen(
                navController = navController,
                appState = appState,
                topicId = topicId
            )
        }


        /* =====================================================
           FLOW 3
           ===================================================== */

        composable(
            route = "topic_detail/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            TopicDetailScreen(
                navController = navController,
                appState = appState,
                flow3State = flow3State,
                topicId = topicId
            )
        }


        composable(
            route = "flashcards/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            FlashcardsScreen(
                navController = navController,
                appState = appState,
                flow3State = flow3State,
                topicId = topicId
            )
        }


        composable(
            route = "resources/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ResourcesScreen(
                navController = navController,
                appState = appState,
                flow3State = flow3State,
                topicId = topicId
            )
        }


        /*
         * IMPORTANT:
         * TopicSettingsScreen does NOT need flow3State.
         */
        composable(
            route = "topic_settings/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            TopicSettingsScreen(
                navController = navController,
                appState = appState,
                topicId = topicId
            )
        }


        /* =====================================================
           FLASHCARDS
           ===================================================== */

        composable(
            route = "add_flashcard/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            AddFlashcardScreen(
                navController = navController,
                flow3State = flow3State,
                topicId = topicId
            )
        }


        composable(
            route =
                "edit_flashcard/{topicId}/{flashcardId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                },

                navArgument("flashcardId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            val flashcardId =
                backStackEntry
                    .arguments
                    ?.getInt("flashcardId")
                    ?: -1

            EditFlashcardScreen(
                navController = navController,
                flow3State = flow3State,
                topicId = topicId,
                flashcardId = flashcardId
            )
        }


        composable(
            route = "flashcard_added/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            LoopiFlowSuccessScreen(
                emoji = "✓",
                title = "Flashcard added!",
                description =
                    "Your new flashcard has been saved.",
                buttonText =
                    "View Flashcards",

                onContinue = {

                    navController.navigate(
                        "flashcards/$topicId"
                    ) {

                        popUpTo(
                            "flashcard_added/$topicId"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        composable(
            route = "flashcard_deleted/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            LoopiFlowSuccessScreen(
                emoji = "✓",
                title = "Flashcard deleted",
                description =
                    "The flashcard was removed successfully.",
                buttonText =
                    "Back to Flashcards",

                onContinue = {

                    navController.navigate(
                        "flashcards/$topicId"
                    ) {

                        popUpTo(
                            "flashcard_deleted/$topicId"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        /* =====================================================
           RESOURCES
           ===================================================== */

        composable(
            route = "add_resource/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            AddResourceScreen(
                navController = navController,
                flow3State = flow3State,
                topicId = topicId
            )
        }


        composable(
            route =
                "resource_detail/{topicId}/{resourceId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                },

                navArgument("resourceId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            val resourceId =
                backStackEntry
                    .arguments
                    ?.getInt("resourceId")
                    ?: -1

            ResourceDetailScreen(
                navController = navController,
                flow3State = flow3State,
                topicId = topicId,
                resourceId = resourceId
            )
        }


        composable(
            route =
                "edit_resource/{topicId}/{resourceId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                },

                navArgument("resourceId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            val resourceId =
                backStackEntry
                    .arguments
                    ?.getInt("resourceId")
                    ?: -1

            EditResourceScreen(
                navController = navController,
                flow3State = flow3State,
                topicId = topicId,
                resourceId = resourceId
            )
        }


        composable(
            route = "resource_added/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            LoopiFlowSuccessScreen(
                emoji = "✓",
                title = "Resource added!",
                description =
                    "Your resource has been saved successfully.",
                buttonText =
                    "View Resources",

                onContinue = {

                    navController.navigate(
                        "resources/$topicId"
                    ) {

                        popUpTo(
                            "resource_added/$topicId"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        composable(
            route = "resource_deleted/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            LoopiFlowSuccessScreen(
                emoji = "✓",
                title = "Resource deleted",
                description =
                    "The resource was removed successfully.",
                buttonText =
                    "Back to Resources",

                onContinue = {

                    navController.navigate(
                        "resources/$topicId"
                    ) {

                        popUpTo(
                            "resource_deleted/$topicId"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        /* =====================================================
           FLOW 4
           QUIZ / REVIEW / CONFIDENCE
           ===================================================== */

        /*
         * 4A / 4B
         * Start Review -> Choose Topic
         */
        composable("review") {

            ChooseReviewTopicScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State
            )
        }


        /*
         * 4C
         * Review Settings
         */
        composable(
            route =
                "review_settings/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewSettingsScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4D
         * Loading Questions
         */
        composable(
            route =
                "review_loading/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewLoadingScreen(
                navController = navController,
                topicId = topicId,
                flow4State = flow4State
            )
        }


        /*
         * 4E
         * Quiz Question
         */
        composable(
            route =
                "review_question/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewQuestionScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4F
         * Correct
         */
        composable(
            route =
                "review_correct/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewCorrectScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4G
         * Incorrect
         */
        composable(
            route =
                "review_incorrect/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewIncorrectScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4H
         * Explanation
         */
        composable(
            route =
                "review_explanation/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewExplanationScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4I - 4L
         * Confidence Check
         */
        composable(
            route =
                "review_confidence/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewConfidenceScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4M
         */
        composable(
            route =
                "review_next/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewNextQuestionScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4N
         */
        composable(
            route =
                "review_mid_progress/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewMidProgressScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4O
         */
        composable(
            route =
                "review_complete/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewCompletedScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4P
         */
        composable(
            route =
                "review_summary/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewSummaryScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4Q
         */
        composable(
            route =
                "review_detailed/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewDetailedResultsScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4R
         */
        composable(
            route =
                "review_incorrect_questions/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewIncorrectQuestionsScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4S
         */
        composable(
            route =
                "review_topic_performance/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewTopicPerformanceScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4T
         */
        composable(
            route =
                "review_confidence_analysis/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewConfidenceAnalysisScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4U
         */
        composable(
            route =
                "review_weak_spots/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewWeakSpotsScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4V
         */
        composable(
            route =
                "review_practice/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewPracticeWeakTopicScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /*
         * 4W
         */
        composable(
            route =
                "review_practice_complete/{topicId}",

            arguments = listOf(

                navArgument("topicId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val topicId =
                backStackEntry
                    .arguments
                    ?.getInt("topicId")
                    ?: -1

            ReviewPracticeCompletedScreen(
                navController = navController,
                flow4State = flow4State,
                topicId = topicId
            )
        }


        /* =====================================================
           FLOW 5 — пока следующий
           ===================================================== */

        composable("progress") {
            Flow5ProgressScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State
            )
        }

        composable(
            route = "progress_topic/{topicId}",
            arguments = listOf(navArgument("topicId") { type = NavType.IntType })
        ) { entry ->
            Flow5TopicProgressScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State,
                topicId = entry.arguments?.getInt("topicId") ?: -1
            )
        }


        composable("profile") {
            Flow5ProfileScreen(
                navController = navController,
                appState = appState,
                flow4State = flow4State
            )
        }

        composable("settings") { Flow6SettingsScreen(navController) }
        composable("help_support") { Flow6HelpSupportScreen(navController) }
    }
}


/* =========================================================
   FLOW 3 SUCCESS SCREEN
   ========================================================= */

@Composable
private fun LoopiFlowSuccessScreen(
    emoji: String,
    title: String,
    description: String,
    buttonText: String,
    onContinue: () -> Unit
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
            text = emoji,
            fontSize = 75.sp,
            color = LoopiColors.Teal
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Text(
            text = title,

            fontSize = 26.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                LoopiColors.Navy,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text = description,

            color =
                Color.Gray,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        Button(
            onClick = onContinue,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {

            Text(
                text = buttonText,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


/* =========================================================
   PLACEHOLDER
   ========================================================= */

@Composable
private fun LoopiPlaceholderScreen(
    title: String,
    message: String,
    navController: NavHostController
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = title,

            fontSize = 27.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                LoopiColors.Navy
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(
            text = message,

            color =
                Color.Gray,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {

                navController.navigate(
                    "home"
                )
            }
        ) {

            Text(
                text =
                    "Back to Home"
            )
        }
    }
}

