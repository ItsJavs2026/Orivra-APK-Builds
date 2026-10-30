package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Single-tap verified boundaries include I/a, but not arbitrary fragments. */
@RunWith(AndroidJUnit4.class)
public final class TappedWordBoundary340AndroidTest {
    @Test public void standaloneOneLetterEnglishWordsAreEligible(){
        assertEquals("i",TappedWordBoundary340.verified("I","I"));
        assertEquals("a",TappedWordBoundary340.verified("a","I need a"));
        assertEquals("i",TappedWordBoundary340.verified("i","I really i"));
    }
    @Test public void unfamiliarWholeWordIsEligibleInSession(){
        assertEquals("orivra",TappedWordBoundary340.verified(
                "Orivra","I use Orivra"));
    }
    @Test public void invalidFragmentsAndNonWordDataAreRejected(){
        assertNull(TappedWordBoundary340.verified("x","x"));
        assertNull(TappedWordBoundary340.verified("I","hiI"));
        assertNull(TappedWordBoundary340.verified("work","homework"));
        assertNull(TappedWordBoundary340.verified("alice@example.com",
                "alice@example.com"));
        assertNull(TappedWordBoundary340.verified("orivra","I use elsewhere"));
    }
}
