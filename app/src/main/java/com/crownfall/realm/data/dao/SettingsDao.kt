package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun clear()
}
