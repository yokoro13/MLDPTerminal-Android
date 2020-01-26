package com.i14yokoro.mldpterminal

import java.util.*

interface InputListener: EventListener {
    fun onKey(text: Char)
}