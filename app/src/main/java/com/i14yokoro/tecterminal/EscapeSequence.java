package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;

public class EscapeSequence {
    EditText editText;
    Context context;
    private ArrayList<RowListItem> items;
    private int max;

    EscapeSequence(Context context, ArrayList<RowListItem> items, int max){
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
        if(editText.getSelectionStart() - max >= 0){
            editText.setSelection(editText.getSelectionStart() - max);
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
        if (editText.getSelectionStart() + max < editText.length()){
            editText.setSelection(editText.getSelectionStart() + max);
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
        if(editText.getSelectionStart() - max * n >= 0){
            editText.setSelection(editText.getSelectionStart() - max * n);
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
        if (editText.getSelectionStart() + max * n < editText.length()){
            editText.setSelection(editText.getSelectionStart() + max * n);
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveRowUp(int n){
        if(editText.getSelectionStart() - max * n > 0){
            editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max - max * n);
        }
    }
    public void moveRowDown(int n){
        if (editText.getSelectionStart() + max * n < editText.length()){
            editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max + max * n);
        }
    }
    public void moveSelection(int n){
        editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max+ n);
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
        for (int i = items.size()-1; i >= editText.getSelectionStart() % max; i --){
            items.remove(i);
        }

    }

    public void clearDisplay(int n){
        if(n == 0){

        }
    }

    public void clearRow(int n){

    }

    public void test(){
        editText.setText("testtt");
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

    //start行からrow行までの文字数を返す
    private int getLength(int start, int end){
        int length = 0;
        for(int i = start; i < end; i++){
            length += items.get(i).getText().length();
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
}
