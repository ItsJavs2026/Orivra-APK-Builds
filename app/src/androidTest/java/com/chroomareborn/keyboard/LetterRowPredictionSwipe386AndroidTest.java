package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Selection;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.LinearLayout;
import android.widget.Button;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research-only Android 35 instrumentation: real View touch dispatch plus
 * InputConnection boundary semantics for single- and short-word completion.
 * Does not instantiate the production IME or simulate Samsung firmware.
 */
@RunWith(AndroidJUnit4.class)
public final class LetterRowPredictionSwipe386AndroidTest {
    private static BaseInputConnection editor(String original,String suffix){
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        BaseInputConnection ic=new BaseInputConnection(new View(ctx),true);
        Editable txt=ic.getEditable();
        txt.replace(0,txt.length(),original+suffix);
        Selection.setSelection(txt,original.length());
        assertTrue(ic.setComposingRegion(0,original.length()));
        return ic;
    }
    private static SentenceSuggestionCommit.Editor bridge(BaseInputConnection ic){
        return new SentenceSuggestionCommit.Editor(){
            public CharSequence before(int n){return ic.getTextBeforeCursor(n,0);}
            public CharSequence after(int n){return ic.getTextAfterCursor(n,0);}
            public void beginBatch(){ic.beginBatchEdit();}
            public boolean commit(String s){return ic.commitText(s,1);}
            public void endBatch(){ic.endBatchEdit();}
        };
    }
    private static void insert(String start,String selected,String suffix,String expected){
        BaseInputConnection ic=editor(start,suffix);
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(bridge(ic),start,selected));
        assertEquals(expected,ic.getEditable().toString());
    }
    @Test public void downwardSwipeSelectsOnlyPhysicalCenter(){
        assertEquals("tell",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("tear","tell","test")));
        assertNull(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("tear","","test")));
    }
    @Test public void centerOnlyShortWordStartsLetterRowGesture(){
        java.util.List<String> onlyCenter=Arrays.asList("","a","");
        assertTrue("Center-only chip must arm DOWN recognizer",
                LetterRowPredictionSwipe386.hasSelectable(onlyCenter));
        LetterRowPredictionSwipe386 g=
                new LetterRowPredictionSwipe386(0,70f,20f,1f);
        assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,
                g.release(0,70f,67f));
        assertEquals("a",LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,onlyCenter));
        insert("a","a","","a ");
    }
    @Test public void twoLetterCenterOnlyCompletesWithOneSpace(){
        java.util.List<String> onlyCenter=Arrays.asList("","te","");
        assertTrue(LetterRowPredictionSwipe386.hasSelectable(onlyCenter));
        LetterRowPredictionSwipe386 g=
                new LetterRowPredictionSwipe386(4,70f,20f,1f);
        assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,
                g.release(4,70f,64f));
        insert("te","te","","te ");
        insert("te","te"," ","te ");
    }
    @Test public void centerOnlyPunctuationAndExistingWhitespaceNeverDoubleSpace(){
        java.util.List<String> onlyCenter=Arrays.asList("","I","");
        assertTrue(LetterRowPredictionSwipe386.hasSelectable(onlyCenter));
        insert("I","I","","I ");
        insert("I","I"," ","I ");
        insert("I","I",",","I,");
        insert("I","I",".","I.");
    }
    @Test public void emptyAndAbsentCenterDoesNotArmLetterRowGesture(){
        assertFalse(LetterRowPredictionSwipe386.hasSelectable(
                Arrays.asList("","","")));
        assertFalse(LetterRowPredictionSwipe386.hasSelectable(
                java.util.Collections.emptyList()));
        assertFalse(LetterRowPredictionSwipe386.hasSelectable(null));
        assertFalse(LetterRowPredictionSwipe386.hasSelectable(
                Arrays.asList(" ",null,"")));
    }
    @Test public void centerOnlyCannotBeForcedWithShortOrCanceledSwipe(){
        java.util.List<String> onlyCenter=Arrays.asList("","a","");
        assertTrue(LetterRowPredictionSwipe386.hasSelectable(onlyCenter));
        LetterRowPredictionSwipe386 shortMove=
                new LetterRowPredictionSwipe386(1,60f,20f,1f);
        assertEquals(LetterRowPredictionSwipe386.Direction.NONE,
                shortMove.release(1,60f,55f));
        LetterRowPredictionSwipe386 wrongPointer=
                new LetterRowPredictionSwipe386(1,60f,20f,1f);
        assertEquals(LetterRowPredictionSwipe386.Direction.CANCEL,
                wrongPointer.release(2,60f,70f));
        LetterRowPredictionSwipe386 aborted=
                new LetterRowPredictionSwipe386(1,60f,20f,1f);
        aborted.cancel();
        assertEquals(LetterRowPredictionSwipe386.Direction.CANCEL,
                aborted.release(1,60f,70f));
    }
    @Test public void middleRowPreferredButBothRowsOptional(){
        assertEquals(LetterRowPredictionSwipe386.MIDDLE_ONLY,
                LetterRowPredictionSwipe386.next(LetterRowPredictionSwipe386.OFF));
        assertTrue(LetterRowPredictionSwipe386.enabledForRow(2,1));
        assertFalse(LetterRowPredictionSwipe386.enabledForRow(2,0));
        assertTrue(LetterRowPredictionSwipe386.enabledForRow(1,0));
        assertTrue(LetterRowPredictionSwipe386.enabledForRow(3,1));
        assertFalse(LetterRowPredictionSwipe386.enabledForRow(3,2));
    }
    @Test public void oneLetterWordCompletesWithExactlyOneSpace(){
        insert("t","the","","the ");
        insert("a","and","","and ");
        insert("I","I will","","I will ");
    }
    @Test public void twoLetterWordCompletesWithExactlyOneSpace(){
        insert("te","tell","","tell ");
        insert("th","the","","the ");
    }
    @Test public void unchangedCenterWordStillAddsOneBoundary(){
        insert("a","a","","a ");
        insert("I","I","","I ");
    }
    @Test public void neverInsertDoubleSpaceBeforeExistingWhitespace(){
        insert("te","tell"," ","tell ");
        insert("t","the","\t","the\t");
        insert("a","and","\n","and\n");
    }
    @Test public void noExtraSpaceBeforePunctuation(){
        insert("te","tell",",","tell,");
        insert("a","and",".","and.");
        insert("t","the","!","the!");
    }
    @Test public void oneTwoAndThreeLetterPrefixRulesRemainEligibleForSpace(){
        String[] inputs={"t","te","tel"};
        for(String input:inputs){
            assertTrue(HighlightedSpace372.eligible(input,input,"tell",
                    true,true,true,true,true,false,false,false,
                    false,false,false,false));
        }
    }
    @Test public void actualViewReceivesDownSwipeBeyondOriginalKeyBounds(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            row.setMotionEventSplittingEnabled(true);
            Button key=new Button(ctx);
            row.addView(key,new LinearLayout.LayoutParams(150,72));
            row.measure(View.MeasureSpec.makeMeasureSpec(150,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(72,View.MeasureSpec.EXACTLY));
            row.layout(0,0,150,72);
            AtomicReference<LetterRowPredictionSwipe386> gesture=
                    new AtomicReference<>();
            AtomicReference<LetterRowPredictionSwipe386.Direction> result=
                    new AtomicReference<>(LetterRowPredictionSwipe386.Direction.NONE);
            key.setOnTouchListener((v,event)->{
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                    gesture.set(new LetterRowPredictionSwipe386(0,
                            event.getX(),event.getY(),1f));
                    return true;
                }
                if(event.getActionMasked()==MotionEvent.ACTION_MOVE){
                    gesture.get().move(0,event.getX(),event.getY());return true;
                }
                if(event.getActionMasked()==MotionEvent.ACTION_UP){
                    result.set(gesture.get().release(0,event.getX(),event.getY()));
                    return true;
                }
                return true;
            });
            long time=android.os.SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,75f,25f,0);
            MotionEvent move=MotionEvent.obtain(time,time+16,MotionEvent.ACTION_MOVE,75f,80f,0);
            MotionEvent up=MotionEvent.obtain(time,time+32,MotionEvent.ACTION_UP,75f,80f,0);
            try{
                assertTrue(row.dispatchTouchEvent(down));
                assertTrue(row.dispatchTouchEvent(move));
                assertTrue(row.dispatchTouchEvent(up));
            }finally{down.recycle();move.recycle();up.recycle();}
            assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,result.get());
        });
    }
    @Test public void preferenceIsIndependentAndCanBeTurnedOff(){
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=ctx.getSharedPreferences("keyboard_feedback",Context.MODE_PRIVATE);
        int prev=prefs.getInt(LetterRowPredictionSwipe386.PREF,0);
        boolean oldStrip=prefs.getBoolean(PredictionSwipe372.PREF,true);
        boolean oldSpace=prefs.getBoolean(HighlightedSpace372.PREF,true);
        try{
            prefs.edit().putInt(LetterRowPredictionSwipe386.PREF,2).commit();
            assertEquals(2,prefs.getInt(LetterRowPredictionSwipe386.PREF,0));
            assertEquals(oldStrip,prefs.getBoolean(PredictionSwipe372.PREF,true));
            assertEquals(oldSpace,prefs.getBoolean(HighlightedSpace372.PREF,true));
            prefs.edit().putInt(LetterRowPredictionSwipe386.PREF,0).commit();
            assertFalse(LetterRowPredictionSwipe386.enabledForRow(
                    prefs.getInt(LetterRowPredictionSwipe386.PREF,0),1));
        }finally{prefs.edit().putInt(LetterRowPredictionSwipe386.PREF,prev).commit();}
    }
}
