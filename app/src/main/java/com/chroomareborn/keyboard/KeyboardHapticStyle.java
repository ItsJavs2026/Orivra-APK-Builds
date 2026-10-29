package com.chroomareborn.keyboard;

/**
 * Pure-Java feedback profile policy; intentionally no direct vibration API.
 * Android handles per-device strength, silent settings and accessibility policy.
 */
final class KeyboardHapticStyle {
    static final int OFF=0;
    static final int LIGHT=1;
    static final int STANDARD=2;
    static final int CRISP=3;

    private KeyboardHapticStyle() {}

    static int normalize(int mode) {
        return mode>=OFF && mode<=CRISP ? mode : STANDARD;
    }

    static int next(int mode) {
        return (normalize(mode)+1)%4;
    }

    static String label(int mode) {
        switch(normalize(mode)) {
            case OFF: return "OFF";
            case LIGHT: return "LIGHT";
            case CRISP: return "CRISP";
            default: return "STANDARD";
        }
    }

    static boolean enabled(int mode) {
        return normalize(mode)!=OFF;
    }
}
