package com.i14yokoro.mldpterminal

import java.lang.StringBuilder

/**
 * ターミナルの画面情報を扱う
 * @param screenRowSize : 画面の横幅
 * @param screenColumnSize : 画面の縦幅
 */
class TerminalBuffer(var screenRowSize: Int, var screenColumnSize: Int){
    private var textBuffer: ArrayList<TerminalRow> = ArrayList()

    var charColor: Int = 0x00000000    // 文字色(RGB)
    var isColorChange = false
    private val nonBreakingSpace = Typography.nbsp

    var topRow = 0      // 一番上の行
        set(topRow) {
            field = if(topRow < 0){
                0
            } else {
                topRow
            }
        }
    // 現在入力中の行
    var currentRow = 0
        set(currentRow) {
            field = if(currentRow < 0){
                0
            } else {
                currentRow
            }
        }

    var isOutOfScreen = false    // カーソルが画面の外にあれば true

    private var screenBuilder = StringBuilder()

    // カーソルの座標
    var cursorX = 0
        set(cursorX){
            field = if(cursorX >= screenRowSize){
                screenRowSize - 1
            } else {
                if(cursorX < 0){
                        0
                } else {
                    cursorX
                }
            }
        }

    var cursorY = 0
        set(cursorY) {
            field = if(cursorY >= screenColumnSize){
                screenColumnSize - 1
            } else {
                if(cursorY < 0){
                    0
                } else {
                    cursorY
                }
            }

        }

    val totalColumns: Int
        get() {
            return textBuffer.size
        }

    val displayedLines: Int
        get() {
            return if(totalColumns >= screenColumnSize){
                screenColumnSize
            } else {
                totalColumns
            }
        }

    fun makeScreenString(): String{
        screenBuilder.setLength(0)
        for (y in topRow until topRow+screenColumnSize){
            if (y >= totalColumns){
                return screenBuilder.toString()
            } else {
                for (x in 0 until screenRowSize) {
                    screenBuilder.append(textBuffer[y].text[x])
                }
            }
        }
        return screenBuilder.toString()
    }

    fun moveTopRow(n: Int){
        if(topRow + n < 0){
            topRow = 0
        } else {
            topRow += n
        }
    }

    fun incrementCurrentRow(){
        currentRow++
    }

    /**
     * 新しい行を追加する.
     */
    fun addRow(lineWarp: Boolean = false){
        textBuffer.add(TerminalRow(Array(screenRowSize){nonBreakingSpace}, Array(screenRowSize){0}, lineWarp))
    }

    /**
     * y 行目 x 番目の文字を text にする．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param text : 入力文字
     */
    fun setText(x: Int, y: Int, text: Char){
        textBuffer[y].text[x] = text
    }

    /**
     * y 行目 x 番目の文字色を color にする．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param color : 文字色
     */
    fun setColor(x: Int, y: Int, color: Int){
        textBuffer[y].color[x] = color
    }

    /**
     * y 行目のテキストを返す．
     *
     * @param y : y 行目
     * @return
     */
    fun getRowText(y: Int): String{
        return textBuffer[y].text.toString()
    }

    fun resize(newScreenRowSize: Int, newScreenColumnSize: Int){
        var oldX = 0
        var oldY = 0
        var newY = 0  // newTextBuffer の index
        var lineWarp: Boolean
        val newTextBuffer: ArrayList<TerminalRow> = ArrayList()

        newTextBuffer.add(TerminalRow(Array(newScreenRowSize){nonBreakingSpace}, Array(newScreenRowSize){0}, false))

        for (y in 0 until textBuffer.size){
            for (newX in 0 until screenRowSize){
                newTextBuffer[newY].text[newX] = textBuffer[oldY].text[oldX]
                oldX++

                // oldX が 行文字数に
                if (oldX == screenRowSize){
                    // 次の行に移動
                    oldX = 0
                    if(!textBuffer[oldY].lineWrap){
                        oldY++
                        break
                    }
                    oldY++
                }
                if (oldY == textBuffer.size){
                    break
                }
            }
            if (oldY == textBuffer.size){
                break
            }
            newY++
            lineWarp = oldX != screenRowSize
            newTextBuffer.add(TerminalRow(Array(newScreenRowSize){nonBreakingSpace}, Array(newScreenRowSize){0}, lineWarp))
        }

        textBuffer.clear()
        textBuffer = newTextBuffer
        screenRowSize = newScreenRowSize
        screenColumnSize = newScreenColumnSize
    }

    init {
        textBuffer.add(TerminalRow(Array(screenRowSize){nonBreakingSpace}, Array(screenRowSize){0}, false))
    }
}