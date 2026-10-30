package com.chroomareborn.keyboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Candidate 347 opt-in worker, researched and emulator-qualified in 346.
 *
 * The IME will eventually pass only an already-verified word after a
 * successful Space commit, never partial keys or full editor text.
 * All storage, consent checks, validation and count updates happen on ONE
 * background worker. Session transitions are ordered with recording tasks.
 * Reads on the input thread consume only a volatile immutable snapshot.
 *
 * No storage or callback on the keyboard UI thread, no text logs, no network.
 */
final class TypedWordLearningWorker346 implements AutoCloseable {
    private static final TypedWordEvidenceStore341.Snapshot EMPTY=
            new TypedWordEvidenceStore341.Snapshot(Map.of(),Map.of());
    private final Context app;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"OrivraTypedWords346");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    // Only the worker accesses priorWord, workerSession and consentEpoch.
    private String priorWord;
    private long workerSession=-1L;
    private long consentEpoch=-1L;
    // UI-only reads; updated with immutable snapshots on the worker.
    private volatile long visibleSession=-1L;
    private volatile TypedWordEvidenceStore341.Snapshot visible=EMPTY;
    private volatile boolean closed;

    TypedWordLearningWorker346(Context c) {
        if(c==null)throw new IllegalArgumentException("Missing context");
        app=c.getApplicationContext();
    }

    /** Begins a new editor without blocking the caller. Personal-word data
     * is never allowed to bleed from a previous editor during worker loading.
     */
    void beginEditor(long session,boolean editorAllowsLearning) {
        if(closed)return;
        visible=EMPTY;
        visibleSession=session;
        worker.execute(()->{
            workerSession=session;
            priorWord=null;
            consentEpoch=-1L;
            try{
                if(editorAllowsLearning && TypedWordsConsent341.mayRemember(
                        app,true)) {
                    long epoch=TypedWordsConsent341.generation(app);
                    TypedWordEvidenceStore341.Snapshot snap=
                            TypedWordEvidenceStore341.read(app);
                    if(TypedWordsConsent341.mayRemember(app,true) &&
                            epoch==TypedWordsConsent341.generation(app)) {
                        consentEpoch=epoch;
                        if(visibleSession==session)visible=snap;
                    }
                }
            } catch(RuntimeException malformedOrUnavailable) {
                // Fail CLOSED: never modify corrupt private storage.
                consentEpoch=-1L;
                if(visibleSession==session)visible=EMPTY;
            }
        });
    }

    /** Enqueue only after the caller confirmed ic.commitText(" ",1).
     * The caller owns the session ID and editor privacy decision.
     * A null/invalid token breaks the pair chain without disk mutation.
     */
    void committedSpace(String verifiedWord,long editorSession,
            boolean editorAllowsLearning,boolean successfullyCommitted) {
        if(closed)return;
        final String word=TypedSequenceArchive341.canonical(verifiedWord);
        worker.execute(()->{
            if(workerSession!=editorSession || !editorAllowsLearning ||
                    !successfullyCommitted || consentEpoch<0L || word==null) {
                priorWord=null;
                return;
            }
            try {
                TypedWordBoundaryStore346.Result updated=
                        TypedWordBoundaryStore346.record(
                        app,word,priorWord,true,true,consentEpoch);
                if(!updated.stored) {
                    priorWord=null;
                    if(visibleSession==editorSession && !TypedWordsConsent341
                            .mayRemember(app,true))visible=EMPTY;
                    return;
                }
                priorWord=word;
                if(visibleSession==editorSession)visible=updated.evidence;
            } catch(RuntimeException invalidState) {
                // Never delete/rewrite unknown data. No user text in logs.
                priorWord=null;
                if(visibleSession==editorSession)visible=EMPTY;
            }
        });
    }

    /** A punctuation, navigation or other hard boundary must not form
     * false word pairs across unrelated text. Ordered on the same worker.
     */
    void breakContext(long session) {
        if(closed)return;
        worker.execute(()->{
            if(workerSession==session)priorWord=null;
        });
    }

    void endEditor(long session) {
        if(visibleSession==session) {
            visibleSession=-1L;
            visible=EMPTY;
        }
        if(closed)return;
        worker.execute(()->{
            if(workerSession==session) {
                workerSession=-1L;
                priorWord=null;
                consentEpoch=-1L;
            }
        });
    }

    /** Read-only, tap-only suggestions from qualified associations.
     * No SharedPreferences accesses or locks on this code path.
     */
    List<String> nextWords(String preceding,int limit,long editorSession) {
        if(visibleSession!=editorSession || limit<=0)return List.of();
        String key=TypedSequenceArchive341.canonical(preceding);
        if(key==null)return List.of();
        TypedWordEvidenceStore341.Snapshot s=visible;
        String prefix=key+" ";
        ArrayList<Map.Entry<String,Integer>> matches=new ArrayList<>();
        for(Map.Entry<String,Integer> p:s.pairs.entrySet()) {
            if(p.getValue()>=TypedSequenceArchive341.MIN_EVIDENCE &&
                    p.getKey().startsWith(prefix))
                matches.add(p);
        }
        matches.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                p->-p.getValue()).thenComparing(Map.Entry::getKey));
        ArrayList<String> words=new ArrayList<>();
        for(Map.Entry<String,Integer> p:matches){
            if(words.size()>=Math.min(5,limit))break;
            words.add(p.getKey().substring(prefix.length()));
        }
        return Collections.unmodifiableList(words);
    }

    /** Bounded suggestion from manually typed word evidence; min 3 repeats.
     * Display-only; never inserts or silently autocorrects anything.
     */
    List<String> completedWords(String prefix,int limit,long editorSession) {
        if(visibleSession!=editorSession || limit<=0 || prefix==null ||
                prefix.length()<2 || prefix.length()>24 ||
                !prefix.matches("[A-Za-z]{2,24}"))
            return List.of();
        String needle=prefix.toLowerCase(java.util.Locale.ROOT);
        TypedWordEvidenceStore341.Snapshot s=visible;
        ArrayList<Map.Entry<String,Integer>> matches=new ArrayList<>();
        for(Map.Entry<String,Integer> e:s.words.entrySet()) {
            if(e.getValue()>=TypedSequenceArchive341.MIN_EVIDENCE &&
                    e.getKey().startsWith(needle) && !e.getKey().equals(needle))
                matches.add(e);
        }
        matches.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                e->-e.getValue()).thenComparing(Map.Entry::getKey));
        ArrayList<String> words=new ArrayList<>();
        for(Map.Entry<String,Integer> e:matches) {
            if(words.size()>=Math.min(3,limit))break;
            words.add(e.getKey());
        }
        return Collections.unmodifiableList(words);
    }

    int visibleWordCount(long editorSession) {
        return editorSession==visibleSession?visible.words.size():0;
    }

    /** Deterministic Android test seam. Never call from the IME main thread. */
    Future<?> workerIdleForTest() {
        return worker.submit(()->{});
    }

    @Override public void close(){
        if(closed)return;
        closed=true;
        visible=EMPTY;
        visibleSession=-1L;
        // Do not abandon previously queued verified Space commits.
        worker.shutdown();
    }
}
