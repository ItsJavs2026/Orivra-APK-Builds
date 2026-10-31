package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic simulated multi-touch sequencing; not a handset test. */
public final class MixedThumbOrder380Research {
    private static long checks;
    private static void require(boolean truth,String message){
        checks++;
        if(!truth)throw new AssertionError(message);
    }
    private static void mixedReversed(){
        MixedThumbOrder380 q=new MixedThumbOrder380();
        StringBuilder out=new StringBuilder();
        long letter=q.down(),space=q.down();
        q.up(space,()->out.append(' '));
        require(out.length()==0,"Space must await earlier letter");
        q.up(letter,()->out.append('a'));
        require("a ".equals(out.toString()),"letter then Space down order");
        require(!q.hasOpenPresses(),"no pending presses");
    }
    private static void cancelledSpaceSwipe(){
        MixedThumbOrder380 q=new MixedThumbOrder380();
        StringBuilder out=new StringBuilder();
        long letter=q.down(),space=q.down();
        q.cancel(space); // Space swipe navigates cursor: no inserted space.
        q.up(letter,()->out.append('a'));
        require("a".equals(out.toString()),"swipe consumes space");
        require(!q.hasOpenPresses(),"space cancellation drained");
    }
    private static void cancellationAndStaleSession(){
        MixedThumbOrder380 q=new MixedThumbOrder380();
        StringBuilder out=new StringBuilder();
        long a=q.down(),b=q.down();
        q.up(b,()->out.append('b'));
        q.cancel(a);
        require("b".equals(out.toString()),"cancel one letter, keep next");
        long old=q.down(),other=q.down();
        q.up(other,()->out.append('X'));
        q.cancelAll();
        q.up(old,()->out.append('Y'));
        require("b".equals(out.toString()),"session reset drops old work");
    }
    private static void noDuplicates(){
        MixedThumbOrder380 q=new MixedThumbOrder380();
        StringBuilder out=new StringBuilder();
        long key=q.down();
        q.up(key,()->out.append('k'));
        q.up(key,()->out.append('k'));
        q.cancel(key);
        require("k".equals(out.toString()),"no duplicate release");
    }
    private static void boundedOverflow(){
        MixedThumbOrder380 q=new MixedThumbOrder380();
        List<Long> stale=new ArrayList<>();
        for(int i=0;i<24;i++)stale.add(q.down());
        long latest=q.down(); // fail closed and clear old transactions
        StringBuilder out=new StringBuilder();
        for(long token:stale)q.up(token,()->out.append('x'));
        q.up(latest,()->out.append('a'));
        require("a".equals(out.toString()),"stale overflow fails closed");
    }
    private static void stress(){
        Random r=new Random(38020261030L);
        String alphabet="abcdefghijklmnopqrstuvwx yz";
        for(int trial=0;trial<20000;trial++){
            MixedThumbOrder380 q=new MixedThumbOrder380();
            int n=2+r.nextInt(16);
            List<Long> tokens=new ArrayList<>();
            List<Integer> order=new ArrayList<>();
            List<Character> expected=new ArrayList<>();
            StringBuilder actual=new StringBuilder();
            for(int i=0;i<n;i++){
                tokens.add(q.down());
                order.add(i);
                expected.add(alphabet.charAt(r.nextInt(alphabet.length())));
            }
            Collections.shuffle(order,r);
            for(int index:order){
                final char symbol=expected.get(index);
                q.up(tokens.get(index),()->actual.append(symbol));
            }
            StringBuilder wanted=new StringBuilder();
            for(char c:expected)wanted.append(c);
            require(wanted.toString().equals(actual.toString()),
                    "20k varied reverse release sequences preserve down-order");
            require(!q.hasOpenPresses(),"queue drains each trial");
        }
    }
    public static void main(String[] args){
        mixedReversed();
        cancelledSpaceSwipe();
        cancellationAndStaleSession();
        noDuplicates();
        boundedOverflow();
        stress();
        System.out.println("PASS MixedThumbOrder380 "+checks+
                " deterministic assertions incl. 20,000 randomized press sequences");
    }
}
