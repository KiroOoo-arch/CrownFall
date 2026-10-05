package com.crownfall.realm.data.repository

import com.crownfall.realm.data.dao.SettingsDao
import com.crownfall.realm.data.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Animation pacing selected by the player. */
enum class AnimationSpeed(val multiplier: Float, val displayName: String) {
    SLOW(1.6f, "Slow"),
    NORMAL(1.0f, "Normal"),
    FAST(0.55f, "Fast");

    companion object {
        fun fromName(name: String?): AnimationSpeed =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
}

class SettingsRepository(private val settingsDao: SettingsDao) {

    suspend fun ensureDefaults() {
        if (settingsDao.get() == null) settingsDao.upsert(SettingsEntity())
    }

    fun observe(): Flow<SettingsEntity> = settingsDao.observe().map { it ?: SettingsEntity() }

    suspend fun current(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    suspend fun update(transform: (SettingsEntity) -> SettingsEntity): SettingsEntity {
        val next = transform(settingsDao.get() ?: SettingsEntity())
        val sanitized = next.copy(
            musicVolume = next.musicVolume.coerceIn(0f, 1f),
            soundVolume = next.soundVolume.coerceIn(0f, 1f)
        )
        settingsDao.upsert(sanitized)
        return sanitized
    }

    suspend fun setMusicVolume(volume: Float) = update { it.copy(musicVolume = volume) }

    suspend fun setSoundVolume(volume: Float) = update { it.copy(soundVolume = volume) }

    suspend fun setCombatSounds(enabled: Boolean) = update { it.copy(combatSounds = enabled) }

    suspend fun setCombatAnimation(enabled: Boolean) = update { it.copy(combatAnimation = enabled) }

    suspend fun setScreenShake(enabled: Boolean) = update { it.copy(screenShake = enabled) }

    suspend fun setAnimationSpeed(speed: AnimationSpeed) = update { it.copy(animationSpeed = speed.name) }

    suspend fun setDefaultDifficulty(difficultyName: String) =
        update { it.copy(defaultDifficulty = difficultyName) }

    suspend fun animationSpeed(): AnimationSpeed =
        AnimationSpeed.fromName(current().animationSpeed)

    suspend fun reset() {
        settingsDao.upsert(SettingsEntity())
    }
}
