package com.i14yokoro.mldpterminal

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

object HtmlParser {
    fun toHtml(text: Spannable): String {
        val ssBuilder = SpannableStringBuilder(text)
        var start: Int
        var end: Int

        val styleSpans = ssBuilder.getSpans<StyleSpan>(0, text.length, StyleSpan::class.java)
        for (i in styleSpans.indices.reversed()) {
            val span = styleSpans[i]
            start = ssBuilder.getSpanStart(span)
            end = ssBuilder.getSpanEnd(span)
            ssBuilder.removeSpan(span)
            if (span.style == Typeface.BOLD) {
                ssBuilder.insert(start, "<b>")
                ssBuilder.insert(end + 3, "</b>")
            } else if (span.style == Typeface.ITALIC) {
                ssBuilder.insert(start, "<i>")
                ssBuilder.insert(end + 3, "</i>")
            }
        }

        val underSpans = ssBuilder.getSpans<UnderlineSpan>(0, ssBuilder.length, UnderlineSpan::class.java)
        for (i in underSpans.indices.reversed()) {
            val span = underSpans[i]
            start = ssBuilder.getSpanStart(span)
            end = ssBuilder.getSpanEnd(span)
            ssBuilder.removeSpan(span)
            ssBuilder.insert(start, "<u>")
            ssBuilder.insert(end + 3, "</u>")
        }
        replace(ssBuilder, '\n', "<br/>")

        return ssBuilder.toString()
    }

    private fun replace(b: SpannableStringBuilder, oldChar: Char, newStr: String) {
        for (i in b.length - 1 downTo 0) {
            if (b[i] == oldChar) {
                b.replace(i, i + 1, newStr)
            }
        }
    }
}