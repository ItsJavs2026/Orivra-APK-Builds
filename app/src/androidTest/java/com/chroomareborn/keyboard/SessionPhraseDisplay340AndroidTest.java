package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/** Only explicitly tapped, bounded multiword continuations enter the row. */
@RunWith(AndroidJUnit4.class)
public final class SessionPhraseDisplay340AndroidTest {
    @Test public void fullPhraseHasOnePrimaryChipWithoutLosingSingleWords(){
        List<String> row=SessionPhraseDisplay340.promote(
                List.of("need to continue"),List.of("need","can","need"),3);
        assertEquals(List.of("need to continue","need","can"),row);
    }
    @Test public void rejectsPrivateOrUnboundedPhraseCandidates(){
        assertEquals(List.of("continue"),SessionPhraseDisplay340.promote(
                List.of("alice@example.com next","https://bad.example"),List.of("continue"),3));
        assertEquals(List.of("go"),SessionPhraseDisplay340.promote(
                List.of("one two three four five"),List.of("go"),3));
        assertEquals(List.of(),SessionPhraseDisplay340.promote(
                List.of("one two"),List.of("go"),0));
    }
    @Test public void noPhraseAppearsFromSingleUnrepeatedObservation(){
        PersonalNextWordModel model=new PersonalNextWordModel();
        for(String word:List.of("i","need","to","continue"))
            model.observeCommittedWord(word,true);
        assertTrue(model.suggestPhrase("i ",3).isEmpty());
        model.resetContext();
        for(String word:List.of("i","need","to","continue"))
            model.observeCommittedWord(word,true);
        assertEquals(List.of("need to continue"),
                model.suggestPhrase("i ",3));
    }
}
