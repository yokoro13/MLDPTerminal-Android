package com.i14yokoro.tecterminal;

import java.util.ArrayList;

public class TermDisplay {

    private int totalRows;

    private int screenRows, screenColumns;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int topRow;
    private ArrayList<ArrayList<TextItem>> items;

    public TermDisplay(int screenRows, int screenColumns){
        this.screenRows = screenRows;
        this.screenColumns = screenColumns;
        items = new ArrayList<>();
    }

    public void setText(int x, int y, String text){
        this.items.get(y).get(x).setText(text);
    }

    public String getText(int x, int y){
        return this.items.get(y).get(x).getText();
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorX(int cursorX) {
        if(cursorX > screenRows){
            this.cursorX = 0;
        } else {
            if(cursorX < 0){
                this.cursorX = 0;
            } else {
                this.cursorX = cursorX;
            }
        }
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        if(cursorY > screenColumns){
            setTopRow(topRow++);
        } else {
            if(cursorY < 0){
                this.cursorY = 0;
            } else {
                this.cursorY = cursorY;
            }
        }
    }

    public String getDisplay(int x, int y) {
        return display[y][x];
    }

    public void setDisplay(int x, int y, String c) {
        this.display[y][x] = c;
    }

    public int getTotalRows() {
        return this.items.size();
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getScreenRows() {
        return screenRows;
    }

    public int getScreenColumns() {
        return screenColumns;
    }

    public int getTopRow() {
        return topRow;
    }

    public void setTopRow(int topRow) {
        this.topRow = topRow;
    }
}
