package com.chroomareborn.keyboard;

import android.content.Context;

/** Separate opt-in for remembering newly typed words across editor sessions.
 * This is NOT the existing correction-learning switch. Default is OFF.
 */
final class TypedWordsConsent341 {
    static final String PREFS="orivra_remember_typed_words_opt_in_v1";
    static final String KEY="remember_verified_words";
    // Every consent toggle invalidates queued events from earlier sessions,
    // including rapid off -> erase -> on. Default is generation zero.
    private static final String GENERATION="consent_generation_346";
    private TypedWordsConsent341(){}

    static boolean enabled(Context c) {
        return c!=null && c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getBoolean(KEY,false);
    }

    static boolean mayRemember(Context c,boolean editorAllowsLearning) {
        return editorAllowsLearning && enabled(c) && PersonalLearning.enabled(c);
    }

    static long generation(Context c) {
        return c==null?-1L:c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getLong(GENERATION,0L);
    }

    static boolean setEnabled(Context c,boolean enabled) {
        if(c==null)return false;
        // Consent changes and pending store writes share one lock.
        // Once disabling returns, no queued storage writer can commit
        // with the previous enabled state.
        synchronized(TypedWordEvidenceStore341.class) {
            android.content.SharedPreferences prefs=c.getSharedPreferences(
                    PREFS,Context.MODE_PRIVATE);
            long previous=prefs.getLong(GENERATION,0L);
            long next=previous==Long.MAX_VALUE?0L:previous+1L;
            return prefs.edit().putBoolean(KEY,enabled)
                    .putLong(GENERATION,next).commit();
        }
    }
}
