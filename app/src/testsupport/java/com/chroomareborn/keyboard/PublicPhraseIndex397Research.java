package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Dependency-free, deterministic research gate; never reads personal data. */
public final class PublicPhraseIndex397Research {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static String legacyCompact(String phrase){
        return phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
    }
    private static List<String> legacyFirst(String[] corpus,String word){
        ArrayList<String> result=new ArrayList<>();
        for(String phrase:corpus){
            String[] parts=phrase.split(" ");
            if(parts.length<2 || parts.length>5)continue;
            if(!parts[0].replaceAll("[^A-Za-z]","").equalsIgnoreCase(word))
                continue;
            result.add(phrase);
        }
        return result;
    }
    public static void main(String[] args) {
        final String[] corpus={
            "how's it going","how are you doing","how is it going",
            "hello there how's it going","hello there how is it going",
            "what's going on","what are you doing","what's up",
            "I'm going home","I'm going to work","I don't know",
            "don't worry about it","let me know","can you help me",
            "see you tomorrow","thank you so much","good morning",
            "good night","this is awesome","that's awesome",
            "how was your day","what do you think","I love this",
            "I'm ready","let's get it","hell yeah","heck yeah",
            "it's working","okay this works",
            "one  two","A B","A B","single","abc 123"," punctuation !!",
            "i'm still testing","X-y z"
        };
        PublicPhraseIndex397 index=new PublicPhraseIndex397(corpus);
        check(index.size()==corpus.length,"phrase count changed");
        for(int i=0;i<corpus.length;i++)
            check(index.compactAt(i).equals(legacyCompact(corpus[i])),
                    "compact mismatch at "+i);
        Random rng=new Random(397_20261031L);
        StringBuilder query=new StringBuilder();
        int cases=0;
        for(int i=0;i<120_000;i++){
            query.setLength(0);
            if(i%3==0){
                String p=corpus[rng.nextInt(corpus.length)];
                String[] parts=p.split(" ");
                if(parts.length>0){
                    query.append(parts[0].replaceAll("[^A-Za-z]","")
                            .toLowerCase(Locale.US));
                }
            }else{
                int length=1+rng.nextInt(12);
                for(int j=0;j<length;j++)
                    query.append((char)('a'+rng.nextInt(26)));
            }
            String key=query.toString();
            check(legacyFirst(corpus,key).equals(index.startingWith(key)),
                    "first-word mismatch at "+i+" ("+key+")");
            cases++;
        }
        check(index.startingWith(null).isEmpty(),"null lookup");
        List<String> kept=index.startingWith("hello");
        try{kept.add("not allowed");throw new AssertionError("list mutable");}
        catch(UnsupportedOperationException expected){}
        long oldStart=System.nanoTime();
        int a=0;
        for(int i=0;i<10_000;i++)a+=legacyFirst(corpus,"hello").size();
        long oldElapsed=System.nanoTime()-oldStart;
        long newStart=System.nanoTime();
        int b=0;
        for(int i=0;i<10_000;i++)b+=index.startingWith("hello").size();
        long newElapsed=System.nanoTime()-newStart;
        check(a==b,"lookup counts");
        System.out.println("Research397 PASS: "+cases+
                " first-word parity cases, "+corpus.length+
                " compact normalizations, immutable public-only index");
        System.out.printf(Locale.US,
                "Research397 same-JVM public-phrase lookup 10000x old=%.2fms new=%.2fms (diagnostic only)%n",
                oldElapsed/1_000_000.0,newElapsed/1_000_000.0);
    }
}
