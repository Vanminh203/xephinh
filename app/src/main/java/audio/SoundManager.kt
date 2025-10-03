package com.example.tetrisgamegroup11.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import com.example.tetrisgamegroup11.R

class SoundManager private constructor(context: Context) {

    private var lineClearSound: MediaPlayer? = MediaPlayer.create(context, R.raw.line_clear_sound)
    private var gameOverSound: MediaPlayer? = MediaPlayer.create(context, R.raw.lose_sound)
    private var winSound: MediaPlayer? = MediaPlayer.create(context, R.raw.win_sound)
    private var attackSound: MediaPlayer? = MediaPlayer.create(context, R.raw.attack_sound)
    private var defenseSound: MediaPlayer? = MediaPlayer.create(context, R.raw.defense_sound)
    private var timeoutWarningSound: MediaPlayer? = MediaPlayer.create(context, R.raw.timeout_warning)

    private val preferences: SharedPreferences =
        context.getSharedPreferences("TetrisGamePreferences", Context.MODE_PRIVATE)

    private val isMuted: Boolean
        get() = preferences.getBoolean("isMuted", false)

    companion object {
        private var instance: SoundManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = SoundManager(context)
            }
        }

        fun playBackgroundMusic() {
            // No background music in game
        }

        fun pauseBackgroundMusic() {
            // No background music in game
        }

        fun playLineClearSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.lineClearSound?.start()
                }
            }
        }

        fun playLoseSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.gameOverSound?.start()
                }
            }
        }

        fun playWinSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.winSound?.start()
                }
            }
        }

        fun playAttackSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.attackSound?.start()
                }
            }
        }

        fun playDefenseSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.defenseSound?.start()
                }
            }
        }

        fun playTimeoutWarningSound() {
            instance?.let {
                if (!it.isMuted) {
                    it.timeoutWarningSound?.start()
                }
            }
        }

        fun toggleMute() {
            instance?.let {
                val newMutedState = !it.isMuted
                it.preferences.edit().putBoolean("isMuted", newMutedState).apply()
            }
        }

        private var isMuted = false
        fun setMute(mute: Boolean) {
            isMuted = mute
        }

        fun isMuted(): Boolean {
            return instance?.isMuted ?: false
        }

        fun release() {
            instance?.lineClearSound?.release()
            instance?.gameOverSound?.release()
            instance?.winSound?.release()
            instance?.attackSound?.release()
            instance?.defenseSound?.release()
            instance?.timeoutWarningSound?.release()
            instance = null
        }
    }
}
