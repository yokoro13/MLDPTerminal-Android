package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.EditText;

public class EscapeSequence {
    private String TAG = "**debug**";
    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出

    private EditText editText;
    private TermDisplay termDisplay;

    EscapeSequence(Context context, TermDisplay termDisplay){
        this.editText = (EditText) ((MainActivity)context).findViewById(R.id.main_display);
        this.termDisplay = termDisplay;
    }

    public int getTop(){
        return termDisplay.getTopRow();
    }

    public void setTop(int top){
        termDisplay.setTopRow(top);
    }

    public void moveRight(){
        moveCursorX(1);
    }

    public void moveLeft(){
        moveCursorX(-1);
    }

    public void moveUp(){
        moveCursorY(-1);
    }
    public void moveDown(){
        moveCursorY(1);
    }

    public void moveRight(int n){
        if(termDisplay.getCursorX() + n < termDisplay.getRowLength(getSelectRowIndex())) {
            moveCursorX(n);
        } else {
            int move = n;
            int add = 0;
            if(termDisplay.getCursorX() + n >= termDisplay.getDisplayRowSize()){
                add = termDisplay.getDisplayRowSize() - termDisplay.getRowLength(getSelectRowIndex());
                move = termDisplay.getDisplayRowSize() - termDisplay.getCursorX();
            } else {
                add = termDisplay.getCursorX() + n - termDisplay.getRowLength(getSelectRowIndex());
            }
            addBlank(add);
            moveCursorX(move);
        }

    }

    public void moveLeft(int n){
        if(termDisplay.getCursorX() - n >= 0){
            moveCursorX(-n);
        }
    }

