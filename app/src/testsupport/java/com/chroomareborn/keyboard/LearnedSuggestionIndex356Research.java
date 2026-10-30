package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Synthetic parity test for Candidate 354's suggestion ranking.
 * No user words, editor I/O, network, or APK. Timing is diagnostic only.
 */
public final class LearnedSuggestionIndex356Research {
    private static int checks;
    private static volatile int sink;
    private static void eq(String context,Object expected,Object actual) {
        checks++;
        if(!expected.equals(actual))
            throw new AssertionError(context+" expected="+expected+" actual="+actual);
    }
    private static void yes(String context,boolean result) {
        checks++;
        if(!result)throw new AssertionError(context);
    }
    private static List<String> originalNext(Map<String,Integer> pairs,
            String preceding,int limit) {
        if(limit<=0)return List.of();
        String key=TypedSequenceArchive341.canonical(preceding);
        if(key==null)return List.of();
        String prefix=key+" ";
        List<Map.Entry<String,Integer>> matches=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:pairs.entrySet())
            if(entry.getValue()>=TypedSequenceArchive341.MIN_EVIDENCE &&
                    entry.getKey().startsWith(prefix))
                matches.add(entry);
        matches.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                e->-e.getValue()).thenComparing(Map.Entry::getKey));
        List<String> out=new ArrayList<>();
        for(Map.Entry<String,Integer> candidate:matches) {
            if(out.size()>=Math.min(5,limit))break;
            out.add(candidate.getKey().substring(prefix.length()));
        }
        return out;
    }
    private static List<String> originalCompleted(Map<String,Integer> words,
            String prefix,int limit) {
        if(limit<=0 || prefix==null || prefix.length()<2 ||
                prefix.length()>24 || !prefix.matches("[A-Za-z]{2,24}"))
            return List.of();
        String needle=prefix.toLowerCase(Locale.ROOT);
        List<Map.Entry<String,Integer>> matches=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:words.entrySet())
            if(entry.getValue()>=TypedSequenceArchive341.MIN_EVIDENCE &&
                    entry.getKey().startsWith(needle) &&
                    !entry.getKey().equals(needle))
                matches.add(entry);
        matches.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                e->-e.getValue()).thenComparing(Map.Entry::getKey));
        List<String> out=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:matches) {
            if(out.size()>=Math.min(3,limit))break;
            out.add(entry.getKey());
        }
        return out;
    }
    private static String word(int n) {
        return "w"+(char)('a'+(n/26)%26)+(char)('a'+n%26);
    }
    public static void main(String[] args) {
        Random random=new Random(35620261030L);
        Map<String,Integer> words=new TreeMap<>(), pairs=new TreeMap<>();
        for(int i=0;i<TypedSequenceArchive341.MAX_WORDS;i++)
            words.put(word(i),1+random.nextInt(30));
        while(pairs.size()<TypedSequenceArchive341.MAX_PAIRS) {
            String left=word(random.nextInt(words.size()));
            String right=word(random.nextInt(words.size()));
            String pair=TypedSequenceArchive341.pairKey(left,right);
            if(pair==null)continue;
            int max=Math.min(words.get(left),words.get(right));
            pairs.put(pair,1+random.nextInt(max));
        }
        TypedSequenceArchive341.verifyRawEvidence(words,pairs);
        LearnedSuggestionIndex356 index=
                LearnedSuggestionIndex356.fromVerifiedEvidence(words,pairs);
        yes("Index respects word bound",index.indexedWords()<=192);
        yes("Index respects context bound",index.indexedContexts()<=256);
        for(int i=0;i<words.size();i++) {
            String word=word(i);
            for(int length=2;length<=word.length();length++) {
                String prefix=word.substring(0,length);
                for(int limit=0;limit<=6;limit++) {
                    eq("prefix "+prefix+"/"+limit,
                            originalCompleted(words,prefix,limit),
                            index.completedWords(prefix,limit));
                    String caps=prefix.toUpperCase(Locale.ROOT);
                    eq("caps "+prefix+"/"+limit,
                            originalCompleted(words,caps,limit),
                            index.completedWords(caps,limit));
                }
            }
            for(int limit=0;limit<=7;limit++)
                eq("pair "+word+"/"+limit,
                        originalNext(pairs,word,limit),
                        index.nextWords(word,limit));
        }
        for(String invalid:List.of("","w","hello@","xx xx","abc123",
                "name/secret","\nnext","\u00a0okay")) {
            for(int limit=0;limit<=7;limit++) {
                eq("invalid prefix",originalCompleted(words,invalid,limit),
                        index.completedWords(invalid,limit));
                eq("invalid context",originalNext(pairs,invalid,limit),
                        index.nextWords(invalid,limit));
            }
        }
        // Randomized parity against the original algorithms.
        for(int n=0;n<30_000;n++) {
            int idx=random.nextInt(words.size());
            String word=word(idx);
            String prefix=word.substring(0,2+random.nextInt(word.length()-1));
            int limit=random.nextInt(8);
            eq("random prefix "+n,originalCompleted(words,prefix,limit),
                    index.completedWords(prefix,limit));
            eq("random next "+n,originalNext(pairs,word,limit),
                    index.nextWords(word,limit));
        }
        // Index is a snapshot, so changing source evidence never changes it.
        List<String> unchanged=index.completedWords("wa",3);
        words.put("waq",1000);
        eq("immutable old snapshot",unchanged,index.completedWords("wa",3));

        // Corrupt/orphaned counts must be rejected rather than published.
        boolean rejected=false;
        try {
            LearnedSuggestionIndex356.fromVerifiedEvidence(
                    Map.of("hello",3),Map.of("hello missing",3));
        } catch(IllegalArgumentException expected) { rejected=true; }
        yes("orphaned pair rejected",rejected);

        long start=System.nanoTime();
        for(int n=0;n<20_000;n++)
            sink+=originalCompleted(words,"wa",3).size();
        long oldNanos=System.nanoTime()-start;
        start=System.nanoTime();
        for(int n=0;n<20_000;n++)
            sink+=index.completedWords("wa",3).size();
        long newNanos=System.nanoTime()-start;
        System.out.println("PASS Research 356: "+checks+
                " parity/privacy assertions; synthetic diagnostic ms old="+
                (oldNanos/1_000_000)+" indexed="+(newNanos/1_000_000));
        System.out.println("Timing is NOT phone speed, user accuracy, or an APK result.");
    }
}
