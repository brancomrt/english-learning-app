package com.englishlearning.app.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.englishlearning.app.ui.categoryselect.CategorySelectScreen
import com.englishlearning.app.ui.home.HomeScreen
import com.englishlearning.app.ui.learning.LearningSessionScreen
import com.englishlearning.app.ui.lesson.LessonScreen
import com.englishlearning.app.ui.levelselect.LevelSelectScreen
import com.englishlearning.app.ui.onboarding.OnboardingTutorialScreen
import com.englishlearning.app.ui.callan.CallanDrillScreen
import com.englishlearning.app.ui.settings.PrivacyPolicyScreen
import com.englishlearning.app.ui.settings.SettingsScreen
import com.englishlearning.app.ui.vocabulary.VocabularyScreen
import com.englishlearning.app.ui.progress.ProgressScreen
import com.englishlearning.app.ui.srs.SrsReviewScreen
import com.englishlearning.app.ui.summary.SessionSummaryScreen

// ---------------------------------------------------------------------------
// Rotas
// ---------------------------------------------------------------------------

sealed class Screen(val route: String) {
    object Splash        : Screen("splash")
    object Onboarding    : Screen("onboarding")
    object Home          : Screen("home")
    object LevelSelect   : Screen("level_select")
    object CategorySelect : Screen("category_select/{levelId}") {
        fun createRoute(levelId: Int) = "category_select/$levelId"
    }
    object Lesson : Screen("lesson/{levelId}/{categoryId}/{questionType}") {
        fun createRoute(levelId: Int, categoryId: Int, questionType: String) =
            "lesson/$levelId/$categoryId/$questionType"
    }
    object LearningSession : Screen("learning_session/{levelId}/{categoryId}") {
        fun createRoute(levelId: Int, categoryId: Int) =
            "learning_session/$levelId/$categoryId"
    }
    object SessionSummary : Screen("session_summary")
    object Progress       : Screen("progress")
    object Settings       : Screen("settings")
    object SrsReview      : Screen("srs_review")
    object PrivacyPolicy  : Screen("privacy_policy")
    object Vocabulary     : Screen("vocabulary")
    object CallanDrill    : Screen("callan_drill")
}

private const val PREFS_NAME = "app_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

fun isOnboardingPending(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return !prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
}

// ---------------------------------------------------------------------------
// Grafo de navegação
// ---------------------------------------------------------------------------

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // Splash — redireciona imediatamente, sem UI
        composable(Screen.Splash.route) {
            val context = LocalContext.current
            val showOnboarding = remember { isOnboardingPending(context) }
            LaunchedEffect(Unit) {
                val dest = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingTutorialScreen(
                onTutorialCompleted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Home — usa o NavController do grafo principal para que as rotas internas
        // (level_select, progress, settings, srs_review) sejam resolvidas corretamente
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // Level Select
        composable(Screen.LevelSelect.route) {
            LevelSelectScreen(
                onNavigateToCategorySelect = { levelId ->
                    navController.navigate(Screen.CategorySelect.createRoute(levelId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Category Select
        composable(
            route = Screen.CategorySelect.route,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 0
            CategorySelectScreen(
                levelId = levelId,
                onNavigateToSession = { categoryId, _ ->
                    navController.navigate(Screen.LearningSession.createRoute(levelId, categoryId))
                },
                onNavigateToLesson = { categoryId, questionType ->
                    navController.navigate(
                        Screen.Lesson.createRoute(levelId, categoryId, questionType)
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Lesson (mini-lição antes da sessão)
        composable(
            route = Screen.Lesson.route,
            arguments = listOf(
                navArgument("levelId")      { type = NavType.IntType },
                navArgument("categoryId")   { type = NavType.IntType },
                navArgument("questionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val levelId      = backStackEntry.arguments?.getInt("levelId") ?: 0
            val categoryId   = backStackEntry.arguments?.getInt("categoryId") ?: 0
            val questionType = backStackEntry.arguments?.getString("questionType") ?: ""
            LessonScreen(
                levelId      = levelId,
                categoryId   = categoryId,
                questionType = questionType,
                onStartLesson = {
                    navController.navigate(Screen.LearningSession.createRoute(levelId, categoryId)) {
                        popUpTo(Screen.CategorySelect.createRoute(levelId)) { inclusive = false }
                    }
                },
                onSkipLesson = {
                    navController.navigate(Screen.LearningSession.createRoute(levelId, categoryId)) {
                        popUpTo(Screen.CategorySelect.createRoute(levelId)) { inclusive = false }
                    }
                }
            )
        }

        // Learning Session
        composable(
            route = Screen.LearningSession.route,
            arguments = listOf(
                navArgument("levelId")    { type = NavType.IntType },
                navArgument("categoryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            LearningSessionScreen(
                navController  = navController,
                backStackEntry = backStackEntry
            )
        }

        // Session Summary
        composable(Screen.SessionSummary.route) {
            SessionSummaryScreen(navController = navController)
        }

        // Progress
        composable(Screen.Progress.route) {
            ProgressScreen(navController = navController)
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        // Vocabulary
        composable(Screen.Vocabulary.route) {
            VocabularyScreen(navController = navController)
        }

        // Privacy Policy
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController = navController)
        }

        // SRS Review
        composable(Screen.SrsReview.route) {
            SrsReviewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Callan Drill
        composable(Screen.CallanDrill.route) {
            CallanDrillScreen(navController = navController)
        }
    }
}
