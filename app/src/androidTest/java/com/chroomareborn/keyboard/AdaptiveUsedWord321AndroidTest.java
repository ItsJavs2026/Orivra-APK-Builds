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

/** All examples are synthetic; restore app-private state after each test. */
@RunWith(AndroidJUnit4.class)
public final class AdaptiveUsedWord321AndroidTest {
    private static void restore(SharedPreferences p,Map<String,?> original) {
        SharedPreferences.Editor edit=p.edit().clear();
        for(Map.Entry<String,?> e:original.entrySet()) {
            Object v=e.getValue();
            if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);
            else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);
            else if(v instanceof String)edit.putString(e.getKey(),(String)v);
        }
        assertTrue(edit.commit());
    }
    @Test public void actualUsageReinforcesOptInCorrectionsAndResets() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences p=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        SharedPreferences learning=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> oldP=new HashMap<>(p.getAll());
        Map<String,?> oldL=new HashMap<>(learning.getAll());
        try {
            assertTrue(p.edit().clear().commit());
            assertTrue(learning.edit().clear()
                    .putBoolean(PersonalLearning.ENABLED,true).commit());
            PersonalLearning.recordAcceptance(c,"tekk","tell");
            assertNull(PersonalLearning.preferredCorrection(c,"tekk"));
            PersonalContextLearning319.recordUsed(c,"please","tell");
            assertEquals(1,p.getInt("used:please:tell",0));
            assertNull(PersonalLearning.preferredCorrection(c,"tekk"));
            PersonalContextLearning319.recordUsed(c,"please","tell");
            assertEquals(2,p.getInt("used:please:tell",0));
            assertEquals("tell",PersonalLearning.preferredCorrection(c,"tekk"));
            assertTrue("2 unassisted uses must not prematurely become next-word",
                    PersonalContextLearning319.suggest(c,"please",3).isEmpty());
            PersonalContextLearning319.recordUsed(c,"please","tell");
            assertEquals(3,p.getInt("used:please:tell",0));
            assertTrue("3 completed uses qualify personalized next-word",
                    PersonalContextLearning319.suggest(c,"please",3)
                            .contains("tell"));
            PersonalLearning.setEnabled(c,false);
            PersonalContextLearning319.recordUsed(c,"please","tell");
            assertEquals(3,p.getInt("used:please:tell",0));
            assertNull(PersonalLearning.preferredCorrection(c,"tekk"));
            assertTrue(PersonalContextLearning319.suggest(c,"please",3).isEmpty());
            PersonalLearning.setEnabled(c,true);
            PersonalContextLearning319.recordUsed(c,"please","privateunknown");
            PersonalContextLearning319.recordUsed(c,"javier","tell");
            assertEquals(1,p.getAll().size());
            PersonalLearning.clear(c);
            assertTrue(p.getAll().isEmpty());
            assertNull(PersonalLearning.preferredCorrection(c,"tekk"));
        } finally {
            restore(p,oldP);
            restore(learning,oldL);
        }
    }
    @Test public void usageAloneCannotCreateUnsupportedTypos() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences p=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        SharedPreferences learning=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> oldP=new HashMap<>(p.getAll());
        Map<String,?> oldL=new HashMap<>(learning.getAll());
        try {
            p.edit().clear().commit();
            learning.edit().clear().putBoolean(
                    PersonalLearning.ENABLED,true).commit();
            for(int i=0;i<4;i++)
                PersonalContextLearning319.recordUsed(c,"please","tell");
            assertEquals("common word usage cannot invent a typo mapping",
                    null,PersonalLearning.preferredCorrection(c,"tekk"));
            assertTrue(PersonalContextLearning319.suggest(c,"please",3)
                    .contains("tell"));
            PersonalLearning.recordAcceptance(c,"tekk","tell");
            PersonalLearning.recordAcceptance(c,"tekk","test");
            assertNull("competing confirmed correction choices abstain",
                    PersonalLearning.preferredCorrection(c,"tekk"));
        } finally {
            restore(p,oldP);restore(learning,oldL);
        }
    }
}
