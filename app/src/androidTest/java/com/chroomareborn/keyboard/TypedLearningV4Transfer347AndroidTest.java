package com.chroomareborn.keyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Candidate 347 checks unified V4 backup including pending 1–2-use counts,
 * validation, idempotency, cancellation safety and older V1–V3 compatibility.
 * Only synthetic words. Test app is a separate debug trial package.
 */
@RunWith(AndroidJUnit4.class)
public final class TypedLearningV4Transfer347AndroidTest {
    private Context context;
    private boolean learningBefore,consentBefore;

    @Before public void before(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        learningBefore=PersonalLearning.enabled(context);
        consentBefore=TypedWordsConsent341.enabled(context);
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        PersonalLearning.setEnabled(context,false);
    }
    @After public void after(){
        assertTrue(TypedWordsConsent341.setEnabled(context,false));
        assertTrue(TypedWordEvidenceStore341.clearAll(context));
        PersonalLearning.setEnabled(context,learningBefore);
        assertTrue(TypedWordsConsent341.setEnabled(context,consentBefore));
    }
    private String v3(){return LearningTransferStore.exportV3(context);}
    private static void rejects(Runnable action){
        try {action.run();fail("Expected input rejection");}
        catch(IllegalArgumentException expected){}
    }
    private static Map<String,Integer> words(){
        return Map.of("i",1,"need",2,"to",2,"continue",3);
    }
    private static Map<String,Integer> pairs(){
        return Map.of("i need",1,"need to",2,"to continue",2);
    }

    @Test public void v4PreservesSingleAndDoubleUseEvidence(){
        String original=v3();
        String encoded=TypedLearningV4Codec341.encode(original,words(),pairs());
        assertTrue(encoded.startsWith(TypedLearningV4Codec341.HEADER+"\n"));
        TypedLearningV4Codec341.Data decoded=
                TypedLearningV4Codec341.decode(encoded);
        assertEquals(words(),decoded.words);
        assertEquals(pairs(),decoded.pairs);
        assertEquals(1,decoded.words.get("i").intValue());
        assertEquals(2,decoded.pairs.get("to continue").intValue());
        assertEquals(original,FrequencyBackupFormat330.encode(
                decoded.previous.earlier,decoded.previous.frequencies));
    }

    @Test public void legacyV1V2V3RemainReadableWithoutInventedTypedData(){
        for(String old:List.of(
                LearningTransferStore.exportText(context),
                LearningTransferStore.exportV2(context),v3())){
            TypedLearningV4Codec341.Data decoded=
                    TypedLearningV4Codec341.decode(old);
            assertTrue(decoded.words.isEmpty());
            assertTrue(decoded.pairs.isEmpty());
            assertNotNull(decoded.previous);
        }
    }

