package com.example.tetrisgamegroup11.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.example.tetrisgamegroup11.model.GameHistory
import com.example.tetrisgamegroup11.model.GameGrid
import com.example.tetrisgamegroup11.model.GamePiece
import com.example.tetrisgamegroup11.utils.Converters

@Database(entities = [GameHistory::class, GameGrid::class, GamePiece::class, GameSetting::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gamePieceDAO(): GamePieceDAO
    abstract fun gameGridDAO(): GameGridDAO
    abstract fun gameHistoryDAO(): GameHistoryDAO
    abstract fun gameSettingDAO(): GameSettingDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tetris_game_database"
                )
                    .fallbackToDestructiveMigration() // Nếu có cấu trúc cũ, xóa bỏ và tạo lại
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
