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

/** Candidate research 351. No actual user text, logs or persisted data. */
@RunWith(AndroidJUnit4.class)
public final class SelectionCommitRecovery351AndroidTest {
    static final class Fake implements SelectionCommitRecovery351.Editor {
        String left="say ",right=" later",selected="hello";
        boolean connected=true,finishChangesCaret=false,finishSwitchesSession=false,
                commitWorks=true,returnsSuccess=true,commitChangesSuffix=false,
                hideSelection=false;
        int finishes,commits;
        @Override public boolean sameSession(){return connected;}
        @Override public CharSequence before(){return left;}
        @Override public CharSequence after(){return right;}
        @Override public CharSequence selectedText(){
            return hideSelection?null:selected;
        }
        @Override public boolean finishComposing() {
            finishes++;
            if(!selected.isEmpty()) {left+=selected; selected="";}
            if(finishChangesCaret)left+="x";
            if(finishSwitchesSession)connected=false;
            return true;
        }
        @Override public boolean commitOnce(String value) {
            commits++;
            if(commitWorks){
                left+=value;
                selected="";
                if(commitChangesSuffix)right=" elsewhere";
            }
            return returnsSuccess;
        }
    }

    @Test public void selectedWordReplacedOnceWithoutFinishingFirst(){
        Fake e=new Fake();
        assertEquals(SelectionCommitRecovery351.Result.APPLIED,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals("say a",e.left);
        assertEquals(" later",e.right);
        assertEquals("",e.selected);
        assertEquals(0,e.finishes);
        assertEquals(1,e.commits);
    }

    @Test public void editorFalseAfterApplyingReplacementNeverRetries(){
        Fake e=new Fake();e.returnsSuccess=false;
        assertEquals(SelectionCommitRecovery351.Result.APPLIED,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals("say a",e.left);
        assertEquals(1,e.commits);
        assertEquals(0,e.finishes);
    }

    @Test public void rejectedReplacementDoesNotAttemptSecondCommit(){
        Fake e=new Fake();e.returnsSuccess=false;e.commitWorks=false;
        assertEquals(SelectionCommitRecovery351.Result.STALE,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals("say ",e.left);
        assertEquals("hello",e.selected);
        assertEquals(1,e.commits);
        assertEquals(0,e.finishes);
    }

    @Test public void knownSelectionWithMissingRemoteSelectionFailsClosed(){
        Fake e=new Fake();e.hideSelection=true;
        assertEquals(SelectionCommitRecovery351.Result.STALE,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals(0,e.finishes);
        assertEquals(0,e.commits);
    }

    @Test public void noSelectionAtNewCaretFinalizesThenCommitsOnce(){
        Fake e=new Fake();e.selected="";
        assertEquals(SelectionCommitRecovery351.Result.APPLIED,
                SelectionCommitRecovery351.recover(e,false,"a"));
        assertEquals("say a",e.left);
        assertEquals(1,e.finishes);
        assertEquals(1,e.commits);
    }

    @Test public void remoteFinishUnexpectedlyMovesCaretSoDoNotCommit(){
        Fake e=new Fake();e.selected="";e.finishChangesCaret=true;
        assertEquals(SelectionCommitRecovery351.Result.STALE,
                SelectionCommitRecovery351.recover(e,false,"a"));
        assertEquals(1,e.finishes);
        assertEquals(0,e.commits);
    }

    @Test public void editorSwitchDuringFinalizeDoesNotCommit(){
        Fake e=new Fake();e.selected="";e.finishSwitchesSession=true;
        assertEquals(SelectionCommitRecovery351.Result.STALE,
                SelectionCommitRecovery351.recover(e,false,"a"));
        assertEquals(0,e.commits);
    }

    @Test public void mutatedSuffixNeverReportsVerifiedReplacement(){
        Fake e=new Fake();e.commitChangesSuffix=true;
        assertEquals(SelectionCommitRecovery351.Result.STALE,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals(1,e.commits);
        assertEquals(0,e.finishes);
    }

    @Test public void ordinarySelectedEditNeverFinalizesAheadOfReplacement(){
        assertFalse(SelectionCommitRecovery351.finishBeforeTypedReplacement(true));
        assertTrue(SelectionCommitRecovery351.finishBeforeTypedReplacement(false));
    }

    @Test public void actualAndroidBaseInputConnectionReplacesHighlightedWord(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(c),true);
        Editable editable=ic.getEditable();
        editable.replace(0,editable.length(),"say hello later");
        Selection.setSelection(editable,4,9);
        assertEquals("hello",ic.getSelectedText(0).toString());
        SelectionCommitRecovery351.Editor e=new SelectionCommitRecovery351.Editor(){
            @Override public boolean sameSession(){return true;}
            @Override public CharSequence before(){
                return ic.getTextBeforeCursor(SelectionCommitRecovery351.WINDOW,0);
            }
            @Override public CharSequence after(){
                return ic.getTextAfterCursor(SelectionCommitRecovery351.WINDOW,0);
            }
            @Override public CharSequence selectedText(){return ic.getSelectedText(0);}
            @Override public boolean finishComposing(){return ic.finishComposingText();}
            @Override public boolean commitOnce(String value){return ic.commitText(value,1);}
        };
        assertEquals(SelectionCommitRecovery351.Result.APPLIED,
                SelectionCommitRecovery351.recover(e,true,"a"));
        assertEquals("say a later",editable.toString());
        assertEquals(5,Selection.getSelectionStart(editable));
        assertEquals(5,Selection.getSelectionEnd(editable));
    }
}
