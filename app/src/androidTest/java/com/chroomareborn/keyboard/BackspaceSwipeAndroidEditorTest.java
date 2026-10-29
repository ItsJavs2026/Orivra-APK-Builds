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
 * Android EditText / InputConnection integration, distinct from the desktop
 * pure-Java simulation. Uses only disposable fixture strings on an emulator.
 * Does NOT activate Orivra as the device keyboard or test Samsung Android 17.
 */
@RunWith(AndroidJUnit4.class)
public final class BackspaceSwipeAndroidEditorTest {
    private static final class Fixture {
        final EditText view;
        final InputConnection ic;
        Fixture(String initial) {
            view=new EditText(InstrumentationRegistry.getInstrumentation().getTargetContext());
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            view.setText(initial);
            view.setSelection(view.length());
            view.requestFocus();
            ic=view.onCreateInputConnection(new EditorInfo());
            assertNotNull("EditText must supply a real InputConnection",ic);
        }
        BackspaceSwipeSnapshotGuard.Ticket ticket(int words) {
            return BackspaceSwipeSnapshotGuard.capture(
                    ic.getTextBeforeCursor(2048,0),ic.getSelectedText(0),
                    ic.getTextAfterCursor(96,0),
                    view.getSelectionStart()!=view.getSelectionEnd(),words);
        }
        BackspaceSwipeEditorTransaction.Editor adapter() {
            return new BackspaceSwipeEditorTransaction.Editor() {
                @Override public boolean sameSession() { return true; }
                @Override public boolean selectionExpected() {
                    return view.getSelectionStart()!=view.getSelectionEnd();
                }
                @Override public CharSequence before(int size) {
                    return ic.getTextBeforeCursor(size,0);
                }
                @Override public CharSequence selected() {
                    return ic.getSelectedText(0);
                }
                @Override public CharSequence after(int size) {
                    return ic.getTextAfterCursor(size,0);
                }
                @Override public void beginBatchEdit() {ic.beginBatchEdit();}
                @Override public boolean finishComposition() {return ic.finishComposingText();}
                @Override public boolean deleteBeforeCursor(int count) {
                    return ic.deleteSurroundingText(count,0);
                }
                @Override public boolean deleteSelection() {
                    return ic.commitText("",1);
                }
                @Override public void endBatchEdit() {ic.endBatchEdit();}
            };
        }
        String text() {return view.getText().toString();}
    }
    private static void onMain(Runnable test) {
        AtomicReference<Throwable> thrown=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {test.run();}
            catch(Throwable t) {thrown.set(t);}
        });
        if(thrown.get()!=null)throw new AssertionError("Android editor integration failed",thrown.get());
    }
    @Test public void composedLikethisDeletesCompleteToken() {
        onMain(()->{
            Fixture fixture=new Fixture("");
            assertTrue("composing text must be accepted",
                    fixture.ic.setComposingText("likethis",1));
            assertEquals("likethis",fixture.text());
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            assertNotNull("composing word must be visible before cursor",ticket);
            assertEquals("likethis",BackspaceSwipeSnapshotGuard.previewText(ticket,38));
            assertEquals(BackspaceSwipeEditorTransaction.Result.APPLIED,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,"likethis"));
            assertEquals("",fixture.text());
        });
    }
    @Test public void composedHeythereDeletesCompleteToken() {
        onMain(()->{
            Fixture fixture=new Fixture("");
            assertTrue(fixture.ic.setComposingText("heythere",1));
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            assertNotNull(ticket);
            assertEquals("heythere",BackspaceSwipeSnapshotGuard.previewText(ticket,38));
            assertEquals(BackspaceSwipeEditorTransaction.Result.APPLIED,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,"heythere"));
            assertEquals("",fixture.text());
        });
    }
    @Test public void committedLikethisDeletesCompleteToken() {
        onMain(()->{
            Fixture fixture=new Fixture("hello likethis");
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            assertEquals("likethis",BackspaceSwipeSnapshotGuard.previewText(ticket,38));
            assertEquals(BackspaceSwipeEditorTransaction.Result.APPLIED,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,""));
            assertEquals("hello ",fixture.text());
        });
    }
    @Test public void selectionDeletesOnlyHighlightedRange() {
        onMain(()->{
            Fixture fixture=new Fixture("before like this after");
            fixture.view.setSelection(7,16);
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            assertNotNull(ticket);
            assertEquals("like this",BackspaceSwipeSnapshotGuard.previewText(ticket,38));
            assertEquals(BackspaceSwipeEditorTransaction.Result.APPLIED,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,""));
            assertEquals("before  after",fixture.text());
        });
    }
    @Test public void changedCursorRejectsDelayedDeletion() {
        onMain(()->{
            Fixture fixture=new Fixture("likethis");
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            fixture.view.setSelection(3);
            assertEquals(BackspaceSwipeEditorTransaction.Result.STALE,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,""));
            assertEquals("likethis",fixture.text());
        });
    }
    @Test public void changedDraftRejectsDelayedDeletion() {
        onMain(()->{
            Fixture fixture=new Fixture("likethis");
            BackspaceSwipeSnapshotGuard.Ticket ticket=fixture.ticket(1);
            fixture.view.getText().append("!");
            fixture.view.setSelection(fixture.view.length());
            assertEquals(BackspaceSwipeEditorTransaction.Result.STALE,
                    BackspaceSwipeEditorTransaction.apply(
                            fixture.adapter(),ticket,1,""));
            assertEquals("likethis!",fixture.text());
        });
    }
}
