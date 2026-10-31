package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Looper;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android 35 emulator tests. No screen capture, runtime permissions,
 * source app text or private imagery are accessed.
 */
@RunWith(AndroidJUnit4.class)
public final class AppIconColor366AndroidTest {
    private static final int SIZE=64;

    @Test public void rasterizedIconPixelSampleDetectsRedHue() {
        Bitmap bitmap=Bitmap.createBitmap(SIZE,SIZE,
                Bitmap.Config.ARGB_8888);
        try {
            bitmap.eraseColor(Color.WHITE);
            for(int y=10;y<54;y++)for(int x=10;x<54;x++)
                bitmap.setPixel(x,y,Color.RED);
            int[] pixels=new int[SIZE*SIZE];
            bitmap.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE);
            AppIconColorSignature366.Match match=
                    AppIconColorSignature366.detect(pixels,SIZE,SIZE);
            assertNotNull(match);
            assertTrue(match.hue<15f || match.hue>345f);
            assertTrue(match.chromaticCoverage>.3f);
        } finally {
            bitmap.recycle();
        }
    }

    @Test public void monochromeIconsFailToMaterialInsteadOfImaginaryHue() {
        int[] pixels=new int[SIZE*SIZE];
        java.util.Arrays.fill(pixels,Color.GRAY);
        assertNull(AppIconColorSignature366.detect(pixels,SIZE,SIZE));
        Context ctx=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        SharedPreferences theme=ctx.getSharedPreferences(
                AppPalette.PREFS,Context.MODE_PRIVATE);
        int previous=theme.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        try {
            theme.edit().putInt(AppPalette.KEY_MODE,AppPalette.APP_SHADED)
                    .commit();
            AppPalette.Result result=AppPalette.resolve(
                    ctx,"com.example.orivra_unlisted_greyscale_fixture");
            assertTrue(result.label,result.label.contains("Material fallback"));
            assertNotEquals(0,result.background);
            assertNotEquals(result.background,result.keyBackground);
        } finally {
            theme.edit().putInt(AppPalette.KEY_MODE,previous).commit();
        }
    }

    @Test public void backgroundIconCacheNeverNeedsAUiThreadExtraction() {
        Context ctx=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        final String self=ctx.getPackageName();
        // Synchronous request only schedules the worker; completion may
        // represent an icon that is monochrome (cached null hue).
        long before=SystemClock.elapsedRealtime();
        AppIconHueCache366.request(ctx,self,null);
        long duration=SystemClock.elapsedRealtime()-before;
        assertTrue("Scheduling must be fast (<200ms)",duration<200);
        long deadline=SystemClock.elapsedRealtime()+10_000;
        while(!AppIconHueCache366.cacheContainsForTest(self) &&
                SystemClock.elapsedRealtime()<deadline)
            SystemClock.sleep(40);
        assertTrue("Worker must cache icon or failed lookup",
                AppIconHueCache366.cacheContainsForTest(self));
        // Do not require hue != null: monochrome icons correctly fall back.
        Float hue=AppIconHueCache366.cachedHue(self);
        if(hue!=null)assertTrue(hue>=0f && hue<360f);
    }

    @Test public void unknownOrInvalidPackagesNeverRequestBroadVisibility() {
        Context ctx=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        AppIconHueCache366.request(ctx,"bad package",null);
        AppIconHueCache366.request(ctx,"",null);
        assertNull(AppIconHueCache366.cachedHue("bad package"));
        assertFalse(AppIconHueCache366.cacheContainsForTest("bad package"));
        assertNull(AppIconHueCache366.cachedHue(null));
    }

    @Test public void appMatchAndMaterialYouStillHonorNightMode() {
        Context ctx=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        SharedPreferences p=ctx.getSharedPreferences(
                AppPalette.PREFS,Context.MODE_PRIVATE);
        int old=p.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        boolean follow=p.getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        boolean manual=p.getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        try {
            for(int mode:new int[]{AppPalette.APP_SHADED,
                    AppPalette.APP_MATCH,AppPalette.MATERIAL_AUTO}) {
                for(boolean dark:new boolean[]{false,true}) {
                    p.edit().putInt(AppPalette.KEY_MODE,mode)
                            .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,false)
                            .putBoolean(AppPalette.KEY_MANUAL_DARK,dark)
                            .commit();
                    AppPalette.Result result=AppPalette.resolve(
                            ctx,"com.example.orivra_unlisted_fixture");
                    assertEquals(dark,result.dark);
                    assertNotEquals(result.background,result.keyBackground);
                    assertNotEquals(0,result.suggestionTextColor);
                }
            }
        } finally {
            p.edit().putInt(AppPalette.KEY_MODE,old)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,follow)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,manual).commit();
        }
    }
}
