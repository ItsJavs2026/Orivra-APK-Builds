package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bounded, private, local-only research store. Not called by the live IME.
 * IMPORTANT: This can contain identifying names or words even if no digits.
 * A separate default-OFF remember switch AND existing learning must be ON,
 * and the editor must allow personal learning before saving.
 *
 * Words seen only 1-2 times ARE kept privately after explicit opt-in so that
 * repeated occurrences across separate editor sessions can reach 3. They are
 * neither displayed as learned suggestions nor exported until count >= 3.
 *
 * NO automatic backup, network, identifiers, whole sentences or timestamps.
 * Mutation runs off the key-input/main thread and changes one private prefs
 * file in one atomic commit. All input validated BEFORE storage mutation.
 */
final class TypedWordEvidenceStore341 {
    static final String PREFS="orivra_typed_word_evidence_341";
    private static final String WORD="w:";
    private static final String PAIR="p:";
    private TypedWordEvidenceStore341(){}

    static final class Snapshot {
        final Map<String,Integer> words,pairs;
        Snapshot(Map<String,Integer> w,Map<String,Integer> p) {
            words=Collections.unmodifiableMap(new TreeMap<>(w));
            pairs=Collections.unmodifiableMap(new TreeMap<>(p));
        }
        int vocabulary(){return words.size();}
        int associations(){return pairs.size();}
        int confirmedWords() {
            int n=0;
            for(int x:words.values())if(x>=TypedSequenceArchive341.MIN_EVIDENCE)n++;
            return n;
        }
        int confirmedPairs() {
            int n=0;
            for(int x:pairs.values())if(x>=TypedSequenceArchive341.MIN_EVIDENCE)n++;
            return n;
        }
    }

    static synchronized Snapshot read(Context context) {
        if(context==null)throw new IllegalArgumentException("Missing context");
        Map<String,?> stored=context.getSharedPreferences(
                PREFS,Context.MODE_PRIVATE).getAll();
        TreeMap<String,Integer> words=new TreeMap<>(),pairs=new TreeMap<>();
        for(Map.Entry<String,?> e:stored.entrySet()) {
            String k=e.getKey();
            if(!(e.getValue() instanceof Integer))
                throw new IllegalArgumentException("Non-numeric evidence value");
            int count=(Integer)e.getValue();
            if(k.startsWith(WORD)) {
                String word=k.substring(WORD.length());
                if(words.putIfAbsent(word,count)!=null)
                    throw new IllegalArgumentException("Repeated word key");
            }else if(k.startsWith(PAIR)) {
                String pair=k.substring(PAIR.length());
                if(pairs.putIfAbsent(pair,count)!=null)
                    throw new IllegalArgumentException("Repeated pair key");
            }else throw new IllegalArgumentException("Unexpected evidence key");
        }
        TypedSequenceArchive341.verifyRawEvidence(words,pairs);
        return new Snapshot(words,pairs);
    }

    /** New archive for one editor session; caller MUST call on a worker.
     * Never restores prior editor context, only count evidence.
     */
    static synchronized TypedSequenceArchive341 load(Context context) {
        enforceWorker();
        Snapshot evidence=read(context);
        TypedSequenceArchive341 model=new TypedSequenceArchive341();
        model.restoreRawEvidence(evidence.words,evidence.pairs);
        return model;
    }

    /** Existing counts are merged by MAX to make a repeated flush idempotent.
     * It is caller's responsibility to pass a model that was loaded from the
     * persisted snapshot at the start of the editor session, then updated
     * only by verified newly typed successful Space boundaries.
     */
    static synchronized boolean persist(Context context,TypedSequenceArchive341 model,
            boolean editorAllowsLearning) {
        enforceWorker();
        if(model==null || !TypedWordsConsent341.mayRemember(
                context,editorAllowsLearning))return false;
        Map<String,Integer> inputWords=model.rawWordEvidence();
        Map<String,Integer> inputPairs=model.rawPairEvidence();
        TypedSequenceArchive341.verifyRawEvidence(inputWords,inputPairs);
        Snapshot current=read(context);
        TreeMap<String,Integer> words=new TreeMap<>(current.words);
        TreeMap<String,Integer> pairs=new TreeMap<>(current.pairs);
        for(Map.Entry<String,Integer> e:inputWords.entrySet())
            words.merge(e.getKey(),e.getValue(),Math::max);
        for(Map.Entry<String,Integer> e:inputPairs.entrySet())
            pairs.merge(e.getKey(),e.getValue(),Math::max);
        TypedSequenceArchive341.verifyRawEvidence(words,pairs);
        // Consent is checked again immediately before storage mutation.
        if(!TypedWordsConsent341.mayRemember(context,editorAllowsLearning))
            return false;
        SharedPreferences.Editor editor=context.getSharedPreferences(
                PREFS,Context.MODE_PRIVATE).edit().clear();
        for(Map.Entry<String,Integer> e:words.entrySet())
            editor.putInt(WORD+e.getKey(),e.getValue());
        for(Map.Entry<String,Integer> e:pairs.entrySet())
            editor.putInt(PAIR+e.getKey(),e.getValue());
        return editor.commit();
    }

    /** Explicit destructive action. Does not delete existing V1/V2/V3 data. */
    static synchronized boolean clearAll(Context context) {
        if(context==null)return false;
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    private static void enforceWorker() {
        if(Looper.getMainLooper()!=null &&
                Looper.getMainLooper().getThread()==Thread.currentThread())
            throw new IllegalStateException(
                    "Never read/write typed vocabulary on the keyboard UI thread");
    }
}
