package com.chroomareborn.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/**
 * Android 35 real EditText/InputConnection apostrophe composition.
 * Synthetic strings only. This does not claim Samsung IME hardware testing.
 */
@RunWith(AndroidJUnit4.class)
public final class ApostropheComposingAndroidTest {
    private static void onMain(Runnable block) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {block.run();}catch(Throwable bad){failure.set(bad);}
        });
        if(failure.get()!=null)throw new AssertionError("Android composed contraction failed",failure.get());
    }
    private static final class Fixture {
        final EditText edit;
        final InputConnection ic;
        Fixture(String text){
            edit=new EditText(InstrumentationRegistry.getInstrumentation().getTargetContext());
            edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            edit.setText(text);
            edit.setSelection(edit.length());
            edit.requestFocus();
            ic=edit.onCreateInputConnection(new EditorInfo());
            assertNotNull(ic);
        }
        String text(){return edit.getText().toString();}
    }
    @Test public void apostropheInsideComposingSpanDoesNotSplitWord(){
        onMain(()->{
            Fixture fixture=new Fixture("");
            assertTrue(fixture.ic.setComposingText("can",1));
            assertTrue(EnglishApostrophePolicy.canAppendApostrophe("can","'"));
            assertTrue(fixture.ic.setComposingText("can'",1));
            assertEquals("can'",fixture.text());
            assertTrue(fixture.ic.setComposingText("can't",1));
            assertEquals("can't",fixture.text());
            assertTrue(fixture.ic.finishComposingText());
            assertTrue(fixture.ic.commitText(" ",1));
            assertEquals("can't ",fixture.text());
            assertTrue(fixture.ic.setComposingText("ain",1));
            assertTrue(fixture.ic.setComposingText("ain\u2019",1));
            assertTrue(fixture.ic.setComposingText("ain\u2019t",1));
            assertEquals("can't ain\u2019t",fixture.text());
            assertTrue(fixture.ic.finishComposingText());
        });
    }
    @Test public void cursorWordCorrectionReplacesFullContractionAndRejectsStale(){
        onMain(()->{
            Fixture fixture=new Fixture("I cant");
            CharSequence before=fixture.ic.getTextBeforeCursor(64,0);
            CursorWordCorrectionPolicy.Plan plan=CursorWordCorrectionPolicy.plan(
                    before,"","cant","can't",false,false);
            assertNotNull(plan);
            assertEquals(4,plan.lettersBefore);
            assertTrue(fixture.ic.deleteSurroundingText(plan.lettersBefore,plan.lettersAfter));
            assertTrue(fixture.ic.commitText(plan.replacement,1));
            assertEquals("I can't",fixture.text());
            assertNull("stale expected string must not delete",
                    CursorWordCorrectionPolicy.plan(
                            fixture.ic.getTextBeforeCursor(64,0),"",
                            "cant","can't",false,false));
            assertNull("private field cannot replace contraction",
                    CursorWordCorrectionPolicy.plan(
                            fixture.ic.getTextBeforeCursor(64,0),"",
                            "can't","can",false,true));
        });
    }
}
