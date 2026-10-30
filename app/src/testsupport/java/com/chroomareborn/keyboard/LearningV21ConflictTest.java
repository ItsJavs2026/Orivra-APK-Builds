package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Deterministic, synthetic pure-Java Learning V2.1 quality and safety tests. */
public final class LearningV21ConflictTest {
    private static int assertions;
    private static void yes(boolean ok,String why){
        assertions++;
        if(!ok)throw new AssertionError(why);
    }
    private static Map<String,Integer> pairs(Object...values) {
        Map<String,Integer> m=new LinkedHashMap<>();
        for(int i=0;i<values.length;i+=2)
            m.put((String)values[i],(Integer)values[i+1]);
        return m;
    }
    public static void main(String[] args) {
        yes(LearningV2Policy.preferred("cant",pairs("can't",1))==null,
                "one explicit acceptance cannot force correction");
        yes("can't".equals(LearningV2Policy.preferred("cant",pairs("can't",2))),
                "two acceptances reinforce the actual contraction");
        yes("ain't".equals(LearningV2Policy.preferred("aint",pairs("ain’t",3))),
                "curly apostrophe is canonically equivalent");
        yes("Can't".equals(LearningV2Policy.preferred("Cant",pairs("can't",2))),
                "capitalize initial letter");
        yes("CAN'T".equals(LearningV2Policy.preferred("CANT",pairs("can't",2))),
                "preserve uppercase intent");
        yes("I'm".equals(LearningV2Policy.preferred("im",pairs("i'm",3))),
                "first-person contraction should capitalize I");
        yes("I'M".equals(LearningV2Policy.preferred("IM",pairs("i'm",3))),
                "all-caps first person remains all caps");
        yes("we're".equals(LearningV2Policy.preferred("weree",pairs("we’re",2))),
                "learn accepted apostrophized alternatives");
        yes(LearningV2Policy.preferred("cant",pairs("can't",2,"can",2))==null,
                "equally reinforced competing corrections cannot win arbitrarily");
        yes("can't".equals(LearningV2Policy.preferred("cant",
                pairs("can't",3,"can",2))),"distinct stronger choice may win");
        yes("can".equals(LearningV2Policy.preferred("cant",
                pairs("can't",1,"can",4))),"dominant explicit history can change");
        yes(LearningV2Policy.preferred("cant",pairs("cant",100))==null,
                "same canonical word must never learn as a correction");
        yes(LearningV2Policy.preferred("can’t",pairs("can't",100))==null,
                "curly versus straight apostrophe is not a lexical correction");
        yes(LearningV2Policy.preferred("cant",pairs("can't",0))==null,
                "zero count never promoted");
        yes(LearningV2Policy.preferred("cant",pairs("can't",-5))==null,
                "negative count rejected");
        yes(LearningV2Policy.preferred("cant",pairs("can't",1001))==null,
                "invalid excessive count rejected");
        yes(LearningV2Policy.preferred("cant",pairs("can't",2,"a@b.com",900)) != null,
                "email-like learned output must be ignored");
        yes(LearningV2Policy.preferred("cant",pairs("can't",2,"here there",999)) != null,
                "phrases may never be imported as a preferred learned word");
        yes(LearningV2Policy.preferred("cant",pairs("can't",2,"can't",2))
                .equals("can't"),"duplicate equivalent outcomes do not harm ranking");
        yes(LearningV2Policy.preferred(null,pairs("can't",3))==null,
                "null text has no learning");
        yes(LearningV2Policy.preferred("cant",null)==null,
                "no history has no learning");
        yes(LearningV2Policy.preferred("cant",Collections.emptyMap())==null,
                "empty history has no learning");
        yes(LearningV2Policy.preferred("a@b.com",pairs("can't",30))==null,
                "email is not a learning target");
        yes(LearningV2Policy.preferred("hello world",pairs("can't",30))==null,
                "sentence is not an implicit learning target");
        yes(LearningV2Policy.preferred("1234",pairs("can't",30))==null,
                "number is not a learning target");
        yes("you're".equals(LearningV2Policy.preferred("youre",pairs("you're",2))),
                "second-person contraction remains supported");

        // A fixed randomized insertion-order stress: ties MUST never choose
        // different corrections based on map iteration order.
        Random rng=new Random(306_2026L);
        for(int i=0;i<120;i++){
            List<String> candidates=new java.util.ArrayList<>(
                    Arrays.asList("can't","can","cane"));
            Collections.shuffle(candidates,rng);
            Map<String,Integer> tied=new LinkedHashMap<>();
            for(String x:candidates)tied.put(x,2);
            yes(LearningV2Policy.preferred("cant",tied)==null,
                    "tie must remain undecided, shuffled case "+i);
            Map<String,Integer> dominant=new LinkedHashMap<>();
            for(String x:candidates)dominant.put(x,x.equals("can't")?5:3);
            yes("can't".equals(LearningV2Policy.preferred("cant",dominant)),
                    "clear winner independent of preference iteration order "+i);
        }
        System.out.println("PASS: LearningV21ConflictTest "+assertions+" assertions");
    }
}
