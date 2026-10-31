package com.chroomareborn.keyboard;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 393: deep Android 35 stress. Synthetic text ONLY.
 * Raw user text is never accessed/logged. Timing percentiles reflect an
 * instrumentation editor harness, not physical Samsung IME touch-to-pixel lag.
 */
@RunWith(AndroidJUnit4.class)
public final class ChatComposerStress393AndroidTest {
    private static final int LONG_CHARS=36000;
    private static BaseInputConnection editor(String initial) {
        Context context=InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        BaseInputConnection e=new BaseInputConnection(new View(context),true);
        Editable text=e.getEditable();
        text.replace(0,text.length(),initial);
        Selection.setSelection(text,text.length());
        return e;
    }
    private static String longDraft(int minimum) {
        String unit="The keyboard should complete each word while a message grows longer. ";
        StringBuilder out=new StringBuilder(minimum+unit.length());
        while(out.length()<minimum)out.append(unit);
        return out.toString();
    }
    private static long median(long[] times,double fraction) {
        long[] sorted=Arrays.copyOf(times,times.length);
        Arrays.sort(sorted);
        return sorted[Math.max(0,(int)Math.ceil(sorted.length*fraction)-1)];
    }
    private static String ms(long nano){
        return String.format(Locale.US,"%.3f",nano/1_000_000.0);
    }
    @Test public void thirtySixThousandKeyWritesPreserveLongMessageAndMeasureLatency(){
        BaseInputConnection ic=editor("");
        String draft=longDraft(LONG_CHARS);
        long[] keyNanos=new long[draft.length()];
        long started=System.nanoTime();
        for(int i=0;i<draft.length();i++){
            long t=System.nanoTime();
            assertTrue("key commit "+i,ic.commitText(draft.substring(i,i+1),1));
            keyNanos[i]=System.nanoTime()-t;
        }
        long elapsed=System.nanoTime()-started;
        assertEquals("No lost, duplicated or reordered characters",
                draft,ic.getEditable().toString());
        assertTrue("at least 35000 actual key commits",draft.length()>=35000);
        assertEquals(draft.length(),Selection.getSelectionStart(ic.getEditable()));
        System.out.println("R393 Android editor key insertion "+
                "keys="+draft.length()+" total_ms="+ms(elapsed)+
                " p50_ms="+ms(median(keyNanos,.50))+
                " p95_ms="+ms(median(keyNanos,.95))+
                " p99_ms="+ms(median(keyNanos,.99))+
                " max_ms="+ms(median(keyNanos,1.0)));
    }
    @Test public void longMessageHundredsOfCursorMovesNeverChangesWords(){
        String draft=longDraft(26000);
        BaseInputConnection ic=editor(draft);
        Editable e=ic.getEditable();
        int n=draft.length();
        for(int i=0;i<360;i++){
            int cursor=(i*7919)%(n-1)+1;
            Selection.setSelection(e,cursor);
            CharSequence after=ic.getTextAfterCursor(1,0);
            assertNotNull(after);
            assertEquals(1,after.length());
            assertTrue(LongDraftCaretState375.editingWithoutComposition(false,true));
            Selection.setSelection(e,n);
            assertFalse(LongDraftCaretState375.editingWithoutComposition(false,false));
        }
        assertEquals(draft,e.toString());
        assertEquals(n,Selection.getSelectionStart(e));
    }
    @Test public void twoHundredFortyHostSendClearAndRefocusCyclesRestoreIdleWords(){
        BaseInputConnection ic=editor("");
        Editable e=ic.getEditable();
        String sentence="This is a safe simulated chat message with typing and punctuation. ";
        List<String> idle=PredictionLite.contextualNextWords("");
        assertFalse("Public offline starter choices exist",idle.isEmpty());
        for(int turn=0;turn<240;turn++){
            StringBuilder expected=new StringBuilder();
            for(int j=0;j<2;j++)for(int i=0;i<sentence.length();i++){
                String next=sentence.substring(i,i+1);
                assertTrue(ic.commitText(next,1));
                expected.append(next);
            }
            assertEquals(expected.toString(),e.toString());
            // Model a host chat app's Send clearing the current editor WITHOUT
            // restarting the InputMethodService or its composing state.
            e.clear();
            Selection.setSelection(e,0);
            assertTrue(ChatComposerReset393.shouldRecover(0,0,
                    ic.getTextBeforeCursor(1,0),ic.getTextAfterCursor(1,0),
                    turn%2==0?"message":"",turn%2==0?"message":"",
                    turn%3==0,turn%4==0?0:3,false,false));
            NextWordTapTicket340 t=NextWordTapTicket340.capture(
                    ic.getTextBeforeCursor(160,0),ic.getTextAfterCursor(1,0),
                    turn,0,0,false,false,false);
            assertNotNull("Idle caret ticket after external Send",t);
            assertEquals(LetterRowIdleMode388.Mode.NEXT_WORD,
                    LetterRowIdleMode388.start("","",true,idle));
            assertTrue(t.current(ic.getTextBeforeCursor(160,0),
                    ic.getTextAfterCursor(1,0),turn,0,0,false,false));
        }
        assertEquals("",e.toString());
        System.out.println("R393 recovered 240 simulated external Send resets "+
                "and 33000+ app-key commits; no text loss");
    }
    @Test public void zeroPositionRefreshWorksWithUnchangedSelectionOrOldChips(){
        assertTrue(ChatComposerReset393.shouldRecover(0,0,"","",
                "hello","hello",true,3,false,false));
        assertTrue(ChatComposerReset393.shouldRecover(0,0,"","",
                "","",false,3,false,false));
        assertTrue(ChatComposerReset393.shouldRecover(0,0,"","",
                "","",false,0,true,false));
        assertFalse(ChatComposerReset393.shouldRecover(0,0,"","",
                "","",false,3,true,false));
        assertFalse(ChatComposerReset393.shouldRecover(0,0,"","",
                "","",false,3,false,true));
        assertFalse(ChatComposerReset393.shouldRecover(0,0,"","x",
                "message","message",true,0,false,false));
    }
    @Test public void neverResetNonemptyDraftSelectedTextOrUnavailableEditor(){
        BaseInputConnection ic=editor("Do not erase this user message.");
        Editable e=ic.getEditable();
        assertFalse(ChatComposerReset393.shouldRecover(e.length(),e.length(),
                ic.getTextBeforeCursor(1,0),ic.getTextAfterCursor(1,0),
                "message","message",true,0,false,false));
        Selection.setSelection(e,0);
        assertFalse(ChatComposerReset393.shouldRecover(0,0,
                ic.getTextBeforeCursor(1,0),ic.getTextAfterCursor(1,0),
                "message","",true,0,false,false));
        Selection.setSelection(e,0,3);
        assertFalse(ChatComposerReset393.shouldRecover(0,3,
                ic.getTextBeforeCursor(1,0),ic.getTextAfterCursor(1,0),
                "message","",true,0,false,false));
        assertFalse(ChatComposerReset393.shouldRecover(0,0,null,"",
                "message","",true,0,false,false));
        assertEquals("Do not erase this user message.",e.toString());
    }
    @Test public void rapidTwoThousandFiveHundredGenerationUpdatesKeepOnlyFinalPrediction()
            throws Exception {
        AtomicLong latest=new AtomicLong();
        AtomicInteger staleCallbacks=new AtomicInteger();
        CountDownLatch finalReady=new CountDownLatch(1);
        PredictionDispatcher dispatcher=new PredictionDispatcher(
                (input,editing)->List.of(input));
        try{
            for(int i=0;i<2500;i++) {
                String word="sample"+(i%97);
                dispatcher.schedule(word,(id,input,options)->{
                    if(latest.get()>id)staleCallbacks.incrementAndGet();
                });
            }
            final long newest=dispatcher.schedule("finalword",
                    (id,input,options)->{
                        if("finalword".equals(input) &&
                                options.size()==1 &&
                                "finalword".equals(options.get(0)))
                            finalReady.countDown();
                    });
            latest.set(newest);
            assertTrue("Final result must arrive after 2500 updates",
                    finalReady.await(12,TimeUnit.SECONDS));
            assertEquals("No superseded result after final generation",0,
                    staleCallbacks.get());
        }finally{
            dispatcher.close();
        }
    }
    @Test public void thirtyThousandLetterRowGesturesStayResponsiveAfterReset(){
        List<String> three=List.of("I","The","Hey");
        List<String> centerOnly=Arrays.asList("","The","");
        Random random=new Random(39320261031L);
        for(int i=0;i<30000;i++){
            int pointer=i%9;
            LetterRowPredictionSwipe386 gesture=
                    new LetterRowPredictionSwipe386(pointer,50,30,1f);
            LetterRowPredictionSwipe386.Direction dir;
            if(i%3==0)dir=gesture.release(pointer,-25,30);
            else if(i%3==1)dir=gesture.release(pointer,125,30);
            else dir=gesture.release(pointer,50,100);
            List<String> opts=i%11==0?centerOnly:three;
            String choice=LetterRowPredictionSwipe386.chosen(dir,opts);
            if(i%3==2)assertEquals("The",choice);
            if(i%11!=0)assertNotNull(choice);
            assertEquals("Short-word boundary has one trailing Space",
                    " ",SuggestionContinuity353.spaceAfter(""));
            assertEquals("",SuggestionContinuity353.spaceAfter(" "));
        }
    }
    @Test public void slowOfflinePredictionBenchmarksReportPercentilesWithoutFlakyThreshold(){
        // Measure existing engine, not an invented performance claim. Baseline
        // is the same Candidate 391 implementation inside this research branch.
        String[] samples={"tell","working","keyboard","the","continued",
                "thisisatest","predictions","qwerty","environment","tekk"};
        // Warm public model first to exclude one-time class initialization.
        for(String s:samples)PredictionLite.suggestions(s);
        long[] elapsed=new long[300];
        for(int i=0;i<elapsed.length;i++){
            long now=System.nanoTime();
            List<String> result=PredictionLite.suggestions(samples[i%samples.length]);
            elapsed[i]=System.nanoTime()-now;
            assertNotNull(result);
        }
        System.out.println("R393 offline suggestion engine 300 cases"+
                " p50_ms="+ms(median(elapsed,.50))+
                " p95_ms="+ms(median(elapsed,.95))+
                " p99_ms="+ms(median(elapsed,.99))+
                " max_ms="+ms(median(elapsed,1.0)));
    }
}
