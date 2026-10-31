package com.chroomareborn.keyboard;

/**
 * Separate visual A/B test, not a change to press order or InputConnection.
 *
 * Trial ON: keep Button.setPressed(), platform ripple, and device haptic;
 * do not start/cancel a ViewPropertyAnimator or shrink/expand the key.
 * Trial OFF: preserve Candidate 365's existing 32/56ms scale feedback.
 */
final class KeyPressVisualPolicy369 {
    private KeyPressVisualPolicy369() {}
    static final String PREFS="keyboard_feedback";
    static final String KEY_INSTANT_FEEDBACK="instant_visual_key_feedback";
    static boolean shouldScaleAnimate(boolean instant,boolean osMotion) {
        return !instant && osMotion;
    }
}
