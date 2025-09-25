package com.example.tetrisgamegroup11.database

import androidx.room.*
import com.example.tetrisgamegroup11.model.GameHistory

@Dao
interface GameHistoryDAO {

    // Lưu điểm vào cơ sở dữ liệu, thay thế nếu có xung đột
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveScore(gameHistory: GameHistory)

    // Lấy tất cả lịch sử chơi, sắp xếp theo điểm > cấp độ > thời gian
    @Query("SELECT * FROM GameHistory ORDER BY score DESC, level DESC, timestamp DESC")
    fun getAllGameHistory(): List<GameHistory>

    // Lấy điểm cao nhất cho một level cụ thể
    @Query("SELECT MAX(score) FROM GameHistory WHERE level = :level")
    fun getHighestScore(level: String): Int?

    // Lấy top lịch sử chơi theo số lượng giới hạn, sắp xếp theo điểm > cấp độ > thời gian
    @Query("SELECT * FROM GameHistory ORDER BY score DESC, level DESC, timestamp DESC LIMIT :limit")
    fun getTopGameHistory(limit: Int): List<GameHistory>

    // Lấy số dòng xóa nhiều nhất cho một level cụ thể
    @Query("SELECT MAX(linesCleared) FROM GameHistory WHERE level = :level")
    fun getMaxLinesCleared(level: String): Int?

    // Xóa toàn bộ lịch sử chơi
    @Query("DELETE FROM GameHistory")
    fun clearGameHistory()
}
