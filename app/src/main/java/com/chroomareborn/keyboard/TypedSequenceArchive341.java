package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Research-only bounded evidence for typed-and-committed words.
 *
 * This model MUST NOT be wired to an IME until a separate default-OFF
 * "remember typed vocabulary" preference, private-editor gates, explicit
 * data clearing, and a portable-backup UI have been qualified.
 *
 * Potentially identifying words can still pass the lexical filter. Lexical
 * filters are NOT a privacy guarantee. Caller obtains separate consent.
 *
 * No sentences, editor IDs, app names, timestamps, clipboard content, or raw
 * keystrokes. One editor's previous word is dropped at editor boundaries.
 * Not yet used by the installed Candidate 340.
 */
final class TypedSequenceArchive341 {
    static final int MIN_EVIDENCE=3;
    static final int MAX_WORDS=192;
    static final int MAX_PAIRS=256;
    static final int MAX_COUNT=1000;

    private final Map<String,Integer> words=new HashMap<>();
    private final Map<String,Integer> pairs=new HashMap<>();
    private String previousInEditor;

    static String canonical(String raw) {
        if(raw==null || raw.length()<1 || raw.length()>24)return null;
        // Prevent invisible separators, numeric IDs, URL fragments, email,
        // shell paths, possessive names and arbitrary punctuation.
        if("I".equals(raw) || "i".equals(raw))return "i";
        if("a".equals(raw) || "A".equals(raw))return "a";
        String lower=raw.toLowerCase(Locale.ROOT);
        if(raw.matches("[A-Za-z]{2,24}"))return lower;
        if(raw.length()<=24 && EnglishApostrophePolicy.publicContraction(lower))
            return lower;
        return null;
    }

    static String pairKey(String prior,String next) {
        String a=canonical(prior),b=canonical(next);
        if(a==null || b==null || a.equals(b))return null;
        return a+" "+b;
    }

    /**
     * Feed only words verified at a real, successful Space boundary.
     * A separate remembering-consent flag defaults false and MUST be checked
     * independently from the keyboard's existing PersonalLearning flag.
     */
    synchronized void observeVerifiedSpace(String raw,boolean learningEnabled,
            boolean rememberOptIn,boolean editorAllowed,boolean committed) {
        if(!learningEnabled || !rememberOptIn || !editorAllowed || !committed) {
            previousInEditor=null;
            return;
        }
        String word=canonical(raw);
        if(word==null) {
            previousInEditor=null;
            return;
        }
        increase(words,word,MAX_WORDS);
        if(previousInEditor!=null) {
            String pair=pairKey(previousInEditor,word);
            if(pair!=null)increase(pairs,pair,MAX_PAIRS);
        }
        previousInEditor=word;
    }

    private static void increase(Map<String,Integer> map,String key,int capacity) {
        Integer old=map.get(key);
        if(old==null) {
            if(map.size()>=capacity)return;
            map.put(key,1);
        } else map.put(key,Math.min(MAX_COUNT,old+1));
    }

    synchronized void resetEditor() { previousInEditor=null; }

    synchronized void clearAll() {
        words.clear();
        pairs.clear();
        previousInEditor=null;
    }

    /** Only repeated word evidence can qualify for future persistent storage. */
    synchronized Map<String,Integer> eligibleWords() {
        return qualified(words);
    }
    synchronized Map<String,Integer> eligiblePairs() {
        return qualified(pairs);
    }
    private static Map<String,Integer> qualified(Map<String,Integer> map) {
        TreeMap<String,Integer> result=new TreeMap<>();
        for(Map.Entry<String,Integer> e:map.entrySet())
            if(e.getValue()>=MIN_EVIDENCE)result.put(e.getKey(),e.getValue());
        return Collections.unmodifiableMap(result);
    }

    /** Suggestions are explicit taps only; never autocorrect or auto-insert. */
    synchronized List<String> nextWords(String preceding,int limit) {
        String normalized=canonical(preceding);
        if(normalized==null || limit<=0)return List.of();
        String prefix=normalized+" ";
        ArrayList<Map.Entry<String,Integer>> scored=new ArrayList<>();
        for(Map.Entry<String,Integer> e:pairs.entrySet()) {
            if(e.getValue()<MIN_EVIDENCE || !e.getKey().startsWith(prefix))
                continue;
            scored.add(e);
        }
        scored.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                e->-e.getValue()).thenComparing(Map.Entry::getKey));
        ArrayList<String> out=new ArrayList<>();
        for(Map.Entry<String,Integer> e:scored) {
            if(out.size()>=Math.min(5,limit))break;
            out.add(e.getKey().substring(prefix.length()));
        }
        return out;
    }

    /**
     * Pure in-memory merge, for future validated imports. Counts merge by MAX
     * to avoid duplicate backups artificially increasing evidence.
     */
    synchronized void mergeValidated(Map<String,Integer> importedWords,
            Map<String,Integer> importedPairs) {
        if(importedWords==null || importedPairs==null ||
                importedWords.size()>MAX_WORDS || importedPairs.size()>MAX_PAIRS)
            throw new IllegalArgumentException("Archive capacity exceeded");
        for(Map.Entry<String,Integer> e:importedWords.entrySet())
            verifyWord(e.getKey(),e.getValue());
        for(Map.Entry<String,Integer> e:importedPairs.entrySet())
            verifyPair(e.getKey(),e.getValue());
        TreeMap<String,Integer> updatedWords=new TreeMap<>(words);
        TreeMap<String,Integer> updatedPairs=new TreeMap<>(pairs);
        for(Map.Entry<String,Integer> e:importedWords.entrySet())
            updatedWords.merge(e.getKey(),e.getValue(),Math::max);
        for(Map.Entry<String,Integer> e:importedPairs.entrySet())
            updatedPairs.merge(e.getKey(),e.getValue(),Math::max);
        if(updatedWords.size()>MAX_WORDS || updatedPairs.size()>MAX_PAIRS)
            throw new IllegalArgumentException("Archive merge capacity exceeded");
        words.clear();words.putAll(updatedWords);
        pairs.clear();pairs.putAll(updatedPairs);
    }

    static void verifyWord(String word,Integer count) {
        if(word==null || count==null || count<MIN_EVIDENCE || count>MAX_COUNT ||
                !word.equals(canonical(word)))
            throw new IllegalArgumentException("Invalid learned vocabulary");
    }

    static void verifyPair(String pair,Integer count) {
        if(pair==null || count==null || count<MIN_EVIDENCE ||
                count>MAX_COUNT || pair.length()>49)
            throw new IllegalArgumentException("Invalid learned pair");
        int i=pair.indexOf(' ');
        if(i<1 || i!=pair.lastIndexOf(' ') ||
                !pair.equals(pairKey(pair.substring(0,i),pair.substring(i+1))))
            throw new IllegalArgumentException("Invalid learned pair");
    }

    synchronized int observedVocabularyCount() { return words.size(); }
    synchronized int observedPairCount() { return pairs.size(); }
}
