package com.i14yokoro.mldpterminal


/**
 * ANSIのエスケープシーケンスと同じ動作をする
 */
class EscapeSequence
/**
 * @param termBuffer 表示画面の情報
 */
internal constructor(private val termBuffer: TerminalBuffer) {

    private val currentRow: Int
        get() = termBuffer.cursorY + termBuffer.topRow

    /**
     * @param n 移動量
     */
    fun moveRight(n: Int) {
        moveCursorX(n)
    }

    /**
     * @param n 移動量
     */
    fun moveLeft(n: Int) {
        moveCursorX(-n)
    }

    /**
     * @param n 移動量
     */
    fun moveUp(n: Int) {
        moveCursorY(-n)
    }

    /**
     * @param n 移動量
     */
    fun moveDown(n: Int) {
        moveCursorY(n)
    }

    /**
     * @param n 移動量
     */
    fun moveUpToRowLead(n: Int) {
        termBuffer.cursorX = 0
        moveUp(n)
    }

    /**
     * @param n 移動量
     */
    fun moveDownToRowLead(n: Int) {
        termBuffer.cursorX = 0
        moveDown(n)
    }

    /**
     * @param n 移動量
     */
    fun moveCursor(n: Int) {
        termBuffer.cursorX = 0
        moveRight(n)
    }

    /**
     * @param n 縦の移動量(0 < n)
     * @param m 横の移動量(0 < m)
     */
    fun moveCursor(n: Int, m: Int) { //n,mは1~
        termBuffer.cursorY = 0
        termBuffer.cursorX = 0
        moveDown(n)
        moveRight(m)
    }

    /**
     * 画面消去
     */
    private fun clearDisplay() {
        val curr = currentRow
        val displaySize = termBuffer.displaySize
        for (x in termBuffer.cursorX until termBuffer.getRowLength(curr)){
            termBuffer.setText(x, curr, ' ')
        }
        for (y in termBuffer.cursorY until displaySize) {
            for (x in 0 until termBuffer.getRowLength(y)){
                termBuffer.setText(x, y, ' ')
            }
        }
    }

    /**
     * @param n 画面消去の方法
     */
    fun clearDisplay(n: Int) {
        //一番下までスクロールして消去ぽい
        if (n == 0) { //カーソルより後ろにある画面上の文字を消す
            clearDisplay()
        }

        if (n == 1) { //カーソルより前にある画面上の文字を消す
            for (y in termBuffer.topRow .. currentRow) {
                for (x in 0 until termBuffer.getRowLength(y)) {
                    termBuffer.setText(x, y, ' ')
                    if (y == currentRow && x == termBuffer.cursorX) {
                        break
                    }
                }
            }
        }

        if (n == 2) { //全消去
            for (y in termBuffer.topRow until termBuffer.displaySize + termBuffer.topRow) {
                for (x in 0 until termBuffer.getRowLength(y)) {
                    termBuffer.setText(x, y, ' ')
                }
            }
        }
    }

    /**
     * @param n 行削除の方法
     */
    fun clearRow(n: Int) {
        if (n == 0) { //カーソル以降にある文字を消す
            val del = termBuffer.getRowLength(currentRow) - termBuffer.cursorX
            for (x in 0 until del) {
                termBuffer.setText(termBuffer.cursorX, currentRow, ' ')
            }
        }

        if (n == 1) { //カーソル以前にある文字を消す
            for (x in 0..termBuffer.cursorX) {
                termBuffer.setText(x, currentRow, ' ')
            }
        }

        if (n == 2) { //全消去
            val del = termBuffer.getRowLength(currentRow)
            for (x in 0 until del) {
                termBuffer.setText(x, currentRow, ' ')
            }

        }
    }

    /**
     * @param n 移動する量
     */
    fun scrollNext(n: Int) {
        if (termBuffer.topRow + n > termBuffer.totalColumns) return  //一番したに空白追加？？
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
    private fun moveCursorX(x: Int) {
        termBuffer.cursorX += x
    }

    /**
     * @param y カーソルをy移動させる
     */
    private fun moveCursorY(y: Int) {
        termBuffer.cursorY +=  y
    }

}
