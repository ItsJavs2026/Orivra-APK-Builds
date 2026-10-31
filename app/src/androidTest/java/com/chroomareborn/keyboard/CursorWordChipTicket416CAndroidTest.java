package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic editable state for hide/reopen cursor suggestion lifetime. */
@RunWith(AndroidJUnit4.class)
public final class CursorWordChipTicket416CAndroidTest {
    @Test public void ticketSurvivesDisplayFrameButNotOlderInputSession() {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection editor=new BaseInputConnection(new View(app),true);
        Editable text=editor.getEditable();
        text.replace(0,text.length(),"before typinng after");
        Selection.setSelection(text,14);
        String word="typinng";
        long firstRevision=40;
        CursorEditSnapshotGuard.Ticket t=CursorEditSnapshotGuard.capture(word,
                editor.getTextBeforeCursor(64,0),
                editor.getTextAfterCursor(64,0),firstRevision,14,14,
                false,false,false);
        assertNotNull(t);
        assertTrue(CursorWordChipTicket416C.preserve(
                t.word,word,t.sessionVersion,firstRevision,
                t.selectionStart,t.selectionEnd,14,14,false,false,false));
        assertTrue(CursorEditSnapshotGuard.current(t,
                editor.getTextBeforeCursor(64,0),
                editor.getTextAfterCursor(64,0),firstRevision,14,14,
                false,false,false));
        // Dismissing keyboard invalidates the old editing revision.
        long reopenRevision=firstRevision+2;
        assertFalse(CursorWordChipTicket416C.preserve(
                t.word,word,t.sessionVersion,reopenRevision,
                t.selectionStart,t.selectionEnd,14,14,false,false,false));
        CursorEditSnapshotGuard.Ticket replacement=CursorEditSnapshotGuard.capture(
                word,editor.getTextBeforeCursor(64,0),
                editor.getTextAfterCursor(64,0),reopenRevision,14,14,
                false,false,false);
        assertNotNull(replacement);
        assertTrue(CursorWordChipTicket416C.preserve(
                replacement.word,word,replacement.sessionVersion,reopenRevision,
                replacement.selectionStart,replacement.selectionEnd,
                14,14,false,false,false));
        // Later editor text changes must still make the old ticket unusable.
        text.insert(14,"!");
        assertFalse(CursorEditSnapshotGuard.current(replacement,
                editor.getTextBeforeCursor(64,0),
                editor.getTextAfterCursor(64,0),reopenRevision,14,14,
                false,false,false));
    }
}
