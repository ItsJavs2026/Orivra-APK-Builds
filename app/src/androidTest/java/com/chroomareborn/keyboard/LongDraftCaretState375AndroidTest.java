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

/**
 * Android 35 editor selections through large synthetic messages. Tests the
 * state policy at actual Android caret positions, not physical Samsung UI.
 * No private text, logs, screenshots or telemetry.
 */
@RunWith(AndroidJUnit4.class)
public final class LongDraftCaretState375AndroidTest {
    private static BaseInputConnection editor(String text) {
        Context context=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        BaseInputConnection editor=new BaseInputConnection(new View(context),true);
        Editable e=editor.getEditable();
        e.replace(0,e.length(),text);
        Selection.setSelection(e,e.length());
        return editor;
    }
    private static boolean suffix(BaseInputConnection editor) {
        CharSequence rest=editor.getTextAfterCursor(1,0);
        return rest!=null && rest.length()>0;
    }
    @Test public void longCommittedMessageEndsAtWordWithoutStickyEditing() {
        StringBuilder draft=new StringBuilder();
        for(int i=0;i<270;i++)draft.append("this is a harmless draft ");
        draft.append("keyboard");
        BaseInputConnection editor=editor(draft.toString());
        assertFalse(suffix(editor));
        for(int callback=0;callback<20;callback++) {
            assertFalse(LongDraftCaretState375.editingWithoutComposition(
                    false,suffix(editor)));
            assertFalse(LongDraftCaretState375.afterHostEndedComposition(
                    false,suffix(editor)));
        }
    }
    @Test public void movingCursorInsideLongDraftStillProtectsExistingText() {
        StringBuilder draft=new StringBuilder();
        for(int i=0;i<140;i++)draft.append("known sample text ");
        draft.append("keyboard");
        BaseInputConnection editor=editor(draft.toString());
        int length=editor.getEditable().length();
        Selection.setSelection(editor.getEditable(),length-3);
        assertTrue(suffix(editor));
        assertTrue(LongDraftCaretState375.editingWithoutComposition(
                false,suffix(editor)));
        assertTrue(LongDraftCaretState375.afterHostEndedComposition(
                false,suffix(editor)));
        assertEquals(draft.toString(),editor.getEditable().toString());
    }
    @Test public void repeatedMovesRecoverFromMiddleToEnd() {
        BaseInputConnection editor=editor("this is a long test message keyboard");
        int length=editor.getEditable().length();
        for(int n=0;n<120;n++) {
            Selection.setSelection(editor.getEditable(),Math.max(0,length-4));
            assertTrue(LongDraftCaretState375.editingWithoutComposition(
                    false,suffix(editor)));
            Selection.setSelection(editor.getEditable(),length);
            assertFalse(LongDraftCaretState375.editingWithoutComposition(
                    false,suffix(editor)));
        }
    }
    @Test public void selectingLastWordAlwaysPreservesSelectedRange() {
        BaseInputConnection editor=editor("I already typed keyboard");
        int n=editor.getEditable().length();
        Selection.setSelection(editor.getEditable(),n-8,n);
        assertTrue(LongDraftCaretState375.editingWithoutComposition(
                true,suffix(editor)));
        assertTrue(LongDraftCaretState375.afterHostEndedComposition(
                true,suffix(editor)));
        assertEquals("I already typed keyboard",editor.getEditable().toString());
    }
}
