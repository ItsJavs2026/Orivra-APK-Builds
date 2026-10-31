package com.chroomareborn.keyboard;

import java.util.List;

/**
 * Research 388: allow row-key prediction gestures at a verified next-word
 * boundary, in addition to active composition. No gesture or editor actions
 * inside this pure policy.
 */
final class LetterRowIdleMode388 {
    private LetterRowIdleMode388(){}
    enum Mode { UNAVAILABLE, COMPOSING, NEXT_WORD }

    static Mode start(String snapshot,String composing,
            boolean currentIdleTicket,List<String> visible) {
        if(snapshot==null || composing==null ||
                !snapshot.equals(composing) ||
                !LetterRowPredictionSwipe386.hasSelectable(visible))
            return Mode.UNAVAILABLE;
        if(!snapshot.isEmpty())return Mode.COMPOSING;
        return currentIdleTicket?Mode.NEXT_WORD:Mode.UNAVAILABLE;
    }

    static boolean finish(boolean gestureStartedIdle,String expected,
            String composing,String liveCaretWord,boolean currentIdleTicket) {
        if(expected==null || composing==null || liveCaretWord==null)
            return false;
        if(gestureStartedIdle)
            return expected.isEmpty() && composing.isEmpty() &&
                    liveCaretWord.isEmpty() && currentIdleTicket;
        return !expected.isEmpty() && expected.equals(composing) &&
                expected.equals(liveCaretWord);
    }
}
