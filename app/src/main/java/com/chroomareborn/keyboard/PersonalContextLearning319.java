package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Small, opt-in, explicit-tap-only contextual word-pair learner.
 * Persists at most 240 [allowlisted preceding word -> accepted word] counts.
 * It NEVER saves a sentence, unmatched editor context, clipboard, URLs,
 * password fields, typing telemetry or network data.
 */
final class PersonalContextLearning319 {
    static final String PREFS="orivra_explicit_context_learning_v1";
    private static final String PREFIX="pair:";
    private static final int MAX_PAIRS=240;
    private PersonalContextLearning319() {}
    static void clear(Context c) {
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();
    }
    static void record(Context c,String previous,String accepted) {
        if(c==null || !PersonalLearning.enabled(c))return;
        String left=AcceptedContextPolicy319.safeWord(previous);
        String target=AcceptedContextPolicy319.acceptedWord(accepted);
        if(left==null || target==null || left.equals(target) ||
                !approvedTarget(c,target))return;
        SharedPreferences prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String key=PREFIX+left+":"+target;
        if(!prefs.contains(key) && prefs.getAll().size()>=MAX_PAIRS)return;
        int old=prefs.getInt(key,0);
        prefs.edit().putInt(key,Math.min(1000,old+1)).apply();
    }
    private static boolean approvedTarget(Context c,String normalized) {
        if(PredictionLite.publicWordForContext(normalized))return true;
        // Punctuation-normalized public contractions are safe too; reject
        // arbitrary possessive names, since only a known base can qualify.
        if(normalized.indexOf('\'')>=0 &&
                PredictionLite.publicWordForContext(normalized.replace("'","")))
            return true;
        // A previously unknown word is eligible only after the user manually
        // saved it, so the context store does not silently collect names.
        for(String saved:PersonalDictionary.savedWords(c)) {
            String key=AcceptedContextPolicy319.acceptedWord(saved);
            if(normalized.equals(key))return true;
        }
        return false;
    }
    static void recordChosenPhrase(Context c,String selectedPhrase) {
        if(c==null || !PersonalLearning.enabled(c))return;
        List<String> parts=AcceptedContextPolicy319.phraseWords(selectedPhrase);
        for(int i=1;i<parts.size();i++)record(c,parts.get(i-1),parts.get(i));
    }
    static List<String> suggest(Context c,String previous,int limit) {
        String left=AcceptedContextPolicy319.safeWord(previous);
        if(c==null || !PersonalLearning.enabled(c) || left==null || limit<=0)
            return List.of();
        SharedPreferences prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String prefix=PREFIX+left+":";
        ArrayList<Map.Entry<String,Integer>> sorted=new ArrayList<>();
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()) {
            if(!entry.getKey().startsWith(prefix) ||
                    !(entry.getValue() instanceof Integer))continue;
            int count=(Integer)entry.getValue();
            String target=entry.getKey().substring(prefix.length());
            if(count<2 || count>1000 ||
                    !target.equals(AcceptedContextPolicy319.acceptedWord(target)) ||
                    !approvedTarget(c,target))continue;
            sorted.add(Map.entry(target,count));
        }
        sorted.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                item->-item.getValue()).thenComparing(Map.Entry::getKey));
        ArrayList<String> result=new ArrayList<>();
        for(Map.Entry<String,Integer> e:sorted) {
            if(result.size()>=Math.min(limit,5))break;
            result.add(e.getKey());
        }
        return result;
    }
}
