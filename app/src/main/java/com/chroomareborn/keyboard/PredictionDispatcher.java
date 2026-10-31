package com.chroomareborn.keyboard;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runs slow prediction on a single background worker, never the IME UI thread.
 * Interrupts outdated queued/running work and tags results so stale
 * suggestions cannot replace fresh ones. No text is logged, persisted,
 * or sent to a network.
 */
final class PredictionDispatcher implements AutoCloseable {
    interface Listener {
        void onResult(long generation, String input, List<String> suggestions);
    }
    /** 412: the same background worker prepares split text for the strip. */
    interface SplitListener412 {
        void onResult(long generation, String input, List<String> suggestions,
                List<String> splitPhrases, List<String> nextWords);
    }
    /** Test seam for deterministic cancellation races; production remains offline. */
    interface Predictor {
        List<String> predict(String input, boolean editing);
    }
    // Candidate 350: reduce a guaranteed 40 ms wait before EVERY async
    // prediction. Short everyday words take the fast lane; expensive long
    // sentence reconstruction retains a longer delay to avoid overworking
    // the phone during continuous rapid typing.
    static long debounceMillisFor(String input,boolean editing) {
        if(editing)return 14L;
        int length=input==null?0:input.length();
        if(length<=12)return 12L;
        if(length<=28)return 20L;
        return 28L;
    }
    private final Predictor predictor;
    private final ScheduledThreadPoolExecutor worker;
    private final AtomicLong generation = new AtomicLong();
    private ScheduledFuture<?> scheduled;
    private volatile boolean closed;
    PredictionDispatcher() {
        this(true);
    }
    // 409 diagnostic: disable eager model warming when comparing typing
    // without predictions. Normal callers retain the original behavior.
    PredictionDispatcher(boolean warmOnStartup) {
        this((input, editing) -> editing
                ? PredictionLite.editingSuggestions(input)
                : PredictionLite.suggestions(input));
        if(warmOnStartup) {
            // Only a known PUBLIC, non-user word is used for warming.
            worker.execute(()->{
                if(!closed)PredictionLite.publicWordForContext("hello");
            });
        }
    }
    PredictionDispatcher(Predictor predictor) {
        if (predictor == null) throw new IllegalArgumentException("Predictor required");
        this.predictor = predictor;
        worker = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "ChroomaOfflinePredictor");
            t.setDaemon(true);
            // MIN_PRIORITY may starve full predictions while the IME is
            // rendering fast taps. Keep this BELOW the UI priority but
            // sufficiently scheduled on Android when the user pauses.
            t.setPriority(Thread.NORM_PRIORITY-1);
            return t;
        });
        worker.setRemoveOnCancelPolicy(true);
    }
    private boolean publicWarmScheduled410;
    /**
     * Initialize only bundled public prediction data on the existing
     * background worker. Never trigger large lexicon initialization from
     * the FULL mode's suggestion view on the Android main thread.
     */
    synchronized void warmPublicDictionary410() {
        if (closed || publicWarmScheduled410) return;
        publicWarmScheduled410=true;
        worker.execute(()->{
            if (!closed) PredictionLite.publicWordForContext("hello");
        });
    }
    synchronized long schedule(String input, Listener listener) {
        return scheduleInternal(input, listener, null, false, false);
    }
    synchronized long scheduleEditing(String input, Listener listener) {
        return scheduleInternal(input, listener, null, true, false);
    }
    synchronized long scheduleWithSplit412(
            String input, boolean splitEnabled, SplitListener412 listener) {
        if(listener==null)throw new IllegalArgumentException("listener required");
        return scheduleInternal(input, null, listener, false, splitEnabled);
    }
    private synchronized long scheduleInternal(String input,
            Listener listener, SplitListener412 splitListener,
            boolean editing, boolean splitEnabled) {
        if (closed) return -1L;
        long id = generation.incrementAndGet();
        if (scheduled != null && scheduled.cancel(true))
            OrivraPerfAudit411.recordPredictionPreempted415();
        final String snapshot=input;
        OrivraPerfAudit411.recordPredictionScheduled415();
        scheduled=worker.schedule(()->{
            try {
                if(!isCurrent(id))return;
                final long predictStart411=OrivraPerfAudit411.begin();
                List<String> choices;
                try {
                    choices=predictor.predict(snapshot, editing);
                } finally {
                    OrivraPerfAudit411.end(
                            OrivraPerfAudit411.PREDICT_WORKER,predictStart411);
                }
                if(!isCurrent(id))return;
                if(splitListener!=null) {
                    List<String> phrases=java.util.Collections.emptyList();
                    List<String> next=java.util.Collections.emptyList();
                    if(splitEnabled) {
                        // Never do this typo segmentation from renderSuggestions
                        // (the Android UI thread). A cancelled request does
                        // not deliver stale candidates to the next editor.
                        final long splitStart412=OrivraPerfAudit411.begin();
                        try {
                            phrases=PredictionLite.splitPhraseSuggestions(snapshot);
                            if(!isCurrent(id))return;
                            next=PredictionLite.splitNextWords(snapshot);
                        } finally {
                            OrivraPerfAudit411.end(
                                    OrivraPerfAudit411.SPLIT_WORKER_412,splitStart412);
                        }
                    }
                    if(isCurrent(id)) {
                        OrivraPerfAudit411.recordPredictionCompleted415();
                        splitListener.onResult(
                                id,snapshot,choices,phrases,next);
                    }
                } else if(isCurrent(id)) {
                    OrivraPerfAudit411.recordPredictionCompleted415();
                    listener.onResult(id,snapshot,choices);
                }
            } finally {
                clearCompleted(id);
            }
        }, debounceMillisFor(snapshot,editing),TimeUnit.MILLISECONDS);
        return id;
    }
    private synchronized void clearCompleted(long id) {
        if (generation.get() == id) scheduled = null;
    }
    boolean isCurrent(long id) {
        return !closed && generation.get() == id;
    }
    synchronized void cancel() {
        generation.incrementAndGet();
        if (scheduled != null) {
            if(scheduled.cancel(true))
                OrivraPerfAudit411.recordPredictionPreempted415();
            scheduled = null;
        }
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        cancel();
        worker.shutdownNow();
    }
}
