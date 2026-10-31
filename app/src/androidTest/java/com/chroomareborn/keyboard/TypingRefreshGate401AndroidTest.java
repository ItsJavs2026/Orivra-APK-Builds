package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 401: coalesced typing work must never retain text, mix editors,
 * bypass private/numeric field safety, or starve a normal typing burst.
 */
@RunWith(AndroidJUnit4.class)
public final class TypingRefreshGate401AndroidTest {
    @Test public void rapidKeysScheduleOnlyOnePendingRefresh() {
        TypingRefreshGate401 gate=new TypingRefreshGate401();
        Object editor=new Object();
        int scheduled=0;
        for(int i=0;i<3500;i++)if(gate.arm(73L,editor))scheduled++;
        assertEquals(1,scheduled);
        assertTrue(gate.isPendingFor(73L,editor));
        assertTrue(gate.consume(73L,editor));
        assertFalse(gate.isPending());
        assertTrue(gate.arm(73L,editor));
        assertTrue(gate.consume(73L,editor));
    }
    @Test public void inputSessionSwitchRejectsOldQueuedWork() {
        TypingRefreshGate401 gate=new TypingRefreshGate401();
        Object editor=new Object();
        assertTrue(gate.arm(10L,editor));
        assertFalse(gate.consume(11L,editor));
        assertFalse(gate.isPending());
    }
    @Test public void editorSwitchAndFinishReleaseTransientReferences() {
        TypingRefreshGate401 gate=new TypingRefreshGate401();
        Object first=new Object(),second=new Object();
        assertTrue(gate.arm(10L,first));
        assertFalse(gate.isPendingFor(10L,second));
        gate.cancel();
        assertTrue(gate.arm(10L,second));
        assertFalse(gate.consume(10L,first));
        assertFalse(gate.isPending());
        assertFalse(gate.arm(10L,null));
    }
    @Test public void neverCoalesceSensitiveNumericSymbolOrSelection() {
        assertTrue(TypingRefreshGate401.safeToCoalesce(
                false,true,false,false,false));
        assertFalse(TypingRefreshGate401.safeToCoalesce(
                true,true,false,false,false));
        assertFalse(TypingRefreshGate401.safeToCoalesce(
                false,false,false,false,false));
        assertFalse(TypingRefreshGate401.safeToCoalesce(
                false,true,true,false,false));
        assertFalse(TypingRefreshGate401.safeToCoalesce(
                false,true,false,true,false));
        assertFalse(TypingRefreshGate401.safeToCoalesce(
                false,true,false,false,true));
    }
    @Test public void boundedRefreshWindowCannotWaitIndefinitely() {
        assertTrue(TypingRefreshGate401.WAIT_MS>=32L);
        assertTrue(TypingRefreshGate401.WAIT_MS<=120L);
        TypingRefreshGate401 gate=new TypingRefreshGate401();
        Object editor=new Object();
        for(int i=0;i<80;i++) {
            assertTrue(gate.arm(5L,editor));
            assertTrue(gate.consume(5L,editor));
        }
    }
    @Test public void inMemorySessionLearnerIsBoundedAndClearsAtEditorExit()
            throws Exception {
        PersonalNextWordModel model=new PersonalNextWordModel();
        java.lang.reflect.Field rawContexts=
                PersonalNextWordModel.class.getDeclaredField("transitions");
        java.lang.reflect.Field rawVocab=
                PersonalNextWordModel.class.getDeclaredField("unigram");
        rawContexts.setAccessible(true);
        rawVocab.setAccessible(true);
        for(int i=0;i<13000;i++)model.observeCommittedWord(syntheticWord(i),true);
        java.util.Map<?,?> contexts=
                (java.util.Map<?,?>)rawContexts.get(model);
        java.util.Map<?,?> vocab=(java.util.Map<?,?>)rawVocab.get(model);
        assertTrue("context map must be capped",contexts.size()<=4096);
        assertTrue("vocabulary map must be capped",vocab.size()<=8192);
        for(Object value:contexts.values())
            assertTrue(((java.util.Map<?,?>)value).size()<=64);
        model.clear();
        assertEquals(0,contexts.size());
        assertEquals(0,vocab.size());
        assertEquals(0,model.observationCount());
    }
    private static String syntheticWord(int value) {
        char[] chars={'w','a','a','a','a'};
        for(int j=4;j>0;j--){
            chars[j]=(char)('a'+value%26);
            value/=26;
        }
        return new String(chars);
    }
    @Test public void prefixCacheDoesNotGrowWithLongSessions()throws Exception {
        PrefixCandidateCache340 cache=new PrefixCandidateCache340();
        for(int i=0;i<16000;i++){
            String prefix=syntheticWord(i);
            assertEquals(java.util.List.of("completed"),
                    cache.get(prefix,k->java.util.List.of("completed")));
        }
        java.lang.reflect.Field key=
                PrefixCandidateCache340.class.getDeclaredField("previousPrefix");
        java.lang.reflect.Field matches=
                PrefixCandidateCache340.class.getDeclaredField("previousMatches");
        key.setAccessible(true);
        matches.setAccessible(true);
        assertNotNull(key.get(cache));
        assertEquals(1,((java.util.List<?>)matches.get(cache)).size());
        cache.clear();
        assertNull(key.get(cache));
        assertTrue(((java.util.List<?>)matches.get(cache)).isEmpty());
    }
    @Test public void optionalDiagnosticsShowOnlyNumericHeapTimings() {
        String pkg=CandidateLatencyMetrics.CANDIDATE_PACKAGE;
        assertTrue(CandidateLatencyMetrics.setEnabled(pkg,true));
        for(int i=0;i<32;i++) {
            long begin=System.nanoTime();
            CandidateLatencyMetrics.recordKeyHandler340(begin,begin+1000000L);
        }
        String report=CandidateLatencyMetrics.summary(pkg);
        assertTrue(report.contains("Java heap now:"));
        assertTrue(report.contains("Java heap peak sampled:"));
        assertTrue(report.contains("Native heap allocated now:"));
        assertTrue(report.contains("Deferred suggestion UI work:"));
        assertFalse(report.contains("password"));
        assertFalse(report.contains("thoseyou"));
        assertFalse(CandidateLatencyMetrics.setEnabled(pkg,false));
        assertTrue(CandidateLatencyMetrics.summary(pkg).contains("Recording OFF"));
    }
}
