package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Research 356: immutable, pre-ranked suggestions built only from a verified
 * opt-in typed-word evidence snapshot, intended for a background worker.
 *
 * The UI lookup never sorts, performs storage I/O, or mutates the archive.
 * This is NOT yet wired into the phone-tested Candidate 354.
 * No text, app IDs, or telemetry are logged or transmitted.
 */
final class LearnedSuggestionIndex356 {
    private static final int MIN = TypedSequenceArchive341.MIN_EVIDENCE;
    private static final int MAX_WORD_RESULTS=3;
    private static final int MAX_NEXT_RESULTS=5;
    private static final class Ranked {
        final String value;
        final int count;
        Ranked(String text,int n) { value=text;count=n; }
    }
    private static final Comparator<Ranked> BY_RANK =
            Comparator.<Ranked>comparingInt(e -> -e.count)
                    .thenComparing(e -> e.value);
    private final List<String> rankedWords;
    private final Map<String,List<String>> continuations;

    private LearnedSuggestionIndex356(List<String> words,
            Map<String,List<String>> next) {
        rankedWords=List.copyOf(words);
        continuations=Map.copyOf(next);
    }

    static LearnedSuggestionIndex356 fromVerifiedEvidence(
            Map<String,Integer> words, Map<String,Integer> pairs) {
        // Only already-validated, bounded data from the opt-in store may
        // reach this builder. Reject corrupted or orphaned evidence.
        TypedSequenceArchive341.verifyRawEvidence(words,pairs);
        ArrayList<Ranked> wordRanks=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:words.entrySet()) {
            if(entry.getValue()>=MIN)
                wordRanks.add(new Ranked(entry.getKey(),entry.getValue()));
        }
        wordRanks.sort(BY_RANK);
        ArrayList<String> wordOrder=new ArrayList<>(wordRanks.size());
        for(Ranked entry:wordRanks)wordOrder.add(entry.value);

        Map<String,List<Ranked>> pairRanks=new HashMap<>();
        for(Map.Entry<String,Integer> entry:pairs.entrySet()) {
            if(entry.getValue()<MIN)continue;
            String pair=entry.getKey();
            int split=pair.indexOf(' ');
            String prior=pair.substring(0,split);
            String next=pair.substring(split+1);
            pairRanks.computeIfAbsent(prior,k->new ArrayList<>())
                    .add(new Ranked(next,entry.getValue()));
        }
        HashMap<String,List<String>> nextMap=new HashMap<>();
        for(Map.Entry<String,List<Ranked>> group:pairRanks.entrySet()) {
            List<Ranked> values=group.getValue();
            values.sort(BY_RANK);
            ArrayList<String> order=new ArrayList<>(
                    Math.min(values.size(),MAX_NEXT_RESULTS));
            for(Ranked candidate:values) {
                if(order.size()>=MAX_NEXT_RESULTS)break;
                order.add(candidate.value);
            }
            nextMap.put(group.getKey(),List.copyOf(order));
        }
        return new LearnedSuggestionIndex356(wordOrder,nextMap);
    }

    /** Same explicit tap-only candidate order as Candidate 354. */
    List<String> nextWords(String preceding,int limit) {
        if(limit<=0)return List.of();
        String key=TypedSequenceArchive341.canonical(preceding);
        if(key==null)return List.of();
        List<String> matches=continuations.get(key);
        if(matches==null)return List.of();
        return matches.subList(0,Math.min(matches.size(),
                Math.min(MAX_NEXT_RESULTS,limit)));
    }

    /** No regex, sorting or storage lookup on this frequent UI path. */
    List<String> completedWords(String prefix,int limit) {
        if(limit<=0 || prefix==null || prefix.length()<2 ||
                prefix.length()>24)return List.of();
        for(int i=0;i<prefix.length();i++) {
            char ch=prefix.charAt(i);
            if(!((ch>='a'&&ch<='z')||(ch>='A'&&ch<='Z')))
                return List.of();
        }
        String needle=prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> matches=new ArrayList<>(Math.min(MAX_WORD_RESULTS,limit));
        for(String word:rankedWords) {
            if(word.startsWith(needle) && !word.equals(needle)) {
                matches.add(word);
                if(matches.size()>=Math.min(MAX_WORD_RESULTS,limit))break;
            }
        }
        return Collections.unmodifiableList(matches);
    }

    int indexedWords(){return rankedWords.size();}
    int indexedContexts(){return continuations.size();}
}
