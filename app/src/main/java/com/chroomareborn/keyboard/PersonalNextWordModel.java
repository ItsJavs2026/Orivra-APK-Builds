package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental offline-only next-word learner. No network, logging, or storage.
 * Callers must gate observations by editor privacy and explicit learning settings.
 * State is memory-only until a separate, user-controlled persistence layer exists.
 */
final class PersonalNextWordModel {
    private final Map<String,Map<String,Integer>> transitions=new HashMap<>();
    private final Map<String,Integer> unigram=new HashMap<>();
    private int observations;
    // Transient editor-local context, discarded when an editor changes.
    private String previousCommitted;
    private String latestCommitted;
    private static final int MAX_CONTEXTS=4096;
    private static final int MAX_NEXT_WORDS_PER_CONTEXT=64;
    private static final int MAX_VOCABULARY=8192;

    private static List<String> tokenize(String text) {
        ArrayList<String> out=new ArrayList<>();
        if(text==null)return out;
        for(String raw:text.toLowerCase(Locale.US).split("[^a-z0-9']+")) {
            String word=raw.replaceAll("^'+|'+$","");
            if(word.length()>0 && word.length()<=32)out.add(word);
        }
        return out;
    }

    synchronized void observeCommittedText(String text,boolean learningAllowed) {
        if(!learningAllowed || text==null)return;
        // This optional training API is intentionally stricter than tokenizing
        // arbitrary text: never learn from addresses, URLs or contact payloads.
        if(text.indexOf('@')>=0 || text.contains("://") || text.indexOf('/')>=0 ||
                text.indexOf('\\')>=0)return;
        boolean learned=false;
        // Periods, questions, paragraphs and other sentence boundaries must
        // never create an unrelated two- or three-word continuation.
        for(String sentence:text.split("[.!?;\\r\\n]+")) {
            List<String> words=tokenize(sentence);
            if(words.size()<2)continue;
            for(int i=0;i<words.size();i++) {
                String word=words.get(i);
                recordWord(word);
                if(i>0)addTransition(words.get(i-1),word);
                if(i>1)addTransition(words.get(i-2)+" "+words.get(i-1),word);
            }
            learned=true;
        }
        if(learned)observations++;
    }

    /**
     * Observe exactly one committed, accepted word. Unlike observeCommittedText,
     * this supports the word-at-a-time commits of Android input methods.
     * Never call this unless a real editor-level privacy gate has allowed it.
     */
    synchronized void observeCommittedWord(String raw,boolean learningAllowed) {
        if(!learningAllowed) {
            resetContext();
            return;
        }
        if(raw==null) {
            resetContext();
            return;
        }
        // Reject control characters BEFORE trim() can erase a word boundary.
        // A malformed or cross-editor event must invalidate transient context.
        for(int i=0;i<raw.length();i++)
            if(Character.isISOControl(raw.charAt(i))) {
                resetContext();
                return;
            }
        String trimmed=raw.trim();
        if(trimmed.isEmpty() || trimmed.contains(" ")
                || trimmed.contains("@") || trimmed.contains("/")
                || trimmed.contains(":") || trimmed.contains("\\")) {
            resetContext();
            return;
        }
        List<String> tokens=tokenize(trimmed);
        if(tokens.size()!=1) {
            resetContext();
            return;
        }
        String word=tokens.get(0);
        if(!word.matches("[a-z][a-z']{0,31}")) {
            resetContext();
            return;
        }
        recordWord(word);
        if(latestCommitted!=null)addTransition(latestCommitted,word);
        if(previousCommitted!=null && latestCommitted!=null)
            addTransition(previousCommitted+" "+latestCommitted,word);
        previousCommitted=latestCommitted;
        latestCommitted=word;
        observations++;
        // Avoid treating the first word of a new sentence as the continuation
        // of the previous sentence.
        if(trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!"))
            resetContext();
    }

    synchronized List<String> suggestAfterCommittedWord(int limit) {
        if(latestCommitted==null)return Collections.emptyList();
        String context=previousCommitted==null
                ?latestCommitted:previousCommitted+" "+latestCommitted;
        return suggest(context,limit);
    }

    /**
     * Reconstruct one tap-only multiword continuation from repeated typed
     * sequences. No speculative infilling or raw sentence persistence.
     */
    synchronized List<String> suggestPhrase(String committedContext,int maxNextWords) {
        if(maxNextWords<2)return Collections.emptyList();
        List<String> observed=tokenize(committedContext);
        if(observed.isEmpty())return Collections.emptyList();
        String context=observed.get(observed.size()-1);
        if(observed.size()>1)
            context=observed.get(observed.size()-2)+" "+context;
        ArrayList<String> chain=new ArrayList<>();
        for(int i=0;i<Math.min(3,maxNextWords);i++) {
            List<String> next=suggest(context,1);
            if(next.isEmpty())break;
            String word=next.get(0);
            if(chain.contains(word))break; // reject repetitive loops
            chain.add(word);
            List<String> tokens=tokenize(context+" "+word);
            int length=tokens.size();
            context=tokens.get(length-2)+" "+tokens.get(length-1);
        }
        return chain.size()<2?Collections.emptyList():
                List.of(String.join(" ",chain));
    }

    synchronized void resetContext() {
        previousCommitted=null;
        latestCommitted=null;
    }

    private void recordWord(String word) {
        if(!unigram.containsKey(word) && unigram.size()>=MAX_VOCABULARY)return;
        unigram.put(word,Math.min(10000,unigram.getOrDefault(word,0)+1));
    }

    private void addTransition(String context,String next) {
        Map<String,Integer> counts=transitions.get(context);
        if(counts==null) {
            if(transitions.size()>=MAX_CONTEXTS)return;
            counts=new HashMap<>();
            transitions.put(context,counts);
        }
        if(!counts.containsKey(next) && counts.size()>=MAX_NEXT_WORDS_PER_CONTEXT)return;
        counts.put(next,Math.min(10000,counts.getOrDefault(next,0)+1));
    }

    synchronized List<String> suggest(String committedContext,int limit) {
        if(limit<=0)return Collections.emptyList();
        List<String> words=tokenize(committedContext);
        if(words.isEmpty())return Collections.emptyList();
        String last=words.get(words.size()-1);
        String pair=words.size()>1?words.get(words.size()-2)+" "+last:null;
        Map<String,Double> scores=new HashMap<>();
        addScores(scores,transitions.get(last),2.0);
        if(pair!=null)addScores(scores,transitions.get(pair),5.0);
        ArrayList<String> result=new ArrayList<>(scores.keySet());
        result.sort(Comparator.<String>comparingDouble(k->-scores.get(k))
                .thenComparing(Comparator.naturalOrder()));
        if(result.size()>limit)return new ArrayList<>(result.subList(0,limit));
        return result;
    }

    private static void addScores(Map<String,Double> scores,
                                  Map<String,Integer> counts,double weight) {
        if(counts==null)return;
        for(Map.Entry<String,Integer> e:counts.entrySet()) {
            // Never promote a one-off tap sequence. Two independently
            // completed repetitions are the minimum for session learning.
            if(e.getValue()<2)continue;
            scores.put(e.getKey(),scores.getOrDefault(e.getKey(),0.0)
                    +weight*e.getValue());
        }
    }

    synchronized int observationCount() { return observations; }
    synchronized void clear() {
        transitions.clear();
        unigram.clear();
        observations=0;
        resetContext();
    }
}
