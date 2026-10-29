package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** General-purpose transition ranking safety checks, no Android dependency. */
public final class SentenceFlowEvidenceRankerTest {
    private static int checks;
    private static void ok(boolean success,String problem) {
        checks++;
        if(!success)throw new AssertionError(problem);
    }
    private static Map<String,List<String>> transitions(String... pairs) {
        Map<String,List<String>> out=new HashMap<>();
        for(String pair:pairs) {
            String[] words=pair.split(" ");
            out.computeIfAbsent(words[0], k->new ArrayList<>()).add(words[1]);
        }
        return out;
    }
    public static void main(String[] args) {
        Map<String,List<String>> context=transitions(
                "the weather","weather is","is nice",
                "how are","are you","you doing","doing today",
                "we should","should finish","finish this");
        Map<String,List<String>> empty=new HashMap<>();
        String raw="theweatherusnice";
        List<String> ranked=Arrays.asList(
                "the weather us nice","the weather is nice","the with us nice");
        List<String> result=SentenceFlowEvidenceRanker.rerank(
                raw,ranked,empty,context);
        ok("the weather is nice".equals(result.get(0)),
                "Strong, closer, coherent candidate was not promoted: "+result);
        ok(result.size()==ranked.size(),"No candidate can be dropped");
        ok(result.containsAll(ranked),"No candidate can be fabricated");
        ok(SentenceFlowEvidenceRanker.rerank(raw,ranked,empty,empty)
                ==ranked,"No offline evidence must preserve original list");
        ok(SentenceFlowEvidenceRanker.rerank("hello",ranked,empty,context)
                ==ranked,"Short words bypass sentence re-ranking");
        ok(SentenceFlowEvidenceRanker.rerank(raw,
                Arrays.asList("the weather is nice","the weather us nice"),
                empty,context).get(0).equals("the weather is nice"),
                "Already coherent prediction must remain first");
        ok(SentenceFlowEvidenceRanker.rerank(raw,null,empty,context)==null,
                "Null input is fail closed");

        Random random=new Random(29434035L);
        for(int i=0;i<30000;i++) {
            String phrase=i%2==0?"how are you doing today":
                    "we should finish this";
            String wrong=i%2==0?"how area you doing today":
                    "we should fin is this";
            List<String> list=Arrays.asList(wrong,phrase,"the weather is nice");
            String typed=(i%2==0?"howareyoudoingtoday":"weshouldfinishthis");
            List<String> replay=SentenceFlowEvidenceRanker.rerank(
                    typed,list,empty,random.nextInt(5)==0?empty:context);
            ok(replay!=null && replay.size()==list.size(),
                    "candidate count changed");
            ok(replay.containsAll(list),"candidate fabricated or removed");
            ok(list.equals(Arrays.asList(wrong,phrase,
                    "the weather is nice")),"caller list mutated");
        }
        System.out.println("SentenceFlowEvidenceRankerTest passed "+
                checks+" assertions on 30k deterministic candidate scenarios");
    }
}
