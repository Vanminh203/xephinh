package com.example.tetrisgamegroup11.model

import android.graphics.Color
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tetrisgamegroup11.utils.Converters

@Entity
data class GameGrid(
    @PrimaryKey val gridId: String,
    val rows: Int,
    val columns: Int,
    @TypeConverters(Converters::class) var grid: Array<IntArray>,
    @TypeConverters(Converters::class) var colors: Array<IntArray>
) {
    // Method to load data from another GameGrid instance
    fun loadFrom(otherGrid: GameGrid) {
        grid = otherGrid.grid
        colors = otherGrid.colors
    }

    // Method to clear the grid data, setting all cells to zero
    fun clearGrid() {
        for (i in grid.indices) {
            grid[i].fill(0)
            colors[i].fill(Color.TRANSPARENT)
        }
    }

    fun checkAndClearFullRows(): Int {
        var rowsCleared = 0
        for (i in 0 until rows) {
            if (grid[i].all { it == 1 }) {
                clearRow(i)
                rowsCleared++
                Log.d("GameGrid", "Cleared row: $i")
            }
        }
        return rowsCleared
    }

    private fun clearRow(row: Int) {
        for (i in row downTo 1) {
            grid[i] = grid[i - 1].clone()
            colors[i] = colors[i - 1].clone()
        }
        grid[0] = IntArray(columns)
        colors[0] = IntArray(columns) { Color.TRANSPARENT }
    }
}
