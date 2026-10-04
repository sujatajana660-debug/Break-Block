package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.GameStatsEntity
import com.example.data.LevelProgressEntity
import com.example.model.AdventureCatalog
import com.example.model.AdventureLevel
import com.example.model.BOARD_SIZE
import com.example.model.BlockType
import com.example.model.BoardState
import com.example.model.BoosterType
import com.example.model.ClearResult
import com.example.model.PolyominoCatalog
import com.example.model.PolyominoPiece
import com.example.model.SPECIAL_EMERALD_GREEN
import com.example.model.SPECIAL_GEM_PINK
import com.example.model.SPECIAL_NONE
import com.example.model.SPECIAL_STAR_YELLOW
import com.example.network.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenType {
    MODE_SELECT,
    CLASSIC_PLAY,
    ADVENTURE_MAP,
    ADVENTURE_PLAY,
    LEADERBOARD
}

enum class MascotMood {
    NORMAL,
    THUMBS_UP,
    AMAZED,
    CHEERING,
    RELAXED,
    RECORD
}

data class DragState(
    val pieceIndex: Int = -1,
    val piece: PolyominoPiece? = null,
    val touchOffset: Offset = Offset.Zero,
    val currentPosition: Offset = Offset.Zero,
    val hoveredOrigin: Pair<Int, Int>? = null,
    val isValidPlacement: Boolean = false
)

data class PreviousMove(
    val boardState: BoardState,
    val trayPieces: List<PolyominoPiece?>,
    val score: Int,
    val comboCount: Int,
    val collectedGems: Int = 0,
    val collectedStars: Int = 0,
    val collectedEmeralds: Int = 0,
    val remainingMoves: Int = 0
)

