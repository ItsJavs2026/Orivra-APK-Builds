package com.chroomareborn.keyboard;

/**
 * Build 410: UI work follows an actual break in typing, not a forced
 * timeout during a continuous burst. Pure policy: no text, editor or views.
 * Scheduling/cancellation belongs to the IME's editor-scoped Handler.
 */
final class IdlePredictionPaint410 {
    static final long QUIET_AFTER_KEY_MS = 210L;
    private long lastKeyMs = -1L;

    long onKey(long uptimeMs) {
        lastKeyMs = uptimeMs;
        return QUIET_AFTER_KEY_MS;
    }

    boolean isQuiet(long uptimeMs) {
        return lastKeyMs < 0L ||
                (uptimeMs >= lastKeyMs &&
                        uptimeMs - lastKeyMs >= QUIET_AFTER_KEY_MS);
    }

    void reset() { lastKeyMs = -1L; }
}