    public void moveUp(int n){
        if(termDisplay.getCursorY() - n < 0){
            termDisplay.setCursorY(0);
        } else {
            moveCursorY(-n);
        }
        if(termDisplay.getRowLength(termDisplay.getTopRow() + termDisplay.getCursorY()) < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - termDisplay.getRowLength(termDisplay.getTopRow() + termDisplay.getCursorY());
            addBlank(add);
        }

    }
    public void moveDown(int n){
        if(termDisplay.getCursorY() + n >= termDisplay.getDisplayColumnSize()) { //移動先が一番下の行を超える場合
            if(termDisplay.getCursorY() + n >= termDisplay.getDisplayColumnSize()){ //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
            int move = termDisplay.getCursorY() - termDisplay.getDisplayColumnSize();
            for (int i = 0; i < move; i++){
                termDisplay.addEmptyRow();
            }
        }else { //移動先が一番下の行を超えない
            if (termDisplay.getCursorY() + n > termDisplay.getDisplaySize()) { //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
        }
        //移動先の文字数がcursorXより小さい
        if(termDisplay.getRowLength(termDisplay.getTopRow() + termDisplay.getCursorY()) < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - termDisplay.getRowLength(termDisplay.getTopRow() + termDisplay.getCursorY());
            addBlank(add);
        }
    }

    public void moveUpToRowLead(int n){
        termDisplay.setCursorX(0);
        moveUp(n);
    }

    public void moveDownToRowLead(int n){
        termDisplay.setCursorX(0);
        moveDown(n);
    }

    public void moveSelection(int n){
        termDisplay.setCursorX(0);
        moveRight(n);
    }

    public void moveSelection(int n, int m){ //n,mは1~
        if(n < 1){
            n = 1;
        }
        if(m < 1){
            m = 1;
        }
        termDisplay.setCursorX(0);
        termDisplay.setCursorY(0);
        moveRight(n-1);
        moveDown(m-1);
    }

    public void clearDisplay(){
        //カーソルをしゅとく
        //０から取得したカーソルまでの部分をedittextから切り取る
        //貼り付け
        //getSelection % maxChar番地 のリストからうしろをクリア
        int x = termDisplay.getCursorX();
        for(int y = termDisplay.getCursorY(); y < termDisplay.getDisplaySize(); y++){
            for (; x < termDisplay.getRowLength(y); x++){
                termDisplay.deleteTextItem(x, y);
            }
            x = 0;
        }
    }

    public void clearDisplay(int n){
        //一番下までスクロールして消去ぽい
        if(n == 0){ //カーソルより後ろにある画面上の文字を消す
            clearDisplay();
        }

        if(n == 1){ //カーソルより前にある画面上の文字を消す
            for (int y = 0; y <= termDisplay.getCursorY(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    if(y == termDisplay.getCursorY()){
                        termDisplay.deleteTextItem(x, y);
                        if(x == termDisplay.getCursorX()){
                            break;
                        }
                    }
                }
            }
        }

        if(n == 2){ //全消去
            for (int y = 0; y < termDisplay.getDisplaySize(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.deleteTextItem(x, y);
                }
            }
        }

    }

    public void clearRow(){
        for (int x = termDisplay.getCursorX(); x < termDisplay.getRowLength(termDisplay.getCursorY()); x++){
            termDisplay.deleteTextItem(x, termDisplay.getCursorY());
        }
    }

    public void clearRow(int n){
        if(n == 0){ //カーソル以降にある文字を消す
            clearRow();
        }

        if(n == 1){ //カーソル以前にある文字を消す
            for (int x = 0; x <= termDisplay.getCursorX(); x++){
                termDisplay.deleteTextItem(x, termDisplay.getCursorY());
            }
        }

        if(n == 2){ //全消去
            for (int x = 0; x < termDisplay.getRowLength(termDisplay.getCursorY()); x++){
                termDisplay.deleteTextItem(x, termDisplay.getCursorY());
            }

        }
    }

    public void scrollNext(int n){
        if (getTop() + n > termDisplay.getTotalColumns()) return; //一番したに空白追加？？
        setTop(getTop()+n);
    }

    public void scrollBack(int n){
        //一番上に空白追加で一番した削除？？？(あくまで画面上でスクロールしていると見せかけている?)
        if(getTop() - n < 0) return;
        setTop(getTop()-n);
    }

    public void selectGraphicRendition(){ //色

    }

    //row行までの文字数をかえす
    private int getLength(int row){
        int length = 0;
        int rowId = rowNumToListId(row);
        for(int i = 0; i < rowId; i++){
            length += termDisplay.getRowLength(i);
            Log.d(TAG, "getLength" + Integer.toString(termDisplay.getRowLength(i)));
        }
        return length;
    }

    //ディスプレイ上で選択中の行番号を返す
    //rowは0から
    public int getSelectRow(){
        int start = editText.getSelectionStart();
        int row = termDisplay.getTopRow()+1;
        //int count = items.get(getTop()).getText().length();
        int count = termDisplay.getRowLength(getTop());

        if(row < 1){
            return 0;
        }
        for (; count < start; row++){
            if(row < termDisplay.getTotalColumns()){
                count += termDisplay.getRowLength(row);
            }
            else break;

            //Log.d(TAG, "count : " + count);
        }
        //Log.d(TAG, "number/ " + Integer.toString(row-1) + " contents/ " + termDisplay.getRowText(row-1));
        //Log.d(TAG, "row/: " + (row-1 - termDisplay.getTopRow()));
        return row - termDisplay.getTopRow() - 1;
    }

    private int getSelectRowLength(int selectRow){
        Log.d(TAG, "getSelectionRowIndex : " + (rowNumToListId(selectRow)));
        return termDisplay.getRowLength(rowNumToListId(selectRow));
    }

    //start行からrow行までの文字数を返す
    //FIXME 上下移動のエズケープシーケンスの移動量算出をなおす
    private int getSelectRowLength(int start, int end){
        int length = 0;
        int startId = rowNumToListId(start);
        int endId = rowNumToListId(end);
        if(endId > termDisplay.getTotalColumns()){
            endId = termDisplay.getTotalColumns();
        }
        for(int i = startId; i < endId; i++){
            length += termDisplay.getRowLength(i);
        }
        return length;
    }

    private int rowNumToListId(int rowNum){
        if (rowNum >= 0) {
            return rowNum + termDisplay.getTopRow();
        }
        return 0;
    }

    public void changeDisplay(){
        String output = "";
        //Log.d(TAG, "topRow/ " + topRow);
        editText.setText("");
        termDisplay.createDisplay();
        TimingLogger logger = new TimingLogger("TAG_TEST", "change display");
        for (int y = 0; y < termDisplay.getTotalColumns() && y < termDisplay.getDisplayColumnSize(); y++){
            for (int x = 0; x < termDisplay.getDisplayRowSize(); x++){
                if(!termDisplay.getDisplay(x, y).equals("EOL")) {
                    //できれば，文字列を作ってからsetTextでいいかも
                    output = output + termDisplay.getDisplay(x, y);

                } else{
                    editText.setText(output);
                    logger.dumpToLog();
                    return;
                }
                if(x == termDisplay.getDisplayRowSize()-1){
                    output = output + LF;
                    //editText.append(LF);
                }
                if(termDisplay.getDisplay(x, y).equals(LF)){
                    break;
                }
            }
        }
        editText.setText(output);
        logger.dumpToLog();
    }

    private void addBlank(int n){
        for (int i = 0; i < n; i++){
            termDisplay.addTextItem(getSelectRowIndex()+termDisplay.getCursorY(), " ", 0);
        }
    }

    public void setCursol(int x, int y){
        int move = getSelectRowLength(0, y) + x;
        editText.setSelection(move);
    }

    private int getSelectRowIndex() {
        return getSelectRow() + termDisplay.getTopRow();
    }

    private String getSelectLineText(){
        return termDisplay.getRowText(getSelectRowIndex());
    }

    private void moveCursorX(int x){
        termDisplay.setCursorX(termDisplay.getCursorX() + x);
    }

    private void moveCursorY(int y){
        termDisplay.setCursorY(termDisplay.getCursorY() + y);
    }

    private void setCursor(int x, int y){
        termDisplay.setCursorX(x);
        termDisplay.setCursorY(y);
    }


}
