package com.chroomareborn.keyboard;

/**
 * Limits expensive prediction-strip refreshes during rapid typing.
 * No editor, key contents, predictions, or other user data is retained.
 *
 * Refresh after a short quiet gap, but do not starve predictions while
 * someone types continuously. All times are monotonic uptime milliseconds.
 */
final class PredictionUiBudget408 {
    static final long QUIET_GAP_MS = 150L;
    static final long MAX_BURST_MS = 360L;
    private long burstStartMs = -1L;

    long delayAfterKey(long nowMs) {
        if (burstStartMs < 0L || nowMs < burstStartMs)
            burstStartMs = nowMs;
        long elapsed = Math.max(0L, nowMs - burstStartMs);
        // Delay at least 1ms: heavy render must not run inside a key press.
        return Math.min(QUIET_GAP_MS,
                Math.max(1L, MAX_BURST_MS - elapsed));
    }

    void reset() { burstStartMs = -1L; }
}
