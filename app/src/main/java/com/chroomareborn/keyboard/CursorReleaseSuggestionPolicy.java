package com.chroomareborn.keyboard;

/**
 * Chooses suggestions ONLY after a completed spacebar cursor swipe.
 * Never edits text; the editor must still validate the live word when the
 * user explicitly taps a proposed correction.
 */
final class CursorReleaseSuggestionPolicy {
    enum Mode { CORRECT_WORD, NEXT_WORD, CLEAR }

    static final class Decision {
        final Mode mode;
        final String word;
        private Decision(Mode mode,String word) {
            this.mode=mode;
            this.word=word;
        }
    }

    private CursorReleaseSuggestionPolicy() {}

    /**
     * Treat a letter directly to either side of the caret as the editable
     * word. If the caret is at a whitespace boundary at document end,
     * next-word suggestions are appropriate. A gap inside existing text
     * should not generate a next-word insertion into the middle.
     *
     * before/after are bounded editor snapshots, not stored or logged.
     */
    static Decision classify(CharSequence before,CharSequence after,
                             boolean selected,boolean sensitive) {
        if(sensitive || selected || before==null || after==null)
            return new Decision(Mode.CLEAR,"");
        int left=before.length(),right=0;
        while(left>0 && Character.isLetter(before.charAt(left-1)))left--;
        while(right<after.length() && Character.isLetter(after.charAt(right)))right++;
        if(left<before.length() || right>0) {
            String word=before.subSequence(left,before.length()).toString()
                    +after.subSequence(0,right).toString();
            if(word.length()>0 && word.length()<=64)
                return new Decision(Mode.CORRECT_WORD,word);
            return new Decision(Mode.CLEAR,"");
        }
        return new Decision(after.length()==0?Mode.NEXT_WORD:Mode.CLEAR,"");
    }
}
