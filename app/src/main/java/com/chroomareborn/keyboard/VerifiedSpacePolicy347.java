package com.chroomareborn.keyboard;

/** Candidate 347: privacy/commit gate for only a word the IME itself typed
 * and verified against the live editor BEFORE a successful Space commit.
 * Never derive a token from clipboard, arbitrary editor content, selection
 * edits or a suggestion currently visible in the strip.
 */
final class VerifiedSpacePolicy347 {
    private VerifiedSpacePolicy347(){}

    static String verifiedToken(String alreadyVerifiedTypedWord,
            boolean sameInputConnection,boolean sameEditorSession,
            boolean personalLearningAllowed,boolean privateOrNumeric,
            boolean editingMiddleOrSelection,boolean textAfterCursor,
            boolean spaceCommitted) {
        if(!sameInputConnection || !sameEditorSession ||
                !personalLearningAllowed || privateOrNumeric ||
                editingMiddleOrSelection || textAfterCursor ||
                !spaceCommitted)
            return null;
        return TypedSequenceArchive341.canonical(alreadyVerifiedTypedWord);
    }
}
