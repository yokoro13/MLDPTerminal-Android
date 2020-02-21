package com.i14yokoro.mldpterminal.terminalview

import java.util.*

interface InputListener: EventListener {
    fun onKey(text: Char)
}