package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val name: String,
    val description: String,
    val progress: Int = 0,
    val target: Int = 1,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null
) {
    val ratio: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}
