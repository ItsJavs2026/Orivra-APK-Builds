package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Map;

public final class AdaptivePrefix328Test {
    private static int checks;
    private static void ok(boolean value,String note) {
        checks++;if(!value)throw new AssertionError(note);
    }
    public static void main(String[] args) {
        Map<String,Integer> words=Map.of(
            "continue",7,"contact",3,"content",2,
            "my",9,"me",6,"can",9,"candy",8,"can't",6);
        ok(AdaptivePrefixPolicy328.candidates("con",words,3).get(0)
                .equals("continue"),"con -> frequently used continue");
        ok(AdaptivePrefixPolicy328.candidates("cont",words,3).get(0)
                .equals("continue"),"cont -> continue");
        ok(AdaptivePrefixPolicy328.candidates("contin",words,3).get(0)
                .equals("continue"),"contin -> continue");
        ok(AdaptivePrefixPolicy328.candidates("co",words,3).get(0)
                .equals("continue"),"2-letter prefix after 5+ uses");
        ok(AdaptivePrefixPolicy328.candidates("m",words,3)
                .contains("my"),"2-letter word learned from 1 key");
        ok(!AdaptivePrefixPolicy328.candidates("m",words,3)
                .contains("me"),"one-letter prefix requires stronger evidence");
        ok(AdaptivePrefixPolicy328.candidates("c",words,3)
                .contains("can"),"short 3-letter word can be recalled");
        ok(AdaptivePrefixPolicy328.candidates("ca",words,3)
                .contains("can't"),"contractions can complete");
        ok(AdaptivePrefixPolicy328.candidates("CONT",words,3)
                .contains("CONTINUE"),"uppercase input matched");
        ok(AdaptivePrefixPolicy328.candidates("Con",words,3)
                .contains("Continue"),"sentence case preserved");
        ok(AdaptivePrefixPolicy328.candidates("x",words,3).isEmpty(),
                "no unrelated prefix guesses");
        ok(AdaptivePrefixPolicy328.candidates("continue",words,3).isEmpty(),
                "already complete word not autocompleted");
        ok(AdaptivePrefixPolicy328.candidates("con",Map.of("continue",2),3)
                .isEmpty(),"two unconfirmed uses do not promote");
        ok(AdaptivePrefixPolicy328.candidates("co",Map.of("continue",3),3)
                .isEmpty(),"early 2-letter prefixes require stronger learning");
        ok(AdaptivePrefixPolicy328.candidates("m",Map.of("my",6),3)
                .isEmpty(),"one-letter threshold protects UI");
        ok(AdaptivePrefixPolicy328.merge("can",List.of("can","can go"),
                List.of("candy"),true).equals(
                    List.of("can","candy","can go")),
                "exact correct short words never displaced");
        ok(AdaptivePrefixPolicy328.merge("con",List.of("con test"),
                List.of("continue"),false).get(0).equals("continue"),
                "incomplete prefix promotes learned completion");
        ok(AdaptivePrefixPolicy328.merge("con",List.of("continue"),
                List.of("continue"),false).size()==1,
                "case-insensitive dedup prevents flashing");
        ok(AdaptivePrefixPolicy328.candidates("c",Map.of("continue",100),3)
                .isEmpty(),"never flood one-key prediction with long words");
        ok(AdaptivePrefixPolicy328.candidates("1",words,3).isEmpty(),
                "numeric amount text excluded");
        ok(AdaptivePrefixPolicy328.candidates("a b",words,3).isEmpty(),
                "full sentence not a prefix");
        ok(AdaptivePrefixPolicy328.candidates("con",Map.of("continuation",5,
                "continue",8),1).equals(List.of("continue")),
                "explicitly used words rank by evidence");
        System.out.println("PASS AdaptivePrefix328Test "+checks+" checks");
    }
}
