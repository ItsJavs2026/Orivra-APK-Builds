package com.chroomareborn.keyboard;

/**
 * Cursor recovery gate for typing into the end of an existing *committed*
 * editor word. The input is the 1-character editor sample already taken by
 * the punctuation rule; no extra InputConnection call or storage is needed.
 *
 * Without this gate, moving the cursor out of a composing word and returning
 * to its end can start a fresh composing span for the next letter only:
 * "instakke" + "d" becomes a prediction for "d", not "instakked".
 *
 * This class is pure: no editor operations, logging or automatic correction.
 */
final class CommittedWordBoundary360 {
    private CommittedWordBoundary360() {}

    static boolean resumeWholeWordCorrection(CharSequence leftOneChar,
            String activeComposition,String incoming,
            boolean selected,boolean followingText,boolean sensitive,
            boolean suggestionsAllowed,boolean symbols,boolean numeric) {
        if(sensitive || !suggestionsAllowed || symbols || numeric ||
                selected || followingText ||
                activeComposition==null || !activeComposition.isEmpty() ||
                incoming==null || incoming.length()!=1)
            return false;
        char letter=incoming.charAt(0);
        if(!((letter>='a' && letter<='z') ||
                (letter>='A' && letter<='Z')))
            return false;
        // An existing letter directly before the caret proves we are
        // extending a committed word, not starting a fresh composing word.
        // Do not infer a boundary from a space, mark, number or emoji.
        if(leftOneChar==null || leftOneChar.length()!=1)return false;
        return Character.isLetter(leftOneChar.charAt(0));
    }
}
