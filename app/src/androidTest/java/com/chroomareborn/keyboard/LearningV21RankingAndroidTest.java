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

/**
 * Isolated Android SharedPreferences integration for Learning V2.1 ranking.
 * Only synthetic corrections are used; existing prefs are restored in finally.
 */
@RunWith(AndroidJUnit4.class)
public final class LearningV21RankingAndroidTest {
    private Context context(){
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    @Test public void conflictDoesNotOverrideWordAndExistingV1CountIsPreserved() {
        SharedPreferences prefs=context().getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> old=new HashMap<>(prefs.getAll());
        try{
            assertTrue(prefs.edit().clear().putBoolean(PersonalLearning.ENABLED,true)
                    .putInt("accepted:cant:can't",2)
                    .putInt("accepted:cant:can",2)
                    .putInt("accepted:helo:hello",3).commit());
            assertNull("ambiguous history must not randomly alter cant",
                    PersonalLearning.preferredCorrection(context(),"cant"));
            assertEquals("hello",PersonalLearning.preferredCorrection(context(),"helo"));
            PersonalLearning.recordAcceptance(context(),"cant","can't");
            assertEquals("can't",PersonalLearning.preferredCorrection(context(),"cant"));
            assertEquals("CAN'T",PersonalLearning.preferredCorrection(context(),"CANT"));
            assertEquals("Can't",PersonalLearning.preferredCorrection(context(),"Cant"));
            assertEquals(3,prefs.getInt("accepted:cant:can't",0));
            assertEquals(2,prefs.getInt("accepted:cant:can",0));
            PersonalLearning.setEnabled(context(),false);
            assertNull("learning off must never influence suggestions",
                    PersonalLearning.preferredCorrection(context(),"cant"));
        }finally{
            SharedPreferences.Editor restore=prefs.edit().clear();
            for(Map.Entry<String,?> entry:old.entrySet()){
                Object val=entry.getValue();
                if(val instanceof Boolean)restore.putBoolean(entry.getKey(),(Boolean)val);
                else if(val instanceof Integer)restore.putInt(entry.getKey(),(Integer)val);
                else if(val instanceof Long)restore.putLong(entry.getKey(),(Long)val);
                else if(val instanceof Float)restore.putFloat(entry.getKey(),(Float)val);
                else if(val instanceof String)restore.putString(entry.getKey(),(String)val);
            }
            restore.commit();
        }
    }

    @Test public void curlyAndStraightApostrophePreferenceRowsDoNotDoubleCount() {
        SharedPreferences prefs=context().getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> old=new HashMap<>(prefs.getAll());
        try{
            assertTrue(prefs.edit().clear().putBoolean(PersonalLearning.ENABLED,true)
                    .putInt("accepted:aint:ain't",1)
                    .putInt("accepted:aint:ain’t",1)
                    .commit());
            assertNull("two spellings for one historical tap must not fake reinforcement",
                    PersonalLearning.preferredCorrection(context(),"aint"));
            PersonalLearning.recordAcceptance(context(),"aint","ain't");
            assertEquals("ain't",PersonalLearning.preferredCorrection(context(),"aint"));
        }finally{
            SharedPreferences.Editor restore=prefs.edit().clear();
            for(Map.Entry<String,?> entry:old.entrySet()){
                Object val=entry.getValue();
                if(val instanceof Boolean)restore.putBoolean(entry.getKey(),(Boolean)val);
                else if(val instanceof Integer)restore.putInt(entry.getKey(),(Integer)val);
                else if(val instanceof Long)restore.putLong(entry.getKey(),(Long)val);
                else if(val instanceof Float)restore.putFloat(entry.getKey(),(Float)val);
                else if(val instanceof String)restore.putString(entry.getKey(),(String)val);
            }
            restore.commit();
        }
    }
}
