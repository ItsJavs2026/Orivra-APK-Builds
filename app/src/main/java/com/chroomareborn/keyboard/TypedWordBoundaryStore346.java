package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import java.util.Map;
import java.util.TreeMap;

/**
 * Research-only serial-worker transaction: persist ONE already-verified,
 * successfully committed Space boundary. Never called by the release IME.
 *
 * Unlike older model-snapshot MAX merging, each transaction reads the LATEST
 * private storage counts while holding the same monitor as the existing
 * store and its consent toggle. Concurrent IME instances cannot lose counts.
 * A captured consent generation rejects queued events after OFF/erase/ON.
 *
 * Sensitive words are possible: opt-in defaults OFF, no text logged, no
 * network, no Android Auto Backup, no editor IDs, no whole sentences.
 */
final class TypedWordBoundaryStore346 {
    private TypedWordBoundaryStore346(){}

    static final class Result {
        final boolean stored;
        final TypedWordEvidenceStore341.Snapshot evidence;
        Result(boolean ok,TypedWordEvidenceStore341.Snapshot snapshot) {
            stored=ok; evidence=snapshot;
        }
    }

    static Result record(Context c,String verifiedWord,String precedingCommitted,
            boolean editorAllowsLearning,boolean successfulSpace,
            long capturedConsentGeneration) {
        if(c==null)throw new IllegalArgumentException("Missing app context");
        if(Looper.getMainLooper()!=null &&
                Thread.currentThread()==Looper.getMainLooper().getThread())
            throw new IllegalStateException("Typed-word disk writes must be off IME UI");
        if(!editorAllowsLearning || !successfulSpace)return new Result(false,null);
        String next=TypedSequenceArchive341.canonical(verifiedWord);
        if(next==null)return new Result(false,null);
        synchronized(TypedWordEvidenceStore341.class) {
            if(!TypedWordsConsent341.mayRemember(c,editorAllowsLearning) ||
                    TypedWordsConsent341.generation(c)!=capturedConsentGeneration)
                return new Result(false,null);

            TypedWordEvidenceStore341.Snapshot old=TypedWordEvidenceStore341.read(c);
            TreeMap<String,Integer> words=new TreeMap<>(old.words);
            TreeMap<String,Integer> pairs=new TreeMap<>(old.pairs);
            Integer priorCount=words.get(next);
            if(priorCount==null && words.size()>=TypedSequenceArchive341.MAX_WORDS)
                return new Result(false,null);
            words.put(next,Math.min(TypedSequenceArchive341.MAX_COUNT,
                    (priorCount==null?0:priorCount)+1));

            String pair=TypedSequenceArchive341.pairKey(precedingCommitted,next);
            if(pair!=null && words.containsKey(pair.substring(0,pair.indexOf(' ')))) {
                Integer oldPair=pairs.get(pair);
                if(oldPair!=null || pairs.size()<TypedSequenceArchive341.MAX_PAIRS) {
                    int space=pair.indexOf(' ');
                    int ceiling=Math.min(words.get(pair.substring(0,space)),
                            words.get(pair.substring(space+1)));
                    int newCount=Math.min(ceiling,Math.min(
                            TypedSequenceArchive341.MAX_COUNT,
                            (oldPair==null?0:oldPair)+1));
                    if(newCount>=1)pairs.put(pair,newCount);
                }
            }

            TypedSequenceArchive341.verifyRawEvidence(words,pairs);
            // Both privacy gates are checked inside the consent/store lock,
            // immediately before committing a single private prefs file.
            if(!TypedWordsConsent341.mayRemember(c,editorAllowsLearning) ||
                    TypedWordsConsent341.generation(c)!=capturedConsentGeneration)
                return new Result(false,null);

            SharedPreferences.Editor edit=c.getSharedPreferences(
                    TypedWordEvidenceStore341.PREFS,Context.MODE_PRIVATE)
                    .edit().clear();
            for(Map.Entry<String,Integer> e:words.entrySet())
                edit.putInt("w:"+e.getKey(),e.getValue());
            for(Map.Entry<String,Integer> e:pairs.entrySet())
                edit.putInt("p:"+e.getKey(),e.getValue());
            if(!edit.commit())return new Result(false,null);
            return new Result(true,new TypedWordEvidenceStore341.Snapshot(words,pairs));
        }
    }
}
