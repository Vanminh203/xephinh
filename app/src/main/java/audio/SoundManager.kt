package com.example.tetrisgamegroup11.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import com.example.tetrisgamegroup11.R

class SoundManager private constructor(context: Context) {

    private var backgroundMusic: MediaPlayer? = MediaPlayer.create(context, R.raw.background_music).apply {
        isLooping = true
    }
    private var lineClearSound: MediaPlayer? = MediaPlayer.create(context, R.raw.line_clear_sound)
    private var gameOverSound: MediaPlayer? = MediaPlayer.create(context, R.raw.game_over)
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
            instance?.let {
                if (!it.isMuted && it.backgroundMusic?.isPlaying == false) {
                    it.backgroundMusic?.start()
                }
            }
        }

        fun pauseBackgroundMusic() {
            instance?.backgroundMusic?.pause()
        }

        fun playLineClearSound() {
            instance?.lineClearSound?.start() // Không kiểm tra `isMuted` trước khi phát âm thanh
        }


        fun playGameOverSound() {
            instance?.gameOverSound?.start()
        }

        fun toggleMute() {
            instance?.let {
                val newMutedState = !it.isMuted
                it.preferences.edit().putBoolean("isMuted", newMutedState).apply()

                if (newMutedState) {
                    pauseBackgroundMusic()
                } else {
                    playBackgroundMusic()
                }
            }
        }
        private var isMuted = false
        fun setMute(mute: Boolean) {
            isMuted = mute
            if (isMuted) {
                pauseBackgroundMusic()
            } else {
                playBackgroundMusic()
            }
        }

        fun isMuted(): Boolean {
            return instance?.isMuted ?: false
        }

        fun release() {
            instance?.backgroundMusic?.release()
            instance?.lineClearSound?.release()
            instance?.gameOverSound?.release()
            instance = null
        }
    }

    init {
        if (isMuted) {
            backgroundMusic?.pause()
        }
    }
}
