package com.i14yokoro.tecterminal;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
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
        //012|3456 (n = 2, x = 3)-> 01234|56 (n = 2, x = 5) -> 0123456| (x = 7)
        if(termDisplay.getCursorX() + n <= termDisplay.getRowLength(getSelectRowIndex())) {
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
        //FIXME 追加できない，カーソル合わない，バグる
        //たまにうまくいく
        if(termDisplay.getCursorY() - n < 0){ //画面外にでる
            termDisplay.setCursorY(0);
        } else {
            moveCursorY(-n);
            Log.d("termDisplay***","moveUp to y: " + Integer.toString(termDisplay.getCursorY()));
        }
        if(termDisplay.getRowLength(getSelectRowIndex()) < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - termDisplay.getRowLength(getSelectRowIndex());
            Log.d("termDisplay***","sub curX " + Integer.toString(add));
            addBlank(add);
        }

    }
    public void moveDown(int n){
        /**
         * よころのあたまのなか
         * からのやつつけるーしたいくーないーいっこしたでがまんーつけるーおわり
         * だうんのとこでひとつつける必要ありそ？
         */
        //FIXME ２つ以上したにいくとカーソルが移動してくれないfk
        //たぶんgetOffsetForPosition関係　（もしかしたらバグかも）
        if(termDisplay.getCursorY() + n >= termDisplay.getDisplaySize()) { //移動先が一番下の行を超える場合
            if(termDisplay.getCursorY() + n >= termDisplay.getDisplayColumnSize()){ //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
            int move = termDisplay.getCursorY() - termDisplay.getDisplaySize();//2,1のばあい
            //termDisplay.addTextItem(termDisplay.getTotalColumns()-1,LF, 0);
            for (int i = 0; i <= move; i++){
                Log.d("termDisplay**", "add empty" + Integer.toString(i));
                termDisplay.addEmptyRow();
            }
            //
            // termDisplay.setTextItem(" ", 0);
        }else { //移動先が一番下の行を超えない
            if (termDisplay.getCursorY() + n > termDisplay.getDisplaySize()) { //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
        }
        //移動先の文字数がcursorXより小さい
        if(termDisplay.getRowLength(getSelectRowIndex()) < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - termDisplay.getRowLength(getSelectRowIndex());
            addBlank(add);
        }
    }

    private void addBlank(int n){
        //Log.d("termDisplay**", "add Blank"+);
        int x = termDisplay.getRowLength(getSelectRowIndex())-1;
        if(x < 0){
            x = 0;
        }
        for (int i = 0; i <= n; i++){
            Log.d("termDisplay**", "add Blank"+Integer.toString(i));
            termDisplay.insertTextItem(x, getSelectRowIndex(),"p", termDisplay.getDefaultColor());
            //termDisplay.addTextItem(termDisplay.getTopRow()+termDisplay.getCursorY(), "d", 0);
        }
    }

    public void moveUpToRowLead(int n){
        termDisplay.setCursorX(0);
        moveUp(n);
    }

    public void moveDownToRowLead(int n){
        //FIXME downがなおらないとうまくいかない
        termDisplay.setCursorX(0);
        moveDown(n);
    }

    public void moveSelection(int n){
        termDisplay.setCursorX(0);
        moveRight(n);
    }

    public void moveSelection(int n, int m){ //n,mは1~
        //FIXME downがなおらないとうまくいかない
        if(n < 1){
            n = 1;
        }
        if(m < 1){
            m = 1;
        }
        termDisplay.setCursorY(0);
        termDisplay.setCursorX(0);
        moveDown(n-1);
        moveRight(m-1);
    }

    public void clearDisplay(){
        int x = termDisplay.getCursorX();
        for(int y = termDisplay.getCursorY(); y < termDisplay.getDisplaySize(); y++){
            for (; x < termDisplay.getRowLength(y); x++){
                termDisplay.deleteTextItem(x, y);
            }
            x = 0;
        }
    }

    public void clearDisplay(int n){
        //FIXME きえかたがおかしい
        //一番下までスクロールして消去ぽい
        if(n == 0){ //カーソルより後ろにある画面上の文字を消す
            clearDisplay();
        }

        //FIXME 入力中の行がきえない
        if(n == 1){ //カーソルより前にある画面上の文字を消す
            for (int y = 0; y <= termDisplay.getCursorY(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeText(x, y, " ");
                    if(y == termDisplay.getCursorY()){
                        if(x == termDisplay.getCursorX()){
                            break;
                        }
                    }
                }
            }
        }

        //FIXME 入力中の行がきえない
        if(n == 2){ //全消去
            for (int y = 0; y < termDisplay.getDisplaySize(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeTextItem(x, y, " ", termDisplay.getDefaultColor());
                }
            }
        }

    }

    public void clearRow(){
        int del = termDisplay.getRowLength(termDisplay.getCursorY()) - termDisplay.getCursorX();
        for (int x = 0; x < del; x++){
            termDisplay.deleteTextItem(termDisplay.getCursorX(), termDisplay.getCursorY());
        }
    }

    public void clearRow(int n){
        if(n == 0){ //カーソル以降にある文字を消す
            clearRow();
        }

        if(n == 1){ //カーソル以前にある文字を消す
            for (int x = 0; x <= termDisplay.getCursorX(); x++){
                termDisplay.changeText(x, termDisplay.getCursorY(), " ");
                //termDisplay.deleteTextItem(x, termDisplay.getCursorY());
            }
        }

        if(n == 2){ //全消去
            int del = termDisplay.getRowLength(termDisplay.getCursorY());
            for (int x = 0; x < del; x++){
                termDisplay.changeText(x, termDisplay.getCursorY(), " ");
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

    public void selectGraphicRendition(int n){ //色
        /**色付け手順
         * １．エスケープシーケンスを受信
         * ２．標準を色を指定の色に変える setDefaultColor(color)
         * ３．これが黒以外なら    textItem = new TextItem("<font color=#" + getcolor + ">" + str + "</font>");
         * ２．\1b[0mを受信すると黒にリセット
         */
        //output = "<font color=#000000>TextView</font>" ;
        String output;
        switch (n){
            case 0:
                termDisplay.setDefaultColor("000000");
                break;
            case 30:
                termDisplay.setDefaultColor("000000");
                break;
            case 31:
                termDisplay.setDefaultColor("FF0000");
                break;
            case 32:
                termDisplay.setDefaultColor("008000");
                break;
            case 33:
                termDisplay.setDefaultColor("FFFF00");
                break;
            case 34:
                termDisplay.setDefaultColor("0000FF");
                break;
            case 35:
                termDisplay.setDefaultColor("FF00FF");
                break;
            case 36:
                termDisplay.setDefaultColor("00FFFF");
                break;
            case 37:
                termDisplay.setDefaultColor("FFFFFF");
                break;
            case 39:
                termDisplay.setDefaultColor("000000");
                break;
            default:
                //termDisplay.setDefaultColor("000000");
        }
    }

    public void changeDisplay(){
        String output = "";
        String result = "";
        SpannableString spannable;

        //Log.d(TAG, "topRow/ " + topRow);
        editText.setText("");
        termDisplay.createDisplay();
        TimingLogger logger = new TimingLogger("TAG_TEST", "change display");
        int totalColumns = termDisplay.getTotalColumns();
        int displayColumnsSize = termDisplay.getDisplaySize();
        int displayRowSize = termDisplay.getDisplayRowSize();

        for (int y = 0; y < totalColumns && y < displayColumnsSize; y++){
            for (int x = 0; x < displayRowSize; x++){
                Log.d("termDisplay", "y "+Integer.toString(y));
                if(!termDisplay.getDisplay(x, y).equals("EOL")) {
                    //できれば，文字列を作ってからsetTextでいいかも
                    if (termDisplay.getDisplay(x, y).equals("")){
                        Log.d("termDisplay**", "empty");
                        if (y < termDisplay.getDisplaySize()-1){
                            output = output + LF;
                        }
                        break;
                    }
                    output = output + "<font color=#" + termDisplay.getColor(x, getTop() + y) +
                            ">" + termDisplay.getDisplay(x, y) + "</font>";
                } else{
                    Log.d("termDisplay**", "here is EOL");
                    spannable = new SpannableString(output);
                    result = HtmlParser.toHtml(spannable);
                    editText.setText(Html.fromHtml(result));
                    logger.dumpToLog();
                    return;
                }
                if(x == displayRowSize-1){
                    Log.d("termDisplay**", "max size");
                    output = output + LF;
                    //editText.append(LF);
                }
                if(termDisplay.getDisplay(x, y).equals(LF)){
                    Log.d("termDisplay**", "this is LF");
                    break;
                }
            }
        }
        //editText.setText(Html.fromHtml(output));
        spannable = new SpannableString(output);
        result = HtmlParser.toHtml(spannable);
        editText.setText(Html.fromHtml(result));
        logger.dumpToLog();
    }


    private int getSelectRowIndex() {
        return termDisplay.getCursorY() + termDisplay.getTopRow();
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
