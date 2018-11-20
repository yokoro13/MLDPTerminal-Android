package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;

public class EscapeSequence {
    EditText editText;
    Context context;
    private ArrayList<RowListItem> items;

    EscapeSequence(Context context, ArrayList<RowListItem> items){
        this.context = context;
        this.editText = (EditText) ((MainActivity)context).findViewById(R.id.main_display);
        this.items = items;
    }

    public void moveRight(int n){
        if(editText.length() >= editText.getSelectionStart()+n){
            editText.setSelection(editText.getSelectionStart()+n);
        }
        else {
            editText.setSelection(editText.length());
        }
    }
    public void moveLeft(int n){
        if(editText.getSelectionStart()-n >= 0) {
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
        if(editText.getSelectionStart()-43 >= 0){
            editText.setSelection(editText.getSelectionStart()-43);
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
        if (editText.getSelectionStart()+43 < editText.length()){
            editText.setSelection(editText.getSelectionStart()+43);
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveRowUp(int n){

    }
    public void moveRowDown(int n){

    }
    public void moveSelection(int n){

    }
    public void moveSelection(int n, int m){

    }
    public void clearDisplay(int n){

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
