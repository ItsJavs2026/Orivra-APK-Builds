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
    private static final long DEBOUNCE_MS = 40;
    private final Predictor predictor;
    private final ScheduledThreadPoolExecutor worker;
    private final AtomicLong generation = new AtomicLong();
    private ScheduledFuture<?> scheduled;
    private volatile boolean closed;
    PredictionDispatcher() {
        this((input, editing) -> editing
                ? PredictionLite.editingSuggestions(input)
                : PredictionLite.suggestions(input));
    }
    PredictionDispatcher(Predictor predictor) {
        if (predictor == null) throw new IllegalArgumentException("Predictor required");
        this.predictor = predictor;
        worker = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "ChroomaOfflinePredictor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
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
            if (!isCurrent(id)) return;
            List<String> choices = predictor.predict(snapshot, editing);
            if (isCurrent(id)) listener.onResult(id, snapshot, choices);
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        return id;
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
