package com.chroomareborn.keyboard;

import java.util.Random;

/** Deterministic no-editor/no-device synthetic geometry research. */
public final class ThumbTapGuard377Research {
    private static long assertions;
    private static void check(boolean pass,String caseName){
        assertions++;
        if(!pass)throw new AssertionError(caseName);
    }
    private static ThumbTapGuard377 key(float x,float y){
        return new ThumbTapGuard377(1,40f,72f,x,y,1f);
    }
    private static void rejectsInvalid(){
        float[] invalid={Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1f,0f};
        for(float v:invalid){
            boolean rejected=false;
            try{new ThumbTapGuard377(1,v,72f,20f,30f,1f);}
            catch(IllegalArgumentException ok){rejected=true;}
            check(rejected,"reject invalid width");
            rejected=false;
            try{new ThumbTapGuard377(1,40f,72f,20f,30f,v);}
            catch(IllegalArgumentException ok){rejected=true;}
            check(rejected,"reject invalid density");
        }
        boolean rejected=false;
        try{new ThumbTapGuard377(-1,40f,72f,20f,30f,1f);}
        catch(IllegalArgumentException ok){rejected=true;}
        check(rejected,"reject negative pointer");
    }
    private static void behavior(){
        check(key(39,36).release(1,52,36),"+12px outside cell is a recoverable slight slip");
        check(!key(39,36).release(1,55,36),"+15px outside does not commit");
        check(key(20,71).release(1,20,88),"+16px vertical drift tolerated");
        check(!key(20,71).release(1,20,92),"+20px vertical drift canceled");
        ThumbTapGuard377 k=key(38,30);
        check(!k.move(1,65,30),"outside move cancels");
        check(!k.release(1,38,30),"reenter cannot resurrect");
        k=key(20,30);
        check(!k.move(2,20,30),"wrong pointer fails closed");
        check(!k.release(1,20,30),"late matching release stays canceled");
        k=key(20,30); k.cancel();
        check(!k.release(1,20,30),"cancel prevents ghost");
        k=key(20,30);
        check(k.release(1,20,30),"normal tap commits");
        check(!k.release(1,20,30),"duplicate UP never commits twice");
        k=key(0,20);
        check(!k.release(1,41,20),"crossing an entire key width is a swipe, not tap");
        k=key(20,30);
        check(!k.release(1,Float.NaN,30),"invalid UP coordinates fail closed");
    }
    private static void batch(){
        Random random=new Random(37720261030L);
        int oldWouldReject=0,acceptedSlip=0,deliberateSlidesRejected=0;
        for(int i=0;i<50000;i++){
            float y=3+random.nextFloat()*66f;
            float x=35+random.nextFloat()*5f;
            float drift=10.5f+random.nextFloat()*2.7f;
            ThumbTapGuard377 k=key(x,y);
            boolean accepted=k.release(1,40+drift,y);
            if(40+drift>50)oldWouldReject++;
            if(accepted)acceptedSlip++;
            check(accepted,"short edge overshoot accepted #"+i);
            ThumbTapGuard377 slide=key(2+random.nextFloat()*4, y);
            boolean rejected=!slide.release(1,45+random.nextFloat()*12,y);
            if(rejected)deliberateSlidesRejected++;
            check(rejected,"long swipe rejected #"+i);
        }
        check(oldWouldReject==50000,"10px baseline would reject generated edge slips");
        check(acceptedSlip==50000,"research edge slips accepted");
        check(deliberateSlidesRejected==50000,"simulated slides rejected");
        // No claim about on-device error rates or production gesture safety.
        System.out.println("PASS ThumbTapGuard377: "+assertions+
                " synthetic assertions, 50,000 generated edge-slips and 50,000 slides");
    }
    private static void twoThumbOrder(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        StringBuilder out=new StringBuilder();
        for(int i=0;i<1000;i++){
            ThumbTapGuard377 first=new ThumbTapGuard377(1,40,72,30,30,1);
            ThumbTapGuard377 second=new ThumbTapGuard377(2,40,72,10,30,1);
            long a=q.begin(),b=q.begin();
            boolean secondOk=second.release(2,12,30);
            q.complete(b,secondOk?()->out.append('b'):null);
            boolean firstOk=first.release(1,30,30);
            q.complete(a,firstOk?()->out.append('a'):null);
        }
        check(out.length()==2000,"2,000 synthetic overlapping keys retained");
        for(int i=0;i<2000;i+=2)check(out.charAt(i)=='a'&&out.charAt(i+1)=='b',"down-order preserved");
        check(!q.hasOpenPresses(),"no stale queue");
    }
    public static void main(String[] unused){
        rejectsInvalid(); behavior(); twoThumbOrder(); batch();
    }
}
