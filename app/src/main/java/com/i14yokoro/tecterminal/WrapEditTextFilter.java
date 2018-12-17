package com.i14yokoro.tecterminal;

import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.widget.EditText;

public class WrapEditTextFilter implements InputFilter {
    private final EditText editText;
    public WrapEditTextFilter(EditText editText){
        this.editText = editText;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        TextPaint textPaint = editText.getPaint();
        int w = editText.getWidth();
        int wpl = editText.getCompoundPaddingLeft();
        int wpr = editText.getCompoundPaddingRight();
        int width = w - wpl - wpr;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int index = start; index < end; index++){
            if(Layout.getDesiredWidth(source, start, index + 1, textPaint) > width){
                builder.append(source.subSequence(start, index));
                builder.append("\n");
                start = index;
            }
            else{
                if(source.charAt(index) == '\n'){
                    builder.append(source.subSequence(start, index));
                    start = index;
                }
            }
        }
        if(start < end){
            builder.append(source.subSequence(start, end));
        }

        return builder;
    }
}
