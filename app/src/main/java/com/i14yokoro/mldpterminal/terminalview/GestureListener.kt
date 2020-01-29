package com.i14yokoro.mldpterminal.terminalview

import java.util.*

interface GestureListener: EventListener{
    fun onDown()
    fun onMove()
}