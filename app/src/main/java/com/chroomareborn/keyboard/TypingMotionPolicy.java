package com.chroomareborn.keyboard;

/**
 * Reversible visual feedback only. The View keeps its layout bounds and the
 * InputMethodService owns all touch and text-editing decisions.
 */
public final class TypingMotionPolicy {
    private TypingMotionPolicy() {}
    public static final float DOWN_SCALE=.978f;
    public static final float REST_SCALE=1f;
    public static final long DOWN_MS=32L;
    public static final long RELEASE_MS=56L;

    public static float scale(boolean pressed,boolean animationsEnabled) {
        return !animationsEnabled ? REST_SCALE : pressed ? DOWN_SCALE : REST_SCALE;
    }
    public static long duration(boolean pressed,boolean animationsEnabled) {
        return !animationsEnabled?0:pressed?DOWN_MS:RELEASE_MS;
    }
}
