package com.example.tetrisgamegroup11.ui.game

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tetrisgamegroup11.MainActivity
import com.example.tetrisgamegroup11.R
import com.example.tetrisgamegroup11.database.AppDatabase
import com.example.tetrisgamegroup11.model.GameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankingActivity : AppCompatActivity() {
    private lateinit var classicContainer: LinearLayout
    private lateinit var secretContainer: LinearLayout
    private lateinit var targetContainer: LinearLayout
    private lateinit var classicSection: View
    private lateinit var secretSection: View
    private lateinit var targetSection: View
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        classicSection = findViewById(R.id.classic_section)
        secretSection = findViewById(R.id.secret_section)
        targetSection = findViewById(R.id.target_section)
        classicContainer = findViewById(R.id.classic_scores_container)
        secretContainer = findViewById(R.id.secret_scores_container)
        targetContainer = findViewById(R.id.target_scores_container)
        btnClose = findViewById(R.id.btn_close)

        btnClose.setOnClickListener {
            val source = intent.getStringExtra("SOURCE")

            if (source == "GAME") {
                // Quay lại GameActivity
                val intent = Intent(this, GameActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } else {
                // Quay lại MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        val selectedMode = intent.getStringExtra("MODE")

        if (selectedMode != null) {
            // Show only the selected mode (from GameActivity)
            showSingleMode(selectedMode)
        } else {
            // Show all modes (from MainActivity)
            showAllModes()
        }
    }

    private fun showSingleMode(mode: String) {
        when (mode) {
            "CLASSIC" -> {
                classicSection.visibility = View.VISIBLE
                secretSection.visibility = View.GONE
                targetSection.visibility = View.GONE
                loadHighScores(GameMode.CLASSIC.name, classicContainer)
            }
            "SECRET" -> {
                classicSection.visibility = View.GONE
                secretSection.visibility = View.VISIBLE
                targetSection.visibility = View.GONE
                loadHighScores(GameMode.SECRET.name, secretContainer)
            }
            "TARGET" -> {
                classicSection.visibility = View.GONE
                secretSection.visibility = View.GONE
                targetSection.visibility = View.VISIBLE
                loadHighScores(GameMode.TARGET.name, targetContainer)
            }
        }
    }

    private fun showAllModes() {
        classicSection.visibility = View.VISIBLE
        secretSection.visibility = View.VISIBLE
        targetSection.visibility = View.VISIBLE

        loadHighScores(GameMode.CLASSIC.name, classicContainer)
        loadHighScores(GameMode.SECRET.name, secretContainer)
        loadHighScores(GameMode.TARGET.name, targetContainer)
    }

    private fun loadHighScores(gameMode: String, container: LinearLayout) {
        val database = AppDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            val scores = database.gameHistoryDAO().getTopGameHistoryByMode(gameMode, 10)

            withContext(Dispatchers.Main) {
                container.removeAllViews()

                if (scores.isEmpty()) {
                    val noScoresView = LayoutInflater.from(this@RankingActivity)
                        .inflate(R.layout.item_no_scores, container, false)
                    container.addView(noScoresView)
                } else {
                    scores.forEachIndexed { index, gameHistory ->
                        val scoreView = LayoutInflater.from(this@RankingActivity)
                            .inflate(R.layout.item_high_score, container, false)

                        scoreView.findViewById<TextView>(R.id.tv_rank).text = "${index + 1}"
                        scoreView.findViewById<TextView>(R.id.tv_score_value).text = gameHistory.score.toString()
                        scoreView.findViewById<TextView>(R.id.tv_level_value).text = gameHistory.level
                        scoreView.findViewById<TextView>(R.id.tv_lines_value).text = gameHistory.linesCleared.toString()
                        scoreView.findViewById<TextView>(R.id.tv_date).text = gameHistory.timestampFormatted

                        container.addView(scoreView)
                    }
                }
            }
        }
    }
}
