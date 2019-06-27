package com.i14yokoro.mldpterminal

/**
 * ANSIのエスケープシーケンスと同じ動作をする
 */
class EscapeSequence
/**
 * @param termDisplay 表示画面の情報
 */
internal constructor(private val termDisplay: TermDisplay) {

    private val selectRowIndex: Int
        get() = termDisplay.cursorY + termDisplay.topRow

    /**
     * @param n 移動する量
     */
    fun moveRight(n: Int) {
        if (termDisplay.cursorX + n < termDisplay.getRowLength(selectRowIndex)) {
            moveCursorX(n)
        } else {
            var move = n
            val add: Int
            if (termDisplay.cursorX + n >= termDisplay.displayRowSize) {
                add = termDisplay.displayRowSize - termDisplay.getRowLength(selectRowIndex)
                move = termDisplay.displayRowSize - termDisplay.cursorX
            } else {
                add = termDisplay.cursorX + n - termDisplay.getRowLength(selectRowIndex)
            }
            addBlank(add)
            moveCursorX(move)
        }
    }

    /**
     * @param n 移動する量
     */
    fun moveLeft(n: Int) {
        if (termDisplay.cursorX - n >= 0) {
            moveCursorX(-n)
        }
    }

    /**
     * @param n 移動する量
     */
    fun moveUp(n: Int) {

        if (termDisplay.cursorY - n < 0) { //画面外にでる
            termDisplay.cursorY = 0
        } else {
            moveCursorY(-n)
        }
        val rowLength = termDisplay.getRowLength(selectRowIndex)

        if (rowLength < termDisplay.cursorX) {
            val add = termDisplay.cursorX - rowLength + 1
            addBlank(add)
        }
    }

    /**
     * @param n 移動する量
     */
    fun moveDown(n: Int) {

        if (termDisplay.cursorY + n >= termDisplay.displaySize) { //移動先が一番下の行を超える場合
            if (termDisplay.cursorY + n >= termDisplay.displayColumnSize) { //ディスプレイサイズを超える場合
                termDisplay.cursorY = termDisplay.displayColumnSize - 1
            } else {
                moveCursorY(n)
            }
            val move = termDisplay.cursorY - termDisplay.displaySize
            for (i in 0..move) {
                termDisplay.addEmptyRow()
            }
        } else { //移動先が一番下の行を超えない
            if (termDisplay.cursorY + n > termDisplay.displaySize) { //ディスプレイサイズを超える場合
                termDisplay.cursorY = termDisplay.displayColumnSize - 1
            } else {
                moveCursorY(n)
            }
        }
        //移動先の文字数がcursorXより小さい
        if (termDisplay.getRowLength(selectRowIndex) < termDisplay.cursorX) {
            val add = termDisplay.cursorX - termDisplay.getRowLength(selectRowIndex)
            addBlank(add)
        }
    }

    /**
     * @param n 追加する空白の量
     */
    private fun addBlank(n: Int) {
        var x = termDisplay.getRowLength(selectRowIndex) - 1 //現在の行の最後のindexを取得
        if (x < 0) {
            x = 0
        }
        for (i in 0 until n) {
            termDisplay.insertTextItem(x, selectRowIndex, ' ', termDisplay.defaultColor)
            x = termDisplay.getRowLength(selectRowIndex) - 1
        }
    }

    /**
     * @param n 移動する量
     */
    fun moveUpToRowLead(n: Int) {
        termDisplay.cursorX = 0
        moveUp(n)
    }

    /**
     * @param n 移動する量
     */
    fun moveDownToRowLead(n: Int) {
        termDisplay.cursorX = 0
        moveDown(n)
    }

    /**
     * @param n 移動する量
     */
    fun moveSelection(n: Int) {
        termDisplay.cursorX = 0
        moveRight(n - 1)
    }

    /**
     * @param n1 縦に移動する量(0 < n)
     * @param m1 横に移動する量(0 < m)
     */
    fun moveSelection(n1: Int, m1: Int) { //n,mは1~
        var n = n1
        var m = m1
        if (n < 1) {
            n = 1
        }
        if (m < 1) {
            m = 1
        }
        if (n > termDisplay.displayColumnSize) {
            n = termDisplay.displayColumnSize
        }
        if (m > termDisplay.displayRowSize) {
            m = termDisplay.displayRowSize
        }
        termDisplay.cursorY = 0
        termDisplay.cursorX = 0
        moveDown(n - 1)
        moveRight(m - 1)
    }

    /**
     * 画面消去
     */
    private fun clearDisplay() {
        var x = termDisplay.cursorX
        var i = x
        var length: Int
        val displaySize = termDisplay.displaySize
        for (y in termDisplay.cursorY until displaySize) {
            length = termDisplay.getRowLength(y + termDisplay.topRow)
            while (i < length) {
                if (termDisplay.getRowText(y + termDisplay.topRow).length > x) {
                    termDisplay.deleteTextItem(x, y + termDisplay.topRow)
                }
                i++
            }
            x = 0
            i = 0
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
            for (y in termDisplay.topRow..selectRowIndex) {
                for (x in 0 until termDisplay.getRowLength(y)) {
                    termDisplay.changeText(x, y, '\u0000')
                    if (y == selectRowIndex && x == termDisplay.cursorX) {
                        break
                    }
                }
            }
        }

        if (n == 2) { //全消去
            for (y in termDisplay.topRow until termDisplay.displaySize + termDisplay.topRow) {
                for (x in 0 until termDisplay.getRowLength(y)) {
                    termDisplay.changeTextItem(x, y, '\u0000', termDisplay.defaultColor)
                }
            }
        }

    }

    /**
     * @param n 行削除の方法
     */
    fun clearRow(n: Int) {
        if (n == 0) { //カーソル以降にある文字を消す
            val del = termDisplay.getRowLength(selectRowIndex) - termDisplay.cursorX
            for (x in 0 until del) {
                termDisplay.deleteTextItem(termDisplay.cursorX, selectRowIndex)
            }
        }

        if (n == 1) { //カーソル以前にある文字を消す
            for (x in 0..termDisplay.cursorX) {
                termDisplay.changeText(x, selectRowIndex, ' ')
            }
        }

        if (n == 2) { //全消去
            val del = termDisplay.getRowLength(selectRowIndex)
            for (x in 0 until del) {
                termDisplay.changeText(x, selectRowIndex, ' ')
            }

        }
    }

    /**
     * @param n 移動する量
     */
    fun scrollNext(n: Int) {
        if (termDisplay.topRow + n > termDisplay.totalColumns) return  //一番したに空白追加？？
        termDisplay.topRow = termDisplay.topRow + n
    }

    /**
     * @param n 移動する量
     */
    fun scrollBack(n: Int) {
        //一番上に空白追加で一番した削除？？？(あくまで画面上でスクロールしていると見せかけている?)
        if (termDisplay.topRow - n < 0) return
        termDisplay.topRow = termDisplay.topRow - n
    }

    /**
     * @param n 変化させる色
     */
    fun selectGraphicRendition(n: Int) { //色
        termDisplay.isColorChange = true
        when (n) {
            0  -> termDisplay.defaultColor = 0xFF000000.toInt()
            30 -> termDisplay.defaultColor = 0xFF000000.toInt()
            31 -> termDisplay.defaultColor = 0xFFff0000.toInt()
            32 -> termDisplay.defaultColor = 0xFF008000.toInt()
            33 -> termDisplay.defaultColor = 0xFFFFFF00.toInt()
            34 -> termDisplay.defaultColor = 0xFF0000FF.toInt()
            35 -> termDisplay.defaultColor = 0xFFFF00FF.toInt()
            36 -> termDisplay.defaultColor = 0xFF00FFFF.toInt()
            37 -> termDisplay.defaultColor = 0xFFFFFFFF.toInt()
            39 -> termDisplay.defaultColor = 0xFF000000.toInt()
        }//termDisplay.setDefaultColor(0x000000);
    }

    /**
     * @param x カーソルをx移動させる
     */
    private fun moveCursorX(x: Int) {
        termDisplay.cursorX = termDisplay.cursorX + x
    }

    /**
     * @param y カーソルをy移動させる
     */
    private fun moveCursorY(y: Int) {
        termDisplay.cursorY = termDisplay.cursorY + y
    }

}