    @Test public void damagedChecksumCannotMutateStorage(){
        String encoded=TypedLearningV4Codec341.encode(v3(),words(),pairs());
        String corrupted=encoded.replace("VOC\ti\t1\n","VOC\ti\t9\n");
        assertNotEquals(encoded,corrupted);
        rejects(()->TypedLearningV4Codec341.decode(corrupted));
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void orphanedOrOvercountedPairRejectedBeforeImport(){
        rejects(()->TypedLearningV4Codec341.encode(v3(),
                Map.of("i",1,"need",1),Map.of("i unknown",1)));
        rejects(()->TypedLearningV4Codec341.encode(v3(),
                Map.of("i",1,"need",1),Map.of("i need",2)));
        assertEquals(0,TypedWordEvidenceStore341.read(context).associations());
    }

    @Test public void v4PreviewDoesNotModifyDataAndImportIsIdempotent(){
        TypedLearningV4Codec341.Data proposed=TypedLearningV4Codec341.decode(
                TypedLearningV4Codec341.encode(v3(),words(),pairs()));
        String existingV3=v3();
        TypedLearningV4Transfer347.Preview preview=
                TypedLearningV4Transfer347.preview(context,proposed);
        assertEquals(4,preview.savedTypedWords);
        assertEquals(3,preview.savedTypedPairs);
        assertEquals(4,preview.newTypedWords);
        assertEquals(3,preview.newTypedPairs);
        assertEquals(0,TypedWordEvidenceStore341.read(context).vocabulary());
        TypedLearningV4Transfer347.importConfirmed(context,proposed);
        assertEquals(words(),TypedWordEvidenceStore341.read(context).words);
        assertEquals(pairs(),TypedWordEvidenceStore341.read(context).pairs);
        assertEquals(existingV3,v3());
        TypedLearningV4Transfer347.Preview repeated=
                TypedLearningV4Transfer347.importConfirmed(context,proposed);
        assertEquals(0,repeated.newTypedWords);
        assertEquals(0,repeated.newTypedPairs);
        assertEquals(words(),TypedWordEvidenceStore341.read(context).words);
        assertEquals(pairs(),TypedWordEvidenceStore341.read(context).pairs);
    }

    @Test public void exportIncludesPendingAndAlwaysUsesOneFile(){
        PersonalLearning.setEnabled(context,true);
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
        long gen=TypedWordsConsent341.generation(context);
        assertTrue(TypedWordBoundaryStore346.record(context,"continue",null,
                true,true,gen).stored);
        assertTrue(TypedWordBoundaryStore346.record(context,"continue",null,
                true,true,gen).stored);
        String exported=TypedLearningV4Transfer347.exportV4(context);
        TypedLearningV4Codec341.Data parsed=TypedLearningV4Codec341.decode(exported);
        assertEquals(2,parsed.words.get("continue").intValue());
        assertTrue(parsed.pairs.isEmpty());
    }

    @Test public void importingV4NeverTurnsOnLearningPermission(){
        assertFalse(TypedWordsConsent341.enabled(context));
        String file=TypedLearningV4Codec341.encode(v3(),words(),pairs());
        TypedLearningV4Transfer347.importConfirmed(
                context,TypedLearningV4Codec341.decode(file));
        assertFalse(TypedWordsConsent341.enabled(context));
        assertFalse(PersonalLearning.enabled(context));
        assertEquals(4,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void importInvalidatesQueuedWorkerConsentEpoch(){
        PersonalLearning.setEnabled(context,true);
        assertTrue(TypedWordsConsent341.setEnabled(context,true));
        long oldEpoch=TypedWordsConsent341.generation(context);
        String file=TypedLearningV4Codec341.encode(v3(),words(),pairs());
        TypedLearningV4Transfer347.importConfirmed(
                context,TypedLearningV4Codec341.decode(file));
        assertTrue(TypedWordsConsent341.generation(context)!=oldEpoch);
        assertFalse(TypedWordBoundaryStore346.record(context,"newword",null,
                true,true,oldEpoch).stored);
        assertEquals(4,TypedWordEvidenceStore341.read(context).vocabulary());
    }

    @Test public void capacityOverflowFailsBeforeModifyingLegacyLearning(){
        String before=v3();
        TreeMap<String,Integer> tooMany=new TreeMap<>();
        for(int i=0;i<=TypedSequenceArchive341.MAX_WORDS;i++){
            String key="test"+(char)('a'+(i/26)%26)+(char)('a'+i%26);
            tooMany.put(key,1);
        }
        rejects(()->TypedLearningV4Codec341.encode(before,tooMany,Map.of()));
        assertEquals(before,v3());
        assertTrue(TypedWordEvidenceStore341.read(context).words.isEmpty());
    }

    @Test public void higherCountsMergeByMaxAndNoOrphan(){
        String base=v3();
        TypedLearningV4Transfer347.importConfirmed(context,
                TypedLearningV4Codec341.decode(
                        TypedLearningV4Codec341.encode(base,words(),pairs())));
        TypedLearningV4Transfer347.importConfirmed(context,
                TypedLearningV4Codec341.decode(
                        TypedLearningV4Codec341.encode(base,
                                Map.of("i",2,"need",2,"to",2,"continue",8),
                                Map.of("i need",1,"need to",2,"to continue",2))));
        TypedWordEvidenceStore341.Snapshot snap=
                TypedWordEvidenceStore341.read(context);
        assertEquals(2,snap.words.get("i").intValue());
        assertEquals(8,snap.words.get("continue").intValue());
        assertEquals(2,snap.pairs.get("to continue").intValue());
        assertEquals(1,snap.pairs.get("i need").intValue());
    }
}
