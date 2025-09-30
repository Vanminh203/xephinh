package com.example.tetrisgamegroup11.model

enum class GameMode {
    CLASSIC,  // Chế độ Tetris cổ điển
    MORPH,    // Khối có thể biến hình ngẫu nhiên khi rơi
    TARGET;   // Phải xóa các dòng mục tiêu cụ thể

    fun getDisplayName(): String {
        return when (this) {
            CLASSIC -> "Classic"
            MORPH -> "Morph"
            TARGET -> "Target"
        }
    }
}
