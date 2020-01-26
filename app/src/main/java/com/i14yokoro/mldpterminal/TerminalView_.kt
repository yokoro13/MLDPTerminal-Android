package com.i14yokoro.mldpterminal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class TerminalView_ : View, View.OnClickListener{

    private var listener: InputListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)


    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return BaseInputConnection(this, false)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dispatchFirst = super.dispatchKeyEvent(event)

        if (event.action == KeyEvent.ACTION_UP){
            val charCode = event.unicodeChar
            // 入力時の処理
            Log.d("MyEditText", "onKey keyCode=$charCode")
            Log.d("MyEditText", "onKey input=${charCode.toChar()}")
            // 入力を通知
            listener?.onKey(charCode.toChar())
        }
        return dispatchFirst
    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onClick(p0: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    fun setInputListener(listener: InputListener){
        this.listener = listener
    }
}