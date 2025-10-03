package com.example.tetrisgamegroup11.model

enum class GameMode {
    CLASSIC,  // Chế độ Tetris cổ điển
    SECRET,   // Không hiển thị khối tiếp theo (Next Piece)
    TARGET;   // Phải đạt điểm mục tiêu trong thời gian giới hạn

    fun getDisplayName(): String {
        return when (this) {
            CLASSIC -> "Classic"
            SECRET -> "Secret"
            TARGET -> "Target"
        }
    }
}
