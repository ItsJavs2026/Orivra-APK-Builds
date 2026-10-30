package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Map;
public final class FrequentWordCompletion328Test {
    private static int checks=0;
    private static void ok(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Map<String,Integer> history=Map.of(
                "continue",3,"contact",1,"correct",2,
                "go",3,"good",5,"hi",6,"can",5,"candy",2);
        ok(FrequentWordCompletionPolicy328.matching("con",history,5)
                .contains("continue"),"learn continue after con");
        ok(FrequentWordCompletionPolicy328.matching("co",history,5)
                .contains("continue"),"learn continue after co");
        ok(FrequentWordCompletionPolicy328.matching("cont",history,5)
                .contains("continue"),"learn continue after cont");
        ok(!FrequentWordCompletionPolicy328.matching("con",history,5)
                .contains("contact"),"one use insufficient");
        ok(FrequentWordCompletionPolicy328.matching("g",history,5)
                .isEmpty(),"single letter blocks long and weak small words");
        ok(FrequentWordCompletionPolicy328.matching("g",Map.of("go",5),5)
                .contains("go"),"short words can learn");
        ok(FrequentWordCompletionPolicy328.matching("h",history,5)
                .contains("hi"),"hi after h when familiar");
        ok(FrequentWordCompletionPolicy328.matching("Co",history,5)
                .contains("Continue"),"sentence capitalization");
        ok(FrequentWordCompletionPolicy328.matching("CON",history,5)
                .contains("CONTINUE"),"caps lock");
        ok(FrequentWordCompletionPolicy328.isCompletion("con","continue"),
                "prefix completion recognized");
        ok(!FrequentWordCompletionPolicy328.isCompletion("tekk","tell"),
                "typo correction not a completion");
        ok(!FrequentWordCompletionPolicy328.isCompletion("continue","continue"),
                "complete word not a completion");
        ok(!FrequentWordCompletionPolicy328.isCompletion("co","contact me"),
                "full phrases not stored");
        List<String> ranked=FrequentWordCompletionPolicy328.merge(
                "con",List.of("concern","con"),List.of("continue"),8);
        ok("continue".equals(ranked.get(0)),"personal frequency outranks general guesses");
        ok(ranked.contains("con"),"literal remains visible");
        List<String> exact=FrequentWordCompletionPolicy328.merge(
                "can",List.of("can","candy"),List.of("candy"),8);
        ok("can".equals(exact.get(0)),"valid original protected");
        ok("candy".equals(exact.get(1)),"learned completion still offered");
        ok(FrequentWordCompletionPolicy328.merge(
                "con",List.of("con"),List.of("continue","continue"),8).size()==2,
                "no duplicates");
        for(int i=0;i<1500;i++) {
            ok(FrequentWordCompletionPolicy328.matching(
                    "con",history,5).contains("continue"),"stable prefix ranking");
        }
        System.out.println("PASS FrequentWordCompletion328Test checks="+checks);
    }
}
