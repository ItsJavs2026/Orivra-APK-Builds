package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research-only Android tests. The installed Candidate 340 IME never calls
 * this store. No private text is used; the Android app has no INTERNET access.
 */
@RunWith(AndroidJUnit4.class)
public final class TypedWordEvidenceStore341AndroidTest {
    private Context context;
    private boolean originalLearning,originalRemember;

    @Before public void setup(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        originalLearning=PersonalLearning.enabled(context);
        originalRemember=TypedWordsConsent341.enabled(context);
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        PersonalLearning.setEnabled(context,false);
    }
    @After public void teardown(){
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        assertTrue(TypedWordsConsent341.setEnabled(context,originalRemember));
        PersonalLearning.setEnabled(context,originalLearning);
    }
    private TypedSequenceArchive341 trained(int repeats){
        TypedSequenceArchive341 model=new TypedSequenceArchive341();
        for(int n=0;n<repeats;n++){
            model.resetEditor();
            for(String word:List.of("I","need","to","continue"))
                model.observeVerifiedSpace(word,true,true,true,true);
        }
        return model;
    }
    private void enable(){
        PersonalLearning.setEnabled(context,true);
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
    }
    private static void rejected(Runnable action){
        try{action.run();fail("Expected malformed evidence rejection");}
        catch(IllegalArgumentException expected){}
    }
    @Test public void separateOptInDefaultsOffAndCannotBypassPersonalLearning(){
        assertFalse(TypedWordsConsent341.enabled(context));
        TypedSequenceArchive341 sample=trained(3);
        assertFalse(TypedWordEvidenceStore341.persist(context,sample,true));
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
        assertFalse(TypedWordEvidenceStore341.persist(context,sample,true));
        assertFalse(TypedWordsConsent341.mayRemember(context,true));
        PersonalLearning.setEnabled(context,true);
        assertFalse(TypedWordEvidenceStore341.persist(context,sample,false));
        assertEquals(0,TypedWordEvidenceStore341.read(context).associations());
        assertTrue(TypedWordEvidenceStore341.persist(context,sample,true));
        assertEquals(4,TypedWordEvidenceStore341.read(context).confirmedWords());
    }

    @Test public void pendingShortWordsPersistAcrossIndependentEditors(){
        enable();
        for(int n=0;n<3;n++){
            TypedSequenceArchive341 model=TypedWordEvidenceStore341.load(context);
            model.resetEditor();
            for(String word:List.of("I","need","to","continue"))
                model.observeVerifiedSpace(word,true,true,true,true);
            assertTrue(TypedWordEvidenceStore341.persist(context,model,true));
            if(n<2)
                assertEquals(0,TypedWordEvidenceStore341.read(context).confirmedPairs());
        }
        TypedWordEvidenceStore341.Snapshot state=
                TypedWordEvidenceStore341.read(context);
        assertEquals(4,state.confirmedWords());
        assertEquals(3,state.confirmedPairs());
        TypedSequenceArchive341 model=TypedWordEvidenceStore341.load(context);
        assertEquals(List.of("continue"),model.nextWords("to",3));
        assertEquals(3,model.eligiblePairs().get("to continue").intValue());
        assertEquals(3,model.eligibleWords().get("i").intValue());
    }

    @Test public void neverPersistAPrivateEditorOrUnsuccessfulCommit(){
        enable();
        TypedSequenceArchive341 model=TypedWordEvidenceStore341.load(context);
        for(int n=0;n<4;n++){
            model.resetEditor();
            model.observeVerifiedSpace("Secret",true,true,false,true);
            model.observeVerifiedSpace("password",true,true,true,false);
            model.observeVerifiedSpace("unsafe",true,false,true,true);
            model.observeVerifiedSpace("work",false,true,true,true);
        }
        assertTrue(TypedWordEvidenceStore341.persist(context,model,true));
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void disableThenClearCannotResurrectStoredCounts(){
        enable();
        TypedSequenceArchive341 sample=trained(3);
        assertTrue(TypedWordEvidenceStore341.persist(context,sample,true));
        TypedWordEvidenceStore341.Snapshot before=
                TypedWordEvidenceStore341.read(context);
        assertTrue(before.confirmedPairs()>0);
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertFalse(TypedWordEvidenceStore341.persist(context,sample,true));
        assertEquals(before.vocabulary(),
                TypedWordEvidenceStore341.read(context).vocabulary());
        String originalV3=LearningTransferStore.exportV3(context);
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
        assertEquals(originalV3,LearningTransferStore.exportV3(context));
        assertFalse(TypedWordEvidenceStore341.persist(context,sample,true));
        assertEquals(0,TypedWordEvidenceStore341.read(context).associations());
    }

    @Test public void corruptStoredEvidenceMustFailWithoutSilentRewrite(){
        enable();
        TypedSequenceArchive341 sample=trained(3);
        assertTrue(TypedWordEvidenceStore341.persist(context,sample,true));
        context.getSharedPreferences(TypedWordEvidenceStore341.PREFS,
                Context.MODE_PRIVATE).edit().putInt("p:orphan phrase",3).commit();
        rejected(()->TypedWordEvidenceStore341.read(context));
        rejected(()->TypedWordEvidenceStore341.persist(context,sample,true));
        assertTrue(context.getSharedPreferences(TypedWordEvidenceStore341.PREFS,
                Context.MODE_PRIVATE).contains("p:orphan phrase"));
    }

    @Test public void noTypedVocabularyIOOnKeyboardMainThread(){
        enable();
        AtomicBoolean rejectedRead=new AtomicBoolean(false);
        AtomicBoolean rejectedWrite=new AtomicBoolean(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try{TypedWordEvidenceStore341.load(context);}
            catch(IllegalStateException expected){rejectedRead.set(true);}
            try{TypedWordEvidenceStore341.persist(context,trained(3),true);}
            catch(IllegalStateException expected){rejectedWrite.set(true);}
        });
        assertTrue(rejectedRead.get());
        assertTrue(rejectedWrite.get());
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void noOrphanPairFromStoreCapacityOverflow(){
        TypedSequenceArchive341 model=new TypedSequenceArchive341();
        Map<String,Integer> full=new java.util.HashMap<>();
        for(int a=0;a<TypedSequenceArchive341.MAX_WORDS;a++){
            int i=a;
            // 192 distinct canonical alphabetic words, no number IDs.
            StringBuilder word=new StringBuilder("test");
            word.append((char)('a'+((i/26)%26)));
            word.append((char)('a'+(i%26)));
            full.put(word.toString(),1);
        }
        model.restoreRawEvidence(full,Map.of());
        model.observeVerifiedSpace("unrecordable",true,true,true,true);
        model.observeVerifiedSpace("testaa",true,true,true,true);
        assertTrue(model.rawPairEvidence().isEmpty());
        assertEquals(TypedSequenceArchive341.MAX_WORDS,
                model.observedVocabularyCount());
    }
}
