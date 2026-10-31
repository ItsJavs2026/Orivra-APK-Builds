package com.chroomareborn.keyboard;
import java.util.Random;
/** Research 390 non-Android boundary invariants, all fictional counts. */
public final class LearningCapacity390Research {
    private static long checks;
    private static void check(boolean b,String message){
        checks++;if(!b)throw new AssertionError(message);
    }
    public static void main(String[] args){
        check(LearningCapacity390.MAX_CORRECTIONS==384,"correction bound");
        check(LearningCapacity390.MAX_CONTEXT_KEYS==320,"context bound");
        check(LearningCapacity390.MAX_FREQUENCIES==400,"frequency bound");
        check(LearningCapacity390.fits(304,242,326),"verified union fits");
        check(!LearningCapacity390.fits(304,242,401),"freq overflow");
        check(!LearningCapacity390.fits(304,321,326),"context overflow");
        check(!LearningCapacity390.fits(385,242,326),"correction overflow");
        check(!LearningCapacity390.fits(-1,242,326),"negative count rejected");
        check(!LearningCapacity390.fits(304,-1,326),"negative context rejected");
        check(!LearningCapacity390.fits(304,242,-1),"negative frequency rejected");
        Random r=new Random(39020261031L);
        for(int i=0;i<200000;i++){
            int corrections=r.nextInt(500);
            int context=r.nextInt(500);
            int freq=r.nextInt(500);
            boolean expected=corrections<=384 && context<=320 && freq<=400;
            check(LearningCapacity390.fits(corrections,context,freq)==expected,
                    "bounded capacity matrix");
        }
        System.out.println("PASS LearningCapacity390 "+checks+
                " assertions across 200000 synthetic capacity combinations");
    }
}
