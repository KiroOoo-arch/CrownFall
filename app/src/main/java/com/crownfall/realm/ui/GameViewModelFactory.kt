package com.crownfall.realm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.crownfall.realm.di.AppContainer
import com.crownfall.realm.ui.achievements.AchievementsViewModel
import com.crownfall.realm.ui.game.GameViewModel
import com.crownfall.realm.ui.history.HistoryViewModel
import com.crownfall.realm.ui.mainmenu.MainMenuViewModel
import com.crownfall.realm.ui.settings.SettingsViewModel
import com.crownfall.realm.ui.statistics.StatisticsViewModel

/**
 * Minimal ViewModel factory. Each screen receives exactly the collaborators it
 * needs, constructed from the single [AppContainer].
 */
class CrownfallViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> GameViewModel(
                chess = container.chessEngine,
                ai = container.aiEngine,
                animations = container.animationController,
                games = container.gameRepository,
                settings = container.settingsRepository,
                audio = container.audioEngine
            )

            modelClass.isAssignableFrom(MainMenuViewModel::class.java) -> MainMenuViewModel(
                games = container.gameRepository,
                statistics = container.statisticsRepository,
                settings = container.settingsRepository,
                audio = container.audioEngine
            )

            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
                games = container.gameRepository
            )

            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> StatisticsViewModel(
                statistics = container.statisticsRepository
            )

            modelClass.isAssignableFrom(AchievementsViewModel::class.java) -> AchievementsViewModel(
                achievements = container.achievementRepository,
                statistics = container.statisticsRepository
            )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settings = container.settingsRepository,
                statistics = container.statisticsRepository,
                audio = container.audioEngine
            )

            else -> error("Unknown ViewModel $modelClass")
        }
        return viewModel as T
    }
}
