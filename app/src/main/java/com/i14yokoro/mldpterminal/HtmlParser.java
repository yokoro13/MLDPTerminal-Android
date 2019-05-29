package com.i14yokoro.mldpterminal;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

public class HtmlParser {
    public static String toHtml(Spannable text) {
        final SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        int start, end;

        StyleSpan[] styleSpans = ssBuilder.getSpans(0, text.length(), StyleSpan.class);
        for (int i = styleSpans.length - 1; i >= 0; i--) {
            StyleSpan span = styleSpans[i];
            start = ssBuilder.getSpanStart(span);
            end = ssBuilder.getSpanEnd(span);
            ssBuilder.removeSpan(span);
            if (span.getStyle() == Typeface.BOLD) {
                ssBuilder.insert(start, "<b>");
                ssBuilder.insert(end + 3, "</b>");
            } else if (span.getStyle() == Typeface.ITALIC) {
                ssBuilder.insert(start, "<i>");
                ssBuilder.insert(end + 3, "</i>");
            }
        }

        UnderlineSpan[] underSpans = ssBuilder.getSpans(0, ssBuilder.length(), UnderlineSpan.class);
        for (int i = underSpans.length - 1; i >= 0; i--) {
            UnderlineSpan span = underSpans[i];
            start = ssBuilder.getSpanStart(span);
            end = ssBuilder.getSpanEnd(span);
            ssBuilder.removeSpan(span);
            ssBuilder.insert(start, "<u>");
            ssBuilder.insert(end + 3, "</u>");
        }
        replace(ssBuilder, '\n', "<br/>");

        return ssBuilder.toString();
    }

    private static void replace(SpannableStringBuilder b, char oldChar, String newStr) {
        for (int i = b.length() - 1; i >= 0; i--) {
            if (b.charAt(i) == oldChar) {
                b.replace(i, i + 1, newStr);
            }
        }
    }
}