package com.chroomareborn.keyboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Research 382: pure-Java illustration of async letter vs direct Space
 * editor-mutation ordering. Never calls InputConnection or any Android API.
 * All fictional text and pending callbacks exist only during this test.
 */
public final class DeferredEditorOrder382Research {
    private static long checks=0;
    private static void check(boolean condition,String name){
        checks++;
        if(!condition)throw new AssertionError(name);
    }
    private static final class FakeHandler {
        final ArrayDeque<Runnable> tasks=new ArrayDeque<>();
        void post(Runnable task){tasks.addLast(task);}
        void drain(){
            while(!tasks.isEmpty())tasks.removeFirst().run();
        }
    }
    private static void demonstrateNaiveBarrierMistake(){
        MixedThumbOrder380 barrier=new MixedThumbOrder380();
        FakeHandler handler=new FakeHandler();
        StringBuilder editor=new StringBuilder();
        long letter=barrier.down(),space=barrier.down();
        barrier.up(space,()->editor.append(' ')); // synchronous Space
        barrier.up(letter,()->handler.post(()->editor.append('a'))); // async letter
        handler.drain();
        check(" a".contentEquals(editor),
                "Naive barrier cannot order actual editor mutations");
    }
    private static void queueBothEditorMutations(){
        MixedThumbOrder380 barrier=new MixedThumbOrder380();
        FakeHandler handler=new FakeHandler();
        StringBuilder editor=new StringBuilder();
        long letter=barrier.down(),space=barrier.down();
        barrier.up(space,()->handler.post(()->editor.append(' ')));
        barrier.up(letter,()->handler.post(()->editor.append('a')));
        handler.drain();
        check("a ".contentEquals(editor),
                "Both accepted actions need a common serial editor queue");
        check(!barrier.hasOpenPresses(),"No unresolved touches");
    }
    private static void correctionVerifiedOnlyWhenSpaceExecutes(){
        MixedThumbOrder380 barrier=new MixedThumbOrder380();
        FakeHandler handler=new FakeHandler();
        StringBuilder editor=new StringBuilder("k");
        long letter=barrier.down(),space=barrier.down();
        // Space checks the completed composing word at its actual execution,
        // not at UP while the earlier letter is still pending.
        barrier.up(space,()->handler.post(()->{
            if("ke".contentEquals(editor))editor.replace(0,editor.length(),"keyboard ");
            else editor.append(' ');
        }));
        barrier.up(letter,()->handler.post(()->editor.append('e')));
        check("k".contentEquals(editor),"No early candidate or literal Space");
        handler.drain();
        check("keyboard ".contentEquals(editor),
                "Deferred correction observes preceding confirmed character");
    }
    private static void deadSessionRejectsAlreadyQueuedActions(){
        MixedThumbOrder380 barrier=new MixedThumbOrder380();
        FakeHandler handler=new FakeHandler();
        StringBuilder editor=new StringBuilder();
        final int[] generation={1};
        final int recorded=generation[0];
        long letter=barrier.down(),space=barrier.down();
        barrier.up(space,()->handler.post(()->{
            if(generation[0]==recorded)editor.append(' ');
        }));
        barrier.up(letter,()->handler.post(()->{
            if(generation[0]==recorded)editor.append('a');
        }));
        // A new editor session invalidates already posted work as well as
        // unresolved press tokens.
        generation[0]++;
        barrier.cancelAll();
        handler.drain();
        check(editor.length()==0,"No stale editor mutation after focus switch");
    }
    private static void cursorSwipeNeverSubmitsLiteralSpace(){
        MixedThumbOrder380 barrier=new MixedThumbOrder380();
        FakeHandler handler=new FakeHandler();
        StringBuilder editor=new StringBuilder();
        long letter=barrier.down(),space=barrier.down();
        barrier.cancel(space); // consumed by cursor navigation
        barrier.up(letter,()->handler.post(()->editor.append('a')));
        handler.drain();
        check("a".contentEquals(editor),"Spacebar swipe stays navigation-only");
    }
    private static void randomDelayedReleaseSequences(){
        Random random=new Random(38220261030L);
        for(int n=0;n<20000;n++){
            MixedThumbOrder380 barrier=new MixedThumbOrder380();
            FakeHandler handler=new FakeHandler();
            StringBuilder editor=new StringBuilder();
            StringBuilder expected=new StringBuilder();
            int size=2+random.nextInt(14);
            List<Long> tokens=new ArrayList<>();
            List<Integer> releaseOrder=new ArrayList<>();
            List<Character> chars=new ArrayList<>();
            for(int i=0;i<size;i++){
                char c=(i%4==3)?' ':(char)('a'+random.nextInt(26));
                chars.add(c);
                tokens.add(barrier.down());
                releaseOrder.add(i);
                expected.append(c);
            }
            Collections.shuffle(releaseOrder,random);
            for(int i:releaseOrder){
                final char c=chars.get(i);
                barrier.up(tokens.get(i),()->handler.post(()->editor.append(c)));
            }
            handler.drain();
            check(expected.toString().contentEquals(editor),
                    "Posted actions must follow press order in test "+n);
            check(!barrier.hasOpenPresses(),"No outstanding touches in test "+n);
        }
    }
    public static void main(String[] args){
        demonstrateNaiveBarrierMistake();
        queueBothEditorMutations();
        correctionVerifiedOnlyWhenSpaceExecutes();
        deadSessionRejectsAlreadyQueuedActions();
        cursorSwipeNeverSubmitsLiteralSpace();
        randomDelayedReleaseSequences();
        System.out.println("PASS DeferredEditorOrder382 "+checks+
                " synthetic assertions, 20,000 randomized delayed-action sequences");
    }
}
