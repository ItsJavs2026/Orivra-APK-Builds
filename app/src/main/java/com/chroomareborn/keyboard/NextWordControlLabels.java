package com.chroomareborn.keyboard;

/**
 * Pure UI text for Orivra's opt-in, offline-only next-word seed switch.
 * Always displays the *stored* state; never implies personal phrase learning.
 */
final class NextWordControlLabels {
    private NextWordControlLabels() {}

    static String buttonText(boolean enabled) {
        return enabled ? "NW ON" : "NW OFF";
    }

    // Make the OFF state visible even when the toolbar is collapsed.
    // Do not imply that ordinary correction or SentenceFlow is disabled.
    static String emptyBarText(boolean enabled) {
        return enabled ? "Keep typing • corrections appear here" :
                "NW OFF • next-word suggestions disabled";
    }

    static String contentDescription(boolean enabled) {
        return "Offline next-word suggestions "+(enabled?"on":"off")+
                ". Tap to turn "+(enabled?"off":"on")+
                ". Personal next-word learning is not enabled by this switch.";
    }
}
