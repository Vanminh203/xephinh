package com.example.tetrisgamegroup11.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class GameHistory(
    @PrimaryKey(autoGenerate = true) val gameId: Int = 0,
    val score: Int,
    val level: String,
    val timestamp: Long,
    val linesCleared: Int,
    val rank: Int,
    val gameMode: String = "CLASSIC",
    val playTime: Long = 0L,
)

