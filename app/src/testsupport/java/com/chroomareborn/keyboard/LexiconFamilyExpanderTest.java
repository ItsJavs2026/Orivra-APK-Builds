package com.chroomareborn.keyboard;

import java.util.HashMap;
import java.util.Map;

/** Standalone policy tests; no Android, network, personal data or model tuning. */
public final class LexiconFamilyExpanderTest {
    private static int checks;
    private static void expect(boolean condition, String message) {
        checks++;
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Map<String,Integer> words=new HashMap<>();
        String[] family={
            "enjoy","enjoyed","enjoying",
            "move","moved","moving",
            "try","tried","trying",
            "stop","stopped","stopping",
            "play","jump"
        };
        for(String word:family)words.put(word,24);
        words.put("movement",99);
        LexiconFamilyExpander.enrich(words);
        for(String expected:new String[]{"enjoys","moves","tries","stops"})
            expect(words.containsKey(expected),"missing validated form "+expected);
        expect(!words.containsKey("plays"),"must not invent unsupported form");
        expect(!words.containsKey("jumps"),"must not invent unsupported form");
        expect(words.get("movement")==99,"never lower existing word frequency");
        expect(words.containsKey("workshop"),"verified broad compound lexicon missing");
        expect(words.containsKey("thoughtful"),"verified adjective lexicon missing");
        expect(words.containsKey("batteries"),"verified ordinary noun form missing");
        expect(!words.containsKey("colorfuls"),"unvalidated pseudo-inflection");
        int size=words.size();
        LexiconFamilyExpander.enrich(words);
        expect(words.size()==size,"enrichment should be idempotent");
        LexiconFamilyExpander.enrich(null);
        System.out.println("PASS: LexiconFamilyExpanderTest "+checks+" assertions");
    }
}
