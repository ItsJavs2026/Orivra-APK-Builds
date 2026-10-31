package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Looper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 394: isolate Main Looper synchronous personal-learning lookup cost
 * at the user's 378+386 merged counts. Only fictional records and aggregate
 * durations are used. These are NOT touch-to-pixel Galaxy S26 Ultra readings.
 */
@RunWith(AndroidJUnit4.class)
public final class LoadedLearningLatency394AndroidTest {
    private Context base,context;
    private static final String PREFIX="r394_profile_only_";
    private static final String[] PREFS={
            PersonalLearning.PREFS,PersonalContextLearning319.PREFS,
            PersonalWordFrequency328.PREFS};
    private static final int SAMPLES=900;
    private static class Stats {
        final long[] values;
        final String label;
        Stats(String name,int n){label=name;values=new long[n];}
        String describe(){
            long[] a=values.clone();
            java.util.Arrays.sort(a);
            return String.format(Locale.US,
                    "%s n=%d p50=%.3fms p95=%.3fms p99=%.3fms max=%.3fms",
                    label,a.length,
                    a[(int)Math.ceil(a.length*.50)-1]/1e6,
                    a[(int)Math.ceil(a.length*.95)-1]/1e6,
                    a[(int)Math.ceil(a.length*.99)-1]/1e6,
                    a[a.length-1]/1e6);
        }
    }
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
            assertTrue(base.getSharedPreferences(PREFIX+name,
                    Context.MODE_PRIVATE).edit().clear().commit());
    }
    private static String alphabet(int i){
        return ""+(char)('a'+(i/676)%26)+
                (char)('a'+(i/26)%26)+(char)('a'+i%26);
    }
    @SuppressWarnings("unchecked")
    private static List<String> publicWords() throws Exception {
        Field f=PredictionLite.class.getDeclaredField("WORDS");
        f.setAccessible(true);
        Map<String,Integer> words=(Map<String,Integer>)f.get(null);
        ArrayList<String> keys=new ArrayList<>();
        for(String s:words.keySet())
            if(s!=null && PredictionLite.publicWordForContext(s) &&
                    s.equals(EnglishApostrophePolicy.canonical(s)))
                keys.add(s);
        Collections.sort(keys);
        assertTrue("English dictionary has 326 words",keys.size()>=326);
        return keys;
    }
    private void populateMergedV4Shape() throws Exception {
        List<String> words=publicWords();
        SharedPreferences.Editor correction=context.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE).edit();
        correction.putInt("accepted:teh:the",8);
        for(int i=0;i<303;i++)
            correction.putInt("accepted:typx"+alphabet(i)+":fixx"+alphabet(i),1);
        assertTrue(correction.commit());

        SharedPreferences.Editor frequency=context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE).edit();
        for(int i=0;i<326;i++)
            frequency.putInt("word:"+words.get(i),i%10+1);
        assertTrue(frequency.commit());

        SharedPreferences.Editor contextEntries=context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE).edit();
        for(int i=0;i<242;i++)
            contextEntries.putInt("pair:the:"+words.get(i),i%4+1);
        assertTrue(contextEntries.commit());
        assertEquals(304,PersonalLearning.countEntries(
                context.getSharedPreferences(PersonalLearning.PREFS,Context.MODE_PRIVATE)));
        assertEquals(326,context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE).getAll().size());
        assertEquals(242,context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE).getAll().size());
    }
    private void profile(String prefix,Stats correction,Stats frequency,Stats contextCost){
        assertEquals(Looper.getMainLooper(),Looper.myLooper());
        // Warming public vocabulary separately avoids measuring class load time.
        for(int i=0;i<40;i++){
            PersonalLearning.preferredCorrection(context,"teh");
            PersonalWordFrequency328.matching(context,"th",3);
            PersonalContextLearning319.suggest(context,"the",3);
        }
        for(int i=0;i<correction.values.length;i++){
            long s=System.nanoTime();
            PersonalLearning.preferredCorrection(context,"teh");
            correction.values[i]=System.nanoTime()-s;
            s=System.nanoTime();
            PersonalWordFrequency328.matching(context,"th",3);
            frequency.values[i]=System.nanoTime()-s;
            s=System.nanoTime();
            PersonalContextLearning319.suggest(context,"the",3);
            contextCost.values[i]=System.nanoTime()-s;
        }
    }
    @Test public void measureEmptyVsLoadedLearningSynchronousMainThreadCosts() throws Exception {
        final Stats baseCorrections=new Stats("empty corrections",SAMPLES);
        final Stats baseFreq=new Stats("empty frequency",SAMPLES);
        final Stats baseContext=new Stats("empty context",SAMPLES);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                ()->profile("th",baseCorrections,baseFreq,baseContext));
        populateMergedV4Shape();
        final Stats loadedCorrections=new Stats("304 corrections",SAMPLES);
        final Stats loadedFreq=new Stats("326 frequencies",SAMPLES);
        final Stats loadedContext=new Stats("242 context",SAMPLES);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                ()->profile("th",loadedCorrections,loadedFreq,loadedContext));
        System.out.println("R394 " + baseCorrections.describe());
        System.out.println("R394 " + loadedCorrections.describe());
        System.out.println("R394 " + baseFreq.describe());
        System.out.println("R394 " + loadedFreq.describe());
        System.out.println("R394 " + baseContext.describe());
        System.out.println("R394 " + loadedContext.describe());
        assertEquals("No preferences changed by read-only lookup",
                304,PersonalLearning.countEntries(
                    context.getSharedPreferences(
                        PersonalLearning.PREFS,Context.MODE_PRIVATE)));
    }
    @Test public void repeatedLookupsNeverDiscardMergedLearningRecords() throws Exception {
        populateMergedV4Shape();
        SharedPreferences corrections=context.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        SharedPreferences frequency=context.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        SharedPreferences contexts=context.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        for(int i=0;i<5000;i++){
            PersonalLearning.preferredCorrection(context,i%2==0?"teh":"typxaaa");
            PersonalWordFrequency328.matching(context,i%2==0?"th":"he",3);
            PersonalContextLearning319.suggest(context,"the",3);
        }
        assertEquals(304,PersonalLearning.countEntries(corrections));
        assertEquals(326,frequency.getAll().size());
        assertEquals(242,contexts.getAll().size());
        assertEquals(8,corrections.getInt("accepted:teh:the",0));
    }
    @Test public void newlyLearnedCorrectionIsVisibleWithoutReopeningKeyboard(){
        SharedPreferences p=context.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        assertNull(PersonalLearning.preferredCorrection(context,"teh"));
        assertTrue(p.edit().putInt("accepted:teh:the",8).commit());
        assertEquals("the",PersonalLearning.preferredCorrection(context,"teh"));
        assertTrue(p.edit().putInt("accepted:teh:the",9).commit());
        assertEquals("the",PersonalLearning.preferredCorrection(context,"teh"));
        assertEquals(9,p.getInt("accepted:teh:the",0));
    }
    @Test public void benchmarkRecordsNeverContainActualLearnedWordsOrFiles(){
        Stats s=new Stats("test",4);
        s.values[0]=1000;s.values[1]=2000;s.values[2]=3000;s.values[3]=4000;
        String summary=s.describe();
        assertTrue(summary.contains("p95="));
        assertFalse(summary.contains("accepted:"));
        assertFalse(summary.contains("word:"));
        assertFalse(summary.contains("pair:"));
    }
}
