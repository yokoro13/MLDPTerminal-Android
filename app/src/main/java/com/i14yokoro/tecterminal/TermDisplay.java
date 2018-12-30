package com.i14yokoro.tecterminal;

import java.util.ArrayList;

//TODO 改行コードを入れるようにdisplayの横幅を１つ増やす必要がありそう？？
public class TermDisplay {

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出

    private int totalRows;

    private int screenRows, screenColumns;

    private String[][] display;

    private int cursorX;
    private int cursorY;

    private int displaySize = 0;

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
        int move = 0;
        int displayY = 0;//displayの縦移動用
        displaySize = 0;

        for(int y = 0; y < screenColumns; y++){//これはリストの縦移動用（最大スクリーンの最大値分移動）
            if(displayY >= getScreenColumns()){ //displayの描画が終わったらおわり
                break;
            }
            if(y > getTotalColumns()){ //yがリストよりでかくなったら
                setDisplay(0, displayY, "EOL"); //そのyの最初に "EOL" という目印をつける
                break;
            }
            for (int x = 0; x < textList.get(y+topRow).size(); x++){ //xはそのyのサイズまで
                setDisplay(x, displayY, textList.get(y+topRow).get(x + move).getText()); //そのないようをdisplayに
                displaySize++; //ついでにサイズも保存しておく
                if(x > screenRows){ //その行の文字数が横文字の最大数をこえそうならもういっかいループ
                    move = x + move; //移動分をたす
                    y--;
                    displayY++; //displayのyは移動
                    break;
                }
                move = 0; //こえないならリセット
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

    public int getRowLength(int y){
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
        for(int y = 0; y < screenColumns; y++){
            if(getDisplay(0, y).equals("EOL")){
                break;
            }
            for (int x = 0; x < textList.get(y+topRow).size(); x++){
                if(getDisplay(x, y) == null){
                    break;
                }
                size += getDisplay(x,y).length();
                if(x > screenRows){
                    break;
                }
                if(getDisplay(x, y).equals(LF)){
                    break;
                }
            }
        }
        return size;
    }


}
