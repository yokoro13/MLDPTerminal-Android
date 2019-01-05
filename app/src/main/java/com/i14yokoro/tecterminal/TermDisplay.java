package com.i14yokoro.tecterminal;

import android.util.Log;
import android.util.TimingLogger;

import java.util.ArrayList;

public class TermDisplay {
    private final String TAG = "termDisplay**";

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出

    private int displayRowSize, displayColumnSize;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int displayContentsLength = 0;
    private int displaySize = 0;

    private int topRow;

    private ArrayList<ArrayList<TextItem>> textList;

    private String defaultColor = "000000";

    public TermDisplay(int displayRowSize, int displayColumnSize){
        this.displayRowSize = displayRowSize;
        this.displayColumnSize = displayColumnSize;
        cursorX = 0;
        cursorY = 0;
        topRow = 0;
        display = new String[displayColumnSize][displayRowSize+1];
        ArrayList<TextItem> items = new ArrayList<>();
        textList = new ArrayList<>();
        textList.add(items);
        createDisplay();
    }

    public void setTextItem(String text, String color){

        TextItem textItem = new TextItem(text, color);
        int columnSize = getTotalColumns();
        //Log.d(TAG, "adding text: " + text);
        //TODO 途中に書き込むのに対応させる

        Log.d(TAG, "Add new textItem");
        textList.get(columnSize - 1).add(textItem);
        if(text.equals(LF) || textList.get(getTotalColumns()-1).size() >= displayRowSize){
            Log.d(TAG, "Add new line2");
            ArrayList<TextItem> items1 = new ArrayList<>();
            textList.add(items1);
        }

        //if(false && textList.get(getTopRow() + getCursorY()).size() >= displayRowSize){
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

    public void changeTextItem(int x, int y, String text, String color){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            TextItem textItem = new TextItem(text, color);
            this.textList.get(y).set(x, textItem);
        }
    }

    public void addTextItem(int y, String text, String color){
        if(y < this.textList.size()) {
            TextItem textItem = new TextItem(text, color);
            this.textList.get(y).add(textItem);
        }
    }

    public void insertRow(){

    }

    public void addTextItemOverSize(int y, String text, String color){
        TextItem textItem = new TextItem(text, color);
        ArrayList<TextItem> items = new ArrayList<>();
        items.add(textItem);
        Log.d(TAG, "Add new line1");
        textList.add(items);
    }

    public void addEmptyRow(){
        TextItem textItem = new TextItem("", getDefaultColor());
        ArrayList<TextItem> items1 = new ArrayList<>();
        //items1.add(textItem);
        textList.add(items1);
        Log.d(TAG, "Add new line1");
    }

    public void insertTextItem(int x, int y, String text, String color){
        int checkLF = x - 1;
        if(checkLF < 0){
            checkLF = 0;
        }
        if(y < this.textList.size() && x <= this.textList.get(y).size()) {
            if(textList.get(y).size() < displayRowSize && !getText(checkLF, y).equals(LF)) {
                //if(getRowText(getCursorY()).lastIndexOf(LF) == )
                TextItem textItem = new TextItem(text, color);
                this.textList.get(y).add(x, textItem);
            }
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

    public void changeColor(int x, int y, String color){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            this.textList.get(y).get(x).setColor(color);
        }
    }

    public String getColor(int x, int y){
        if(y < this.textList.size() && x < this.textList.get(y).size()) {
            return this.textList.get(y).get(x).getColor();
        } else {
            return null;
        }
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorX(int cursorX) {
        Log.d(TAG, "cursorX: " + cursorX);
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
                    //if(cursorX >= dis)
                    this.cursorX = cursorX;
                }
            }
        }
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        Log.d(TAG, "setCursorY: " + cursorY);

        if(cursorY >= displayColumnSize){
            //setTopRow(topRow+1);
            this.cursorY = displayColumnSize-1;
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
        TimingLogger logger = new TimingLogger("TAG_TEST", "create display");

        int displayY = 0;//displayの縦移動用
        displayContentsLength = 0;
        initialDisplay();

        for(int y = 0, totalColumns = getTotalColumns(); y < displayColumnSize; y++){//これはリストの縦移動用（最大でスクリーンの最大値分移動）

            if(displayY >= getDisplayColumnSize()){ //displayの描画が終わったらおわり
                break;
            }
            if((y >= totalColumns || y+topRow >= totalColumns)){ //yがリストよりでかくなったら
                //FIXME 最後に空白入るのここらへんが原因
                setDisplay(textList.get(totalColumns-1).size(), displayY, "EOL"); //最後に "EOL" という目印をつける
                break;
            }
            for (int x = 0, n = textList.get(y+topRow).size(); x < n; x++){ //xはそのyのサイズまで
                //Log.d(TAG, "get y+topRow" + (y+topRow));
                setDisplay(x, displayY, textList.get(y+topRow).get(x).getText()); //そのないようをdisplayに
                displayContentsLength++; //ついでにサイズも保存しておく
            }
            displayY++;
            //logger.addSplit("abeshi");
        }
        setDisplaySize(displayY);
        Log.d(TAG, "displaySize : " + displayY);
        setDisplayContentsLength(displayContentsLength);

        logger.dumpToLog();
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


    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }
}
