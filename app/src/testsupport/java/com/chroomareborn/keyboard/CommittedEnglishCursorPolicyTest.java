package com.chroomareborn.keyboard;

import java.util.List;

/** Device-free English correction regression for no-composition editors. */
public final class CommittedEnglishCursorPolicyTest {
    private static int checks=0;
    private static void yes(boolean b,String label) {
        checks++; if(!b)throw new AssertionError(label);
    }
    private static boolean eligible(String word) {
        return CommittedEnglishCursorPolicy.eligible(
                word,false,false,false,false,false,true);
    }
    public static void main(String[] args) {
        for(String word:new String[]{"helo","keyboadr","were","hows",
                "cant","im","can't","we’re","hello"}) {
            yes(eligible(word),"valid English word eligible");
            yes(CommittedEnglishCursorPolicy.immediate(word).get(0).equals(word),
                    "exact input remains first and unchanged");
        }
        for(String word:new String[]{"", "a", "I", "5word","john@example.com",
                "hello world","hello.com","abc_def","hey!","ab🙂",
                "x".repeat(25),null}) {
            yes(!eligible(word),"reject nonwords, PII or too-short tokens");
        }
        yes(!CommittedEnglishCursorPolicy.eligible("helo",true,false,false,
                false,false,true),"composition owns its own prediction path");
        yes(!CommittedEnglishCursorPolicy.eligible("helo",false,true,false,
                false,false,true),"midword caret uses existing path");
        yes(!CommittedEnglishCursorPolicy.eligible("helo",false,false,true,
                false,false,true),"selection never replaced");
        yes(!CommittedEnglishCursorPolicy.eligible("helo",false,false,false,
                true,false,true),"text after cursor uses existing path");
        yes(!CommittedEnglishCursorPolicy.eligible("helo",false,false,false,
                false,true,true),"private editor must not predict");
        yes(!CommittedEnglishCursorPolicy.eligible("helo",false,false,false,
                false,false,false),"NO_SUGGESTIONS respected");
        List<String> candidates=CommittedEnglishCursorPolicy.merge(
                "helo",List.of("helo","hello","he lo","hell"));
        yes(candidates.get(0).equals("helo"),"typed text always first");
        yes(candidates.contains("hello"),"ordinary spelling correction reachable");
        yes(!candidates.contains("he lo"),"single word correction cannot insert phrase");
        yes(CommittedEnglishCursorPolicy.merge("were",List.of("were")).contains("we're"),
                "valid were retains optional we're");
        yes(CommittedEnglishCursorPolicy.merge("cant",List.of("cant")).contains("can't"),
                "missing apostrophe remains tap-only");
        yes(CommittedEnglishCursorPolicy.merge("hows",List.of("hows")).contains("how's"),
                "Google-style commit-each-key supports contractions");
        yes("I'm".equals(CursorWordCorrectionPolicy.matchCase("im","I'm")),
                "English first-person I cannot become lower-case");
        yes("I've".equals(CursorWordCorrectionPolicy.matchCase("ive","I've")),
                "first-person I've remains capitalized");
        yes("I'M".equals(CursorWordCorrectionPolicy.matchCase("IM","I'm")),
                "all-uppercase stays uppercase");
        yes("we're".equals(CursorWordCorrectionPolicy.matchCase("were","We're")),
                "ordinary lowercase contractions stay lowercase");
        yes("Correct".equals(CursorWordCorrectionPolicy.matchCase("Corect","correct")),
                "other capitalized corrections unchanged");
        yes("correct".equals(CursorWordCorrectionPolicy.matchCase("corect","Correct")),
                "lowercase words remain lowercase");
        System.out.println("PASS: CommittedEnglishCursorPolicyTest "+checks+" checks");
    }
}
