package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research 353: deterministic, offline prediction-chip and boundary tests.
 * Uses synthetic text only; never requires a device keyboard setting.
 */
@RunWith(AndroidJUnit4.class)
public final class SuggestionContinuity353AndroidTest {
    @Test public void emptyDocumentSuffixAddsExactlyOneSpace(){
        assertEquals(" ",SuggestionContinuity353.spaceAfter(""));
    }
    @Test public void existingSpaceAndNewlinePreventDoubleSpacing(){
        assertEquals("",SuggestionContinuity353.spaceAfter(" going"));
        assertEquals("",SuggestionContinuity353.spaceAfter("\nnext"));
        assertEquals("",SuggestionContinuity353.spaceAfter("\t"));
    }
    @Test public void closingPunctuationStaysAttachedToAcceptedWord(){
        for(String symbol:new String[]{".",",","!","?",";",":",")","]","}","”","’"})
            assertEquals(symbol,"",SuggestionContinuity353.spaceAfter(symbol+" next"));
    }
    @Test public void adjacentLettersAndDigitsGainOneWordBoundary(){
        assertEquals(" ",SuggestionContinuity353.spaceAfter("going"));
        assertEquals(" ",SuggestionContinuity353.spaceAfter("123"));
    }
    @Test public void unavailableEditorSuffixDoesNotGuessTwoSpaces(){
        assertEquals("",SuggestionContinuity353.spaceAfter(null));
    }
    @Test public void cursorWordIsAlwaysTheMiddleChip(){
        assertEquals(List.of("what's","how's","where's"),
                SuggestionContinuity353.centerCurrentWord("how's",
                        List.of("how's","what's","where's")));
        assertEquals(List.of("goal","going","doing"),
                SuggestionContinuity353.centerCurrentWord("going",
                        List.of("going","goal","doing")));
    }
    @Test public void missingAlternativesRetainPermanentInvisibleSideSlots(){
        assertEquals(List.of("","it",""),
                SuggestionContinuity353.centerCurrentWord("it",List.of("it")));
        assertEquals(List.of("other","it",""),
                SuggestionContinuity353.centerCurrentWord("it",List.of("other")));
    }
    @Test public void cursorWordsCannotBeDuplicatedByAlternativeCasing(){
        assertEquals(List.of("ALTERNATIVE","Hello",""),
                SuggestionContinuity353.centerCurrentWord("Hello",
                        List.of("hello","ALTERNATIVE","alternative")));
    }
    @Test public void multipleWordsAreNotPresentedAsCursorWordReplacements(){
        assertEquals(List.of("going","goign","goin"),
                SuggestionContinuity353.centerCurrentWord("goign",
                        List.of("how's it going","going","goin")));
    }
    @Test public void oldEndOfLongDocumentStateResumesPredictions(){
        assertTrue(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,true,false,false,""));
        assertTrue(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,true,false,false,null));
    }
    @Test public void realMidWordEditingAndSelectionStayProtected(){
        assertFalse(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,true,false,false,"going"));
        assertFalse(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,true,false,true,""));
        assertFalse(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,true,true,false,""));
        assertFalse(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                true,false,false,false,""));
    }
    @Test public void noStaleEditingStateRequiresNoNewReconciliation(){
        assertFalse(SuggestionContinuity353.resumeNextWordsAtDocumentEnd(
                false,true,false,false,""));
    }
    @Test public void cursorCorrectionAtEndAddsSpaceWhenOriginalTapped(){
        CursorWordCorrectionPolicy.Plan p=CursorWordCorrectionPolicy.plan(
                "how's it go","ing","going","going",false,false);
        assertNotNull(p);
        assertEquals(2,p.lettersBefore);
        assertEquals(3,p.lettersAfter);
        assertEquals(" ",p.trailingSeparator);
    }
    @Test public void cursorCorrectionInSentenceDoesNotDoubleSpace(){
        CursorWordCorrectionPolicy.Plan p=CursorWordCorrectionPolicy.plan(
                "how's it g","oing today","going","good",false,false);
        assertNotNull(p);
        assertEquals("",p.trailingSeparator);
        assertEquals("good",p.replacement);
    }
    @Test public void cursorCorrectionDoesNotGuessUnknownJoinedWordBoundary(){
        // Without whitespace, "goingtoday" may be a single novel word.
        // Never delete its trailing letters to force a suggested separator.
        CursorWordCorrectionPolicy.Plan p=CursorWordCorrectionPolicy.plan(
                "how's it g","oingtoday","going","going",false,false);
        assertNull(p);
    }
    @Test public void punctuationAfterCorrectionDoesNotGetExtraSpace(){
        CursorWordCorrectionPolicy.Plan p=CursorWordCorrectionPolicy.plan(
                "how's it go","ing!","going","going",false,false);
        assertNotNull(p);
        assertEquals("",p.trailingSeparator);
    }
    private static final class FakeEditor implements SentenceSuggestionCommit.Editor {
        String before="howareyou",after="";
        int commits;String committed;
        @Override public CharSequence before(int count){return before;}
        @Override public CharSequence after(int count){return after;}
        @Override public void beginBatch(){}
        @Override public boolean commit(String value){commits++;committed=value;return true;}
        @Override public void endBatch(){}
    }
    @Test public void sentenceChipAddsSingleSpaceAtDocumentEnd(){
        FakeEditor f=new FakeEditor();
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(f,"howareyou","how are you"));
        assertEquals("how are you ",f.committed);
        assertEquals(1,f.commits);
    }
    @Test public void sentenceChipDoesNotDoubleExistingSeparator(){
        FakeEditor f=new FakeEditor();f.after=" ";
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(f,"howareyou","how are you"));
        assertEquals("how are you",f.committed);
    }
    @Test public void sentenceChipDoesNotAttachToNextWord(){
        FakeEditor f=new FakeEditor();f.after="going";
        assertEquals(SentenceSuggestionCommit.Result.APPLIED,
                SentenceSuggestionCommit.insert(f,"howareyou","how are you"));
        assertEquals("how are you ",f.committed);
    }
}
