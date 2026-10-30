package com.chroomareborn.keyboard;

import android.content.Context;
import java.util.List;
import java.util.Map;
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
    /** Atomic view of one editor session, its evidence and pre-ranked lookup.
     * The worker constructs the index; the IME only reads this immutable ref.
     * A session carried inside the snapshot forbids stale-worker publishing.
     */
    private static final class Display {
        final long session;
        final TypedWordEvidenceStore341.Snapshot evidence;
        final LearnedSuggestionIndex356 index;
        Display(long id,TypedWordEvidenceStore341.Snapshot values,
                LearnedSuggestionIndex356 prepared) {
            session=id;
            evidence=values;
            index=prepared;
        }
    }
    private static final Display EMPTY_DISPLAY=new Display(-1L,EMPTY,
            LearnedSuggestionIndex356.fromVerifiedEvidence(Map.of(),Map.of()));
    private volatile long visibleSession=-1L;
    private volatile Display display=EMPTY_DISPLAY;
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
        display=EMPTY_DISPLAY;
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
                        // Precompute on this low-priority worker, never when
                        // handling a key or rendering the suggestion strip.
                        Display prepared=new Display(session,snap,
                                LearnedSuggestionIndex356.fromVerifiedEvidence(
                                        snap.words,snap.pairs));
                        if(visibleSession==session &&
                                TypedWordsConsent341.mayRemember(app,true) &&
                                epoch==TypedWordsConsent341.generation(app)) {
                            consentEpoch=epoch;
                            display=prepared;
                        }
                    }
                }
            } catch(RuntimeException malformedOrUnavailable) {
                // Fail CLOSED: never modify corrupt private storage.
                consentEpoch=-1L;
                if(visibleSession==session)display=EMPTY_DISPLAY;
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
                            .mayRemember(app,true))display=EMPTY_DISPLAY;
                    return;
                }
                Display prepared=new Display(editorSession,updated.evidence,
                        LearnedSuggestionIndex356.fromVerifiedEvidence(
                                updated.evidence.words,updated.evidence.pairs));
                priorWord=word;
                // Consent/session may change while the worker prepares the
                // index. Re-check before any new suggestions become visible.
                if(visibleSession==editorSession &&
                        TypedWordsConsent341.mayRemember(app,true) &&
                        consentEpoch==TypedWordsConsent341.generation(app))
                    display=prepared;
                else if(visibleSession==editorSession)
                    display=EMPTY_DISPLAY;
            } catch(RuntimeException invalidState) {
                // Never delete/rewrite unknown data. No user text in logs.
                priorWord=null;
                if(visibleSession==editorSession)display=EMPTY_DISPLAY;
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
            display=EMPTY_DISPLAY;
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

    /** Indexed, explicit-tap-only suggestions. No scan, sorting, regex,
     * SharedPreferences, or mutable builder work on the UI thread.
     */
    List<String> nextWords(String preceding,int limit,long editorSession) {
        if(closed || visibleSession!=editorSession || limit<=0)return List.of();
        Display snapshot=display;
        if(snapshot.session!=editorSession)return List.of();
        return snapshot.index.nextWords(preceding,limit);
    }

    /** A candidate is still learned only after three verified uses.
     * Every returned value was built from the user's opt-in evidence.
     */
    List<String> completedWords(String prefix,int limit,long editorSession) {
        if(closed || visibleSession!=editorSession || limit<=0)return List.of();
        Display snapshot=display;
        if(snapshot.session!=editorSession)return List.of();
        return snapshot.index.completedWords(prefix,limit);
    }

    int visibleWordCount(long editorSession) {
        Display snapshot=display;
        return !closed && visibleSession==editorSession &&
                snapshot.session==editorSession?
                snapshot.evidence.words.size():0;
    }

    /** Deterministic Android test seam. Never call from the IME main thread. */
    Future<?> workerIdleForTest() {
        return worker.submit(()->{});
    }

    @Override public void close(){
        if(closed)return;
        closed=true;
        display=EMPTY_DISPLAY;
        visibleSession=-1L;
        // Do not abandon previously queued verified Space commits.
        worker.shutdown();
    }
}
