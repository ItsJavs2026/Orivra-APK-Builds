package com.chroomareborn.keyboard;

import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Android platform MotionEvent boundary qualification. This tests final
 * coordinate semantics, NOT a physical IME key gesture on Samsung hardware.
 */
@RunWith(AndroidJUnit4.class)
public final class EnterReleaseMotionEventAndroidTest {
    private static boolean wouldSend(MotionEvent up,float downX,float downY,
                                     boolean armed,boolean sameEditor) {
        boolean released=up.getActionMasked()==MotionEvent.ACTION_UP;
        boolean moved=!EnterKeyPolicy.gestureWithinSlop(
                up.getRawX()-downX,up.getRawY()-downY,22f);
        return EnterKeyPolicy.shouldSendOnRelease(
                released,armed,moved,sameEditor,true);
    }

    @Test public void coalescedFinalUpOutsideSlopCannotSend() {
        MotionEvent down=MotionEvent.obtain(0L,0L,MotionEvent.ACTION_DOWN,
                150f,90f,0);
        MotionEvent end=MotionEvent.obtain(0L,650L,MotionEvent.ACTION_UP,
                200f,90f,0);
        try {
            assertEquals(MotionEvent.ACTION_UP,end.getActionMasked());
            assertFalse("armed hold with final displacement must not send",
                    wouldSend(end,down.getRawX(),down.getRawY(),true,true));
        } finally {down.recycle();end.recycle();}
    }

    @Test public void stationaryLongHoldMaySendOnlyWithSameEditor() {
        MotionEvent up=MotionEvent.obtain(0L,650L,MotionEvent.ACTION_UP,
                150f,90f,0);
        MotionEvent cancelled=MotionEvent.obtain(0L,650L,
                MotionEvent.ACTION_CANCEL,150f,90f,0);
        try {
            assertTrue(wouldSend(up,150f,90f,true,true));
            assertFalse(wouldSend(up,150f,90f,false,true));
            assertFalse(wouldSend(up,150f,90f,true,false));
            assertFalse(wouldSend(cancelled,150f,90f,true,true));
        } finally {up.recycle();cancelled.recycle();}
    }
}
