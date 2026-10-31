package com.chroomareborn.keyboard;

/**
 * 415: Skip an optional 1-character punctuation/cursor-boundary sample when
 * extending an already active, normal alphabetic composing word.
 *
 * The actual composing mutation still checks the live editor precondition
 * with ComposingMutation340. No assumption about remote editor success.
 */
final class FastKeySample415 {
    private FastKeySample415(){}
    static boolean skipInitialBeforeRead(
            String composing, String incoming, boolean editingInMiddle,
            boolean selected, boolean sensitive, boolean numeric,
            boolean symbols, boolean suggestionsAllowed) {
        if(composing==null || composing.isEmpty() || incoming==null ||
                incoming.length()!=1 || editingInMiddle || selected ||
                sensitive || numeric || symbols || !suggestionsAllowed)
            return false;
        char c=incoming.charAt(0);
        return (c>='a' && c<='z') || (c>='A' && c<='Z');
    }
}
