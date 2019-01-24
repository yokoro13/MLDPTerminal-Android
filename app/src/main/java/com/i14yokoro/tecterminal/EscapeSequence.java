package com.i14yokoro.tecterminal;

public class EscapeSequence {
    private TermDisplay termDisplay;

    EscapeSequence(TermDisplay termDisplay){
        this.termDisplay = termDisplay;
    }

    public int getTop(){
        return termDisplay.getTopRow();
    }

    public void setTop(int top){
        termDisplay.setTopRow(top);
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

        if(termDisplay.getCursorY() - n < 0){ //画面外にでる
            termDisplay.setCursorY(0);
        } else {
            moveCursorY(-n);
            //Log.d("termDisplay***","moveUp to y: " + Integer.toString(termDisplay.getCursorY()));
        }
        int rowLength = termDisplay.getRowLength(getSelectRowIndex());

        if(rowLength < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - rowLength + 1;
            //Log.d("termDisplay***","sub curX " + Integer.toString(add));
            addBlank(add);
        }

    }
    public void moveDown(int n){

        if(termDisplay.getCursorY() + n >= termDisplay.getDisplaySize()) { //移動先が一番下の行を超える場合
            if(termDisplay.getCursorY() + n >= termDisplay.getDisplayColumnSize()){ //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
            int move = termDisplay.getCursorY() - termDisplay.getDisplaySize();//2,1のばあい
            for (int i = 0; i <= move; i++){
                //Log.d("termDisplay**", "add empty" + Integer.toString(i));
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
        for (int i = 0; i < n; i++){
            //Log.d("termDisplay**", "add Blank"+Integer.toString(i));
            termDisplay.insertTextItem(x, getSelectRowIndex(),' ', termDisplay.getDefaultColor());
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
        moveRight(n-1);
    }

    public void moveSelection(int n, int m){ //n,mは1~
        if(n < 1){
            n = 1;
        }
        if(m < 1){
            m = 1;
        }
        if(n > termDisplay.getDisplayColumnSize()){
            n = termDisplay.getDisplayColumnSize();
        }
        if (m > termDisplay.getDisplayRowSize()){
            m = termDisplay.getDisplayRowSize();
        }
        termDisplay.setCursorY(0);
        termDisplay.setCursorX(0);
        moveDown(n-1);
        moveRight(m-1);
    }

    public void clearDisplay(){
        int x = termDisplay.getCursorX();
        int i = x;
        int length;
        int displaySize = termDisplay.getDisplaySize();
        for(int y = termDisplay.getCursorY(); y < displaySize; y++){
            length = termDisplay.getRowLength(y + getTop());
            for (; i < length; i++){
                if(termDisplay.getRowText(y + getTop()).length() > x) {
                    termDisplay.deleteTextItem(x, y + getTop());
                }
            }
            x = 0;
            i = 0;
        }
    }

    public void clearDisplay(int n){
        //一番下までスクロールして消去ぽい
        if(n == 0){ //カーソルより後ろにある画面上の文字を消す
            clearDisplay();
        }

        if(n == 1){ //カーソルより前にある画面上の文字を消す
            for (int y = getTop(); y <= getSelectRowIndex(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeText(x, y, '\u0000');
                    if(y == getSelectRowIndex()){
                        if(x == termDisplay.getCursorX()){
                            break;
                        }
                    }
                }
            }
        }

        if(n == 2){ //全消去
            for (int y = getTop(); y < termDisplay.getDisplaySize() + getTop(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeTextItem(x, y, '\u0000', termDisplay.getDefaultColor());
                }
            }
        }

    }

    public void clearRow(){
        int del = termDisplay.getRowLength(getSelectRowIndex()) - termDisplay.getCursorX();
        for (int x = 0; x < del; x++){
            termDisplay.deleteTextItem(termDisplay.getCursorX(), getSelectRowIndex());
        }
    }

    public void clearRow(int n){
        if(n == 0){ //カーソル以降にある文字を消す
            clearRow();
        }

        if(n == 1){ //カーソル以前にある文字を消す
            for (int x = 0; x <= termDisplay.getCursorX(); x++){
                termDisplay.changeText(x, getSelectRowIndex(), ' ');
                //termDisplay.deleteTextItem(x, termDisplay.getCursorY());
            }
        }

        if(n == 2){ //全消去
            int del = termDisplay.getRowLength(getSelectRowIndex());
            for (int x = 0; x < del; x++){
                termDisplay.changeText(x, getSelectRowIndex(), ' ');
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
        termDisplay.setColorChange(true);
        switch (n){
            case 0:
                termDisplay.setDefaultColor(0xFF000000);
                break;
            case 30:
                termDisplay.setDefaultColor(0xFF000000);
                break;
            case 31:
                termDisplay.setDefaultColor(0xFFff0000);
                break;
            case 32:
                termDisplay.setDefaultColor(0xFF008000);
                break;
            case 33:
                termDisplay.setDefaultColor(0xFFFFFF00);
                break;
            case 34:
                termDisplay.setDefaultColor(0xFF0000FF);
                break;
            case 35:
                termDisplay.setDefaultColor(0xFFFF00FF);
                break;
            case 36:
                termDisplay.setDefaultColor(0xFF00FFFF);
                break;
            case 37:
                termDisplay.setDefaultColor(0xFFFFFFFF);
                break;
            case 39:
                termDisplay.setDefaultColor(0xFF000000);
                break;
            default:
                //termDisplay.setDefaultColor(0x000000);
        }
    }

    private int getSelectRowIndex() {
        return termDisplay.getCursorY() + termDisplay.getTopRow();
    }

    private void moveCursorX(int x){
        termDisplay.setCursorX(termDisplay.getCursorX() + x);
    }

    private void moveCursorY(int y){
        termDisplay.setCursorY(termDisplay.getCursorY() + y);
    }

}
