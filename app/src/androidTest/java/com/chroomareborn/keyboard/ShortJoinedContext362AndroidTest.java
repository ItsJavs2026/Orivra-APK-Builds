package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android 35 prediction, editor and explicit Space behavior.
 * No user text is stored; scenarios are all synthetic examples.
 */
@RunWith(AndroidJUnit4.class)
public final class ShortJoinedContext362AndroidTest {
    @Test public void screenshotExactInputSuggestsIWill() {
        List<String> proposals=PredictionLite.suggestions("Iwikk");
        assertFalse(proposals.isEmpty());
        assertEquals("I will",proposals.get(0));
        assertTrue(proposals.contains("Iwikk"));
    }
    @Test public void exactJoinedWordsReceiveSafeSpaceOnlyOnTap() {
        assertEquals("I will",PredictionLite.suggestions("Iwill").get(0));
        assertEquals("I am",PredictionLite.suggestions("Iam").get(0));
        assertEquals("we can",PredictionLite.suggestions("wecan").get(0));
    }
    @Test public void correctlySpelledWordsAreNotRewritten() {
        assertEquals("will",PredictionLite.suggestions("will").get(0));
        assertFalse(PredictionLite.suggestions("Iwikk2").contains("I will"));
    }
    @Test public void acceptingIWillReplacesCompositionExactlyOnce() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(ctx),true);
        Editable editor=ic.getEditable();
        editor.clear();
        Selection.setSelection(editor,0);
        assertTrue(ic.commitText("Installed! ",1));
        assertTrue(ic.setComposingText("Iwikk",1));
        assertEquals("Installed! Iwikk",editor.toString());
        SentenceSuggestionCommit.Editor bridge=new SentenceSuggestionCommit.Editor(){
            @Override public CharSequence before(int n){return ic.getTextBeforeCursor(n,0);}
            @Override public CharSequence after(int n){return ic.getTextAfterCursor(n,0);}
            @Override public void beginBatch(){ic.beginBatchEdit();}
            @Override public boolean commit(String text){return ic.commitText(text,1);}
            @Override public void endBatch(){ic.endBatchEdit();}
        };
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge,"Iwikk","I will"));
        assertEquals("Installed! I will ",editor.toString());
        assertFalse(editor.toString().contains("  "));
    }
    @Test public void existingSpaceOrClosingMarkIsNotDuplicated() {
        // The candidate itself contains a single word boundary.
        // Its accepted trailing separator is separately handled by the
        // existing Candidate 354 spaceAfter policy.
        assertEquals("",SuggestionContinuity353.spaceAfter(" "));
        assertEquals("",SuggestionContinuity353.spaceAfter("!"));
        assertEquals(" ",SuggestionContinuity353.spaceAfter(""));
    }
}
