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
        String left=EnglishApostrophePolicy.canonical(raw.substring(0,p));
        String right=EnglishApostrophePolicy.canonical(raw.substring(p+1));
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


    // Research 325: completely optional format, keeping V1 exports unchanged.
    private static SharedPreferences contextPrefs(Context c) {
        return c.getSharedPreferences(PersonalContextLearning319.PREFS,
                Context.MODE_PRIVATE);
    }
    private static boolean approvedTarget(String word,List<String> manual) {
        if(PredictionLite.publicWordForContext(word))return true;
        for(String saved:manual) {
            if(word.equals(AcceptedContextPolicy319.acceptedWord(saved)))
                return true;
        }
        return false;
    }
    static String exportV2(Context c) {
        LearningTransferCodec.Bundle legacy=snapshot(c);
        TreeMap<String,int[]> grouped=new TreeMap<>();
        for(Map.Entry<String,?> e:contextPrefs(c).getAll().entrySet()){
            String key=e.getKey();
            boolean tap=key.startsWith("pair:");
            if(!tap && !key.startsWith("used:"))continue;
            if(!(e.getValue() instanceof Integer))continue;
            int count=(Integer)e.getValue();
            if(count<=0||count>1000)continue;
            String raw=key.substring(5);
            int sep=raw.indexOf(':');
            if(sep<=0||sep!=raw.lastIndexOf(':'))continue;
            String left=AcceptedContextPolicy319.safeWord(raw.substring(0,sep));
            String target=AcceptedContextPolicy319.acceptedWord(raw.substring(sep+1));
            if(left==null||target==null||left.equals(target)||
                    !approvedTarget(target,legacy.words))continue;
            int[] n=grouped.computeIfAbsent(left+":"+target,k->new int[2]);
            n[tap?0:1]=Math.max(n[tap?0:1],count);
        }
        TreeMap<String,ContextBackupFormat322.Counts> contexts=new TreeMap<>();
        for(Map.Entry<String,int[]> entry:grouped.entrySet())
            contexts.put(entry.getKey(),new ContextBackupFormat322.Counts(
                    entry.getValue()[0],entry.getValue()[1]));
        return ContextBackupFormat322.encode(
                legacy.words,legacy.learnedPairs,contexts);
    }
    private static final class V2Plan {
        final Plan legacy;
        final LinkedHashMap<String,Integer> newContext=new LinkedHashMap<>();
        final int added,stronger;
        V2Plan(Plan base,LinkedHashMap<String,Integer> changes,int add,int boosted){
            legacy=base;newContext.putAll(changes);added=add;stronger=boosted;
        }
    }
    private static V2Plan planV2(Context c,ContextBackupFormat322.Data input){
        if(input==null||input.legacy==null||input.contexts==null)
            throw new IllegalArgumentException("Invalid portable file");
        // Validate even synthetic in-memory callers before any mutation.
        ContextBackupFormat322.Data safe=ContextBackupFormat322.decode(
                ContextBackupFormat322.encode(input.legacy.words,
                        input.legacy.learnedPairs,input.contexts));
        Plan legacy=buildPlan(c,safe.legacy);
        List<String> known=new ArrayList<>(safe.legacy.words);
        known.addAll(PersonalDictionary.savedWords(c));
        Map<String,?> current=contextPrefs(c).getAll();
        LinkedHashMap<String,Integer> changes=new LinkedHashMap<>();
        int added=0,strengthened=0;
        for(Map.Entry<String,ContextBackupFormat322.Counts> e:
                safe.contexts.entrySet()){
            String pair=e.getKey();
            String target=pair.substring(pair.indexOf(':')+1);
            if(!approvedTarget(target,known))
                throw new IllegalArgumentException("Unknown context target");
            for(int variant=0;variant<2;variant++){
                int count=variant==0?e.getValue().accepted:e.getValue().used;
                if(count<=0)continue;
                String key=(variant==0?"pair:":"used:")+pair;
                Object prior=current.get(key);
                if(prior!=null && !(prior instanceof Integer))
                    throw new IllegalArgumentException("Invalid existing context");
                int old=prior==null?0:(Integer)prior;
                if(old<0||old>1000)throw new IllegalArgumentException(
                        "Invalid existing context count");
                if(prior==null)added++;
                if(count>old){
                    changes.put(key,count);
                    if(old>0)strengthened++;
                }
            }
        }
        if(current.size()+added>ContextBackupFormat322.MAX_CONTEXT)
            throw new IllegalArgumentException("Context capacity exceeded");
        return new V2Plan(legacy,changes,added,strengthened);
    }
    static Preview previewV2(Context c,ContextBackupFormat322.Data input){
        return planV2(c,input).legacy.preview;
    }
    static Preview importV2Confirmed(Context c,ContextBackupFormat322.Data input){
        V2Plan validated=planV2(c,input);
        // Existing V1 path stays unchanged. Up-front validation prevents
        // rejected V2 targets from partially importing words/corrections.
        Preview result=importConfirmed(c,input.legacy);
        if(!validated.newContext.isEmpty()){
            SharedPreferences.Editor e=contextPrefs(c).edit();
            for(Map.Entry<String,Integer> item:validated.newContext.entrySet())
                e.putInt(item.getKey(),item.getValue());
            if(!e.commit())throw new IllegalStateException("Context save failed");
        }
        return result;
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
