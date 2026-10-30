package com.chroomareborn.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Synthetic Android editor flags only. No private content is accessed.
 */
@RunWith(AndroidJUnit4.class)
public final class StableWordConfidence334AndroidTest {
    @Test public void bankAmountAndPinNeverTriggerIdleWordLearning() {
        EditorInfo amount=new EditorInfo();
        amount.inputType=InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL;
        EditorSuggestionPrivacyPolicy.Decision money=
                RebornImeService.editorPrivacyDecision(amount);
        assertTrue(NumericKeypadPolicy323.isNumericField(amount.inputType));
        assertFalse(money.allowPersonalLearning);
        assertNull(StableWordUsagePolicy329.candidate(
                "continue",true,money.allowPersonalLearning,money.sensitive,
                false,false,false,false,true));

        EditorInfo pin=new EditorInfo();
        pin.inputType=InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        EditorSuggestionPrivacyPolicy.Decision protectedField=
                RebornImeService.editorPrivacyDecision(pin);
        assertTrue(protectedField.sensitive);
        assertNull(StableWordUsagePolicy329.candidate(
                "continue",true,protectedField.allowPersonalLearning,
                protectedField.sensitive,false,false,false,false,true));
    }
    @Test public void privateTextAndNoPersonalizedLearningCannotStoreFrequency() {
        EditorInfo password=new EditorInfo();
        password.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD;
        EditorSuggestionPrivacyPolicy.Decision decision=
                RebornImeService.editorPrivacyDecision(password);
        assertFalse(decision.allowPersonalLearning);
        assertNull(StableWordUsagePolicy329.candidate(
                "continue",true,decision.allowPersonalLearning,
                decision.sensitive,false,false,false,false,false));

        EditorInfo blocked=new EditorInfo();
        blocked.inputType=InputType.TYPE_CLASS_TEXT;
        blocked.imeOptions=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        EditorSuggestionPrivacyPolicy.Decision noLearning=
                RebornImeService.editorPrivacyDecision(blocked);
        assertFalse(noLearning.allowPersonalLearning);
        assertNull(StableWordUsagePolicy329.candidate(
                "continue",true,noLearning.allowPersonalLearning,
                noLearning.sensitive,false,false,false,false,false));
    }
    @Test public void normalFieldsKeepShortWordsConservativeAndLongWordsPrompt() {
        EditorInfo normal=new EditorInfo();
        normal.inputType=InputType.TYPE_CLASS_TEXT;
        EditorSuggestionPrivacyPolicy.Decision decision=
                RebornImeService.editorPrivacyDecision(normal);
        assertTrue(decision.allowPersonalLearning);
        assertEquals(550L,StableWordTiming334.milliseconds("continue"));
        assertEquals(1600L,StableWordTiming334.milliseconds("con"));
        assertEquals(1600L,StableWordTiming334.milliseconds("my"));
        assertEquals("continue",StableWordUsagePolicy329.candidate(
                "Continue",true,decision.allowPersonalLearning,
                decision.sensitive,false,false,false,false,false));
    }
}
