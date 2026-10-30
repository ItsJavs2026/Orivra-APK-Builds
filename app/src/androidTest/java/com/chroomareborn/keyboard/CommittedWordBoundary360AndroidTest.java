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
 * Android BaseInputConnection regression for word prediction after caret moves.
 * Models concrete editor operations. A real Samsung app remains user-tested.
 */
@RunWith(AndroidJUnit4.class)
public final class CommittedWordBoundary360AndroidTest {
    private static BaseInputConnection fresh(String initial,int caret) {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(ctx),true);
        Editable e=ic.getEditable();
        e.replace(0,e.length(),initial);
        Selection.setSelection(e,caret);
        return ic;
    }

    @Test public void screenshotUnknownWordAfterCaretReturnStaysWholeWord() {
        BaseInputConnection ic=fresh("App is instakke",15);
        // 15 is the end of the deliberately misspelled word.
        assertEquals("e",ic.getTextBeforeCursor(1,0).toString());
        assertTrue(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","d",false,
                ic.getTextAfterCursor(1,0).length()>0,false,true,false,false));
        assertTrue(ic.commitText("d",1));
        assertEquals("App is instakked",ic.getEditable().toString());
        assertEquals("instakked",
                CursorReleaseSuggestionPolicy.classify(
                        ic.getTextBeforeCursor(64,0),
                        ic.getTextAfterCursor(64,0),false,false).word);
    }

    @Test public void betweenWordsCursorRemainsCommittableWithWholeWord() {
        // Move between words, edit one word at its edge, return to end.
        BaseInputConnection ic=fresh("instakke mistake",8);
        assertEquals("e",ic.getTextBeforeCursor(1,0).toString());
        assertEquals(" ",ic.getTextAfterCursor(1,0).toString());
        assertFalse(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","d",false,
                true,false,true,false,false));
        assertTrue(ic.commitText("d",1));
        assertEquals("instakked mistake",ic.getEditable().toString());
        assertEquals("instakked",
                CursorReleaseSuggestionPolicy.classify(
                        ic.getTextBeforeCursor(64,0),
                        ic.getTextAfterCursor(64,0),false,false).word);
        int end=ic.getEditable().length();
        Selection.setSelection(ic.getEditable(),end);
        assertEquals("e",ic.getTextBeforeCursor(1,0).toString()); // mistake
        assertTrue(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","s",false,
                false,false,true,false,false));
        assertTrue(ic.commitText("s",1));
        assertEquals("instakked mistakes",ic.getEditable().toString());
        assertEquals("mistakes",
                CursorReleaseSuggestionPolicy.classify(
                        ic.getTextBeforeCursor(64,0),
                        ic.getTextAfterCursor(64,0),false,false).word);
    }

    @Test public void aSpaceResetsToNormalFastComposingPath() {
        BaseInputConnection ic=fresh("instakke ",9);
        assertFalse(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","n",false,
                false,false,true,false,false));
        assertTrue(ic.setComposingText("n",1));
        assertEquals("instakke n",ic.getEditable().toString());
    }

    @Test public void selectionOrPrivateEditorNeverEntersNewRecoveryState() {
        BaseInputConnection ic=fresh("instakke mistake",8);
        assertFalse(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","d",true,false,
                false,true,false,false));
        assertFalse(CommittedWordBoundary360.resumeWholeWordCorrection(
                ic.getTextBeforeCursor(1,0),"","d",false,false,
                true,true,false,false));
    }

    @Test public void cursorWordReplacementUsesOneCheckedTransaction() {
        CursorWordCorrectionPolicy.Plan replacement=
                CursorWordCorrectionPolicy.plan(
                        "App is instakked","",
                        "instakked","installed",false,false);
        assertNotNull(replacement);
        assertEquals(9,replacement.lettersBefore);
        assertEquals(0,replacement.lettersAfter);
        assertEquals(" ",replacement.trailingSeparator);
        BaseInputConnection ic=fresh("App is instakked",16);
        assertTrue(ic.deleteSurroundingText(replacement.lettersBefore,0));
        assertTrue(ic.commitText(replacement.replacement,1));
        assertEquals("App is installed",ic.getEditable().toString());
    }
}
