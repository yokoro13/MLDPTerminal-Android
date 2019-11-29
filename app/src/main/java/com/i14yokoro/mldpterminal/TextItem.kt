package com.i14yokoro.mldpterminal

/**
 * １文字を表すクラス
 * 文字の情報と色の情報をもつ　
 * 色は16進数(#AARRGGBB)
 */
class TextItem internal constructor(var text: Char, var color: Int)