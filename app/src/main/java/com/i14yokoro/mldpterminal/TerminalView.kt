package com.i14yokoro.mldpterminal

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText

class TerminalView : EditText {
    private var listener: InputListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

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

    fun setListener(listener: InputListener){
        this.listener = listener
    }
}