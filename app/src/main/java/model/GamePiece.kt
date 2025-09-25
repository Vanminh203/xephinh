package com.example.tetrisgamegroup11.model

import android.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tetrisgamegroup11.utils.Converters

@Entity
data class GamePiece(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @TypeConverters(Converters::class) val type: Type,
    @TypeConverters(Converters::class) var shape: Array<IntArray>, // Shape is stored as Array<IntArray>
    var positionX: Int = 3,
    var positionY: Int = 0
) {

    fun rotate() {
        // Logic để xoay khối 90 độ
        val n = shape.size
        val m = shape[0].size
        val newShape = Array(m) { IntArray(n) }
        for (i in shape.indices) {
            for (j in shape[i].indices) {
                newShape[j][n - i - 1] = shape[i][j]
            }
        }
        shape = newShape
    }

    // Enum class for piece types with color mapping
    enum class Type {
        I, J, L, O, S, T, Z;

        fun getColor(): Int {
            return when (this) {
                I -> Color.rgb(242, 0, 0)
                J -> Color.rgb(254, 255, 0)
                L -> Color.rgb(0, 255, 0)
                O -> Color.rgb(0, 153, 68)
                S -> Color.rgb(0, 159, 231)
                T -> Color.rgb(29, 32, 136)
                Z -> Color.rgb(228, 0, 127)
            }
        }
    }

    companion object {
        fun createPiece(type: Type): GamePiece {
            return when (type) {
                Type.I -> GamePiece(type = type, shape = arrayOf(intArrayOf(1, 1, 1, 1)))
                Type.O -> GamePiece(type = type, shape = arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)))
                Type.T -> GamePiece(type = type, shape = arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)))
                Type.S -> GamePiece(type = type, shape = arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)))
                Type.Z -> GamePiece(type = type, shape = arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)))
                Type.L -> GamePiece(type = type, shape = arrayOf(intArrayOf(1, 0), intArrayOf(1, 0), intArrayOf(1, 1)))
                Type.J -> GamePiece(type = type, shape = arrayOf(intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(1, 1)))
            }
        }
    }
}
