package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** Synthetic completed-word usage only; restores private prefs after tests. */
@RunWith(AndroidJUnit4.class)
public final class PersonalPrefix328AndroidTest {
    private static void restore(SharedPreferences pref,Map<String,?> previous) {
        SharedPreferences.Editor e=pref.edit().clear();
        for(Map.Entry<String,?> entry:previous.entrySet()) {
            Object v=entry.getValue();
            if(v instanceof Integer)e.putInt(entry.getKey(),(Integer)v);
            else if(v instanceof String)e.putString(entry.getKey(),(String)v);
            else if(v instanceof Boolean)e.putBoolean(entry.getKey(),(Boolean)v);
        }
        assertTrue(e.commit());
    }
    @Test public void repeatedCompletedWordsTeachEarlyAndShortSuggestions() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences p=c.getSharedPreferences(PersonalWordFrequency328.PREFS,
                Context.MODE_PRIVATE);
        SharedPreferences learning=c.getSharedPreferences(PersonalLearning.PREFS,
                Context.MODE_PRIVATE);
        Map<String,?> initial=new HashMap<>(p.getAll());
        Map<String,?> initialLearning=new HashMap<>(learning.getAll());
        try {
            p.edit().clear().commit();
            PersonalLearning.setEnabled(c,false);
            PersonalWordFrequency328.recordCommitted(c,"continue");
            assertTrue(p.getAll().isEmpty());
            PersonalLearning.setEnabled(c,true);
            PersonalWordFrequency328.recordCommitted(c,"continue");
            PersonalWordFrequency328.recordCommitted(c,"continue");
            assertTrue(PersonalWordFrequency328.matching(c,"con",3).isEmpty());
            PersonalWordFrequency328.recordCommitted(c,"continue");
            assertTrue(PersonalWordFrequency328.matching(c,"con",3)
                    .contains("continue"));
            assertTrue(PersonalWordFrequency328.matching(c,"co",3).isEmpty());
            PersonalWordFrequency328.recordCommitted(c,"continue");
            PersonalWordFrequency328.recordCommitted(c,"continue");
            assertTrue(PersonalWordFrequency328.matching(c,"co",3)
                    .contains("continue"));
            for(int i=0;i<7;i++)PersonalWordFrequency328.recordCommitted(c,"my");
            assertTrue("short words can learn",PersonalWordFrequency328
                    .matching(c,"m",3).contains("my"));
            PersonalWordFrequency328.recordCommitted(c,"1234");
            PersonalWordFrequency328.recordCommitted(c,"privateunknown");
            assertFalse(p.contains("word:1234"));
            assertFalse(p.contains("word:privateunknown"));
            PersonalLearning.setEnabled(c,false);
            assertTrue(PersonalWordFrequency328.matching(c,"con",3).isEmpty());
            PersonalWordFrequency328.recordCommitted(c,"continue");
            assertEquals(5,p.getInt("word:continue",0));
            PersonalLearning.setEnabled(c,true);
            PersonalWordFrequency328.clear(c);
            assertTrue(p.getAll().isEmpty());
        } finally {
            restore(p,initial);
            restore(learning,initialLearning);
        }
    }
    @Test public void candidateRankingPreservesExactShortWords() {
        List<String> output=AdaptivePrefixPolicy328.merge("can",
                List.of("can","can go"),List.of("candy"),true);
        assertEquals("can",output.get(0));
        assertEquals("candy",output.get(1));
    }
}
