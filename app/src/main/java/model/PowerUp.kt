package com.example.tetrisgamegroup11.model

enum class PowerUpType {
    // Tấn công (Attack) - 45s cooldown
    DELETE_BOTTOM_ROW,      // Xóa 1 hàng dưới cùng của bảng
    SWITCH_NEXT_PIECE,      // Đổi sang khối tiếp theo ngay lập tức
    EXPLODING_PIECE,        // Khối nổ - phá hủy tất cả block mà nó chạm vào

    // Phòng thủ (Defense) - 30s cooldown (trừ FREEZE_TIME = 60s)
    REVERSE_GRAVITY,        // Đảo ngược trọng lực - khối bay lên trong 3 giây
    RANDOM_NEXT_PIECE,      // Đổi khối tiếp theo thành khối khác ngẫu nhiên
    FREEZE_TIME;            // Ngưng đọng thời gian - dừng rơi nhưng vẫn di chuyển được

    fun isDefense(): Boolean {
        return this in listOf(REVERSE_GRAVITY, RANDOM_NEXT_PIECE, FREEZE_TIME)
    }

    fun isAttack(): Boolean {
        return this in listOf(DELETE_BOTTOM_ROW, SWITCH_NEXT_PIECE, EXPLODING_PIECE)
    }

    fun getDisplayName(): String {
        return when (this) {
            DELETE_BOTTOM_ROW -> "Delete Bottom Row"
            SWITCH_NEXT_PIECE -> "Switch Next"
            EXPLODING_PIECE -> "Exploding Piece"
            REVERSE_GRAVITY -> "Reverse Gravity"
            RANDOM_NEXT_PIECE -> "Random Next"
            FREEZE_TIME -> "Freeze Time"
        }
    }

    fun getCooldown(): Long {
        return when (this) {
            DELETE_BOTTOM_ROW -> 45000L      // 45 giây
            SWITCH_NEXT_PIECE -> 45000L      // 45 giây
            EXPLODING_PIECE -> 45000L        // 45 giây
            REVERSE_GRAVITY -> 30000L        // 30 giây
            RANDOM_NEXT_PIECE -> 30000L      // 30 giây
            FREEZE_TIME -> 60000L            // 60 giây
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
