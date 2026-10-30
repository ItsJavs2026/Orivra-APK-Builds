package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Framework-backed local SharedPreferences acceptance and backup check.
 * Only synthetic words; cleanup restores any previous preference values.
 */
@RunWith(AndroidJUnit4.class)
public final class SpanishPersonalLearningAndroidTest {
    @Test public void spanishCorrectionRequiresOptInAndTwoExplicitAcceptances() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        String key="accepted:camion:camión";
        boolean oldEnabled=PersonalLearning.enabled(c);
        boolean hadValue=prefs.contains(key);
        Object oldValue=prefs.getAll().get(key);
        try {
            prefs.edit().remove(key).commit();
            PersonalLearning.setEnabled(c,false);
            PersonalLearning.recordAcceptance(c,"camion","camión");
            assertFalse("opt-out does not store anything",prefs.contains(key));
            PersonalLearning.setEnabled(c,true);
            PersonalLearning.recordAcceptance(c,"camion","camión");
            assertNull("one choice cannot rank",PersonalLearning.preferredCorrection(
                    c,"camion"));
            PersonalLearning.recordAcceptance(c,"camion","camión");
            assertEquals("camión",PersonalLearning.preferredCorrection(c,"camion"));
            LearningTransferCodec.Bundle backup=
                    LearningTransferStore.snapshot(c);
            assertEquals(Integer.valueOf(2),
                    backup.learnedPairs.get("camion:camión"));
        } finally {
            SharedPreferences.Editor restore=prefs.edit();
            if(hadValue && oldValue instanceof Integer)
                restore.putInt(key,(Integer)oldValue);
            else restore.remove(key);
            restore.commit();
            PersonalLearning.setEnabled(c,oldEnabled);
        }
    }
    @Test public void spanishManualWordIsOptionalAndRemovable() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String word="Niño";
        boolean already=PersonalDictionary.savedWords(c).contains(word);
        try {
            if(!already)assertEquals(PersonalDictionary.Add.ADDED,
                    PersonalDictionary.add(c,word));
            assertTrue(PersonalDictionary.matching(c,"Ni").contains(word));
            assertTrue(PersonalDictionaryPolicy.merge("ni",List.of("ni"),
                    List.of(word)).contains("niño"));
        } finally {
            if(!already)PersonalDictionary.remove(c,word);
        }
    }
}
