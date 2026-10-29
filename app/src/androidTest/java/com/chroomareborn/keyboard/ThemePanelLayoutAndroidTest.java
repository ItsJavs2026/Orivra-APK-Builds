package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Android 35 real View-measurement fixture for the Candidate 294 panel
 * structure: prediction row always present, theme header + controls beneath,
 * and no reserved panel height when fully collapsed. This does NOT replace
 * Samsung hardware screenshot or complete InputMethodService QA.
 */
@RunWith(AndroidJUnit4.class)
public final class ThemePanelLayoutAndroidTest {
    private int dp(Context c,int n) {
        return Math.round(n*c.getResources().getDisplayMetrics().density);
    }
    private int measuredHeight(LinearLayout root,int width) {
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(root.getContext(),720),
                        View.MeasureSpec.AT_MOST));
        root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
        return root.getMeasuredHeight();
    }
    private static final class Fixture {
        final LinearLayout root, panel, header, controls, predictions, keys;
        final Context ctx;
        Fixture(Context ctx,int hHeader,int hControls,int hPrediction,int hKeys) {
            this.ctx=ctx;
            root=new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            predictions=new LinearLayout(ctx);
            panel=new LinearLayout(ctx);
            panel.setOrientation(LinearLayout.VERTICAL);
            header=new LinearLayout(ctx);
            controls=new LinearLayout(ctx);
            keys=new LinearLayout(ctx);
            panel.addView(header,new LinearLayout.LayoutParams(-1,hHeader));
            panel.addView(controls,new LinearLayout.LayoutParams(-1,hControls));
            root.addView(predictions,new LinearLayout.LayoutParams(-1,hPrediction));
            root.addView(panel,new LinearLayout.LayoutParams(-1,-2));
            root.addView(keys,new LinearLayout.LayoutParams(-1,hKeys));
        }
    }
    @Test public void collapseTrulyRemovesThemeHeightWithoutRemovingChildViews() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            int header=dp(ctx,28), controls=dp(ctx,36), prediction=dp(ctx,48), keys=dp(ctx,100);
            Fixture f=new Fixture(ctx,header,controls,prediction,keys);
            int count=f.root.getChildCount(), panelCount=f.panel.getChildCount();
            ThemePanelLayout.apply(f.panel,false);
            assertEquals(View.GONE,f.panel.getVisibility());
            int collapsed=measuredHeight(f.root,dp(ctx,360));
            assertEquals("theme panel must reserve zero height",prediction+keys,collapsed);
            ThemePanelLayout.apply(f.panel,true);
            assertEquals(View.VISIBLE,f.panel.getVisibility());
            int expanded=measuredHeight(f.root,dp(ctx,360));
            assertEquals("theme info and separate controls row",collapsed+header+controls,expanded);
            assertSame(f.predictions,f.root.getChildAt(0));
            assertSame(f.panel,f.root.getChildAt(1));
            assertSame(f.keys,f.root.getChildAt(2));
            assertSame(f.header,f.panel.getChildAt(0));
            assertSame(f.controls,f.panel.getChildAt(1));
            assertEquals(count,f.root.getChildCount());
            assertEquals(panelCount,f.panel.getChildCount());
            ThemePanelLayout.apply(f.panel,false);
            assertEquals(collapsed,measuredHeight(f.root,dp(ctx,360)));
        });
    }
    @Test public void rapidReopenAndCollapseKeepsRowMeasurementsStable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Fixture f=new Fixture(ctx,dp(ctx,28),dp(ctx,36),dp(ctx,48),dp(ctx,100));
            int collapsed=dp(ctx,48)+dp(ctx,100);
            int expanded=collapsed+dp(ctx,28)+dp(ctx,36);
            for(int i=0;i<250;i++) {
                boolean open=(i%2)==0;
                ThemePanelLayout.apply(f.panel,open);
                assertEquals("no accumulated phantom theme height",
                        open?expanded:collapsed,
                        measuredHeight(f.root,dp(ctx,360)));
                assertEquals(3,f.root.getChildCount());
                assertEquals(2,f.panel.getChildCount());
            }
        });
    }
}
