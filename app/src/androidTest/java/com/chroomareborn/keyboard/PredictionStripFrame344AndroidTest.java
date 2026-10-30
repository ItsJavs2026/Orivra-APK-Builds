package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Normal prediction bar only: rapid synchronous/async result simulation.
 * No editor text, private typing, network or suggestion acceptance.
 */
@RunWith(AndroidJUnit4.class)
public final class PredictionStripFrame344AndroidTest {
    @Test public void rapidTypingWithOneTwoThreeOptionsRetainsAllChipInstances() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(context);
            assertTrue(PredictionStripFrame344.ensureThreeSlots(row,context));
            assertEquals(3,row.getChildCount());
            View[] initial={row.getChildAt(0),row.getChildAt(1),row.getChildAt(2)};
            for(int i=0;i<600;i++) {
                List<String> streaming=i%4==0?Collections.singletonList("con"):
                        i%4==1?Arrays.asList("con","continue"):
                        i%4==2?Arrays.asList("con","continue","continues"):
                                Arrays.asList("con","continue","continues","continued");
                assertFalse("no row reconstruction on frame "+i,
                        PredictionStripFrame344.ensureThreeSlots(row,context));
                assertEquals(3,PredictionStripFrame344.shownCount(streaming.size(),true));
                for(int slot=0;slot<3;slot++) {
                    TextView text=(TextView)row.getChildAt(slot);
                    String value=PredictionStripFrame344.slotValue(streaming,slot);
                    text.setText(value);
                    text.setVisibility(value.isEmpty()?View.INVISIBLE:View.VISIBLE);
                    assertSame("reused cell "+slot+" on keypress "+i,
                            initial[slot],row.getChildAt(slot));
                }
                assertEquals(3,row.getChildCount());
                assertEquals(View.VISIBLE,row.getChildAt(0).getVisibility());
                assertEquals(i%4==0?View.INVISIBLE:View.VISIBLE,
                        row.getChildAt(1).getVisibility());
            }
        });
    }

    @Test public void activeWordIsStableButSentenceAndSecureModeStayUntouched(){
        assertTrue(PredictionStripFrame344.stableWord("continu",false,true,false,false));
        assertTrue(PredictionStripFrame344.stableWord("can't",false,true,false,false));
        assertTrue(PredictionStripFrame344.stableWord("i",false,true,false,false));
        assertFalse(PredictionStripFrame344.stableWord("",false,true,false,false));
        assertFalse(PredictionStripFrame344.stableWord("longword without spaces",
                false,true,false,false));
        assertFalse(PredictionStripFrame344.stableWord("continue",true,true,false,false));
        assertFalse(PredictionStripFrame344.stableWord("continue",false,false,false,false));
        assertFalse(PredictionStripFrame344.stableWord("continue",false,true,true,false));
        assertFalse(PredictionStripFrame344.stableWord("continue",false,true,false,true));
        assertEquals(3,PredictionStripFrame344.shownCount(1,true));
        assertEquals(3,PredictionStripFrame344.shownCount(3,true));
        assertEquals(1,PredictionStripFrame344.shownCount(1,false));
    }

    @Test public void noTapTargetForUnavailableSuggestionOrDoubleCursorContext(){
        assertEquals("",PredictionStripFrame344.slotValue(Arrays.asList("continu"),1));
        assertEquals("",PredictionStripFrame344.slotValue(Collections.emptyList(),0));
        assertEquals("",PredictionStripFrame344.slotValue(null,1));
        assertEquals("continue",PredictionStripFrame344.slotValue(
                Arrays.asList("con","continue"),1));
        assertEquals("",PredictionStripFrame344.slotValue(
                Arrays.asList("con","continue"),-1));
    }

    @Test public void structuralTransitionRebuildsOnlyOnce(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(context);
            assertTrue(PredictionStripFrame344.ensureThreeSlots(row,context));
            View original=row.getChildAt(0);
            assertFalse(PredictionStripFrame344.ensureThreeSlots(row,context));
            assertSame(original,row.getChildAt(0));
            // Entering from a different view family is an intentional
            // structural transition, but the very next fast frame is stable.
            row.setTag("message");
            assertTrue(PredictionStripFrame344.ensureThreeSlots(row,context));
            assertNotSame(original,row.getChildAt(0));
            assertFalse(PredictionStripFrame344.ensureThreeSlots(row,context));
        });
    }
}
