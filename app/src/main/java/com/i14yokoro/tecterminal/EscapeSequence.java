package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;

/**
 *
 **/
public class EscapeSequence {
    private String TAG = "**debug**";
    private EditText editText;
    private Context context;
    private int width;
    private ArrayList<RowItem> items;
    private int height;
    private int top;

    EscapeSequence(Context context, ArrayList<RowItem> items,int width, int height){
        this.context = context;
        this.editText = (EditText) ((MainActivity)context).findViewById(R.id.main_display);
        this.items = items;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height){
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getTop(){
        return top;
    }
    public void setTop(int top){
        this.top = top;
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
        /**
         if(editText.getSelectionStart() - getLength(getSelectRow()-n, getSelectRow()) >= 0){
         //editText.setSelection(editText.getSelectionStart() - getLength(getSelectRow()-n, getSelectRow()));
         editText.setSelection(editText.getSelectionStart()-42);
         }**/
        Log.d(TAG,
                "\n moveUP" + "\n" + " goto " + (editText.getSelectionStart() - getSelectRowLength(getSelectRow())) + "\n"
                        +" selectRow " + getSelectRow() + "\n" + " length " +getSelectRowLength(getSelectRow()));
        //FIXME 上下移動のエズケープシーケンスの移動量算出をなおす
        if(editText.getSelectionStart() - getSelectRowLength(getSelectRow()) >= 0){
            editText.setSelection(editText.getSelectionStart() - getSelectRowLength(getSelectRow()));
        }
        else {
            editText.setSelection(0);
        }

    }
    public void moveDown(){
        /**
         if(editText.getSelectionStart() + getLength( getSelectRow(), getSelectRow() + n) < editText.length()){
         editText.setSelection(editText.getSelectionStart() + getLength( getSelectRow(), getSelectRow() + n));
         }**/
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
        /**
        if(editText.getSelectionStart() - getLength(getSelectRow()-n, getSelectRow()) >= 0){
            //editText.setSelection(editText.getSelectionStart() - getLength(getSelectRow()-n, getSelectRow()));
            editText.setSelection(editText.getSelectionStart()-42);
        }**/


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
        /**
        if(editText.getSelectionStart() + getLength( getSelectRow(), getSelectRow() + n) < editText.length()){
            editText.setSelection(editText.getSelectionStart() + getLength( getSelectRow(), getSelectRow() + n));
        }**/
        Log.d(TAG,
                "\n moveUP" + "\n" + " goto " + (editText.getSelectionStart() + getSelectRowLength(getSelectRow(), getSelectRow() + n)) + "\n"
                        +" selectRow " + getSelectRow() + "\n" + " length " + getSelectRowLength(getSelectRow(), getSelectRow() + n));

        if (getSelectRow() + n > items.size() || n <= 0){
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
        String str = editText.getText().subSequence(0, editText.getSelectionStart()-1).toString();
        editText.setText(str);
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
        if (top + n > items.size()) return;
        setTop(top+n);
        changeDisplay();
    }

    public void scrollBack(int n){

        if(top - n < 0) return;
        setTop(top-n);
        changeDisplay();
    }

    //row行までの文字数をかえす
    private int getLength(int row){
        int length = 0;
        int rowId = rowNumToListId(row);
        for(int i = 0; i < rowId; i++){
            length += items.get(i).getText().length();
            Log.d(TAG, "getLength" + Integer.toString(items.get(i).getText().length()));
        }
        return length;
    }


    //ディスプレイ上で選択中の行番号を返す
    //FIXME rowに基準がわからん
    public int getSelectRow(){
        int count = 0;
        int start = editText.getSelectionStart();
        int row = getTop();
        if(row < 1){
            return 0;
        }
        for (; count < start; row++){
            if(row < items.size()){
                count += items.get(row).getText().length();
            }
            else break;
        }
        Log.d(TAG, "number/ " + Integer.toString(row-1) + " contents/ " + items.get(row-1).getText());
        return row-1;
    }

    private int getSelectRowLength(int selectRow){
        Log.d(TAG, "getSelectionRowLength : " + rowNumToListId(selectRow));
        return items.get(rowNumToListId(selectRow)).getText().length();
    }

    //start行からrow行までの文字数を返す
    //FIXME 上下移動のエズケープシーケンスの移動量算出をなおす
    private int getSelectRowLength(int start, int end){
        int length = 0;
        int startId = rowNumToListId(start);
        int endId = rowNumToListId(end);
        for(int i = startId; i < endId; i++){
            length += items.get(i).getText().length();
        }
        return length;
    }

    private int rowNumToListId(int rowNum){
        return getTop() + rowNum;
    }

    public void changeDisplay(){
        int topRow = getTop();
        Log.d(TAG, "topRow/ " + topRow);
        editText.setText("");
        for (int i = topRow; i < items.size() && i < topRow+height-1; i++){
            Log.d("debug***", items.get(i).getText());
            editText.append(items.get(i).getText());
        }
    }

    /**
    //端っこのいくまでスペース追加
    private void addSpace() {
        editingFlag = false;

        StringBuilder space = new StringBuilder();
        Log.d(TAG, "add Space");
        for (int i = lineText.length() % maxRowLength; i <= maxRowLength; i++) {
            space.append(" ");
        }
        inputEditText.append(space.toString());
        lineText += space;
        editingFlag = true;
    }**/

}
