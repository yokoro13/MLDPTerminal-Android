package com.i14yokoro.tecterminal;

import android.content.Context;
import android.widget.EditText;

public class EscapeSequence {
    private EditText editText;
    private Context context;
    private int max;

    EscapeSequence(Context context, int max){
        this.context = context;
        this.editText = (EditText) ((MainActivity)context).findViewById(R.id.main_display);
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
        if(editText.getSelectionStart() - (max+1) >= 0){
            editText.setSelection(editText.getSelectionStart() - (max+1));
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
        if (editText.getSelectionStart() + (max+1) < editText.length()){
            editText.setSelection(editText.getSelectionStart() + (max+1));
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
        if(editText.getSelectionStart() - (max+1) * n - max + 1>= 0){
            editText.setSelection(editText.getSelectionStart() - (max+1) * n);
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
        if (editText.getSelectionStart() + (max+1) * n < editText.length()){
            editText.setSelection(editText.getSelectionStart() + max * (max+1));
        }
        else {
            editText.setSelection(editText.length());
        }
    }

    public void moveRowUp(int n){
        if(editText.getSelectionStart() - max * n > 0){
            editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max - (max+1) * n);
        }
    }
    public void moveRowDown(int n){
        if (editText.getSelectionStart() + max * n < editText.length()){
            editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max + (max+1) * n);
        }
    }
    public void moveSelection(int n){
        editText.setSelection(editText.getSelectionStart() - editText.getSelectionStart() % max + n);
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

    public void clearRow(int n){

    }
}
