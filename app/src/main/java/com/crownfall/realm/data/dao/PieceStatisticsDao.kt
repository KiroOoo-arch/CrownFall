package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.PieceStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceStatisticsDao {

    @Query("SELECT * FROM piece_statistics")
    suspend fun getAll(): List<PieceStatisticsEntity>

    @Query("SELECT * FROM piece_statistics ORDER BY captures DESC, moves DESC")
    fun observeAll(): Flow<List<PieceStatisticsEntity>>

    @Query("SELECT * FROM piece_statistics WHERE pieceType = :pieceType LIMIT 1")
    suspend fun get(pieceType: String): PieceStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: PieceStatisticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<PieceStatisticsEntity>)

    @Query("DELETE FROM piece_statistics")
    suspend fun clear()
}
