package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Worker-qualified only: NOT a released typing feature. Uses synthetic words,
 * never records screenshots, private editor contents, device IDs or logs.
 */
@RunWith(AndroidJUnit4.class)
public final class TypedWordLearningWorker346AndroidTest {
    private Context context;
    private boolean previousLearning,previousConsent;

    @Before public void before() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        previousLearning=PersonalLearning.enabled(context);
        previousConsent=TypedWordsConsent341.enabled(context);
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        PersonalLearning.setEnabled(context,false);
    }
    @After public void after() {
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        PersonalLearning.setEnabled(context,previousLearning);
        assertTrue(TypedWordsConsent341.setEnabled(context,previousConsent));
    }
    private void enable() {
        PersonalLearning.setEnabled(context,true);
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
    }
    private static void idle(TypedWordLearningWorker346 w)throws Exception {
        w.workerIdleForTest().get(12,TimeUnit.SECONDS);
    }
    private void typePhrase(TypedWordLearningWorker346 w,long session)
            throws Exception {
        w.beginEditor(session,true);
        idle(w);
        for(String word:List.of("I","need","to","continue")) {
            w.committedSpace(word,session,true,true);
        }
        idle(w);
        w.endEditor(session);
        idle(w);
    }

    @Test public void disabledByDefaultAndExistingLearningRequired()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try {
            w.beginEditor(1,true);idle(w);
            w.committedSpace("continue",1,true,true);idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
            assertTrue(TypedWordsConsent341.setEnabled(context,true));
            w.beginEditor(2,true);idle(w);
            w.committedSpace("continue",2,true,true);idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
            PersonalLearning.setEnabled(context,true);
            w.beginEditor(3,true);idle(w);
            w.committedSpace("continue",3,true,true);idle(w);
            assertEquals(1,TypedWordEvidenceStore341.read(context).vocabulary());
        }finally{w.close();}
    }

    @Test public void shortWordsAndContinuationsSurviveThreeIndependentEditors()
            throws Exception {
        enable();
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try {
            for(int n=1;n<=3;n++)typePhrase(w,n);
            TypedWordEvidenceStore341.Snapshot state=
                    TypedWordEvidenceStore341.read(context);
            assertEquals(4,state.confirmedWords());
            assertEquals(3,state.confirmedPairs());
            assertEquals(3,state.words.get("i").intValue());
            assertEquals(3,state.words.get("continue").intValue());
            w.beginEditor(4,true);idle(w);
            assertEquals(List.of("continue"),w.nextWords("to",3,4));
            assertEquals(List.of("continue"),w.completedWords("cont",3,4));
            assertEquals(List.of(),w.nextWords("to",3,3));
            assertEquals(4,w.visibleWordCount(4));
        }finally{w.close();}
    }

    @Test public void privateEditorFailedCommitAndNewSessionDoNotFormPairs()
            throws Exception {
        enable();
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try{
            w.beginEditor(11,false);idle(w);
            w.committedSpace("private",11,false,true);
            w.committedSpace("word",11,false,true);
            idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
            w.endEditor(11);idle(w);

            w.beginEditor(12,true);idle(w);
            w.committedSpace("apple",12,true,true);
            w.committedSpace("broken",12,true,false);
            w.committedSpace("banana",12,true,true);
            idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).associations());
            w.endEditor(12);idle(w);

            w.beginEditor(13,true);idle(w);
            w.committedSpace("cherry",13,true,true);
            idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).associations());
            assertEquals(3,TypedWordEvidenceStore341.read(context).vocabulary());
        }finally{w.close();}
    }

    @Test public void consentEpochPreventsRepopulationAfterEraseAndReEnable()
            throws Exception {
        enable();
        long oldEpoch=TypedWordsConsent341.generation(context);
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
        long newEpoch=TypedWordsConsent341.generation(context);
        assertTrue(newEpoch!=oldEpoch);
        TypedWordBoundaryStore346.Result rejected=TypedWordBoundaryStore346
                .record(context,"continue",null,true,true,oldEpoch);
        assertFalse(rejected.stored);
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
        TypedWordBoundaryStore346.Result accepted=TypedWordBoundaryStore346
                .record(context,"continue",null,true,true,newEpoch);
        assertTrue(accepted.stored);
        assertEquals(1,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void twoSimultaneousControllersDoNotLoseVerifiedCounts()
            throws Exception {
        enable();
        TypedWordLearningWorker346 a=new TypedWordLearningWorker346(context);
        TypedWordLearningWorker346 b=new TypedWordLearningWorker346(context);
        try{
            a.beginEditor(25,true);
            b.beginEditor(26,true);
            idle(a);idle(b);
            for(int n=0;n<3;n++){
                a.committedSpace("continue",25,true,true);
                b.committedSpace("continue",26,true,true);
            }
            idle(a);idle(b);
            TypedWordEvidenceStore341.Snapshot snap=
                    TypedWordEvidenceStore341.read(context);
            assertEquals(6,snap.words.get("continue").intValue());
        }finally{a.close();b.close();}
    }

    @Test public void keyboardUiThreadNeverCommitsTypedWords()
            throws Exception {
        enable();
        AtomicBoolean protectedUi=new AtomicBoolean(false);
        long epoch=TypedWordsConsent341.generation(context);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                TypedWordBoundaryStore346.record(context,"continue",null,
                        true,true,epoch);
            } catch(IllegalStateException expected) {
                protectedUi.set(true);
            }
        });
        assertTrue(protectedUi.get());
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());

        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try{
            w.beginEditor(27,true);idle(w);
            // The real IME would enqueue a short verified word on the UI;
            // only its worker is allowed to touch private storage.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    ()->w.committedSpace("continue",27,true,true));
            idle(w);
            assertEquals(1,TypedWordEvidenceStore341.read(context).vocabulary());
        }finally{w.close();}
    }

    @Test public void invalidTokenAndStaleEditorSessionAreDiscarded()
            throws Exception {
        enable();
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try{
            w.beginEditor(40,true);idle(w);
            w.committedSpace("continue",39,true,true); // old session
            w.committedSpace("hello@",40,true,true); // invalid
            w.committedSpace(null,40,true,true); // failed boundary
            idle(w);
            assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
            w.committedSpace("hello",40,true,true);
            w.committedSpace("world",40,true,true);
            idle(w);
            assertEquals(1,TypedWordEvidenceStore341.read(context).associations());
            assertEquals(0,TypedWordEvidenceStore341.read(context).confirmedPairs());
            w.endEditor(40);idle(w);
            assertEquals(List.of(),w.nextWords("hello",3,40));
        }finally{w.close();}
    }

    @Test public void corruptStoreIsNotSilentlyRewritten()
            throws Exception {
        enable();
        context.getSharedPreferences(TypedWordEvidenceStore341.PREFS,
                Context.MODE_PRIVATE).edit().putInt("p:orphan missing",4).commit();
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try{
            w.beginEditor(50,true);idle(w);
            w.committedSpace("hello",50,true,true);idle(w);
            assertEquals(0,w.visibleWordCount(50));
            assertTrue(context.getSharedPreferences(TypedWordEvidenceStore341.PREFS,
                    Context.MODE_PRIVATE).contains("p:orphan missing"));
        }finally{w.close();}
    }

    @Test public void preferenceResetCannotChangeLegacyV3Export()
            throws Exception {
        enable();
        String before=LearningTransferStore.exportV3(context);
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(context);
        try{
            w.beginEditor(60,true);idle(w);
            for(int n=0;n<3;n++)w.committedSpace("continue",60,true,true);
            idle(w);
            assertEquals(before,LearningTransferStore.exportV3(context));
            assertTrue(TypedWordsConsent341.setEnabled(context,false));
            assertTrue(TypedWordEvidenceStore341.clearAll(context));
            assertEquals(before,LearningTransferStore.exportV3(context));
        }finally{w.close();}
    }
}
