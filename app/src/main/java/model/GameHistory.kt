package com.example.tetrisgamegroup11.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity
data class GameHistory(
    @PrimaryKey(autoGenerate = true) val gameId: Int = 0, // Auto-generated ID for each game record
    val score: Int,
    val level: String,
    val timestamp: Long,
    val linesCleared: Int,
    val rank: Int,
    val timestampFormatted: String = formatTimestamp(timestamp)
){
    companion object {
        // Hàm để định dạng timestamp thành chuỗi
        fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy - HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }
}
