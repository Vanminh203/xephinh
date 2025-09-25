package com.example.tetrisgamegroup11.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.tetrisgamegroup11.manager.GameManager
import com.example.tetrisgamegroup11.model.GamePiece

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint()
    private lateinit var gameManager: GameManager
    private var cellSize: Int = 0
    private var gridWidth: Int = 0
    private var gridHeight: Int = 0
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    // Phương thức thiết lập GameManager sau khi khởi tạo
    fun setGameManager(manager: GameManager) {
        this.gameManager = manager
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Kích thước 1 ô vuông
        cellSize = minOf(
            w / gameManager.gameGrid.columns,
            h / gameManager.gameGrid.rows
        )

        // Tính toán offset để căn giữa trong view
        val gridWidth = cellSize * gameManager.gameGrid.columns
        val gridHeight = cellSize * gameManager.gameGrid.rows

        offsetX = (w - gridWidth) / 2f
        offsetY = (h - gridHeight) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)           // Vẽ lưới
        drawPlacedPieces(canvas)    // Vẽ các khối đã xếp trong lưới
        drawCurrentPiece(canvas)    // Vẽ khối hiện tại
    }

    private fun drawGrid(canvas: Canvas) {
        val gridWidth = gameManager.gameGrid.columns * cellSize
        val gridHeight = gameManager.gameGrid.rows * cellSize

        // Vẽ nền toàn bộ lưới
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(
            offsetX,
            offsetY,
            offsetX + gridWidth,
            offsetY + gridHeight,
            paint
        )

        // Vẽ các đường kẻ
        paint.color = Color.BLACK
        paint.alpha = 80   // Làm nhạt để không lấn át khối
        paint.strokeWidth = 4f

        // Kẻ cột dọc
        for (i in 0..gameManager.gameGrid.columns) {
            val x = offsetX + i * cellSize.toFloat()
            canvas.drawLine(x, offsetY, x, offsetY + gridHeight, paint)
        }

        // Kẻ hàng ngang
        for (j in 0..gameManager.gameGrid.rows) {
            val y = offsetY + j * cellSize.toFloat()
            canvas.drawLine(offsetX, y, offsetX + gridWidth, y, paint)
        }
    }

    private fun drawPlacedPieces(canvas: Canvas) {
        gameManager.gameGrid.colors.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, color ->
                if (color != Color.TRANSPARENT) {
                    paint.color = color
                    val left = offsetX + colIndex * cellSize.toFloat()
                    val top = offsetY + rowIndex * cellSize.toFloat()
                    val right = left + cellSize
                    val bottom = top + cellSize
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(left, top, right, bottom, paint)

                    paint.color = Color.BLACK
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(left, top, right, bottom, paint)

                    paint.style = Paint.Style.FILL
                }
            }
        }
    }


    private fun drawCurrentPiece(canvas: Canvas) {
        drawPieceAtPosition(
            canvas,
            gameManager.currentPiece,
            gameManager.currentPiece.positionX.toFloat(),
            gameManager.currentPiece.positionY.toFloat()
        )
    }

    private fun drawPieceAtPosition(canvas: Canvas, piece: GamePiece, x: Float, y: Float) {
        piece.shape.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell == 1) {
                    val left = offsetX + (x + colIndex) * cellSize
                    val top = offsetY + (y + rowIndex) * cellSize
                    val right = left + cellSize
                    val bottom = top + cellSize
                    paint.color = piece.type.getColor()
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(left, top, right, bottom, paint)

                    paint.color = Color.BLACK
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(left, top, right, bottom, paint)

                    paint.style = Paint.Style.FILL
                }
            }
        }
    }

    fun reset() {
        invalidate()
    }

    fun pauseGame() {
        // Thực hiện các thao tác khi tạm dừng
    }

    fun resumeGame() {
        // Thực hiện các thao tác khi tiếp tục trò chơi
    }
}
