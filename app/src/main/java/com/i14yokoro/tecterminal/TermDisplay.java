package com.i14yokoro.tecterminal;

import java.util.ArrayList;

public class TermDisplay {

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出

    private int totalRows;

    private int screenRows, screenColumns;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int topRow;
    private ArrayList<ArrayList<TextItem>> textList;
    private TextItem textItem;

    public TermDisplay(int screenRows, int screenColumns){
        this.screenRows = screenRows;
        this.screenColumns = screenColumns;
        textList = new ArrayList<>();
    }

    public void setTextItem(String text, int color){
        textItem = new TextItem(text, color);
        this.textList.get(getTotalColumns()).add(textItem);
    }

    public TextItem getTextItem(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x);
        } else {
            return null;
        }

    }

    public void changeText(int x, int y, String text){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            this.textList.get(y).get(x).setText(text);
        }
    }

    public String getText(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getText();
        } else {
            return null;
        }
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

    public int getTotalColumns() {
        return this.textList.size();
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

    public void addTopRow(int count){
        this.topRow = this.topRow + count;
    }

    public void createDisplay(){
        for(int y = 0; y < screenColumns; y++){
            for (int x = 0; x < textList.get(y+topRow).size(); x++){
                setDisplay(x, y, textList.get(y+topRow).get(x).getText());
                if(x > screenRows){
                    break;
                }
                if(textList.get(y+topRow).get(x).getText().equals(LF)){
                    break;
                }
            }
        }
    }

    public int getRowSize(int y){
        return this.textList.get(y).size();
    }
}
