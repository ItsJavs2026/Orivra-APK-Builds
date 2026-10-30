package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Real Android 35 View measurement regression for the Candidate 304 idle
 * bar alignment. Uses only synthetic strings; no editor/notes/screenshots.
 */
@RunWith(AndroidJUnit4.class)
public final class IdleSuggestionCenterAndroidTest {
    private static int dp(Context c,int n) {
        return Math.round(n*c.getResources().getDisplayMetrics().density);
    }
    private static final class Fixture {
        final Context context;
        final LinearLayout header, row;
        final HorizontalScrollView scroll;
        final TextView caret;
        Fixture(Context c,int count) {
            context=c;
            header=new LinearLayout(c);
            header.setGravity(Gravity.CENTER_VERTICAL);
            scroll=new HorizontalScrollView(c);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setFillViewport(true);
            row=new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            scroll.addView(row);
            header.addView(scroll,new LinearLayout.LayoutParams(0,dp(c,48),1f));
            caret=new TextView(c);
            caret.setGravity(Gravity.CENTER);
            caret.setText("⌃");
            header.addView(caret,new LinearLayout.LayoutParams(dp(c,38),dp(c,44)));
            for(int i=0;i<count;i++){
                TextView chip=new TextView(c);
                chip.setText(new String[]{"I","The","Hey","Okay","Yes"}[i%5]);
                chip.setGravity(Gravity.CENTER);
                chip.setTextSize(17);
                chip.setPadding(dp(c,13),0,dp(c,13),0);
                LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(-2,dp(c,44));
                cell.setMargins(dp(c,2),dp(c,2),dp(c,2),dp(c,2));
                row.addView(chip,cell);
                if(i<count-1){
                    View divider=new View(c);
                    LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(
                            dp(c,1),dp(c,26));
                    line.setMargins(dp(c,3),0,dp(c,3),0);
                    row.addView(divider,line);
                }
            }
        }
        void layout() {
            int w=dp(context,360);
            header.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(context,48),View.MeasureSpec.EXACTLY));
            header.layout(0,0,header.getMeasuredWidth(),header.getMeasuredHeight());
        }
        void align(String composing,boolean privateField,int count) {
            int gravity=Gravity.CENTER_VERTICAL |
                    (SuggestionStripLayoutPolicy.centerIdleSuggestions(
                            composing,count,privateField)?
                    Gravity.CENTER_HORIZONTAL:Gravity.START);
            row.setGravity(gravity);
            layout();
        }
        float chipCenter() {
            return (row.getChildAt(0).getLeft()+
                    row.getChildAt(row.getChildCount()-1).getRight())/2f;
        }
    }

    @Test public void threeIdleChipsAreCenteredInRealFillViewport() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Fixture f=new Fixture(c,3);
            f.align("",false,3);
            assertEquals("scroll must occupy area excluding caret",
                    f.header.getMeasuredWidth()-dp(c,38),f.scroll.getMeasuredWidth());
            assertEquals("fillViewport should expand short suggestion row",
                    f.scroll.getMeasuredWidth(),f.row.getMeasuredWidth());
            assertEquals("idle chips group centered in available strip",
                    f.scroll.getWidth()/2f,f.chipCenter(),dp(c,4));
            assertEquals("caret never moves from right side",
                    f.header.getMeasuredWidth()-dp(c,38),f.caret.getLeft());
        });
    }

    @Test public void typingAndPrivacyRestoreStartGravityAndCaretStaysFixed() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Fixture f=new Fixture(c,3);
            f.align("",false,3);
            float centered=f.chipCenter();
            f.align("hel",false,3);
            assertTrue("typed suggestions return to leading edge",
                    f.row.getChildAt(0).getLeft()<=dp(c,5));
            assertTrue("gravity visibly changes on typing",
                    centered-f.chipCenter()>dp(c,30));
            f.align("",true,3);
            assertTrue("private editor not centered",
                    f.row.getChildAt(0).getLeft()<=dp(c,5));
            f.align("",false,3);
            assertEquals("idle layout returns after backspace",
                    f.scroll.getWidth()/2f,f.chipCenter(),dp(c,4));
            assertEquals("caret stays fixed through gravity changes",
                    f.header.getMeasuredWidth()-dp(c,38),f.caret.getLeft());
        });
    }

    @Test public void oneAndTwoSuggestionsAlsoCenter() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            for(int count:new int[]{1,2,3}) {
                Fixture f=new Fixture(c,count);
                f.align("",false,count);
                assertEquals("group centered for "+count+" idle next-words",
                        f.scroll.getWidth()/2f,f.chipCenter(),dp(c,4));
            }
        });
    }
}
