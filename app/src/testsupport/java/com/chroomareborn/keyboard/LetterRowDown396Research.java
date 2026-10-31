package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** 120,000 DOWN/middle + stale-selection comparisons; entirely synthetic. */
public final class LetterRowDown396Research {
    private static long checks;
    private static void ok(boolean condition,String message){
        checks++;
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        Random r=new Random(39620261031L);
        String[] words={"a","I","The","tell","working","keyboard"};
        for(int i=0;i<120000;i++){
            String center=words[i%words.length];
            int pattern=i%4;
            List<String> live=pattern==0?List.of(center):
                    pattern==1?Arrays.asList("",center,""):
                    pattern==2?Arrays.asList("left",center,"right"):
                    Arrays.asList("left","","right");
            boolean idle=i%3==0;
            String selected=LetterRowPredictionSwipe386.chosen(
                    LetterRowPredictionSwipe386.Direction.DOWN,live,idle);
            String expected=pattern==3?null:pattern==0?
                    (idle?center:null):center;
            ok(expected==null?selected==null:expected.equals(selected),
                    "only visual center qualifies; singleton requires idle mode");
            LetterRowPredictionSwipe386 gesture=
                    new LetterRowPredictionSwipe386(i%13,50f,10f,1f);
            int travel=32+r.nextInt(78);
            ok(gesture.release(i%13,50f,10f+travel)==
                    LetterRowPredictionSwipe386.Direction.DOWN,
                    "shorter deliberate DOWN remains recognized");
            LetterRowPredictionSwipe386 tap=
                    new LetterRowPredictionSwipe386(i%13,50f,10f,1f);
            int jitter=r.nextInt(30);
            ok(tap.release(i%13,50f,10f+jitter)==
                    LetterRowPredictionSwipe386.Direction.NONE,
                    "ordinary tap drift never becomes a correction");
            String rerendered=i%17==0?center+"x":center;
            String again=LetterRowPredictionSwipe386.chosen(
                    LetterRowPredictionSwipe386.Direction.DOWN,
                    pattern==0?List.of(rerendered):
                    pattern==1?Arrays.asList("",rerendered,""):
                    pattern==2?Arrays.asList("left",rerendered,"right"):
                    Arrays.asList("left","","right"),idle);
            if(selected!=null && !center.equals(rerendered))
                ok(!selected.equals(again),"changed center rejects stale lift");
        }
        System.out.println("PASS LetterRowDown396 "+checks+
                " assertions over 120000 single/three-chip DOWN and tap cases");
    }
}
