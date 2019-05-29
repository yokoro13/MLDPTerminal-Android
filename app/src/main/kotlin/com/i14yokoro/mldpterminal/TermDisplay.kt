package com.i14yokoro.mldpterminal

import android.util.Log

import java.util.ArrayList

class TermDisplay(val displayRowSize: Int, val displayColumnSize: Int) {

    var cursorX = 0
        set(cursorX) {
            if (cursorX >= displayRowSize) {
                field = displayRowSize - 1
            } else {
                if (cursorX >= textList[cursorY + topRow].size) {
                    field = cursorX % displayRowSize
                    Log.d(TAG, Integer.toString(cursorX))
                } else {
                    field = if (cursorX < 0) {
                        0
                    } else {
                        cursorX
                    }
                }
            }
        }
    var cursorY = 0
        set(cursorY) {
            field = if (cursorY >= displayColumnSize) {
                displayColumnSize - 1
            } else {
                if (cursorY < 0) {
                    0
                } else {
                    cursorY
                }
            }
            Log.d("changeTop", "top :" + Integer.toString(topRow + this.cursorY))
            currRow = topRow + this.cursorY
        }

    var displaySize = 0

    var topRow = 0
    private val textList: ArrayList<ArrayList<TextItem>>

    var defaultColor = 0x000000

    var isColorChange = false

    private val sb = StringBuilder()

    var currRow = 0

    var isOutOfScreen = false

    val totalColumns: Int
        get() = this.textList.size

    init {

        val items = ArrayList<TextItem>(displayRowSize)
        textList = ArrayList()
        textList.add(items)
    }

    fun setTextItem(text: Char, color: Int) {

        val textItem = TextItem(text, color)
        val columnSize = totalColumns

        textList[columnSize - 1].add(textItem)
        if (text == LF || textList[totalColumns - 1].size >= displayRowSize) {
            val items1 = ArrayList<TextItem>(displayRowSize + 5)
            textList.add(items1)
        }
    }

    fun deleteTextItem(x: Int, y: Int) {
        textList[y].removeAt(x)
    }

    fun changeTextItem(x: Int, y: Int, text: Char, color: Int) {
        if (y < textList.size && x < textList[y].size) {
            val textItem = TextItem(text, color)
            textList[y][x] = textItem
        }
    }

    fun addTextItem(y: Int, text: Char, color: Int) {
        if (y < textList.size) {
            val textItem = TextItem(text, color)
            textList[y].add(textItem)
        }
        if ((text == LF || textList[y].size >= displayRowSize) && y == totalColumns - 1) {
            val items1 = ArrayList<TextItem>(displayRowSize)
            textList.add(items1)
        }
    }

    fun addEmptyRow() {
        val items1 = ArrayList<TextItem>()
        textList.add(items1)
    }

    fun insertTextItem(x: Int, y: Int, text: Char, color: Int) {
        if (y > textList.size || textList[y].size >= displayRowSize) { //行の長さが最大文字数をこえる
            return
        }
        if (x <= textList[y].size) { //行の範囲内
            if (getText(displayRowSize - 1, y) == LF) { //一番最後がLF
                deleteTextItem(displayRowSize - 1, y)
            }
            if (getText(x, y) != LF) { //LFじゃない
                addTextItem(y, text, color)
            } else {
                deleteTextItem(x, y)
                addTextItem(y, text, color)
                addTextItem(y, LF, color)
            }
        }
    }

    fun changeText(x: Int, y: Int, text: Char) {
        if (y < this.textList.size && x < this.textList[y].size) {
            this.textList[y][x].text = text
        }
    }

    private fun getText(x: Int, y: Int): Char {
        return if (y < this.textList.size && x < this.textList[y].size) {
            this.textList[y][x].text
        } else {
            '\u0000'
        }
    }

    private fun getColor(x: Int, y: Int): Int {
        return if (y < this.textList.size && x < this.textList[y].size) {
            this.textList[y][x].color
        } else {
            0x000000
        }
    }

    fun addTopRow(count: Int) {
        this.topRow = this.topRow + count
    }

    fun createDisplay(): String {
        sb.setLength(0)
        var text: Char

        val totalColumns = totalColumns
        val displayRowSize = displayRowSize
        val displayColumnSize = displayColumnSize

        for (y in 0 until displayColumnSize) {//これはリストの縦移動用（最大でスクリーンの最大値分移動）

            if (y + topRow >= totalColumns) { //yがリストよりでかくなったら
                displaySize = y - 1
                return sb.toString()
            }
            var x = 0
            val n = textList[y + topRow].size
            while (x < n) { //xはそのyのサイズまで
                text = textList[y + topRow][x].text
                if (text == LF && y + 1 == displayColumnSize) {
                    break
                }
                if (!isColorChange) {
                    sb.append(text)
                } else {
                    sb.append("<font color=#").append(Integer.toHexString(getColor(x, topRow + y))).append(">").append(text).append("</font>")
                }
                if (x == displayRowSize - 1 && text != LF && y + 1 != displayColumnSize) {
                    sb.append(LF)
                    break
                }
                if (text == LF) {
                    if (y - 1 == displayColumnSize) {
                        sb.deleteCharAt(sb.length - 1)
                    }
                    break
                }
                x++
            }
            if (y + 1 < displayColumnSize && y + topRow < totalColumns - 1 && !getRowText(y + topRow).contains("\n") && textList[y + topRow].size < displayRowSize) {
                sb.append(LF)
            }
        }
        displaySize = displayColumnSize

        return sb.toString()
    }

    fun getRowLength(y: Int): Int {
        if (y >= totalColumns) {
            return 0
        }
        return if (textList.size == 0) {
            0
        } else textList[y].size
    }

    fun getRowText(y: Int): String {
        val str = StringBuilder()
        for (x in 0 until getRowLength(y)) {
            str.append(getText(x, y))
        }
        return str.toString()
    }

    companion object {
        private const val TAG = "termDisplay**"
        private const val LF = '\n'
    }
}
