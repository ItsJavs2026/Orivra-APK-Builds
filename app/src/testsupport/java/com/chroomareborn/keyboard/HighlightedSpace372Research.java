package com.chroomareborn.keyboard;

import java.util.Random;

/** Synthetic word-level Space autocorrect eligibility, no editor access. */
public final class HighlightedSpace372Research {
    private static int n;
    private static boolean eligible(String raw,String typed,String option,
            boolean finalFrame,boolean highlight) {
        return HighlightedSpace372.eligible(raw,typed,option,
                finalFrame,highlight,true,true,true,
                false,false,false,false,false,false,false);
    }
    private static void check(boolean actual,boolean expected,String why) {
        n++;
        if(actual!=expected)throw new AssertionError(why+
                " expected="+expected+" actual="+actual);
    }
    public static void main(String[] args) {
        check(eligible("tekk","tekk","tell",true,true),true,
                "Screenshot-style highlighted correction");
        check(eligible("Iwikk","Iwikk","I will",true,true),true,
                "Joined missing space + double neighboring key");
        check(eligible("hellp","hellp","hello",true,true),true,
                "Single misspelled letter");
        check(eligible("tekk","tekk","tekk",true,true),false,
                "Unchanged original cannot auto correct");
        check(eligible("tekk","tekk","tell",false,true),false,
                "Preliminary prediction not settled");
        check(eligible("tekk","tekk","tell",true,false),false,
                "No visible highlighted correction");
        check(eligible("tekk","tell","tell",true,true),false,
                "Stale word cannot be corrected");
        check(eligible("cat","cat","dog",true,true),false,
                "Unrelated words never auto replace");
        check(eligible("te","te","telephone",true,true),false,
                "Long speculative expansion blocked");
        check(eligible("john","john","john",true,true),false,
                "Known name remains original");
        check(eligible("let's","let's","lets",true,true),true,
                "Contraction short correction may be allowed");
        check(eligible("I am","I am","I'm",true,true),false,
                "Space handles only active ONE word");
        check(eligible("tekk","tekk","tell",true,true),true,
                "Good editor normal mode");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,true,false,false,false,false,false,false),
                false,"Private field");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,false,true,true,false,false,false,false,false,false,false),
                false,"Old session");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,false,true,false,false,false,false,false,false,false),
                false,"Switched app");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,true,false,false,false,false,false),
                false,"Mid-cursor");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,true,false,false,false,false),
                false,"Selection");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,true,false,false,false),
                false,"Right-side word present");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,false,true,false,false),
                false,"Numeric editor");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,false,false,true,false),
                false,"Symbols mode");
        check(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,false,false,false,true),
                false,"Split next-word not correction");
        Random random=new Random(37293023L);
        for(int i=0;i<30_000;i++) {
            String word="a"+(char)('a'+random.nextInt(26))+
                    (char)('a'+random.nextInt(26))+"b";
            String correction=word.substring(0,word.length()-1)+"c";
            boolean permit=eligible(word,word,correction,true,true);
            check(permit,true,"One character correction "+i);
            check(eligible(word,word,correction,false,true),false,
                    "Unsettled suggestion "+i);
        }
        System.out.println("PASS Research 372 Space: "+n+
                " confidence/privacy/visible-highlight eligibility checks");
    }
}
