package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/** Research 355 regression for the next separate candidate qualification. */
@RunWith(AndroidJUnit4.class)
public final class SuggestionBoundary355AndroidTest {
    @Test public void asciiClosingQuotesRemainAttachedToSelectedWord() {
        assertEquals("", SuggestionContinuity353.spaceAfter("'"));
        assertEquals("", SuggestionContinuity353.spaceAfter("\""));
    }
    @Test public void unicodeClosingAngleQuotationRemainsAttached() {
        assertEquals("", SuggestionContinuity353.spaceAfter("\u203a"));
        assertEquals("", SuggestionContinuity353.spaceAfter("\u00bb"));
    }
    @Test public void nonbreakingSpacesAlreadySeparateWords() {
        assertEquals("", SuggestionContinuity353.spaceAfter("\u00a0next"));
        assertEquals("", SuggestionContinuity353.spaceAfter("\u202fnext"));
        assertEquals("", SuggestionContinuity353.spaceAfter("\u2003next"));
    }
    @Test public void ordinaryWordBoundariesStillWork() {
        assertEquals(" ", SuggestionContinuity353.spaceAfter("next"));
        assertEquals(" ", SuggestionContinuity353.spaceAfter(""));
        assertEquals("", SuggestionContinuity353.spaceAfter(" next"));
    }
}
