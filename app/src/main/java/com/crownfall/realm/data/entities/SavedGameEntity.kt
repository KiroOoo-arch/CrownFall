package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One resumable match. The board can be rebuilt either from [fen] or, for a
 * complete history, by replaying [movesEncoded].
 */
@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val id: Long = 1L,
    val fen: String,
    val movesEncoded: String,
    val sideToMove: String,
    val gameMode: String,
    val difficulty: String,
    val playerName: String,
    val opponentName: String,
    val dawnTimeMs: Long,
    val duskTimeMs: Long,
    val startedAt: Long,
    val savedAt: Long
)
