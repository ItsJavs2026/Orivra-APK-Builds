package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research 349: snapshot validation still covers all existing evidence,
 * while each successful Space mutates only the one changed word and pair.
 * This test never reads real users' text. Synthetic alphabetic words only.
 */
@RunWith(AndroidJUnit4.class)
public final class TypedWordDelta349AndroidTest {
    private Context ctx;
    private boolean oldLearning,oldConsent;
    private SharedPreferences data;
    @Before public void start(){
        ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        oldLearning=PersonalLearning.enabled(ctx);
        oldConsent=TypedWordsConsent341.enabled(ctx);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(ctx));
        PersonalLearning.setEnabled(ctx,true);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,true));
        data=ctx.getSharedPreferences(TypedWordEvidenceStore341.PREFS,Context.MODE_PRIVATE);
    }
    @After public void finish(){
        assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(ctx));
        PersonalLearning.setEnabled(ctx,oldLearning);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,oldConsent));
    }
    private TypedWordBoundaryStore346.Result word(String next,String prior){
        return TypedWordBoundaryStore346.record(ctx,next,prior,true,true,
                TypedWordsConsent341.generation(ctx),TypedWordsConsent341.liveRevision());
    }
    private String synthetic(int n){
        return "test"+(char)('a'+(n/26)%26)+(char)('a'+n%26);
    }

    @Test public void firstVerifiedSpaceCreatesExactlyOneKey(){
        assertTrue(word("continue",null).stored);
        assertEquals(Map.of("w:continue",1),data.getAll());
        assertEquals(1,TypedWordEvidenceStore341.read(ctx).vocabulary());
        assertEquals(0,TypedWordEvidenceStore341.read(ctx).associations());
    }
    @Test public void verifiedPairChangesOnlyWordAndPairEntries(){
        assertTrue(data.edit().putInt("w:to",3).putInt("w:continue",2).commit());
        Map<String,?> before=data.getAll();
        Set<String> changed=Collections.synchronizedSet(new HashSet<>());
        SharedPreferences.OnSharedPreferenceChangeListener listener=
                (prefs,key)->changed.add(key==null?"NULL_KEY":key);
        data.registerOnSharedPreferenceChangeListener(listener);
        try {
            assertTrue(word("continue","to").stored);
        }finally {data.unregisterOnSharedPreferenceChangeListener(listener);}
        assertEquals(3,data.getInt("w:to",0));
        assertEquals(3,data.getInt("w:continue",0));
        assertEquals(1,data.getInt("p:to continue",0));
        assertEquals(before.get("w:to"),data.getAll().get("w:to"));
        assertEquals(Set.of("w:continue","p:to continue"),changed);
    }
    @Test public void largeVocabularyIsNotClearedAndRepopulated(){
        SharedPreferences.Editor e=data.edit();
        for(int n=0;n<170;n++)e.putInt("w:"+synthetic(n),1);
        e.putInt("w:to",3);
        assertTrue(e.commit());
        Map<String,?> before=data.getAll();
        Set<String> changed=Collections.synchronizedSet(new HashSet<>());
        SharedPreferences.OnSharedPreferenceChangeListener listener=
                (prefs,key)->changed.add(key==null?"NULL_KEY":key);
        data.registerOnSharedPreferenceChangeListener(listener);
        try {
            assertTrue(word("continue","to").stored);
        }finally {data.unregisterOnSharedPreferenceChangeListener(listener);}
        assertEquals(before.size()+2,data.getAll().size());
        for(Map.Entry<String,?> entry:before.entrySet())
            assertEquals(entry.getValue(),data.getAll().get(entry.getKey()));
        assertEquals(Set.of("w:continue","p:to continue"),changed);
        assertEquals(172,TypedWordEvidenceStore341.read(ctx).vocabulary());
    }
    @Test public void previousPairsAndCountsSurviveNewWrites(){
        assertTrue(data.edit().putInt("w:i",6).putInt("w:need",4)
                .putInt("w:to",5).putInt("w:continue",3)
                .putInt("p:i need",4).putInt("p:need to",4)
                .putInt("p:to continue",3).commit());
        assertTrue(word("continue","to").stored);
        TypedWordEvidenceStore341.Snapshot state=TypedWordEvidenceStore341.read(ctx);
        assertEquals(4,state.words.get("continue").intValue());
        assertEquals(4,state.pairs.get("to continue").intValue());
        assertEquals(4,state.pairs.get("i need").intValue());
        assertEquals(4,state.pairs.get("need to").intValue());
    }
    @Test public void saturatedWordAndPairDoNotNeedPreferenceWrite(){
        assertTrue(data.edit().putInt("w:to",1000)
                .putInt("w:continue",1000)
                .putInt("p:to continue",1000).commit());
        Set<String> changed=Collections.synchronizedSet(new HashSet<>());
        SharedPreferences.OnSharedPreferenceChangeListener listener=
                (prefs,key)->changed.add(key==null?"NULL_KEY":key);
        data.registerOnSharedPreferenceChangeListener(listener);
        try{
            assertTrue(word("continue","to").stored);
        }finally{data.unregisterOnSharedPreferenceChangeListener(listener);}
        assertTrue("unchanged max-count evidence never emits a mutation",changed.isEmpty());
        assertEquals(1000,data.getInt("p:to continue",0));
    }
    @Test public void staleRevisionNeverTouchesEvidenceAfterRevocation(){
        long oldEpoch=TypedWordsConsent341.generation(ctx);
        long oldRevision=TypedWordsConsent341.liveRevision();
        PersonalLearning.setEnabled(ctx,false);
        PersonalLearning.setEnabled(ctx,true);
        assertFalse(TypedWordBoundaryStore346.record(ctx,"continue",null,
                true,true,oldEpoch,oldRevision).stored);
        assertTrue(data.getAll().isEmpty());
    }
    @Test public void corruptExistingStateIsNeverOverwritten(){
        assertTrue(data.edit().putInt("w:continue",1)
                .putInt("p:unknown missing",2).commit());
        Map<String,?> before=data.getAll();
        try{
            word("continue",null);
            fail("Malformed old evidence must be rejected");
        }catch(IllegalArgumentException expected){}
        assertEquals(before,data.getAll());
    }
    @Test public void concurrentWritersNeverLoseCounts()throws Exception {
        assertTrue(data.edit().putInt("w:to",60).commit());
        long epoch=TypedWordsConsent341.generation(ctx);
        long rev=TypedWordsConsent341.liveRevision();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            ArrayList<Future<?>> futures=new ArrayList<>();
            for(int t=0;t<2;t++)futures.add(pool.submit(()->{
                for(int i=0;i<15;i++) {
                    assertTrue(TypedWordBoundaryStore346.record(ctx,"continue",
                            "to",true,true,epoch,rev).stored);
                }
            }));
            for(Future<?> future:futures)future.get(20,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        TypedWordEvidenceStore341.Snapshot result=TypedWordEvidenceStore341.read(ctx);
        assertEquals(30,result.words.get("continue").intValue());
        assertEquals(30,result.pairs.get("to continue").intValue());
        assertEquals(60,result.words.get("to").intValue());
    }
    @Test public void oneFileV4IncludesNewDeltaCounts(){
        assertTrue(word("to",null).stored);
        assertTrue(word("continue","to").stored);
        assertTrue(word("to",null).stored);
        assertTrue(word("continue","to").stored);
        String backup=TypedLearningV4Transfer347.exportV4(ctx);
        TypedLearningV4Codec341.Data restored=TypedLearningV4Codec341.decode(backup);
        assertEquals(2,restored.words.get("to").intValue());
        assertEquals(2,restored.words.get("continue").intValue());
        assertEquals(2,restored.pairs.get("to continue").intValue());
    }
}
