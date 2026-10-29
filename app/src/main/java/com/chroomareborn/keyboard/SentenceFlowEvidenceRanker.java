package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bounded, evidence-only re-ranking among candidates the OFFLINE predictor
 * has already found. Never manufactures text, never auto-commits, and never
 * modifies correctly spelled dictionary words handled upstream.
 *
 * Promotion requires more offline-supported word transitions, no worse
 * compact-text edit distance, and a substantial coherence improvement.
 * Ambiguous ties and strong existing predictions keep their original order.
 */
final class SentenceFlowEvidenceRanker {
    private SentenceFlowEvidenceRanker() {}

    private static String compact(String text) {
        return text.toLowerCase(Locale.US).replaceAll("[^a-z]","");
    }

    private static int countWords(String input) {
        String clean=input.trim().replaceAll(" +"," ");
        if(clean.isEmpty())return 0;
        int count=1;
        for(int i=0;i<clean.length();i++)if(clean.charAt(i)==' ')count++;
        return count;
    }

    static List<String> rerank(String typed,List<String> candidates,
                               Map<String,List<String>> common,
                               Map<String,List<String>> context) {
        if(typed==null || candidates==null || candidates.size()<2 ||
                typed.length()<13 || typed.length()>65 ||
                Thread.currentThread().isInterrupted())return candidates;
        String raw=compact(typed);
        if(raw.length()<13 || raw.length()>65)return candidates;
        String original=candidates.get(0);
        if(original==null || countWords(original)<3)return candidates;
        double firstCoherence=SentenceRescue.coherence(
                original,common,context);
        // Already supported? Don't replace a coherent suggestion with a
        // speculative alternative that happens to have two matching pairs.
        if(firstCoherence>=.70)return candidates;
        int maxEdits=Math.max(2,raw.length()/4);
        int firstEdits=PredictionLite.editDistanceLimited(
                raw,compact(original),maxEdits);
        if(firstEdits>maxEdits)return candidates;
        int bestIndex=-1;
        double bestGain=2.5;
        // Strictly inspect the 3 visible suggestion candidates, not an
        // unbounded cache or hidden speculative options.
        int until=Math.min(3,candidates.size());
        for(int i=1;i<until;i++) {
            String alternative=candidates.get(i);
            if(alternative==null || countWords(alternative)<3)continue;
            int nextEdits=PredictionLite.editDistanceLimited(
                    raw,compact(alternative),maxEdits);
            if(nextEdits>firstEdits || nextEdits>maxEdits)continue;
            double coherence=SentenceRescue.coherence(
                    alternative,common,context);
            if(coherence<.67 || coherence<firstCoherence+.32)continue;
            // Slightly prefer candidates already ranked closer to the top;
            // raw closeness cannot substitute for phrase evidence.
            double gain=(coherence-firstCoherence)*9.0+
                    (firstEdits-nextEdits)*1.75-i*.35;
            if(gain>bestGain) {bestGain=gain;bestIndex=i;}
        }
        if(bestIndex<0)return candidates;
        ArrayList<String> reordered=new ArrayList<>(candidates);
        String promoted=reordered.remove(bestIndex);
        reordered.add(0,promoted);
        return reordered;
    }
}
