package com.chroomareborn.keyboard;

import java.util.Random;

/** Only synthetic text; no personal editor text, analytics or network. */
public final class PredictionTapCaret364Research {
    private static int checks;
    private static void ok(boolean expected,boolean actual,String name) {
        checks++;
        if(expected!=actual)
            throw new AssertionError(name+" expected="+expected+" actual="+actual);
    }
    private static void eq(String expected,String actual,String name) {
        checks++;
        if(!expected.equals(actual))
            throw new AssertionError(name+" expected="+expected+" actual="+actual);
    }
    private static PredictionTapCaret364.Ticket arm(
            String before,String after,String accepted) {
        return PredictionTapCaret364.arm(before,after,accepted,
                true,false,false,false,false);
    }
    private static boolean prefix(PredictionTapCaret364.Ticket ticket,
            String before,String after,String key) {
        return PredictionTapCaret364.prefixFirstLetter(
                ticket,before,after,key,
                true,true,true,false,false,false,false,true);
    }
    public static void main(String[] args) {
        // User's real-world example:
        // I am| hungry -> tap "am" -> type "not".
        PredictionTapCaret364.Ticket between=arm("I am"," hungry","am");
        ok(true,between!=null,"Only explicit tap arms boundary");
        ok(true,prefix(between,"I am"," hungry","n"),
                "Prefix first letter with a single separating space");
        String left="I am",right=" hungry";
        left+=" n"; // one commitText(" n",1) after the tapped chip
        left+="o";left+="t"; // ordinary typing stays unchanged
        eq("I am not hungry",left+right,
                "Final text has one separator on each side");
        ok(false,prefix(between,"I am"," hungry"," "),
                "Space key does not cause an automatic insertion");
        ok(false,prefix(between,"I am"," hungry","!"),
                "Punctuation key does not trigger prefix");
        ok(false,prefix(between,"I am"," hungry","23"),
                "Multiple chars are not treated as one key");
        ok(false,prefix(between,"I am"," happy","n"),
                "Host text changed: old ticket rejected");
        ok(false,prefix(between,"I ","am hungry","n"),
                "Cursor moved: old ticket rejected");
        ok(false,prefix(between,"I is"," hungry","n"),
                "Earlier word changed: old ticket rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                false,true,true,false,false,false,false,true),
                "Changed input connection rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,false,true,false,false,false,false,true),
                "Changed input session rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,true,true,false,false,false,true),
                "Selection rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,true,false,true,false,false,true),
                "Private field rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,true,false,false,true,false,true),
                "Numeric field rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,true,false,false,false,true,true),
                "Symbols rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,true,false,false,false,false,false),
                "Suggestions-disabled editor rejected");
        ok(false,PredictionTapCaret364.prefixFirstLetter(
                between,"I am"," hungry","n",
                true,true,false,false,false,false,false,true),
                "Not between words rejected");
        ok(true,arm("I am"," hungry","am")!=null,
                "Untouched prior real word is supported");
        ok(false,arm("I am","","am")!=null,
                "End of document already gets its own trailing space");
        ok(false,arm("I am","  hungry","am")!=null,
                "Existing double space preserved without extra edits");
        ok(false,arm("I am","\nhungry","am")!=null,
                "Newline is a structural boundary");
        ok(false,arm("I am",", hungry","am")!=null,
                "Closing punctuation must stay attached");
        ok(false,arm("I am","! hungry","am")!=null,
                "Sentence closing punctuation is preserved");
        ok(false,arm("I am","\u00a0hungry","am")!=null,
                "NBSP not converted or doubled");
        ok(false,arm("I am","\thungry","am")!=null,
                "Tab and indentation not modified");
        ok(false,arm("Iam"," hungry","am")!=null,
                "Must match full word boundary");
        ok(false,PredictionTapCaret364.arm("I am"," hungry","am",
                true,true,false,false,false)!=null,
                "No pending insertion for selected text");
        ok(false,PredictionTapCaret364.arm("I am"," hungry","am",
                true,false,true,false,false)!=null,
                "No pending insertion for private editor");
        ok(false,PredictionTapCaret364.arm("I am"," hungry","am",
                true,false,false,true,false)!=null,
                "No pending insertion for numeric editor");
        ok(false,PredictionTapCaret364.arm("I am"," hungry","am",
                false,false,false,false,false)!=null,
                "No pending insertion when suggestions disabled");
        // Test different existing words without any hardcoded prose phrase.
        for(String pair:new String[]{"we can","she has","they are","it works",
                "I am","you might","my car","that is"}) {
            int split=pair.lastIndexOf(' ');
            String selected=pair.substring(split+1);
            String before=pair;
            PredictionTapCaret364.Ticket t=arm(before," something",selected);
            ok(true,t!=null,"General word boundary "+pair);
            ok(true,prefix(t,before," something","n"),
                    "New word after "+pair);
            eq(pair+" not something",before+" not"+" something",
                    "Two-sided spacing "+pair);
        }
        Random random=new Random(36420261030L);
        for(int i=0;i<40_000;i++) {
            String suffix=" next"+i;
            PredictionTapCaret364.Ticket t=arm("hello this",suffix,"this");
            ok(true,t!=null,"Stress arm "+i);
            ok(true,prefix(t,"hello this",suffix,"a"),
                    "Stress matching first letter "+i);
            ok(false,prefix(t,"hello this",suffix+"!","a"),
                    "Stress stale suffix "+i);
        }
        System.out.println("PASS Research 364: "+checks+
                " one-shot editor-insertion spacing assertions");
    }
}
