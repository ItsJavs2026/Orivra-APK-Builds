package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * Android 35 local-only backup preview/import regression.
 * All data is synthetic; existing values are restored after each test.
 */
@RunWith(AndroidJUnit4.class)
public final class LearningTransferStoreAndroidTest {
    private Context context(){
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    private static void restore(SharedPreferences p,Map<String,?> old){
        SharedPreferences.Editor editor=p.edit().clear();
        for(Map.Entry<String,?> e:old.entrySet()){
            Object v=e.getValue();
            if(v instanceof Boolean)editor.putBoolean(e.getKey(),(Boolean)v);
            else if(v instanceof Integer)editor.putInt(e.getKey(),(Integer)v);
            else if(v instanceof Long)editor.putLong(e.getKey(),(Long)v);
            else if(v instanceof Float)editor.putFloat(e.getKey(),(Float)v);
            else if(v instanceof String)editor.putString(e.getKey(),(String)v);
        }
        assertTrue("Restore all pre-test app-local values",editor.commit());
    }
    @Test public void portableImportMergesOldAndNewWithoutInflatingCountsOrEnablingLearning(){
        SharedPreferences manual=context().getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences learned=context().getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> originalWords=new HashMap<>(manual.getAll());
        Map<String,?> originalLearning=new HashMap<>(learned.getAll());
        try{
            assertTrue(manual.edit().clear()
                    .putString("word:o'connor","O'Connor").commit());
            assertTrue(learned.edit().clear()
                    .putBoolean(PersonalLearning.ENABLED,false)
                    .putInt("accepted:cant:can't",2).commit());
            LearningTransferCodec.Bundle imported=LearningTransferCodec.decode(
                    LearningTransferCodec.encode(
                            List.of("O’Connor","Ain't"),
                            Map.of("cant:can't",4,"aint:ain't",2)));
            LearningTransferStore.Preview preview=
                    LearningTransferStore.preview(context(),imported);
            assertEquals(1,preview.newWords);
            assertEquals(1,preview.newPairs);
            assertEquals(1,preview.strongerPairs);
            assertFalse("preview cannot modify learning",
                    PersonalLearning.enabled(context()));
            assertEquals(2,learned.getInt("accepted:cant:can't",0));
            LearningTransferStore.Preview applied=
                    LearningTransferStore.importConfirmed(context(),imported);
            assertEquals(1,applied.newWords);
            assertEquals(2,PersonalDictionary.wordCount(context()));
            assertEquals("O'Connor",manual.getString("word:o'connor",null));
            assertEquals("Ain't",manual.getString("word:ain't",null));
            assertEquals(4,learned.getInt("accepted:cant:can't",0));
            assertEquals(2,learned.getInt("accepted:aint:ain't",0));
            assertFalse("import never enables learning",
                    PersonalLearning.enabled(context()));
            LearningTransferStore.Preview twice=
                    LearningTransferStore.importConfirmed(context(),imported);
            assertEquals(0,twice.newWords);
            assertEquals(0,twice.newPairs);
            assertEquals(0,twice.strongerPairs);
            assertEquals(4,learned.getInt("accepted:cant:can't",0));
            LearningTransferCodec.Bundle exported=LearningTransferCodec.decode(
                    LearningTransferStore.exportText(context()));
            assertTrue(exported.words.contains("O'Connor"));
            assertTrue(exported.words.contains("Ain't"));
            assertEquals(Integer.valueOf(4),exported.learnedPairs.get("cant:can't"));
            assertEquals(Integer.valueOf(2),exported.learnedPairs.get("aint:ain't"));
        }finally{
            restore(learned,originalLearning);
            restore(manual,originalWords);
        }
    }

    @Test public void malformedBackupCannotModifyStorage() {
        SharedPreferences manual=context().getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences learning=context().getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        Map<String,?> originalWords=new HashMap<>(manual.getAll());
        Map<String,?> originalLearning=new HashMap<>(learning.getAll());
        try{
            assertTrue(manual.edit().clear().commit());
            assertTrue(learning.edit().clear().putBoolean(
                    PersonalLearning.ENABLED,false).commit());
            String valid=LearningTransferCodec.encode(
                    List.of("Headphones"),Map.of("helo:hello",2));
            boolean rejected=false;
            try{LearningTransferCodec.decode(valid.replace("hello","other"));}
            catch(IllegalArgumentException invalid){rejected=true;}
            assertTrue("checksum must reject tampered data",rejected);
            assertEquals(0,PersonalDictionary.wordCount(context()));
            assertEquals(0,PersonalLearning.countEntries(learning));
            assertFalse(PersonalLearning.enabled(context()));
        }finally{
            restore(learning,originalLearning);
            restore(manual,originalWords);
        }
    }

    @Test public void systemPickerIntentsHaveExplicitDocumentScope(){
        Intent create=LearningTransferDocumentIO.createIntent();
        Intent open=LearningTransferDocumentIO.openIntent();
        assertEquals(Intent.ACTION_CREATE_DOCUMENT,create.getAction());
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,open.getAction());
        assertTrue(create.hasCategory(Intent.CATEGORY_OPENABLE));
        assertTrue(open.hasCategory(Intent.CATEGORY_OPENABLE));
        assertEquals("text/plain",create.getType());
        assertEquals("text/plain",open.getType());
        assertEquals("orivra-local-learning-backup.txt",
                create.getStringExtra(Intent.EXTRA_TITLE));
    }
}
