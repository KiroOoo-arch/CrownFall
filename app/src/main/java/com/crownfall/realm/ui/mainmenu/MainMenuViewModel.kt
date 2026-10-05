package com.crownfall.realm.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.audio.AudioEngine
import com.crownfall.realm.data.repository.GameRepository
import com.crownfall.realm.data.repository.SettingsRepository
import com.crownfall.realm.data.repository.StatisticsRepository
import com.crownfall.realm.domain.achievements.Achievements
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.models.GameMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainMenuState(
    val playerName: String = "Realm Champion",
    val rating: Int = 1200,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val difficulty: Difficulty = Difficulty.HARD,
    val hasSavedGame: Boolean = false,
    val savedGameLabel: String = "",
    val savedGameMode: GameMode = GameMode.SINGLE_PLAYER,
    val showDifficultyPicker: Boolean = false,
    val achievementTotal: Int = Achievements.ALL.size
)

/** Drives the main menu: profile summary, difficulty choice and Continue Game. */
class MainMenuViewModel(
    private val games: GameRepository,
    private val statistics: StatisticsRepository,
    private val settings: SettingsRepository,
    private val audio: AudioEngine
) : ViewModel() {

    private val _state = MutableStateFlow(MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { settings.current() }
            _state.update { it.copy(difficulty = Difficulty.fromName(saved.defaultDifficulty)) }
        }
        viewModelScope.launch {
            statistics.observeProfile().collect { profile ->
                _state.update {
                    it.copy(
                        playerName = profile.name,
                        rating = profile.rating,
                        gamesPlayed = profile.gamesPlayed,
                        wins = profile.wins
                    )
                }
            }
        }
        viewModelScope.launch {
            games.observeSavedGame().collect { saved ->
                _state.update {
                    it.copy(
                        hasSavedGame = saved != null,
                        savedGameMode = saved?.mode ?: GameMode.SINGLE_PLAYER,
                        savedGameLabel = saved?.let { game ->
                            val detail = if (game.mode == GameMode.SINGLE_PLAYER) {
                                game.difficulty?.displayName ?: "Local"
                            } else {
                                "Local duel"
                            }
                            "${game.mode.displayName} · $detail · ${game.movesEncoded.split(",").filter { m -> m.isNotBlank() }.size} plies"
                        } ?: ""
                    )
                }
            }
        }
    }

    fun click() = audio.play(SoundEffect.BUTTON_CLICK)

    fun openDifficultyPicker() {
        audio.play(SoundEffect.BUTTON_CLICK)
        _state.update { it.copy(showDifficultyPicker = true) }
    }

    fun dismissDifficultyPicker() = _state.update { it.copy(showDifficultyPicker = false) }

    fun selectDifficulty(difficulty: Difficulty) {
        audio.play(SoundEffect.BUTTON_CLICK)
        _state.update { it.copy(difficulty = difficulty, showDifficultyPicker = false) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                statistics.selectDifficulty(difficulty)
                settings.setDefaultDifficulty(difficulty.name)
            }
        }
    }

    fun discardSavedGame() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { games.clearInProgress() }
        }
    }
}
