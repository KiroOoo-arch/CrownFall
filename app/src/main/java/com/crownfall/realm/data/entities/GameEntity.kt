package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val gameId: Long = 0L,
    val player1: String,
    val player2: String,
    val gameMode: String,
    val difficulty: String,
    val startTime: Long,
    val endTime: Long,
    val winner: String?,
    val result: String,
    val totalMoves: Int
)
