package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic editor commits only. This tests worker integration, NOT
 * Samsung 17 latency or real Gboard-competitive prediction accuracy.
 * No user text, contacts, editor identifiers or network access.
 */
@RunWith(AndroidJUnit4.class)
public final class RankedLearningWorker357AndroidTest {
    private Context app;
    private boolean oldLearning,oldConsent;

    @Before public void before() {
        app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        oldLearning=PersonalLearning.enabled(app);
        oldConsent=TypedWordsConsent341.enabled(app);
        assertTrue(TypedWordsConsent341.setEnabled(app,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(app));
        PersonalLearning.setEnabled(app,true);
        assertTrue(TypedWordsConsent341.setEnabled(app,true));
    }

    @After public void after() {
        assertTrue(TypedWordsConsent341.setEnabled(app,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(app));
        PersonalLearning.setEnabled(app,oldLearning);
        assertTrue(TypedWordsConsent341.setEnabled(app,oldConsent));
    }

    private static void idle(TypedWordLearningWorker346 worker) throws Exception {
        worker.workerIdleForTest().get(12,TimeUnit.SECONDS);
    }

    @Test public void repeatedManualWordsQualifyWithoutImmediateTakeover()
            throws Exception {
        TypedWordLearningWorker346 worker=new TypedWordLearningWorker346(app);
        try {
            for(long session=1;session<=2;session++) {
                worker.beginEditor(session,true);
                idle(worker);
                for(String word:List.of("we","continue")) {
                    worker.committedSpace(word,session,true,true);
                }
                idle(worker);
                assertEquals(List.of(),worker.nextWords("we",3,session));
                assertEquals(List.of(),worker.completedWords("cont",3,session));
                worker.endEditor(session);
                idle(worker);
            }
            worker.beginEditor(3,true);
            idle(worker);
            worker.committedSpace("we",3,true,true);
            worker.committedSpace("continue",3,true,true);
            idle(worker);
            assertEquals(List.of("continue"),worker.nextWords("we",3,3));
            assertEquals(List.of("continue"),worker.completedWords("cont",3,3));
            assertEquals(2,worker.visibleWordCount(3));
            assertEquals(List.of(),worker.completedWords("c",3,3));
        } finally {worker.close();}
    }

    @Test public void tiesStayAlphabeticalWithNoWrongWordInsertion()
            throws Exception {
        TypedWordLearningWorker346 worker=new TypedWordLearningWorker346(app);
        try {
            for(long session=10;session<=13;session++) {
                worker.beginEditor(session,true);
                idle(worker);
                worker.committedSpace("hello",session,true,true);
                worker.committedSpace("world",session,true,true);
                worker.breakContext(session);
                worker.committedSpace("hello",session,true,true);
                worker.committedSpace("there",session,true,true);
                idle(worker);
                worker.endEditor(session);
                idle(worker);
            }
            worker.beginEditor(14,true);
            idle(worker);
            assertEquals(List.of("there","world"),
                    worker.nextWords("hello",5,14));
            assertEquals(List.of("there"),worker.completedWords("th",3,14));
            assertEquals(List.of(),worker.nextWords("hello",0,14));
        } finally {worker.close();}
    }

    @Test public void sessionChangeNeverExposesOldEditorResults()
            throws Exception {
        TypedWordLearningWorker346 worker=new TypedWordLearningWorker346(app);
        try {
            worker.beginEditor(20,true);
            idle(worker);
            for(int n=0;n<3;n++)
                worker.committedSpace("continuation",20,true,true);
            idle(worker);
            assertEquals(List.of("continuation"),
                    worker.completedWords("cont",3,20));

            worker.beginEditor(21,false);
            assertEquals(List.of(),worker.completedWords("cont",3,20));
            assertEquals(List.of(),worker.completedWords("cont",3,21));
            idle(worker);
            assertEquals(List.of(),worker.completedWords("cont",3,21));
            worker.committedSpace("private",21,false,true);
            idle(worker);
            assertEquals(List.of(),worker.completedWords("priv",3,21));
            assertEquals(0,worker.visibleWordCount(21));
        } finally {worker.close();}
    }

    @Test public void explicitConsentOffAndEraseCannotRestoreOldSuggestions()
            throws Exception {
        TypedWordLearningWorker346 worker=new TypedWordLearningWorker346(app);
        try {
            worker.beginEditor(30,true);
            idle(worker);
            for(int n=0;n<3;n++)
                worker.committedSpace("continue",30,true,true);
            idle(worker);
            assertEquals(List.of("continue"),
                    worker.completedWords("cont",3,30));

            assertTrue(TypedWordsConsent341.setEnabled(app,false));
            assertTrue(TypedWordEvidenceStore341.clearAll(app));
            worker.endEditor(30);
            idle(worker);
            worker.beginEditor(31,true);
            idle(worker);
            assertEquals(List.of(),worker.completedWords("cont",3,31));
            assertEquals(0,worker.visibleWordCount(31));
            assertEquals(0,TypedWordEvidenceStore341.read(app).vocabulary());
        } finally {worker.close();}
    }

    @Test public void suggestionReadsOnImeThreadNeedNoDiskOrNewSorting()
            throws Exception {
        TypedWordLearningWorker346 worker=new TypedWordLearningWorker346(app);
        try {
            worker.beginEditor(40,true);
            idle(worker);
            for(int n=0;n<3;n++) {
                worker.committedSpace("hello",40,true,true);
                worker.committedSpace("friend",40,true,true);
            }
            idle(worker);
            AtomicReference<List<String>> returned=new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    ()->returned.set(worker.nextWords("hello",3,40)));
            assertEquals(List.of("friend"),returned.get());
        } finally {worker.close();}
    }
}
