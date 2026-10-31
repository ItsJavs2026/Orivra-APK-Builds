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
    synchronized long schedule(String input, Listener listener) {
        return scheduleInternal(input, listener, false);
    }
    synchronized long scheduleEditing(String input, Listener listener) {
        return scheduleInternal(input, listener, true);
    }
    private synchronized long scheduleInternal(String input, Listener listener, boolean editing) {
        if (closed) return -1L;
        long id = generation.incrementAndGet();
        // Interrupt a superseded computation, not just a queued debounce.
        // The offline prediction loops check interruption between batches.
        if (scheduled != null) scheduled.cancel(true);
        final String snapshot = input;
        scheduled = worker.schedule(() -> {
            try {
                if (!isCurrent(id)) return;
                List<String> choices = predictor.predict(snapshot, editing);
                if (isCurrent(id)) listener.onResult(id, snapshot, choices);
            } finally {
                // Release references to finished input/listener captures instead
                // of retaining them inside the last ScheduledFuture.
                clearCompleted(id);
            }
        }, debounceMillisFor(snapshot,editing), TimeUnit.MILLISECONDS);
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
            scheduled.cancel(true);
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
