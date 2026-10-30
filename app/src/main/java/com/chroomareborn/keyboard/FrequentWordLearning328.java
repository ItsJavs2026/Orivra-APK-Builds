package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Local opt-in word frequency. Public or hand-saved words only. */
final class FrequentWordLearning328 {
    static final String PREFS="orivra_personal_word_completions_v1";
    private static final String KEY="freq:";
    private FrequentWordLearning328(){}
    static void clear(Context c) {
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();
    }
    private static boolean approved(Context c,String w) {
        if(PredictionLite.publicWordForContext(w))return true;
        for(String saved:PersonalDictionary.savedWords(c))
            if(w.equals(EnglishApostrophePolicy.canonical(saved)))return true;
        return false;
    }
    static void recordCompleted(Context c,String word) { record(c,word,1); }
    static void recordAccepted(Context c,String word) { record(c,word,2); }
    private static void record(Context c,String raw,int weight) {
        if(c==null||!PersonalLearning.enabled(c))return;
        String w=EnglishApostrophePolicy.canonical(raw);
        if(w==null||!approved(c,w))return;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String key=KEY+w;
        if(!p.contains(key)&&p.getAll().size()>=FrequentWordCompletionPolicy328.MAX_WORDS)
            return;
        int old=p.getInt(key,0);
        p.edit().putInt(key,Math.min(1000,Math.max(0,old)+weight)).apply();
    }
    static List<String> matching(Context c,String raw,int limit) {
        if(c==null||!PersonalLearning.enabled(c))return List.of();
        Map<String,Integer> counts=new HashMap<>();
        for(Map.Entry<String,?> e:c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getAll().entrySet()) {
            if(!e.getKey().startsWith(KEY)||!(e.getValue() instanceof Integer))
                continue;
            int n=(Integer)e.getValue();
            String w=e.getKey().substring(KEY.length());
            if(n>0&&n<=1000&&w.equals(EnglishApostrophePolicy.canonical(w)))
                counts.put(w,n);
        }
        List<String> ranked=FrequentWordCompletionPolicy328.matching(raw,counts,limit);
        java.util.ArrayList<String> result=new java.util.ArrayList<>();
        for(String word:ranked) {
            String canonical=EnglishApostrophePolicy.canonical(word);
            if(canonical!=null&&approved(c,canonical))result.add(word);
        }
        return result;
    }
}
