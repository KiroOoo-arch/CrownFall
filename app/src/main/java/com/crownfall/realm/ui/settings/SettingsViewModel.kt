package com.crownfall.realm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.audio.AudioEngine
import com.crownfall.realm.data.entities.SettingsEntity
import com.crownfall.realm.data.repository.AnimationSpeed
import com.crownfall.realm.data.repository.SettingsRepository
import com.crownfall.realm.data.repository.StatisticsRepository
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.audio.SoundEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings screen state. Every change is persisted through the repository and
 * pushed straight into the audio engine so the effect is audible immediately.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val statistics: StatisticsRepository,
    private val audio: AudioEngine
) : ViewModel() {

    val state: StateFlow<SettingsEntity> = settings.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsEntity())

    private fun apply(action: suspend SettingsRepository.() -> Unit, preview: SoundEffect? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { settings.action() }
            preview?.let { audio.play(it) }
        }
    }

    fun setMusicVolume(value: Float) {
        audio.setMusicVolume(value)
        apply({ setMusicVolume(value) })
    }

    fun setSoundVolume(value: Float) {
        audio.setSoundVolume(value)
        apply({ setSoundVolume(value) }, SoundEffect.BUTTON_CLICK)
    }

    fun setCombatSounds(enabled: Boolean) {
        audio.setCombatSoundsEnabled(enabled)
        apply({ setCombatSounds(enabled) }, if (enabled) SoundEffect.SWORD_IMPACT else null)
    }

    fun setCombatAnimation(enabled: Boolean) = apply({ setCombatAnimation(enabled) }, SoundEffect.SWORD_SWING)

    fun setScreenShake(enabled: Boolean) = apply({ setScreenShake(enabled) })

    fun setAnimationSpeed(speed: AnimationSpeed) = apply({ setAnimationSpeed(speed) }, SoundEffect.ARMOR_MOVE)

    fun setHints(enabled: Boolean) = apply({ update { it.copy(showLegalMoveHints = enabled) } })

    fun setLastMove(enabled: Boolean) = apply({ update { it.copy(showLastMove = enabled) } })

    fun setCoordinates(enabled: Boolean) = apply({ update { it.copy(showCoordinates = enabled) } })

    fun setBoardRotation(enabled: Boolean) =
        apply({ update { it.copy(boardRotation = enabled) } }, SoundEffect.ARMOR_MOVE)

    fun setDefaultDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                settings.setDefaultDifficulty(difficulty.name)
                statistics.selectDifficulty(difficulty)
            }
            audio.play(SoundEffect.BUTTON_CLICK)
        }
    }

    fun setPlayerName(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { statistics.updatePlayerName(name) }
        }
    }

    fun previewSword() = audio.play(SoundEffect.SWORD_IMPACT)

    fun previewCheck() = audio.play(SoundEffect.CHECK_WARNING)

    fun previewVictory() = audio.play(SoundEffect.VICTORY_MUSIC)

    fun resetProgress() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                settings.reset()
                statistics.reset()
            }
            val fresh = settings.current()
            audio.setMusicVolume(fresh.musicVolume)
            audio.setSoundVolume(fresh.soundVolume)
            audio.setCombatSoundsEnabled(fresh.combatSounds)
        }
    }
}
