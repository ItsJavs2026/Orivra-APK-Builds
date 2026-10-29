package com.chroomareborn.keyboard;

/** Local-only comparison of real editor text after a manually performed tap. */
final class SentenceTapOutcome {
    private SentenceTapOutcome() {}
    enum Result { NEED_EXPECTED, NEED_EDITOR_TEXT, EXACT, CASE_DIFFERENCE, MISMATCH }

    static Result compare(String actual, String intended) {
        if (intended == null || intended.trim().isEmpty())
            return Result.NEED_EXPECTED;
        if (actual == null || actual.isEmpty())
            return Result.NEED_EDITOR_TEXT;
        // The IME may append one trailing separator on accepting a suggestion.
        // An extra second separator must remain detectable as a mismatch.
        String observed = actual.endsWith(" ")
                ? actual.substring(0, actual.length()-1) : actual;
        String expected = intended.trim();
        if (observed.equals(expected)) return Result.EXACT;
        if (observed.equalsIgnoreCase(expected)) return Result.CASE_DIFFERENCE;
        return Result.MISMATCH;
    }

    static String description(Result result) {
        switch (result) {
            case EXACT:
                return "PASS — the editor contains the intended sentence.";
            case CASE_DIFFERENCE:
                return "PASS — words and spacing match; capitalization differs.";
            case NEED_EXPECTED:
                return "Enter the intended sentence in the field above first.";
            case NEED_EDITOR_TEXT:
                return "Type using Orivra Candidate and tap a suggestion first.";
            default:
                return "NOT MATCHED — the editor text differs from the intended sentence.";
        }
    }
}
