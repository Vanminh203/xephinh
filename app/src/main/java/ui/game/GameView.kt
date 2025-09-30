package com.example.tetrisgamegroup11.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.view.View
import com.example.tetrisgamegroup11.manager.GameManager
import com.example.tetrisgamegroup11.model.GameMode
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

    fun setGameManager(manager: GameManager) {
        this.gameManager = manager
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        cellSize = minOf(
            w / gameManager.gameGrid.columns,
            h / gameManager.gameGrid.rows
        )

        val gridWidth = cellSize * gameManager.gameGrid.columns
        val gridHeight = cellSize * gameManager.gameGrid.rows

        offsetX = (w - gridWidth) / 2f
        offsetY = (h - gridHeight) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        if (gameManager.gameMode == GameMode.TARGET) {
            drawTargetLines(canvas)
        }
        drawPlacedPieces(canvas)
        drawCurrentPiece(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val gridWidth = gameManager.gameGrid.columns * cellSize
        val gridHeight = gameManager.gameGrid.rows * cellSize

        paint.color = Color.LTGRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(
            offsetX,
            offsetY,
            offsetX + gridWidth,
            offsetY + gridHeight,
            paint
        )

        paint.color = Color.BLACK
        paint.alpha = 80
        paint.strokeWidth = 4f

        for (i in 0..gameManager.gameGrid.columns) {
            val x = offsetX + i * cellSize.toFloat()
            canvas.drawLine(x, offsetY, x, offsetY + gridHeight, paint)
        }

        for (j in 0..gameManager.gameGrid.rows) {
            val y = offsetY + j * cellSize.toFloat()
            canvas.drawLine(offsetX, y, offsetX + gridWidth, y, paint)
        }
    }

    private fun drawTargetLines(canvas: Canvas) {
        val gridWidth = gameManager.gameGrid.columns * cellSize
        paint.color = Color.rgb(255, 215, 0) // Gold color
        paint.alpha = 100
        paint.style = Paint.Style.FILL

        gameManager.getTargetLines().forEach { lineIndex ->
            if (lineIndex in 0 until gameManager.gameGrid.rows) {
                val top = offsetY + lineIndex * cellSize.toFloat()
                val bottom = top + cellSize
                canvas.drawRect(offsetX, top, offsetX + gridWidth, bottom, paint)
            }
        }

        // Draw dashed border for target lines
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)

        gameManager.getTargetLines().forEach { lineIndex ->
            if (lineIndex in 0 until gameManager.gameGrid.rows) {
                val top = offsetY + lineIndex * cellSize.toFloat()
                val bottom = top + cellSize
                canvas.drawRect(offsetX, top, offsetX + gridWidth, bottom, paint)
            }
        }

        paint.pathEffect = null
        paint.style = Paint.Style.FILL
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
