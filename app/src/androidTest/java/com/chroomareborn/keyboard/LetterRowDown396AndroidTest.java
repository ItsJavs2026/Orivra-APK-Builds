package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 396 Android 35: target DOWN-only regressions after chat Send.
 * Synthetic InputConnections and Views; not the full real InputMethodService.
 */
@RunWith(AndroidJUnit4.class)
public final class LetterRowDown396AndroidTest {
    private static BaseInputConnection editor(String initial) {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(ctx),true);
        Editable editable=ic.getEditable();
        editable.replace(0,editable.length(),initial);
        Selection.setSelection(editable,editable.length());
        return ic;
    }
    private static NextWordTapTicket340 ticket(BaseInputConnection ic,long session){
        int cursor=Selection.getSelectionStart(ic.getEditable());
        return NextWordTapTicket340.capture(
                ic.getTextBeforeCursor(NextWordTapTicket340.BEFORE_LIMIT,0),
                ic.getTextAfterCursor(1,0),session,cursor,cursor,
                false,false,false);
    }
    private static boolean current(NextWordTapTicket340 ticket,
            BaseInputConnection ic,long session){
        if(ticket==null)return false;
        int cursor=Selection.getSelectionStart(ic.getEditable());
        return ticket.current(
                ic.getTextBeforeCursor(NextWordTapTicket340.BEFORE_LIMIT,0),
                ic.getTextAfterCursor(1,0),session,cursor,cursor,false,false);
    }
    private static LetterRowPredictionSwipe386.Direction down(int pointer,float travel){
        LetterRowPredictionSwipe386 g=
                new LetterRowPredictionSwipe386(pointer,45f,15f,1f);
        return g.release(pointer,45f,15f+travel);
    }
    @Test public void oneCenteredIdleChipMapsToDOWNNotLeftWhenExplicitlyIdle(){
        List<String> only=List.of("The");
        assertEquals(LetterRowIdleMode388.Mode.NEXT_WORD,
                LetterRowIdleMode388.start("","",true,only));
        assertNull("Active left-only chip must remain non-center",
                LetterRowPredictionSwipe386.chosen(
                    LetterRowPredictionSwipe386.Direction.DOWN,only,false));
        assertEquals("The",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,only,true));
        assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,down(0,34f));
    }
    @Test public void onlyActiveChipMustNeverAutoinsertAsMiddle(){
        List<String> only=List.of("test");
        assertEquals(LetterRowIdleMode388.Mode.COMPOSING,
                LetterRowIdleMode388.start("te","te",false,only));
        assertNull(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,only,false));
        assertEquals("test",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.LEFT,only,false));
        assertEquals("test",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.LEFT,only,true));
    }
    @Test public void threeSlotCenterAndBlankCenterRemainStrict(){
        assertEquals("The",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("I","The","Hey"),true));
        assertEquals("working",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("","working",""),false));
        assertNull(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("I","","Hey"),true));
        assertNull(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                List.of(""),true));
    }
    @Test public void shorterTapNeverCommitsAndSmallEarlyDOWNCanCommit(){
        for(int dp=0;dp<32;dp++){
            assertEquals("tap travel="+dp,
                    LetterRowPredictionSwipe386.Direction.NONE,down(dp%5,dp));
        }
        for(int dp=32;dp<=80;dp++){
            assertEquals("deliberate DOWN travel="+dp,
                    LetterRowPredictionSwipe386.Direction.DOWN,down(dp%5,dp));
        }
    }
    @Test public void realViewReceivesEarlierDOWNWhileStillInsideKeyBounds(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            row.setMotionEventSplittingEnabled(true);
            Button key=new Button(ctx);
            row.addView(key,new LinearLayout.LayoutParams(130,64));
            row.measure(View.MeasureSpec.makeMeasureSpec(130,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));
            row.layout(0,0,130,64);
            AtomicReference<LetterRowPredictionSwipe386> gesture=new AtomicReference<>();
            AtomicReference<LetterRowPredictionSwipe386.Direction> finished=
                    new AtomicReference<>(LetterRowPredictionSwipe386.Direction.NONE);
            key.setOnTouchListener((view,event)->{
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        gesture.set(new LetterRowPredictionSwipe386(
                                0,event.getX(),event.getY(),1f));
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        return gesture.get().move(0,event.getX(),event.getY());
                    case MotionEvent.ACTION_UP:
                        finished.set(gesture.get().release(
                                0,event.getX(),event.getY()));
                        return true;
                    default:return true;
                }
            });
            long t=SystemClock.uptimeMillis();
            MotionEvent d=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,65f,12f,0);
            MotionEvent m=MotionEvent.obtain(t,t+14,MotionEvent.ACTION_MOVE,65f,46f,0);
            MotionEvent u=MotionEvent.obtain(t,t+28,MotionEvent.ACTION_UP,65f,46f,0);
            try{
                assertTrue(row.dispatchTouchEvent(d));
                assertTrue(row.dispatchTouchEvent(m));
                assertTrue(row.dispatchTouchEvent(u));
            }finally{d.recycle();m.recycle();u.recycle();}
            assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,finished.get());
        });
    }
    @Test public void repeatedSendEmptyRefocusAndDOWNPreservesOneSpace(){
        BaseInputConnection ic=editor("hello ");
        Editable text=ic.getEditable();
        for(int iteration=0;iteration<1200;iteration++){
            // An app clears the same editor after a sent chat reply.
            text.clear();
            Selection.setSelection(text,0);
            final long session=iteration+50;
            NextWordTapTicket340 t=ticket(ic,session);
            assertNotNull("Each empty editor has a fresh idle ticket",t);
            assertTrue(current(t,ic,session));
            assertEquals("The",LetterRowPredictionSwipe386.chosen(
                    down(iteration%5,34f),List.of("The"),true));
            assertTrue(LetterRowIdleMode388.finish(true,"","","",true));
            assertTrue(ic.commitText("The ",1));
            assertEquals("The ",text.toString());
            assertFalse("Old caret ticket expires immediately after commit",
                    current(t,ic,session));
        }
        assertEquals("The ",text.toString());
    }
    @Test public void staleCursorOrDifferentCenterNeverCommits(){
        BaseInputConnection ic=editor("hello ");
        NextWordTapTicket340 t=ticket(ic,8);
        assertNotNull(t);
        List<String> initial=List.of("The");
        assertEquals("The",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,initial,true));
        List<String> changed=List.of("There");
        assertNotEquals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,initial,true),
                LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,changed,true));
        assertTrue(ic.commitText("x",1));
        assertFalse(current(t,ic,8));
        assertFalse(LetterRowIdleMode388.finish(true,"","","x",false));
        assertEquals("hello x",ic.getEditable().toString());
    }
    @Test public void wrongPointerReversalAndGestureCancellationStillFailClosed(){
        LetterRowPredictionSwipe386 g=new LetterRowPredictionSwipe386(
                7,45f,10f,1f);
        assertFalse(g.move(8,45f,46f));
        assertEquals(LetterRowPredictionSwipe386.Direction.CANCEL,
                g.release(7,45f,46f));
        LetterRowPredictionSwipe386 reverse=
                new LetterRowPredictionSwipe386(1,45f,10f,1f);
        assertTrue(reverse.move(1,110f,10f));
        assertFalse(reverse.move(1,45f,50f));
        assertEquals(LetterRowPredictionSwipe386.Direction.CANCEL,
                reverse.release(1,45f,50f));
        LetterRowPredictionSwipe386 cancelled=
                new LetterRowPredictionSwipe386(0,45f,10f,1f);
        cancelled.cancel();
        assertEquals(LetterRowPredictionSwipe386.Direction.CANCEL,
                cancelled.release(0,45f,80f));
    }
}
