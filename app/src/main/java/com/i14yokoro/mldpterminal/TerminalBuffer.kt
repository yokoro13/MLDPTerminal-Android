package com.i14yokoro.mldpterminal

import java.lang.StringBuilder

/**
 * ターミナルの画面情報を扱う
 * @param screenRowSize : 画面の横幅
 * @param screenColumnSize : 画面の縦幅
 */

class TerminalBuffer(var screenRowSize: Int, var screenColumnSize: Int){
    private val NULL = '\u0000'
    private val LF = '\n'

    var charColor: Int = 0x00000000    // 文字色(RGB)
    var isColorChange = false
    private var textBuffer: ArrayList<TerminalRow> = ArrayList()

    var topRow = 0      // 一番上の行

    var currentRow = 0

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
            return if(totalColumns - topRow >= screenColumnSize){
                screenColumnSize
            } else {
                totalColumns - topRow
            }
        }

    fun display(): String{
        screenBuilder.setLength(0)
        for (y in topRow until topRow+screenColumnSize){
            if (y >= totalColumns){
                for (x in 0 until screenRowSize){
                    screenBuilder.append(' ')
                }
            } else {
                for (x in 0 until screenRowSize) {
                    if (x < getRowLength(y) || textBuffer[y].text[x] != '\n') {
                        screenBuilder.append(textBuffer[y].text[x])
                    } else {
                        screenBuilder.append(' ')
                    }
                }
            }
        }
        return screenBuilder.toString()
    }

    fun moveTopRow(n: Int){
        topRow += n
    }

    /**
     * 新しい行を追加する.
     */
    fun addRow(){
        textBuffer.add(TerminalRow(Array(screenRowSize){NULL}, Array(screenRowSize){0}, false))
    }

    /**
     * リストに文字を記録する.
     *
     * @param text : 入力文字
     * @param color : 文字色
     */
    fun addText(y: Int, text: Char, color: Int){
        textBuffer[y].text[cursorX] = text
        textBuffer[y].color[cursorX] = color
        if(cursorX == screenColumnSize){
            textBuffer[y].lineWrap = true
        }
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
        val sb = StringBuilder()
        for (x in 0 until textBuffer[y].text.indexOf(NULL)){
            sb.append(textBuffer[y].text[x])
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
        return if (textBuffer[y].text.indexOf(LF) != -1){
            textBuffer[y].text.indexOf(LF)
        } else {
            screenRowSize
        }
    }

    fun resize(){
        var dx = 0
        var dy = 0
        val newTextBuffer: ArrayList<TerminalRow> = ArrayList()
        newTextBuffer.add(TerminalRow(Array(screenRowSize){NULL}, Array(screenRowSize){0}, false))
        for (y in 0 until textBuffer.size){
            for (x in 0 until screenRowSize){
                newTextBuffer[y].text[x] = textBuffer[dy].text[dx]
                if(textBuffer[dy].text[dx] == LF){
                    break
                }
                dx++
                if (dx == getRowLength(y)){
                    dx = 0
                    dy++
                    if(dy == textBuffer.size){
                        break
                    }
                }
            }
            if(dy == textBuffer.size){
                break
            }
            if (newTextBuffer[y].text.indexOf(LF) == -1) {
                newTextBuffer[y].lineWrap = true
            }
            newTextBuffer.add(TerminalRow(Array(screenRowSize) { NULL }, Array(screenRowSize) { 0 }, false))

        }
        textBuffer.clear()
        textBuffer = newTextBuffer
    }

    init {
        textBuffer.add(TerminalRow(Array(screenRowSize){NULL}, Array(screenRowSize){0}, false))
    }
}