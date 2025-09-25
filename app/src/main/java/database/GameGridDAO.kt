package com.example.tetrisgamegroup11.database

import androidx.room.*
import com.example.tetrisgamegroup11.model.GameGrid

@Dao
interface GameGridDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveGrid(grid: GameGrid)

    @Query("SELECT * FROM GameGrid WHERE gridId = :id LIMIT 1")
    fun getGrid(id: String): GameGrid?

    @Update
    fun updateGrid(grid: GameGrid)

    @Delete
    fun deleteGrid(grid: GameGrid)
}
