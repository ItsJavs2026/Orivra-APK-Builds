package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Explicit, user-driven import/export storage adapter. Never runs in the IME.
 * Only hand-saved personal words and accepted correction pairs are backed up.
 * Existing V1 preference names/keys and the enabled preference are preserved.
 */
final class LearningTransferStore {
    // Existing PersonalDictionary V2 private storage naming, intentionally
    // repeated here instead of changing the phone-accepted candidate.
    private static final String WORD_PREFS="orivra_manual_dictionary_v2";
    private static final String WORD_PREFIX="word:";
    private static final String LEARN_PREFIX="accepted:";

    private LearningTransferStore() {}

    static final class Preview {
        final int newWords,newPairs,strongerPairs;
        Preview(int words,int pairs,int stronger){
            newWords=words;newPairs=pairs;strongerPairs=stronger;
        }
    }
    private static final class Plan {
        final Map<String,String> addWords=new LinkedHashMap<>();
        final Map<String,Integer> writePairs=new LinkedHashMap<>();
        final Preview preview;
        Plan(int words,int pairs,int stronger){
            preview=new Preview(words,pairs,stronger);
        }
    }
    private static String canonicalPair(String raw) {
        if(raw==null)return null;
        int p=raw.indexOf(':');
        if(p<=0||p!=raw.lastIndexOf(':'))return null;
        String left=PersonalWordCanonical.canonical(raw.substring(0,p));
        String right=PersonalWordCanonical.canonical(raw.substring(p+1));
        if(left==null||right==null||left.equals(right))return null;
        return left+":"+right;
    }
    private static SharedPreferences words(Context ctx){
        return ctx.getSharedPreferences(WORD_PREFS,Context.MODE_PRIVATE);
    }
    private static SharedPreferences learning(Context ctx){
        return ctx.getSharedPreferences(PersonalLearning.PREFS,Context.MODE_PRIVATE);
    }

    static LearningTransferCodec.Bundle snapshot(Context context) {
        List<String> words=PersonalDictionary.savedWords(context);
        TreeMap<String,Integer> pairs=new TreeMap<>();
        for(Map.Entry<String,?> entry:learning(context).getAll().entrySet()) {
            if(!entry.getKey().startsWith(LEARN_PREFIX)||
                    !(entry.getValue() instanceof Integer))continue;
            String canonical=canonicalPair(
                    entry.getKey().substring(LEARN_PREFIX.length()));
            int count=(Integer)entry.getValue();
            if(canonical!=null && count>=1 && count<=1000)
                pairs.merge(canonical,count,Math::max);
        }
        return LearningTransferCodec.decode(
                LearningTransferCodec.encode(words,pairs));
    }
    static String exportText(Context context) {
        LearningTransferCodec.Bundle data=snapshot(context);
        return LearningTransferCodec.encode(data.words,data.learnedPairs);
    }

    private static Plan buildPlan(Context context,LearningTransferCodec.Bundle bundle){
        if(context==null||bundle==null)throw new IllegalArgumentException("Missing backup");
        // Round-trip to validate the entire in-memory bundle before any
        // mutation; do not trust a crafted Bundle object from tests.
        LearningTransferCodec.Bundle safe=LearningTransferCodec.decode(
                LearningTransferCodec.encode(bundle.words,bundle.learnedPairs));
        SharedPreferences manual=words(context);
        SharedPreferences learned=learning(context);
        Map<String,?> existing=learned.getAll();
        TreeMap<String,String> selectedExistingKey=new TreeMap<>();
        TreeMap<String,Integer> selectedExistingCount=new TreeMap<>();
        for(Map.Entry<String,?> entry:existing.entrySet()) {
            if(!entry.getKey().startsWith(LEARN_PREFIX) ||
                    !(entry.getValue() instanceof Integer))continue;
            String raw=entry.getKey().substring(LEARN_PREFIX.length());
            String pair=canonicalPair(raw);
            int count=(Integer)entry.getValue();
            if(pair==null||count<1||count>1000)continue;
            String selected=selectedExistingKey.get(pair);
            int previously=selectedExistingCount.getOrDefault(pair,0);
            if(selected==null||count>previously||
                    (count==previously&&entry.getKey().compareTo(selected)<0)){
                selectedExistingKey.put(pair,entry.getKey());
                selectedExistingCount.put(pair,count);
            }
        }

        LinkedHashMap<String,String> newManual=new LinkedHashMap<>();
        LinkedHashMap<String,Integer> changedPairs=new LinkedHashMap<>();
        int introducedPairs=0,raised=0;
        for(String display:safe.words){
            String canonical=PersonalDictionaryPolicy.canonical(display);
            String key=WORD_PREFIX+canonical;
            if(!manual.contains(key))newManual.put(key,display);
        }
        for(Map.Entry<String,Integer> entry:safe.learnedPairs.entrySet()){
            String pair=entry.getKey();
            int count=entry.getValue();
            String target=selectedExistingKey.get(pair);
            if(target==null){
                changedPairs.put(LEARN_PREFIX+pair,count);
                introducedPairs++;
            }else if(count>selectedExistingCount.get(pair)){
                changedPairs.put(target,count);
                raised++;
            }
        }
        if(PersonalDictionary.wordCount(context)+newManual.size()>
                PersonalDictionaryPolicy.MAX_WORDS ||
                PersonalLearning.countEntries(learned)+introducedPairs>
                        LearningTransferCodec.MAX_PAIRS)
            throw new IllegalArgumentException("Import exceeds available local capacity");
        Plan plan=new Plan(newManual.size(),introducedPairs,raised);
        plan.addWords.putAll(newManual);
        plan.writePairs.putAll(changedPairs);
        return plan;
    }

    /** Safe to call before showing a user confirmation; does not write. */
    static Preview preview(Context context,LearningTransferCodec.Bundle bundle){
        return buildPlan(context,bundle).preview;
    }

    /** Import only AFTER the user confirms. An identical retry is idempotent. */
    static Preview importConfirmed(Context context,LearningTransferCodec.Bundle bundle){
        Plan plan=buildPlan(context,bundle);
        if(!plan.addWords.isEmpty()){
            SharedPreferences.Editor editor=words(context).edit();
            for(Map.Entry<String,String> e:plan.addWords.entrySet())
                editor.putString(e.getKey(),e.getValue());
            if(!editor.commit())throw new IllegalStateException("Could not save words");
        }
        if(!plan.writePairs.isEmpty()){
            SharedPreferences.Editor editor=learning(context).edit();
            for(Map.Entry<String,Integer> e:plan.writePairs.entrySet())
                editor.putInt(e.getKey(),e.getValue());
            if(!editor.commit())
                throw new IllegalStateException("Could not save corrections");
        }
        // Note: learning ENABLED is never imported or changed.
        return plan.preview;
    }
}
