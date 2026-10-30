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

/** Reproducible Android + simulated misbehaving-editor composition checks. */
@RunWith(AndroidJUnit4.class)
public final class ComposingMutation340AndroidTest {
    static final class Fake implements ComposingMutation340.Editor {
        String text="hello", composing="hello";
        boolean live=true, mutation=true, returnsSuccess=true, duplicate=false;
        boolean detachOnSet=false;
        int sets,finishes,commits;
        @Override public boolean sameSession(){return live;}
        @Override public CharSequence before(){
            return text.length()>256?text.substring(text.length()-256):text;
        }
        @Override public boolean setComposing(String next){
            sets++;
            if(mutation) {
                String base=text.substring(0,text.length()-composing.length());
                text=base+next;
                if(duplicate)text=text+next.substring(next.length()-1);
                composing=next;
            }
            if(detachOnSet)live=false;
            return returnsSuccess;
        }
        @Override public boolean finishComposing() {
            finishes++;
            composing="";
            return true;
        }
        @Override public boolean commit(String addition){
            commits++;
            text=text+addition;
            return true;
        }
    }
    @Test public void falseReturnAfterWritingDoesNotDoubleType(){
        Fake e=new Fake();e.returnsSuccess=false;
        assertEquals(ComposingMutation340.Result.COMMITTED,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("helloa",e.text);
        assertEquals(1,e.sets);
        assertEquals(0,e.commits);
    }
    @Test public void falseReturnWithoutWritingGetsOneGuardedCommit(){
        Fake e=new Fake();e.returnsSuccess=false;e.mutation=false;
        assertEquals(ComposingMutation340.Result.COMMITTED,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("helloa",e.text);
        assertEquals(1,e.commits);
        assertEquals(1,e.finishes);
    }
    @Test public void staleCaretDoesNotWrite(){
        Fake e=new Fake();e.text="hello different";e.composing="";
        assertEquals(ComposingMutation340.Result.PRECONDITION,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals(0,e.sets);
        assertEquals(0,e.commits);
    }
    @Test public void unexpectedDoubleMutationIsNeverRetried(){
        Fake e=new Fake();e.returnsSuccess=false;e.duplicate=true;
        assertEquals(ComposingMutation340.Result.STALE,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("helloaa",e.text);
        assertEquals(0,e.commits);
    }
    @Test public void lateSessionInvalidatesEdit(){
        Fake e=new Fake();e.detachOnSet=true;
        assertEquals(ComposingMutation340.Result.STALE,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals(0,e.commits);
    }
    @Test public void affirmativeButInvisibleResultCannotTriggerRetry(){
        Fake e=new Fake();e.mutation=false;
        assertEquals(ComposingMutation340.Result.REJECTED,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("hello",e.text);
        assertEquals(0,e.commits);
    }
    @Test public void trueReturnAndVerifiedTextRetainsComposition(){
        Fake e=new Fake();
        assertEquals(ComposingMutation340.Result.COMPOSING,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("helloa",e.text);
    }
    @Test public void snapshotWindowHandlesLongDraftPrefix(){
        Fake e=new Fake();
        e.text="z".repeat(250)+"hello";
        assertEquals(ComposingMutation340.Result.COMPOSING,
                ComposingMutation340.append(e,"hello","a"));
        assertEquals("z".repeat(250)+"helloa",e.text);
    }
    @Test public void realBaseInputConnectionComposesLetterOnce(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(c),true);
        Editable editable=ic.getEditable();
        editable.replace(0,editable.length(),"");
        Selection.setSelection(editable,0);
        assertTrue(ic.setComposingText("hello",1));
        ComposingMutation340.Editor bridge=new ComposingMutation340.Editor(){
            @Override public boolean sameSession(){return true;}
            @Override public CharSequence before(){
                return ic.getTextBeforeCursor(ComposingMutation340.READ_WINDOW,0);
            }
            @Override public boolean setComposing(String next){
                return ic.setComposingText(next,1);
            }
            @Override public boolean finishComposing(){return ic.finishComposingText();}
            @Override public boolean commit(String t){return ic.commitText(t,1);}
        };
        assertEquals(ComposingMutation340.Result.COMPOSING,
                ComposingMutation340.append(bridge,"hello","a"));
        assertEquals("helloa",editable.toString());
        assertEquals(6,Selection.getSelectionStart(editable));
    }
}
