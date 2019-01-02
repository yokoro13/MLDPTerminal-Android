package com.i14yokoro.tecterminal;

import android.util.Log;

import java.util.ArrayList;

//TODO 改行コードを入れるようにdisplayの横幅を１つ増やす必要がありそう？？
public class TermDisplay {
    private final String TAG = "termDisplay**";

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出

    private int totalRows;

    private int displayRowSize, displayColumnSize;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int displayContentsLength = 0;
    private int displaySize = 0;

    private int topRow;
    private int inputRow;
    private ArrayList<ArrayList<TextItem>> textList;
    private TextItem textItem;

    public TermDisplay(int displayRowSize, int displayColumnSize){
        this.displayRowSize = displayRowSize+1;
        this.displayColumnSize = displayColumnSize;
        cursorX = 0;
        cursorY = 0;
        inputRow = 0;
        topRow = 0;
        display = new String[displayColumnSize][displayRowSize+1];
        textList = new ArrayList<>();
    }

    public void setTextItem(String text, int color){
        textItem = new TextItem(text, color);
        Log.d(TAG, "adding text: " + text);
        if(getTotalColumns() <= 0 ||textList.get(getTotalColumns()-1).size() >= displayRowSize-1|| textList.get(getTotalColumns()-1).get(textList.get(getTotalColumns()-1).size()-1).getText().equals("\n")){
            inputRow++;
            ArrayList<TextItem> items = new ArrayList<>();
            items.add(textItem);
            textList.add(items);
        }
        else {
            textList.get(getTotalColumns()-1).add(textItem);
        }
    }

    public void deleteTextItem(int x, int y){
        textList.get(y).remove(x);
    }

    public TextItem getTextItem(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return textList.get(y).get(x);
        } else {
            return null;
        }
    }

    public int getInputRow(){
        return inputRow;
    }

    public void setInputRow(int inputRow) {
        this.inputRow = inputRow;
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
        if(cursorX > textList.get(getCursorY() + topRow).size()){
            if(getCursorY()<displayColumnSize-1) {
                setCursorY(getCursorY() + 1);
            }
            this.cursorX = cursorX % displayRowSize;
        } else {
            if(cursorX < 0){
                if(0 < cursorY){
                    this.cursorX = textList.get(getCursorY() + topRow).size()-1;
                    setCursorY(getCursorY()-1);
                } else {
                    this.cursorX = 0;
                }
            } else {
                this.cursorX = cursorX;
            }
        }
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        Log.d(TAG, "setCursorY");

        if(cursorY > displayColumnSize){
            setTopRow(topRow+1);
        } else {
            //if(cursorY >= getDisplaySize()){
                //this.cursorY = getDisplaySize();
            //} else {
                if (cursorY < 0) {
                    this.cursorY = 0;
                } else {
                    this.cursorY = cursorY;
                }
            //}
        }
    }

    public String getDisplay(int x, int y) {
        if(display[y][x] == null){
            return "";
        }
        return display[y][x];
    }

    public void setDisplay(int x, int y, String c) {
        System.out.println(Integer.toString(x) + " " + Integer.toString(y) + " " + c);
        this.display[y][x] = c;
    }

    public int getTotalColumns() {
        return this.textList.size();
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
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

    public void addTopRow(int count){
        this.topRow = this.topRow + count;
    }

    public void createDisplay(){
        int displayY = 0;//displayの縦移動用
        displayContentsLength = 0;
        initialDisplay();

        for(int y = 0; y < displayColumnSize; y++){//これはリストの縦移動用（最大でスクリーンの最大値分移動）
            if(displayY >= getDisplayColumnSize()){ //displayの描画が終わったらおわり
                break;
            }
            if((y >= getTotalColumns() || y+topRow >= getTotalColumns())){ //yがリストよりでかくなったら
                setDisplay(textList.get(getTotalColumns()-1).size(), displayY, "EOL"); //最後に "EOL" という目印をつける
                break;
            }
            for (int x = 0; x < textList.get(y+topRow).size(); x++){ //xはそのyのサイズまで
                Log.d(TAG, "get y+topRow" + (y+topRow));
                setDisplay(x, displayY, textList.get(y+topRow).get(x).getText()); //そのないようをdisplayに
                displayContentsLength++; //ついでにサイズも保存しておく
            }
            displayY++;
            if (y == getTotalColumns()){
                break;
            }
        }
        setDisplaySize(displayY);
        Log.d(TAG, "displaySize : " + displayY);
        setDisplayContentsLength(displayContentsLength);
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
        String str = "";
        for (int x = 0; x < getRowLength(y); x++){
            str += getText(x, y);
        }
        return str;
    }

    public int getDisplayContentsLength(){
        return displayContentsLength;
    }

    private void setDisplayContentsLength(int displayContentsLength) {
        this.displayContentsLength = displayContentsLength;
    }

    private void initialDisplay(){
        for (int y = 0; y < displayColumnSize; y++){
            for (int x = 0; x < displayRowSize; x++){
                display[y][x] = "";
            }
        }
    }

    public int getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }
}
