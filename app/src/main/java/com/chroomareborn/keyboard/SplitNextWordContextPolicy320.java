package com.chroomareborn.keyboard;

/**
 * Fail-closed context extraction for the right-half split prediction row.
 * That row commits a following word with an explicit tap; it must be a
 * genuine approved acceptance in a nonprivate editor, not raw key logging.
 */
final class SplitNextWordContextPolicy320 {
    private SplitNextWordContextPolicy320() {}
    static String safePrevious(String composing,boolean learningEnabled,
            boolean sensitive,boolean editing,boolean selected,
            boolean textAfterCaret) {
        if(!learningEnabled || sensitive || editing || selected ||
                textAfterCaret)return null;
        // The closed-list check rejects names, URLs, incomplete sentences,
        // numbers and arbitrary personal words as prior context.
        return AcceptedContextPolicy319.safeWord(composing);
    }
}
