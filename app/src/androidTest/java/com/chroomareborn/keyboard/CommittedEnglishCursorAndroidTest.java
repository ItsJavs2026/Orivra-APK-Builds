package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/** Android EditText safety integration with synthetic Google-search-like text.
 * No interaction with real Google app, sensitive user text or the web.
 */
@RunWith(AndroidJUnit4.class)
public final class CommittedEnglishCursorAndroidTest {
    @Test public void searchWordHeloOffersOptionalCorrectionNotImplicitEdit() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            EditText e=new EditText(c);
            e.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            e.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            e.setText("helo");
            e.setSelection(e.length());
            EditorInfo info=new EditorInfo();
            InputConnection ic=e.onCreateInputConnection(info);
            assertNotNull(ic);
            var decision=RebornImeService.editorPrivacyDecision(info);
            assertTrue(decision.allowSuggestions);
            assertFalse(decision.allowPersonalLearning);
            assertTrue(CommittedEnglishCursorPolicy.eligible(
                    "helo",false,false,false,false,decision.sensitive,
                    decision.allowSuggestions));
            List<String> choices=CommittedEnglishCursorPolicy.merge(
                    "helo",PredictionLite.editingSuggestions("helo"));
            assertEquals("helo",choices.get(0));
            assertTrue("ordinary correction available",choices.contains("hello"));
            assertEquals("nothing changed without tap","helo",e.getText().toString());
            CharSequence before=ic.getTextBeforeCursor(64,0);
            CharSequence after=ic.getTextAfterCursor(64,0);
            var ticket=CursorEditSnapshotGuard.capture("helo",before,after,1,
                    e.getSelectionStart(),e.getSelectionEnd(),
                    false,false,false);
            assertNotNull(ticket);
            var plan=CursorWordCorrectionPolicy.plan(before,after,"helo",
                    "hello",false,false);
            assertNotNull(plan);
            assertTrue(ic.deleteSurroundingText(plan.lettersBefore,plan.lettersAfter));
            assertTrue(ic.commitText(plan.replacement,1));
            assertEquals("hello",e.getText().toString());
            assertFalse("stale suggestion invalid after edit",
                    CursorEditSnapshotGuard.current(ticket,
                    ic.getTextBeforeCursor(64,0),ic.getTextAfterCursor(64,0),1,
                    e.getSelectionStart(),e.getSelectionEnd(),
                    false,false,false));
        });
    }
}
