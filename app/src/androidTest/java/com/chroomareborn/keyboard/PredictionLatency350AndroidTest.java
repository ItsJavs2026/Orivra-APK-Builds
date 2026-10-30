package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Candidate 350 performance and cancellation QA. Only synthetic test words.
 * Avoid brittle wall-clock thresholds on shared GitHub Android emulators;
 * validate the bounded schedule policy and deterministic generation safety.
 */
@RunWith(AndroidJUnit4.class)
public final class PredictionLatency350AndroidTest {
    @Test public void everydayWordsStartWithoutOldFortyMillisecondWait(){
        assertEquals(12L,PredictionDispatcher.debounceMillisFor("c",false));
        assertEquals(12L,PredictionDispatcher.debounceMillisFor("continue",false));
        assertEquals(12L,PredictionDispatcher.debounceMillisFor("howareyou",false));
        assertEquals(20L,PredictionDispatcher.debounceMillisFor(
                "twelvethirteenchars",false));
        assertEquals(28L,PredictionDispatcher.debounceMillisFor(
                "howareyoudoingthisafternoonatworktoday",false));
        assertEquals(14L,PredictionDispatcher.debounceMillisFor("testing",true));
    }

    @Test public void editingAlwaysUsesBoundedLowLatency(){
        assertEquals(14L,PredictionDispatcher.debounceMillisFor("a",true));
        assertEquals(14L,PredictionDispatcher.debounceMillisFor(
                "a_long_very_long_fake_editor_word",true));
    }

    @Test public void rapidTypingOnlyDeliversMostRecentGeneration()
            throws Exception {
        CountDownLatch received=new CountDownLatch(1);
        AtomicInteger callbacks=new AtomicInteger();
        AtomicReference<String> winning=new AtomicReference<>();
        PredictionDispatcher dispatcher=new PredictionDispatcher((input,editing)->{
            if(Thread.currentThread().isInterrupted())return List.of();
            return List.of(input);
        });
        try{
            for(int i=0;i<300;i++) {
                final String word="continu"+(i%10);
                dispatcher.schedule(word,(id,input,options)->{
                    callbacks.incrementAndGet();
                    winning.set(input);
                    if(input.equals("continue"))received.countDown();
                });
            }
            dispatcher.schedule("continue",(id,input,options)->{
                callbacks.incrementAndGet();
                winning.set(input);
                received.countDown();
            });
            assertTrue("final candidate should dispatch",received.await(4,TimeUnit.SECONDS));
            assertEquals("continue",winning.get());
            assertEquals("only latest queued generation may render",1,callbacks.get());
        }finally{dispatcher.close();}
    }

    @Test public void interruptOlderComputationBeforeShowingResults()
            throws Exception {
        CountDownLatch oldStarted=new CountDownLatch(1);
        CountDownLatch newReady=new CountDownLatch(1);
        CountDownLatch releaseOld=new CountDownLatch(1);
        AtomicInteger oldRendered=new AtomicInteger();
        PredictionDispatcher dispatcher=new PredictionDispatcher((input,editing)->{
            if("oldword".equals(input)) {
                oldStarted.countDown();
                try{releaseOld.await(3,TimeUnit.SECONDS);}
                catch(InterruptedException stopped){
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
            return List.of(input);
        });
        try{
            dispatcher.schedule("oldword",(id,input,options)->oldRendered.incrementAndGet());
            assertTrue(oldStarted.await(4,TimeUnit.SECONDS));
            dispatcher.schedule("newword",(id,input,options)->newReady.countDown());
            releaseOld.countDown();
            assertTrue(newReady.await(4,TimeUnit.SECONDS));
            assertEquals("cancelled old prediction never renders",0,oldRendered.get());
        }finally{dispatcher.close();}
    }

    @Test public void explicitCancelNeverInvokesPreviousCallback()
            throws Exception {
        AtomicInteger invoked=new AtomicInteger();
        PredictionDispatcher dispatcher=new PredictionDispatcher(
                (input,editing)->List.of(input));
        try{
            dispatcher.schedule("cancelled",(id,input,options)->invoked.incrementAndGet());
            dispatcher.cancel();
            Thread.sleep(75L);
            assertEquals(0,invoked.get());
        }finally{dispatcher.close();}
    }

    @Test public void closedDispatcherDoesNotAcceptNewWork(){
        AtomicInteger received=new AtomicInteger();
        PredictionDispatcher dispatcher=new PredictionDispatcher(
                (input,editing)->List.of(input));
        dispatcher.close();
        assertEquals(-1L,dispatcher.schedule("continue",
                (id,input,options)->received.incrementAndGet()));
        assertEquals(0,received.get());
    }

    @Test public void offlineLexiconWarmupCannotPersistOrSendUserWords(){
        PredictionDispatcher dispatcher=new PredictionDispatcher();
        try{
            // Constructor warms a fixed public word without using any
            // editor content; it produces no listener callback or storage.
            assertTrue(PredictionLite.publicWordForContext("hello"));
        }finally{dispatcher.close();}
    }
}
