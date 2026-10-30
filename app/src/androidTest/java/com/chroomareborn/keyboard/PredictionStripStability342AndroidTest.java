package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Regression tests for hotfix 342: stable, quiet active-typing prediction strip. */
@RunWith(AndroidJUnit4.class)
public final class PredictionStripStability342AndroidTest {
    @Test public void ordinaryEditorNeverUsesAKeepTypingOrNwOffPlaceholder() {
        assertFalse(PredictionStripStability342.showPrivateStatus(false,true));
        assertTrue(PredictionStripStability342.showPrivateStatus(true,true));
        assertTrue(PredictionStripStability342.showPrivateStatus(false,false));
    }
    @Test public void exactAsyncSingleLetterCursorEchoIsIgnoredOnce() {
        assertTrue(PredictionStripStability342.suppressExpectedCursorEcho(
                20L,14,14,15,15,15,true,true));
        assertTrue(PredictionStripStability342.suppressExpectedCursorEcho(
                180L,3,3,4,4,4,true,true));
    }
    @Test public void realCursorGesturesAndStaleEditsAreNotSuppressed(){
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                181L,14,14,15,15,15,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                -1L,14,14,15,15,15,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,17,17,17,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,15,15,16,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,15,15,15,false,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,15,15,15,true,false));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,15,15,15,15,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,15,16,15,true,true));
        assertFalse(PredictionStripStability342.suppressExpectedCursorEcho(
                50L,14,14,13,13,13,true,true));
    }
}
