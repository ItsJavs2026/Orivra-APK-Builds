package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Looper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Entirely fictional records. Main-looper timing and strict pre-395 output
 * reference share the same isolated in-memory Android preferences. */
@RunWith(AndroidJUnit4.class)
public final class FastStoredLearning395AndroidTest {
    private Context base,context;
    private static final String PREFIX="r395_test_only_";
    private static final String[] PREFS={
            PersonalLearning.PREFS,PersonalContextLearning319.PREFS,
            PersonalWordFrequency328.PREFS};
    private static final int SAMPLES=500;
    @Before public void before(){
        base=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context=new ContextWrapper(base){
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                return base.getSharedPreferences(PREFIX+name,mode);
            }
            @Override public Context getApplicationContext(){return this;}
        };
        clear();
        PersonalLearning.setEnabled(context,true);
    }
    @After public void after(){clear();}
    private void clear(){
        for(String name:PREFS)
            assertTrue(base.getSharedPreferences(PREFIX+name,Context.MODE_PRIVATE)
                    .edit().clear().commit());
    }
    private static String randomToken(Random r){
        final String chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'' 1!-’";
        int length=r.nextInt(30);
        StringBuilder out=new StringBuilder(length);
        for(int i=0;i<length;i++)out.append(chars.charAt(r.nextInt(chars.length())));
        return out.toString();
    }
    @Test public void storedContextPredicateEqualsOriginalStrictValidator(){
        Random r=new Random(395011L);
        for(int i=0;i<60000;i++){
            String token=randomToken(r);
            String old=AcceptedContextPolicy319.acceptedWord(token);
            boolean expected=token.equals(old);
            assertEquals("Context canonical parity at "+i,expected,
                    FastStoredLearningPolicy395.canonicalContextTarget(token));
        }
        for(String token:List.of("hello","it's","a'a","ab'cd","the","we're")){
            assertEquals(token.equals(AcceptedContextPolicy319.acceptedWord(token)),
                    FastStoredLearningPolicy395.canonicalContextTarget(token));
        }
    }
    @Test public void fastFilterNeverRejectsOriginalEligibleFrequencyWord(){
        Random r=new Random(395022L);
        String[] prefixes={"a","th","the","co","con","we'","we","you","i","t","ne"};
        for(int i=0;i<60000;i++){
            String prefix=prefixes[i%prefixes.length];
            String word=i%5==0?prefix+"thing":randomToken(r);
            int uses=1+r.nextInt(12);
            if(FastStoredLearningPolicy395.definitelyNotPrefix(prefix,word))
                assertFalse("Must never discard an eligible word "+i,
                        AdaptivePrefixPolicy328.eligible(prefix,word,uses));
        }
    }
    @SuppressWarnings("unchecked")
    private static List<String> publicWords() throws Exception {
        Field f=PredictionLite.class.getDeclaredField("WORDS");
        f.setAccessible(true);
        Map<String,Integer> words=(Map<String,Integer>)f.get(null);
        ArrayList<String> out=new ArrayList<>();
        for(String word:words.keySet())if(word!=null &&
                PredictionLite.publicWordForContext(word) &&
                word.equals(EnglishApostrophePolicy.canonical(word)))
            out.add(word);
        out.sort(String::compareTo);
        assertTrue(out.size()>=326);
        return out;
    }
    private void loadStore() throws Exception {
        List<String> words=publicWords();
        SharedPreferences.Editor corrections=context.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE).edit();
        corrections.putInt("accepted:teh:the",8);
        for(int i=0;i<303;i++)
            corrections.putInt("accepted:typx"+i+":fixx"+i,1);
        assertTrue(corrections.commit());
        SharedPreferences.Editor frequency=context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE).edit();
        for(int i=0;i<326;i++)frequency.putInt("word:"+words.get(i),i%10+1);
        assertTrue(frequency.commit());
        SharedPreferences.Editor contexts=context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE).edit();
        for(int i=0;i<242;i++)
            contexts.putInt("pair:the:"+words.get(i),i%4+1);
        assertTrue(contexts.commit());
        assertEquals(304,PersonalLearning.countEntries(
                context.getSharedPreferences(PersonalLearning.PREFS,Context.MODE_PRIVATE)));
        assertEquals(326,context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE).getAll().size());
        assertEquals(242,context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE).getAll().size());
    }

    /** Exact original Candidate 391 method body, no fast predicate. */
    private static List<String> legacyFrequency(Context c,String prefix,int limit){
        if(c==null || !PersonalLearning.enabled(c) || limit<=0)return List.of();
        String raw=EnglishApostrophePolicy.prefix(prefix);
        if(raw==null)return List.of();
        Map<String,Integer> counts=new HashMap<>();
        Set<String> personalWords=null;
        SharedPreferences p=c.getSharedPreferences(PersonalWordFrequency328.PREFS,
                Context.MODE_PRIVATE);
        for(Map.Entry<String,?> e:p.getAll().entrySet()){
            if(!e.getKey().startsWith("word:") || !(e.getValue() instanceof Integer))
                continue;
            String word=e.getKey().substring("word:".length());
            int score=(Integer)e.getValue();
            if(score<1 || score>1000 ||
                    !AdaptivePrefixPolicy328.eligible(raw,word,score))
                continue;
            if(PredictionLite.publicWordForContext(word)){
                counts.put(word,score);
            } else {
                if(personalWords==null){
                    personalWords=new HashSet<>();
                    for(String saved:PersonalDictionary.savedWords(c)){
                        String canonical=EnglishApostrophePolicy.canonical(saved);
                        if(canonical!=null)personalWords.add(canonical);
                    }
                }
                if(personalWords.contains(word))counts.put(word,score);
            }
        }
        return AdaptivePrefixPolicy328.candidates(prefix,counts,limit);
    }
    private static boolean legacyPublicContextTarget(String word){
        return PredictionLite.publicWordForContext(word) ||
                (word.indexOf('\'')>=0 &&
                 PredictionLite.publicWordForContext(word.replace("'","")));
    }
    /** Exact original Candidate 391 context suggestion method body. */
    private static List<String> legacyContext(Context c,String previous,int limit){
        String left=AcceptedContextPolicy319.safeWord(previous);
        if(c==null || !PersonalLearning.enabled(c) || left==null || limit<=0)
            return List.of();
        SharedPreferences prefs=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        String acceptedPrefix="pair:"+left+":",usedPrefix="used:"+left+":";
        HashMap<String,int[]> evidence=new HashMap<>();
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
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
        Set<String> personalWords=null;
        for(Map.Entry<String,int[]> item:evidence.entrySet()){
            int[] values=item.getValue();
            if(!AdaptiveUsagePolicy321.suggestable(values[0],values[1]))continue;
            String target=item.getKey();
            if(!legacyPublicContextTarget(target)){
                if(personalWords==null){
                    personalWords=new HashSet<>();
                    for(String word:PersonalDictionary.savedWords(c)){
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
        for(Map.Entry<String,Integer> e:sorted){
            if(result.size()>=Math.min(limit,5))break;
            result.add(e.getKey());
        }
        return result;
    }
    @Test public void sameSuggestionsOnLoadedStoreAndMalformedLegacyKeys() throws Exception {
        loadStore();
        SharedPreferences frequency=context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        SharedPreferences contexts=context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        assertTrue(frequency.edit().putInt("word:ThE",8)
                .putInt("word:we’re",9).putInt("word:invalid value",3)
                .putInt("word:a''b",9).commit());
        assertTrue(contexts.edit().putInt("pair:the:Unverified",10)
                .putInt("pair:the:a''b",10).putInt("used:the:WE",10)
                .commit());
        for(String prefix:List.of("t","th","he","co","con","we'","we","i",
                "the","keyboard","a","ne","zzz")){
            assertEquals("Prefix "+prefix,legacyFrequency(context,prefix,3),
                    PersonalWordFrequency328.matching(context,prefix,3));
        }
        for(String prev:List.of("the","you","my","i","please","this",
                "go","anything","we")){
            assertEquals("Context "+prev,legacyContext(context,prev,3),
                    PersonalContextLearning319.suggest(context,prev,3));
        }
    }
    private static long p95(long[] timings){
        long[] arr=timings.clone();
        Arrays.sort(arr);
        return arr[(int)Math.ceil(arr.length*.95)-1];
    }
    @Test public void sameRunnerMainThreadLoadedLookupP95() throws Exception {
        loadStore();
        final long[] oldFreq=new long[SAMPLES],newFreq=new long[SAMPLES];
        final long[] oldCtx=new long[SAMPLES],newCtx=new long[SAMPLES];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            assertEquals(Looper.getMainLooper(),Looper.myLooper());
            for(int k=0;k<25;k++){
                legacyFrequency(context,"th",3);
                PersonalWordFrequency328.matching(context,"th",3);
                legacyContext(context,"the",3);
                PersonalContextLearning319.suggest(context,"the",3);
            }
            for(int i=0;i<SAMPLES;i++){
                long t=System.nanoTime();
                List<String> oldF=legacyFrequency(context,"th",3);
                oldFreq[i]=System.nanoTime()-t;
                t=System.nanoTime();
                List<String> newF=PersonalWordFrequency328.matching(context,"th",3);
                newFreq[i]=System.nanoTime()-t;
                assertEquals(oldF,newF);
                t=System.nanoTime();
                List<String> oldC=legacyContext(context,"the",3);
                oldCtx[i]=System.nanoTime()-t;
                t=System.nanoTime();
                List<String> newC=PersonalContextLearning319.suggest(context,"the",3);
                newCtx[i]=System.nanoTime()-t;
                assertEquals(oldC,newC);
            }
        });
        android.util.Log.i("OrivraPerf395",String.format(Locale.US,
                "R395 frequency SAME-RUN p95 old=%.3fms new=%.3fms n=%d",
                p95(oldFreq)/1e6,p95(newFreq)/1e6,SAMPLES));
        android.util.Log.i("OrivraPerf395",String.format(Locale.US,
                "R395 context SAME-RUN p95 old=%.3fms new=%.3fms n=%d",
                p95(oldCtx)/1e6,p95(newCtx)/1e6,SAMPLES));
        assertTrue("All reference vs fast results identical",SAMPLES>400);
    }
    @Test public void immediateLearningUpdatesStayVisibleWithoutAnyCache(){
        SharedPreferences p=context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        assertFalse(PersonalWordFrequency328.matching(context,"th",3).contains("the"));
        assertTrue(p.edit().putInt("word:the",12).commit());
        assertTrue(PersonalWordFrequency328.matching(context,"th",3).contains("the"));
        SharedPreferences ctx=context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        assertFalse(PersonalContextLearning319.suggest(context,"the",3).contains("help"));
        assertTrue(ctx.edit().putInt("pair:the:help",10).commit());
        assertTrue(PersonalContextLearning319.suggest(context,"the",3).contains("help"));
    }
    @Test public void disabledPersonalLearningAlwaysReturnsNoLearnedCandidates(){
        assertTrue(context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE)
                .edit().putInt("word:the",10).commit());
        PersonalLearning.setEnabled(context,false);
        assertTrue(PersonalWordFrequency328.matching(context,"th",3).isEmpty());
        assertTrue(PersonalContextLearning319.suggest(context,"the",3).isEmpty());
    }
}
