package com.i14yokoro.mldpterminal

/**
 * １文字を表すクラス
 * 文字の情報と色の情報をもつ
 * 色は16進数(#AARRGGBB)
 */
class TerminalRow internal constructor(var text: CharArray, var color: IntArray, var lineWrap: Boolean)