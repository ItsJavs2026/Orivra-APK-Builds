package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Map;
import static org.junit.Assert.*;

/** Tests local context persistence with synthetic words only.
 * Saves and restores the two touched prefs, no real user data or network.
 */
@RunWith(AndroidJUnit4.class)
public final class PersonalContextLearning319AndroidTest {
    @Test public void optInReinforcementAndPrivacyRestrictions() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences pairs=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        SharedPreferences learning=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        String good="pair:please:tell";
        String bad="pair:javier:tell";
        Map<String,?> before=pairs.getAll();
        boolean wasEnabled=PersonalLearning.enabled(c);
        try {
            pairs.edit().clear().commit();
            PersonalLearning.setEnabled(c,false);
            PersonalContextLearning319.record(c,"please","tell");
            assertTrue(pairs.getAll().isEmpty());
            PersonalLearning.setEnabled(c,true);
            PersonalContextLearning319.record(c,"please","tell");
            assertEquals(1,pairs.getInt(good,0));
            assertTrue("one tap cannot promote",PersonalContextLearning319
                    .suggest(c,"please",3).isEmpty());
            PersonalContextLearning319.record(c,"please","tell");
            assertEquals(2,pairs.getInt(good,0));
            assertTrue(PersonalContextLearning319.suggest(c,"please",3)
                    .contains("tell"));
            PersonalContextLearning319.record(c,"javier","tell");
            assertFalse("no names as context",pairs.contains(bad));
            PersonalContextLearning319.record(c,"please","12345");
            assertEquals(1,pairs.getAll().size());
            PersonalLearning.setEnabled(c,false);
            assertTrue("opt out prevents reading",PersonalContextLearning319
                    .suggest(c,"please",3).isEmpty());
            assertTrue("opt out preserves existing pairs until clear",
                    pairs.contains(good));
            PersonalContextLearning319.clear(c);
            assertTrue("clear removes all context records",pairs.getAll().isEmpty());
        } finally {
            SharedPreferences.Editor restore=pairs.edit().clear();
            for(Map.Entry<String,?> entry:before.entrySet()) {
                Object v=entry.getValue();
                if(v instanceof Integer)
                    restore.putInt(entry.getKey(),(Integer)v);
                else if(v instanceof String)
                    restore.putString(entry.getKey(),(String)v);
                else if(v instanceof Boolean)
                    restore.putBoolean(entry.getKey(),(Boolean)v);
            }
            restore.commit();
            PersonalLearning.setEnabled(c,wasEnabled);
        }
    }
    @Test public void protectedAndroidEditorFlagsNeverAuthorizeContextRead() {
        EditorInfo password=new EditorInfo();
        password.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD;
        assertFalse(RebornImeService.editorPrivacyDecision(password)
                .allowPersonalLearning);
        EditorInfo noLearning=new EditorInfo();
        noLearning.inputType=InputType.TYPE_CLASS_TEXT;
        noLearning.imeOptions=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertFalse(RebornImeService.editorPrivacyDecision(noLearning)
                .allowPersonalLearning);
        EditorInfo name=new EditorInfo();
        name.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
        assertFalse(RebornImeService.editorPrivacyDecision(name)
                .allowPersonalLearning);
        EditorInfo address=new EditorInfo();
        address.inputType=InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS;
        assertFalse(RebornImeService.editorPrivacyDecision(address)
                .allowPersonalLearning);
    }
}
