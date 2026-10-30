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

/** Synthetic V2 transfer checks. Existing local test state is restored. */
@RunWith(AndroidJUnit4.class)
public final class ContextBackupTransfer325AndroidTest {
    private static void restore(SharedPreferences pref,Map<String,?> old) {
        SharedPreferences.Editor edit=pref.edit().clear();
        for(Map.Entry<String,?> e:old.entrySet()) {
            Object v=e.getValue();
            if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);
            if(v instanceof String)edit.putString(e.getKey(),(String)v);
            if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);
        }
        assertTrue(edit.commit());
    }
    @Test public void optInV2TransfersUsedWordsAndV1StaysExact() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences manual=c.getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences accepted=c.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        SharedPreferences context=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        Map<String,?> a=new HashMap<>(manual.getAll());
        Map<String,?> b=new HashMap<>(accepted.getAll());
        Map<String,?> d=new HashMap<>(context.getAll());
        try {
            manual.edit().clear().putString("word:customword","Customword").commit();
            accepted.edit().clear().putBoolean(PersonalLearning.ENABLED,false)
                    .putInt("accepted:tekk:tell",2).commit();
            context.edit().clear().putInt("pair:please:tell",2)
                    .putInt("used:please:tell",4)
                    .putInt("used:like:customword",3).commit();
            String v1=LearningTransferStore.exportText(c);
            assertTrue("legacy backup untouched",
                    v1.startsWith(LearningTransferCodec.HEADER+"\n"));
            String v2=LearningTransferStore.exportV2(c);
            ContextBackupFormat322.Data content=ContextBackupFormat322.decode(v2);
            assertEquals(2,content.contexts.size());
            assertEquals(4,content.contexts.get("please:tell").used);
            assertEquals(3,content.contexts.get("like:customword").used);
            assertEquals(Integer.valueOf(2),
                    content.legacy.learnedPairs.get("tekk:tell"));
            context.edit().clear().commit();
            manual.edit().clear().commit();
            accepted.edit().clear().putBoolean(
                    PersonalLearning.ENABLED,false).commit();
            LearningTransferStore.previewV2(c,content);
            assertTrue("preview never writes context",context.getAll().isEmpty());
            assertFalse(PersonalLearning.enabled(c));
            LearningTransferStore.importV2Confirmed(c,content);
            assertEquals(2,context.getInt("pair:please:tell",0));
            assertEquals(4,context.getInt("used:please:tell",0));
            assertEquals(3,context.getInt("used:like:customword",0));
            assertEquals("Customword",
                    manual.getString("word:customword",null));
            assertFalse("import cannot opt user into learning",
                    PersonalLearning.enabled(c));
            LearningTransferStore.importV2Confirmed(c,content);
            assertEquals(4,context.getInt("used:please:tell",0));
            assertEquals(3,context.getAll().size());
            ContextBackupFormat322.Data legacy=ContextBackupFormat322.decode(v1);
            assertTrue(legacy.contexts.isEmpty());
            LearningTransferStore.importV2Confirmed(c,legacy);
            assertEquals(4,context.getInt("used:please:tell",0));
        }finally{
            restore(context,d);restore(accepted,b);restore(manual,a);
        }
    }
    @Test public void unsupportedContextCannotImportEvenOneNewWord(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences manual=c.getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences context=c.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        Map<String,?> a=new HashMap<>(manual.getAll());
        Map<String,?> b=new HashMap<>(context.getAll());
        try {
            manual.edit().clear().commit();
            context.edit().clear().commit();
            String v2=ContextBackupFormat322.encode(
                    List.of("Newmanual"),Map.of(),
                    Map.of("please:unsavedzz",
                            new ContextBackupFormat322.Counts(2,0)));
            boolean rejected=false;
            try {
                LearningTransferStore.importV2Confirmed(c,
                        ContextBackupFormat322.decode(v2));
            }catch(IllegalArgumentException unsafe){rejected=true;}
            assertTrue("unknown word cannot silently become context",rejected);
            assertFalse(manual.contains("word:newmanual"));
            assertTrue(context.getAll().isEmpty());
        }finally{restore(context,b);restore(manual,a);}
    }
}
