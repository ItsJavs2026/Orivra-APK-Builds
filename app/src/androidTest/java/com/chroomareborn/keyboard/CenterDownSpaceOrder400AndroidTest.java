package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Research400: guarded chip-DOWN and physical Space/letter touch ordering. */
@RunWith(AndroidJUnit4.class)
public final class CenterDownSpaceOrder400AndroidTest {
    @Test public void physicalCenterChipDownAcceptsOnlyItsOwnDisplayedCandidate(){
        assertEquals(PredictionSwipe372.Direction.DOWN,
                PredictionSwipe372.direction(0f,36f,12f,false));
        assertEquals("those you",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.DOWN,1,
                List.of("thoseyou","those you","these you"),
                List.of("other suggestion"),false));
        assertNull(PredictionSwipe372.choose(
                PredictionSwipe372.Direction.DOWN,0,
                List.of("thoseyou","those you","these you"),List.of(),false));
        assertNull(PredictionSwipe372.choose(
                PredictionSwipe372.Direction.DOWN,1,
                List.of("thoseyou","","these you"),List.of(),false));
        assertEquals("there",PredictionSwipe372.choose(
                PredictionSwipe372.Direction.DOWN,0,
                List.of("there"),List.of(),false));
    }
    @Test public void smallTapAndDiagonalNeverEnterDown(){
        for(int y=0;y<32;y++)
            assertNotEquals(PredictionSwipe372.Direction.DOWN,
                    PredictionSwipe372.direction(0f,y,12f,false));
        for(float x:new float[]{40f,-40f}){
            assertNotEquals(PredictionSwipe372.Direction.DOWN,
                    PredictionSwipe372.direction(x,36f,12f,false));
        }
        assertEquals(PredictionSwipe372.Direction.LEFT,
                PredictionSwipe372.direction(-50f,0f,12f,false));
        assertEquals(PredictionSwipe372.Direction.UP,
                PredictionSwipe372.direction(0f,-48f,12f,true));
        assertNull(PredictionSwipe372.choose(PredictionSwipe372.Direction.DOWN,
                0,List.of("left","center","right"),List.of("rise"),true));
    }
    @Test public void earlyAsdfDownOnlySuppressesHoldNotCommit(){
        LetterRowPredictionSwipe386 swipe=
                new LetterRowPredictionSwipe386(4,20f,20f,1f);
        assertFalse(swipe.earlyDownIntent(4,20f,30f));
        assertTrue(swipe.earlyDownIntent(4,20f,33f));
        assertEquals(LetterRowPredictionSwipe386.Direction.NONE,swipe.preview());
        assertEquals(LetterRowPredictionSwipe386.Direction.DOWN,
                swipe.release(4,20f,60f));
        LetterRowPredictionSwipe386 diagonal=
                new LetterRowPredictionSwipe386(2,10f,10f,1f);
        assertFalse(diagonal.earlyDownIntent(2,35f,22f));
        assertFalse(diagonal.earlyDownIntent(3,10f,30f));
        diagonal.cancel();
        assertFalse(diagonal.earlyDownIntent(2,10f,33f));
    }
    @Test public void spaceBetweenOverlappingLettersKeepsFingerDownOrder(){
        HintKeyPressOrder353 queue=new HintKeyPressOrder353();
        ArrayDeque<Runnable> main=new ArrayDeque<>();
        StringBuilder text=new StringBuilder("thos");
        long e=queue.begin(),space=queue.begin(),y=queue.begin();
        // The later 'y' finishes before the pending 'e' or Space.
        queue.complete(y,()->main.add(()->text.append("y")));
        queue.complete(space,()->main.add(()->text.append(" ")));
        queue.complete(e,()->main.add(()->text.append("e")));
        while(!main.isEmpty())main.removeFirst().run();
        assertEquals("those y",text.toString());
        assertFalse(queue.hasOpenPresses());
    }
    @Test public void canceledSpaceAndStaleSelectionNeverWrite(){
        HintKeyPressOrder353 queue=new HintKeyPressOrder353();
        StringBuilder text=new StringBuilder("those");
        long space=queue.begin(),letter=queue.begin();
        queue.complete(letter,()->text.append("y"));
        queue.cancel(space);
        assertEquals("thosey",text.toString());
        assertFalse(queue.hasOpenPresses());
        assertFalse(PredictionSwipe372.safeRelease("thoseyou","thoseyou",
                true,true,false,false,true,false,false,false,true));
        assertFalse(PredictionSwipe372.safeRelease("thoseyou","other",
                true,true,true,false,true,false,false,false,true));
    }
}
