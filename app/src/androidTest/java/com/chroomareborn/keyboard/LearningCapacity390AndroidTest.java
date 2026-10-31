package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Entirely fictional V4 files; isolated test-only preferences.
 * Tests the exact 378 + 386 union shape without reading user documents.
 */
@RunWith(AndroidJUnit4.class)
public final class LearningCapacity390AndroidTest {
    private static final String PREFIX="r390_test_only_";
    private static final String[] PREFS={
        "orivra_manual_dictionary_v2",
        PersonalLearning.PREFS,
        PersonalContextLearning319.PREFS,
        PersonalWordFrequency328.PREFS,
        TypedWordEvidenceStore341.PREFS,
        TypedWordsConsent341.PREFS
    };
    private Context base,isolated;

    @Before public void before(){
        base=InstrumentationRegistry.getInstrumentation().getTargetContext();
        isolated=new ContextWrapper(base){
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                return base.getSharedPreferences(PREFIX+name,mode);
            }
            @Override public Context getApplicationContext(){return this;}
        };
        wipeTestPreferences();
    }
    @After public void after(){wipeTestPreferences();}
    private void wipeTestPreferences(){
        for(String name:PREFS)
            assertTrue(base.getSharedPreferences(PREFIX+name,Context.MODE_PRIVATE)
                    .edit().clear().commit());
    }
    private static String suffix(int id){
        return ""+(char)('a'+(id/676)%26)+
                (char)('a'+(id/26)%26)+(char)('a'+id%26);
    }
    private static String correctionKey(int index){
        return "typed"+suffix(index)+":fixed"+suffix(index);
    }
    private static TreeMap<String,Integer> corrections(int start,int end,int n){
        TreeMap<String,Integer> map=new TreeMap<>();
        for(int i=start;i<end;i++)map.put(correctionKey(i),n);
        return map;
    }
    @SuppressWarnings("unchecked")
    private static List<String> publicWords() throws Exception {
        Field field=PredictionLite.class.getDeclaredField("WORDS");
        field.setAccessible(true);
        Map<String,Integer> dictionary=(Map<String,Integer>)field.get(null);
        List<String> words=new ArrayList<>();
        for(String word:dictionary.keySet())
            if(word!=null && word.equals(EnglishApostrophePolicy.canonical(word)) &&
                    PredictionLite.publicWordForContext(word))
                words.add(word);
        Collections.sort(words);
        assertTrue("Public dictionary must supply at least 326 unique words",
                words.size()>=326);
        return words;
    }
    @SuppressWarnings("unchecked")
    private static List<String> safePreviousWords() throws Exception {
        Field field=AcceptedContextPolicy319.class.getDeclaredField("SAFE");
        field.setAccessible(true);
        List<String> words=new ArrayList<>((Set<String>)field.get(null));
        Collections.sort(words);
        assertTrue(words.size()>=40);
        return words;
    }
    private static List<String> contextKeys(List<String> publicWords,
            List<String> priorWords) {
        ArrayList<String> result=new ArrayList<>();
        for(String target:publicWords) {
            if(AcceptedContextPolicy319.acceptedWord(target)==null)continue;
            for(String previous:priorWords) {
                if(previous.equals(target) ||
                        AcceptedContextPolicy319.safeWord(previous)==null)continue;
                result.add(previous+":"+target);
                if(result.size()==242)return result;
            }
        }
        throw new AssertionError("Not enough approved context keys");
    }
    private static Map<String,ContextBackupFormat322.Counts> contexts(
            List<String> keys,int from,int end) {
        TreeMap<String,ContextBackupFormat322.Counts> result=new TreeMap<>();
        for(int i=from;i<end;i++)
            result.put(keys.get(i),new ContextBackupFormat322.Counts(1,0));
        return result;
    }
    private static Map<String,Integer> frequencies(
            List<String> words,int from,int end,int count){
        TreeMap<String,Integer> result=new TreeMap<>();
        for(int i=from;i<end;i++)result.put(words.get(i),count);
        return result;
    }
    private static Map<String,Integer> typedWords(){
        TreeMap<String,Integer> words=new TreeMap<>();
        for(int i=0;i<58;i++)words.put("saved"+suffix(i),3);
        return words;
    }
    private static Map<String,Integer> typedPairs(){
        TreeMap<String,Integer> pairs=new TreeMap<>();
        for(int i=0;i<30;i++)
            pairs.put("saved"+suffix(i)+" saved"+suffix(i+1),1);
        return pairs;
    }
    private static String v4(Map<String,Integer> corrections,
            Map<String,ContextBackupFormat322.Counts> ctx,
            Map<String,Integer> wordFrequency,
            Map<String,Integer> typed,Map<String,Integer> typedPairs){
        ContextBackupFormat322.Data v2=ContextBackupFormat322.decode(
                ContextBackupFormat322.encode(
                        List.of(),corrections,ctx));
        String v3=FrequencyBackupFormat330.encode(v2,wordFrequency);
        return TypedLearningV4Codec341.encode(v3,typed,typedPairs);
    }
    private static final class PairOfBackups {
        final String old378,new386;
        PairOfBackups(String a,String b){old378=a;new386=b;}
    }
    private static PairOfBackups fixture() throws Exception {
        List<String> words=publicWords();
        List<String> context=contextKeys(words,safePreviousWords());
        String old=v4(corrections(0,300,1),
                contexts(context,0,240),frequencies(words,0,320,1),
                typedWords(),typedPairs());
        // Six overlapping corrections, four new; nineteen overlapping
        // frequencies, six new; two entirely new context keys.
        String newer=v4(corrections(294,304,3),
                contexts(context,240,242),frequencies(words,301,326,4),
                Map.of(),Map.of());
        assertTrue(old.length()<100000 && newer.length()<100000);
        return new PairOfBackups(old,newer);
    }
    private void importFile(String raw){
        TypedLearningV4Codec341.Data parsed=TypedLearningV4Codec341.decode(raw);
        TypedLearningV4Transfer347.preview(isolated,parsed);
        TypedLearningV4Transfer347.importConfirmed(isolated,parsed);
    }
    private TypedLearningV4Codec341.Data exported(){
        return TypedLearningV4Codec341.decode(
                TypedLearningV4Transfer347.exportV4(isolated));
    }
    private void assertCombined(){
        TypedLearningV4Codec341.Data file=exported();
        assertEquals(304,file.previous.earlier.legacy.learnedPairs.size());
        assertEquals(242,file.previous.earlier.contexts.size());
        assertEquals(326,file.previous.frequencies.size());
        assertEquals(58,file.words.size());
        assertEquals(30,file.pairs.size());
        assertEquals(3,file.previous.earlier.legacy.learnedPairs.get(
                correctionKey(294)).intValue());
        assertEquals(1,file.previous.earlier.legacy.learnedPairs.get(
                correctionKey(0)).intValue());
        assertEquals(3,file.words.get("saved"+suffix(0)).intValue());
        assertEquals(1,file.pairs.get(
                "saved"+suffix(0)+" saved"+suffix(1)).intValue());
        assertEquals(304,PersonalLearning.countEntries(
                isolated.getSharedPreferences(PersonalLearning.PREFS,Context.MODE_PRIVATE)));
        assertEquals(242,isolated.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE).getAll().size());
        assertEquals(326,isolated.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE).getAll().size());
        assertFalse(PersonalLearning.enabled(isolated));
        assertFalse(TypedWordsConsent341.enabled(isolated));
    }

    @Test public void importing378Then386KeepsExactUnionAndTypedEvidence() throws Exception {
        PairOfBackups f=fixture();
        importFile(f.old378);
        assertEquals(300,exported().previous.earlier.legacy.learnedPairs.size());
        importFile(f.new386);
        assertCombined();
    }
    @Test public void importing386Then378HasSameUnionAndStrongerCounts() throws Exception {
        PairOfBackups f=fixture();
        importFile(f.new386);
        importFile(f.old378);
        assertCombined();
    }
    @Test public void repeatedImportCannotDuplicateOrDowngradeRecords() throws Exception {
        PairOfBackups f=fixture();
        importFile(f.new386);
        importFile(f.old378);
        String merged=TypedLearningV4Transfer347.exportV4(isolated);
        importFile(f.new386);
        importFile(f.old378);
        assertEquals(merged,TypedLearningV4Transfer347.exportV4(isolated));
        assertCombined();
    }
    @Test public void previewNeverMutatesExisting378Or386Records() throws Exception {
        PairOfBackups f=fixture();
        importFile(f.new386);
        String before=TypedLearningV4Transfer347.exportV4(isolated);
        TypedLearningV4Transfer347.preview(isolated,
                TypedLearningV4Codec341.decode(f.old378));
        assertEquals(before,TypedLearningV4Transfer347.exportV4(isolated));
    }
    @Test public void beyondCorrectionCeilingRejectsWithoutChangingStore() throws Exception {
        importFile(v4(corrections(0,384,1),Map.of(),Map.of(),Map.of(),Map.of()));
        String before=TypedLearningV4Transfer347.exportV4(isolated);
        String oneMore=v4(corrections(384,385,1),
                Map.of(),Map.of(),Map.of(),Map.of());
        try{
            TypedLearningV4Transfer347.preview(isolated,
                    TypedLearningV4Codec341.decode(oneMore));
            fail("Overflow must be rejected by preflight");
        }catch(IllegalArgumentException expected){}
        assertEquals(before,TypedLearningV4Transfer347.exportV4(isolated));
    }
    @Test public void newLimitsAreConsistentAndStillBounded(){
        assertEquals(384,LearningCapacity390.MAX_CORRECTIONS);
        assertEquals(320,LearningCapacity390.MAX_CONTEXT_KEYS);
        assertEquals(400,LearningCapacity390.MAX_FREQUENCIES);
        assertEquals(LearningCapacity390.MAX_CORRECTIONS,LearningTransferCodec.MAX_PAIRS);
        assertEquals(LearningCapacity390.MAX_CONTEXT_KEYS,ContextBackupFormat322.MAX_CONTEXT);
        assertEquals(LearningCapacity390.MAX_FREQUENCIES,FrequencyBackupFormat330.MAX_WORDS);
        assertTrue(LearningCapacity390.fits(304,242,326));
        assertFalse(LearningCapacity390.fits(385,242,326));
        assertFalse(LearningCapacity390.fits(304,321,326));
        assertFalse(LearningCapacity390.fits(304,242,401));
    }
}
