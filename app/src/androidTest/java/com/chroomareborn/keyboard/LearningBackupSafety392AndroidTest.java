package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research 392: deliberately fictional local V4 records only.
 * Uses a test-only SharedPreferences namespace and app-private scratch files.
 */
@RunWith(AndroidJUnit4.class)
public final class LearningBackupSafety392AndroidTest {
    private static final String PREFIX="r392_test_only_";
    private static final String[] PREFS={
        "orivra_manual_dictionary_v2",
        PersonalLearning.PREFS,
        PersonalContextLearning319.PREFS,
        PersonalWordFrequency328.PREFS,
        TypedWordEvidenceStore341.PREFS,
        TypedWordsConsent341.PREFS
    };
    private Context base,isolated;
    private File temp;
    private Uri selected;
    @Before public void before(){
        base=InstrumentationRegistry.getInstrumentation().getTargetContext();
        isolated=new ContextWrapper(base){
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                return base.getSharedPreferences(PREFIX+name,mode);
            }
            @Override public Context getApplicationContext(){return this;}
        };
        wipeTestPreferences();
        temp=new File(base.getCacheDir(),"orivra-r392-test-export.txt");
        if(temp.exists())assertTrue(temp.delete());
        selected=Uri.fromFile(temp);
    }
    @After public void after(){
        if(temp!=null && temp.exists())assertTrue(temp.delete());
        wipeTestPreferences();
    }
    private void wipeTestPreferences(){
        for(String name:PREFS)
            assertTrue(base.getSharedPreferences(PREFIX+name,
                    Context.MODE_PRIVATE).edit().clear().commit());
    }
    @Test public void exportRoundTripVerifiesExactReadbackAndChecksum() throws Exception {
        String original=TypedLearningV4Transfer347.exportV4(isolated);
        LearningBackupSafety392.ExportResult outcome=
                LearningBackupSafety392.exportAndVerify(isolated,selected);
        assertEquals(LearningBackupSafety392.ExportState.VERIFIED,outcome.state);
        String actual=LearningTransferDocumentIO.read(isolated,selected);
        assertEquals(original,actual);
        TypedLearningV4Codec341.Data decoded=TypedLearningV4Codec341.decode(actual);
        assertEquals(0,decoded.words.size());
        assertEquals(0,decoded.pairs.size());
        assertEquals(original,TypedLearningV4Transfer347.exportV4(isolated));
        assertFalse(PersonalLearning.enabled(isolated));
        assertFalse(TypedWordsConsent341.enabled(isolated));
    }
    @Test public void changedBytesAfterSuccessfulWriteNeverCountAsVerified() throws Exception {
        String original=TypedLearningV4Transfer347.exportV4(isolated);
        LearningTransferDocumentIO.write(isolated,selected,original);
        String changed=original+"not-a-valid-footer";
        try(FileOutputStream out=new FileOutputStream(temp,false)){
            out.write(changed.getBytes(StandardCharsets.UTF_8));
        }
        LearningBackupSafety392.ExportResult result=
                LearningBackupSafety392.verifyWritten(isolated,selected,original);
        assertEquals(LearningBackupSafety392.ExportState.SAVED_BYTES_DIFFER,result.state);
        assertFalse(result.display().contains("not-a-valid-footer"));
        assertEquals(original,TypedLearningV4Transfer347.exportV4(isolated));
    }
    @Test public void otherValidV4ContentDoesNotPassExpectedReadback() throws Exception {
        String original=TypedLearningV4Transfer347.exportV4(isolated);
        String other=TypedLearningV4Codec341.encode(
                LearningTransferStore.exportV3(isolated),
                Map.of("continue",1),Map.of());
        assertNotEquals(original,other);
        LearningTransferDocumentIO.write(isolated,selected,other);
        assertEquals(LearningBackupSafety392.ExportState.SAVED_BYTES_DIFFER,
                LearningBackupSafety392.verifyWritten(
                        isolated,selected,original).state);
        assertNotNull(TypedLearningV4Codec341.decode(other));
    }
    @Test public void unreadableDocumentIsUnverifiedNotFalseCorruption(){
        String original=TypedLearningV4Transfer347.exportV4(isolated);
        assertFalse(temp.exists());
        LearningBackupSafety392.ExportResult result=
                LearningBackupSafety392.verifyWritten(isolated,selected,original);
        assertEquals(LearningBackupSafety392.ExportState.WRITTEN_NOT_VERIFIED,result.state);
        assertTrue(result.display().contains("could not reopen"));
        assertFalse(temp.exists());
    }
    @Test public void impossibleWriteFailsClosedWithoutErasingMemory(){
        String before=TypedLearningV4Transfer347.exportV4(isolated);
        Uri folder=Uri.fromFile(base.getCacheDir());
        assertEquals(LearningBackupSafety392.ExportState.WRITE_FAILED,
                LearningBackupSafety392.exportAndVerify(isolated,folder).state);
        assertEquals(before,TypedLearningV4Transfer347.exportV4(isolated));
    }
    @Test public void healthCountsAreReadOnlyAndNeverShowLearnedWords(){
        String baseV3=LearningTransferStore.exportV3(isolated);
        String content=TypedLearningV4Codec341.encode(
                baseV3,Map.of("continue",3,"secretword",2),
                Map.of("continue secretword",1));
        TypedLearningV4Transfer347.importConfirmed(isolated,
                TypedLearningV4Codec341.decode(content));
        String before=TypedLearningV4Transfer347.exportV4(isolated);
        LearningBackupSafety392.Health health=LearningBackupSafety392.inspect(isolated);
        assertTrue(health.valid);
        assertEquals(0,health.corrections);
        assertEquals(0,health.contexts);
        assertEquals(2,health.typedWords);
        assertEquals(1,health.typedPairs);
        assertEquals(before.getBytes(StandardCharsets.UTF_8).length,health.bytes);
        String displayed=health.display();
        assertFalse(displayed.contains("secretword"));
        assertFalse(displayed.contains("continue"));
        assertTrue(displayed.contains("Typed vocabulary: 2/192"));
        assertEquals(before,TypedLearningV4Transfer347.exportV4(isolated));
        assertFalse(PersonalLearning.enabled(isolated));
        assertFalse(TypedWordsConsent341.enabled(isolated));
    }
    @Test public void healthAdvisoryTracksNewCapsAndProtectsEmptySnapshot(){
        LearningBackupSafety392.Health actual=
                LearningBackupSafety392.inspect(isolated);
        assertTrue(actual.valid);
        assertFalse(actual.nearingCapacity());
        LearningBackupSafety392.Health near=
                new LearningBackupSafety392.Health(true,1,304,242,326,58,30,14906);
        assertTrue("326/400 is above 80% advisory",
                near.nearingCapacity());
        assertTrue(near.display().contains("Word frequencies: 326/400"));
        assertTrue(near.display().contains("384"));
        assertTrue(near.display().contains("320"));
        LearningBackupSafety392.Health below=
                new LearningBackupSafety392.Health(true,1,300,240,319,58,30,14906);
        assertFalse(below.nearingCapacity());
        LearningBackupSafety392.Health unavailable=
                LearningBackupSafety392.Health.unavailable();
        assertFalse(unavailable.valid);
        assertFalse(unavailable.display().contains("secretword"));
    }
}
