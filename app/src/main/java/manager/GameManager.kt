package com.example.tetrisgamegroup11.manager

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.tetrisgamegroup11.audio.SoundManager
import com.example.tetrisgamegroup11.database.AppDatabase
import com.example.tetrisgamegroup11.database.GameSetting
import com.example.tetrisgamegroup11.model.GamePiece
import com.example.tetrisgamegroup11.model.GameGrid
import com.example.tetrisgamegroup11.model.GameHistory
import com.example.tetrisgamegroup11.model.GameMode
import com.example.tetrisgamegroup11.model.PowerUp
import com.example.tetrisgamegroup11.model.PowerUpType
import com.example.tetrisgamegroup11.ui.game.GameActivity
import com.example.tetrisgamegroup11.ui.game.GameView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameManager(
    private val gameView: GameView,
    val gameGrid: GameGrid,
    var level: String,
    context: Context
) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val gameHistoryDAO = database.gameHistoryDAO()
    private val gameGridDAO = database.gameGridDAO()
    private val gamePieceDAO = database.gamePieceDAO()
    private val gameSettingDAO = database.gameSettingDAO()

    var currentPiece: GamePiece = generateNewPiece()
    var nextPiece: GamePiece = generateNewPiece()
    var linesCleared: Int = 0
    var score: Int = 0
    var isPaused: Boolean = false
    var isGameInProcess: Boolean = true

    var gameMode: GameMode = GameMode.CLASSIC
        set(value) {
            field = value
            when (value) {
                GameMode.TARGET -> {
                    // Generate 5 random target lines
                    targetLines.clear()
                    repeat(5) {
                        targetLines.add(Random.nextInt(5, 15))
                    }
                }
                GameMode.MORPH -> {
                    morphCounter = 0
                }
                else -> {}
            }
        }

    private var targetLines: MutableSet<Int> = mutableSetOf()
    private var morphCounter: Int = 0

    val powerUps = mutableMapOf<PowerUpType, PowerUp>()
    private var shieldActive = false
    private var slowTimeActive = false
    private var speedBoostActive = false
    private var slowTimeEndTime = 0L
    private var speedBoostEndTime = 0L

    var onLinesClearedUpdated: ((Int) -> Unit)? = null
    var onScoreUpdated: ((Int) -> Unit)? = null
    var onPowerUpStateChanged: ((PowerUpType, PowerUp) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val fallRunnable = object : Runnable {
        override fun run() {
            if (isGameInProcess && !isPaused) {
                if (gameMode == GameMode.MORPH) {
                    morphCounter++
                    if (morphCounter >= 5) { // Morph every 5 moves
                        morphCurrentPiece()
                        morphCounter = 0
                    }
                }

                checkPowerUpDurations()

                movePieceDown()
            }
            handler.postDelayed(this, getFallSpeed())
        }
    }

    init {
        startFalling()
        initializePowerUps()

        CoroutineScope(Dispatchers.IO).launch {
            loadVolumeSetting()
        }
    }

    private fun startFalling() {
        handler.post(fallRunnable)
    }

    var onNewPieceGenerated: ((GamePiece) -> Unit)? = null

    init {
        onNewPieceGenerated?.invoke(nextPiece)
    }

    private fun getFallSpeed(): Long {
        val currentTime = System.currentTimeMillis()
        var baseSpeed = when (level) {
            "Easy" -> 800L
            "Medium" -> 600L
            "Hard" -> 400L
            else -> 800L
        }

        // Apply slow time effect
        if (slowTimeActive && currentTime < slowTimeEndTime) {
            baseSpeed = (baseSpeed * 1.5).toLong()
        }

        // Apply speed boost effect
        if (speedBoostActive && currentTime < speedBoostEndTime) {
            baseSpeed = (baseSpeed * 0.5).toLong()
        }

        return baseSpeed
    }

    private fun initializePowerUps() {
        PowerUpType.values().forEach { type ->
            powerUps[type] = PowerUp(type)
        }
    }

    private fun morphCurrentPiece() {
        val types = GamePiece.Type.values()
        val newType = types[Random.nextInt(types.size)]
        val newPiece = GamePiece.createPiece(newType)
        currentPiece.shape = newPiece.shape
        currentPiece.type = newType
        gameView.invalidate()
    }

    private fun checkPowerUpDurations() {
        val currentTime = System.currentTimeMillis()

        if (slowTimeActive && currentTime >= slowTimeEndTime) {
            slowTimeActive = false
        }

        if (speedBoostActive && currentTime >= speedBoostEndTime) {
            speedBoostActive = false
        }
    }

    fun usePowerUp(type: PowerUpType): Boolean {
        val powerUp = powerUps[type] ?: return false
        val currentTime = System.currentTimeMillis()

        if (!powerUp.canUse(currentTime)) {
            return false
        }

        powerUp.lastUsedTime = currentTime

        when (type) {
            PowerUpType.SHIELD -> {
                shieldActive = true
                powerUp.activeDuration = 15000L // 15 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    shieldActive = false
                }, powerUp.activeDuration)
            }
            PowerUpType.SLOW_TIME -> {
                slowTimeActive = true
                slowTimeEndTime = currentTime + 10000L // 10 seconds
            }
            PowerUpType.LINE_BOMB -> {
                clearRandomLine()
            }
            PowerUpType.SPEED_BOOST -> {
                speedBoostActive = true
                speedBoostEndTime = currentTime + 8000L // 8 seconds
            }
            PowerUpType.CHAOS_ROTATE -> {
                repeat(Random.nextInt(2, 5)) {
                    currentPiece.rotate()
                }
                gameView.invalidate()
            }
            PowerUpType.JUNK_LINES -> {
                addJunkLines(2)
            }
        }

        onPowerUpStateChanged?.invoke(type, powerUp)
        return true
    }

    private fun clearRandomLine() {
        val nonEmptyLines = mutableListOf<Int>()
        for (i in 0 until gameGrid.rows) {
            if (gameGrid.grid[i].any { it == 1 }) {
                nonEmptyLines.add(i)
            }
        }

        if (nonEmptyLines.isNotEmpty()) {
            val lineToRemove = nonEmptyLines[Random.nextInt(nonEmptyLines.size)]
            gameGrid.grid[lineToRemove].fill(0)
            gameGrid.colors[lineToRemove].fill(Color.TRANSPARENT)

            // Shift down
            for (i in lineToRemove downTo 1) {
                gameGrid.grid[i] = gameGrid.grid[i - 1].clone()
                gameGrid.colors[i] = gameGrid.colors[i - 1].clone()
            }
            gameGrid.grid[0].fill(0)
            gameGrid.colors[0].fill(Color.TRANSPARENT)

            score += 50
            onScoreUpdated?.invoke(score)
            SoundManager.playLineClearSound()
            gameView.invalidate()
        }
    }

    private fun addJunkLines(count: Int) {
        // Shift existing blocks up
        for (i in 0 until gameGrid.rows - count) {
            gameGrid.grid[i] = gameGrid.grid[i + count].clone()
            gameGrid.colors[i] = gameGrid.colors[i + count].clone()
        }

        // Add junk lines at bottom
        for (i in gameGrid.rows - count until gameGrid.rows) {
            val holePosition = Random.nextInt(gameGrid.columns)
            for (j in 0 until gameGrid.columns) {
                if (j != holePosition) {
                    gameGrid.grid[i][j] = 1
                    gameGrid.colors[i][j] = Color.GRAY
                } else {
                    gameGrid.grid[i][j] = 0
                    gameGrid.colors[i][j] = Color.TRANSPARENT
                }
            }
        }
        gameView.invalidate()
    }

    fun getTargetLines(): Set<Int> = targetLines

    fun saveCurrentGameState() {
        CoroutineScope(Dispatchers.IO).launch {
            gamePieceDAO.saveCurrentPiece(currentPiece)
            gameGridDAO.saveGrid(gameGrid)
        }
    }

    fun loadPreviousGameState() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedPiece = gamePieceDAO.getCurrentPiece()
            if (savedPiece != null) {
                currentPiece = savedPiece
            }
            gameGridDAO.getGrid(gameGrid.gridId)?.let { savedGrid ->
                gameGrid.loadFrom(savedGrid)
            }
        }
    }

    fun clearLine() {
        score += 100
        SoundManager.playLineClearSound()
    }

    fun resetGame() {
        currentPiece = generateNewPiece()
        nextPiece = generateNewPiece()
        onNewPieceGenerated?.invoke(nextPiece)
        score = 0
        linesCleared = 0
        onScoreUpdated?.invoke(score)
        onLinesClearedUpdated?.invoke(linesCleared)
        isPaused = false
        isGameInProcess = true
        isGameEnded = false
        gameGrid.clearGrid()
        gameView.reset()
        gameView.invalidate()

        shieldActive = false
        slowTimeActive = false
        speedBoostActive = false
        morphCounter = 0
        initializePowerUps()

        if (gameMode == GameMode.TARGET) {
            targetLines.clear()
            repeat(5) {
                targetLines.add(Random.nextInt(5, 15))
            }
        }
    }

    private var isGameEnded = false

    fun endGame() {
        if (isGameEnded) return

        isGameEnded = true
        isGameInProcess = false
        val timestamp = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val rank = calculateRank(score, level.toString(), timestamp)
            gameHistoryDAO.saveScore(
                GameHistory(
                    score = score,
                    level = level.toString(),
                    linesCleared = linesCleared,
                    timestamp = timestamp,
                    rank = rank
                )
            )
        }
        (gameView.context as? GameActivity)?.showGameOverDialog(score)
    }

    fun pauseGame() {
        isPaused = true
        gameView.pauseGame()
        SoundManager.pauseBackgroundMusic()
    }

    fun resumeGame() {
        isPaused = false
        gameView.resumeGame()
        SoundManager.playBackgroundMusic()
    }

    fun togglePause() {
        if (isPaused) resumeGame() else pauseGame()
    }

    fun movePieceLeft() {
        if (!isPaused && !detectCollision(-1, 0)) {
            currentPiece.positionX--
        }
    }

    fun movePieceRight() {
        if (!isPaused && !detectCollision(1, 0)) {
            currentPiece.positionX++
        }
    }

    fun rotatePiece() {
        if (!isPaused) {
            currentPiece.rotate()
            if (detectCollision(0, 0)) {
                repeat(3) { currentPiece.rotate() }
            }
        }
    }

    fun movePieceDown() {
        if (!isPaused && !detectCollision(0, 1)) {
            currentPiece.positionY++
        } else {
            placePieceInGrid()
            val rowsCleared = gameGrid.checkAndClearFullRows()
            if (rowsCleared > 0) {
                updateScore(rowsCleared)

                if (gameMode == GameMode.TARGET) {
                    checkTargetCompletion(rowsCleared)
                }
            }
            currentPiece = nextPiece
            currentPiece.positionX = (gameGrid.columns - currentPiece.shape[0].size) / 2
            currentPiece.positionY = 0
            nextPiece = generateNewPiece()
            onNewPieceGenerated?.invoke(nextPiece)
            checkGameOver()
        }
        gameView.invalidate()
    }

    private fun checkTargetCompletion(rowsCleared: Int) {
        // Remove cleared target lines and add bonus score
        val iterator = targetLines.iterator()
        while (iterator.hasNext()) {
            val targetLine = iterator.next()
            if (gameGrid.grid[targetLine].all { it == 0 }) {
                iterator.remove()
                score += 500 // Bonus for clearing target line
                onScoreUpdated?.invoke(score)
            }
        }

        // If all targets cleared, generate new ones
        if (targetLines.isEmpty()) {
            repeat(5) {
                targetLines.add(Random.nextInt(5, 15))
            }
            score += 1000 // Big bonus for completing all targets
            onScoreUpdated?.invoke(score)
        }
    }

    private fun detectCollision(deltaX: Int, deltaY: Int): Boolean {
        currentPiece.shape.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell == 1) {
                    val newX = currentPiece.positionX + colIndex + deltaX
                    val newY = currentPiece.positionY + rowIndex + deltaY
                    if (newX < 0 || newX >= gameGrid.columns || newY >= gameGrid.rows) {
                        return true
                    }
                    if (newY >= 0 && gameGrid.grid[newY][newX] == 1) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun placePieceInGrid() {
        currentPiece.shape.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell == 1) {
                    val x = currentPiece.positionX + colIndex
                    val y = currentPiece.positionY + rowIndex
                    if (y >= 0 && y < gameGrid.rows && x >= 0 && x < gameGrid.columns) {
                        gameGrid.grid[y][x] = 1
                        gameGrid.colors[y][x] = currentPiece.type.getColor()
                    }
                }
            }
        }
    }

    fun checkGameOver() {
        if (shieldActive) {
            val protectedRows = 3
            var hasCollisionInProtectedZone = false

            for (row in gameGrid.rows - protectedRows until gameGrid.rows) {
                if (gameGrid.grid[row].any { it == 1 }) {
                    hasCollisionInProtectedZone = true
                    break
                }
            }

            if (hasCollisionInProtectedZone) {
                // Clear protected rows
                for (row in gameGrid.rows - protectedRows until gameGrid.rows) {
                    gameGrid.grid[row].fill(0)
                    gameGrid.colors[row].fill(Color.TRANSPARENT)
                }
                shieldActive = false
                gameView.invalidate()
                return
            }
        }

        if (detectCollision(0, 0)) {
            endGame()
        }
    }

    private fun updateScore(rowsCleared: Int) {
        if (rowsCleared > 0) {
            score += rowsCleared * 100 * rowsCleared
            linesCleared += rowsCleared
            Log.d("GameManager", "Updating linesCleared: $linesCleared")
            onScoreUpdated?.invoke(score)
            onLinesClearedUpdated?.invoke(linesCleared)
            SoundManager.playLineClearSound()
        }
    }

    private fun generateNewPiece(): GamePiece {
        val types = GamePiece.Type.values()
        return GamePiece.createPiece(types[Random.nextInt(types.size)])
    }

    private suspend fun loadVolumeSetting() {
        val isVolumeOn = gameSettingDAO.getVolumeSetting() ?: true
        withContext(Dispatchers.Main) {
            SoundManager.setMute(!isVolumeOn)
        }
    }

    fun updateVolumeSetting(isVolumeOn: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            gameSettingDAO.saveSettings(GameSetting(volumeOn = isVolumeOn))
        }
    }

    private suspend fun calculateRank(score: Int, level: String, timestamp: Long): Int {
        val historyList = withContext(Dispatchers.IO) {
            gameHistoryDAO.getAllGameHistory()
        }

        val sortedList = historyList.sortedWith(
            compareByDescending<GameHistory> { it.score }
                .thenByDescending { it.level }
                .thenByDescending { it.timestamp }
        )

        return sortedList.indexOfFirst { it.score == score && it.level == level && it.timestamp == timestamp } + 1
    }
}
