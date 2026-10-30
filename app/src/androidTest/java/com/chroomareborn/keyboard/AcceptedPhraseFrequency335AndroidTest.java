package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Only synthetic accepted phrases; restore opt-in and counters afterward. */
@RunWith(AndroidJUnit4.class)
public final class AcceptedPhraseFrequency335AndroidTest {
    private static void restore(SharedPreferences prefs,Map<String,?> old) {
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> e:old.entrySet()) {
            Object value=e.getValue();
            if(value instanceof Integer)edit.putInt(e.getKey(),(Integer)value);
            else if(value instanceof String)edit.putString(e.getKey(),(String)value);
            else if(value instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)value);
        }
        assertTrue(edit.commit());
    }
    @Test public void explicitlyAcceptedPhraseStrengthensCompletedWords(){
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences words=ctx.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        SharedPreferences prefs=ctx.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> oldWords=new HashMap<>(words.getAll());
        Map<String,?> oldSettings=new HashMap<>(prefs.getAll());
        try {
            words.edit().clear().commit();
            PersonalLearning.setEnabled(ctx,false);
            PersonalWordFrequency328.recordAcceptedPhrase(ctx,
                    "Please continue");
            assertTrue("opt-out blocks phrase learning",words.getAll().isEmpty());
            PersonalLearning.setEnabled(ctx,true);
            PersonalWordFrequency328.recordAcceptedPhrase(ctx,
                    "Please continue");
            assertEquals(2,words.getInt("word:continue",0));
            assertEquals(2,words.getInt("word:please",0));
            PersonalWordFrequency328.recordAcceptedPhrase(ctx,
                    "I can continue continue");
            assertEquals("same word not double counted within a phrase",
                    4,words.getInt("word:continue",0));
            assertEquals(2,words.getInt("word:can",0));
            PersonalWordFrequency328.recordAcceptedPhrase(ctx,
                    "continue 12345");
            assertEquals(4,words.getInt("word:continue",0));
            assertFalse(words.contains("word:12345"));
            String export=LearningTransferStore.exportV3(ctx);
            FrequencyBackupFormat330.Data reloaded=
                    FrequencyBackupFormat330.decode(export);
            assertEquals(4,(int)reloaded.frequencies.get("continue"));
            assertEquals(2,(int)reloaded.frequencies.get("can"));
            PersonalLearning.setEnabled(ctx,false);
            PersonalWordFrequency328.recordAcceptedPhrase(ctx,
                    "Please continue");
            assertEquals(4,words.getInt("word:continue",0));
        } finally {
            restore(words,oldWords);
            restore(prefs,oldSettings);
        }
    }
}
