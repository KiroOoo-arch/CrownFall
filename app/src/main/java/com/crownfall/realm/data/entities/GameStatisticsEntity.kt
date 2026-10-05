package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row lifetime aggregates for the local player. Keeping the counters
 * denormalised makes the statistics and achievements screens read a single row.
 */
@Entity(tableName = "game_statistics")
data class GameStatisticsEntity(
    @PrimaryKey val id: Long = 1L,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val totalCaptures: Int = 0,
    val knightCaptures: Int = 0,
    val checkmatesDelivered: Int = 0,
    val winsWithoutQueenLoss: Int = 0,
    val expertWins: Int = 0,
    val masterWins: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val favouritePiece: String = "Foot Soldier",
    val totalMoves: Int = 0
)
