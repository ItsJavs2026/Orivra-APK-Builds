package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

/** Synthetic, deterministic learned-NONE joined-word regression. */
public final class ShortJoinedContext362Research {
    private static int checks;
    private static void eq(Object a,Object b,String reason) {
        checks++;
        if(!a.equals(b))throw new AssertionError(
                reason+" expected="+a+" got="+b);
    }
    private static final Map<String,Integer> WORDS=Map.ofEntries(
            Map.entry("i",65),Map.entry("will",65),Map.entry("am",65),
            Map.entry("was",65),Map.entry("dont",37),Map.entry("we",65),
            Map.entry("can",65),Map.entry("are",65),Map.entry("you",65),
            Map.entry("this",65),Map.entry("hello",75),Map.entry("know",65),
            Map.entry("friend",12),Map.entry("tell",55));
    private static final Map<String,List<String>> NEXT=Map.of(
            "i",List.of("am","will","was","dont"),
            "we",List.of("can","are"),"you",List.of("are","can"),
            "this",List.of("is","was"));
    private static List<String> suggest(String input) {
        return ShortJoinedContext362.suggestions(input,WORDS,NEXT);
    }
    public static void main(String[] args) {
        eq(List.of("I will","Iwikk"),suggest("Iwikk"),
                "Screenshot: uppercase I + double neighboring-key typo");
        eq(List.of("I will","iwikk"),suggest("iwikk"),
                "Lowercase I normalizes first-person pronoun");
        eq(List.of("I WILL","IWIKK"),suggest("IWIKK"),
                "All caps preserved");
        eq(List.of("I will","Iwill"),suggest("Iwill"),
                "Exact missing space");
        eq(List.of("I am","Iam"),suggest("Iam"),
                "Three letter missing space");
        eq(List.of("we can","wecan"),suggest("wecan"),
                "Non-I dictionary pair");
        eq(List.of("You are","Youare"),suggest("Youare"),
                "Capitalized sentence-case pair");
        eq(List.of(),suggest("hello"),
                "Known word never split");
        eq(List.of(),suggest("will"),
                "Known continuation never split");
        eq(List.of(),suggest("iwikked"),
                "Unverified extra suffix never guessed");
        eq(List.of(),suggest("iwixx"),
                "Non-neighbor repeated typo never guessed");
        eq(List.of(),suggest("iwilkk"),
                "Three errors never guessed");
        eq(List.of(),suggest("iwi"),
                "Incomplete candidate has no exact supported pair");
        eq(List.of(),suggest("Iwill2"),
                "Numbers not predicted");
        eq(List.of(),suggest("I.wikk"),
                "Punctuation not interpreted as missing space");
        eq(List.of(),suggest("https://secret"),
                "URLs never interpreted");
        eq(List.of(),ShortJoinedContext362.suggestions("Iam",
                WORDS,Map.of()),"No supported context means no speculation");
        eq(List.of(),ShortJoinedContext362.suggestions("Iwikk",
                Map.of("i",65,"will",11),NEXT),
                "Untrusted low-frequency data rejected");
        eq(List.of(),ShortJoinedContext362.suggestions("iwikk",
                null,NEXT),"Missing dictionary fails closed");
        eq(List.of(),ShortJoinedContext362.suggestions("iwikk",
                WORDS,null),"Missing transition map fails closed");
        // Known words trump the short joined correction (no autocorrect).
        Map<String,Integer> enriched=new HashMap<>(WORDS);
        enriched.put("iwikk",12);
        eq(List.of(),ShortJoinedContext362.suggestions(
                "Iwikk",enriched,NEXT),"User-known word preserved");
        // Exercise variation and bounds without user messages or telemetry.
        Random random=new Random(36220261030L);
        String[] candidates={"Iwikk","Iwill","Iam","wecan","Youare",
                "hello","iwikks","iwillthe","it","Iam!","IWiKk"};
        for(int n=0;n<30_000;n++) {
            String word=candidates[random.nextInt(candidates.length)];
            List<String> suggestions=suggest(word);
            if(!suggestions.isEmpty()) {
                eq(2,suggestions.size(),"Two explicit chips max");
                eq(word,suggestions.get(1),"Original always preserved");
                boolean separated=suggestions.get(0).contains(" ");
                eq(true,separated,"Correction contains real word boundary");
            }
        }
        System.out.println("PASS Research 362: "+checks+
                " short-joined context, privacy and regression checks");
    }
}
