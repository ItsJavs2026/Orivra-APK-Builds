package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.List;

public final class CursorContextCorrectionRankerTest {
    private static int checks;
    private static void expect(boolean condition,String description) {
        checks++;
        if(!condition)throw new AssertionError(description);
    }

    public static void main(String[] args) {
        // Independent test fixture for the ranking policy, not a language model
        // trained from Orivra's quality or real-user examples.
        CursorContextCorrectionRanker.PairEvidence transitions=(a,b)->
                (a.equals("the") && b.equals("keyboard")) ||
                (a.equals("keyboard") && b.equals("feels")) ||
                (a.equals("the") && b.equals("keypad")) ||
                (a.equals("phone") && b.equals("works"));

        List<String> options=Arrays.asList("keyboadr","keypad","keyboard","keynote");
        List<String> ranked=CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard feels smooth",transitions);
        expect(ranked.equals(Arrays.asList("keyboadr","keyboard","keypad","keynote")),
                "two-sided context promotes a supported correction only");
        expect(options.equals(Arrays.asList("keyboadr","keypad","keyboard","keynote")),
                "source candidates not mutated");
        expect(ranked.get(0).equals("keyboadr"),
                "original word retains first display slot");

        List<String> unknown=CursorContextCorrectionRanker.rerank("keyboadr",options,
                "we saw a keyb","oard yesterday",transitions);
        expect(unknown==options,"no supporting neighbors does not rerank");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard! feels smooth",transitions)==options,
                "punctuation prevents crossing a sentence boundary");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard, feels smooth",transitions)==options,
                "comma-separated neighbors are not treated as adjacent");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard",transitions)==options,
                "no following word yields no promotion");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "keyb","oard feels",transitions)==options,
                "no previous word yields no promotion");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard feels",null)==options,
                "no evidence provider: no change");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard feels",
                (a,b)->a.equals("the") && b.equals("keyboard"))==options,
                "one-sided evidence is insufficient to reorder alternatives");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",
                Arrays.asList("keyboadr","keyboard"),
                "the keyb","oard feels",transitions).size()==2,
                "two-chip result unchanged");
        expect(CursorContextCorrectionRanker.rerank("keyboadr",options,
                "the keyb","oard\nfeels",transitions)==options,
                "newline inside neighbor gap does not create extra words");
        // Both sides may be genuine words, but a fabricated combined phrase
        // must not be promoted as a single-word correction.
        List<String> mixed=Arrays.asList("keyboadr","keyboard feels","keypad","keyboard");
        List<String> clean=CursorContextCorrectionRanker.rerank("keyboadr",mixed,
                "the keyb","oard feels",transitions);
        expect(clean.get(1).equals("keyboard") &&
                clean.get(2).equals("keyboard feels"),
                "multiword suggestion does not receive context boost");
        // Deterministic property batch: unseen contexts must be an exact
        // identity transform, and no displayed alternative may be added,
        // lost, or automatically committed by a display-only reranker.
        java.util.Random random=new java.util.Random(270271);
        for(int iteration=0;iteration<4000;iteration++) {
            StringBuilder left=new StringBuilder(),right=new StringBuilder();
            for(int i=0;i<5;i++) {
                left.append((char)('a'+random.nextInt(26)));
                right.append((char)('a'+random.nextInt(26)));
            }
            String before=left+" keyb",after="oard "+right;
            List<String> candidate=CursorContextCorrectionRanker.rerank(
                    "keyboadr",options,before,after,transitions);
            expect(candidate==options,
                    "unseen pseudo-random contexts preserve source ranking");
        }
        System.out.println("CURSOR_CONTEXT_RANKER checks="+checks+" passed");
    }
}
