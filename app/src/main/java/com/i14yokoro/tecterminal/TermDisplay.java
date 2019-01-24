package com.i14yokoro.tecterminal;

import android.util.Log;

import java.util.ArrayList;

public class TermDisplay{
    private static final String TAG = "termDisplay**";
    private static final char LF = '\n'; //システムの改行コードを検出

    private int displayRowSize, displayColumnSize;

    private int cursorX = 0;
    private int cursorY = 0;

    private int displaySize = 0;

    private int topRow = 0;
    private ArrayList<ArrayList<TextItem>> textList;

    private int defaultColor = 0x000000;

    private boolean colorChange = false;

    StringBuilder sb = new StringBuilder();

    private int currRow = 0;

    public TermDisplay(int displayRowSize, int displayColumnSize){
        this.displayRowSize = displayRowSize;
        this.displayColumnSize = displayColumnSize;

        ArrayList<TextItem> items = new ArrayList<>(displayRowSize);
        textList = new ArrayList<>();
        textList.add(items);
    }

    public void setTextItem(char text, int color){

        TextItem textItem = new TextItem(text, color);
        int columnSize = getTotalColumns();

        textList.get(columnSize - 1).add(textItem);
        if(text == LF || textList.get(getTotalColumns()-1).size() >= displayRowSize){
            ArrayList<TextItem> items1 = new ArrayList<>(displayRowSize+5);
            textList.add(items1);
            Log.d("termDisplay***", "Add new line1");
        }
    }

    public void deleteTextItem(int x, int y){
        textList.get(y).remove(x);
    }

    public void changeTextItem(int x, int y, char text, int color){
        if(y < textList.size() && x < textList.get(y).size()) {
            TextItem textItem = new TextItem(text, color);
            textList.get(y).set(x, textItem);
        }
    }

    public void addTextItem(int y, char text, int color){
        if(y < textList.size()) {
            TextItem textItem = new TextItem(text, color);
            textList.get(y).add(textItem);
        }
        if((text == LF || textList.get(y).size() >= displayRowSize) && y == getTotalColumns()-1){
            ArrayList<TextItem> items1 = new ArrayList<>(displayRowSize);
            textList.add(items1);
            Log.d("termDisplay***", "Add new line2");
        }
    }

    public void addEmptyRow(){
        ArrayList<TextItem> items1 = new ArrayList<>();
        textList.add(items1);
        Log.d("termDisplay***", "Add new line3");
    }

    public void insertTextItem(int x, int y, char text, int color){
        int checkLF = x - 1;
        if(checkLF < 0){
            checkLF = 0;
        }
        if(y < textList.size() && x <= textList.get(y).size()) {
            if (getRowLength(y) >= displayRowSize && getText(displayRowSize-1, y) == LF){
                deleteTextItem(displayRowSize-1, y);
            }
            //Log.d(TAG, "insert to :" + x + ", " + y);
            if(textList.get(y).size() < displayRowSize && getText(checkLF, y) != LF) {
                //if(getRowText(getCursorY()).lastIndexOf(LF) == )
                TextItem textItem = new TextItem(text, color);
                textList.get(y).add(x, textItem);
            } else {
                if(getText(checkLF, y) == LF){
                    deleteTextItem(checkLF, y);
                    TextItem textItem = new TextItem(text, color);
                    textList.get(y).add(checkLF, textItem);
                    textItem = new TextItem(LF, color);
                    textList.get(y).add(textItem);
                }
            }
        }
    }

    public void changeText(int x, int y, char text){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            this.textList.get(y).get(x).setText(text);
        }
    }

    public char getText(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getText();
        } else {
            return '\u0000';
        }
    }

    public int getColor(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getColor();
        } else {
            return 0x000000;
        }
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorX(int cursorX) {
        if(cursorX >= displayRowSize){
            this.cursorX = displayRowSize - 1;
        }else {
            if (cursorX >= textList.get(getCursorY() + topRow).size()) {
                this.cursorX = cursorX % displayRowSize;
                Log.d(TAG, Integer.toString(getCursorX()));
            } else {
                if (cursorX < 0) {
                    this.cursorX = 0;
                } else {
                    this.cursorX = cursorX;
                }
            }
        }
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        if(cursorY >= displayColumnSize){
            this.cursorY = displayColumnSize-1;
        } else {
            if (cursorY < 0) {
                this.cursorY = 0;
            } else {
                this.cursorY = cursorY;
            }
        }
        setCurrRow(topRow + this.cursorY);
    }

    public int getTotalColumns() {
        return this.textList.size();
    }

    public void setDisplayRowSize(int displayRowSize) {
        this.displayRowSize = displayRowSize;
    }

    public int getDisplayRowSize() {
        return displayRowSize;
    }

    public void setDisplayColumnSize(int displayColumnSize) {
        this.displayColumnSize = displayColumnSize;
    }

    public int getDisplayColumnSize() {
        return displayColumnSize;
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

    public String createDisplay(){
        sb.setLength(0);
        char text;

        int totalColumns = getTotalColumns();
        int displayRowSize = getDisplayRowSize();
        int displayColumnSize = getDisplayColumnSize();

        for(int y = 0; y < displayColumnSize; y++){//これはリストの縦移動用（最大でスクリーンの最大値分移動）

            if((y+topRow >= totalColumns)){ //yがリストよりでかくなったら
                setDisplaySize(y-1);
                return sb.toString();
            }
            for (int x = 0, n = textList.get(y+topRow).size(); x < n; x++){ //xはそのyのサイズまで
                text = textList.get(y+topRow).get(x).getText();
                if (text == LF && y+1 == displayColumnSize){
                    break;
                }
                if (!colorChange){
                    sb.append(text);
                } else {
                    sb.append("<font color=#").append(Integer.toHexString(getColor(x, getTopRow() + y))).append(">").append(text).append("</font>");
                }
                if((x == displayRowSize-1) && text != LF && y+1 != displayColumnSize){
                    sb.append(LF);
                    break;
                }
                if(text == LF){
                    if (y-1 == displayColumnSize){
                        sb.deleteCharAt(sb.length()-1);
                    }
                    break;
                }
            }
            if (y+1 < displayColumnSize && y + getTopRow() < totalColumns - 1 && !getRowText(y + getTopRow()).contains("\n") && textList.get(y + topRow).size() < getDisplayRowSize()) {
                sb.append(LF);
            }
        }
        setDisplaySize(displayColumnSize);

        return sb.toString();
    }

    public int getRowLength(int y){
        if(y >= getTotalColumns()){
            return 0;
        }
        if(textList.size() == 0 || textList.get(y).size() == 0){
            return 0;
        }
        return this.textList.get(y).size();
    }

    public String getRowText(int y){
        StringBuilder str = new StringBuilder();
        for (int x = 0; x < getRowLength(y); x++){
            str.append(getText(x, y));
        }
        return str.toString();
    }

    public int getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(int defaultColor) {

        this.defaultColor = defaultColor;
    }

    public void setColorChange(boolean colorChange){
        this.colorChange = colorChange;
    }

    public boolean isColorChange(){
        return this.colorChange;
    }

    public int getCurrRow() {
        return currRow;
    }

    public void setCurrRow(int currRow) {
        this.currRow = currRow;
    }
}
