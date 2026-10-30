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

import static org.junit.Assert.*;

/**
 * Framework-backed EditText/InputConnection tests for Google-search-like
 * editor flags. Uses synthetic nonprivate strings and no external Google app.
 */
@RunWith(AndroidJUnit4.class)
public final class SearchEditorActionsAndroidTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test public void searchTapInvokesHostActionInsteadOfInsertingNewline() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            EditText field=new EditText(context());
            field.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            field.setText("keyboard live touch wallpaper");
            field.setSelection(field.length());
            int[] calls={0};
            int[] lastAction={-1};
            field.setOnEditorActionListener((view,actionId,event)->{
                calls[0]++;
                lastAction[0]=actionId;
                return true;
            });
            EditorInfo info=new EditorInfo();
            InputConnection connection=field.onCreateInputConnection(info);
            assertNotNull(connection);
            assertEquals("Search is a declared IME action",
                    EditorInfo.IME_ACTION_SEARCH,
                    EnterKeyPolicy.tapEditorAction(info.imeOptions));
            assertTrue("performEditorAction delegates to host listener",
                    connection.performEditorAction(
                            EnterKeyPolicy.tapEditorAction(info.imeOptions)));
            assertEquals("host must receive one Search",1,calls[0]);
            assertEquals(EditorInfo.IME_ACTION_SEARCH,lastAction[0]);
            assertEquals("Search must never add newline or change query",
                    "keyboard live touch wallpaper",field.getText().toString());
        });
    }

    @Test public void sendIsStillHeldAndSearchActionRespectsNoEnterAction(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            EditText field=new EditText(context());
            field.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setImeOptions(EditorInfo.IME_ACTION_SEND);
            EditorInfo info=new EditorInfo();
            InputConnection connection=field.onCreateInputConnection(info);
            assertNotNull(connection);
            assertEquals("SEND must not activate on an ordinary tap",
                    0,EnterKeyPolicy.tapEditorAction(info.imeOptions));
            assertTrue("SEND remains available only via hold",
                    EnterKeyPolicy.supportsLongPressSend(info.imeOptions));
            field.setImeOptions(EditorInfo.IME_ACTION_SEARCH |
                    EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            EditorInfo restricted=new EditorInfo();
            assertNotNull(field.onCreateInputConnection(restricted));
            assertEquals(0,EnterKeyPolicy.tapEditorAction(restricted.imeOptions));
        });
    }

    @Test public void noSuggestionsDoesNotBlockDeleteOrSpacebarCursor() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            EditText field=new EditText(context());
            field.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            field.setText("keyboard search");
            field.setSelection(field.length());
            EditorInfo info=new EditorInfo();
            info.packageName="com.google.android.googlequicksearchbox";
            InputConnection connection=field.onCreateInputConnection(info);
            assertNotNull(connection);
            var privacy=RebornImeService.editorPrivacyDecision(info);
            assertFalse("ordinary search is not a private password",privacy.sensitive);
            assertTrue("explicit Search still permits ephemeral predictions",
                    privacy.allowSuggestions);
            assertFalse("NO_SUGGESTIONS must not learn",privacy.allowPersonalLearning);
            CharSequence before=connection.getTextBeforeCursor(64,0);
            CharSequence after=connection.getTextAfterCursor(64,0);
            assertNotNull(before);
            assertNotNull(after);
            var ticket=BackspaceSwipeSnapshotGuard.capture(before,null,after,false,1);
            assertNotNull("Backspace swipe can safely snapshot normal search",ticket);
            assertTrue(BackspaceSwipeSnapshotGuard.isCurrent(ticket,
                    before,null,after,false,1));

            SpacebarCursorGesture gesture=new SpacebarCursorGesture(12,16,28);
            gesture.start(100,20);
            int steps=gesture.move(68,20);
            assertTrue(gesture.isNavigating());
            assertTrue("horizontal swipe should request movement",steps<0);
            assertFalse("cursor drag must not insert text",gesture.finish());
            int newPosition=CursorMovementPolicy.move(field.getText(),
                    field.getSelectionStart(),steps);
            assertTrue(connection.setSelection(newPosition,newPosition));
            assertEquals("spacebar cursor movement must not edit query text",
                    "keyboard search",field.getText().toString());
            assertEquals(newPosition,field.getSelectionStart());
            assertTrue("Backspace should delete at the selected position",
                    connection.deleteSurroundingTextInCodePoints(1,0));
            String original="keyboard search";
            assertEquals("single Backspace must delete one character at live caret",
                    original.substring(0,newPosition-1)+original.substring(newPosition),
                    field.getText().toString());
        });
    }
}
