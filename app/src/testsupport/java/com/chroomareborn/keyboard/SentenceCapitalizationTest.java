package com.chroomareborn.keyboard;

public final class SentenceCapitalizationTest {
    private static void check(String before, boolean expected) {
        boolean actual = SentenceCapitalization.shouldCapitalize(before);
        if (actual != expected)
            throw new AssertionError("before=[" + before + "] expected=" + expected + " actual=" + actual);
    }
    public static void main(String[] args) {
        check("", true);
        check(" ", true);
        check("Hello ", false);
        check("Hello. ", true);
        check("Hello! ", true);
        check("Hello?  ", true);
        check("Hello, ", false);
        check("Hello; ", false);
        check("Hello.", false);
        check("Hello world ", false);
        check("Hi! Next ", false);
        // Moving the caret inside a previous word must disable stale Shift.
        check("I need to cor", false);
        check("I need to ", false);
        check("I need to corect", false);
        check("I need to corect this ", false);
        check("I need to corect this.", false);
        check("I need to corect this. ", true);
        check("I need to corect this. Next", false);
        check("\n", true);
        check("Hello.\n", true);
        check("Hello\n", true);
        check("Hello\n  ", true);
        check("Hello\r\n", true);
        if (SentenceCapitalization.shouldCapitalize(null))
            throw new AssertionError("Unknown editor context must not force caps");
        System.out.println("Sentence capitalization boundary checks passed: 24");
    }
}
