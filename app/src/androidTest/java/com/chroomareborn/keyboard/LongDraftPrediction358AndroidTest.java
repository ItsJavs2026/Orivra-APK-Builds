package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 358 Android 35 emulator tests; pure synthetic text.
 * These do not claim phone approval or verify on-screen touch accuracy.
 */
@RunWith(AndroidJUnit4.class)
public final class LongDraftPrediction358AndroidTest {
    private static String paragraph(String last) {
        StringBuilder s=new StringBuilder();
        for(int i=0;i<18;i++)
            s.append("The keyboard should work normally while I type. ");
        return s.append(last).toString();
    }
    private static final class MockEditor implements SentenceSuggestionCommit.Editor {
        String before="screenshot",after="";
        int batchStarts,commits,ends;
        String inserted;
        @Override public CharSequence before(int length) {
            return before.substring(Math.max(0,before.length()-length));
        }
        @Override public CharSequence after(int length) {return after;}
        @Override public void beginBatch(){batchStarts++;}
        @Override public boolean commit(String value) {
            commits++;inserted=value;return true;
        }
        @Override public void endBatch(){ends++;}
    }
    @Test public void screenshotGetsTwoCompletionPreviewsWithoutAutoCommit() {
        assertTrue(PredictionLite.publicWordForContext("screenshot"));
        assertEquals(List.of("screenshot","screenshot of","screenshot and"),
                LongDraftPrediction358.whileComposing(
                        "screenshot",List.of("screenshot"),true,true));
    }
    @Test public void explicitPhraseTapReplacesComposingOnceWithOneSpace() {
        MockEditor editor=new MockEditor();
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(
                        editor,"screenshot","screenshot of"));
        assertEquals("screenshot of ",editor.inserted);
        assertEquals(1,editor.commits);
        assertEquals(1,editor.batchStarts);
        assertEquals(1,editor.ends);
    }
    @Test public void nextWordPredictionAtLongDocumentEndStillHasSafeTicket() {
        String text=paragraph("Especially what I screenshot ");
        // The actual editor boundary is bounded to the same final 160 chars
        // that NextWordTapTicket340 stores. No unbounded document capture.
        String recent=text.substring(Math.max(0,text.length()-160));
        assertEquals(List.of("of","and"),
                LongDraftPrediction358.afterCommittedBoundary(
                        recent,List.of(),true));
        NextWordTapTicket340 ticket=NextWordTapTicket340.capture(
                recent,"",12,600,600,false,false,false);
        assertNotNull(ticket);
        assertTrue(ticket.current(recent,"",12,600,600,false,false));
        assertFalse(ticket.current(recent+"a","",12,601,601,false,false));
        assertFalse(ticket.current(recent,"",13,600,600,false,false));
    }
    @Test public void selectionAndPrivateEditorsNeverReceiveFallback() {
        assertEquals(List.of(),LongDraftPrediction358.whileComposing(
                "screenshot",List.of(),false,true));
        assertEquals(List.of(),LongDraftPrediction358.afterCommittedBoundary(
                paragraph("screenshot "),List.of(),false));
        assertEquals(List.of(),LongDraftPrediction358.afterCommittedBoundary(
                paragraph("screenshot"),List.of(),true));
    }
    @Test public void higherConfidenceModelCandidatesKeepTheirRank() {
        assertEquals(List.of("there","after","tomorrow"),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph("screenshot "),
                        List.of("there","after","tomorrow"),true));
        assertEquals(List.of("screenshot","screenshot from"),
                LongDraftPrediction358.whileComposing("screenshot",
                        List.of("screenshot","screenshot from"),true,true));
    }
}
