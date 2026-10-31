package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
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

/** Android 35 input transactions and pure suggestion gesture policies.
 * Does not claim to reproduce touch ergonomics on Samsung Android 17.
 */
@RunWith(AndroidJUnit4.class)
public final class PredictionGestures372AndroidTest {
    private static BaseInputConnection editor(String initial) {
        Context context=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(context),true);
        Editable editable=ic.getEditable();
        editable.replace(0,editable.length(),initial);
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
            @Override public boolean commit(String s){return ic.commitText(s,1);}
            @Override public void endBatch(){ic.endBatchEdit();}
        };
    }
    @Test public void screenshotTypoSpaceAcceptsVisibleCorrectionOnce() {
        assertTrue(SuggestionVisualStyle.highlight("tekk","tell",1,1));
        assertTrue(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,false,false,false,false));
        BaseInputConnection ic=editor("Installed! tekk");
        assertTrue(ic.setComposingRegion(11,15));
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge(ic),"tekk","tell"));
        assertEquals("Installed! tell ",ic.getEditable().toString());
        assertFalse(ic.getEditable().toString().contains("  "));
    }
    @Test public void joinedTypoSpaceCreatesRealSingleWordBoundary() {
        assertTrue(HighlightedSpace372.eligible("Iwikk","Iwikk","I will",
                true,true,true,true,true,false,false,false,false,false,false,false));
        BaseInputConnection ic=editor("Iwikk");
        assertTrue(ic.setComposingRegion(0,5));
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge(ic),"Iwikk","I will"));
        assertEquals("I will ",ic.getEditable().toString());
    }
    @Test public void noValidHighlightedWordKeepsNormalSpace() {
        assertFalse(HighlightedSpace372.eligible("good","good","good",
                true,true,true,true,true,false,false,false,false,false,false,false));
        BaseInputConnection ic=editor("good");
        assertTrue(ic.commitText(" ",1));
        assertEquals("good ",ic.getEditable().toString());
    }
    @Test public void leftRightAndUpResolveOnlyOnVerifiedRelease() {
        List<String> strip=List.of("tekk","tell","test");
        List<String> rise=List.of("tell me","tell us");
        assertEquals(PredictionSwipe372.Direction.LEFT,
                PredictionSwipe372.direction(-70,0,12,true));
        assertEquals("tekk",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.LEFT,1,strip,rise,false));
        assertEquals("test",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.RIGHT,1,strip,rise,false));
        assertEquals("tell me",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.UP,1,strip,rise,false));
        assertFalse(PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,false,false,true,false,false,false,true));
        assertTrue(PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,false,false,false,true));
    }
    @Test public void privateAndNumericEditorsNeverAcceptSpaceCorrection() {
        assertFalse(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,true,false,false,false,false,false,false));
        assertFalse(HighlightedSpace372.eligible("tekk","tekk","tell",
                true,true,true,true,true,false,false,false,false,true,false,false));
        assertFalse(PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,true,true,false,false,false,true));
    }
    @Test public void independentOnOffPreferencesPersistWithoutInputText() {
        Context context=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        SharedPreferences prefs=context.getSharedPreferences("keyboard_feedback",
                Context.MODE_PRIVATE);
        boolean oldSwipe=prefs.getBoolean(PredictionSwipe372.PREF,true);
        boolean oldSpace=prefs.getBoolean(HighlightedSpace372.PREF,true);
        try {
            prefs.edit().putBoolean(PredictionSwipe372.PREF,false)
                    .putBoolean(HighlightedSpace372.PREF,true).commit();
            assertFalse(prefs.getBoolean(PredictionSwipe372.PREF,true));
            assertTrue(prefs.getBoolean(HighlightedSpace372.PREF,false));
            prefs.edit().putBoolean(PredictionSwipe372.PREF,true)
                    .putBoolean(HighlightedSpace372.PREF,false).commit();
            assertTrue(prefs.getBoolean(PredictionSwipe372.PREF,false));
            assertFalse(prefs.getBoolean(HighlightedSpace372.PREF,true));
        } finally {
            prefs.edit().putBoolean(PredictionSwipe372.PREF,oldSwipe)
                    .putBoolean(HighlightedSpace372.PREF,oldSpace).commit();
        }
    }
}
