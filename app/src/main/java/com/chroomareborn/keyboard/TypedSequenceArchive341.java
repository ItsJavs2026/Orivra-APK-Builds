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
        Map<String,Integer> confirmed=eligibleWords();
        TreeMap<String,Integer> anchored=new TreeMap<>();
        for(Map.Entry<String,Integer> e:pairs.entrySet()) {
            if(e.getValue()<MIN_EVIDENCE)continue;
            int separator=e.getKey().indexOf(' ');
            if(separator<1)continue;
            String left=e.getKey().substring(0,separator);
            String right=e.getKey().substring(separator+1);
            if(confirmed.containsKey(left) && confirmed.containsKey(right))
                anchored.put(e.getKey(),e.getValue());
        }
        return Collections.unmodifiableMap(anchored);
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
        for(Map.Entry<String,Integer> e:updatedPairs.entrySet()) {
            if(e.getValue()<MIN_EVIDENCE)continue;
            String pair=e.getKey();
            int sep=pair.indexOf(' ');
            String left=pair.substring(0,sep),right=pair.substring(sep+1);
            if(updatedWords.getOrDefault(left,0)<MIN_EVIDENCE ||
                    updatedWords.getOrDefault(right,0)<MIN_EVIDENCE)
                throw new IllegalArgumentException("Orphaned typed pair evidence");
        }
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

    /** Includes pending observations (counts 1-2) only for explicit-consent
     * PRIVATE on-device storage. V4 export still filters evidence >=3.
     */
    synchronized Map<String,Integer> rawWordEvidence() {
        return Collections.unmodifiableMap(new TreeMap<>(words));
    }
    synchronized Map<String,Integer> rawPairEvidence() {
        return Collections.unmodifiableMap(new TreeMap<>(pairs));
    }

    static void verifyRawEvidence(Map<String,Integer> wordCounts,
            Map<String,Integer> pairCounts) {
        if(wordCounts==null || pairCounts==null ||
                wordCounts.size()>MAX_WORDS || pairCounts.size()>MAX_PAIRS)
            throw new IllegalArgumentException("Evidence storage over capacity");
        for(Map.Entry<String,Integer> entry:wordCounts.entrySet()) {
            String word=entry.getKey();
            Integer value=entry.getValue();
            if(word==null || value==null || value<1 || value>MAX_COUNT ||
                    !word.equals(canonical(word)))
                throw new IllegalArgumentException("Invalid typed-word evidence");
        }
        for(Map.Entry<String,Integer> entry:pairCounts.entrySet()) {
            String pair=entry.getKey();
            Integer count=entry.getValue();
            if(pair==null || count==null || count<1 || count>MAX_COUNT)
                throw new IllegalArgumentException("Invalid typed-pair count");
            int split=pair.indexOf(' ');
            if(split<1 || split!=pair.lastIndexOf(' ') ||
                    !pair.equals(pairKey(pair.substring(0,split),
                            pair.substring(split+1))))
                throw new IllegalArgumentException("Invalid typed pair");
            if(wordCounts.getOrDefault(pair.substring(0,split),0)<count ||
                    wordCounts.getOrDefault(pair.substring(split+1),0)<count)
                throw new IllegalArgumentException("Pair lacks verified word evidence");
        }
    }

    /** Strict, all-or-nothing restoration, never merges with an editor context.
     * Use only from the research Android private store after consent.
     */
    synchronized void restoreRawEvidence(Map<String,Integer> wordCounts,
            Map<String,Integer> pairCounts) {
        verifyRawEvidence(wordCounts,pairCounts);
        words.clear();words.putAll(wordCounts);
        pairs.clear();pairs.putAll(pairCounts);
        previousInEditor=null;
    }

    synchronized int observedVocabularyCount() { return words.size(); }
    synchronized int observedPairCount() { return pairs.size(); }
}
