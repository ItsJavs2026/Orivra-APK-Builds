package com.chroomareborn.keyboard;

/** Collects an observation ONLY at a successfully committed space boundary.
 * Never inspect/copy full surrounding sentences or editor-private content.
 */
final class CommittedWordUsagePolicy321 {
    private CommittedWordUsagePolicy321() {}
    static final class Observation {
        final String prior,word;
        Observation(String p,String w){prior=p;word=w;}
    }
    static Observation capture(CharSequence before,String composed,
            boolean learningOn,boolean editorPermits,boolean sensitive,
            boolean editing,boolean selected,boolean textAfterCaret,
            boolean isSymbols) {
        if(!learningOn || !editorPermits || sensitive || editing ||
                selected || textAfterCaret || isSymbols || composed==null)
            return null;
        String word=AcceptedContextPolicy319.acceptedWord(composed);
        if(word==null)return null;
        String previous=AcceptedContextPolicy319.precedingForReplacement(
                before,composed);
        if(previous==null)return null;
        return new Observation(previous,word);
    }
}
