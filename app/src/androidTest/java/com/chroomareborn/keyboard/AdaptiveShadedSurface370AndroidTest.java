package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Android 35 emulator integration tests. Uses synthetic package IDs and
 * disposable local app prefs, never other app screenshots or editor text.
 */
@RunWith(AndroidJUnit4.class)
public final class AdaptiveShadedSurface370AndroidTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    private static int rgbDistance(int a,int b) {
        return Math.max(
                Math.max(Math.abs(((a>>16)&255)-((b>>16)&255)),
                        Math.abs(((a>>8)&255)-((b>>8)&255))),
                Math.abs((a&255)-(b&255)));
    }
    private void checkSurface(boolean dark) {
        Context ctx=context();
        SharedPreferences theme=ctx.getSharedPreferences(
                AppPalette.PREFS,Context.MODE_PRIVATE);
        int mode=theme.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        boolean follow=theme.getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        boolean priorDark=theme.getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        try {
            theme.edit().putInt(AppPalette.KEY_MODE,AppPalette.APP_SHADED)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,false)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,dark).commit();
            AppPalette.Result noMatch=AppPalette.resolve(
                    ctx,"com.example.unknown_shaded_surface_370");
            AppPalette.Result red=AppPalette.resolve(ctx,"com.youtube.example");
            AppPalette.Result green=AppPalette.resolve(ctx,"com.spotify.example");
            assertEquals(dark,noMatch.dark);
            assertEquals(dark,red.dark);
            assertEquals(dark,green.dark);
            for(AppPalette.Result selected:new AppPalette.Result[]{
                    noMatch,red,green}) {
                assertEquals(dark?0xFF202328:0xFFF3F5F7,
                        selected.background);
                assertEquals(dark?0xFF353A43:0xFFFFFFFF,
                        selected.keyBackground);
                assertNotEquals(selected.background,selected.keyBackground);
                assertNotEquals(0,selected.suggestionTextColor);
            }
            assertNotEquals(red.accent,green.accent);
            assertEquals(red.background,green.background);
            assertEquals(red.keyBackground,green.keyBackground);
            assertTrue(rgbDistance(red.suggestionBackground,
                    green.suggestionBackground)<=25);
            assertTrue(rgbDistance(red.specialBackground,
                    green.specialBackground)<=35);
            assertTrue(noMatch.label.toLowerCase().contains("neutral"));
        } finally {
            theme.edit().putInt(AppPalette.KEY_MODE,mode)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,follow)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,priorDark).commit();
        }
    }
    @Test public void lightSurfaceStaysShadedAndBrandIsOnlyAnAccent() {
        checkSurface(false);
    }
    @Test public void darkSurfaceStaysNeutralAndReadable() {
        checkSurface(true);
    }
    @Test public void motionAndSpecialSymbolHoldHaveIndependentToggles() {
        SharedPreferences prefs=context().getSharedPreferences(
                "keyboard_feedback",Context.MODE_PRIVATE);
        boolean instant=prefs.getBoolean(
                KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,true);
        boolean fast=prefs.getBoolean(
                SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,true);
        try {
            prefs.edit().putBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,true)
                    .putBoolean(SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,false)
                    .commit();
            assertTrue(prefs.getBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,false));
            assertFalse(prefs.getBoolean(
                    SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,true));
            assertEquals(500,SymbolHoldTiming367.delayMs(500,false));
            prefs.edit().putBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,false)
                    .putBoolean(SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,true)
                    .commit();
            assertTrue(KeyPressVisualPolicy369.shouldScaleAnimate(
                    !prefs.getBoolean(KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,true),
                    true)==false);
            assertEquals(320,SymbolHoldTiming367.delayMs(500,
                    prefs.getBoolean(SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,false)));
        } finally {
            prefs.edit().putBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,instant)
                    .putBoolean(SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,fast)
                    .commit();
        }
    }
    @Test public void invalidColorSignatureLeavesSurfaceUnchanged() {
        for(boolean dark:new boolean[]{false,true}) {
            int[] neutral=AppSurfaceAccent368.colors(null,dark);
            int[] invalid=AppSurfaceAccent368.colors(Float.NaN,dark);
            assertArrayEquals(neutral,invalid);
            assertEquals(neutral[AppSurfaceAccent368.BG],
                    invalid[AppSurfaceAccent368.BG]);
        }
    }
}
