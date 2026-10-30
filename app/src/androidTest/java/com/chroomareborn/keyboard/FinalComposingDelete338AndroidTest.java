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

/**
 * Android framework editing simulation, not a real Google Messages device
 * test. Exercises real BaseInputConnection composing-span operations.
 */
@RunWith(AndroidJUnit4.class)
public final class FinalComposingDelete338AndroidTest {
    private BaseInputConnection connection() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new BaseInputConnection(new View(c),true);
    }
    private static FinalComposingDelete338.Editor bridge(
            final BaseInputConnection connection) {
        return new FinalComposingDelete338.Editor() {
            @Override public boolean sameSession(){return true;}
            @Override public CharSequence before(){
                return connection.getTextBeforeCursor(96,0);
            }
            @Override public boolean replaceComposingWithEmpty(){
                return connection.setComposingText("",1);
            }
            @Override public boolean finishComposing(){
                return connection.finishComposingText();
            }
            @Override public boolean deleteOneCodePoint(){
                return connection.deleteSurroundingTextInCodePoints(1,0) ||
                        connection.deleteSurroundingText(1,0);
            }
        };
    }
    @Test public void realAndroidFinishComposingAloneDoesNotDeleteLastLetter(){
        BaseInputConnection ic=connection();
        Editable editor=ic.getEditable();
        editor.replace(0,editor.length(),"");
        Selection.setSelection(editor,0);
        assertTrue(ic.setComposingText("a",1));
        assertEquals("a",editor.toString());
        assertTrue(ic.finishComposingText());
        assertEquals("a",editor.toString());
    }
    @Test public void askingToEmptyNeedsExactlySixBackspaces(){
        BaseInputConnection ic=connection();
        Editable editor=ic.getEditable();
        editor.replace(0,editor.length(),"Where you at lol Greg over here ");
        Selection.setSelection(editor,editor.length());
        String prefix=editor.toString(),word="asking";
        assertTrue(ic.setComposingText(word,1));
        for(int remaining=word.length();remaining>1;remaining--){
            String shortened=word.substring(0,remaining-1);
            assertTrue(ic.setComposingText(shortened,1));
            assertEquals(prefix+shortened,editor.toString());
        }
        assertEquals(prefix+"a",editor.toString());
        assertEquals(FinalComposingDelete338.Result.APPLIED,
                FinalComposingDelete338.apply(bridge(ic),"a"));
        assertEquals("one final Backspace must delete a",prefix,editor.toString());
        assertEquals(prefix.length(),Selection.getSelectionStart(editor));
    }
    @Test public void isolatedOneCharacterCompositionDeletesOnFirstPress(){
        BaseInputConnection ic=connection();
        Editable editor=ic.getEditable();
        editor.replace(0,editor.length(),"");
        Selection.setSelection(editor,0);
        assertTrue(ic.setComposingText("a",1));
        assertEquals(FinalComposingDelete338.Result.APPLIED,
                FinalComposingDelete338.apply(bridge(ic),"a"));
        assertEquals("",editor.toString());
    }
}
