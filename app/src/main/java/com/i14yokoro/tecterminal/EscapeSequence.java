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
        if(editText.length() >= editText.getSelectionStart() + 1){
            editText.setSelection(editText.getSelectionStart() + 1);
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveLeft(){
        if(editText.getSelectionStart() - 1 >= 0) {
            editText.setSelection(editText.getSelectionStart() - 1);
        }
        else {
            editText.setSelection(0);
        }
    }

    public void moveUp(){
        //FIXME 上下移動のエズケープシーケンスの移動量算出をなおす
        if(editText.getSelectionStart() - getSelectRowLength(getSelectRow()) >= 0){
            Log.d(TAG,
                    "\n"
                            + "moveUP" + "\n"
                            + " goto " + (editText.getSelectionStart() - getSelectRowLength(getSelectRow())) + "\n"
                            + " selectRow " + getSelectRow() + " " + termDisplay.getRowLength(rowNumToListId(getSelectRow()))+ "\n"
                            + " length " +getSelectRowLength(getSelectRow()));
            if(getSelectRowLength(getSelectRow()) < getSelectRowLength(getSelectRow()+1)){
                editText.setSelection(getSelectRowLength(1, getSelectRow()+1)-1);
            }
            else {
                editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow()));
            }
        }
        else {
            editText.setSelection(0);
        }

    }
    public void moveDown(){
        if (editText.getSelectionStart() + getSelectRowLength(getSelectRow()) <= editText.length()){
            editText.setSelection(editText.getSelectionStart() + getSelectRowLength(getSelectRow()));
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveRight(int n){
        if(editText.length() >= editText.getSelectionStart() + n){
            editText.setSelection(editText.getSelectionStart() + n);
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveLeft(int n){
        if(editText.getSelectionStart() - n >= 0) {
            editText.setSelection(editText.getSelectionStart() - n);
        }
        else {
            editText.setSelection(0);
        }
    }

    public void moveUp(int n){
        if(getSelectRow() - n < 0 || n <= 0){
            return;
        }
        Log.d(TAG,
                "\n moveUP" + "\n" + " goto " + (editText.getSelectionStart() - getSelectRowLength(getSelectRow() - n, getSelectRow())) + "\n"
                        +" selectRow " + getSelectRow() + "\n" + " length " +getSelectRowLength(getSelectRow()));

        if(editText.getSelectionStart() - getSelectRowLength(getSelectRow() - n, getSelectRow()) >= 0){
            editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow() - n, getSelectRow()));
        }
        else {
            editText.setSelection(0);
        }

    }
    public void moveDown(int n){
        Log.d(TAG,
                "\n moveUP" + "\n" + " goto " + (editText.getSelectionStart() + getSelectRowLength(getSelectRow(), getSelectRow() + n)) + "\n"
                        +" selectRow " + getSelectRow() + "\n" + " length " + getSelectRowLength(getSelectRow(), getSelectRow() + n));

        if (getSelectRow() + n > termDisplay.getTotalColumns() || n <= 0){
            return;
        }
        if (editText.getSelectionStart() + getSelectRowLength(getSelectRow(), getSelectRow() + n) < editText.length()){
            editText.setSelection(editText.getSelectionStart() + getSelectRowLength(getSelectRow(), getSelectRow() + n));
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveRowUp(int n){
        if(editText.getSelectionStart() - getSelectRowLength(getSelectRow() - n, getSelectRow()) * n > 0){
            editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow()) - getSelectRowLength(getSelectRow() - n, getSelectRow()));
        }
        else {

        }
    }

    public void moveRowDown(int n){
        if (editText.getSelectionStart() + getSelectRowLength(getSelectRow(), getSelectRow() + n) < editText.length()){
            editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow()) + getSelectRowLength(getSelectRow(), getSelectRow() + n));
        }
    }

    public void moveSelection(int n){
        editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow()) + n);
    }

    public void moveSelection(int n, int m){
        editText.setSelection(getSelectRowLength(0, n) + m);
    }

    public void clearDisplay(){
        //カーソルをしゅとく
        //０から取得したカーソルまでの部分をedittextから切り取る
        //貼り付け
        //getSelection % maxChar番地 のリストからうしろをクリア
    }

    public void clearDisplay(int n){
        if(n == 0){

        }
    }

    public void clearRow(){

    }

    public void clearRow(int n){

    }

    public void scrollNext(int n){
        if (getTop() + n > termDisplay.getTotalColumns()) return;
        setTop(getTop()+n);
        changeDisplay();
    }

    public void scrollBack(int n){

        if(getTop() - n < 0) return;
        setTop(getTop()-n);
        changeDisplay();
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
        if (rowNum > 0) {
            return getTop()-1 + rowNum;
        }
        return 0;

    }

    public void changeDisplay(){
        int topRow = termDisplay.getTopRow();
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
