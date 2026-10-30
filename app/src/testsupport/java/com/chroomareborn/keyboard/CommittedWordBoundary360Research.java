package com.chroomareborn.keyboard;

import java.util.List;

/** Research-only synthetic cross-state regression. */
public final class CommittedWordBoundary360Research {
    private static int assertions;
    private static void check(boolean actual,boolean expected,String label){
        assertions++;
        if(actual!=expected)throw new AssertionError(label+
                " expected="+expected+" actual="+actual);
    }
    public static void main(String[] args) {
        // User-reported screenshot: after moving away and back to "instakke",
        // typing "d" should use a one-letter COMMIT, not setComposingText("d").
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,false,true,false,false),true,
                "resumed unknown word at document end");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,false,true,false,false),true,
                "lexicon membership irrelevant");
        check(CommittedWordBoundary360.resumeWholeWordCorrection(" ","","d",
                false,false,false,true,false,false),false,
                "new word after Space may safely compose");
        check(CommittedWordBoundary360.resumeWholeWordCorrection(".","","D",
                false,false,false,true,false,false),false,
                "new sentence after punctuation may safely compose");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","instakke","d",
                false,false,false,true,false,false),false,
                "ongoing composition remains fast");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,true,false,true,false,false),false,
                "mid-document typing stays in editor commit mode");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                true,false,false,true,false,false),false,
                "selected word should replace selection using editor");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,true,true,false,false),false,
                "private field never forces prediction state");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,false,false,false,false),false,
                "no-suggestions editor stays safe");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,false,true,true,false),false,
                "symbol keyboard not intercepted");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                false,false,false,true,false,true),false,
                "numeric keyboard not intercepted");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("1","","d",
                false,false,false,true,false,false),false,
                "numeric last char isn't a composed word");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","9",
                false,false,false,true,false,false),false,
                "number key isn't a word completion");
        check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","'",
                false,false,false,true,false,false),false,
                "apostrophe handled elsewhere");
        check(CommittedWordBoundary360.resumeWholeWordCorrection(null,"","d",
                false,false,false,true,false,false),false,
                "missing editor snapshot remains fail closed");
        // Two words: "instakke mistake" -> move caret inside first word,
        // edit, return after second word, type again, and begin a new word.
        for(int i=0;i<40_000;i++){
            check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                    false,false,false,true,false,false),true,
                    "repeat end-of-word returns "+i);
            check(CommittedWordBoundary360.resumeWholeWordCorrection("e","","d",
                    false,true,false,true,false,false),false,
                    "mid-text caret "+i);
            check(CommittedWordBoundary360.resumeWholeWordCorrection(" ","","n",
                    false,false,false,true,false,false),false,
                    "new word after Space "+i);
        }
        System.out.println("PASS Research 360: "+assertions+
                " synthetic boundary-state assertions (not phone tested)");
    }
}
