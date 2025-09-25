package com.example.tetrisgamegroup11.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.tetrisgamegroup11.audio.SoundManager
import com.example.tetrisgamegroup11.database.AppDatabase
import com.example.tetrisgamegroup11.database.GameSetting
import com.example.tetrisgamegroup11.model.GamePiece
import com.example.tetrisgamegroup11.model.GameGrid
import com.example.tetrisgamegroup11.model.GameHistory
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

    var onLinesClearedUpdated: ((Int) -> Unit)? = null
    var onScoreUpdated: ((Int) -> Unit)? = null


    private val handler = Handler(Looper.getMainLooper())
    private val fallRunnable = object : Runnable {
        override fun run() {
            if (isGameInProcess && !isPaused) {
                movePieceDown()
            }
            handler.postDelayed(this, getFallSpeed())
        }
    }

    init {
        startFalling()
        // Sử dụng coroutine để tải âm lượng khi game bắt đầu
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
        return when (level) {
            "Easy" -> 800L
            "Medium" -> 600L
            "Hard" -> 400L
            else -> 800L
        }
    }


    // Lưu trạng thái hiện tại của trò chơi vào cơ sở dữ liệu
    fun saveCurrentGameState() {
        CoroutineScope(Dispatchers.IO).launch {
            gamePieceDAO.saveCurrentPiece(currentPiece)
            gameGridDAO.saveGrid(gameGrid)
        }
    }

    // Tải trạng thái trò chơi từ cơ sở dữ liệu
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
    }

    // Kết thúc trò chơi và lưu vào lịch sử
    private var isGameEnded = false

    fun endGame() {
        if (isGameEnded) return  // Ngăn chặn gọi nhiều lần

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
                repeat(3) { currentPiece.rotate() } // Undo rotation
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
                updateScore(rowsCleared) // Cập nhật điểm và số dòng
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
        if (rowsCleared > 0) { // Chỉ gọi khi có dòng bị xóa
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
        val isVolumeOn = gameSettingDAO.getVolumeSetting() ?: true // hoặc `false` tùy theo ý muốn của bạn
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
            gameHistoryDAO.getAllGameHistory() // Lấy tất cả lịch sử chơi từ cơ sở dữ liệu
        }

        // Sắp xếp danh sách dựa trên các tiêu chí ưu tiên
        val sortedList = historyList.sortedWith(
            compareByDescending<GameHistory> { it.score }
                .thenByDescending { it.level }  // Cấp độ ưu tiên cao hơn nếu điểm bằng nhau
                .thenByDescending { it.timestamp } // Thời gian ưu tiên cao hơn nếu điểm và cấp độ bằng nhau
        )

        // Tìm vị trí của bản ghi hiện tại trong danh sách đã sắp xếp
        return sortedList.indexOfFirst { it.score == score && it.level == level && it.timestamp == timestamp } + 1
    }

}
