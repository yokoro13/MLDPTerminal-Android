package com.i14yokoro.tecterminal;

import android.text.method.ReplacementTransformationMethod;

public class WordBreakTransformationMethod extends ReplacementTransformationMethod {
    private static WordBreakTransformationMethod instance;

    private WordBreakTransformationMethod() {}

    public static WordBreakTransformationMethod getInstance()
    {
        if (instance == null)
        {
            instance = new WordBreakTransformationMethod();
        }

        return instance;
    }

    private static char[] dash = new char[] {'-', '\u2011'}; //non-breaking dash
    private static char[] space = new char[] {' ', '\u00A0'}; //non-breaking space

    private static char[] original = new char[] {dash[0], space[0]}; //変換前
    private static char[] replacement = new char[] {dash[1], space[1]}; //変換後

    @Override
    protected char[] getOriginal()
    {
        return original;
    }

    @Override
    protected char[] getReplacement()
    {
        return replacement;
    }
}
