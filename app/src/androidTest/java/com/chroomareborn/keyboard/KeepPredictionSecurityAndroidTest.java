package com.chroomareborn.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android framework flag mapping: no live Keep app or real note data. */
@RunWith(AndroidJUnit4.class)
public final class KeepPredictionSecurityAndroidTest {
    private static EditorInfo info(String pkg,int input,int options) {
        EditorInfo e=new EditorInfo();
        e.packageName=pkg;
        e.inputType=input;
        e.imeOptions=options;
        return e;
    }
    @Test public void googleKeepNoteWithNoSuggestionsGetsEphemeralPrediction() {
        EditorSuggestionPrivacyPolicy.Decision choice=
                RebornImeService.editorPrivacyDecision(info(
                        "com.google.android.keep",
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,0));
        assertFalse(choice.sensitive);
        assertTrue(choice.keepCompatibility);
        assertFalse(choice.allowPersonalLearning);
    }
    @Test public void keepExplicitNoPersonalizedLearningDoesNotHideCandidates() {
        EditorSuggestionPrivacyPolicy.Decision choice=
                RebornImeService.editorPrivacyDecision(info(
                        "com.google.android.keep",InputType.TYPE_CLASS_TEXT,
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING));
        assertFalse(choice.sensitive);
        assertFalse(choice.allowPersonalLearning);
    }
    @Test public void actualCredentialVariantsAlwaysStayPrivateEvenInKeep() {
        for(int variation:new int[]{
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_URI
        }) {
            EditorSuggestionPrivacyPolicy.Decision choice=
                    RebornImeService.editorPrivacyDecision(info(
                            "com.google.android.keep",
                            InputType.TYPE_CLASS_TEXT|variation|
                                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,0));
            assertTrue("secure variation "+variation,choice.sensitive);
            assertFalse(choice.allowPersonalLearning);
        }
    }
    @Test public void nonKeepNoSuggestionsAndNullInfoRemainProtected() {
        assertTrue(RebornImeService.editorPrivacyDecision(
                info("com.example.bank",InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,0)).sensitive);
        assertTrue(RebornImeService.editorPrivacyDecision(
                info("com.google.android.keep",InputType.TYPE_CLASS_NUMBER,0)).sensitive);
        assertTrue(RebornImeService.editorPrivacyDecision(null).sensitive);
    }
}
