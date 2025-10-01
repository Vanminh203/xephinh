package com.example.tetrisgamegroup11.ui.game

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.tetrisgamegroup11.R
import com.example.tetrisgamegroup11.audio.SoundManager
import com.example.tetrisgamegroup11.database.AppDatabase
import com.example.tetrisgamegroup11.manager.GameManager
import com.example.tetrisgamegroup11.model.GameGrid
import com.example.tetrisgamegroup11.model.GameMode
import com.example.tetrisgamegroup11.model.PowerUpType
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity() {
    private var isPaused = false
    private var wasPausedBeforeDialog = false
    private lateinit var gameManager: GameManager
    private lateinit var gameView: GameView
    private lateinit var volumeButton: ImageButton
    private lateinit var levelTextView: TextView
    private lateinit var nextPieceView: NextPieceView
    private lateinit var scoreTextView: TextView
    private lateinit var linesClearedTextView: TextView
    private lateinit var gameContainer: ConstraintLayout

    private var powerUpDialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        linesClearedTextView = findViewById(R.id.tv_line)
        scoreTextView = findViewById(R.id.tv_score)
        nextPieceView = findViewById(R.id.next_piece_container)
        gameContainer = findViewById(R.id.game_grid_container)

        val appDatabase = AppDatabase.getDatabase(this)
        val gamePieceDAO = appDatabase.gamePieceDAO()
        val gameGridDAO = appDatabase.gameGridDAO()
        val gameSettingDAO = appDatabase.gameSettingDAO()
        val gameHistoryDAO = appDatabase.gameHistoryDAO()

        CoroutineScope(Dispatchers.IO).launch {
            val isMuted = gameSettingDAO.getVolumeSetting() ?: false
            withContext(Dispatchers.Main) {
                SoundManager.setMute(isMuted)
            }
        }

        SoundManager.playBackgroundMusic()

        val selectedLevel = intent.getStringExtra("LEVEL") ?: "Easy"
        val selectedMode = intent.getStringExtra("MODE") ?: "CLASSIC"
        val gameMode = GameMode.valueOf(selectedMode)

        val levelValue = when (selectedLevel) {
            "Easy" -> 1
            "Medium" -> 2
            "Hard" -> 3
            else -> 1
        }

        val gameGrid = GameGrid(
            gridId = "default",
            rows = 20,
            columns = 10,
            grid = Array(20) { IntArray(10) },
            colors = Array(20) { IntArray(10) { Color.TRANSPARENT } }
        )

        gameView = GameView(this)

        gameManager = GameManager(
            gameView = gameView,
            gameGrid = gameGrid,
            level = selectedLevel,
            context = this
        )

        gameManager.gameMode = gameMode

        gameManager.onLinesClearedUpdated = { linesCleared ->
            updateLinesCleared(linesCleared)
        }

        gameManager.onScoreUpdated = { score ->
            updateScore(score)
        }

        gameManager.onPowerUpStateChanged = { type, powerUp ->
            updatePowerUpDialogButtons()
        }

        gameManager.onVisualEffect = { effectType ->
            showVisualEffect(effectType)
        }

        gameView.setGameManager(gameManager)
        nextPieceView.updateNextPiece(gameManager.nextPiece)

        gameManager.onNewPieceGenerated = { nextPiece ->
            nextPieceView.updateNextPiece(nextPiece)
        }

        findViewById<ConstraintLayout>(R.id.game_grid_container)?.addView(gameView)
        levelTextView = findViewById(R.id.tv_level)
        levelTextView.text = selectedLevel

        setupButtonListeners()
        setupPowerUpFab()
        updateVolumeIcon()

        startCooldownUpdates()
    }

    private fun resetPauseButton() {
        isPaused = false
        findViewById<ImageButton>(R.id.btn_pause).setImageResource(R.drawable.ic_pause)
    }

    private fun setupButtonListeners() {
        findViewById<ImageButton>(R.id.btn_pause).setOnClickListener {
            togglePause()
        }

        findViewById<ImageButton>(R.id.btn_close_game).setOnClickListener {
            showExitDialog()
        }

        volumeButton = findViewById(R.id.btn_volume)
        volumeButton.setOnClickListener {
            SoundManager.toggleMute()
            val appDatabase = AppDatabase.getDatabase(this)
            CoroutineScope(Dispatchers.IO).launch {
                appDatabase.gameSettingDAO().updateVolumeSetting(SoundManager.isMuted())
            }
            updateVolumeIcon()
        }

        findViewById<ImageButton>(R.id.btn_restart).setOnClickListener {
            gameManager.resetGame()
            updateScore(0)
            updateLinesCleared(0)
            updateLevel(levelTextView.text.toString())
            resetPauseButton()
        }
        findViewById<Button>(R.id.btn_left).setOnClickListener {
            gameManager.movePieceLeft()
            gameView.invalidate()
        }

        findViewById<Button>(R.id.btn_right).setOnClickListener {
            gameManager.movePieceRight()
            gameView.invalidate()
        }

        findViewById<Button>(R.id.btn_down).setOnClickListener {
            gameManager.movePieceDown()
            gameView.invalidate()
        }

        findViewById<Button>(R.id.btn_rotate).setOnClickListener {
            gameManager.rotatePiece()
            gameView.invalidate()
        }
    }

    private fun setupPowerUpFab() {
        findViewById<FloatingActionButton>(R.id.fab_powerup).setOnClickListener {
            showPowerUpDialog()
        }
    }

    private fun showPowerUpDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_powerup_menu, null)
        dialogBuilder.setView(dialogView)

        powerUpDialog = dialogBuilder.create()

        // Defense skills
        dialogView.findViewById<Button>(R.id.btn_invert).setOnClickListener {
            usePowerUp(PowerUpType.REVERSE_GRAVITY)
        }

        dialogView.findViewById<Button>(R.id.btn_next).setOnClickListener {
            usePowerUp(PowerUpType.RANDOM_NEXT_PIECE)
        }

        dialogView.findViewById<Button>(R.id.btn_slow_time).setOnClickListener {
            usePowerUp(PowerUpType.FREEZE_TIME)
        }

        // Attack skills
        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            usePowerUp(PowerUpType.DELETE_BOTTOM_ROW)
        }

        dialogView.findViewById<Button>(R.id.btn_convert).setOnClickListener {
            usePowerUp(PowerUpType.SWITCH_NEXT_PIECE)
        }

        dialogView.findViewById<Button>(R.id.btn_line_bomb).setOnClickListener {
            usePowerUp(PowerUpType.EXPLODING_PIECE)
        }

        dialogView.findViewById<Button>(R.id.btn_close_menu).setOnClickListener {
            powerUpDialog?.dismiss()
        }

        updatePowerUpDialogButtons()

        powerUpDialog?.show()
    }

    private fun updatePowerUpDialogButtons() {
        powerUpDialog?.let { dialog ->
            val dialogView = dialog.window?.decorView

            updateDialogButton(dialogView, R.id.btn_invert, PowerUpType.REVERSE_GRAVITY)
            updateDialogButton(dialogView, R.id.btn_next, PowerUpType.RANDOM_NEXT_PIECE)
            updateDialogButton(dialogView, R.id.btn_slow_time, PowerUpType.FREEZE_TIME)
            updateDialogButton(dialogView, R.id.btn_delete, PowerUpType.DELETE_BOTTOM_ROW)
            updateDialogButton(dialogView, R.id.btn_convert, PowerUpType.SWITCH_NEXT_PIECE)
            updateDialogButton(dialogView, R.id.btn_line_bomb, PowerUpType.EXPLODING_PIECE)
        }
    }

    private fun updateDialogButton(dialogView: View?, buttonId: Int, type: PowerUpType) {
        dialogView?.findViewById<Button>(buttonId)?.let { button ->
            val powerUp = gameManager.powerUps[type]
            val canUse = powerUp?.canUse(System.currentTimeMillis()) ?: false
            button.isEnabled = canUse
            button.alpha = if (canUse) 1.0f else 0.5f

            if (!canUse) {
                val remainingSeconds = (powerUp?.getRemainingCooldown(System.currentTimeMillis()) ?: 0) / 1000
                val originalText = button.text.toString().split("\n")[0]
                button.text = "$originalText\n(${remainingSeconds}s)"
            }
        }
    }

    private fun usePowerUp(type: PowerUpType) {
        if (gameManager.usePowerUp(type)) {
            Toast.makeText(this, "${type.getDisplayName()} activated!", Toast.LENGTH_SHORT).show()
            powerUpDialog?.dismiss()
        } else {
            val powerUp = gameManager.powerUps[type]
            val remainingSeconds = (powerUp?.getRemainingCooldown(System.currentTimeMillis()) ?: 0) / 1000
            Toast.makeText(this, "Cooldown: ${remainingSeconds}s", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCooldownUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updatePowerUpDialogButtons()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun togglePause() {
        isPaused = !isPaused
        val pauseButton = findViewById<ImageButton>(R.id.btn_pause)
        gameManager.togglePause()
        pauseButton.setImageResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause)
    }

    private fun showExitDialog() {
        wasPausedBeforeDialog = isPaused
        gameManager.pauseGame()

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_confirmation, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.btn_yes).setOnClickListener {
            dialog.dismiss()
            saveGameState()
            finish()
        }

        dialogView.findViewById<Button>(R.id.btn_resume).setOnClickListener {
            dialog.dismiss()
            if (!wasPausedBeforeDialog) {
                gameManager.resumeGame()
                findViewById<ImageButton>(R.id.btn_pause).setImageResource(R.drawable.ic_pause)
            }
        }
        dialog.show()
    }

    private fun updateVolumeIcon() {
        volumeButton.setImageResource(
            if (SoundManager.isMuted()) R.drawable.ic_volume_off else R.drawable.ic_volume_on
        )
    }

    private fun updateScore(score: Int) {
        scoreTextView.text = score.toString()
    }

    private fun updateLinesCleared(linesCleared: Int) {
        linesClearedTextView.text = linesCleared.toString()
    }

    private fun updateLevel(level: String) {
        levelTextView.text = level
    }

    private fun saveGameState() {
        gameManager.saveCurrentGameState()
    }

    fun showGameOverDialog(score: Int) {
        SoundManager.playGameOverSound()

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over, null)
        dialogBuilder.setView(dialogView)

        dialogView.findViewById<TextView>(R.id.tv_score).text = "Your Score: $score"

        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.btn_restart).setOnClickListener {
            dialog.dismiss()
            gameManager.resetGame()
            resetPauseButton()
        }
        dialogView.findViewById<Button>(R.id.btn_quit).setOnClickListener {
            dialog.dismiss()
            saveGameState()
            finish()
        }

        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        saveGameState()
        SoundManager.pauseBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        if (!SoundManager.isMuted()) {
            SoundManager.playBackgroundMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        powerUpDialog?.dismiss()
    }

    private fun showVisualEffect(effectType: String) {
        when (effectType) {
            "explosion" -> {
                // Flash red for explosion
                flashScreen(Color.argb(100, 255, 0, 0), 200)
                Toast.makeText(this, "💥 EXPLOSION!", Toast.LENGTH_SHORT).show()
            }
            "reverse_gravity" -> {
                // Flash blue for reverse gravity
                flashScreen(Color.argb(100, 0, 100, 255), 300)
                Toast.makeText(this, "⬆️ Reverse Gravity Active!", Toast.LENGTH_SHORT).show()
            }
            "reverse_gravity_end" -> {
                Toast.makeText(this, "Gravity Restored", Toast.LENGTH_SHORT).show()
            }
            "freeze_time" -> {
                // Flash white for freeze time
                flashScreen(Color.argb(150, 255, 255, 255), 500)
                Toast.makeText(this, "⏸️ Time Frozen!", Toast.LENGTH_SHORT).show()
            }
            "freeze_time_end" -> {
                Toast.makeText(this, "Time Resumed", Toast.LENGTH_SHORT).show()
            }
            "delete_row" -> {
                flashScreen(Color.argb(80, 255, 255, 0), 200)
            }
            "switch_piece" -> {
                flashScreen(Color.argb(80, 0, 255, 0), 200)
            }
            "exploding_activated" -> {
                Toast.makeText(this, "💣 Exploding Piece Ready!", Toast.LENGTH_SHORT).show()
            }
            "random_piece" -> {
                Toast.makeText(this, "🎲 Next Piece Changed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun flashScreen(color: Int, duration: Long) {
        val flashView = View(this)
        flashView.setBackgroundColor(color)
        flashView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        gameContainer.addView(flashView)

        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = duration
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                gameContainer.removeView(flashView)
            }
        })

        flashView.startAnimation(fadeOut)
    }
}
