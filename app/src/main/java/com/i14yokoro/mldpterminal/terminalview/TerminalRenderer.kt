package com.i14yokoro.mldpterminal.terminalview

import android.graphics.*
import com.i14yokoro.mldpterminal.TerminalBuffer
import kotlin.math.abs
import kotlin.math.ceil

class TerminalRenderer(textSize: Int) {
    private val textPaint: Paint = Paint()

    val fontWidth: Int    // 文字幅
    private val fontLineSpacing: Int    // 行のスペース
    var titleBar: Int = 0
    var fontHeight: Int = 0

    fun render(termBuffer: TerminalBuffer, canvas: Canvas, topRow: Int, x: Int, y: Int, showCursor: Boolean) {
        val displayRows = if (topRow + termBuffer.screenRowSize <= termBuffer.totalLines){
            topRow + termBuffer.screenRowSize
        } else {
            termBuffer.totalLines
        }
        for (row in topRow until displayRows) {
            canvas.drawText(termBuffer.getRowText(row), 0, termBuffer.screenColumnSize, 0f,titleBar + (fontLineSpacing * (row-topRow).toFloat()), textPaint)
        }
        moveToSavedCursor(canvas, x, y, showCursor)
    }

    private fun moveToSavedCursor(canvas: Canvas, x: Int, y: Int, showCursor: Boolean) {
        if(showCursor) {
            canvas.drawRect((x) * fontWidth.toFloat(), titleBar + (fontLineSpacing * (y - 1)).toFloat(),
                    (x + 1) * fontWidth.toFloat(), titleBar + (fontLineSpacing * y).toFloat(), textPaint)
        }
    }

    init {
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)// 等幅フォント
        textPaint.isAntiAlias = true
        textPaint.textSize = textSize.toFloat()

        fontLineSpacing = ceil(textPaint.fontSpacing).toInt()
        fontHeight = (abs(textPaint.fontMetrics.top) + abs(textPaint.fontMetrics.bottom)).toInt()
        fontWidth = textPaint.measureText(" ").toInt()
    }
}