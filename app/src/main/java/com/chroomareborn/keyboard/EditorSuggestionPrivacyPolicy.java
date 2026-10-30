package com.chroomareborn.keyboard;

/**
 * Pure-Java editor prediction/privacy policy. The Android adapter supplies
 * the platform InputType / EditorInfo flags; no app text is inspected here.
 *
 * Android's TYPE_TEXT_FLAG_NO_SUGGESTIONS describes dictionary UI, NOT
 * credentials or whether InputConnection editing is allowed. Never couple
 * backspace, spacebar cursor movement, or Enter actions to this flag.
 *
 * Actual credential/identifier variations and non-text fields remain private.
 * In ordinary free-form text, including Google search and Google Keep,
 * ephemeral, fully local tap-only suggestions are allowed. When an editor
 * requests NO_SUGGESTIONS, personal learning is always disabled. The
 * IME_FLAG_NO_PERSONALIZED_LEARNING restriction is also always honored.
 */
final class EditorSuggestionPrivacyPolicy {
    private EditorSuggestionPrivacyPolicy() {}
    static final String GOOGLE_KEEP_PACKAGE="com.google.android.keep";

    static final class Decision {
        final boolean sensitive;
        final boolean allowPersonalLearning;
        final boolean keepCompatibility;
        final boolean allowSuggestions;
        Decision(boolean privateField,boolean allowLearning,boolean keep,
                 boolean showSuggestions) {
            sensitive=privateField;
            allowPersonalLearning=allowLearning;
            keepCompatibility=keep;
            allowSuggestions=showSuggestions;
        }
    }

    static Decision decide(String editorPackage,boolean isTextClass,
            boolean isPasswordOrIdentifier,boolean noSuggestions,
            boolean noPersonalizedLearning) {
        return decide(editorPackage,isTextClass,isPasswordOrIdentifier,
                noSuggestions,noPersonalizedLearning,false);
    }

    static Decision decide(String editorPackage,boolean isTextClass,
            boolean isPasswordOrIdentifier,boolean noSuggestions,
            boolean noPersonalizedLearning,boolean searchOrGoAction) {
        return decide(editorPackage,isTextClass,isPasswordOrIdentifier,
                noSuggestions,noPersonalizedLearning,searchOrGoAction,false);
    }

    /** Name, postal address and phonetic-name editors can display suggestions
     * and use editing gestures, but should not feed persistent learning. */
    static Decision decide(String editorPackage,boolean isTextClass,
            boolean isPasswordOrIdentifier,boolean noSuggestions,
            boolean noPersonalizedLearning,boolean searchOrGoAction,
            boolean personalDataVariation) {
        // A password-style variation, URI/email or non-text input always
        // remains private—even inside Google Keep itself.
        if(!isTextClass || isPasswordOrIdentifier)
            return new Decision(true,false,false,false);
        final boolean keep=GOOGLE_KEEP_PACKAGE.equals(editorPackage);
        // This flag is a suggestion preference, not a password signal.
        // Treating it as "sensitive" broke ordinary Google search fields,
        // disabled text editing gestures, and hid the suggestion strip.
        // No app-name allowlist is needed; only trustworthy InputType
        // credential/identifier signals above can mark the editor private.
        final boolean allowLearning=!noSuggestions && !noPersonalizedLearning &&
                !personalDataVariation;
        // Only a declared Search/Go intent or the already-qualified Keep
        // note exception can override the host's NO_SUGGESTIONS UI hint.
        // Other normal text fields honor the hint, but their editing
        // controls remain functional; NO_SUGGESTIONS is not 'sensitive'.
        final boolean allowSuggestions=!noSuggestions || keep || searchOrGoAction;
        return new Decision(false,allowLearning,keep && noSuggestions,
                allowSuggestions);
    }
}
