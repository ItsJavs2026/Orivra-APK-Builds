package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, private, opt-in, completed-word frequency learning.
 * No raw keystrokes, locations, whole sentences or app identifiers.
 * User-created words qualify only if explicitly saved in the dictionary.
 */
final class PersonalWordFrequency328 {
    static final String PREFS="orivra_confirmed_word_uses_v1";
    private static final String PREFIX="word:";
    private static final int MAX_WORDS=320;
    private PersonalWordFrequency328() {}
    static void clear(Context c) {
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();
    }
    private static boolean approved(Context c,String word) {
        if(PredictionLite.publicWordForContext(word))return true;
        for(String saved:PersonalDictionary.savedWords(c))
            if(word.equals(EnglishApostrophePolicy.canonical(saved)))return true;
        return false;
    }
    static void recordCommitted(Context c,String word) {
        if(c==null || !PersonalLearning.enabled(c))return;
        String target=EnglishApostrophePolicy.canonical(word);
        if(target==null || !approved(c,target))return;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String key=PREFIX+target;
        if(!p.contains(key) && p.getAll().size()>=MAX_WORDS)return;
        p.edit().putInt(key,Math.min(1000,p.getInt(key,0)+1)).apply();
    }
    static void recordTapped(Context c,String word) {
        // A user explicitly choosing a candidate reinforces a known word.
        // Two points, but no immediate one-letter takeover.
        if(c==null || !PersonalLearning.enabled(c))return;
        String target=EnglishApostrophePolicy.canonical(word);
        if(target==null || !approved(c,target))return;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String key=PREFIX+target;
        if(!p.contains(key) && p.getAll().size()>=MAX_WORDS)return;
        p.edit().putInt(key,Math.min(1000,p.getInt(key,0)+2)).apply();
    }
    static List<String> matching(Context c,String prefix,int limit) {
        if(c==null || !PersonalLearning.enabled(c) || limit<=0)return List.of();
        String raw=EnglishApostrophePolicy.prefix(prefix);
        if(raw==null)return List.of();
        Map<String,Integer> counts=new HashMap<>();
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        for(Map.Entry<String,?> e:p.getAll().entrySet()) {
            if(!e.getKey().startsWith(PREFIX) || !(e.getValue() instanceof Integer))
                continue;
            String word=e.getKey().substring(PREFIX.length());
            int score=(Integer)e.getValue();
            if(score<1 || score>1000 ||
                    !AdaptivePrefixPolicy328.eligible(raw,word,score))
                continue;
            // Still validate the word on use: it may have been removed
            // from the personal dictionary since recording.
            if(approved(c,word))counts.put(word,score);
        }
        return AdaptivePrefixPolicy328.candidates(prefix,counts,limit);
    }
}
