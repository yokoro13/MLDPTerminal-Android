package com.i14yokoro.tecterminal;

public class TermDisplay {

    private int totalRows;

    private int screenRows, screenColumns;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int topRow;

    public TermDisplay(int screenRows, int screenColumns){
        this.screenRows = screenRows;
        this.screenColumns = screenColumns;
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorx(int cursorX) {
        this.cursorX = cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        this.cursorY = cursorY;
    }

    public String getDisplay(int x, int y) {
        return display[y][x];
    }

    public void setDisplay(int x, int y, String c) {
        this.display[y][x] = c;
    }

    public int getTotalRows() {
        return totalRows;
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
}
