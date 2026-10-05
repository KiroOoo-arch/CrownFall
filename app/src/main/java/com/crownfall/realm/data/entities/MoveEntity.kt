package com.crownfall.realm.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "moves",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["gameId"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true) val moveId: Long = 0L,
    val gameId: Long,
    val moveNumber: Int,
    val player: String,
    val piece: String,
    val startingPosition: String,
    val destinationPosition: String,
    val capturedPiece: String?,
    val promotion: String?,
    val notation: String,
    val timestamp: Long
)
