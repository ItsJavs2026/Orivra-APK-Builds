package com.chroomareborn.keyboard;

import java.util.List;

/**
 * Research 355: deterministic offline boundary regression, not a phone
 * usability test and not a public candidate APK.
 * No raw user typing, network, storage, or Android dependencies.
 */
public final class SuggestionBoundary355Research {
    private static int checks;
    private static void expect(String message, String expected, String actual) {
        checks++;
        if (!expected.equals(actual))
            throw new AssertionError(message + " expected=[" + expected
                    + "] actual=[" + actual + "]");
    }
    private static void truth(String message, boolean actual) {
        checks++;
        if (!actual) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String[] existingBoundaries = {
            " next", "\tmore", "\nnext",
            "\u00a0following", "\u202ffollowing", "\u2003following",
            ".", ",", ";", ":", "!", "?", ")", "]", "}",
            "\u2019", "\u201d", "\u00bb", "'", "\"", "\u203a"
        };
        for (String suffix : existingBoundaries)
            expect("Do not create a space before existing separator/closer",
                    "", SuggestionContinuity353.spaceAfter(suffix));
        String[] needsBoundaries = {
            "", "next", "another", "123", "(next", "_next", "-next"
        };
        for (String suffix : needsBoundaries)
            expect("Insert exactly one boundary before adjacent content",
                    " ", SuggestionContinuity353.spaceAfter(suffix));
        expect("Unavailable editor suffix is not guessed", "",
                SuggestionContinuity353.spaceAfter(null));
        truth("Current cursor word remains physically centered",
                List.of("walk", "went", "want").equals(
                    SuggestionContinuity353.centerCurrentWord(
                        "went", List.of("went", "walk", "want"))));
        truth("No false next-word recovery while actual word remains",
                !SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                    true, true, false, false, "word"));
        truth("Allow safe stale-state recovery at document end",
                SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                    true, true, false, false, ""));
        // A reproducible 20,000-suffix stability sweep. This is not an
        // estimate of real-world prediction accuracy or Samsung latency.
        for (int i = 0; i < 20_000; i++) {
            String suffix = i % 3 == 0 ? "\u00a0more"
                    : i % 3 == 1 ? "\""
                    : "following";
            String expected = i % 3 == 2 ? " " : "";
            expect("Generated boundary sample #" + i, expected,
                    SuggestionContinuity353.spaceAfter(suffix));
        }
        System.out.println("PASS Research 355: " + checks +
                " deterministic offline boundary and cursor-policy assertions");
    }
}
