package com.chroomareborn.keyboard;

import java.util.Collections;
import java.util.List;

/** English candidate policy for hosts that commit characters immediately.
 * Shows options only; no editing, logging, learning or network. A live editor
 * snapshot and explicit tap are required for the actual replacement.
 */
final class CommittedEnglishCursorPolicy {
    private CommittedEnglishCursorPolicy() {}

    static boolean eligible(String word,boolean composing,boolean editing,
            boolean selection,boolean textAfter,boolean sensitive,
            boolean suggestionsEnabled) {
        return !composing && !editing && !selection && !textAfter &&
                !sensitive && suggestionsEnabled && word!=null &&
                word.length()>=2 && word.length()<=24 &&
                EnglishApostrophePolicy.prefix(word)!=null;
    }

    static List<String> immediate(String word) {
        if(word==null)return Collections.emptyList();
        return CursorWordCorrectionPolicy.safeOptions(word,
                EnglishApostrophePolicy.suggestions(word));
    }

    static List<String> merge(String word,List<String> fromWorker) {
        if(word==null)return Collections.emptyList();
        return CursorWordCorrectionPolicy.safeOptions(word,
                EnglishApostrophePolicy.merge(word,fromWorker));
    }
}
