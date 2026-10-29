package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Real Android SDK 35 integration against AppPalette.resolve and TextView.
 * No screenshots, no user text, no network or editor changes.
 */
@RunWith(AndroidJUnit4.class)
public final class SuggestionVisualAndroidTest {
    @Test public void allEightThemeModesHaveReadableRecommendedChips() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=context.getSharedPreferences(
                AppPalette.PREFS,Context.MODE_PRIVATE);
        int initial=prefs.getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        boolean follow=prefs.getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        boolean darkPref=prefs.getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        try {
            for(int mode=0;mode<AppPalette.MODE_COUNT;mode++) {
                for(boolean dark:new boolean[]{false,true}) {
                    prefs.edit().putInt(AppPalette.KEY_MODE,mode)
                            .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,false)
                            .putBoolean(AppPalette.KEY_MANUAL_DARK,dark).commit();
                    AppPalette.Result palette=AppPalette.resolve(context,
                            "com.example.orivra.visual.test");
                    assertEquals("selected light/dark state",dark,palette.dark);
                    int basic=SuggestionVisualStyle.chipColor(
                            palette.suggestionBackground,palette.keyBackground,
                            palette.dark,false);
                    int recommended=SuggestionVisualStyle.chipColor(
                            palette.suggestionBackground,palette.keyBackground,
                            palette.dark,true);
                    int text=SuggestionVisualStyle.readableText(
                            palette.suggestionTextColor,recommended);
                    assertTrue("Text contrast below 4.5 in mode "
                            +AppPalette.modeName(mode),
                            SuggestionVisualStyle.contrastRatio(
                                    text,recommended)>=4.5d);
                    assertEquals(255,(recommended>>>24)&255);
                    assertTrue(dark ?
                            SuggestionVisualStyle.contrastRatio(
                                    0xff000000,recommended)>=
                            SuggestionVisualStyle.contrastRatio(
                                    0xff000000,basic) :
                            SuggestionVisualStyle.contrastRatio(
                                    0xffffffff,recommended)>=
                            SuggestionVisualStyle.contrastRatio(
                                    0xffffffff,basic));
                }
            }
        } finally {
            prefs.edit().putInt(AppPalette.KEY_MODE,initial)
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,follow)
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,darkPref).commit();
        }
    }

    @Test public void textViewTypographyStaysInsidePredictionTouchCell() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            TextView chip=new TextView(ctx);
            chip.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            chip.setText("Hey there, how are you doing?");
            chip.setTextSize(SuggestionVisualStyle.textSizeSp(true,
                    chip.getText().length()));
            chip.setLetterSpacing(SuggestionVisualStyle.letterSpacing(true));
            chip.setIncludeFontPadding(false);
            chip.setMaxLines(2);
            chip.setWidth(Math.round(300*ctx.getResources().getDisplayMetrics().density));
            assertTrue(chip.getTextSize()>0f);
            assertTrue(chip.getLetterSpacing()<.02f);
            assertEquals(2,chip.getMaxLines());
            assertNotNull(chip.getTypeface());
            assertEquals(View.VISIBLE,chip.getVisibility());
        });
    }
}
