package com.chroomareborn.keyboard;

import java.util.Random;

/**
 * Regression coverage for Android delivering the final finger displacement
 * only in ACTION_UP (no preceding MOVE frame), including armed SEND holds.
 * The IME uses gestureWithinSlop on the FINAL release coordinates.
 */
public final class EnterReleaseBoundaryTest {
    private static long checks=0;
    private static void expect(boolean b,String reason) {
        checks++;
        if(!b)throw new AssertionError(reason);
    }
    private static void release(float dx,float dy,float slop,boolean armed,
            boolean supportsSend,boolean sameSession,boolean preCancelled,
            boolean expectedAllowed) {
        boolean finalWithin=EnterKeyPolicy.gestureWithinSlop(dx,dy,slop);
        boolean cancelled=preCancelled || !finalWithin;
        boolean canSend=EnterKeyPolicy.shouldSendOnRelease(true,armed,
                cancelled,sameSession,supportsSend);
        boolean canNewline=EnterKeyPolicy.shouldInsertNewline(true,armed,
                cancelled,sameSession);
        expect((canSend||canNewline)==expectedAllowed,
                "unexpected final release for motion "+dx+","+dy);
        expect(!(canSend&&canNewline),
                "Enter must never send and insert newline together");
        expect(!canSend || (armed&&supportsSend&&sameSession&&finalWithin&&!preCancelled),
                "SEND without an armed, supported, stationary release");
        expect(!canNewline || (!armed&&sameSession&&finalWithin&&!preCancelled),
                "newline triggered by a drifting / armed release");
    }
    public static void main(String[] args) {
        release(0f,0f,22f,true,true,true,false,true);
        release(22f,-22f,22f,true,true,true,false,true);
        release(22.01f,0f,22f,true,true,true,false,false);
        release(0f,-22.01f,22f,true,true,true,false,false);
        release(Float.NaN,0f,22f,true,true,true,false,false);
        release(Float.POSITIVE_INFINITY,0f,22f,true,true,true,false,false);
        release(0f,0f,Float.NaN,true,true,true,false,false);
        release(0f,0f,0f,true,true,true,false,false);
        release(0f,0f,22f,true,true,false,false,false);
        release(0f,0f,22f,true,true,true,true,false);
        release(0f,0f,22f,false,true,true,false,true); // tap = newline only
        release(0f,0f,22f,true,false,true,false,false); // no implicit SEND
        release(0f,0f,22f,false,false,true,false,true);
        Random random=new Random(292650L);
        for(int n=0;n<100000;n++) {
            float dx=(random.nextFloat()-.5f)*160f;
            float dy=(random.nextFloat()-.5f)*160f;
            if(n%29==0)dx=Float.NaN;
            if(n%37==0)dy=Float.POSITIVE_INFINITY;
            boolean armed=random.nextBoolean();
            boolean supports=random.nextBoolean();
            boolean same=random.nextBoolean();
            boolean cancelled=random.nextBoolean();
            boolean within=EnterKeyPolicy.gestureWithinSlop(dx,dy,22f);
            boolean allowed=within && same && !cancelled &&
                    (armed?supports:true);
            release(dx,dy,22f,armed,supports,same,cancelled,allowed);
        }
        System.out.println("EnterReleaseBoundaryTest passed: 100000 "+
                "seeded final release scenarios, "+checks+" checks");
    }
}
