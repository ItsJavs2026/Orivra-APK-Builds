package com.chroomareborn.keyboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Entirely synthetic public-text and touch-policy cases, no Android/device data. */
public final class CenterDownSpaceOrder400Research {
    private static long assertions;
    private static void ok(boolean yes,String failure){
        assertions++;
        if(!yes)throw new AssertionError(failure);
    }
    private static void testCenterDown() {
        Random r=new Random(40020261031L);
        for(int i=0;i<150_000;i++){
            float dx=r.nextInt(45)-22,dy=r.nextInt(120)-20;
            PredictionSwipe372.Direction d=PredictionSwipe372.direction(
                    dx,dy,12f,false);
            if(d==PredictionSwipe372.Direction.DOWN)
                ok(dy>=32.04f && dy>=Math.abs(dx)*1.55f,
                        "spurious DOWN at "+i);
            else if(dy>=34f && dy>=Math.abs(dx)*1.55f)
                ok(false,"missing recognized DOWN at "+i);
            int width=i%4;
            List<String> chips=width==0?List.of("those you"):
                    width==1?List.of("thoseyou","those you","these you"):
                    width==2?List.of("thoseyou","","these you"):
                    List.of("thoseyou","those you","these you");
            int index=width==0?0:width==1?1:width==2?1:0;
            String chosen=PredictionSwipe372.choose(PredictionSwipe372.Direction.DOWN,
                    index,chips,List.of("unrelated"),false);
            String expected=width==0||width==1?"those you":null;
            ok(expected==null?chosen==null:expected.equals(chosen),
                    "only actual touched center eligible at "+i);
            ok(PredictionSwipe372.choose(PredictionSwipe372.Direction.DOWN,
                    index,chips,List.of("unrelated"),true)==null,
                    "Rise cannot accept center DOWN");
            LetterRowPredictionSwipe386 row=new LetterRowPredictionSwipe386(
                    i%13,10f,10f,1f);
            ok(row.earlyDownIntent(i%13,10f,23f),
                    "13dp vertical intent must preserve slower deliberate DOWN");
            ok(!row.earlyDownIntent(i%13,35f,23f),
                    "diagonal drift must not suppress normal long press");
            ok(!row.earlyDownIntent((i+1)%13,10f,23f),
                    "wrong pointer cannot arm DOWN intent");
        }
        for(int pixels=0;pixels<32;pixels++)
            ok(PredictionSwipe372.direction(0,pixels,12f,false)==
                    PredictionSwipe372.Direction.NONE,
                    "short tap drift became swipe");
        ok(PredictionSwipe372.direction(0,35,12f,false)==
                PredictionSwipe372.Direction.DOWN,"35px DOWN failed");
        ok(PredictionSwipe372.choose(PredictionSwipe372.Direction.DOWN,1,
                List.of("left","those you","right"),List.of(),false)
                .equals("those you"),"center under finger must match");
        ok(!PredictionSwipe372.safeRelease("thoseyou","thoseyou",
                true,true,false,false,true,false,false,false,true),
                "stale caret must not commit");
    }
    private static void testSpaceTouchOrder(){
        Random r=new Random(4000400L);
        String[] tokens={"t","h","o","s","e"," ","y","o","u"};
        for(int i=0;i<35_000;i++){
            HintKeyPressOrder353 sequencer=new HintKeyPressOrder353();
            ArrayList<Runnable> ui=new ArrayList<>();
            long[] keys=new long[tokens.length];
            for(int k=0;k<keys.length;k++)keys[k]=sequencer.begin();
            ArrayList<Integer> releases=new ArrayList<>();
            for(int j=0;j<keys.length;j++)releases.add(j);
            Collections.shuffle(releases,r);
            for(int j:releases) {
                String segment=tokens[j];
                sequencer.complete(keys[j],()->ui.add(()->{}));
                // The queue intentionally defers main-thread work in the
                // sequence of ACTION_DOWN, not alphabetical release order.
            }
            ok(!sequencer.hasOpenPresses(),"pending contacts after flush "+i);
            // A second independent sequence validates actual scheduled text.
            HintKeyPressOrder353 order=new HintKeyPressOrder353();
            ArrayDeque<Runnable> scheduled=new ArrayDeque<>();
            StringBuilder written=new StringBuilder();
            long[] down=new long[tokens.length];
            for(int j=0;j<tokens.length;j++)down[j]=order.begin();
            for(int j:releases){
                String text=tokens[j];
                order.complete(down[j],()->scheduled.add(()->written.append(text)));
            }
            while(!scheduled.isEmpty())scheduled.removeFirst().run();
            ok("those you".contentEquals(written),
                    "Space out of ACTION_DOWN order: "+written);
        }
        HintKeyPressOrder353 queue=new HintKeyPressOrder353();
        StringBuilder output=new StringBuilder();
        long alpha=queue.begin(),space=queue.begin(),next=queue.begin();
        queue.complete(next,()->output.append("y"));
        queue.cancel(space); // pointer cancel/draggable Space: no text write
        queue.complete(alpha,()->output.append("e"));
        ok("ey".contentEquals(output),"canceled Space must not insert");
        queue.cancelAll();
        ok(!queue.hasOpenPresses(),"input restart must clear pending work");
    }
    public static void main(String[] args){
        testCenterDown();testSpaceTouchOrder();
        System.out.println("PASS Research400: "+assertions+
                " synthetic DOWN, stale-caret, and ACTION_DOWN-space-order assertions");
    }
}
