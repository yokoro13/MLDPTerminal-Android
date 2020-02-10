package com.i14yokoro.mldpterminal

import android.util.Log

/**
 * ターミナルの画面情報を扱う
 * @param screenColumnSize : 画面の横幅
 * @param screenRowSize : 画面の縦幅
 */
class TerminalBuffer(var screenColumnSize: Int, var screenRowSize: Int){
    private var textBuffer: ArrayList<TerminalRow> = ArrayList()

    var charColor: Int = 0x00000000    // 文字色(RGB)
    var isColorChange = false
    private val space = ' '

    // TODO move to View
    var topRow = 0      // 一番上の行
        set(topRow) {
            field = if(topRow < 0){
                0
            } else {
                topRow
            }
        }

    val totalLines: Int
        get() {
            return textBuffer.size
        }

    // TODO move to View
    val displayedLines: Int
        get() {
            return if(totalLines >= screenRowSize){
                screenRowSize
            } else {
                totalLines
            }
        }
    // TODO move to View
    fun moveTopRow(n: Int){
        if(topRow + n < 0){
            topRow = 0
        } else {
            topRow += n
        }
    }

    /**
     * 新しい行を追加する.
     */
    fun addRow(lineWarp: Boolean = false){
        textBuffer.add(TerminalRow(CharArray(screenColumnSize){space}, IntArray(screenColumnSize){0}, lineWarp))
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
    fun getRowText(y: Int): CharArray{
        return textBuffer[y].text
    }

    fun getRowStringText(y: Int): String{
        return String(textBuffer[y].text)
    }

    fun resize(newScreenColumnSize: Int, newScreenRowSize: Int){
        var oldX = 0
        var oldY = 0
        var newY = 0  // newTextBuffer の index
        var lineWarp: Boolean
        val newTextBuffer: ArrayList<TerminalRow> = ArrayList()

        newTextBuffer.add(TerminalRow(CharArray(newScreenColumnSize){space}, IntArray(newScreenColumnSize){0}, false))

        for (y in 0 until textBuffer.size){
            for (newX in 0 until screenColumnSize){
                newTextBuffer[newY].text[newX] = textBuffer[oldY].text[oldX]
                oldX++

                // oldX が 行文字数に
                if (oldX == screenColumnSize){
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
            lineWarp = oldX != screenColumnSize
            newTextBuffer.add(TerminalRow(CharArray(newScreenColumnSize){space}, IntArray(newScreenColumnSize){0}, lineWarp))
        }

        textBuffer.clear()
        textBuffer = newTextBuffer
        screenColumnSize = newScreenColumnSize
        screenRowSize = newScreenRowSize
    }

    init {
        textBuffer.add(TerminalRow(CharArray(screenColumnSize){space}, IntArray(screenColumnSize){0}, false))
    }
}