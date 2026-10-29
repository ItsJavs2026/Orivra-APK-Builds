package com.chroomareborn.keyboard;

import android.view.View;
import android.widget.LinearLayout;

/**
 * Applies actual Android GONE rather than hiding child labels inside a
 * permanently measured toolbar. All child views remain mounted.
 */
final class ThemePanelLayout {
    private ThemePanelLayout() {}

    static void apply(LinearLayout panel,boolean expanded) {
        if(panel!=null)panel.setVisibility(expanded?View.VISIBLE:View.GONE);
    }
}
