package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val playerId: Long = 1L,
    val name: String,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val draws: Int = 0,
    val rating: Int = 1200,
    val selectedDifficulty: String = "HARD"
)
