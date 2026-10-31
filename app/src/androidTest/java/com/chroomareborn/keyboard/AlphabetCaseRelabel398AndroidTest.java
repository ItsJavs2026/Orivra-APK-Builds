package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android View-tree and touch-identity checks; synthetic public key labels only. */
@RunWith(AndroidJUnit4.class)
public final class AlphabetCaseRelabel398AndroidTest {
    private static final class Fixture {
        LinearLayout keys;
        Button shift;
        Button[][] letters=new Button[3][];
        TextView[][] hints=new TextView[3][];
        final int[] clicked={0};
    }

    private static Fixture fixture() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Fixture f=new Fixture();
        f.keys=new LinearLayout(context);
        f.keys.setOrientation(LinearLayout.VERTICAL);
        String[] rows=KeyboardLayoutSpec.rows(KeyboardLayoutSpec.ABC);
        for(int rowIndex=0;rowIndex<3;rowIndex++) {
            LinearLayout row=new LinearLayout(context);
            if(rowIndex==2) {
                f.shift=new Button(context);
                f.shift.setText("⇧");
                row.addView(f.shift);
            }
            f.letters[rowIndex]=new Button[rows[rowIndex].length()];
            f.hints[rowIndex]=new TextView[rows[rowIndex].length()];
            for(int col=0;col<rows[rowIndex].length();col++){
                FrameLayout cell=new FrameLayout(context);
                Button key=new Button(context);
                String character=String.valueOf(rows[rowIndex].charAt(col));
                key.setText(character);
                String hint=KeyboardLayoutSpec.longPressHint(rowIndex,col);
                key.setContentDescription(character+", hold for "+hint);
                key.setOnClickListener(v->f.clicked[0]++);
                TextView label=new TextView(context);
                label.setText(hint);
                cell.addView(key);
                cell.addView(label);
                row.addView(cell);
                f.letters[rowIndex][col]=key;
                f.hints[rowIndex][col]=label;
            }
            if(rowIndex==2) {
                Button delete=new Button(context);
                delete.setText("⌫");
                row.addView(delete);
            }
            f.keys.addView(row);
        }
        f.keys.addView(new LinearLayout(context)); // preserve bottom row
        return f;
    }

    @Test public void capitalizationTogglesWithoutReplacingAnyTouchTarget() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Fixture f=fixture();
            View originalCell=f.letters[0][0].getParent() instanceof View ?
                    (View)f.letters[0][0].getParent():null;
            Button originalQ=f.letters[0][0];
            Button originalZ=f.letters[2][0];
            assertTrue(AlphabetCaseRelabel398.relabel(f.keys,true,false));
            assertSame(originalQ,f.letters[0][0]);
            assertSame(originalCell,originalQ.getParent());
            assertEquals("Q",originalQ.getText().toString());
            assertEquals("Z",originalZ.getText().toString());
            assertEquals("Q, hold for 1",originalQ.getContentDescription().toString());
            assertEquals("⇧",f.shift.getText().toString());
            assertTrue(AlphabetCaseRelabel398.relabel(f.keys,false,false));
            assertEquals("q",originalQ.getText().toString());
            assertEquals("z",originalZ.getText().toString());
            assertEquals(4,f.keys.getChildCount());
            assertTrue(originalQ.performClick());
            assertEquals(1,f.clicked[0]);
        });
    }

    @Test public void capsLockKeepsModifierAndAccessibilityConsistent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Fixture f=fixture();
            assertTrue(AlphabetCaseRelabel398.relabel(f.keys,true,true));
            assertEquals("⇪",f.shift.getText().toString());
            assertTrue(f.shift.isSelected());
            assertEquals("A",f.letters[1][0].getText().toString());
            assertEquals("A, hold for @",
                    f.letters[1][0].getContentDescription().toString());
            assertTrue(AlphabetCaseRelabel398.relabel(f.keys,false,false));
            assertEquals("⇧",f.shift.getText().toString());
            assertFalse(f.shift.isSelected());
            assertEquals("a",f.letters[1][0].getText().toString());
        });
    }

    @Test public void wrongLayoutFailsClosedBeforeAnyPartialMutation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Fixture f=fixture();
            TextView last=f.hints[2][f.hints[2].length-1];
            last.setText("broken");
            assertFalse(AlphabetCaseRelabel398.relabel(f.keys,true,false));
            assertEquals("q",f.letters[0][0].getText().toString());
            assertEquals("⇧",f.shift.getText().toString());
            last.setText(KeyboardLayoutSpec.longPressHint(2,f.hints[2].length-1));
            assertTrue(AlphabetCaseRelabel398.relabel(f.keys,true,false));
            Fixture wrong=fixture();
            wrong.keys.removeViewAt(3);
            assertFalse(AlphabetCaseRelabel398.relabel(wrong.keys,true,false));
            assertEquals("q",wrong.letters[0][0].getText().toString());
            Fixture symbols=fixture();
            symbols.shift.setText("=\\<");
            assertFalse(AlphabetCaseRelabel398.relabel(symbols.keys,true,false));
        });
    }

    @Test public void longTogglingRetainsPressedStateListenersAndGeometry() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Fixture f=fixture();
            Button key=f.letters[1][2];
            FrameLayout cell=(FrameLayout)key.getParent();
            LinearLayout row=(LinearLayout)cell.getParent();
            key.setPressed(true);
            int count=row.getChildCount();
            long start=System.nanoTime();
            for(int i=0;i<1200;i++) {
                boolean upper=(i&1)==0;
                assertTrue(AlphabetCaseRelabel398.relabel(f.keys,upper,false));
            }
            long elapsed=System.nanoTime()-start;
            assertEquals(count,row.getChildCount());
            assertSame(cell,key.getParent());
            assertTrue("Only label state may change",key.isPressed());
            assertEquals("d",key.getText().toString());
            assertTrue(key.performClick());
            assertEquals(1,f.clicked[0]);
            android.util.Log.i("OrivraShift398",
                    "1200 in-place synthetic Shift updates elapsedMs="+
                            (elapsed/1_000_000.0)+" (Android 35 only; not Samsung)");
        });
    }
}
