package com.i14yokoro.tecterminal;

public class TextItem {
    private char text;
    private String color;

    TextItem(char text, String color){
        this.text = text;
        this.color = color;
    }

    public void setText(char text) {
        this.text = text;
    }

    public char getText() {
        return text;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

}
