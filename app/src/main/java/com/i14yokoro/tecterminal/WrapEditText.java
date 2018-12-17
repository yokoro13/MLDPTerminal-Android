package com.i14yokoro.tecterminal;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.util.AttributeSet;

public class WrapEditText extends android.support.v7.widget.AppCompatEditText{
    private CharSequence charSequence = "";
    private BufferType bufferType = BufferType.EDITABLE;

    public WrapEditText(Context context){
        super(context);
        setFilters(new InputFilter[]{new WrapEditTextFilter(this)});
    }

    public WrapEditText(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setFilters(new InputFilter[]{new WrapEditTextFilter(this)});
    }

    public WrapEditText(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
        setFilters(new InputFilter[]{new WrapEditTextFilter(this)});
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){
        setText(charSequence, bufferType);
    }

    @Override
    public void setText(CharSequence text, BufferType type){
        charSequence = text;
        super.setText(text, bufferType);
    }

    @Override
    public Editable getText(){
        return (Editable) super.getText();
    }

    @Override
    public int length(){
        return charSequence.length();
    }

}
