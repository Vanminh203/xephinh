package com.example.tetrisgamegroup11.utils

import androidx.room.TypeConverter
import com.example.tetrisgamegroup11.model.GamePiece
import org.json.JSONArray

object Converters {

    @JvmStatic
    @TypeConverter
    fun fromArrayToString(array: Array<IntArray>): String {
        val jsonArray = JSONArray()
        for (intArray in array) {
            val innerArray = JSONArray()
            for (item in intArray) {
                innerArray.put(item)
            }
            jsonArray.put(innerArray)
        }
        return jsonArray.toString()
    }

    @JvmStatic
    @TypeConverter
    fun fromStringToArray(value: String): Array<IntArray> {
        val jsonArray = JSONArray(value)
        val array = Array(jsonArray.length()) { IntArray(jsonArray.getJSONArray(0).length()) }
        for (i in 0 until jsonArray.length()) {
            val innerArray = jsonArray.getJSONArray(i)
            for (j in 0 until innerArray.length()) {
                array[i][j] = innerArray.getInt(j)
            }
        }
        return array
    }

    @JvmStatic
    @TypeConverter
    fun fromGamePieceType(type: GamePiece.Type): String = type.name

    @JvmStatic
    @TypeConverter
    fun toGamePieceType(value: String): GamePiece.Type = GamePiece.Type.valueOf(value)
}
