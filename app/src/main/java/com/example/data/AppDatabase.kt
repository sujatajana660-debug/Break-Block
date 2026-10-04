package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val classicHighScore: Int = 0,
    val maxCombo: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalLinesCleared: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    // Boosters (Starting with 5 each for every user as requested)
    val bombCount: Int = 5,
    val lineBreakerCount: Int = 5,
    val plusBreakerCount: Int = 5,
    val undoCount: Int = 5,
    // Guest Profile & Player Name
    val guestId: String = "GST-7842",
    val playerName: String = "Guest Player",
    val avatarEmoji: String = "😎",
    val isGuestLoggedIn: Boolean = true,
    // Milestone Gate Lock: every 3 levels locked, unlocks via Rewarded Ad
    val unlockedGateLevel: Int = 3
)

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val stars: Int = 0,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0
)

@Entity(tableName = "leaderboard_scores")
data class LeaderboardScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val score: Int,
    val weekKey: String, // e.g. "2026-W39"
    val monthKey: String, // e.g. "2026-09"
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean = false,
    val rankTitle: String = "Block Master",
    val countryFlag: String = "🇮🇳",
    val isOnlineNow: Boolean = true,
    val guestIdTag: String = ""
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getStats(): Flow<GameStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: GameStatsEntity)

    @Query("UPDATE leaderboard_scores SET playerName = :newName WHERE isUser = 1")
    suspend fun updateUserNameInLeaderboard(newName: String)

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLevel(level: LevelProgressEntity)

    @Query("SELECT MAX(levelNumber) FROM level_progress WHERE isCompleted = 1")
    fun getMaxCompletedLevel(): Flow<Int?>

    // Leaderboard Queries
    @Query("SELECT * FROM leaderboard_scores WHERE weekKey = :weekKey ORDER BY score DESC LIMIT 50")
    fun getWeeklyScores(weekKey: String): Flow<List<LeaderboardScoreEntity>>

    @Query("SELECT * FROM leaderboard_scores WHERE monthKey = :monthKey ORDER BY score DESC LIMIT 50")
    fun getMonthlyScores(monthKey: String): Flow<List<LeaderboardScoreEntity>>

    @Query("SELECT * FROM leaderboard_scores ORDER BY score DESC LIMIT 50")
    fun getAllTimeScores(): Flow<List<LeaderboardScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: LeaderboardScoreEntity)

    @Query("SELECT COUNT(*) FROM leaderboard_scores")
    suspend fun getScoresCount(): Int
}

@Database(
    entities = [GameStatsEntity::class, LevelProgressEntity::class, LeaderboardScoreEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "break_blocks_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
