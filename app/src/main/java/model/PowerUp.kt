package com.example.tetrisgamegroup11.model

// Các cơ chế phòng thủ và tấn công
enum class PowerUpType {
    // Phòng thủ (Defense)
    SHIELD,        // Bảo vệ 3 dòng dưới cùng khỏi game over
    SLOW_TIME,     // Làm chậm tốc độ rơi 50% trong 10 giây
    LINE_BOMB,     // Xóa ngay lập tức 1 dòng bất kỳ

    // Tấn công (Attack)
    SPEED_BOOST,   // Tăng tốc độ rơi 200% trong 8 giây
    CHAOS_ROTATE,  // Xoay ngẫu nhiên khối hiện tại 3 lần
    JUNK_LINES;    // Thêm 2 dòng rác (có lỗ trống) ở đáy

    fun isDefense(): Boolean {
        return this in listOf(SHIELD, SLOW_TIME, LINE_BOMB)
    }

    fun isAttack(): Boolean {
        return this in listOf(SPEED_BOOST, CHAOS_ROTATE, JUNK_LINES)
    }

    fun getDisplayName(): String {
        return when (this) {
            SHIELD -> "Shield"
            SLOW_TIME -> "Slow Time"
            LINE_BOMB -> "Line Bomb"
            SPEED_BOOST -> "Speed Boost"
            CHAOS_ROTATE -> "Chaos"
            JUNK_LINES -> "Junk Lines"
        }
    }

    fun getCooldown(): Long {
        return when (this) {
            SHIELD -> 30000L      // 30 giây
            SLOW_TIME -> 25000L   // 25 giây
            LINE_BOMB -> 20000L   // 20 giây
            SPEED_BOOST -> 20000L // 20 giây
            CHAOS_ROTATE -> 15000L // 15 giây
            JUNK_LINES -> 25000L  // 25 giây
        }
    }
}

data class PowerUp(
    val type: PowerUpType,
    var isActive: Boolean = false,
    var lastUsedTime: Long = 0L,
    var activeDuration: Long = 0L
) {
    fun canUse(currentTime: Long): Boolean {
        return currentTime - lastUsedTime >= type.getCooldown()
    }

    fun getRemainingCooldown(currentTime: Long): Long {
        val elapsed = currentTime - lastUsedTime
        val cooldown = type.getCooldown()
        return if (elapsed < cooldown) cooldown - elapsed else 0L
    }
}
