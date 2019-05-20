package com.i14yokoro.mldpterminal;

/**
 * １文字を表すクラス
 * 文字の情報と色の情報をもつ　
 * 色は16進数(#AARRGGBB)
 */
public class TextItem {
    private char text;
    private int color;

    TextItem(char text, int color){
        this.text = text;
        this.color = color;
    }

    public void setText(char text) {
        this.text = text;
    }

    public char getText() {
        return text;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

}
