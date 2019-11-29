package com.i14yokoro.mldpterminal

import java.lang.StringBuilder

/**
 * ターミナルの画面情報を扱う
 * @param screenRowSize : 画面の横幅
 * @param screenColumnSize : 画面の縦幅
 */

class TerminalBuffer(private val screenRowSize: Int, private val screenColumnSize: Int){
    var charColor: Int = 0x00000000    // 文字色(RGB)
    var isColorChange = false
    private val textBuffer: ArrayList<ArrayList<TextItem>> = ArrayList()

    var topRow = 0      // 一番上の行
    var bottomRow = 0

    // 行が８０を超える場合に使用
    var rowOffset = 0 // textBuffer[y].size > 80 -> getRowLength(y-1)/screenRowSize + 1

    var currentRow = 0

    var isOutOfScreen = false    // カーソルが画面の外にあれば true

    private var screenArray = Array(screenColumnSize) {Array(screenRowSize){' '}}

    // カーソルの座標
    var cursorX = 0
        set(cursorX){
            field = if(cursorX >= screenRowSize){
                screenRowSize -1
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
            var total = 0
            for (y in 0 until textBuffer.size){
                total += usedLine(y)
            }
            return total
        }

    fun display(): Array<Array<Char>>{
        var dx = 0
        var dy = 0
        for (y in topRow until topRow+screenColumnSize){
            if (y >= totalColumns){
                for (x in 0 until screenRowSize){
                    screenArray[dy][x] = ' '
                }
                dy++
            } else {
                for (x in 0 until getRowLength(y) + (screenRowSize - (getRowLength(y) % screenRowSize))) {
                    if (x < getRowLength(y) || textBuffer[y][x].text != '\n') {
                        screenArray[dy][dx] = textBuffer[y][x].text
                    } else {
                        screenArray[dy][dx] = ' '
                    }
                    dx++
                    if (dx == screenRowSize) {
                        dx = 0
                        dy++
                    }
                    if (dy == screenColumnSize) {
                        break
                    }
                }
            }
        }
        return screenArray
    }


    fun moveTopRow(n: Int){
        topRow += n
    }

    private fun usedLine(y: Int): Int{
        return (getRowLength(y) - 1) / screenRowSize + 1
    }

    /**
     * 新しい行を追加する.
     */
    fun addRow(){
        textBuffer.add(ArrayList())
    }

    /**
     * リストに文字を記録する.
     *
     * @param text : 入力文字
     * @param color : 文字色
     */
    fun addText(y: Int, text: Char, color: Int){
        textBuffer[y].add(TextItem(text, color))
    }

    /**
     * y 行目 x 番目の文字を text にする．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param text : 入力文字
     */
    fun setText(x: Int, y: Int, text: Char){
        textBuffer[y][x].text = text
    }

    /**
     * y 行目 x 番目の文字色を color にする．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param color : 文字色
     */
    fun setColor(x: Int, y: Int, color: Int){
        textBuffer[y][x].color = color
    }

    /**
     * y 行目 x 番目の文字をリストから消す．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     */
    fun removeText(x: Int, y: Int){
        textBuffer[y].removeAt(x)
    }

    /**
     * y 行目のテキストを返す．
     *
     * @param y : y 行目
     * @return
     */
    fun getRowText(y: Int): String{
        val sb = StringBuilder()
        for (x in 0 until textBuffer[y].size){
            sb.append(textBuffer[y][x])
        }
        return sb.toString()
    }

    /**
     * y 行目のテキストの長さを返す．
     *
     * @param y : y 行目
     * @return
     */
    fun getRowLength(y: Int): Int{
        return textBuffer[y].size
    }

    init {
        textBuffer.add(ArrayList())
    }
}