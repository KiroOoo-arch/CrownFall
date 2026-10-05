package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.crownfall.realm.data.entities.MoveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoveDao {

    @Insert
    suspend fun insertAll(moves: List<MoveEntity>)

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY moveNumber ASC")
    suspend fun movesFor(gameId: Long): List<MoveEntity>

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY moveNumber ASC")
    fun observeMovesFor(gameId: Long): Flow<List<MoveEntity>>

    @Query("SELECT COUNT(*) FROM moves WHERE gameId = :gameId")
    suspend fun countFor(gameId: Long): Int

    @Query("DELETE FROM moves")
    suspend fun clear()
}
