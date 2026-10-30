package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.TreeMap;

/**
 * User-initiated ONE-FILE V4 learning transfer (candidate 347).
 * Includes V1 words/corrections, V2 context, V3 frequencies and ALL
 * opt-in verified typed-word/pair counts, including pending counts 1-2.
 *
 * No automatic export or import; never invoked from keyboard input code.
 * All callers must execute on the settings Activity's IO worker.
 * Private text may exist in the selected plaintext document; show warning.
 *
 * Legacy V1/V2/V3 imports remain fully supported. All malformed data and
 * capacity conflicts are rejected BEFORE any preferences are modified.
 * Like the pre-existing V3 transfer, several separate preference files are
 * not one filesystem-atomic transaction; disk failures may need retry.
 */
final class TypedLearningV4Transfer347 {
    private TypedLearningV4Transfer347(){}

    static final class Preview {
        final LearningTransferStore.Preview previous;
        final int savedTypedWords,savedTypedPairs,newTypedWords,newTypedPairs;
        final int strongerWords,strongerPairs;
        Preview(LearningTransferStore.Preview v3,
                int words,int pairs,int addedWords,int addedPairs,
                int strengthenedWords,int strengthenedPairs) {
            previous=v3;
            savedTypedWords=words;
            savedTypedPairs=pairs;
            newTypedWords=addedWords;
            newTypedPairs=addedPairs;
            strongerWords=strengthenedWords;
            strongerPairs=strengthenedPairs;
        }
    }

    static String exportV4(Context app) {
        // Source data comes from the same app; older Candidate 345 is
        // intentionally separate and must be exported/imported explicitly.
        String v3=LearningTransferStore.exportV3(app);
        TypedWordEvidenceStore341.Snapshot typed=
                TypedWordEvidenceStore341.read(app);
        return TypedLearningV4Codec341.encode(v3,typed.words,typed.pairs);
    }

    private static final class Plan {
        final TreeMap<String,Integer> words,pairs;
        final Preview preview;
        Plan(TreeMap<String,Integer> w,TreeMap<String,Integer> p,Preview v){
            words=w;pairs=p;preview=v;
        }
    }

    private static Plan plan(Context context,TypedLearningV4Codec341.Data data){
        if(context==null || data==null || data.previous==null ||
                data.words==null || data.pairs==null)
            throw new IllegalArgumentException("No validated V4 data");
        // Round trip both formats before any mutation. This rejects
        // malformed or inconsistent objects produced outside the decoder.
        String base=FrequencyBackupFormat330.encode(
                data.previous.earlier,data.previous.frequencies);
        TypedLearningV4Codec341.Data validated=TypedLearningV4Codec341.decode(
                TypedLearningV4Codec341.encode(base,data.words,data.pairs));
        LearningTransferStore.Preview existing=
                LearningTransferStore.previewV3(context,validated.previous);
        TypedWordEvidenceStore341.Snapshot current=
                TypedWordEvidenceStore341.read(context);
        TreeMap<String,Integer> words=new TreeMap<>(current.words);
        TreeMap<String,Integer> pairs=new TreeMap<>(current.pairs);
        int newW=0,newP=0,greaterW=0,greaterP=0;
        for(Map.Entry<String,Integer> e:validated.words.entrySet()){
            Integer old=words.get(e.getKey());
            if(old==null)newW++;
            else if(e.getValue()>old)greaterW++;
            words.merge(e.getKey(),e.getValue(),Math::max);
        }
        for(Map.Entry<String,Integer> e:validated.pairs.entrySet()){
            Integer old=pairs.get(e.getKey());
            if(old==null)newP++;
            else if(e.getValue()>old)greaterP++;
            pairs.merge(e.getKey(),e.getValue(),Math::max);
        }
        // Reject capacity overflow and orphaned associations BEFORE import.
        TypedSequenceArchive341.verifyRawEvidence(words,pairs);
        return new Plan(words,pairs,
                new Preview(existing,validated.words.size(),
                        validated.pairs.size(),newW,newP,greaterW,greaterP));
    }

    static Preview preview(Context context,TypedLearningV4Codec341.Data data) {
        synchronized(TypedWordEvidenceStore341.class){
            return plan(context,data).preview;
        }
    }

    /** Explicit confirmation only. Preflight is repeated to handle state
     * changed since the preview dialog. Counts merge by MAX, never ADD.
     * Import does not enable private typed-word learning or change its consent.
     */
    static Preview importConfirmed(Context context,TypedLearningV4Codec341.Data data){
        synchronized(TypedWordEvidenceStore341.class){
            Plan verified=plan(context,data);
            // Existing V3 imports already validate every legacy preference
            // before applying changes. New store has also been validated.
            LearningTransferStore.importV3Confirmed(context,data.previous);
            SharedPreferences.Editor edit=context.getSharedPreferences(
                    TypedWordEvidenceStore341.PREFS,Context.MODE_PRIVATE)
                    .edit().clear();
            for(Map.Entry<String,Integer> e:verified.words.entrySet())
                edit.putInt("w:"+e.getKey(),e.getValue());
            for(Map.Entry<String,Integer> e:verified.pairs.entrySet())
                edit.putInt("p:"+e.getKey(),e.getValue());
            if(!edit.commit())
                throw new IllegalStateException("Could not import typed vocabulary");
            // Invalidate queued sessions captured before this import. The
            // current opt-in state is preserved; reopen field to reload.
            if(!TypedWordsConsent341.setEnabled(
                    context,TypedWordsConsent341.enabled(context)))
                throw new IllegalStateException("Could not invalidate stale session");
            return verified.preview;
        }
    }
}
