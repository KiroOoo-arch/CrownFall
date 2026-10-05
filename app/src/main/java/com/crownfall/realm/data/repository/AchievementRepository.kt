package com.crownfall.realm.data.repository

import com.crownfall.realm.data.dao.AchievementDao
import com.crownfall.realm.data.entities.AchievementEntity
import com.crownfall.realm.data.entities.GameStatisticsEntity
import com.crownfall.realm.domain.achievements.Achievements
import kotlinx.coroutines.flow.Flow

/**
 * Achievements are derived from the lifetime statistics rather than tracked
 * ad hoc: syncing is idempotent and works retroactively, and an unlocked
 * achievement never reverts.
 */
class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val statisticsRepository: StatisticsRepository
) {

    suspend fun ensureSeeded() {
        if (achievementDao.count() == 0) {
            achievementDao.upsertAll(
                Achievements.ALL.map {
                    AchievementEntity(
                        achievementId = it.id,
                        name = it.name,
                        description = it.description,
                        target = it.target
                    )
                }
            )
        }
    }

    fun observeAll(): Flow<List<AchievementEntity>> = achievementDao.observeAll()

    suspend fun all(): List<AchievementEntity> {
        ensureSeeded()
        return achievementDao.getAll()
    }

    /** Recomputes progress from the given aggregates and persists any change. */
    suspend fun syncFromStatistics(stats: GameStatisticsEntity): List<AchievementEntity> {
        ensureSeeded()
        val now = System.currentTimeMillis()
        val existing = achievementDao.getAll().associateBy { it.achievementId }
        val updated = Achievements.ALL.map { definition ->
            val current = existing[definition.id]
            val progress = Achievements.progress(definition, stats).coerceAtMost(definition.target)
            val unlocked = current?.unlocked == true || progress >= definition.target
            AchievementEntity(
                achievementId = definition.id,
                name = definition.name,
                description = definition.description,
                progress = progress,
                target = definition.target,
                unlocked = unlocked,
                unlockedAt = current?.unlockedAt ?: if (unlocked) now else null
            )
        }
        achievementDao.upsertAll(updated)
        return updated
    }

    suspend fun unlockedCount(): Int = achievementDao.getAll().count { it.unlocked }

    suspend fun reset() {
        achievementDao.clear()
        ensureSeeded()
    }
}
