package com.chroomareborn.keyboard;

/**
 * Gboard-reference-inspired placement, independently implemented for Orivra.
 * The user supplied ABC and primary ?123 reference screenshots.
 * Second symbol page is a usable Orivra fallback until screenshot provided.
 * Pure data, never changes typed content, accessibility or prediction logic.
 */
final class KeyboardLayoutSpec {
    static final int ABC=0, SYMBOLS=1, EXTRA=2;
    static final String[] LETTERS={"qwertyuiop","asdfghjkl","zxcvbnm"};
    static final String[] PRIMARY_SYMBOLS={"1234567890","@#$_&-+()/","*\"':;!?"};
    static final String[] EXTRA_SYMBOLS={"~`|•√π÷×¶∆","€£¥^°={}\\","%©®™✓[]"};
    static final String[] LETTER_HINTS={"1234567890","@#$_&-+()","*\"':;!?"};
    private KeyboardLayoutSpec(){}

    static String[] rows(int mode) {
        if(mode==SYMBOLS)return PRIMARY_SYMBOLS.clone();
        if(mode==EXTRA)return EXTRA_SYMBOLS.clone();
        return LETTERS.clone();
    }
    static String longPressHint(int row,int col) {
        if(row<0||row>=LETTERS.length||col<0 ||
             col>=LETTERS[row].length())return null;
        return String.valueOf(LETTER_HINTS[row].charAt(col));
    }
    static String bottomModeLabel(int mode) {return mode==ABC?"?123":"ABC";}
    static String thirdRowPrefix(int mode) {
        return mode==SYMBOLS?"=\\<":mode==EXTRA?"?123":null;
    }
    static int toggleMainMode(int mode) {return mode==ABC?SYMBOLS:ABC;}
    static int toggleSymbolPage(int mode) {
        return mode==SYMBOLS?EXTRA:mode==EXTRA?SYMBOLS:ABC;
    }
    static boolean containsPrimarySymbol(char symbol) {
        for(String line:PRIMARY_SYMBOLS)if(line.indexOf(symbol)>=0)return true;
        return false;
    }
}
