package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Synthetic two-thumb presses. Gestures are transient; never saved/logged. */
@RunWith(AndroidJUnit4.class)
public final class HintKeyPressOrder353AndroidTest {
    @Test public void sequentialQuickTapsCommitExactlyOnceEach(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long h=q.begin();q.complete(h,()->out.append("h"));
        long o=q.begin();q.complete(o,()->out.append("o"));
        assertEquals("ho",out.toString());
        assertFalse(q.hasOpenPresses());
    }
    @Test public void secondFingerReleasingFirstStillProducesDownOrder(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long h=q.begin(),o=q.begin();
        q.complete(o,()->out.append("o"));
        assertEquals("",out.toString());
        assertTrue(q.hasOpenPresses());
        q.complete(h,()->out.append("h"));
        assertEquals("ho",out.toString());
        assertFalse(q.hasOpenPresses());
    }
    @Test public void threeOverlappingTouchesFlushDownOrder(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long a=q.begin(),b=q.begin(),c=q.begin();
        q.complete(c,()->out.append("c"));
        q.complete(b,()->out.append("b"));
        assertEquals("",out.toString());
        q.complete(a,()->out.append("a"));
        assertEquals("abc",out.toString());
    }
    @Test public void canceledPrimaryDoesNotLoseConfirmedSecondThumb(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long x=q.begin(),y=q.begin();
        q.complete(y,()->out.append("y"));
        q.cancel(x);
        assertEquals("y",out.toString());
    }
    @Test public void longPressSymbolSubstitutesLetterWithoutDuplicate(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long t=q.begin();
        q.complete(t,()->out.append("@"));
        q.complete(t,()->out.append("a"));
        assertEquals("@",out.toString());
    }
    @Test public void staleCancelAllDiscardsConfirmedPendingActions(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long a=q.begin(),b=q.begin();
        q.complete(b,()->out.append("b"));
        q.cancelAll();
        q.complete(a,()->out.append("a"));
        assertEquals("",out.toString());
        assertFalse(q.hasOpenPresses());
        long c=q.begin();
        q.complete(c,()->out.append("c"));
        assertEquals("c",out.toString());
    }
    @Test public void malformedExtraReleasesCannotInsertAgain(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        long k=q.begin();
        q.complete(k,()->out.append("k"));
        q.cancel(k);
        q.complete(k,()->out.append("x"));
        assertEquals("k",out.toString());
    }
    @Test public void manyFastFingerPairsRemainBoundedAndOrdered(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        for(int i=0;i<250;i++) {
            long first=q.begin(),second=q.begin();
            q.complete(second,()->out.append("o"));
            q.complete(first,()->out.append("h"));
        }
        assertEquals(500,out.length());
        for(int i=0;i<500;i+=2)assertEquals("ho",out.substring(i,i+2));
        assertFalse(q.hasOpenPresses());
    }
    @Test public void overflowingTwelveStaleGesturesReclaimsTheQueue(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        List<Long> stale=new ArrayList<>();
        for(int i=0;i<12;i++)stale.add(q.begin());
        long next=q.begin(); // fail closed and start a fresh queue
        for(long old:stale)q.complete(old,()->out.append("X"));
        assertEquals("",out.toString());
        q.complete(next,()->out.append("n"));
        assertEquals("n",out.toString());
    }
}
