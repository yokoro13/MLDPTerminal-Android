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

    private int displaySize = 0;

    private int topRow;
    private int inputRow;
    private ArrayList<ArrayList<TextItem>> textList;
    private TextItem textItem;

    public TermDisplay(int displayRowSize, int displayColumnSize){
        this.displayRowSize = displayRowSize+1;
        this.displayColumnSize = displayColumnSize;
        inputRow = 0;
        display = new String[displayColumnSize][displayRowSize+1];
        textList = new ArrayList<>();
    }

    public void setTextItem(String text, int color){
        textItem = new TextItem(text, color);
        Log.d(TAG, "adding text: " + text);
        if(getTotalColumns() <= 0 || textList.get(getTotalColumns()-1).get(textList.get(getTotalColumns()-1).size()-1).getText().equals("\n")){
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
        if(cursorX > displayRowSize){
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
        if(cursorY > displayColumnSize){
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
        int move = 0;
        displaySize = 0;
        initialDisplay();

        for(int y = 0; y < displayColumnSize; y++){//これはリストの縦移動用（最大でスクリーンの最大値分移動）
            if(displayY >= getDisplayColumnSize()){ //displayの描画が終わったらおわり
                break;
            }
            if(y >= getTotalColumns() || y+topRow >= getTotalColumns()){ //yがリストよりでかくなったら
                setDisplay(0, displayY, "EOL"); //そのyの最初に "EOL" という目印をつける
                break;
            }
            for (int x = 0; x < textList.get(y+topRow).size(); x++){ //xはそのyのサイズまで
                int usedColumnLength = textList.get(y).size()/displayRowSize;
                int lastRowPart = textList.get(y).size()%displayRowSize;
                if(lastRowPart > 0){
                    usedColumnLength++;
                }
                if(textList.get(y+topRow).size() > displayRowSize){
                    String partStr = "";

                    int size = textList.get(y).size();
                    for (int movex = size - lastRowPart; movex < size; movex++){
                        partStr = textList.get(y).get(movex).getText();
                    }
                }
                if(x != 0 && x % displayRowSize == 0){
                    displayY++; //displayのyは移動

                }
                setDisplay(x % displayRowSize, displayY, textList.get(y+topRow).get(x).getText()); //そのないようをdisplayに
                displaySize++; //ついでにサイズも保存しておく

                if(textList.get(y+topRow).get(x).getText().equals(LF)){ //改行はあれば次のyへ
                    displayY++;
                    break;
                }
            }
            if (y == getTotalColumns()){
                break;
            }
        }
        setDisplaySize(displaySize);
    }

    public String subListContent(int y){
        String partStr = "";
        int lastRowPart = textList.get(y).size()%displayRowSize;
        int size = textList.get(y).size();
        for (int x = size - lastRowPart; x < size; x++){
            partStr = textList.get(y).get(x).getText();
        }
        return partStr;
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

    public int getDisplaySize(){
        return displaySize;
    }

    private void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }

    public int getDisplayContentsSize(){
        int size = 0;
        for(int y = 0; y < displayColumnSize; y++){
            if(getDisplay(0, y).equals("EOL")){
                break;
            }
            for (int x = 0; x < textList.get(y+topRow).size(); x++){
                if(getDisplay(x, y) == null){
                    break;
                }
                size += getDisplay(x,y).length();
                if(x > displayRowSize){
                    break;
                }
                if(getDisplay(x, y).equals(LF)){
                    break;
                }
            }
        }
        return size;
    }

    private int getDisplayRange(){
        int range = 0;
        for (int y = 0;y + topRow < textList.size() && range < displayColumnSize; y++){
            range = range + textList.get(y).size()/displayRowSize + 1;
        }
        return range;
    }

    private void initialDisplay(){
        for (int y = 0; y < displayColumnSize; y++){
            for (int x = 0; x < displayRowSize; x++){
                display[y][x] = "";
            }
        }
    }
}
