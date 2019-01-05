package com.i14yokoro.tecterminal;

public class TextItem {
    private String text;
    private String color;

    TextItem(String text, String color){
        this.text = text;
        this.color = color;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

}
