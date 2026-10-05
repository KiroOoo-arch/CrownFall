package com.crownfall.realm.di

import android.content.Context
import com.crownfall.realm.audio.AudioEngine
import com.crownfall.realm.audio.SynthesizedAudioEngine
import com.crownfall.realm.data.database.CrownfallDatabase
import com.crownfall.realm.data.repository.AchievementRepository
import com.crownfall.realm.data.repository.GameRepository
import com.crownfall.realm.data.repository.SettingsRepository
import com.crownfall.realm.data.repository.StatisticsRepository
import com.crownfall.realm.domain.ai.AIEngine
import com.crownfall.realm.domain.animation.PieceAnimationController
import com.crownfall.realm.domain.chess.GameEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency container. Small enough that a full DI framework
 * would be noise, but explicit enough that every layer - database, repository,
 * engine, AI, animation, audio - can be swapped independently.
 */
class AppContainer(context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = CrownfallDatabase.get(context)

    val settingsRepository = SettingsRepository(database.settingsDao())

    val statisticsRepository = StatisticsRepository(
        playerDao = database.playerDao(),
        statisticsDao = database.gameStatisticsDao(),
        pieceStatisticsDao = database.pieceStatisticsDao()
    )

    val achievementRepository = AchievementRepository(
        achievementDao = database.achievementDao(),
        statisticsRepository = statisticsRepository
    )

    val gameRepository = GameRepository(
        gameDao = database.gameDao(),
        moveDao = database.moveDao(),
        savedGameDao = database.savedGameDao(),
        statisticsRepository = statisticsRepository,
        achievementRepository = achievementRepository
    )

    /** Synthesised PCM engine; swap for a recorded-asset engine without touching gameplay. */
    val audioEngine: AudioEngine = SynthesizedAudioEngine()

    val chessEngine = GameEngine()

    val aiEngine = AIEngine()

    val animationController = PieceAnimationController()

    /** Creates the default rows (player, settings, achievements, per-piece stats). */
    fun bootstrap() {
        applicationScope.launch {
            settingsRepository.ensureDefaults()
            statisticsRepository.ensureSeeded()
            achievementRepository.ensureSeeded()
            val settings = settingsRepository.current()
            audioEngine.setMusicVolume(settings.musicVolume)
            audioEngine.setSoundVolume(settings.soundVolume)
            audioEngine.setCombatSoundsEnabled(settings.combatSounds)
        }
    }

    fun shutdown() {
        audioEngine.release()
    }
}
