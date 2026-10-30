package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Reproduces user-video normal-strip flicker at rapid 3->1->3 updates.
 * Original 344 regression covered VIEW identity, but omitted VISIBILITY and
 * transient click safety. No user words or app IDs are stored or logged.
 */
@RunWith(AndroidJUnit4.class)
public final class PredictionStripContinuity345AndroidTest {
    @Test public void sameWordRapidKeysPreserveSideChipVisuals(){
        assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",true,true,true,80));
        assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                "contin","continu",true,true,true,90));
        assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                "continu","contin",true,true,true,90));
        assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                "continu","continu",true,true,true,90));
    }

    @Test public void oldPredictionRemainsVisibleButCannotBeTapped(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            assertTrue(PredictionStripFrame344.ensureThreeSlots(row,ctx));
            TextView first=(TextView)row.getChildAt(0);
            TextView second=(TextView)row.getChildAt(1);
            TextView third=(TextView)row.getChildAt(2);
            AtomicInteger clicks=new AtomicInteger();
            first.setText("tslk");
            second.setText("walking");
            third.setText("typing");
            second.setVisibility(View.VISIBLE);
            third.setVisibility(View.VISIBLE);
            second.setOnClickListener(v->clicks.incrementAndGet());
            third.setOnClickListener(v->clicks.incrementAndGet());
            second.setContentDescription("Insert suggestion walking");
            third.setContentDescription("Insert suggestion typing");
            assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                    "tslk","tslki",true,true,true,45));
            first.setText("tslki"); // immediate current word always updates
            assertTrue(PredictionStripContinuity345.holdVisualOnly(second));
            assertTrue(PredictionStripContinuity345.holdVisualOnly(third));
            assertEquals("walking",second.getText().toString());
            assertEquals("typing",third.getText().toString());
            assertEquals(View.VISIBLE,second.getVisibility());
            assertEquals(View.VISIBLE,third.getVisibility());
            assertFalse(second.isClickable());
            assertFalse(third.isClickable());
            assertNull(second.getContentDescription());
            assertNull(third.getContentDescription());
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                    second.getImportantForAccessibility());
            second.performClick();
            third.performClick();
            assertEquals("stale candidate never inserts text",0,clicks.get());
            // A CURRENT async result may reactivate a candidate with new text
            // and a new tap handler; the visual TextView itself never changes.
            TextView retained=second;
            second.setText("talking");
            PredictionStripContinuity345.currentChip(second);
            second.setOnClickListener(v->clicks.incrementAndGet());
            assertSame(retained,row.getChildAt(1));
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
                    second.getImportantForAccessibility());
            assertTrue(second.isClickable());
            second.performClick();
            assertEquals(1,clicks.get());
        });
    }

    @Test public void noHoldAcrossPrivateEditorOrNewWordOrOldStaleData(){
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",true,true,false,50));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",true,false,true,50));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",false,true,true,50));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",true,true,true,-1));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","tslki",true,true,true,501));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","typing",true,true,true,30));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","t",true,true,true,30));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "tslk","",true,true,true,30));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "word","wo$rd",true,true,true,30));
        assertFalse(PredictionStripContinuity345.preserveInterimSideChips(
                "word","wordlongtext",true,true,true,30));
    }

    @Test public void emptyOrInvisibleChipMustNeverBeFrozen(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            TextView cell=new TextView(ctx);
            assertFalse(PredictionStripContinuity345.holdVisualOnly(cell));
            cell.setText("talking");
            cell.setVisibility(View.INVISIBLE);
            assertFalse(PredictionStripContinuity345.holdVisualOnly(cell));
            cell.setVisibility(View.VISIBLE);
            assertTrue(PredictionStripContinuity345.holdVisualOnly(cell));
            assertFalse(cell.isClickable());
        });
    }

    @Test public void simulatedFastSequenceKeepsSameThreeViews(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            assertTrue(PredictionStripFrame344.ensureThreeSlots(row,ctx));
            TextView[] cells={(TextView)row.getChildAt(0),
                    (TextView)row.getChildAt(1),(TextView)row.getChildAt(2)};
            cells[1].setText("walking");
            cells[2].setText("typing");
            cells[1].setVisibility(View.VISIBLE);
            cells[2].setVisibility(View.VISIBLE);
            for(int frame=0;frame<400;frame++) {
                String current=frame%2==0?"tslki":"tslkin";
                String prior=frame%2==0?"tslk":"tslki";
                assertTrue(PredictionStripContinuity345.preserveInterimSideChips(
                        prior,current,true,true,true,100));
                assertFalse(PredictionStripFrame344.ensureThreeSlots(row,ctx));
                cells[0].setText(current);
                for(int i=1;i<=2;i++) {
                    assertTrue(PredictionStripContinuity345.holdVisualOnly(cells[i]));
                    assertEquals(View.VISIBLE,cells[i].getVisibility());
                    assertSame(cells[i],row.getChildAt(i));
                }
                // An async result has arrived and is now current.
                for(int i=1;i<=2;i++){
                    PredictionStripContinuity345.currentChip(cells[i]);
                    cells[i].setOnClickListener(v->{});
                }
            }
        });
    }
}
