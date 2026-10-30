package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/** Real Android EditText/InputConnection behavior, synthetic strings only. */
@RunWith(AndroidJUnit4.class)
public final class AmbiguousContractionEditorAndroidTest {
    private void caseAtCaret(String original,String chosen){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        EditText field=new EditText(c);
        String sentence="Hello there. "+original;
        field.setText(sentence);
        field.setSelection(field.length());
        EditorInfo info=new EditorInfo();
        InputConnection connection=field.onCreateInputConnection(info);
        assertNotNull("Editor must provide input connection",connection);
        CharSequence left=connection.getTextBeforeCursor(64,0);
        CharSequence right=connection.getTextAfterCursor(64,0);
        assertNotNull(left);
        assertNotNull(right);

        List<String> options=CursorWordCorrectionPolicy.safeOptions(original,
                EnglishApostrophePolicy.suggestions(original));
        assertEquals("original must remain tappable",original,options.get(0));
        assertTrue("manual alternative visible for "+original,options.contains(chosen));
        CursorEditSnapshotGuard.Ticket ticket=CursorEditSnapshotGuard.capture(
                original,left,right,1,field.getSelectionStart(),field.getSelectionEnd(),
                false,false,false);
        assertNotNull("snapshot allows verified explicit tap",ticket);
        assertTrue(CursorEditSnapshotGuard.current(ticket,left,right,1,
                field.getSelectionStart(),field.getSelectionEnd(),
                false,false,false));
        assertEquals("nothing changes until user taps",sentence,field.getText().toString());

        CursorWordCorrectionPolicy.Plan plan=CursorWordCorrectionPolicy.plan(
                connection.getTextBeforeCursor(64,0),connection.getTextAfterCursor(64,0),
                original,chosen,false,false);
        assertNotNull("original whole word can be replaced safely",plan);
        connection.beginBatchEdit();
        try{
            assertTrue("delete original whole word",
                    connection.deleteSurroundingText(plan.lettersBefore,plan.lettersAfter));
            assertTrue("commit only the tapped replacement",
                    connection.commitText(plan.replacement,1));
        } finally {
            connection.endBatchEdit();
        }
        assertEquals("Hello there. "+chosen,field.getText().toString());
        assertFalse("old caret ticket invalid after actual correction",
                CursorEditSnapshotGuard.current(ticket,
                        connection.getTextBeforeCursor(64,0),
                        connection.getTextAfterCursor(64,0),1,
                        field.getSelectionStart(),field.getSelectionEnd(),
                        false,false,false));
    }

    @Test public void punctuationContextHowsConvertsOnlyOnTap() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                ()->caseAtCaret("hows","how's"));
    }
    @Test public void validWereIsNeverSilentlyChanged() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                ()->caseAtCaret("Were","We're"));
    }
    @Test public void allCapsRemainAUserChoice() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                ()->caseAtCaret("WERE","WE'RE"));
    }
}
