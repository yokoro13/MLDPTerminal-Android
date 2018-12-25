package com.i14yokoro.tecterminal;

public class TextItem {
    private String text;
    private int color;

    TextItem(String text, int color){
        this.text = text;
        this.color = color;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

}
