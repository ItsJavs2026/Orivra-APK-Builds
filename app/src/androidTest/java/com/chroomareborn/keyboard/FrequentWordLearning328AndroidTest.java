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

@RunWith(AndroidJUnit4.class)
public final class FrequentWordLearning328AndroidTest {
    private static void restore(SharedPreferences p,Map<String,?> saved){
        SharedPreferences.Editor edit=p.edit().clear();
        for(Map.Entry<String,?> e:saved.entrySet()){
            Object value=e.getValue();
            if(value instanceof Integer)edit.putInt(e.getKey(),(Integer)value);
            else if(value instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)value);
            else if(value instanceof String)edit.putString(e.getKey(),(String)value);
        }
        assertTrue(edit.commit());
    }
    @Test public void repeatedWordsPromoteEarlyWithClearAndOptOut() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences f=c.getSharedPreferences(
                FrequentWordLearning328.PREFS,Context.MODE_PRIVATE);
        SharedPreferences opt=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> prior=new HashMap<>(f.getAll());
        Map<String,?> learning=new HashMap<>(opt.getAll());
        try{
            f.edit().clear().commit();
            PersonalLearning.setEnabled(c,true);
            FrequentWordLearning328.recordCompleted(c,"continue");
            assertTrue(FrequentWordLearning328.matching(c,"con",3).isEmpty());
            FrequentWordLearning328.recordCompleted(c,"continue");
            assertTrue(FrequentWordLearning328.matching(c,"con",3)
                    .contains("continue"));
            assertTrue(FrequentWordLearning328.matching(c,"Cont",3)
                    .contains("Continue"));
            for(int i=0;i<4;i++)FrequentWordLearning328.recordCompleted(c,"go");
            assertFalse(FrequentWordLearning328.matching(c,"g",3).contains("go"));
            FrequentWordLearning328.recordCompleted(c,"go");
            assertTrue(FrequentWordLearning328.matching(c,"g",3).contains("go"));
            FrequentWordLearning328.recordCompleted(c,"unknownprivateword");
            FrequentWordLearning328.recordCompleted(c,"123456");
            assertEquals("unknown words and numeric strings must be excluded",
                    2,f.getAll().size());
            PersonalLearning.setEnabled(c,false);
            FrequentWordLearning328.recordCompleted(c,"continue");
            assertTrue(FrequentWordLearning328.matching(c,"con",3).isEmpty());
            assertEquals(2,f.getInt("freq:continue",0));
            PersonalLearning.clear(c);
            assertTrue(f.getAll().isEmpty());
        }finally{
            restore(f,prior);
            restore(opt,learning);
        }
    }
    @Test public void acceptedCompletionCountsTwiceWithoutLearningTypoPair(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences f=c.getSharedPreferences(
                FrequentWordLearning328.PREFS,Context.MODE_PRIVATE);
        SharedPreferences opt=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> a=new HashMap<>(f.getAll()),b=new HashMap<>(opt.getAll());
        try{
            f.edit().clear().commit();
            opt.edit().clear().putBoolean(PersonalLearning.ENABLED,true).commit();
            FrequentWordLearning328.recordAccepted(c,"continue");
            assertEquals(2,f.getInt("freq:continue",0));
            assertTrue(FrequentWordLearning328.matching(c,"cont",3)
                    .contains("continue"));
            assertNull("prefix completion is not a typo correction",
                    PersonalLearning.preferredCorrection(c,"cont"));
        }finally{restore(f,a);restore(opt,b);}
    }
}
