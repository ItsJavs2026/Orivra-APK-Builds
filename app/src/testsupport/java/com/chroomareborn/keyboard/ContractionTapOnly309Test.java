package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Locale;

/** Synthetic, no-editor/no-logs regression for tap-only ambiguous contractions. */
public final class ContractionTapOnly309Test {
    private static int checks;
    private static void ok(boolean value,String description) {
        checks++;
        if(!value)throw new AssertionError(description);
    }
    public static void main(String[] args) {
        for(String[] row:new String[][] {
                {"were","were","we're"},{"Were","Were","We're"},
                {"WERE","WERE","WE'RE"},{"hows","hows","how's"},
                {"Hows","Hows","How's"},{"HOWS","HOWS","HOW'S"},
                {"cant","cant","can't"},{"ain’t","ain’t","ain’t"},
                {"dont","dont","don't"},{"youre","youre","you're"}
        }) {
            String typed=row[0],literal=row[1],optional=row[2];
            List<String> extras=EnglishApostrophePolicy.suggestions(typed);
            List<String> merged=EnglishApostrophePolicy.merge(typed,List.of(literal));
            ok(merged.get(0).equals(literal),
                    "literal always preserved as first candidate "+typed);
            ok(merged.contains(optional),"tap alternative exists "+typed);
            if(!typed.contains("’"))
                ok(EnglishApostrophePolicy.hasDistinctSuggestion(typed),
                    "missing punctuation is distinct "+typed);
        }
        for(String original:new String[]{
                "its","well","shell","ill","id","wereabouts","howsoever",
                "now","anything","hello","keyboard","have","yes","we"
        }) {
            ok(!EnglishApostrophePolicy.hasDistinctSuggestion(original),
                    "no speculative contraction for legitimate word "+original);
        }
        for(String unsafe:new String[]{
                "hello there. hows","hi@example.com","password123","http://hows",
                "a.b","we're on","john's@example.com","hi🙂","5'10","hows?"
        }) {
            ok(EnglishApostrophePolicy.suggestions(unsafe).isEmpty(),
                    "don't ingest full sentence/URI/PII as a contraction");
            ok(!EnglishApostrophePolicy.hasDistinctSuggestion(unsafe),
                    "unsafe token has no fast lane");
        }
        for(String input:new String[]{"were","Were","WERE","hows","Hows","HOWS"}) {
            List<String> candidates=CursorWordCorrectionPolicy.safeOptions(input,
                    EnglishApostrophePolicy.suggestions(input));
            ok(candidates.size()>=2,"cursor editor gets both literal + optional "+input);
            ok(candidates.get(0).equals(input),"cursor word remains primary "+input);
            ok(CursorWordCorrectionPolicy.plan("Hello there. "+input,"",
                    input,candidates.get(1),false,false)!=null,
                    "verified explicit tap may replace exact live token "+input);
            ok(CursorWordCorrectionPolicy.plan("Hello there. "+input,"",
                    input,candidates.get(1),false,true)==null,
                    "private input cannot apply optional contraction "+input);
            ok(CursorWordCorrectionPolicy.plan("Hello there. "+input+"!",
                    "",input,candidates.get(1),false,false)==null,
                    "never delete across ending punctuation "+input);
            ok(CursorWordCorrectionPolicy.plan("Hello there. "+input+"x","",
                    input,candidates.get(1),false,false)==null,
                    "stale original cannot be replaced "+input);
        }
        for(String word:new String[]{"were","hows"}) {
            var ticket=CursorEditSnapshotGuard.capture(word,
                    "Hello there. "+word,"",17,19,19,false,false,false);
            ok(ticket!=null,"trailing uncommitted contraction gets caret ticket");
            ok(CursorEditSnapshotGuard.current(ticket,
                    "Hello there. "+word,"",17,19,19,false,false,false),
                    "exact current editor snapshot still accepted");
            ok(!CursorEditSnapshotGuard.current(ticket,
                    "Hello there. "+word+"x","",17,19,19,false,false,false),
                    "stale snapshot rejected");
        }
        System.out.println("PASS: ContractionTapOnly309Test "+checks+
                " deterministic assertions");
    }
}
