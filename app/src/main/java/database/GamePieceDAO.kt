package com.example.tetrisgamegroup11.database

import androidx.room.*
import com.example.tetrisgamegroup11.model.GamePiece

@Dao
interface GamePieceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveCurrentPiece(piece: GamePiece)

    @Query("SELECT * FROM GamePiece ORDER BY id DESC LIMIT 1")
    fun getCurrentPiece(): GamePiece?

    @Update
    fun updatePiece(piece: GamePiece)
}
