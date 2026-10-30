package com.chroomareborn.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Framework InputType / privacy verification using synthetic field metadata.
 * No access to banking app content, user payments or private text.
 */
@RunWith(AndroidJUnit4.class)
public final class NumericKeypad323AndroidTest {
    @Test public void moneyEditorOffersNumbersWithoutReenablingLearning(){
        EditorInfo payment=new EditorInfo();
        payment.inputType=InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL;
        payment.imeOptions=EditorInfo.IME_ACTION_GO;
        assertTrue(NumericKeypadPolicy323.isNumericField(payment.inputType));
        assertEquals(".",NumericKeypadPolicy323.secondarySymbol(payment.inputType));
        EditorSuggestionPrivacyPolicy.Decision privateField=
                RebornImeService.editorPrivacyDecision(payment);
        assertTrue("nontext amount remains private",privateField.sensitive);
        assertFalse(privateField.allowSuggestions);
        assertFalse(privateField.allowPersonalLearning);
        assertEquals(EditorInfo.IME_ACTION_GO,
                EnterKeyPolicy.tapEditorAction(payment.imeOptions));
    }
    @Test public void pinsPhonesDatesAndFallbackTextRemainCorrect(){
        EditorInfo pin=new EditorInfo();
        pin.inputType=InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        assertTrue(NumericKeypadPolicy323.isNumericField(pin.inputType));
        assertEquals("",NumericKeypadPolicy323.secondarySymbol(pin.inputType));
        assertFalse(RebornImeService.editorPrivacyDecision(pin)
                .allowPersonalLearning);
        EditorInfo phone=new EditorInfo();
        phone.inputType=InputType.TYPE_CLASS_PHONE;
        assertTrue(NumericKeypadPolicy323.isNumericField(phone.inputType));
        assertEquals("+",NumericKeypadPolicy323.secondarySymbol(phone.inputType));
        EditorInfo date=new EditorInfo();
        date.inputType=InputType.TYPE_CLASS_DATETIME;
        assertTrue(NumericKeypadPolicy323.isNumericField(date.inputType));
        assertEquals("/",NumericKeypadPolicy323.secondarySymbol(date.inputType));
        EditorInfo privateText=new EditorInfo();
        privateText.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD;
        assertFalse("text password can still use explicit ?123 switch",
                NumericKeypadPolicy323.isNumericField(privateText.inputType));
        assertTrue(RebornImeService.editorPrivacyDecision(privateText).sensitive);
    }
}
