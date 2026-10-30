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

@RunWith(AndroidJUnit4.class)
public final class FrequencyTransfer331AndroidTest {
    private static void restore(SharedPreferences prefs,Map<String,?> old) {
        SharedPreferences.Editor editor=prefs.edit().clear();
        for(Map.Entry<String,?> e:old.entrySet()) {
            Object v=e.getValue();
            if(v instanceof String)editor.putString(e.getKey(),(String)v);
            else if(v instanceof Integer)editor.putInt(e.getKey(),(Integer)v);
            else if(v instanceof Boolean)editor.putBoolean(e.getKey(),(Boolean)v);
        }
        assertTrue(editor.commit());
    }
    @Test public void V1V2V3RoundTripAndIdempotentImport() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences dictionary=ctx.getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences corrections=ctx.getSharedPreferences(
                PersonalLearning.PREFS,Context.MODE_PRIVATE);
        SharedPreferences context=ctx.getSharedPreferences(
                PersonalContextLearning319.PREFS,Context.MODE_PRIVATE);
        SharedPreferences freq=ctx.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        Map<String,?> d=new HashMap<>(dictionary.getAll()),
                p=new HashMap<>(corrections.getAll()),
                c=new HashMap<>(context.getAll()),
                f=new HashMap<>(freq.getAll());
        try {
            assertTrue(dictionary.edit().clear().putString(
                    "word:customword","Customword").commit());
            assertTrue(corrections.edit().clear()
                    .putBoolean(PersonalLearning.ENABLED,false)
                    .putInt("accepted:tekk:tell",2).commit());
            assertTrue(context.edit().clear().putInt("pair:please:tell",2)
                    .putInt("used:please:tell",4).commit());
            assertTrue(freq.edit().clear().putInt("word:continue",7)
                    .putInt("word:my",9).putInt("word:customword",5).commit());
            String v1=LearningTransferStore.exportText(ctx);
            String v2=LearningTransferStore.exportV2(ctx);
            String v3=LearningTransferStore.exportV3(ctx);
            assertTrue(v1.startsWith(LearningTransferCodec.HEADER+"\n"));
            assertTrue(v2.startsWith(ContextBackupFormat322.HEADER+"\n"));
            assertTrue(v3.startsWith(FrequencyBackupFormat330.HEADER+"\n"));
            FrequencyBackupFormat330.Data decoded=FrequencyBackupFormat330.decode(v3);
            assertEquals(7,(int)decoded.frequencies.get("continue"));
            assertEquals(9,(int)decoded.frequencies.get("my"));
            assertEquals(5,(int)decoded.frequencies.get("customword"));
            assertEquals(4,decoded.earlier.contexts.get("please:tell").used);
            assertEquals(2,(int)decoded.earlier.legacy.learnedPairs.get("tekk:tell"));
            dictionary.edit().clear().commit();
            corrections.edit().clear().putBoolean(
                    PersonalLearning.ENABLED,false).commit();
            context.edit().clear().commit();
            freq.edit().clear().commit();
            LearningTransferStore.previewV3(ctx,decoded);
            assertTrue("preview has zero writes",freq.getAll().isEmpty());
            LearningTransferStore.importV3Confirmed(ctx,decoded);
            assertEquals(7,freq.getInt("word:continue",0));
            assertEquals(9,freq.getInt("word:my",0));
            assertEquals(5,freq.getInt("word:customword",0));
            assertEquals(4,context.getInt("used:please:tell",0));
            assertEquals(2,corrections.getInt("accepted:tekk:tell",0));
            assertEquals("Customword",dictionary.getString("word:customword",null));
            assertFalse("Import must not turn Learning on",PersonalLearning.enabled(ctx));
            LearningTransferStore.importV3Confirmed(ctx,decoded);
            assertEquals(7,freq.getInt("word:continue",0));
            assertEquals(3,freq.getAll().size());
            LearningTransferStore.importV3Confirmed(ctx,FrequencyBackupFormat330.decode(v1));
            LearningTransferStore.importV3Confirmed(ctx,FrequencyBackupFormat330.decode(v2));
            assertEquals(7,freq.getInt("word:continue",0));
        } finally {
            restore(freq,f);restore(context,c);restore(corrections,p);restore(dictionary,d);
        }
    }
    @Test public void unapprovedFrequencyRejectedBeforeAnyWrites() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences dict=ctx.getSharedPreferences(
                "orivra_manual_dictionary_v2",Context.MODE_PRIVATE);
        SharedPreferences freq=ctx.getSharedPreferences(
                PersonalWordFrequency328.PREFS,Context.MODE_PRIVATE);
        Map<String,?> oldD=new HashMap<>(dict.getAll());
        Map<String,?> oldF=new HashMap<>(freq.getAll());
        try {
            dict.edit().clear().commit();freq.edit().clear().commit();
            String forged=FrequencyBackupFormat330.encode(
                    new ContextBackupFormat322.Data(
                        new LearningTransferCodec.Bundle(
                            List.of("Newmanual"),Map.of()),Map.of()),
                    Map.of("unsavedzz",4));
            boolean refused=false;
            try{LearningTransferStore.importV3Confirmed(ctx,
                    FrequencyBackupFormat330.decode(forged));}
            catch(IllegalArgumentException invalid){refused=true;}
            assertTrue(refused);
            assertFalse(dict.contains("word:newmanual"));
            assertTrue(freq.getAll().isEmpty());
        } finally {
            restore(freq,oldF);restore(dict,oldD);
        }
    }
}
