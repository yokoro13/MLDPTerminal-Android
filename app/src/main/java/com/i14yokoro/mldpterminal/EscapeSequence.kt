package com.i14yokoro.mldpterminal

import com.i14yokoro.mldpterminal.terminalview.TerminalView.Cursor


/**
 * ANSIのエスケープシーケンスと同じ動作をする
 */
class EscapeSequence
/**
 * @param termBuffer 表示画面の情報
 */
internal constructor(private val termBuffer: TerminalBuffer) {

    private val nonBreakingSpace = Typography.nbsp

    private val currentRow: Int
        get() = termBuffer.currentRow

    /**
     * @param n 移動量
     */
    fun moveRight(cursor: Cursor, n: Int) {
        moveCursorX(cursor, n)
    }

    /**
     * @param n 移動量
     */
    fun moveLeft(cursor: Cursor, n: Int) {
        moveCursorX(cursor, -n)
    }

    /**
     * @param n 移動量
     */
    fun moveUp(cursor: Cursor, n: Int) {
        moveCursorY(cursor, -n)
        if(cursor.y > 0) {
            termBuffer.currentRow -= n
        }
    }

    /**
     * @param n 移動量
     */
    fun moveDown(cursor: Cursor, n: Int) {
        if(cursor.y + n >= termBuffer.screenRowSize){
            termBuffer.currentRow += cursor.y + n - termBuffer.screenRowSize
        } else {
            termBuffer.currentRow += n
        }
        if(termBuffer.currentRow >= termBuffer.totalLines) {
            for(i in 0 .. termBuffer.currentRow - termBuffer.totalLines) {
                termBuffer.addRow()
            }
        }

        moveCursorY(cursor, n)
    }

    /**
     * @param n 移動量
     */
    fun moveUpToRowLead(cursor: Cursor, n: Int) {
        cursor.x = 0
        moveUp(cursor, n)
    }

    /**
     * @param n 移動量
     */
    fun moveDownToRowLead(cursor: Cursor, n: Int) {
        cursor.x = 0
        moveDown(cursor, n)
    }

    /**
     * @param n 移動量
     */
    fun moveCursor(cursor: Cursor, n: Int) {
        cursor.x = 0
        moveRight(cursor, n-1)
    }

    /**
     * @param n 縦の移動量(0 < n)
     * @param m 横の移動量(0 < m)
     */
    fun moveCursor(cursor: Cursor, n: Int, m: Int) { //n,mは1~
        cursor.y = 0
        cursor.x = 0
        moveDown(cursor, n-1)
        moveRight(cursor, m-1)
    }

    /**
     * 画面消去
     */
    private fun clearDisplay(cursor: Cursor) {
        for (x in cursor.x until termBuffer.screenColumnSize){
            termBuffer.setText(x, currentRow, nonBreakingSpace)
        }
        for (y in cursor.y+1 until termBuffer.displayedLines) {
            for (x in 0 until termBuffer.screenColumnSize){
                termBuffer.setText(x, y, nonBreakingSpace)
            }
        }
    }

    /**
     * @param n 画面消去の方法
     */
    fun clearDisplay(cursor: Cursor, n: Int) {
        //一番下までスクロールして消去ぽい
        if (n == 0) { //カーソルより後ろにある画面上の文字を消す
            clearDisplay(cursor)
        }

        if (n == 1) { //カーソルより前にある画面上の文字を消す
            for (y in termBuffer.topRow .. currentRow) {
                for (x in 0 until termBuffer.screenColumnSize) {
                    termBuffer.setText(x, y, nonBreakingSpace)
                    if (y == currentRow && x == cursor.x) {
                        break
                    }
                }
            }
        }

        if (n == 2) { //全消去
            for (y in termBuffer.topRow until termBuffer.displayedLines + termBuffer.topRow) {
                for (x in 0 until termBuffer.screenColumnSize) {
                    termBuffer.setText(x, y, nonBreakingSpace)
                }
            }
        }
    }

    /**
     * @param n 行削除の方法
     */
    fun clearRow(cursor: Cursor, n: Int) {
        if (n == 0) { //カーソル以降にある文字を消す
            for (x in cursor.x until termBuffer.screenColumnSize) {
                termBuffer.setText(x, currentRow, nonBreakingSpace)
            }
        }

        if (n == 1) { //カーソル以前にある文字を消す
            for (x in 0 until cursor.x) {
                termBuffer.setText(x, currentRow, nonBreakingSpace)
            }
        }

        if (n == 2) { //全消去
            for (x in 0 until termBuffer.screenColumnSize) {
                termBuffer.setText(x, currentRow, nonBreakingSpace)
            }

        }
    }

    /**
     * @param n 移動する量
     */
    fun scrollNext(n: Int) {
        if (termBuffer.topRow + n > termBuffer.totalLines) return  //一番したに空白追加？？
        termBuffer.topRow = termBuffer.topRow + n
    }

    /**
     * @param n 移動する量
     */
    fun scrollBack(n: Int) {
        //一番上に追加で一番した削除？？？(あくまで画面上でスクロールしていると見せかけている?)
        if (termBuffer.topRow - n < 0) return
        termBuffer.topRow = termBuffer.topRow - n
    }

    /**
     * @param n 変化させる色
     */
    fun selectGraphicRendition(n: Int) { //色
        termBuffer.isColorChange = true
        when (n) {
            0, 39, 30 -> termBuffer.charColor = 0xFF000000.toInt()
            31 -> termBuffer.charColor = 0xFFff0000.toInt()
            32 -> termBuffer.charColor = 0xFF008000.toInt()
            33 -> termBuffer.charColor = 0xFFFFFF00.toInt()
            34 -> termBuffer.charColor = 0xFF0000FF.toInt()
            35 -> termBuffer.charColor = 0xFFFF00FF.toInt()
            36 -> termBuffer.charColor = 0xFF00FFFF.toInt()
            37 -> termBuffer.charColor = 0xFFFFFFFF.toInt()
        }
    }

    /**
     * @param x カーソルをx移動させる
     */
    private fun moveCursorX(cursor: Cursor, x: Int) {
        cursor.x += x
    }

    /**
     * @param y カーソルをy移動させる
     */
    private fun moveCursorY(cursor: Cursor, y: Int) {
        cursor.y +=  y
    }

}
