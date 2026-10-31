package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 388 Android editor-boundary tests for key-row swipes while the
 * idle strip displays next-word choices (e.g., I, The, Hey).
 * This is an isolated InputConnection harness, not an IME Samsung test.
 */
@RunWith(AndroidJUnit4.class)
public final class LetterRowIdle388AndroidTest {
    private static BaseInputConnection editor(String initial) {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(c),true);
        Editable text=ic.getEditable();
        text.replace(0,text.length(),initial);
        Selection.setSelection(text,text.length());
        return ic;
    }
    private static NextWordTapTicket340 capture(BaseInputConnection ic,long session,
            int cursor){
        return NextWordTapTicket340.capture(
                ic.getTextBeforeCursor(NextWordTapTicket340.BEFORE_LIMIT,0),
                ic.getTextAfterCursor(1,0),session,cursor,cursor,
                false,false,false);
    }
    private static boolean stillCurrent(NextWordTapTicket340 ticket,
            BaseInputConnection ic,long session,int cursor){
        return ticket!=null && ticket.current(
                ic.getTextBeforeCursor(NextWordTapTicket340.BEFORE_LIMIT,0),
                ic.getTextAfterCursor(1,0),session,cursor,cursor,false,false);
    }
    @Test public void idleImageCandidatesCanArmASDFDown(){
        List<String> visible=Arrays.asList("I","The","Hey");
        assertEquals(LetterRowIdleMode388.Mode.NEXT_WORD,
                LetterRowIdleMode388.start("","",true,visible));
        assertEquals("The",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,visible));
        LetterRowPredictionSwipe386 gesture=
                new LetterRowPredictionSwipe386(1,45f,20f,1f);
        assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,
                gesture.release(1,45f,73f));
    }
    @Test public void nextWordAtVerifiedSpaceBoundaryCommitsExactlyOnce(){
        BaseInputConnection ic=editor("Hello ");
        NextWordTapTicket340 ticket=capture(ic,19,6);
        assertNotNull(ticket);
        assertTrue(stillCurrent(ticket,ic,19,6));
        assertEquals(LetterRowIdleMode388.Mode.NEXT_WORD,
                LetterRowIdleMode388.start("","",true,
                        Arrays.asList("I","The","Hey")));
        assertTrue(LetterRowIdleMode388.finish(true,"","","",true));
        assertTrue(ic.commitText("The ",1));
        assertEquals("Hello The ",ic.getEditable().toString());
        assertFalse("A second action from the stale gesture must fail",
                stillCurrent(ticket,ic,19,6));
    }
    @Test public void fieldMoveOrOtherTypingRejectsStaleIdleGesture(){
        BaseInputConnection ic=editor("Hello ");
        NextWordTapTicket340 ticket=capture(ic,22,6);
        assertNotNull(ticket);
        assertTrue(ic.commitText("x",1));
        assertFalse(stillCurrent(ticket,ic,22,6));
        assertFalse(LetterRowIdleMode388.finish(true,"","","x",false));
        assertFalse(LetterRowIdleMode388.finish(true,"","","",false));
    }
    @Test public void unfinishedWordAndMidDocumentNeverArmIdle(){
        BaseInputConnection ic=editor("Hello");
        assertNull(capture(ic,23,5));
        assertEquals(LetterRowIdleMode388.Mode.UNAVAILABLE,
                LetterRowIdleMode388.start("","",false,
                        Arrays.asList("I","The","Hey")));
        assertFalse(LetterRowIdleMode388.finish(true,"","","",false));
        assertEquals(LetterRowIdleMode388.Mode.UNAVAILABLE,
                LetterRowIdleMode388.start("Hello","",true,
                        Arrays.asList("I","The","Hey")));
    }
    @Test public void composingGestureStillUsesOriginalCorrectionPath(){
        List<String> options=Arrays.asList("tell","te","test");
        assertEquals(LetterRowIdleMode388.Mode.COMPOSING,
                LetterRowIdleMode388.start("te","te",false,options));
        assertTrue(LetterRowIdleMode388.finish(false,"te","te","te",false));
        assertFalse(LetterRowIdleMode388.finish(false,"te","","",true));
        assertFalse(LetterRowIdleMode388.finish(false,"te","tell","tell",true));
    }
    @Test public void centerOnlyNextWordAndNoCandidateFailClosed(){
        List<String> center=Arrays.asList("","The","");
        assertEquals(LetterRowIdleMode388.Mode.NEXT_WORD,
                LetterRowIdleMode388.start("","",true,center));
        assertEquals("The",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,center));
        assertEquals(LetterRowIdleMode388.Mode.UNAVAILABLE,
                LetterRowIdleMode388.start("","",true,
                        Arrays.asList("","","")));
    }
}
