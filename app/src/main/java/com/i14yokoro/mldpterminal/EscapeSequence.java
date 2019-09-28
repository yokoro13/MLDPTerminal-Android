package com.i14yokoro.mldpterminal;

/**
 * ANSIのエスケープシーケンスと同じ動作をする
 */
public class EscapeSequence {
    private TermDisplay termDisplay;

    /**
     * @param termDisplay 表示画面の情報
     */
    EscapeSequence(TermDisplay termDisplay){
        this.termDisplay = termDisplay;
    }

    /**
     * @param n 移動する量
     */
    public void moveRight(int n){
        if (n >= termDisplay.getDisplayRowSize()) {
            n = termDisplay.getDisplayRowSize() - 1;
        }

        if(termDisplay.getCursorX() + n < termDisplay.getRowLength(getSelectRowIndex())) {
            moveCursorX(n);
        } else {
            int move = n;
            int add;
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

    /**
     * @param n 移動する量
     */
    public void moveLeft(int n){
        if (n >= termDisplay.getDisplayRowSize()) {
            n = termDisplay.getDisplayRowSize() - 1;
        }

        if(termDisplay.getCursorX() - n >= 0){
            moveCursorX(-n);
        }
    }

    /**
     * @param n 移動する量
     */
    public void moveUp(int n){
        if (n >= termDisplay.getDisplayColumnSize()) {
            n = termDisplay.getDisplayColumnSize() - 1;
        }

        if(termDisplay.getCursorY() - n < 0){ //画面外にでる
            termDisplay.setCursorY(0);
        } else {
            moveCursorY(-n);
        }
        int rowLength = termDisplay.getRowLength(getSelectRowIndex());

        if(rowLength < termDisplay.getCursorX()){
            int add = termDisplay.getCursorX() - rowLength + 1;
            addBlank(add);
        }
    }

    /**
     * @param n 移動する量
     */
    public void moveDown(int n){
        if (n >= termDisplay.getDisplayColumnSize()) {
            n = termDisplay.getDisplayColumnSize() - 1;
        }

        if(termDisplay.getCursorY() + n >= termDisplay.getDisplaySize()) { //移動先が一番下の行を超える場合
            if(termDisplay.getCursorY() + n >= termDisplay.getDisplayColumnSize()){ //ディスプレイサイズを超える場合
                termDisplay.setCursorY(termDisplay.getDisplayColumnSize() - 1);
            } else {
                moveCursorY(n);
            }
            int move = termDisplay.getCursorY() - termDisplay.getDisplaySize();
            for (int i = 0; i <= move; i++){
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

    /**
     * @param n 追加する空白の量
     */
    private void addBlank(int n){
        int x = termDisplay.getRowLength(getSelectRowIndex())-1; //現在の行の最後のindexを取得
        if(x < 0){
            x = 0;
        }
        for (int i = 0; i < n; i++){
            termDisplay.insertTextItem(x, getSelectRowIndex(),' ', termDisplay.getDefaultColor());
            x = termDisplay.getRowLength(getSelectRowIndex())-1;
        }
    }

    /**
     * @param n 移動する量
     */
    public void moveUpToRowLead(int n){
        termDisplay.setCursorX(0);
        moveUp(n);
    }

    /**
     * @param n 移動する量
     */
    public void moveDownToRowLead(int n){
        termDisplay.setCursorX(0);
        moveDown(n);
    }

    /**
     * @param n 移動する量
     */
    public void moveSelection(int n){
        termDisplay.setCursorX(0);
        moveRight(n-1);
    }

    /**
     * @param n 縦に移動する量(0 < n)
     * @param m 横に移動する量(0 < m)
     */
    public void moveSelection(int n, int m){ //n,mは1~
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

    /**
     * 画面消去
     */
    public void clearDisplay(){
        int x = termDisplay.getCursorX();
        int i = x;
        int length;
        int displaySize = termDisplay.getDisplaySize();
        for(int y = termDisplay.getCursorY(); y < displaySize; y++){
            length = termDisplay.getRowLength(y + termDisplay.getTopRow());
            for (; i < length; i++){
                if(termDisplay.getRowText(y + termDisplay.getTopRow()).length() > x) {
                    termDisplay.deleteTextItem(x, y + termDisplay.getTopRow());
                }
            }
            x = 0;
            i = 0;
        }
    }

    /**
     * @param n 画面消去の方法
     */
    public void clearDisplay(int n){
        //一番下までスクロールして消去ぽい
        if(n == 0){ //カーソルより後ろにある画面上の文字を消す
            clearDisplay();
        }

        if(n == 1){ //カーソルより前にある画面上の文字を消す
            for (int y = termDisplay.getTopRow(); y <= getSelectRowIndex(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeText(x, y, '\u0000');
                    if (y == getSelectRowIndex() && x == termDisplay.getCursorX()) {
                        break;
                    }
                }
            }
        }

        if(n == 2){ //全消去
            for (int y = termDisplay.getTopRow(); y < termDisplay.getDisplaySize() + termDisplay.getTopRow(); y++){
                for (int x = 0; x < termDisplay.getRowLength(y); x++){
                    termDisplay.changeTextItem(x, y, '\u0000', termDisplay.getDefaultColor());
                }
            }
        }

    }

    /**
     * @param n 行削除の方法
     */
    public void clearRow(int n){
        if(n == 0){ //カーソル以降にある文字を消す
            int del = termDisplay.getRowLength(getSelectRowIndex()) - termDisplay.getCursorX();
            for (int x = 0; x < del; x++){
                termDisplay.deleteTextItem(termDisplay.getCursorX(), getSelectRowIndex());
            }
        }

        if(n == 1){ //カーソル以前にある文字を消す
            for (int x = 0; x <= termDisplay.getCursorX(); x++){
                termDisplay.changeText(x, getSelectRowIndex(), ' ');
            }
        }

        if(n == 2){ //全消去
            int del = termDisplay.getRowLength(getSelectRowIndex());
            for (int x = 0; x < del; x++){
                termDisplay.changeText(x, getSelectRowIndex(), ' ');
            }

        }
    }

    /**
     * @param n 移動する量
     */
    public void scrollNext(int n){
        if (termDisplay.getTopRow() + n > termDisplay.getTotalColumns()) return; //一番したに空白追加？？
        termDisplay.setTopRow(termDisplay.getTopRow()+n);
    }

    /**
     * @param n 移動する量
     */
    public void scrollBack(int n){
        //一番上に空白追加で一番した削除？？？(あくまで画面上でスクロールしていると見せかけている?)
        if(termDisplay.getTopRow() - n < 0) return;
        termDisplay.setTopRow(termDisplay.getTopRow()-n);
    }

    /**
     * @param n 変化させる色
     */
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

    /**
     * @param x カーソルをx移動させる
     */
    private void moveCursorX(int x){
        termDisplay.setCursorX(termDisplay.getCursorX() + x);
    }

    /**
     * @param y カーソルをy移動させる
     */
    private void moveCursorY(int y){
        termDisplay.setCursorY(termDisplay.getCursorY() + y);
    }

}
