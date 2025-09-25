package com.example.tetrisgamegroup11.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.tetrisgamegroup11.model.GamePiece

class NextPieceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    var nextPiece: GamePiece? = null
    private var cellSize = 40 // Kích thước ô nhỏ hơn cho khối tiếp theo

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        nextPiece?.let { piece ->
            // Lấy màu của khối từ `GamePiece.Type`
            val pieceColor = piece.type.getColor()

            // Căn giữa khối tiếp theo trong khung hiển thị
            val pieceWidth = piece.shape[0].size * cellSize
            val pieceHeight = piece.shape.size * cellSize
            val offsetX = (width - pieceWidth) / 2f
            val offsetY = (height - pieceHeight) / 2f

            piece.shape.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { colIndex, cell ->
                    if (cell == 1) {
                        val left = offsetX + colIndex * cellSize
                        val top = offsetY + rowIndex * cellSize
                        val right = left + cellSize
                        val bottom = top + cellSize

                        // Vẽ ô với màu của loại khối
                        paint.color = pieceColor
                        paint.style = Paint.Style.FILL
                        canvas.drawRect(left, top, right, bottom, paint)

                        // Vẽ viền cho ô
                        paint.color = Color.DKGRAY
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        canvas.drawRect(left, top, right, bottom, paint)
                    }
                }
            }
        }
    }

    // Phương thức này được gọi để cập nhật khối tiếp theo và vẽ lại
    fun updateNextPiece(nextPiece: GamePiece) {
        this.nextPiece = nextPiece
        invalidate() // Cập nhật lại giao diện khi có khối mới
    }

    // Phương thức khôi phục khối tiếp theo từ dữ liệu lưu trữ
    fun restoreNextPiece(piece: GamePiece) {
        this.nextPiece = piece
        invalidate() // Cập nhật giao diện với khối được khôi phục
    }
}
