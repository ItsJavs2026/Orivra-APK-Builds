package com.chroomareborn.keyboard;

/**
 * Temporary, opt-in diagnostic modes for separating keyboard input latency
 * from prediction CPU and suggestion-view rendering. Only an enum-like
 * integer is stored. No editor content, suggestions or user text is saved.
 */
final class PredictionIsolationMode409 {
    static final int FULL = 0;
    static final int WORKER_ONLY = 1;
    static final int OFF = 2;
    static final String PREF = "orivra_prediction_isolation_409";

    private PredictionIsolationMode409() {}

    static int normalize(int value) {
        return value >= FULL && value <= OFF ? value : OFF;
    }

    static int next(int current) {
        switch (normalize(current)) {
            case OFF: return WORKER_ONLY;
            case WORKER_ONLY: return FULL;
            default: return OFF;
        }
    }

    static String label(int mode) {
        switch (normalize(mode)) {
            case FULL: return "FULL (normal predictions)";
            case WORKER_ONLY: return "WORKER ONLY (no prediction display)";
            default: return "OFF (no prediction processing)";
        }
    }

    static boolean renders(int mode) {
        return normalize(mode) == FULL;
    }

    static boolean computes(int mode) {
        return normalize(mode) != OFF;
    }
}
