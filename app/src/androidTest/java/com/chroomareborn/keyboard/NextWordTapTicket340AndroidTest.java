package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** A changed cursor or editor must not receive an old phrase suggestion. */
@RunWith(AndroidJUnit4.class)
public final class NextWordTapTicket340AndroidTest {
    @Test public void matchingEndOfEditorCanAcceptPhrase(){
        NextWordTapTicket340 t=NextWordTapTicket340.capture(
                "I need ", "",11,7,7,false,false,false);
        assertNotNull(t);
        assertTrue(t.current("I need ","",11,7,7,false,false));
    }
    @Test public void changedSessionOrCursorRejectsOldChip(){
        NextWordTapTicket340 t=NextWordTapTicket340.capture(
                "I need ", "",11,7,7,false,false,false);
        assertFalse(t.current("I need ","",12,7,7,false,false));
        assertFalse(t.current("I need ","",11,4,4,false,false));
        assertFalse(t.current("I need ","",11,7,8,true,false));
        assertFalse(t.current("I need ","",11,7,7,false,true));
    }
    @Test public void changedWordAndAfterTextRejectsOldChip(){
        NextWordTapTicket340 t=NextWordTapTicket340.capture(
                "I need ", "",11,7,7,false,false,false);
        assertFalse(t.current("I needed ","",11,7,7,false,false));
        assertFalse(t.current("I need ","other",11,7,7,false,false));
    }
    @Test public void noBoundarySelectionOrPrivateFieldCannotCapture(){
        assertNull(NextWordTapTicket340.capture(
                "I need","",11,6,6,false,false,false));
        assertNull(NextWordTapTicket340.capture(
                "I need ","",11,7,8,false,true,false));
        assertNull(NextWordTapTicket340.capture(
                "I need ","",11,7,7,true,false,false));
        assertNull(NextWordTapTicket340.capture(
                "I need ","after",11,7,7,false,false,true));
    }
    @Test public void unresolvedInitialCursorCanBeCheckedByText(){
        NextWordTapTicket340 t=NextWordTapTicket340.capture(
                "", "", 19,-1,-1,false,false,false);
        assertNotNull(t);
        assertTrue(t.current("","",19,-1,-1,false,false));
    }
}
