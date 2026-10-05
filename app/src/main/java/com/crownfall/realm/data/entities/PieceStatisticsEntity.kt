package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-piece-type career record, e.g. "Knight - moves 128, captures 37, times captured 19".
 */
@Entity(tableName = "piece_statistics")
data class PieceStatisticsEntity(
    @PrimaryKey val pieceType: String,
    val medievalName: String,
    val gamesUsed: Int = 0,
    val moves: Int = 0,
    val captures: Int = 0,
    val deaths: Int = 0
)
