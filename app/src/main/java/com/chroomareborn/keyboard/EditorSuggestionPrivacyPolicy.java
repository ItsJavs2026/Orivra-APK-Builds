package com.chroomareborn.keyboard;

/**
 * Pure-Java editor prediction/privacy policy. The Android adapter supplies
 * the platform InputType / EditorInfo flags; no app text is inspected here.
 *
 * TYPE_TEXT_FLAG_NO_SUGGESTIONS is an editor UI hint, not itself a credential
 * marker. Google Keep is an explicitly scoped compatibility exception for
 * normal free-form note fields only. Password, email/URL, non-text and
 * IME-requested no-personalization rules always take precedence.
 */
final class EditorSuggestionPrivacyPolicy {
    private EditorSuggestionPrivacyPolicy() {}
    static final String GOOGLE_KEEP_PACKAGE="com.google.android.keep";

    static final class Decision {
        final boolean sensitive;
        final boolean allowPersonalLearning;
        final boolean keepCompatibility;
        Decision(boolean privateField,boolean allowLearning,boolean keep) {
            sensitive=privateField;
            allowPersonalLearning=allowLearning;
            keepCompatibility=keep;
        }
    }

    static Decision decide(String editorPackage,boolean isTextClass,
            boolean isPasswordOrIdentifier,boolean noSuggestions,
            boolean noPersonalizedLearning) {
        // A password-style variation, URI/email or non-text input always
        // remains private—even inside Google Keep itself.
        if(!isTextClass || isPasswordOrIdentifier)
            return new Decision(true,false,false);
        final boolean keep=GOOGLE_KEEP_PACKAGE.equals(editorPackage);
        if(noSuggestions && !keep)
            return new Decision(true,false,false);

        // If Keep requests no dictionary suggestions, we can still offer
        // ephemeral, offline one-tap candidates without retaining a learning
        // record. Never bypass the IME no-personalized-learning flag.
        final boolean allowLearning=!noSuggestions && !noPersonalizedLearning;
        return new Decision(false,allowLearning,keep && noSuggestions);
    }
}
