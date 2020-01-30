package com.i14yokoro.mldpterminal.terminalview

import android.content.Context
import android.graphics.*
import android.support.v4.text.HtmlCompat
import android.view.WindowManager
import com.i14yokoro.mldpterminal.TerminalBuffer
import com.i14yokoro.mldpterminal.TerminalRow
import kotlin.math.abs
import kotlin.math.ceil

class TerminalRenderer(textSize: Int) {
    private val textPaint: Paint = Paint()

    val fontWidth: Int    // 文字幅
    val fontLineSpacing: Int    // 行のスペース
    val fontAscent: Int // ベースラインからの高さ
    val fontLineSpacingAndAscent: Int
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

    fun moveToSavedCursor(canvas: Canvas, x: Int, y: Int, showCursor: Boolean) {
        if(showCursor) {
            canvas.drawRect((x) * fontWidth.toFloat(), titleBar + (fontLineSpacing * (y - 1)).toFloat(),
                    (x + 1) * fontWidth.toFloat(), titleBar + (fontLineSpacing * y).toFloat(), textPaint)
        }
    }

    init {
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)// 等幅フォント
        textPaint.isAntiAlias = true
        textPaint.textSize = textSize.toFloat()
        val fontMetrics = textPaint.fontMetrics

        fontLineSpacing = ceil(textPaint.fontSpacing).toInt()
        fontAscent = ceil(textPaint.ascent()).toInt()
        fontLineSpacingAndAscent = (abs(fontMetrics.top) + abs(fontMetrics.bottom)).toInt()
        fontHeight = (abs(textPaint.fontMetrics.top) + abs(textPaint.fontMetrics.bottom)).toInt()
        fontWidth = textPaint.measureText(" ").toInt()
    }
}