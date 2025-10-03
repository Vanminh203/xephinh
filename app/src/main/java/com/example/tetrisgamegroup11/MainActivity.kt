package com.example.tetrisgamegroup11

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tetrisgamegroup11.audio.SoundManager
import com.example.tetrisgamegroup11.ui.game.GameActivity
import com.example.tetrisgamegroup11.ui.game.RankingActivity

class MainActivity : AppCompatActivity() {
    private lateinit var newGameButton: Button
    private lateinit var selectModeButton: Button
    private lateinit var levelButton: Button
    private lateinit var rankingButton: Button
    private lateinit var volumeButton: ImageButton
    private var selectedMode: String? = null
    private var selectedLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        SoundManager.initialize(this)
        SoundManager.playBackgroundMusic()

        newGameButton = findViewById(R.id.btn_new_game)
        selectModeButton = findViewById(R.id.btn_selectmode)
        levelButton = findViewById(R.id.btn_level)
        rankingButton = findViewById(R.id.btn_ranking)
        volumeButton = findViewById(R.id.btn_volume)

        setupButtonListeners()
        updateVolumeIcon()
    }

    override fun onResume() {
        super.onResume()
        updateVolumeIcon()
        if (!SoundManager.isMuted()) {
            SoundManager.playBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseBackgroundMusic()
    }

    private fun setupButtonListeners() {
        newGameButton.setOnClickListener {
            if (selectedMode == null) {
                Toast.makeText(this, "Bạn phải chọn chế độ trước!", Toast.LENGTH_SHORT).show()
            } else if (selectedLevel == null) {
                showCustomToast()
            } else {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("LEVEL", selectedLevel)
                intent.putExtra("MODE", selectedMode)
                startActivity(intent)
            }
        }

        selectModeButton.setOnClickListener {
            showModeSelectionDialog()
        }

        levelButton.setOnClickListener {
            if (selectedMode == null) {
                Toast.makeText(this, "Bạn phải chọn chế độ trước!", Toast.LENGTH_SHORT).show()
            } else {
                showLevelSelectionDialog()
            }
        }

        rankingButton.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }

        volumeButton.setOnClickListener {
            SoundManager.toggleMute()
            updateVolumeIcon()
        }
    }

    private fun updateVolumeIcon() {
        volumeButton.setImageResource(
            if (SoundManager.isMuted()) R.drawable.ic_volume_off else R.drawable.ic_volume_on
        )
    }

    private fun showCustomToast() {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.main), false)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()
    }

    private fun showModeSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_mode_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val btnClassic = dialog.findViewById<Button>(R.id.btn_classic)
        val btnSecret = dialog.findViewById<Button>(R.id.btn_secret)
        val btnTarget = dialog.findViewById<Button>(R.id.btn_target)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)

        btnClassic.setOnClickListener {
            selectedMode = "CLASSIC"
            selectModeButton.text = "Mode: Classic"
            dialog.dismiss()
        }
        btnSecret.setOnClickListener {
            selectedMode = "SECRET"
            selectModeButton.text = "Mode: Secret"
            dialog.dismiss()
        }
        btnTarget.setOnClickListener {
            selectedMode = "TARGET"
            selectModeButton.text = "Mode: Target"
            dialog.dismiss()
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLevelSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_level_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val btnEasy = dialog.findViewById<Button>(R.id.btn_easy)
        val btnMedium = dialog.findViewById<Button>(R.id.btn_medium)
        val btnHard = dialog.findViewById<Button>(R.id.btn_hard)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)

        btnEasy.setOnClickListener {
            selectedLevel = "Easy"
            levelButton.text = "Level: $selectedLevel"
            dialog.dismiss()
        }
        btnMedium.setOnClickListener {
            selectedLevel = "Medium"
            levelButton.text = "Level: $selectedLevel"
            dialog.dismiss()
        }
        btnHard.setOnClickListener {
            selectedLevel = "Hard"
            levelButton.text = "Level: $selectedLevel"
            dialog.dismiss()
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
