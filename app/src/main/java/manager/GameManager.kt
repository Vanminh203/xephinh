package com.example.tetrisgamegroup11.manager

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
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

    private var gameStartTime: Long = 0
    private var elapsedTime: Long = 0
    private var timerHandler: Handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var hasPlayedWarning = false

    private var pausedStartTime: Long = 0L
    private var timeSpentPaused: Long = 0L

    var gameMode: GameMode = GameMode.CLASSIC
        set(value) {
            field = value
            when (value) {
                GameMode.TARGET -> {
                    targetLines.clear()
                    repeat(5) {
                        targetLines.add(Random.nextInt(5, 15))
                    }
                    startTimer()
                }
                GameMode.SECRET -> {
                    secretCounter = 0
                }
                else -> {
                    startCountUpTimer()
                }
            }
        }

    private var targetLines: MutableSet<Int> = mutableSetOf()
    private var secretCounter: Int = 0

    val powerUps = mutableMapOf<PowerUpType, PowerUp>()
    private var explodingPieceActive = false
    private var reverseGravityActive = false
    private var reverseGravityEndTime = 0L
    private var freezeTimeActive = false
    private var freezeTimeEndTime = 0L

    var onLinesClearedUpdated: ((Int) -> Unit)? = null
    var onScoreUpdated: ((Int) -> Unit)? = null
    var onPowerUpStateChanged: ((PowerUpType, PowerUp) -> Unit)? = null
    var onVisualEffect: ((String) -> Unit)? = null
    var onTimeUpdated: ((Long) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val fallRunnable = object : Runnable {
        override fun run() {
            if (isGameInProcess && !isPaused) {
                if (gameMode == GameMode.SECRET) {
                    secretCounter++
                    if (secretCounter >= 5) {
                        secretMorphPiece()
                        secretCounter = 0
                    }
                }
                checkPowerUpDurations()
                if (!freezeTimeActive) {
                    if (reverseGravityActive) {
                        movePieceUp()
                    } else {
                        movePieceDown()
                    }
                }
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

    private fun startCountUpTimer() {
        gameStartTime = System.currentTimeMillis()
        timerRunnable = object : Runnable {
            override fun run() {
                if (isGameInProcess && !isPaused) {
                    elapsedTime = System.currentTimeMillis() - gameStartTime
                    onTimeUpdated?.invoke(elapsedTime)
                }
                timerHandler.postDelayed(this, 100)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun startTimer() {
        gameStartTime = System.currentTimeMillis()
        hasPlayedWarning = false
        timerRunnable = object : Runnable {
            override fun run() {
                if (isGameInProcess && gameMode == GameMode.TARGET) {

                    if (freezeTimeActive) {
                        onTimeUpdated?.invoke(elapsedTime)
                        timerHandler.postDelayed(this, 100)
                        return
                    }

                    elapsedTime = System.currentTimeMillis() - gameStartTime
                    onTimeUpdated?.invoke(elapsedTime)

                    val remainingTime = 300000L - elapsedTime

                    if (remainingTime <= 30000L && !hasPlayedWarning) {
                        SoundManager.playTimeoutWarningSound()
                        hasPlayedWarning = true
                    }

                    if (elapsedTime >= 300000L) {
                        checkTargetModeResult()
                        return
                    }
                }
                timerHandler.postDelayed(this, 100)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun checkTargetModeResult() {
        val targetScore = when (level) {
            "Easy" -> 300
            "Medium" -> 600
            "Hard" -> 1000
            else -> 300
        }

        if (score >= targetScore) {
            winGame()
        } else {
            endGame()
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
        val baseSpeed = when (level) {
            "Easy" -> 800L
            "Medium" -> 600L
            "Hard" -> 400L
            else -> 800L
        }

        return baseSpeed
    }

    private fun initializePowerUps() {
        PowerUpType.values().forEach { type ->
            powerUps[type] = PowerUp(type)
        }
    }

    private fun secretMorphPiece() {
        val types = GamePiece.Type.values()
        val newType = types[Random.nextInt(types.size)]
        val newPiece = GamePiece.createPiece(newType)
        currentPiece.shape = newPiece.shape
        currentPiece.type = newType
        gameView.invalidate()
    }

    private fun checkPowerUpDurations() {
        val currentTime = System.currentTimeMillis()

        if (reverseGravityActive && currentTime >= reverseGravityEndTime) {
            reverseGravityActive = false
            onVisualEffect?.invoke("reverse_gravity_end")
        }

        if (freezeTimeActive && currentTime >= freezeTimeEndTime) {
            freezeTimeActive = false
            onVisualEffect?.invoke("freeze_time_end")

            if (gameMode == GameMode.TARGET) {
                timeSpentPaused = currentTime - pausedStartTime
                gameStartTime += timeSpentPaused
                timeSpentPaused = 0L
                pausedStartTime = 0L
            }
        }
    }

    private fun movePieceUp() {
        if (!isPaused && !detectCollisionUp(0, -1)) {
            currentPiece.positionY--
        }
        gameView.invalidate()
    }

    private fun detectCollisionUp(deltaX: Int, deltaY: Int): Boolean {
        currentPiece.shape.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell == 1) {
                    val newX = currentPiece.positionX + colIndex + deltaX
                    val newY = currentPiece.positionY + rowIndex + deltaY
                    if (newX < 0 || newX >= gameGrid.columns || newY < 0) {
                        return true
                    }
                    if (newY >= 0 && newY < gameGrid.rows && gameGrid.grid[newY][newX] == 1) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun usePowerUp(type: PowerUpType): Boolean {
        val powerUp = powerUps[type] ?: return false
        val currentTime = System.currentTimeMillis()

        if (!powerUp.canUse(currentTime)) {
            return false
        }

        powerUp.lastUsedTime = currentTime

        when (type) {
            PowerUpType.DELETE_BOTTOM_ROW -> {
                deleteBottomRow()
                SoundManager.playAttackSound()
                onVisualEffect?.invoke("delete_row")
            }
            PowerUpType.SWITCH_NEXT_PIECE -> {
                switchToNextPiece()
                SoundManager.playAttackSound()
                onVisualEffect?.invoke("switch_piece")
            }
            PowerUpType.EXPLODING_PIECE -> {
                explodingPieceActive = true
                SoundManager.playAttackSound()
                onVisualEffect?.invoke("exploding_activated")
            }
            PowerUpType.REVERSE_GRAVITY -> {
                reverseGravityActive = true
                reverseGravityEndTime = currentTime + 3000L
                SoundManager.playDefenseSound()
                onVisualEffect?.invoke("reverse_gravity")
            }
            PowerUpType.RANDOM_NEXT_PIECE -> {
                nextPiece = generateNewPiece()
                onNewPieceGenerated?.invoke(nextPiece)
                gameView.invalidate()
                SoundManager.playDefenseSound()
                onVisualEffect?.invoke("random_piece")
            }
            PowerUpType.FREEZE_TIME -> {
                freezeTimeActive = true
                freezeTimeEndTime = currentTime + 5000L

                if (gameMode == GameMode.TARGET) {
                    pausedStartTime = currentTime
                }

                SoundManager.playDefenseSound()
                onVisualEffect?.invoke("freeze_time")
            }
        }
        onPowerUpStateChanged?.invoke(type, powerUp)
        return true
    }

    private fun deleteBottomRow() {
        val bottomRow = gameGrid.rows - 1
        gameGrid.grid[bottomRow].fill(0)
        gameGrid.colors[bottomRow].fill(Color.TRANSPARENT)

        for (i in bottomRow downTo 1) {
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

    private fun switchToNextPiece() {
        currentPiece = nextPiece
        currentPiece.positionX = (gameGrid.columns - currentPiece.shape[0].size) / 2
        currentPiece.positionY = 0

        nextPiece = generateNewPiece()
        onNewPieceGenerated?.invoke(nextPiece)
        gameView.invalidate()
    }

    private fun explodePiece() {
        val touchedBlocks = mutableSetOf<Pair<Int, Int>>()

        currentPiece.shape.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell == 1) {
                    val x = currentPiece.positionX + colIndex
                    val y = currentPiece.positionY + rowIndex

                    val adjacentCells = listOf(
                        Pair(x, y + 1),
                        Pair(x, y - 1),
                        Pair(x - 1, y),
                        Pair(x + 1, y)
                    )

                    adjacentCells.forEach { (adjX, adjY) ->
                        if (adjX in 0 until gameGrid.columns &&
                            adjY in 0 until gameGrid.rows &&
                            gameGrid.grid[adjY][adjX] == 1) {
                            touchedBlocks.add(Pair(adjX, adjY))
                        }
                    }
                }
            }
        }

        touchedBlocks.forEach { (x, y) ->
            gameGrid.grid[y][x] = 0
            gameGrid.colors[y][x] = Color.TRANSPARENT
        }
        SoundManager.playBoomSound()
        score += touchedBlocks.size * 10
        onScoreUpdated?.invoke(score)
        onVisualEffect?.invoke("explosion")
        gameView.invalidate()
    }

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

        explodingPieceActive = false
        reverseGravityActive = false
        freezeTimeActive = false
        secretCounter = 0
        initializePowerUps()

        if (gameMode == GameMode.TARGET) {
            targetLines.clear()
            repeat(5) {
                targetLines.add(Random.nextInt(5, 15))
            }
            elapsedTime = 0
            hasPlayedWarning = false
            startTimer()
        } else {
            elapsedTime = 0
            startCountUpTimer()
        }
    }

    private var isGameEnded = false

    private fun winGame() {
        if (isGameEnded) return

        isGameEnded = true
        isGameInProcess = false
        timerRunnable?.let { timerHandler.removeCallbacks(it) }

        val timestamp = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val rank = calculateRank(score, level.toString(), timestamp)
            gameHistoryDAO.saveScore(
                GameHistory(
                    score = score,
                    level = level.toString(),
                    linesCleared = linesCleared,
                    timestamp = timestamp,
                    rank = rank,
                    gameMode = gameMode.name
                )
            )
        }
        (gameView.context as? GameActivity)?.showWinDialog(score)
    }

    fun endGame() {
        if (isGameEnded) return

        isGameEnded = true
        isGameInProcess = false
        timerRunnable?.let { timerHandler.removeCallbacks(it) }

        val timestamp = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val rank = calculateRank(score, level.toString(), timestamp)
            gameHistoryDAO.saveScore(
                GameHistory(
                    score = score,
                    level = level.toString(),
                    linesCleared = linesCleared,
                    timestamp = timestamp,
                    rank = rank,
                    gameMode = gameMode.name
                )
            )
        }
        (gameView.context as? GameActivity)?.showGameOverDialog(score)
    }

    fun pauseGame() {
        isPaused = true
        gameView.pauseGame()
    }

    fun resumeGame() {
        isPaused = false
        gameView.resumeGame()
    }

    fun pauseGameLogicOnly() {
        isPaused = true
        gameView.pauseGame()
    }

    fun resumeGameLogicOnly() {
        isPaused = false
        gameView.resumeGame()
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
        if (isPaused) return

        if (!detectCollision(0, 1)) {
            currentPiece.positionY++
        } else {
            if (explodingPieceActive) {
                explodePiece()
                explodingPieceActive = false
            } else {
                placePieceInGrid()
                val rowsCleared = gameGrid.checkAndClearFullRows()
                if (rowsCleared > 0) {
                    updateScore(rowsCleared)
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
        if (detectCollision(0, 0)) {
            endGame()
        }
    }

    private fun updateScore(rowsCleared: Int) {
        if (rowsCleared > 0) {
            score += rowsCleared * 100 * rowsCleared
            linesCleared += rowsCleared
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

    fun isSecretMode(): Boolean {
        return gameMode == GameMode.SECRET
    }
}