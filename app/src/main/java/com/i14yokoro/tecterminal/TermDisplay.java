package com.i14yokoro.tecterminal;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;

public class TermDisplay {
    private Context context;
    private EditText editText;
    private int width;
    private int height;
    private ArrayList<RowItem> items;

    public TermDisplay(Context context, ArrayList<RowItem> items, int width, int height) {
        this.context = context;
        this.editText = (EditText)((MainActivity)context).findViewById(R.id.main_display);
        this.items = items;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void changeDisplay(int topRow){
        editText.setText("");
        for (int i = topRow; i < items.size() && i < topRow+height-1; i++){
            Log.d("debug***", items.get(i).getText());
            editText.append(items.get(i).getText());
        }
    }

}
