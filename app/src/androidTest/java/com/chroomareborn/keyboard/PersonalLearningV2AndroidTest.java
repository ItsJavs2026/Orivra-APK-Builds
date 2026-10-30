package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * Android 35 opt-in Learning V2 regression. Only synthetic examples.
 * Save and restore preference keys to avoid destructive test side effects.
 * Never enters a password, touches real notes, or uses the network.
 */
@RunWith(AndroidJUnit4.class)
public final class PersonalLearningV2AndroidTest {
    private Context context(){
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    @Test public void v1CorrectionPairsRemainUsableAndNewContractionsAreLearnable(){
        SharedPreferences p=context().getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> old=new HashMap<>(p.getAll());
        try {
            p.edit().clear().commit();
            p.edit().putBoolean(PersonalLearning.ENABLED,true)
                    .putInt("accepted:helo:hello",2).commit();
            assertEquals("hello",PersonalLearning.preferredCorrection(
                    context(),"helo"));
            assertNull(PersonalLearning.preferredCorrection(context(),"cant"));
            PersonalLearning.recordAcceptance(context(),"cant","can't");
            assertNull("one acceptance is not reinforced",
                    PersonalLearning.preferredCorrection(context(),"cant"));
            PersonalLearning.recordAcceptance(context(),"CANT","can’t");
            assertEquals("can't",
                    PersonalLearning.preferredCorrection(context(),"cant"));
            // A single aligned phrase difference may reinforce ONE pair,
            // but the full sentence must never appear in SharedPreferences.
            PersonalLearning.recordAcceptance(context(),
                    "the package cant arrive","the package can't arrive");
            assertEquals(3,p.getInt("accepted:cant:can't",0));
            for(String key:p.getAll().keySet()){
                assertFalse("no raw sentence key",key.contains("the package"));
            }
            assertTrue(PersonalLearning.diagnosticSummary(context())
                    .contains("learning v2"));
            assertTrue(PersonalLearning.learnedPairs(context())
                    .contains("cant → can't"));
            PersonalLearning.setEnabled(context(),false);
            assertNull("disabled learning cannot influence candidates",
                    PersonalLearning.preferredCorrection(context(),"cant"));
        }finally{
            SharedPreferences.Editor restore=p.edit().clear();
            for(Map.Entry<String,?> e:old.entrySet()){
                Object value=e.getValue();
                if(value instanceof Boolean)restore.putBoolean(e.getKey(),(Boolean)value);
                else if(value instanceof Integer)restore.putInt(e.getKey(),(Integer)value);
                else if(value instanceof Long)restore.putLong(e.getKey(),(Long)value);
                else if(value instanceof Float)restore.putFloat(e.getKey(),(Float)value);
                else if(value instanceof String)restore.putString(e.getKey(),(String)value);
            }
            restore.commit();
        }
    }
    @Test public void keepNoSuggestionsCannotStoreLearnedPairs(){
        EditorInfo note=new EditorInfo();
        note.packageName="com.google.android.keep";
        note.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        EditorSuggestionPrivacyPolicy.Decision choice=
                RebornImeService.editorPrivacyDecision(note);
        assertFalse(choice.sensitive);
        assertFalse(choice.allowPersonalLearning);
        EditorInfo password=new EditorInfo();
        password.packageName="com.google.android.keep";
        password.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD;
        assertTrue(RebornImeService.editorPrivacyDecision(password).sensitive);
    }
}
