package com.i14yokoro.tecterminal;

public class RowItem implements Cloneable{
    private int id;
    private String text;
    private boolean hasNext;
    private boolean writable;
    public RowItem(int id, String text, boolean hasNext, boolean writable){
        this.id = id;
        this.text = text;
        this.hasNext = hasNext;
        this.writable = writable;
    }

    @Override
    public RowItem clone(){
        RowItem rowItem = null;
        try {
            rowItem = (RowItem)super.clone();
        }catch (Exception e){
            e.printStackTrace();
        }
        return rowItem;
    }

    @Override
    public String toString(){
        String str = "";
        str += "id/" + Integer.toString(this.id);
        str += " text/" + this.text;
        if (this.hasNext){
            str += " hasNext/true";
        }
        else str += " hasNext/false";
        if (this.writable){
            str += " writable/true";
        }
        else str += " writable/false";
        return str;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setHasNext(boolean hasNext){
        this.hasNext = hasNext;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public String getText() {
        return this.text;
    }

    public long getId(){
        return this.id;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isWritable() {
        return this.writable;
    }
}
