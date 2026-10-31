package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research 385: actual Android View measurement and deterministic touch
 * recognition. This does not instantiate the production InputMethodService.
 * No user words, input telemetry or network.
 */
@RunWith(AndroidJUnit4.class)
public final class TopBarTouch385AndroidTest {
    private static int px(Context c,int dp) {
        return Math.round(dp*c.getResources().getDisplayMetrics().density);
    }
    private static void ui(Runnable r){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
    }
    @Test public void expandedOrdinaryChipFillsExistingFortyEightDpRow(){
        ui(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int total=px(ctx,360),height=px(ctx,48);
            TextView[] cells=new TextView[3];
            for(int i=0;i<3;i++){
                cells[i]=new TextView(ctx);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                        total/3,px(ctx,TopBarTouch385.normalHeightDp(true,false)));
                int h=px(ctx,TopBarTouch385.normalHorizontalMarginDp(true));
                int v=px(ctx,TopBarTouch385.normalVerticalMarginDp(true));
                p.setMargins(h,v,h,v);
                row.addView(cells[i],p);
            }
            row.measure(View.MeasureSpec.makeMeasureSpec(total,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            row.layout(0,0,total,height);
            assertEquals(height,row.getHeight());
            for(TextView cell:cells){
                assertEquals(height,cell.getHeight());
                assertEquals(0,cell.getTop());
                assertEquals(height,cell.getBottom());
            }
        });
    }
    @Test public void legacyChipHeightAndGuttersRemainUnchanged(){
        assertEquals(44,TopBarTouch385.normalHeightDp(false,false));
        assertEquals(48,TopBarTouch385.normalHeightDp(false,true));
        assertEquals(2,TopBarTouch385.normalVerticalMarginDp(false));
        assertEquals(2,TopBarTouch385.normalHorizontalMarginDp(false));
        assertEquals(48,TopBarTouch385.normalHeightDp(true,false));
        assertEquals(0,TopBarTouch385.normalVerticalMarginDp(true));
        assertEquals(1,TopBarTouch385.normalHorizontalMarginDp(true));
    }
    @Test public void riseChipsCanFillExistingSixtyFourDpRow(){
        assertEquals(60,TopBarTouch385.riseHeightDp(false));
        assertEquals(1,TopBarTouch385.riseVerticalMarginDp(false));
        assertEquals(2,TopBarTouch385.riseHorizontalMarginDp(false));
        assertEquals(64,TopBarTouch385.riseHeightDp(true));
        assertEquals(0,TopBarTouch385.riseVerticalMarginDp(true));
        assertEquals(1,TopBarTouch385.riseHorizontalMarginDp(true));
    }
    @Test public void nearEdgeTapSlightlyOutsideOldFourteenDpRadiusIsAccepted(){
        TopBarTouch385 gesture=new TopBarTouch385(1,30f,20f,1f);
        assertTrue(gesture.move(1,47f,20f,false));
        assertEquals(TopBarTouch385.Decision.TAP,
                gesture.release(1,47f,20f,false));
        assertFalse(gesture.active());
    }
    @Test public void borderlineDragNeverAutocorrectsOrBecomesTap(){
        TopBarTouch385 gesture=new TopBarTouch385(3,100f,35f,1f);
        assertTrue(gesture.move(3,126f,35f,true));
        assertEquals(TopBarTouch385.Decision.CANCEL,
                gesture.release(3,126f,35f,true));
    }
    @Test public void deliberateHorizontalSwipeStillSelectsAdjacentChip(){
        TopBarTouch385 left=new TopBarTouch385(0,100f,30f,1f);
        assertTrue(left.move(0,60f,30f,false));
        assertEquals(TopBarTouch385.Decision.LEFT,left.preview());
        assertEquals(TopBarTouch385.Decision.LEFT,
                left.release(0,60f,30f,false));
        TopBarTouch385 right=new TopBarTouch385(2,30f,30f,1f);
        assertEquals(TopBarTouch385.Decision.RIGHT,
                right.release(2,70f,30f,false));
        assertEquals("test",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.RIGHT,1,
                Arrays.asList("tekk","tell","test"),
                Arrays.asList("tell me"),false));
    }
    @Test public void riseSelectionOnlyWhenRiseIsVisible(){
        TopBarTouch385 open=new TopBarTouch385(5,50f,70f,1f);
        assertEquals(TopBarTouch385.Decision.UP,
                open.release(5,50f,20f,true));
        TopBarTouch385 closed=new TopBarTouch385(5,50f,70f,1f);
        assertEquals(TopBarTouch385.Decision.CANCEL,
                closed.release(5,50f,20f,false));
    }
    @Test public void overshootAndReturnNeverTriggersTapOrCorrection(){
        TopBarTouch385 g=new TopBarTouch385(0,40f,30f,1f);
        assertTrue(g.move(0,84f,30f,false));
        assertEquals(TopBarTouch385.Decision.RIGHT,g.preview());
        assertEquals(TopBarTouch385.Decision.CANCEL,
                g.release(0,41f,30f,false));
    }
    @Test public void directionReversalCancelsWholeGesture(){
        TopBarTouch385 g=new TopBarTouch385(0,60f,30f,1f);
        assertTrue(g.move(0,100f,30f,false));
        assertFalse(g.move(0,10f,30f,false));
        assertEquals(TopBarTouch385.Decision.CANCEL,
                g.release(0,10f,30f,false));
    }
    @Test public void pointerMismatchAndInvalidCoordinatesFailClosed(){
        TopBarTouch385 g=new TopBarTouch385(1,40f,35f,1f);
        assertFalse(g.move(2,40f,35f,true));
        assertEquals(TopBarTouch385.Decision.CANCEL,
                g.release(1,40f,35f,true));
        assertEquals(TopBarTouch385.Decision.CANCEL,
                new TopBarTouch385(1,40f,35f,1f)
                        .release(1,Float.NaN,35f,true));
    }
    @Test public void independentOptionStartsOffAndPersistsSafely(){
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=ctx.getSharedPreferences("keyboard_feedback",
                Context.MODE_PRIVATE);
        boolean old=prefs.getBoolean(TopBarTouch385.PREF,false);
        boolean swipe=prefs.getBoolean(PredictionSwipe372.PREF,true);
        try{
            prefs.edit().putBoolean(TopBarTouch385.PREF,true).commit();
            assertTrue(prefs.getBoolean(TopBarTouch385.PREF,false));
            assertEquals(swipe,prefs.getBoolean(PredictionSwipe372.PREF,true));
            prefs.edit().putBoolean(TopBarTouch385.PREF,false).commit();
            assertFalse(prefs.getBoolean(TopBarTouch385.PREF,true));
        } finally {prefs.edit().putBoolean(TopBarTouch385.PREF,old).commit();}
    }
}
