package com.crownfall.realm.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.crownfall.realm.data.dao.AchievementDao
import com.crownfall.realm.data.dao.GameDao
import com.crownfall.realm.data.dao.GameStatisticsDao
import com.crownfall.realm.data.dao.MoveDao
import com.crownfall.realm.data.dao.PieceStatisticsDao
import com.crownfall.realm.data.dao.PlayerDao
import com.crownfall.realm.data.dao.SavedGameDao
import com.crownfall.realm.data.dao.SettingsDao
import com.crownfall.realm.data.entities.AchievementEntity
import com.crownfall.realm.data.entities.GameEntity
import com.crownfall.realm.data.entities.GameStatisticsEntity
import com.crownfall.realm.data.entities.MoveEntity
import com.crownfall.realm.data.entities.PieceStatisticsEntity
import com.crownfall.realm.data.entities.PlayerEntity
import com.crownfall.realm.data.entities.SavedGameEntity
import com.crownfall.realm.data.entities.SettingsEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        MoveEntity::class,
        PieceStatisticsEntity::class,
        GameStatisticsEntity::class,
        AchievementEntity::class,
        SettingsEntity::class,
        SavedGameEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CrownfallDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun moveDao(): MoveDao
    abstract fun pieceStatisticsDao(): PieceStatisticsDao
    abstract fun gameStatisticsDao(): GameStatisticsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun settingsDao(): SettingsDao
    abstract fun savedGameDao(): SavedGameDao

    companion object {
        private const val NAME = "crownfall.db"

        /** v1 -> v2 added the "Screen shake" preference without wiping progress. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN screenShake INTEGER NOT NULL DEFAULT 1")
            }
        }

        @Volatile
        private var instance: CrownfallDatabase? = null

        fun get(context: Context): CrownfallDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrownfallDatabase::class.java,
                    NAME
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
