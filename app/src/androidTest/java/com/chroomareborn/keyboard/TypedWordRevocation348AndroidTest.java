package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research 348. Existing opt-in learned words must never remain visible
 * after consent/base Learning revocation, even in an already open editor.
 * Tests use synthetic strings only; no actual user typing is captured.
 */
@RunWith(AndroidJUnit4.class)
public final class TypedWordRevocation348AndroidTest {
    private Context ctx;
    private boolean priorLearning,priorConsent;
    @Before public void setup(){
        ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        priorLearning=PersonalLearning.enabled(ctx);
        priorConsent=TypedWordsConsent341.enabled(ctx);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(ctx));
        PersonalLearning.setEnabled(ctx,true);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,true));
    }
    @After public void cleanup(){
        assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(ctx));
        PersonalLearning.setEnabled(ctx,priorLearning);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,priorConsent));
    }
    static void idle(TypedWordLearningWorker346 worker)throws Exception {
        worker.workerIdleForTest().get(12,TimeUnit.SECONDS);
    }
    private void teach(TypedWordLearningWorker346 worker,long session)
            throws Exception {
        worker.beginEditor(session,true);
        idle(worker);
        for(int i=0;i<3;i++) {
            worker.committedSpace("to",session,true,true);
            worker.committedSpace("continue",session,true,true);
            worker.breakContext(session);
        }
        idle(worker);
        assertEquals(List.of("continue"),worker.nextWords("to",3,session));
        assertEquals(List.of("continue"),worker.completedWords("cont",3,session));
    }

    @Test public void disableTypedConsentHidesCurrentSuggestionsImmediately()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try {
            teach(w,10);
            long rev=TypedWordsConsent341.liveRevision();
            assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
            assertTrue(TypedWordsConsent341.liveRevision()>rev);
            assertEquals(List.of(),w.nextWords("to",3,10));
            assertEquals(List.of(),w.completedWords("cont",3,10));
            assertEquals(0,w.visibleWordCount(10));
            // Disabling is not destructive unless user explicitly chose erase.
            assertEquals(3,TypedWordEvidenceStore341.read(ctx)
                    .words.get("continue").intValue());
        }finally{w.close();}
    }

    @Test public void reenableRequiresNewEditorForWordsToReappear()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try {
            teach(w,11);
            assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
            assertTrue(TypedWordsConsent341.setEnabled(ctx,true));
            assertEquals(List.of(),w.nextWords("to",3,11));
            assertEquals(List.of(),w.completedWords("cont",3,11));
            w.endEditor(11);idle(w);
            w.beginEditor(12,true);idle(w);
            assertEquals(List.of("continue"),w.nextWords("to",3,12));
        }finally{w.close();}
    }

    @Test public void baseLearningOffHidesActiveSnapshotsWithoutDeletingTypedData()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try {
            teach(w,20);
            PersonalLearning.setEnabled(ctx,false);
            assertEquals(List.of(),w.nextWords("to",3,20));
            assertEquals(List.of(),w.completedWords("cont",3,20));
            assertEquals(0,w.visibleWordCount(20));
            assertEquals(2,TypedWordEvidenceStore341.read(ctx).vocabulary());
        }finally{w.close();}
    }

    @Test public void baseLearningToggleDoesNotRestoreStaleExistingEditor()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try {
            teach(w,21);
            PersonalLearning.setEnabled(ctx,false);
            PersonalLearning.setEnabled(ctx,true);
            assertEquals(List.of(),w.nextWords("to",3,21));
            w.endEditor(21);idle(w);
            w.beginEditor(22,true);idle(w);
            assertEquals(List.of("continue"),w.nextWords("to",3,22));
        }finally{w.close();}
    }

    @Test public void clearExistingLearningRevokesTypedSuggestionsImmediately()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try {
            teach(w,23);
            PersonalLearning.clear(ctx);
            assertEquals(List.of(),w.nextWords("to",3,23));
            assertEquals(List.of(),w.completedWords("cont",3,23));
            assertEquals(2,TypedWordEvidenceStore341.read(ctx).vocabulary());
        }finally{w.close();}
    }

    @Test public void baseLearningOffOnBlocksOldQueuedWrite()
            throws Exception {
        long capturedRevision=TypedWordsConsent341.liveRevision();
        long consentEpoch=TypedWordsConsent341.generation(ctx);
        PersonalLearning.setEnabled(ctx,false);
        PersonalLearning.setEnabled(ctx,true);
        assertTrue(TypedWordsConsent341.liveRevision()>capturedRevision);
        TypedWordBoundaryStore346.Result rejected=TypedWordBoundaryStore346
                .record(ctx,"continue",null,true,true,
                        consentEpoch,capturedRevision);
        assertFalse(rejected.stored);
        assertEquals(0,TypedWordEvidenceStore341.read(ctx).vocabulary());
    }

    @Test public void typedConsentOffOnBlocksOldQueuedWrite()
            throws Exception {
        long capturedRevision=TypedWordsConsent341.liveRevision();
        long consentEpoch=TypedWordsConsent341.generation(ctx);
        assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
        assertTrue(TypedWordsConsent341.setEnabled(ctx,true));
        assertTrue(TypedWordsConsent341.liveRevision()>capturedRevision);
        assertFalse(TypedWordBoundaryStore346.record(ctx,"continue",null,
                true,true,consentEpoch,capturedRevision).stored);
        assertEquals(0,TypedWordEvidenceStore341.read(ctx).vocabulary());
    }

    @Test public void disabledMemoryNeverShowsWordsFromAnotherEditor()
            throws Exception {
        TypedWordLearningWorker346 w=new TypedWordLearningWorker346(ctx);
        try{
            teach(w,30);
            assertTrue(TypedWordsConsent341.setEnabled(ctx,false));
            w.endEditor(30);idle(w);
            w.beginEditor(31,true);idle(w);
            assertEquals(List.of(),w.nextWords("to",3,31));
            assertEquals(List.of(),w.completedWords("cont",3,31));
        }finally{w.close();}
    }
}
