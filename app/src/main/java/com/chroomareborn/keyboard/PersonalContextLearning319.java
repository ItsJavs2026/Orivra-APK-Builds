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
    /** Typed and committed known words reinforce personal usage only at a
     * real Space boundary, when the editor allows learning and the user has
     * opted in. Unknown text and unapproved names are never recorded. */
    static void recordUsed(Context c,String previous,String committedWord) {
        if(c==null || !PersonalLearning.enabled(c))return;
        String left=AcceptedContextPolicy319.safeWord(previous);
        String target=AcceptedContextPolicy319.acceptedWord(committedWord);
        if(left==null || target==null || left.equals(target) ||
                !approvedTarget(c,target))return;
        SharedPreferences prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String key="used:"+left+":"+target;
        if(!prefs.contains(key) && prefs.getAll().size()>=MAX_PAIRS)return;
        prefs.edit().putInt(key,Math.min(1000,prefs.getInt(key,0)+1)).apply();
    }
    /** Bounded aggregate evidence: never returns individual editor text. */
    static int usedCount(Context c,String word) {
        String target=AcceptedContextPolicy319.acceptedWord(word);
        if(c==null || !PersonalLearning.enabled(c) || target==null)return 0;
        int count=0;
        for(Map.Entry<String,?> e:c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getAll().entrySet()) {
            if(!e.getKey().startsWith("used:") ||
                    !(e.getValue() instanceof Integer) ||
                    !e.getKey().endsWith(":"+target))continue;
            int observations=(Integer)e.getValue();
            if(observations>0&&observations<=1000)
                count=Math.min(1000,count+observations);
        }
        return count;
    }
    private static boolean publicTarget(String normalized) {
        return PredictionLite.publicWordForContext(normalized) ||
                (normalized.indexOf('\'')>=0 &&
                 PredictionLite.publicWordForContext(normalized.replace("'","")));
    }
    private static boolean approvedTarget(Context c,String normalized) {
        if(publicTarget(normalized))return true;
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
        String acceptedPrefix=PREFIX+left+":", usedPrefix="used:"+left+":";
        java.util.HashMap<String,int[]> evidence=new java.util.HashMap<>();
        // Single bounded scan, then one personal-word lookup only if needed.
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()) {
            String key=entry.getKey();
            boolean accepted=key.startsWith(acceptedPrefix);
            if(!accepted && !key.startsWith(usedPrefix))continue;
            if(!(entry.getValue() instanceof Integer))continue;
            int count=(Integer)entry.getValue();
            if(count<1 || count>1000)continue;
            String target=key.substring((accepted?acceptedPrefix:usedPrefix).length());
            if(!target.equals(AcceptedContextPolicy319.acceptedWord(target)))
                continue;
            int[] values=evidence.computeIfAbsent(target,k->new int[2]);
            values[accepted?0:1]=count;
        }
        ArrayList<Map.Entry<String,Integer>> sorted=new ArrayList<>();
        java.util.Set<String> personalWords=null;
        for(Map.Entry<String,int[]> item:evidence.entrySet()) {
            int[] values=item.getValue();
            if(!AdaptiveUsagePolicy321.suggestable(values[0],values[1]))continue;
            String target=item.getKey();
            if(!publicTarget(target)) {
                if(personalWords==null) {
                    personalWords=new java.util.HashSet<>();
                    for(String word:PersonalDictionary.savedWords(c)) {
                        String normalized=AcceptedContextPolicy319.acceptedWord(word);
                        if(normalized!=null)personalWords.add(normalized);
                    }
                }
                if(!personalWords.contains(target))continue;
            }
            sorted.add(Map.entry(target,
                    AdaptiveUsagePolicy321.rank(values[0],values[1])));
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
