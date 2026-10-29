package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android framework/editor flag integration; no real typed text is accessed. */
@RunWith(AndroidJUnit4.class)
public final class OfflineDictionaryActivationAndroidTest {
    private static final String KEEP="com.google.android.keep";

    private static EditorInfo editor(String pkg,int type,int flags) {
        EditorInfo info=new EditorInfo();
        info.packageName=pkg;
        info.inputType=type;
        info.imeOptions=flags;
        return info;
    }
    @Test public void optInIsFalseUnlessExplicitlyEnabled() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences p=context.getSharedPreferences(
                OfflineDictionaryActivationPolicy.PREFS,Context.MODE_PRIVATE);
        final boolean previouslyPresent=p.contains(OfflineDictionaryActivationPolicy.KEY);
        final boolean previous=p.getBoolean(OfflineDictionaryActivationPolicy.KEY,false);
        try{
            assertTrue(p.edit().remove(OfflineDictionaryActivationPolicy.KEY).commit());
            assertFalse(p.getBoolean(OfflineDictionaryActivationPolicy.KEY,false));
            assertFalse(OfflineDictionaryActivationPolicy.usable(
                    p.getBoolean(OfflineDictionaryActivationPolicy.KEY,false),false));
            assertTrue(p.edit().putBoolean(OfflineDictionaryActivationPolicy.KEY,true).commit());
            assertTrue(OfflineDictionaryActivationPolicy.usable(
                    p.getBoolean(OfflineDictionaryActivationPolicy.KEY,false),false));
        }finally{
            if(previouslyPresent)
                p.edit().putBoolean(OfflineDictionaryActivationPolicy.KEY,previous).commit();
            else p.edit().remove(OfflineDictionaryActivationPolicy.KEY).commit();
        }
    }
    @Test public void keepTextCanOptInButPasswordsAndNoPersonalizationStayProtected() {
        EditorSuggestionPrivacyPolicy.Decision keepNote=
                RebornImeService.editorPrivacyDecision(editor(KEEP,
                        InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING));
        assertFalse(keepNote.sensitive);
        assertFalse(keepNote.allowPersonalLearning);
        assertTrue(OfflineDictionaryActivationPolicy.usable(true,keepNote.sensitive));
        EditorSuggestionPrivacyPolicy.Decision password=
                RebornImeService.editorPrivacyDecision(editor(KEEP,
                        InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,0));
        assertTrue(password.sensitive);
        assertFalse(OfflineDictionaryActivationPolicy.usable(true,password.sensitive));
        EditorSuggestionPrivacyPolicy.Decision email=
                RebornImeService.editorPrivacyDecision(editor(KEEP,
                        InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,0));
        assertTrue(email.sensitive);
        assertFalse(OfflineDictionaryActivationPolicy.usable(true,email.sensitive));
    }
}
