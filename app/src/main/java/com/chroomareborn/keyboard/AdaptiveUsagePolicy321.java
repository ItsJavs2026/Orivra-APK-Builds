package com.chroomareborn.keyboard;

import java.util.Map;

/**
 * When explicit Learning is ON, successful word-boundary commits may
 * reinforce a bounded, general word pair. There is no passive key logger,
 * no automatic correction, and no promotion from one accidental use.
 */
final class AdaptiveUsagePolicy321 {
    private AdaptiveUsagePolicy321() {}
    static final int MIN_TYPED_REPETITIONS=3;
    static boolean suggestable(int confirmedTaps,int usedWordCommits) {
        return confirmedTaps>=2 ||
               usedWordCommits>=MIN_TYPED_REPETITIONS ||
               (confirmedTaps>=1 && usedWordCommits>=2);
    }
    static int rank(int taps,int used) {
        return Math.min(10000,Math.max(0,taps)*4+Math.max(0,used));
    }
    /** One confirmed correction + two independent ordinary uses strengthens
     * an established choice. Multiple competing accepted corrections abstain.
     * Use the original case policy so English first-person I stays uppercase.
     */
    static String reinforcedCorrection(String typed,
            Map<String,Integer> choices,int usedCount) {
        if(typed==null || choices==null || choices.size()!=1 ||
                usedCount<2)return null;
        Map.Entry<String,Integer> chosen=choices.entrySet().iterator().next();
        if(chosen.getValue()==null || chosen.getValue()!=1)return null;
        String normalized=EnglishApostrophePolicy.canonical(chosen.getKey());
        String original=EnglishApostrophePolicy.canonical(typed);
        if(normalized==null||original==null||normalized.equals(original))
            return null;
        return LearningV2Policy.displayCorrection(typed,normalized);
    }
}