data class GameUiState(
    val currentScreen: ScreenType = ScreenType.MODE_SELECT,
    val boardState: BoardState = BoardState(),
    val trayPieces: List<PolyominoPiece?> = listOf(null, null, null),
    val score: Int = 0,
    val comboCount: Int = 0,
    val totalLinesClearedInSession: Int = 0,
    val dragState: DragState = DragState(),
    val celebrationBanner: String? = null,
    val scorePopup: Pair<Int, String>? = null,
    val mascotMood: MascotMood = MascotMood.NORMAL,
    val isGameOver: Boolean = false,
    val isNewRecord: Boolean = false,
    val isPaused: Boolean = false,
    // Interstitial Ad on Game Over
    val isShowingInterstitialAd: Boolean = false,
    // Adventure specifics
    val activeLevel: AdventureLevel? = null,
    val collectedGems: Int = 0,
    val collectedStars: Int = 0,
    val collectedEmeralds: Int = 0,
    val isLevelWon: Boolean = false,
    val earnedStars: Int = 0,
    val remainingMoves: Int = 38,
    // Booster Items State
    val selectedBooster: BoosterType? = null,
    val canUndo: Boolean = false,
    // 1-Chance Ad Revive per match
    val hasUsedReviveInSession: Boolean = false,
    val isWatchingReviveAd: Boolean = false,
    // 3-Level Automatic Streak Reward
    val showStreakRewardDialog: Boolean = false,
    val streakRewardLevelNumber: Int = 0,
    // Extra Booster Reward Prompt & Rewarded Ad
    val boosterRewardPrompt: BoosterType? = null,
    val isWatchingBoosterAd: Boolean = false,
    val boosterAdTarget: BoosterType? = null,
    // Milestone Gate Lock Dialog & Rewarded Ad (Every 3 levels)
    val gateLockTargetLevel: Int? = null,
    val isWatchingGateAd: Boolean = false,
    // 1-Ad Free Booster Watch per Match
    val usedAdBoostersInSession: Set<BoosterType> = emptySet()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    val soundManager: SoundManager
    val networkMonitor: NetworkMonitor

    val isOnline: StateFlow<Boolean>
    val stats: StateFlow<GameStatsEntity>
    val allLevels: StateFlow<List<LevelProgressEntity>>
    val maxUnlockedLevel: StateFlow<Int>
    val weeklyScores: StateFlow<List<com.example.data.LeaderboardScoreEntity>>
    val monthlyScores: StateFlow<List<com.example.data.LeaderboardScoreEntity>>
    val allTimeScores: StateFlow<List<com.example.data.LeaderboardScoreEntity>>

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var previousMove: PreviousMove? = null
    private var bannerJob: Job? = null
    private var levelsWonStreakCount: Int = 0

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        soundManager = SoundManager(application)
        networkMonitor = NetworkMonitor(application)
        isOnline = networkMonitor.isOnline

        soundManager.startHomeMusic()

        stats = repository.stats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            GameStatsEntity()
        )

        allLevels = repository.allLevels.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        maxUnlockedLevel = repository.maxUnlockedLevel.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            1
        )

        weeklyScores = repository.getWeeklyScores().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        monthlyScores = repository.getMonthlyScores().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allTimeScores = repository.getAllTimeScores().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.seedInitialCompetitorsIfEmpty()
        }

        viewModelScope.launch {
            stats.collect { currentStats ->
                soundManager.isSoundEnabled = currentStats.soundEnabled
                soundManager.isHapticEnabled = currentStats.hapticEnabled
            }
        }
    }

    fun checkInternet() {
        soundManager.playSundarClick()
        networkMonitor.recheck()
    }

    fun pauseGame() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isPaused = false) }
    }

    fun quitGameToMenu() {
        soundManager.playSundarClick()
        val currentScore = _uiState.value.score
        if (currentScore > 0 && _uiState.value.currentScreen == ScreenType.CLASSIC_PLAY) {
            viewModelScope.launch {
                repository.saveClassicScore(
                    currentScore,
                    _uiState.value.comboCount,
                    _uiState.value.totalLinesClearedInSession
                )
            }
        }
        soundManager.startHomeMusic()
        _uiState.update {
            it.copy(
                isPaused = false,
                isGameOver = false,
                isShowingInterstitialAd = false,
                currentScreen = ScreenType.MODE_SELECT,
                selectedBooster = null
            )
        }
    }

    fun navigateTo(screen: ScreenType) {
        soundManager.playSundarClick()
        if (screen == ScreenType.MODE_SELECT || screen == ScreenType.ADVENTURE_MAP || screen == ScreenType.LEADERBOARD) {
            soundManager.startHomeMusic()
        } else {
            soundManager.stopHomeMusic()
        }
        _uiState.update {
            it.copy(
                currentScreen = screen,
                selectedBooster = null,
                gateLockTargetLevel = null,
                isWatchingGateAd = false
            )
        }
    }

    fun startClassicGame() {
        soundManager.playSundarClick()
        soundManager.stopHomeMusic()
        val initialPieces = PolyominoCatalog.generateBatch(0f)
        previousMove = null
        _uiState.update {
            it.copy(
                currentScreen = ScreenType.CLASSIC_PLAY,
                boardState = BoardState(),
                trayPieces = initialPieces,
                score = 0,
                comboCount = 0,
                totalLinesClearedInSession = 0,
                dragState = DragState(),
                celebrationBanner = null,
                scorePopup = null,
                mascotMood = MascotMood.NORMAL,
                isGameOver = false,
                isNewRecord = false,
                isShowingInterstitialAd = false,
                selectedBooster = null,
                canUndo = false,
                hasUsedReviveInSession = false,
                isWatchingReviveAd = false,
                boosterRewardPrompt = null,
                isWatchingBoosterAd = false,
                usedAdBoostersInSession = emptySet()
            )
        }
    }

    // Pick from all needed items: Diamonds, Stars, Emeralds
    private fun getNeededAdventureItem(activeLvl: AdventureLevel, gems: Int, stars: Int, emeralds: Int): Int? {
        val needed = mutableListOf<Int>()
        if (gems < activeLvl.target.pinkGemsRequired) {
            // Give diamond high priority so it spawns frequently
            needed.add(SPECIAL_GEM_PINK)
            needed.add(SPECIAL_GEM_PINK)
        }
        if (stars < activeLvl.target.yellowStarsRequired) {
            needed.add(SPECIAL_STAR_YELLOW)
        }
        if (emeralds < activeLvl.target.emeraldsRequired) {
            needed.add(SPECIAL_EMERALD_GREEN)
        }
        return needed.randomOrNull()
    }

    fun startAdventureLevel(levelNum: Int) {
        soundManager.playSundarClick()

        // Check if level is locked by 3-level milestone gate
        val currentGate = stats.value.unlockedGateLevel
        if (levelNum > currentGate) {
            _uiState.update { it.copy(gateLockTargetLevel = levelNum) }
            return
        }

        soundManager.stopHomeMusic()
        val level = AdventureCatalog.getLevel(levelNum)
        val initialPieces = PolyominoCatalog.generateBatch(
            boardOccupancyRatio = level.initialBoard.occupancyRatio,
            ensurePlaceableAgainst = { level.initialBoard.hasAnyLegalPlacement(it) },
            adventureSpecialItem = getNeededAdventureItem(level, 0, 0, 0)
        )
        previousMove = null
        _uiState.update {
            it.copy(
                currentScreen = ScreenType.ADVENTURE_PLAY,
                activeLevel = level,
                boardState = level.initialBoard,
                trayPieces = initialPieces,
                score = 0,
                comboCount = 0,
                collectedGems = 0,
                collectedStars = 0,
                collectedEmeralds = 0,
                dragState = DragState(),
                celebrationBanner = null,
                scorePopup = null,
                mascotMood = MascotMood.NORMAL,
                isGameOver = false,
                isNewRecord = false,
                isShowingInterstitialAd = false,
                isLevelWon = false,
                earnedStars = 0,
                remainingMoves = level.moveLimit,
                selectedBooster = null,
                canUndo = false,
                hasUsedReviveInSession = false,
                isWatchingReviveAd = false,
                boosterRewardPrompt = null,
                isWatchingBoosterAd = false,
                gateLockTargetLevel = null,
                isWatchingGateAd = false,
                usedAdBoostersInSession = emptySet()
            )
        }
    }

    // 3-LEVEL GATE LOCK HANDLING
    fun requestUnlockGate(levelNum: Int) {
        soundManager.playSundarClick()
        _uiState.update { it.copy(gateLockTargetLevel = levelNum) }
    }

    fun dismissGateLockDialog() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(gateLockTargetLevel = null) }
    }

    fun triggerGateUnlockRewardedAd() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isWatchingGateAd = true) }
    }

    fun completeGateUnlockAd() {
        val target = _uiState.value.gateLockTargetLevel ?: 4
        val newGate = target + 2
        viewModelScope.launch {
            repository.unlockGate(newGate)
        }
        soundManager.playLevelWin()
        showCelebrationBanner("🔓 LEVELS $target TO $newGate UNLOCKED!")
        _uiState.update {
            it.copy(
                isWatchingGateAd = false,
                gateLockTargetLevel = null
            )
        }
        startAdventureLevel(target)
    }

    fun dismissGateAd() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isWatchingGateAd = false) }
    }

    // BOOSTER ACTIONS
    fun selectBooster(type: BoosterType) {
        soundManager.playSundarClick()
        if (_uiState.value.isGameOver || _uiState.value.isPaused) return

        if (type == BoosterType.UNDO) {
            if (stats.value.undoCount > 0 && _uiState.value.canUndo) {
                applyUndo()
            } else if (_uiState.value.canUndo) {
                _uiState.update { it.copy(boosterRewardPrompt = type) }
            } else {
                showCelebrationBanner("No move to undo!")
            }
            return
        }

        if (_uiState.value.selectedBooster == type) {
            // Toggle off
            _uiState.update { it.copy(selectedBooster = null) }
            return
        }

        val count = when (type) {
            BoosterType.BOMB -> stats.value.bombCount
            BoosterType.LINE -> stats.value.lineBreakerCount
            BoosterType.PLUS -> stats.value.plusBreakerCount
            BoosterType.UNDO -> stats.value.undoCount
        }

        if (count > 0) {
            _uiState.update { it.copy(selectedBooster = type) }
            val hint = when (type) {
                BoosterType.BOMB -> "💥 Tap any block to blast 4x4 area!"
                BoosterType.LINE -> "⚡ Tap any row to break full line!"
                BoosterType.PLUS -> "➕ Tap any block to blast in 4 directions!"
                else -> ""
            }
            showCelebrationBanner(hint)
        } else {
            // Prompt player to claim booster via Rewarded Ad!
            _uiState.update { it.copy(boosterRewardPrompt = type) }
        }
    }

    fun cancelBooster() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(selectedBooster = null) }
    }

    fun applyBoardBooster(row: Int, col: Int) {
        val booster = _uiState.value.selectedBooster ?: return
        val currentBoard = _uiState.value.boardState

        when (booster) {
            BoosterType.BOMB -> {
                if (stats.value.bombCount <= 0) return
                viewModelScope.launch { repository.consumeBooster(BoosterType.BOMB) }
                val (newBoard, result) = currentBoard.executeBomb(row, col)
                soundManager.playBombExplosion()
                handleBoosterClearResult(newBoard, result, "💥 BOOM 4x4 BLAST!")
            }
            BoosterType.LINE -> {
                if (stats.value.lineBreakerCount <= 0) return
                viewModelScope.launch { repository.consumeBooster(BoosterType.LINE) }
                val (newBoard, result) = currentBoard.executeLineBreaker(row)
                soundManager.playLineLaser()
                handleBoosterClearResult(newBoard, result, "⚡ LINE DESTROYED!")
            }
            BoosterType.PLUS -> {
                if (stats.value.plusBreakerCount <= 0) return
                viewModelScope.launch { repository.consumeBooster(BoosterType.PLUS) }
                val (newBoard, result) = currentBoard.executePlusBreaker(row, col)
                soundManager.playCrossBlast()
                handleBoosterClearResult(newBoard, result, "➕ CROSS BLAST (+ 4 WAYS)!")
            }
            BoosterType.UNDO -> {}
        }
    }

    private fun handleBoosterClearResult(newBoard: BoardState, result: ClearResult, bannerText: String) {
        val newScore = _uiState.value.score + result.pointsEarned
        val newGems = _uiState.value.collectedGems + result.collectedGems
        val newStars = _uiState.value.collectedStars + result.collectedStars
        val newEmeralds = _uiState.value.collectedEmeralds + result.collectedEmeralds

        val activeLvl = _uiState.value.activeLevel
        var isWon = false
        var starRating = 0
        if (activeLvl != null) {
            val reqGems = activeLvl.target.pinkGemsRequired
            val reqStars = activeLvl.target.yellowStarsRequired
            val reqEmeralds = activeLvl.target.emeraldsRequired
            if (newGems >= reqGems && newStars >= reqStars && newEmeralds >= reqEmeralds) {
                isWon = true
                starRating = 3
                soundManager.playLevelWin()
                viewModelScope.launch {
                    repository.completeLevel(activeLvl.levelNumber, starRating, newScore)
                }
            }
        }

        val canAnyFit = _uiState.value.trayPieces.filterNotNull().any { newBoard.hasAnyLegalPlacement(it) }

        _uiState.update {
            it.copy(
                boardState = newBoard,
                score = newScore,
                collectedGems = newGems,
                collectedStars = newStars,
                collectedEmeralds = newEmeralds,
                selectedBooster = null,
                isGameOver = if (canAnyFit) false else it.isGameOver,
                isLevelWon = isWon,
                earnedStars = starRating,
                mascotMood = MascotMood.CHEERING
            )
        }
        showCelebrationBanner(bannerText)
    }

    fun applyUndo() {
        val prev = previousMove
        if (prev != null && stats.value.undoCount > 0 && _uiState.value.canUndo) {
            viewModelScope.launch {
                repository.consumeBooster(BoosterType.UNDO)
            }
            _uiState.update {
                it.copy(
                    boardState = prev.boardState,
                    trayPieces = prev.trayPieces,
                    score = prev.score,
                    comboCount = prev.comboCount,
                    collectedGems = prev.collectedGems,
                    collectedStars = prev.collectedStars,
                    collectedEmeralds = prev.collectedEmeralds,
                    remainingMoves = prev.remainingMoves,
                    canUndo = false,
                    isGameOver = false,
                    selectedBooster = null
                )
            }
            previousMove = null
            soundManager.playUndoSound()
            showCelebrationBanner("↩️ MOVE UNDONE!")
        } else if (stats.value.undoCount <= 0) {
            _uiState.update { it.copy(boosterRewardPrompt = BoosterType.UNDO) }
        }
    }

    // 1-CHANCE REVIVE PER MATCH
    fun triggerReviveAd() {
        soundManager.playSundarClick()
        if (_uiState.value.hasUsedReviveInSession) return
        _uiState.update { it.copy(isWatchingReviveAd = true) }
    }

    fun completeReviveAd() {
        val revivedBoard = _uiState.value.boardState.executeRevive()
        val currentLvl = _uiState.value.activeLevel
        val freshTray = PolyominoCatalog.generateBatch(
            boardOccupancyRatio = revivedBoard.occupancyRatio,
            ensurePlaceableAgainst = { revivedBoard.hasAnyLegalPlacement(it) },
            adventureSpecialItem = if (currentLvl != null) getNeededAdventureItem(
                currentLvl,
                _uiState.value.collectedGems,
                _uiState.value.collectedStars,
                _uiState.value.collectedEmeralds
            ) else null
        )

        _uiState.update {
            it.copy(
                boardState = revivedBoard,
                trayPieces = freshTray,
                isGameOver = false,
                hasUsedReviveInSession = true,
                isWatchingReviveAd = false,
                isShowingInterstitialAd = false,
                mascotMood = MascotMood.CHEERING,
                remainingMoves = if (it.activeLevel != null && it.remainingMoves <= 0) 10 else it.remainingMoves
            )
        }
        soundManager.playReviveSound()
        showCelebrationBanner("✨ REVIVED! 1 CHANCE USED!")
    }

    fun dismissReviveAd() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isWatchingReviveAd = false) }
    }

    // INTERSTITIAL AD ON GAME OVER
    fun dismissInterstitialAd() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isShowingInterstitialAd = false) }
    }

    fun updateGuestProfile(name: String, avatar: String) {
        soundManager.playSundarClick()
        viewModelScope.launch {
            repository.updateGuestProfile(name, avatar)
        }
        showCelebrationBanner("Profile updated! Welcome, $name")
    }

    fun regenerateGuestId() {
        soundManager.playSundarClick()
        viewModelScope.launch {
            val newId = repository.regenerateGuestId()
            showCelebrationBanner("New Guest ID: $newId")
        }
    }

    // Touch & Drag interactions
    fun onDragStart(pieceIndex: Int, piece: PolyominoPiece, startTouch: Offset) {
        soundManager.playPickUp()
        _uiState.update {
            it.copy(
                selectedBooster = null,
                dragState = DragState(
                    pieceIndex = pieceIndex,
                    piece = piece,
                    touchOffset = startTouch,
                    currentPosition = startTouch,
                    hoveredOrigin = null,
                    isValidPlacement = false
                )
            )
        }
    }

    fun onDragMove(position: Offset, boardBounds: Rect, cellSizePx: Float) {
        val drag = _uiState.value.dragState
        val piece = drag.piece ?: return

        val targetPoint = position.copy(y = position.y - 120f)
        if (!boardBounds.contains(targetPoint)) {
            _uiState.update {
                it.copy(
                    dragState = drag.copy(
                        currentPosition = position,
                        hoveredOrigin = null,
                        isValidPlacement = false
                    )
                )
            }
            return
        }

        val relativeX = targetPoint.x - boardBounds.left
        val relativeY = targetPoint.y - boardBounds.top

        val pieceWidthPx = piece.width * cellSizePx
        val pieceHeightPx = piece.height * cellSizePx

        val originCol = ((relativeX - (pieceWidthPx / 2) + (cellSizePx / 2)) / cellSizePx).toInt()
        val originRow = ((relativeY - (pieceHeightPx / 2) + (cellSizePx / 2)) / cellSizePx).toInt()

        val canPlace = _uiState.value.boardState.canPlacePiece(piece, originRow, originCol)

        _uiState.update {
            it.copy(
                dragState = drag.copy(
                    currentPosition = position,
                    hoveredOrigin = if (canPlace) Pair(originRow, originCol) else null,
                    isValidPlacement = canPlace
                )
            )
        }
    }

    fun onDragEnd() {
        val drag = _uiState.value.dragState
        val piece = drag.piece
        val origin = drag.hoveredOrigin
        val pieceIdx = drag.pieceIndex

        if (piece != null && origin != null && drag.isValidPlacement) {
            soundManager.playDrop()
            val (row, col) = origin

            // Save state for UNDO booster
            previousMove = PreviousMove(
                boardState = _uiState.value.boardState,
                trayPieces = _uiState.value.trayPieces,
                score = _uiState.value.score,
                comboCount = _uiState.value.comboCount,
                collectedGems = _uiState.value.collectedGems,
                collectedStars = _uiState.value.collectedStars,
                collectedEmeralds = _uiState.value.collectedEmeralds,
                remainingMoves = _uiState.value.remainingMoves
            )

            // 1. Place piece
            val placedBoard = _uiState.value.boardState.placePiece(piece, row, col)
            val piecePoints = piece.cellCount

            // 2. Remove used piece from tray
            val updatedTray = _uiState.value.trayPieces.toMutableList()
            if (pieceIdx in 0 until updatedTray.size) {
                updatedTray[pieceIdx] = null
            }

            // 3. Line clear detection
            val clearResult = placedBoard.detectClears()
            var newCombo = _uiState.value.comboCount
            var newScore = _uiState.value.score + piecePoints
            var linesCleared = _uiState.value.totalLinesClearedInSession
            val boardAfterClear: BoardState
            var bannerText: String? = null
            var popup: Pair<Int, String>? = null
            var mood = MascotMood.NORMAL

            if (clearResult.totalLines > 0) {
                newCombo++
                val comboBonus = clearResult.pointsEarned + (newCombo * 20 * clearResult.totalLines)
                newScore += comboBonus
                linesCleared += clearResult.totalLines
                boardAfterClear = placedBoard.executeClear(clearResult)
                soundManager.playLineClear(clearResult.totalLines, newCombo)

                bannerText = when {
                    newCombo >= 8 -> "Combo $newCombo! 🔥"
                    newCombo >= 5 -> "EXCELLENT! 🌟"
                    clearResult.totalLines >= 4 -> "AWESOME! 💥"
                    clearResult.totalLines >= 2 -> "AMAZING! ⚡"
                    newCombo >= 3 -> "INTERESTING!"
                    else -> "RELAXING"
                }
                popup = Pair(comboBonus, bannerText)
                mood = when {
                    newCombo >= 5 || clearResult.totalLines >= 3 -> MascotMood.CHEERING
                    newCombo >= 2 -> MascotMood.THUMBS_UP
                    else -> MascotMood.AMAZED
                }
                showCelebrationBanner(bannerText)
            } else {
                newCombo = 0
                boardAfterClear = placedBoard
            }

            // 4. Adventure collection update
            val newGems = _uiState.value.collectedGems + clearResult.collectedGems
            val newStars = _uiState.value.collectedStars + clearResult.collectedStars
            val newEmeralds = _uiState.value.collectedEmeralds + clearResult.collectedEmeralds

            val activeLvl = _uiState.value.activeLevel

            // SPAWN BOTH DIAMONDS AND STARS DYNAMICALLY on the board blocks!
            var boardWithBonusItems = boardAfterClear
            if (activeLvl != null && clearResult.totalLines > 0) {
                val eligibleCoords = mutableListOf<Pair<Int, Int>>()
                for (r in 0 until BOARD_SIZE) {
                    for (c in 0 until BOARD_SIZE) {
                        val cell = boardWithBonusItems.getCell(r, c)
                        if (cell.blockType != BlockType.EMPTY && cell.blockType != BlockType.STONE && cell.specialItem == SPECIAL_NONE) {
                            eligibleCoords.add(Pair(r, c))
                        }
                    }
                }
                if (eligibleCoords.isNotEmpty()) {
                    val shuffled = eligibleCoords.shuffled()
                    var idx = 0
                    // Spawn Diamond if still needed
                    if (newGems < activeLvl.target.pinkGemsRequired && idx < shuffled.size) {
                        val (r, c) = shuffled[idx++]
                        boardWithBonusItems = boardWithBonusItems.setSpecialItem(r, c, SPECIAL_GEM_PINK)
                    }
                    // Spawn Star if still needed
                    if (newStars < activeLvl.target.yellowStarsRequired && idx < shuffled.size) {
                        val (r, c) = shuffled[idx++]
                        boardWithBonusItems = boardWithBonusItems.setSpecialItem(r, c, SPECIAL_STAR_YELLOW)
                    }
                    // Spawn Emerald if still needed
                    if (newEmeralds < activeLvl.target.emeraldsRequired && idx < shuffled.size) {
                        val (r, c) = shuffled[idx++]
                        boardWithBonusItems = boardWithBonusItems.setSpecialItem(r, c, SPECIAL_EMERALD_GREEN)
                    }
                }
            }

            var isWon = false
            var starRating = 0
            val newRemainingMoves = _uiState.value.remainingMoves
            if (activeLvl != null) {
                val reqGems = activeLvl.target.pinkGemsRequired
                val reqStars = activeLvl.target.yellowStarsRequired
                val reqEmeralds = activeLvl.target.emeraldsRequired
                if (newGems >= reqGems && newStars >= reqStars && newEmeralds >= reqEmeralds) {
                    isWon = true
                    starRating = 3
                    levelsWonStreakCount++
                    soundManager.playLevelWin()
                    viewModelScope.launch {
                        repository.completeLevel(activeLvl.levelNumber, starRating, newScore)
                    }
                }
            }

            val isStreakReward = isWon && (levelsWonStreakCount > 0 && levelsWonStreakCount % 3 == 0)

            // 5. Refill tray if all 3 pieces used
            val nextTray = if (updatedTray.all { it == null }) {
                PolyominoCatalog.generateBatch(
                    boardOccupancyRatio = boardWithBonusItems.occupancyRatio,
                    ensurePlaceableAgainst = { boardWithBonusItems.hasAnyLegalPlacement(it) },
                    adventureSpecialItem = if (activeLvl != null) getNeededAdventureItem(activeLvl, newGems, newStars, newEmeralds) else null
                )
            } else {
                updatedTray
            }

            // 6. Game Over check
            val canAnyFit = nextTray.filterNotNull().any { boardWithBonusItems.hasAnyLegalPlacement(it) }
            val isOver = !canAnyFit && !isWon

            if (isOver) {
                soundManager.playGameOver()
                mood = MascotMood.RECORD
                val isRecord = newScore > (stats.value.classicHighScore)
                if (_uiState.value.currentScreen == ScreenType.CLASSIC_PLAY) {
                    viewModelScope.launch {
                        repository.saveClassicScore(newScore, newCombo, linesCleared)
                    }
                }
                _uiState.update {
                    it.copy(
                        isGameOver = true,
                        isNewRecord = isRecord,
                        isShowingInterstitialAd = true, // SHOW INTERSTITIAL AD ON GAME OVER
                        mascotMood = MascotMood.RECORD
                    )
                }
            }

            _uiState.update {
                it.copy(
                    boardState = boardWithBonusItems,
                    trayPieces = nextTray,
                    score = newScore,
                    comboCount = newCombo,
                    totalLinesClearedInSession = linesCleared,
                    dragState = DragState(),
                    scorePopup = popup,
                    mascotMood = if (isOver) MascotMood.RECORD else mood,
                    collectedGems = newGems,
                    collectedStars = newStars,
                    collectedEmeralds = newEmeralds,
                    isLevelWon = isWon,
                    earnedStars = starRating,
                    remainingMoves = newRemainingMoves,
                    canUndo = true,
                    showStreakRewardDialog = isStreakReward,
                    streakRewardLevelNumber = activeLvl?.levelNumber ?: 0
                )
            }
        } else {
            _uiState.update { it.copy(dragState = DragState()) }
        }
    }

    private fun showCelebrationBanner(text: String) {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            _uiState.update { it.copy(celebrationBanner = text) }
            delay(1600)
            _uiState.update { it.copy(celebrationBanner = null) }
        }
    }

    fun advanceToNextAdventureLevel() {
        val currentLvl = _uiState.value.activeLevel?.levelNumber ?: 1
        val nextLvl = currentLvl + 1
        val isStreakMilestone = (levelsWonStreakCount > 0 && levelsWonStreakCount % 3 == 0)
        if (isStreakMilestone && !_uiState.value.showStreakRewardDialog) {
            _uiState.update { it.copy(showStreakRewardDialog = true, streakRewardLevelNumber = currentLvl) }
            return
        }

        // Check if next level is locked by 3-level milestone gate
        val currentGate = stats.value.unlockedGateLevel
        if (nextLvl > currentGate) {
            _uiState.update { it.copy(gateLockTargetLevel = nextLvl, isLevelWon = false) }
            return
        }

        startAdventureLevel(nextLvl)
    }

    fun triggerStreakRewardedAd() {
        soundManager.playSundarClick()
        claimStreakReward()
    }

    fun dismissBoosterRewardPrompt() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(boosterRewardPrompt = null) }
    }

    // Trigger Rewarded Ad for Claiming Booster
    fun triggerExtraBoosterRewardedAd(type: BoosterType) {
        soundManager.playSundarClick()
        _uiState.update {
            it.copy(
                boosterRewardPrompt = null,
                isWatchingBoosterAd = true,
                boosterAdTarget = type
            )
        }
    }

    fun completeBoosterRewardedAd() {
        val target = _uiState.value.boosterAdTarget ?: BoosterType.BOMB
        awardExtraBooster(target)
        _uiState.update { it.copy(isWatchingBoosterAd = false, boosterAdTarget = null) }
    }

    fun dismissBoosterAd() {
        soundManager.playSundarClick()
        _uiState.update { it.copy(isWatchingBoosterAd = false, boosterAdTarget = null) }
    }

    private fun awardExtraBooster(type: BoosterType) {
        viewModelScope.launch {
            when (type) {
                BoosterType.BOMB -> repository.addBoosters(bomb = 1)
                BoosterType.LINE -> repository.addBoosters(line = 1)
                BoosterType.PLUS -> repository.addBoosters(plus = 1)
                BoosterType.UNDO -> repository.addBoosters(undo = 1)
            }
        }
        soundManager.playReviveSound()
        showCelebrationBanner("✨ +1 ${type.title} UNLOCKED (FREE)!")
        _uiState.update {
            it.copy(
                usedAdBoostersInSession = it.usedAdBoostersInSession + type,
                selectedBooster = if (type != BoosterType.UNDO) type else null
            )
        }
        if (type == BoosterType.UNDO && _uiState.value.canUndo) {
            applyUndo()
        }
    }

    fun claimStreakReward() {
        viewModelScope.launch {
            repository.addBoosters(bomb = 1, line = 1, plus = 1, undo = 1)
        }
        val nextLevelNumber = (_uiState.value.activeLevel?.levelNumber ?: 1) + 1
        _uiState.update { it.copy(showStreakRewardDialog = false) }
        soundManager.playReviveSound()
        showCelebrationBanner("🎉 3-LEVEL STREAK! BOOSTERS UNLOCKED!")
        startAdventureLevel(nextLevelNumber)
    }

    fun dismissStreakRewardDialog() {
        val nextLevelNumber = (_uiState.value.activeLevel?.levelNumber ?: 1) + 1
        _uiState.update { it.copy(showStreakRewardDialog = false) }
        startAdventureLevel(nextLevelNumber)
    }

    fun toggleSound() {
        soundManager.playSundarClick()
        val current = stats.value
        val newSound = !current.soundEnabled
        viewModelScope.launch {
            repository.updateSettings(newSound, current.hapticEnabled)
        }
    }

    fun toggleHaptics() {
        soundManager.playSundarClick()
        val current = stats.value
        val newHaptic = !current.hapticEnabled
        viewModelScope.launch {
            repository.updateSettings(current.soundEnabled, newHaptic)
        }
    }
}
