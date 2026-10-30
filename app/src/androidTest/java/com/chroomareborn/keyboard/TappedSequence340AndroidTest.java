package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Session-only prediction learning from independently completed typed words.
 * No editor text or unknown words are written to the V3 backup.
 */
@RunWith(AndroidJUnit4.class)
public final class TappedSequence340AndroidTest {
    private static void sentence(PersonalNextWordModel model) {
        for(String word:List.of("i","like","orivra","today"))
            model.observeCommittedWord(word,true);
        model.resetContext();
    }
    @Test public void unfamiliarWordRequiresRepeatedTapSequence(){
        PersonalNextWordModel model=new PersonalNextWordModel();
        sentence(model);
        assertFalse(model.suggest("i like",3).contains("orivra"));
        sentence(model);
        assertTrue(model.suggest("i like",3).contains("orivra"));
        assertTrue(model.suggest("like orivra",3).contains("today"));
        model.clear();
        assertTrue(model.suggest("i like",3).isEmpty());
    }
    @Test public void malformedOrPrivateWordBreaksTransientContext(){
        PersonalNextWordModel model=new PersonalNextWordModel();
        model.observeCommittedWord("i",true);
        model.observeCommittedWord("alice@example.com",true);
        model.observeCommittedWord("works",true);
        model.observeCommittedWord("i",true);
        model.observeCommittedWord("works",false);
        assertTrue(model.suggest("i",3).isEmpty());
    }
    @Test public void freshEditorResetsSequenceBeforeLearning(){
        PersonalNextWordModel model=new PersonalNextWordModel();
        model.observeCommittedWord("previous",true);
        model.resetContext();
        model.observeCommittedWord("orivra",true);
        model.observeCommittedWord("rocks",true);
        assertTrue(model.suggest("previous orivra",3).isEmpty());
    }
}
