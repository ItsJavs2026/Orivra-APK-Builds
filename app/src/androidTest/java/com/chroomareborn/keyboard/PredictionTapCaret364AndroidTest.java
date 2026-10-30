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

/** Real BaseInputConnection editor transactions with synthetic text.
 * Not a physical Samsung or other-host-app compatibility claim.
 */
@RunWith(AndroidJUnit4.class)
public final class PredictionTapCaret364AndroidTest {
    private static BaseInputConnection editor(String text,int caret) {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(c),true);
        Editable content=ic.getEditable();
        content.replace(0,content.length(),text);
        Selection.setSelection(content,caret);
        return ic;
    }
    private static PredictionTapCaret364.Ticket arm(
            BaseInputConnection ic,String word) {
        return PredictionTapCaret364.arm(
                ic.getTextBeforeCursor(PredictionTapCaret364.sampleLimit(),0),
                ic.getTextAfterCursor(PredictionTapCaret364.sampleLimit(),0),
                word,true,false,false,false,false);
    }
    private static boolean eligible(BaseInputConnection ic,
            PredictionTapCaret364.Ticket ticket,String letter) {
        return PredictionTapCaret364.prefixFirstLetter(
                ticket,
                ic.getTextBeforeCursor(PredictionTapCaret364.sampleLimit(),0),
                ic.getTextAfterCursor(PredictionTapCaret364.sampleLimit(),0),
                letter,true,true,true,false,false,false,false,true);
    }
    private static void tapCursorWord(
            BaseInputConnection ic,String expected,String suggestion) {
        CursorWordCorrectionPolicy.Plan plan=CursorWordCorrectionPolicy.plan(
                ic.getTextBeforeCursor(64,0),
                ic.getTextAfterCursor(64,0),
                expected,suggestion,false,false);
        assertNotNull(plan);
        assertTrue(ic.deleteSurroundingText(plan.lettersBefore,
                plan.lettersAfter));
        assertTrue(ic.commitText(plan.replacement+plan.trailingSeparator,1));
    }

    @Test public void userExampleInsertingNotBetweenAmAndHungry() {
        BaseInputConnection ic=editor("I am hungry",4);
        tapCursorWord(ic,"am","am");
        assertEquals("I am hungry",ic.getEditable().toString());
        PredictionTapCaret364.Ticket ticket=arm(ic,"am");
        assertNotNull(ticket);
        assertTrue(eligible(ic,ticket,"n"));
        assertTrue(ic.commitText(" n",1));
        assertEquals("I am n hungry",ic.getEditable().toString());
        assertTrue(ic.commitText("o",1));
        assertTrue(ic.commitText("t",1));
        assertEquals("I am not hungry",ic.getEditable().toString());
        assertFalse(ic.getEditable().toString().contains("  "));
    }

    @Test public void selectedCorrectedWordInMiddleKeepsOneSpaceBothSides() {
        BaseInputConnection ic=editor("I am hungy",10);
        tapCursorWord(ic,"hungy","hungry");
        assertEquals("I am hungry ",ic.getEditable().toString());
        assertNull(arm(ic,"hungry"));
        // Already at end: normal next letter should start after existing
        // trailing space, with no injected duplicate left boundary.
        assertTrue(ic.commitText("n",1));
        assertEquals("I am hungry n",ic.getEditable().toString());
    }

    @Test public void aNewWordAfterExistingCorrectedWordAndSpaceWorks() {
        BaseInputConnection ic=editor("my car works",6);
        tapCursorWord(ic,"car","car");
        assertNotNull(arm(ic,"car"));
        PredictionTapCaret364.Ticket ticket=arm(ic,"car");
        assertTrue(eligible(ic,ticket,"n"));
        assertTrue(ic.commitText(" n",1));
        assertTrue(ic.commitText("o",1));
        assertTrue(ic.commitText("w",1));
        assertEquals("my car now works",ic.getEditable().toString());
    }

    @Test public void movingCaretAfterTapRejectsStaleInsertionBoundary() {
        BaseInputConnection ic=editor("I am hungry",4);
        tapCursorWord(ic,"am","am");
        PredictionTapCaret364.Ticket ticket=arm(ic,"am");
        assertNotNull(ticket);
        Selection.setSelection(ic.getEditable(),2);
        assertFalse(eligible(ic,ticket,"n"));
        // A normal typed key at a moved caret must not insert speculative
        // spaces or alter the previous word without an explicit tap.
        assertTrue(ic.commitText("n",1));
        assertFalse(ic.getEditable().toString().contains("  "));
    }

    @Test public void deliberateNewlinePunctuationAndAlreadyDoubledSpaceNeverArmed() {
        BaseInputConnection ic=editor("I am, hungry",4);
        tapCursorWord(ic,"am","am");
        assertNull(arm(ic,"am"));
        BaseInputConnection nl=editor("I am\nhungry",4);
        tapCursorWord(nl,"am","am");
        assertNull(arm(nl,"am"));
        BaseInputConnection twice=editor("I am  hungry",4);
        tapCursorWord(twice,"am","am");
        assertNull(arm(twice,"am"));
    }
}
