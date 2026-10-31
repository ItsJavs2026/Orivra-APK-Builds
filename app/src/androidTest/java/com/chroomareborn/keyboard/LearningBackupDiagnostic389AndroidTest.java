package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** All samples fictional. Never reads personal data, no output files. */
@RunWith(AndroidJUnit4.class)
public final class LearningBackupDiagnostic389AndroidTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final String TEST_WORD="laboratory";
    private String validV4(){
        return TypedLearningV4Codec341.encode(
                LearningTransferStore.exportV3(context),
                Map.of("i",1,TEST_WORD,2),
                Map.of("i laboratory",1));
    }
    @Test public void validV4PreflightIsReadOnlyAndShowsOnlyAggregateCounts(){
        String before=LearningTransferStore.exportV3(context);
        TypedWordEvidenceStore341.Snapshot original=
                TypedWordEvidenceStore341.read(context);
        LearningBackupDiagnostic389.Check result=
                LearningBackupDiagnostic389.checkText(context,validV4());
        assertEquals(LearningBackupDiagnostic389.Status.READY,result.status);
        assertEquals(2,result.typedWords);
        assertEquals(1,result.typedPairs);
        assertFalse(result.message().contains(TEST_WORD));
        assertEquals(before,LearningTransferStore.exportV3(context));
        assertEquals(original.words,TypedWordEvidenceStore341.read(context).words);
        assertEquals(original.pairs,TypedWordEvidenceStore341.read(context).pairs);
    }
    @Test public void legacyV3IsRecognizedWithoutTypedEvidence(){
        LearningBackupDiagnostic389.Check result=
                LearningBackupDiagnostic389.checkText(
                        context,LearningTransferStore.exportV3(context));
        assertEquals(LearningBackupDiagnostic389.Status.READY,result.status);
        assertEquals(0,result.typedWords);
        assertEquals(0,result.typedPairs);
    }
    @Test public void changedChecksumNeverTouchesLearning(){
        String original=validV4();
        String changed=original.replace("VOC\ti\t1\n","VOC\ti\t2\n");
        assertNotEquals(original,changed);
        assertEquals(LearningBackupDiagnostic389.Status.CHECKSUM,
                LearningBackupDiagnostic389.checkText(context,changed).status);
    }
    @Test public void wrongFileAndUnsupportedHeaderReportedSafely(){
        assertEquals(LearningBackupDiagnostic389.Status.WRONG_FILE,
                LearningBackupDiagnostic389.checkText(context,"not a backup").status);
        assertEquals(LearningBackupDiagnostic389.Status.WRONG_FILE,
                LearningBackupDiagnostic389.checkText(
                        context,"ORIVRA_LOCAL_LEARNING\t9\n").status);
    }
    @Test public void alteredLineEndingsRejectedButNotRewritten(){
        String changed=validV4().replace("\n","\r\n");
        assertEquals(LearningBackupDiagnostic389.Status.CHECKSUM,
                LearningBackupDiagnostic389.checkText(context,changed).status);
    }
    @Test public void bomPrefixedOrivraFileIsRecognizedAsAlteredNotWrongFile(){
        String changed=String.valueOf((char)0xFEFF)+validV4();
        assertEquals(LearningBackupDiagnostic389.Status.CHECKSUM,
                LearningBackupDiagnostic389.checkText(context,changed).status);
    }
    @Test public void badDocumentSizeAndUtf8GetSeparatePrivateMessages(){
        assertEquals(LearningBackupDiagnostic389.Status.FILE_TOO_LARGE,
                LearningBackupDiagnostic389.readFailure(
                        new IOException("Document exceeds safe import size")).status);
        assertEquals(LearningBackupDiagnostic389.Status.BAD_ENCODING,
                LearningBackupDiagnostic389.readFailure(
                        new IOException("Invalid UTF-8 document")).status);
        assertEquals(LearningBackupDiagnostic389.Status.FILE_ACCESS,
                LearningBackupDiagnostic389.readFailure(
                        new IOException("Could not open document")).status);
        assertFalse(LearningBackupDiagnostic389.readFailure(
                new IOException("private/path/here")).message().contains("private/path/here"));
    }
}
