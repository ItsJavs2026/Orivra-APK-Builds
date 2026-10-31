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
}
