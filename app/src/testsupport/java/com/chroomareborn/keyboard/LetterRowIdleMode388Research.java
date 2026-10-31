package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class LetterRowIdleMode388Research {
    private static long checks;
    private static void ok(boolean b,String info) {
        checks++;
        if(!b)throw new AssertionError(info);
    }
    public static void main(String[] args) {
        List<String> idle=Arrays.asList("I","The","Hey");
        List<String> centerOnly=Arrays.asList("","The","");
        ok(LetterRowIdleMode388.start("","",true,idle)==
                LetterRowIdleMode388.Mode.NEXT_WORD,"Idle screenshot must arm");
        ok("The".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,idle)),"Center is The");
        ok(LetterRowIdleMode388.start("","",true,centerOnly)==
                LetterRowIdleMode388.Mode.NEXT_WORD,"center only works");
        ok(LetterRowIdleMode388.start("","",false,idle)==
                LetterRowIdleMode388.Mode.UNAVAILABLE,"no ticket fail closed");
        ok(!LetterRowIdleMode388.finish(true,"","","",false),
                "expired ticket must never insert");
        ok(LetterRowIdleMode388.finish(true,"","","",true),
                "valid unchanged editor boundary");
        Random rng=new Random(38820261031L);
        for(int i=0;i<80000;i++){
            boolean ticket=rng.nextBoolean();
            List<String> opts=i%5==0?Arrays.asList("","",""):idle;
            boolean available=opts==idle && ticket;
            ok((LetterRowIdleMode388.start("","",ticket,opts)==
                    LetterRowIdleMode388.Mode.NEXT_WORD)==available,
                    "only idle ticket and live candidate arm");
            ok(LetterRowIdleMode388.finish(true,"","","",ticket)==ticket,
                    "only valid ticket accepts next word");
            ok(!LetterRowIdleMode388.finish(true,"","","typed",ticket),
                    "caret word inserted while finger moves rejects");
            ok(!LetterRowIdleMode388.finish(true,"","changed","",ticket),
                    "new composition invalidates idle");
            LetterRowPredictionSwipe386 gesture=
                    new LetterRowPredictionSwipe386(i%7,30f,20f,1f);
            if(i%2==0) {
                ok(gesture.release(i%7,30f,70f)==
                        LetterRowPredictionSwipe386.Direction.DOWN,"DOWN");
            } else {
                ok(gesture.release(i%7,90f,20f)==
                        LetterRowPredictionSwipe386.Direction.RIGHT,"RIGHT");
            }
        }
        System.out.println("PASS LetterRowIdleMode388 "+checks+
                " assertions over 80000 randomized idle/selection cases");
    }
}
