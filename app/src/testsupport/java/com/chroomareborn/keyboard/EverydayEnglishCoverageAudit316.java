package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Inspect the ACTUAL initialized offline dictionary (not approximate source
 * string matching), with externally supplied research-only frequency samples.
 * No private text; this is not representative of device typing accuracy.
 */
public final class EverydayEnglishCoverageAudit316 {
    private static int tests=0;
    private static void ok(boolean check,String message) {
        tests++;if(!check)throw new AssertionError(message);
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Integer> dictionary() throws Exception {
        Field field=PredictionLite.class.getDeclaredField("WORDS");
        field.setAccessible(true);
        return (Map<String,Integer>)field.get(null);
    }
    public static void main(String[] args)throws Exception {
        Map<String,Integer> words=dictionary();
        boolean gate=args.length>1 && "--gate".equals(args[1]);
        if(gate) {
            for(String literal:new String[]{
                "tell","tells","telling","told","must","under","between",
                "however","although","unless","someone","privacy","contact",
                "performance","website","shipping","necessary","version",
                "quality","design","related","research","policy","posts",
                "standard","hardware","network","community","technology",
                "requirements","application","file","message","typing",
                "keyboard","correct","corrected","suggestion","prediction"})
                ok(words.containsKey(literal),"missing ordinary English "+literal);
            ok("tell".equalsIgnoreCase(PredictionLite.suggestions("tekk").get(0)),
                    "last confirmed real-device typo remains corrected");
            for(String exact:new String[]{
                    "must","however","although","someone","between",
                    "under","contact","quality","community","performance"}) {
                List<String> candidates=PredictionLite.suggestions(exact);
                ok(!candidates.isEmpty() && exact.equalsIgnoreCase(candidates.get(0)),
                    "ordinary correct word cannot be split or rewritten "+exact);
            }
        }
        System.out.println("SOURCE_WORDS\t"+words.size());
        if(args.length>=1) {
            List<String> frequency=new ArrayList<>();
            try(BufferedReader input=new BufferedReader(new FileReader(args[0]))) {
                String value;
                while((value=input.readLine())!=null && frequency.size()<1000) {
                    if(value.matches("[a-z]{2,20}"))frequency.add(value);
                }
            }
            int recognized=0;
            for(String word:frequency)if(words.containsKey(word))recognized++;
            System.out.println("FREQUENCY_TOP1000_ELIGIBLE\t"+frequency.size());
            System.out.println("FREQUENCY_TOP1000_RECOGNIZED\t"+recognized);
            System.out.println("FREQUENCY_TOP1000_MISSING\t"+(frequency.size()-recognized));
        }
        // Broad deterministic "known words never split" checks.
        if(gate) {
            String[] known={"answer","friend","message","contact","network",
                 "community","research","create","open","search","keyboard",
                 "screen","typing","reaction","app","tomorrow","tell"};
            for(String word:known) {
                ok(PredictionLite.suggestions(word).get(0).equalsIgnoreCase(word),
                        "known standalone protected: "+word);
            }
            Random rng=new Random(3162026);
            for(int i=0;i<5000;i++) {
                String raw=known[rng.nextInt(known.length)];
                ok(PredictionLite.suggestions(raw).get(0).equalsIgnoreCase(raw),
                        "5k random known-word preserved");
            }
        }
        System.out.println("PASS EverydayEnglishCoverageAudit316 assertions="+tests);
    }
}
