package com.i14yokoro.mldpterminal

/**
 * １文字を表すクラス
 * 文字の情報と色の情報をもつ
 * 色は16進数(#AARRGGBB)
 */
class TerminalRow(var text: Array<Char>, var color: Array<Int>, var lineWrap: Boolean)