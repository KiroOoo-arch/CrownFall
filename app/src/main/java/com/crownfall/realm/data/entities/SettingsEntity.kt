package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val musicVolume: Float = 0.6f,
    val soundVolume: Float = 0.9f,
    val combatSounds: Boolean = true,
    val combatAnimation: Boolean = true,
    /** Screen shake on heavy impacts. Off makes the battlefield perfectly still. */
    val screenShake: Boolean = true,
    /** SLOW, NORMAL or FAST. */
    val animationSpeed: String = "NORMAL",
    val showLegalMoveHints: Boolean = true,
    val showLastMove: Boolean = true,
    val showCoordinates: Boolean = true,
    val boardRotation: Boolean = false,
    val defaultDifficulty: String = "HARD"
)
