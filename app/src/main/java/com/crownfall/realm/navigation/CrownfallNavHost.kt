package com.crownfall.realm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crownfall.realm.di.AppContainer
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.models.GameMode
import com.crownfall.realm.ui.CrownfallViewModelFactory
import com.crownfall.realm.ui.achievements.AchievementsScreen
import com.crownfall.realm.ui.achievements.AchievementsViewModel
import com.crownfall.realm.ui.game.GameScreen
import com.crownfall.realm.ui.game.GameViewModel
import com.crownfall.realm.ui.history.HistoryScreen
import com.crownfall.realm.ui.history.HistoryViewModel
import com.crownfall.realm.ui.mainmenu.MainMenuScreen
import com.crownfall.realm.ui.mainmenu.MainMenuViewModel
import com.crownfall.realm.ui.settings.SettingsScreen
import com.crownfall.realm.ui.settings.SettingsViewModel
import com.crownfall.realm.ui.statistics.StatisticsScreen
import com.crownfall.realm.ui.statistics.StatisticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single navigation graph for the whole game. Every screen gets its own
 * ViewModel built from the [AppContainer] by [CrownfallViewModelFactory].
 */
@Composable
fun CrownfallNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { CrownfallViewModelFactory(container) }

    NavHost(navController = navController, startDestination = Routes.MENU) {

        composable(Routes.MENU) {
            val viewModel: MainMenuViewModel = viewModel(factory = factory)
            MainMenuScreen(
                viewModel = viewModel,
                onStartSinglePlayer = { difficulty ->
                    navController.navigate(Routes.singlePlayer(difficulty))
                },
                onStartTwoPlayer = { navController.navigate(Routes.TWO_PLAYER) },
                onContinue = { navController.navigate(Routes.CONTINUE) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onStatistics = { navController.navigate(Routes.STATISTICS) },
                onAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SINGLE_PLAYER,
            arguments = listOf(navArgument(Routes.ARG_DIFFICULTY) { type = NavType.StringType })
        ) { entry ->
            val difficulty = Difficulty.fromName(entry.arguments?.getString(Routes.ARG_DIFFICULTY))
            GameRoute(
                container = container,
                factory = factory,
                mode = GameMode.SINGLE_PLAYER,
                difficulty = difficulty,
                resume = false,
                onExit = { navController.popBackStack(Routes.MENU, inclusive = false) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.TWO_PLAYER) {
            GameRoute(
                container = container,
                factory = factory,
                mode = GameMode.TWO_PLAYER,
                difficulty = Difficulty.HARD,
                resume = false,
                onExit = { navController.popBackStack(Routes.MENU, inclusive = false) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CONTINUE) {
            GameRoute(
                container = container,
                factory = factory,
                mode = GameMode.CONTINUED,
                difficulty = Difficulty.HARD,
                resume = true,
                onExit = { navController.popBackStack(Routes.MENU, inclusive = false) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(factory = factory)
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.STATISTICS) {
            val viewModel: StatisticsViewModel = viewModel(factory = factory)
            StatisticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.ACHIEVEMENTS) {
            val viewModel: AchievementsViewModel = viewModel(factory = factory)
            AchievementsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Starts (or resumes) a match exactly once per navigation entry, then hands the
 * board to [GameScreen].
 */
@Composable
private fun GameRoute(
    container: AppContainer,
    factory: CrownfallViewModelFactory,
    mode: GameMode,
    difficulty: Difficulty,
    resume: Boolean,
    onExit: () -> Unit,
    onSettings: () -> Unit
) {
    val viewModel: GameViewModel = viewModel(factory = factory)
    var started by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        started = true
        if (resume) {
            viewModel.resumeSavedGame()
        } else {
            val name = withContext(Dispatchers.IO) {
                runCatching { container.statisticsRepository.profile().name }.getOrDefault("Realm Champion")
            }
            viewModel.startNewGame(mode = mode, difficulty = difficulty, playerName = name)
        }
    }

    GameScreen(
        viewModel = viewModel,
        onExitToMenu = {
            viewModel.dismissGameOver()
            onExit()
        },
        onOpenSettings = onSettings
    )
}
