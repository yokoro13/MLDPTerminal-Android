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

    fun render(termBuffer: TerminalBuffer, canvas: Canvas, topRow: Int, cursor: TerminalView.Cursor, showCursor: Boolean, pad: Int) {
        val displayRows = if (topRow + termBuffer.screenRowSize <= termBuffer.totalLines){
            topRow + termBuffer.screenRowSize
        } else {
            termBuffer.totalLines
        }
        for (row in topRow until displayRows) {
            canvas.drawText(termBuffer.getRowText(row), 0, termBuffer.screenColumnSize, 0f,titleBar + (fontLineSpacing * (row-topRow).toFloat())-pad, textPaint)
        }
        moveToSavedCursor(canvas, cursor, showCursor, pad)
    }

    private fun moveToSavedCursor(canvas: Canvas, cursor: TerminalView.Cursor, showCursor: Boolean, pad: Int) {
        if(showCursor) {
            canvas.drawRect((cursor.x) * fontWidth.toFloat(), titleBar + (fontLineSpacing * (cursor.y - 1))-pad.toFloat(),
                     (cursor.x + 1) * fontWidth.toFloat(), titleBar + (fontLineSpacing * cursor.y).toFloat()-pad, textPaint)
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