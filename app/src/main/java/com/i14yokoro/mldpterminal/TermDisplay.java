package com.i14yokoro.mldpterminal;

import android.util.Log;
import java.util.ArrayList;

public class TermDisplay{
    private static final String TAG = "termDisplay**";
    private static final char LF = '\n';

    private final int displayRowSize;
    private final int displayColumnSize;

    // カーソルの座標
    private int cursorX = 0;
    private int cursorY = 0;

    // 表示中の行数
    private int displaySize = 0;

    // 一番上の行
    private int topRow = 0;
    // 入力された文字を保存する
    private final ArrayList<ArrayList<TextItem>> textList;

    // 文字色(RGB)
    private int charColor = 0x000000;

    // 文字色を変えるエスケープシーケンスを受信したら true
    private boolean colorChange = false;

    // 表示する文字を組み立てる
    private final StringBuilder sb = new StringBuilder();

    // 現在カーソルがある行
    private int currRow = 0;

    // カーソルが画面の外にあれば true
    private boolean isOutOfScreen = false;

    /**
     * @param displayRowSize : 画面の横幅
     * @param displayColumnSize : 画面の縦幅
     */
    public TermDisplay(int displayRowSize, int displayColumnSize){
        this.displayRowSize = displayRowSize;
        this.displayColumnSize = displayColumnSize;

        ArrayList<TextItem> items = new ArrayList<>(displayRowSize);
        textList = new ArrayList<>();
        textList.add(items);
    }

    /**
     * リストに文字を記録する.
     *
     * @param text : 入力文字
     * @param color : 文字色
     */
    public void setTextItem(char text, int color){

        TextItem textItem = new TextItem(text, color);
        int columnSize = getTotalColumns();

        textList.get(columnSize - 1).add(textItem);
        if(text == LF || textList.get(getTotalColumns()-1).size() >= displayRowSize){
            ArrayList<TextItem> items1 = new ArrayList<>(displayRowSize+5);
            textList.add(items1);
        }
    }

    /**
     * y 行目 x 番目の文字をリストから消す．
     *
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     */
    public void deleteTextItem(int x, int y){
        textList.get(y).remove(x);
    }


    /**
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param text : 入力文字
     * @param color : 文字色
     */
    public void changeTextItem(int x, int y, char text, int color){
        if(y < textList.size() && x < textList.get(y).size()) {
            TextItem textItem = new TextItem(text, color);
            textList.get(y).set(x, textItem);
        }
    }

    /**
     * @param y : y 行目
     * @param text : 入力文字
     * @param color : 文字色
     */
    public void addTextItem(int y, char text, int color){
        if(y < textList.size()) {
            TextItem textItem = new TextItem(text, color);
            textList.get(y).add(textItem);
        }
        if((text == LF || textList.get(y).size() >= displayRowSize) && y == getTotalColumns()-1){
            ArrayList<TextItem> items1 = new ArrayList<>(displayRowSize);
            textList.add(items1);
        }
    }

    /**
     *
     */
    public void addEmptyRow(){
        ArrayList<TextItem> items1 = new ArrayList<>();
        textList.add(items1);
    }

    /**
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param text : 入力文字
     * @param color : 文字色
     */
    public void insertTextItem(int x, int y, char text, int color){
        if (y > textList.size() || textList.get(y).size() >= displayRowSize){ //行の長さが最大文字数をこえる
            return;
        }
        if(x <= textList.get(y).size()) { //行の範囲内
            if (getText(displayRowSize-1, y) == LF){ //一番最後がLF
                deleteTextItem(displayRowSize-1, y);
            }
            if(getText(x, y) != LF) { //LFじゃない
                addTextItem(y, text, color);
            } else {
                deleteTextItem(x, y);
                addTextItem(y, text, color);
                addTextItem(y, LF, color);
            }
        }
    }

    /**
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @param text : 入力文字
     */
    public void changeText(int x, int y, char text){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            this.textList.get(y).get(x).setText(text);
        }
    }

    /**
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @return
     */
    public char getText(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getText();
        } else {
            return '\u0000';
        }
    }

    /**
     * @param x : 現在の行の x 番目
     * @param y : y 行目
     * @return
     */
    public int getColor(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getColor();
        } else {
            return 0x000000;
        }
    }

    /**
     * @param y : y 行目
     * @return
     */
    public int getRowLength(int y){
        if(y >= getTotalColumns()){
            return 0;
        }
        if(textList.size() == 0){
            return 0;
        }
        return textList.get(y).size();
    }

    /**
     * @param y : y 行目
     * @return
     */
    public String getRowText(int y){
        StringBuilder str = new StringBuilder();
        for (int x = 0; x < getRowLength(y); x++){
            str.append(getText(x, y));
        }
        return str.toString();
    }

    public void moveTopRow(int count){
        this.topRow = this.topRow + count;
    }

    /**
     * @return
     */
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
        Log.d("changeTop", "top :" + (topRow + this.cursorY));
        setCurrRow(topRow + this.cursorY);
    }

    public int getTotalColumns() {
        return this.textList.size();
    }

    public int getDisplayRowSize() {
        return displayRowSize;
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

    public int getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }

    public int getCharColor() {
        return charColor;
    }

    public void setCharColor(int defaultColor) {
        this.charColor = defaultColor;
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

    public boolean isOutOfScreen() {
        return isOutOfScreen;
    }

    public void setOutOfScreen(boolean outOfScreen) {
        isOutOfScreen = outOfScreen;
    }
}
