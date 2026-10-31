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
 * Synthetic Android editor checks: does not inspect or retain user typing.
 * The actual ChatGPT reply composer still requires a physical device test.
 */
@RunWith(AndroidJUnit4.class)
public final class SelectedBackspace416BAndroidTest {
    private BaseInputConnection editorWith(String value,int start,int end) {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(app),true);
        Editable text=ic.getEditable();
        text.replace(0,text.length(),value);
        Selection.setSelection(text,start,end);
        return ic;
    }
    private SelectedBackspace416B.Outcome remove(BaseInputConnection ic) {
        return SelectedBackspace416B.apply(new SelectedBackspace416B.Editor() {
            @Override public boolean sameSession(){return true;}
            @Override public boolean selectionKnown(){
                Editable text=ic.getEditable();
                return Selection.getSelectionStart(text)!=
                        Selection.getSelectionEnd(text);
            }
            @Override public boolean replaceSelectionOnce(){
                return ic.commitText("",1);
            }
        });
    }
    @Test public void longHighlightIsDeletedWithOneReplacement() {
        String longText="hello ".repeat(700);
        String original="before "+longText+"after";
        BaseInputConnection ic=editorWith(original,7,7+longText.length());
        assertEquals(SelectedBackspace416B.Outcome.APPLIED,remove(ic));
        assertEquals("before after",ic.getEditable().toString());
        assertEquals(7,Selection.getSelectionStart(ic.getEditable()));
        assertEquals(7,Selection.getSelectionEnd(ic.getEditable()));
    }
    @Test public void noSelectionNeverDeletesAdjacentCharacters() {
        BaseInputConnection ic=editorWith("before after",7,7);
        assertEquals(SelectedBackspace416B.Outcome.NOT_SELECTED,remove(ic));
        assertEquals("before after",ic.getEditable().toString());
    }
    @Test public void selectionWithinSentencePreservesBothSides() {
        BaseInputConnection ic=editorWith("I have many extra words in here.",
                12,24);
        assertEquals(SelectedBackspace416B.Outcome.APPLIED,remove(ic));
        assertEquals("I have many in here.",ic.getEditable().toString());
    }
}
