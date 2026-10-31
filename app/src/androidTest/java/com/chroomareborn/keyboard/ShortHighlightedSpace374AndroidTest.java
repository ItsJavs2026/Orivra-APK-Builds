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

/** Real editor transactions with synthetic short words; no device-UI claim. */
@RunWith(AndroidJUnit4.class)
public final class ShortHighlightedSpace374AndroidTest {
    private static BaseInputConnection editor(String text) {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(ctx),true);
        Editable editable=ic.getEditable();
        editable.replace(0,editable.length(),text);
        Selection.setSelection(editable,editable.length());
        return ic;
    }
    private static SentenceSuggestionCommit.Editor bridge(BaseInputConnection ic) {
        return new SentenceSuggestionCommit.Editor() {
            @Override public CharSequence before(int n) {
                return ic.getTextBeforeCursor(n,0);
            }
            @Override public CharSequence after(int n) {
                return ic.getTextAfterCursor(n,0);
            }
            @Override public void beginBatch(){ic.beginBatchEdit();}
            @Override public boolean commit(String value){
                return ic.commitText(value,1);
            }
            @Override public void endBatch(){ic.endBatchEdit();}
        };
    }
    private static boolean allowed(String input,String output) {
        return HighlightedSpace372.eligible(
                input,input,output,true,true,true,true,true,
                false,false,false,false,false,false,false);
    }
    @Test public void oneLetterHighlightedPrefixCompletesWithOneSpace() {
        assertTrue(allowed("k","keyboard"));
        BaseInputConnection ic=editor("k");
        assertTrue(ic.setComposingRegion(0,1));
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge(ic),"k","keyboard"));
        assertEquals("keyboard ",ic.getEditable().toString());
    }
    @Test public void twoAndThreeLetterHighlightedPrefixesComplete() {
        for(String fragment:new String[]{"ke","key"}) {
            assertTrue(allowed(fragment,"keyboard"));
            BaseInputConnection ic=editor(fragment);
            assertTrue(ic.setComposingRegion(0,fragment.length()));
            assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                    SentenceSuggestionCommit.insert(bridge(ic),fragment,"keyboard"));
            assertEquals("keyboard ",ic.getEditable().toString());
        }
    }
    @Test public void existingRightSpaceIsNeverDuplicated() {
        assertTrue(allowed("ke","keyboard"));
        BaseInputConnection ic=editor("ke next");
        Selection.setSelection(ic.getEditable(),2);
        assertTrue(ic.setComposingRegion(0,2));
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge(ic),"ke","keyboard"));
        assertEquals("keyboard next",ic.getEditable().toString());
        assertFalse(ic.getEditable().toString().contains("  "));
    }
    @Test public void noHighlightOrLateFrameNeverTriggersReplacement() {
        assertFalse(HighlightedSpace372.eligible("ke","ke","keyboard",
                false,true,true,true,true,
                false,false,false,false,false,false,false));
        assertFalse(HighlightedSpace372.eligible("ke","ke","keyboard",
                true,false,true,true,true,
                false,false,false,false,false,false,false));
    }
    @Test public void sameEditorAndPrivacyRequirementsRemainEnforced() {
        assertFalse(HighlightedSpace372.eligible("ke","ke","keyboard",
                true,true,false,true,true,
                false,false,false,false,false,false,false));
        assertFalse(HighlightedSpace372.eligible("ke","ke","keyboard",
                true,true,true,false,true,
                false,false,false,false,false,false,false));
        assertFalse(HighlightedSpace372.eligible("ke","ke","keyboard",
                true,true,true,true,true,
                true,false,false,false,false,false,false));
    }
}
