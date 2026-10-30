package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Candidate 347: accepted evidence must be verified IME-tapped words only. */
@RunWith(AndroidJUnit4.class)
public final class VerifiedSpacePolicy347AndroidTest {
    private static String token(String word,boolean sameConnection,
            boolean sameSession,boolean learning,boolean privateEditor,
            boolean movedOrSelected,boolean trailing,boolean accepted){
        return VerifiedSpacePolicy347.verifiedToken(word,
                sameConnection,sameSession,learning,privateEditor,
                movedOrSelected,trailing,accepted);
    }

    @Test public void normalVerifiedWordsAndShortWordsCanRecord(){
        assertEquals("continue",token("Continue",true,true,true,false,false,false,true));
        assertEquals("i",token("I",true,true,true,false,false,false,true));
        assertEquals("a",token("a",true,true,true,false,false,false,true));
    }
    @Test public void userTypedOnlyNeverFromPastedUnverifiedWord(){
        assertNull(token(null,true,true,true,false,false,false,true));
        assertNull(token("",true,true,true,false,false,false,true));
        assertNull(token("bad@",true,true,true,false,false,false,true));
    }
    @Test public void editorOrSessionSwitchRejectsOldEvidence(){
        assertNull(token("continue",false,true,true,false,false,false,true));
        assertNull(token("continue",true,false,true,false,false,false,true));
    }
    @Test public void privateNumericOrLearningOffNeverRecords(){
        assertNull(token("continue",true,true,false,false,false,false,true));
        assertNull(token("continue",true,true,true,true,false,false,true));
    }
    @Test public void selectionAndCursorEditNeverRecords(){
        assertNull(token("continue",true,true,true,false,true,false,true));
        assertNull(token("continue",true,true,true,false,false,true,true));
    }
    @Test public void rejectedSpaceNeverRecords(){
        assertNull(token("continue",true,true,true,false,false,false,false));
    }
}
