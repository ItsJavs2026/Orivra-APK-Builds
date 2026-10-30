package com.chroomareborn.keyboard;

import android.content.Context;

/** Separate opt-in for remembering newly typed words across editor sessions.
 * This is NOT the existing correction-learning switch. Default is OFF.
 */
final class TypedWordsConsent341 {
    static final String PREFS="orivra_remember_typed_words_opt_in_v1";
    static final String KEY="remember_verified_words";
    private TypedWordsConsent341(){}

    static boolean enabled(Context c) {
        return c!=null && c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getBoolean(KEY,false);
    }

    static boolean mayRemember(Context c,boolean editorAllowsLearning) {
        return editorAllowsLearning && enabled(c) && PersonalLearning.enabled(c);
    }

    static boolean setEnabled(Context c,boolean enabled) {
        if(c==null)return false;
        // Consent changes and pending store writes share one lock.
        // Once disabling returns, no queued storage writer can commit
        // with the previous enabled state.
        synchronized(TypedWordEvidenceStore341.class) {
            return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY,enabled).commit();
        }
    }
}
