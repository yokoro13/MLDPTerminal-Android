package com.i14yokoro.mldpterminal.terminalview

import android.content.Context
import android.graphics.*
import android.support.v4.text.HtmlCompat
import android.view.WindowManager
import com.i14yokoro.mldpterminal.TerminalBuffer
import com.i14yokoro.mldpterminal.TerminalRow
import kotlin.math.abs
import kotlin.math.ceil

class TerminalRenderer() {
    private val textPaint: Paint = Paint()

    val fontWidth: Float    // 文字幅
    val fontLineSpacing: Int    // 行のスペース
    val fontAscent: Int // ベースラインからの高さ
    val fontLineSpacingAndAscent: Int
    
    private val asciiMeasures = Array(127){0.0.toFloat()}

    fun render(termBuffer: TerminalBuffer, canvas: Canvas, topRow: Int, x: Int, y: Int) {
        for (row in topRow .. topRow + y) {
            canvas.drawText(termBuffer.getRowText(row), 0, termBuffer.screenColumnSize, 0f, (fontLineSpacingAndAscent * row).toFloat(), textPaint)
        }
        moveToSavedCursor(canvas, x, y)
    }

    // TODO CanvasもたせたRenderつくったほうがいい
    fun moveToSavedCursor(canvas: Canvas, x: Int, y: Int) {
        canvas.drawRect((x+1)*fontWidth, (fontLineSpacingAndAscent * (y-1)).toFloat(),
                       (x+2)*fontWidth, (fontLineSpacingAndAscent * y).toFloat(), textPaint)
    }

    init {
        textPaint.typeface = Typeface.MONOSPACE // 等幅フォント
        textPaint.isAntiAlias = true
        textPaint.textSize = 17.0f
        val fontMetrics = textPaint.fontMetrics

        fontLineSpacing = ceil(textPaint.fontSpacing).toInt()
        fontAscent = ceil(textPaint.ascent()).toInt()
        fontLineSpacingAndAscent = (abs(fontMetrics.top) + abs(fontMetrics.bottom)).toInt()
        fontWidth = textPaint.measureText("X")
    }
}