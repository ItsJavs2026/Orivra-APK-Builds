package com.chroomareborn.keyboard;

/**
 * Pure decision rules for optional offline exact-word dictionary activation.
 * Default OFF; never loads or uses the extra dictionary in sensitive fields.
 * Does not examine, log, learn, upload or retain editor text.
 */
final class OfflineDictionaryActivationPolicy {
    static final String PREFS="keyboard_feedback";
    static final String KEY="orivra_offline_exact_dictionary_v1";
    private OfflineDictionaryActivationPolicy() {}

    static boolean usable(boolean userOptedIn,boolean sensitive) {
        return userOptedIn && !sensitive;
    }

    static boolean startWorker(boolean userOptedIn,boolean sensitive,
            boolean alreadyStarted,boolean hasLoadedPack) {
        return usable(userOptedIn,sensitive) && !alreadyStarted && !hasLoadedPack;
    }

    static boolean acceptCallback(boolean userOptedIn,boolean sensitive,
            boolean sameWorker,boolean validPack) {
        return usable(userOptedIn,sensitive) && sameWorker && validPack;
    }
}
