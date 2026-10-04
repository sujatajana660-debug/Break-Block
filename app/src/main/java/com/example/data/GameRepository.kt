package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GameRepository(private val gameDao: GameDao) {

    val stats: Flow<GameStatsEntity> = gameDao.getStats().map {
        it ?: GameStatsEntity()
    }

    val allLevels: Flow<List<LevelProgressEntity>> = gameDao.getAllLevelProgress()

    val maxUnlockedLevel: Flow<Int> = gameDao.getMaxCompletedLevel().map {
        (it ?: 0) + 1
    }

    fun getCurrentWeekKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format("%04d-W%02d", year, week)
    }

    fun getCurrentMonthKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    fun getWeeklyScores(): Flow<List<LeaderboardScoreEntity>> {
        return gameDao.getWeeklyScores(getCurrentWeekKey())
    }

    fun getMonthlyScores(): Flow<List<LeaderboardScoreEntity>> {
        return gameDao.getMonthlyScores(getCurrentMonthKey())
    }

    fun getAllTimeScores(): Flow<List<LeaderboardScoreEntity>> {
        return gameDao.getAllTimeScores()
    }

    suspend fun saveClassicScore(score: Int, combo: Int, linesCleared: Int) {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        val newHighScore = maxOf(current.classicHighScore, score)
        val newMaxCombo = maxOf(current.maxCombo, combo)

        gameDao.insertOrUpdateStats(
            current.copy(
                classicHighScore = newHighScore,
                maxCombo = newMaxCombo,
                totalGamesPlayed = current.totalGamesPlayed + 1,
                totalLinesCleared = current.totalLinesCleared + linesCleared
            )
        )

        // Record user score on current week and month leaderboards
        if (score > 0) {
            val weekKey = getCurrentWeekKey()
            val monthKey = getCurrentMonthKey()
            val displayName = if (current.playerName.isNotBlank()) current.playerName else "Guest Player"
            gameDao.insertScore(
                LeaderboardScoreEntity(
                    playerName = displayName,
                    score = score,
                    weekKey = weekKey,
                    monthKey = monthKey,
                    isUser = true,
                    rankTitle = when {
                        score > 4000 -> "Grandmaster"
                        score > 2500 -> "Block Legend"
                        score > 1500 -> "Pro Blaster"
                        else -> "Challenger"
                    },
                    countryFlag = "🇮🇳",
                    isOnlineNow = true,
                    guestIdTag = current.guestId
                )
            )
        }
    }

    suspend fun updateGuestProfile(newName: String, avatar: String) {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        val trimmed = newName.trim().ifEmpty { "Guest Player" }
        gameDao.insertOrUpdateStats(
            current.copy(
                playerName = trimmed,
                avatarEmoji = avatar,
                isGuestLoggedIn = true
            )
        )
        gameDao.updateUserNameInLeaderboard(trimmed)
    }

    suspend fun regenerateGuestId(): String {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        val newGuestId = "GST-" + (1000..9999).random()
        gameDao.insertOrUpdateStats(
            current.copy(
                guestId = newGuestId,
                isGuestLoggedIn = true
            )
        )
        return newGuestId
    }

    suspend fun seedInitialCompetitorsIfEmpty() {
        val count = gameDao.getScoresCount()
        if (count > 0) return

        val weekKey = getCurrentWeekKey()
        val monthKey = getCurrentMonthKey()

        data class Competitor(val name: String, val score: Int, val title: String, val flag: String)
        val sampleCompetitors = listOf(
            Competitor("Aarav_Pro", 5480, "Grandmaster", "🇮🇳"),
            Competitor("Elena_Rox", 4920, "Grandmaster", "🇺🇸"),
            Competitor("Lucas_Silva", 4680, "Grandmaster", "🇧🇷"),
            Competitor("Kenji_Blocks", 4310, "Block Legend", "🇯🇵"),
            Competitor("Sophie_M", 3950, "Block Legend", "🇫🇷"),
            Competitor("Max_Power", 3720, "Block Legend", "🇩🇪"),
            Competitor("Oliver_UK", 3420, "Pro Blaster", "🇬🇧"),
            Competitor("Budi_Santoso", 2980, "Pro Blaster", "🇮🇩"),
            Competitor("Mateo_G", 2650, "Pro Blaster", "🇪🇸"),
            Competitor("Liam_Aus", 2210, "Challenger", "🇦🇺"),
            Competitor("Chloe_Ca", 1890, "Challenger", "🇨🇦"),
            Competitor("Priya_Gamer", 1540, "Challenger", "🇮🇳")
        )

        for (comp in sampleCompetitors) {
            gameDao.insertScore(
                LeaderboardScoreEntity(
                    playerName = comp.name,
                    score = comp.score,
                    weekKey = weekKey,
                    monthKey = monthKey,
                    isUser = false,
                    rankTitle = comp.title,
                    countryFlag = comp.flag,
                    isOnlineNow = true,
                    guestIdTag = "GST-${(1000..9999).random()}"
                )
            )
        }
    }

    suspend fun updateSettings(sound: Boolean, haptic: Boolean) {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        gameDao.insertOrUpdateStats(
            current.copy(
                soundEnabled = sound,
                hapticEnabled = haptic
            )
        )
    }

    suspend fun unlockGate(newGateLevel: Int) {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        val updatedGate = maxOf(current.unlockedGateLevel, newGateLevel)
        gameDao.insertOrUpdateStats(
            current.copy(unlockedGateLevel = updatedGate)
        )
    }

    suspend fun consumeBooster(type: com.example.model.BoosterType): Boolean {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        val updated = when (type) {
            com.example.model.BoosterType.BOMB -> {
                if (current.bombCount <= 0) return false
                current.copy(bombCount = current.bombCount - 1)
            }
            com.example.model.BoosterType.LINE -> {
                if (current.lineBreakerCount <= 0) return false
                current.copy(lineBreakerCount = current.lineBreakerCount - 1)
            }
            com.example.model.BoosterType.PLUS -> {
                if (current.plusBreakerCount <= 0) return false
                current.copy(plusBreakerCount = current.plusBreakerCount - 1)
            }
            com.example.model.BoosterType.UNDO -> {
                if (current.undoCount <= 0) return false
                current.copy(undoCount = current.undoCount - 1)
            }
        }
        gameDao.insertOrUpdateStats(updated)
        return true
    }

    suspend fun addBoosters(bomb: Int = 0, line: Int = 0, plus: Int = 0, undo: Int = 0) {
        val current = stats.firstOrNull() ?: GameStatsEntity()
        gameDao.insertOrUpdateStats(
            current.copy(
                bombCount = current.bombCount + bomb,
                lineBreakerCount = current.lineBreakerCount + line,
                plusBreakerCount = current.plusBreakerCount + plus,
                undoCount = current.undoCount + undo
            )
        )
    }

    suspend fun completeLevel(levelNumber: Int, stars: Int, score: Int) {
        val existing = gameDao.getLevelProgress(levelNumber)
        val bestScore = maxOf(existing?.bestScore ?: 0, score)
        val bestStars = maxOf(existing?.stars ?: 0, stars)
        gameDao.insertOrUpdateLevel(
            LevelProgressEntity(
                levelNumber = levelNumber,
                stars = bestStars,
                isCompleted = true,
                bestScore = bestScore
            )
        )
    }
}
