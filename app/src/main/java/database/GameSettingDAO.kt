package com.example.tetrisgamegroup11.database

import androidx.room.*

@Entity
data class GameSetting(
    @PrimaryKey var id: Int = 0, // ID duy nhất để lưu một hàng cài đặt duy nhất
    var volumeOn: Boolean = true
)

@Dao
interface GameSettingDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSettings(settings: GameSetting)

    @Query("SELECT volumeOn FROM GameSetting WHERE id = 0")
    fun getVolumeSetting(): Boolean?

    @Query("UPDATE GameSetting SET volumeOn = :isVolumeOn WHERE id = 0")
    fun updateVolumeSetting(isVolumeOn: Boolean)
}
