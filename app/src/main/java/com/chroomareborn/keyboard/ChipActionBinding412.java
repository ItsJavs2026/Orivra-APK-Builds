package com.chroomareborn.keyboard;

/**
 * An immutable binding signature for a currently visible suggestion chip.
 * Store only in the View's in-process keyed tag; never persist raw typing.
 *
 * Avoids rebuilding click/touch/long-press listeners when both the expected
 * editor input and suggestion action are unchanged.
 */
final class ChipActionBinding412 {
    final String expected, suggestion;
    final long session;
    final boolean longPreview;

    ChipActionBinding412(String expected,String suggestion,
            long session,boolean longPreview){
        this.expected=expected;
        this.suggestion=suggestion;
        this.session=session;
        this.longPreview=longPreview;
    }
    boolean matches(String currentExpected,String currentSuggestion,
            long currentSession,boolean currentLongPreview){
        return session==currentSession &&
                longPreview==currentLongPreview &&
                expected.equals(currentExpected) &&
                suggestion.equals(currentSuggestion);
    }
}
