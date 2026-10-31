package com.chroomareborn.keyboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic, fake Looper research. No IME wiring, user data or I/O. */
public final class SessionBoundEditorOrder384Research {
    private static long checks=0;
    private static void ok(boolean value,String why){
        checks++;
        if(!value)throw new AssertionError(why);
    }
    private static final class FakePoster implements SessionBoundEditorOrder384.Poster {
        final ArrayDeque<Runnable> pending=new ArrayDeque<>();
        boolean allow=true;
        @Override public boolean post(Runnable action){
            if(!allow)return false;
            pending.addLast(action);return true;
        }
        void drain(){while(!pending.isEmpty())pending.removeFirst().run();}
    }
    private static void actualMutationOrder(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        long letter=q.down(),space=q.down();
        ok(q.accepted(space,()->text.append(' '),()->true),"Space accepted");
        ok(text.length()==0,"Space cannot bypass earlier letter");
        ok(q.accepted(letter,()->text.append('a'),()->true),"Letter accepted");
        ok(text.length()==0,"No editor IPC inside touch callback");
        poster.drain();
        ok("a ".contentEquals(text),"Mutations preserve down order");
        ok(!q.hasOpenPresses()&&q.queuedCount()==0,"queue drained");
        ok(!q.accepted(letter,()->text.append('x'),()->true),"duplicate is ignored");
    }
    private static void postReleaseSessionChanges(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        final Object[] active={new Object()};
        final Object editor=active[0];
        final long[] generation={1};
        long key=q.down();
        q.accepted(key,()->text.append('x'),
                ()->active[0]==editor && generation[0]==1);
        generation[0]=2;active[0]=new Object();
        poster.drain();
        ok(text.length()==0,"Old editor callback rejected after focus change");
    }
    private static void cancelAllInvalidatesAlreadyPostedWork(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        long key=q.down();
        q.accepted(key,()->text.append('a'),()->true);
        q.cancelAll();
        poster.drain();
        ok(text.length()==0,"Posted action cannot survive queue reset");
        ok(!q.hasOpenPresses(),"No pending work after reset");
    }
    private static void canceledGestureDoesNotInsert(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        long letter=q.down(),swipe=q.down();
        ok(q.cancel(swipe),"Cursor swipe canceled");
        q.accepted(letter,()->text.append('a'),()->true);
        poster.drain();
        ok("a".contentEquals(text),"Canceled Space never inserts text");
        ok(!q.cancel(swipe),"Duplicate cancellation rejected");
    }
    private static void overflowClearsUnreleasedTokens(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        List<Long> stale=new ArrayList<>();
        for(int i=0;i<24;i++)stale.add(q.down());
        long newer=q.down();
        for(Long t:stale)ok(!q.accepted(t,()->text.append('x'),()->true),
                "Overflow stale token rejected");
        ok(q.accepted(newer,()->text.append('a'),()->true),"new token valid");
        poster.drain();
        ok("a".contentEquals(text),"Overflow fail closed");
    }
    private static void postingFailureDoesNotCommitOrHang(){
        FakePoster poster=new FakePoster();
        poster.allow=false;
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        long one=q.down(),two=q.down();
        q.accepted(two,()->text.append('b'),()->true);
        q.accepted(one,()->text.append('a'),()->true);
        poster.drain();
        ok(text.length()==0,"Failed poster does not execute callbacks");
        ok(q.queuedCount()==0&&!q.hasOpenPresses(),"Posting failure clears queue");
    }
    private static void predicateExceptionFailsClosed(){
        FakePoster poster=new FakePoster();
        SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
        StringBuilder text=new StringBuilder();
        long token=q.down();
        q.accepted(token,()->text.append('x'),()->{
            throw new IllegalStateException("Detached editor");
        });
        poster.drain();
        ok(text.length()==0,"Detached editor cannot receive old key");
    }
    private static void shuffledReleaseStress(){
        Random random=new Random(38420261030L);
        for(int run=0;run<50000;run++){
            FakePoster poster=new FakePoster();
            SessionBoundEditorOrder384 q=new SessionBoundEditorOrder384(poster);
            StringBuilder expected=new StringBuilder(),result=new StringBuilder();
            int n=2+random.nextInt(20);
            List<Long> tokens=new ArrayList<>();
            List<Integer> order=new ArrayList<>();
            List<Character> chars=new ArrayList<>();
            for(int i=0;i<n;i++){
                tokens.add(q.down());
                order.add(i);
                char ch=i%5==4?' ':(char)('a'+random.nextInt(26));
                chars.add(ch);
                expected.append(ch);
            }
            Collections.shuffle(order,random);
            for(int i:order){
                final char c=chars.get(i);
                ok(q.accepted(tokens.get(i),()->result.append(c),()->true),
                        "press accepted");
            }
            ok(result.length()==0,"No eager editor mutations");
            poster.drain();
            ok(expected.toString().contentEquals(result),
                    "FIFO editor sequence despite shuffled releases");
            ok(q.queuedCount()==0&&!q.hasOpenPresses(),"Queue drained");
        }
    }
    public static void main(String[] args){
        actualMutationOrder();
        postReleaseSessionChanges();
        cancelAllInvalidatesAlreadyPostedWork();
        canceledGestureDoesNotInsert();
        overflowClearsUnreleasedTokens();
        postingFailureDoesNotCommitOrHang();
        predicateExceptionFailsClosed();
        shuffledReleaseStress();
        System.out.println("PASS SessionBoundEditorOrder384 "+checks+
                " synthetic assertions across 50,000 shuffled press sequences");
    }
}
