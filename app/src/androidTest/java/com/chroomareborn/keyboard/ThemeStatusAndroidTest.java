package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android 35 emulator-only palette-state qualification, not Samsung UI QA. */
@RunWith(AndroidJUnit4.class)
public final class ThemeStatusAndroidTest {
    @Test public void selectedPaletteAndManualLightDarkAreRepresented() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=ctx.getSharedPreferences(AppPalette.PREFS,Context.MODE_PRIVATE);
        int savedMode=prefs.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        boolean savedFollow=prefs.getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        boolean savedDark=prefs.getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        try {
            for(int mode=0;mode<AppPalette.MODE_COUNT;mode++) {
                for(boolean dark:new boolean[]{true,false}) {
                    prefs.edit().putInt(AppPalette.KEY_MODE,mode)
                            .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,false)
                            .putBoolean(AppPalette.KEY_MANUAL_DARK,dark).commit();
                    AppPalette.Result applied=AppPalette.resolve(ctx,"com.example.orivrafixture");
                    assertEquals("manual appearance differs",dark,applied.dark);
                    String chip=ThemeStatusLabel.expandedChip(
                            AppPalette.modeName(mode),applied.dark);
                    assertTrue(chip,chip.endsWith(dark?" D":" L"));
                    assertTrue(chip,chip.length()<=10);
                    String status=ThemeStatusLabel.compact(
                            AppPalette.modeName(mode),applied.dark);
                    String details=ThemeStatusLabel.details(
                            AppPalette.modeName(mode),applied.dark,
                            AppPalette.followsSystem(ctx),applied.label);
                    assertTrue(status,status.contains(AppPalette.modeName(mode)));
                    assertTrue(status,status.contains(dark?"Dark":"Light"));
                    assertTrue(details,details.contains("Manual light/dark"));
                    assertTrue(details,details.contains("Applied: "));
                }
            }
        } finally {
            prefs.edit().putInt(AppPalette.KEY_MODE,savedMode)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,savedFollow)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,savedDark).commit();
        }
    }

    @Test public void systemModeStatusIsClearAndExistingSettingsRestored() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=ctx.getSharedPreferences(AppPalette.PREFS,Context.MODE_PRIVATE);
        int prior=prefs.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        boolean follow=prefs.getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        boolean manual=prefs.getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        try {
            prefs.edit().putInt(AppPalette.KEY_MODE,AppPalette.CLEAN_NEUTRAL)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true).commit();
            AppPalette.Result resolved=AppPalette.resolve(ctx,"com.example.orivrafixture");
            String label=ThemeStatusLabel.details("Clean Neutral",resolved.dark,
                    AppPalette.followsSystem(ctx),resolved.label);
            assertTrue(label,label.contains("Follows system"));
            assertTrue(label,label.contains("Clean Neutral"));
            assertTrue(label,label.contains(resolved.dark?"Dark":"Light"));
        } finally {
            prefs.edit().putInt(AppPalette.KEY_MODE,prior)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,follow)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,manual).commit();
        }
    }
}
