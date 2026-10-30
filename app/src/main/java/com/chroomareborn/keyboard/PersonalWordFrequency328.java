package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    static void recordAcceptedPhrase(Context c,String selectedPhrase) {
        if(c==null || !PersonalLearning.enabled(c))return;
        List<String> selected=AcceptedPhraseFrequencyPolicy335.words(selectedPhrase);
        if(selected.isEmpty())return;
        // The old code called recordTapped separately for every chosen word,
        // repeatedly reading SharedPreferences and scanning the dictionary,
        // then scheduling up to twelve disk updates on a single chip tap.
        // Accumulate at most 12 approved individual counters in ONE write.
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        Map<String,?> existing=p.getAll();
        Set<String> saved=null;
        SharedPreferences.Editor editor=p.edit();
        int count=existing.size();
        boolean changed=false;
        for(String token:selected) {
            if(!PredictionLite.publicWordForContext(token)) {
                if(saved==null) {
                    saved=new HashSet<>();
                    for(String custom:PersonalDictionary.savedWords(c)) {
                        String word=EnglishApostrophePolicy.canonical(custom);
                        if(word!=null)saved.add(word);
                    }
                }
                if(!saved.contains(token))continue;
            }
            String key=PREFIX+token;
            Object prior=existing.get(key);
            if(prior!=null && !(prior instanceof Integer))continue;
            if(prior==null) {
                if(count>=MAX_WORDS)continue;
                count++;
            }
            int old=prior==null?0:(Integer)prior;
            if(old<0 || old>1000 || old==1000)continue;
            editor.putInt(key,Math.min(1000,old+2));
            changed=true;
        }
        if(changed && PersonalLearning.enabled(c))editor.apply();
    }
    static List<String> matching(Context c,String prefix,int limit) {
        if(c==null || !PersonalLearning.enabled(c) || limit<=0)return List.of();
        String raw=EnglishApostrophePolicy.prefix(prefix);
        if(raw==null)return List.of();
        Map<String,Integer> counts=new HashMap<>();
        java.util.Set<String> personalWords=null;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        for(Map.Entry<String,?> e:p.getAll().entrySet()) {
            if(!e.getKey().startsWith(PREFIX) || !(e.getValue() instanceof Integer))
                continue;
            String word=e.getKey().substring(PREFIX.length());
            int score=(Integer)e.getValue();
            if(score<1 || score>1000 ||
                    !AdaptivePrefixPolicy328.eligible(raw,word,score))
                continue;
            // Still validate manually saved words on every refresh, but
            // load the capped dictionary only ONCE per suggestion update.
            // Earlier code reread/sorted up to 250 words for each of 320
            // frequency candidates. This must never slow the key-tap UI.
            if(PredictionLite.publicWordForContext(word)) {
                counts.put(word,score);
            } else {
                if(personalWords==null) {
                    personalWords=new java.util.HashSet<>();
                    for(String saved:PersonalDictionary.savedWords(c)) {
                        String canonical=EnglishApostrophePolicy.canonical(saved);
                        if(canonical!=null)personalWords.add(canonical);
                    }
                }
                if(personalWords.contains(word))counts.put(word,score);
            }
        }
        return AdaptivePrefixPolicy328.candidates(prefix,counts,limit);
    }
}
