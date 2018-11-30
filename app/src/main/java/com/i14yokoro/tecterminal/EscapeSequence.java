package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;

public class EscapeSequence {
    private EditText editText;
    private Context context;
    private int max;
    private ArrayList<RowItem> items;

    EscapeSequence(Context context, ArrayList<RowItem> items,int max){
        this.context = context;
        this.editText = (EditText) ((MainActivity)context).findViewById(R.id.main_display);
        this.items = items;
        this.max = max;
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
        if (getSelectRow() < items.size()){
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
        if(getSelectRow() - n >= 0 || n <= 0){
            return;
        }
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
        if (getSelectRow() + n < items.size() && n <= 0){
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
        editText.setSelection(max * n + m);
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

    }

    public void scrollBack(int n){

    }

    //row行までの文字数をかえす
    private int getLength(int row){
        int length = 0;
        for(int i = 0; i < row; i++){
            length += items.get(i).getText().length();
            Log.d("debug****", Integer.toString(items.get(i).getText().length()));
        }
        return length;
    }


    //選択中の行番号を返す
    private int getSelectRow(){
        int count = 0;
        int start = editText.getSelectionStart();
        int row = 0;
        for (; count < start; row++){
            if(row < items.size() - 1){
                count += items.get(row).getText().length();
            }
            else break;
        }
        Log.d("debug**** / getselect", Integer.toString(row));
        return row;
    }

    private int getSelectRowLength(int selectRow){
        return items.get(selectRow).getText().length();
    }

    //start行からrow行までの文字数を返す
    private int getSelectRowLength(int start, int end){
        int length = 0;
        for(int i = start; i <= end; i++){
            length += items.get(i).getText().length();
        }
        return length;
    }

}
