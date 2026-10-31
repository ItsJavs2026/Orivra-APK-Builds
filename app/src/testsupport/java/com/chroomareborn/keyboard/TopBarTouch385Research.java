package com.chroomareborn.keyboard;

import java.util.Random;

/** Offline 385 synthetic threshold and cancellation regression batch. */
public final class TopBarTouch385Research {
    private static long checks;
    private static void check(boolean b,String why){
        checks++;
        if(!b)throw new AssertionError(why);
    }
    public static void main(String[] args){
        Random random=new Random(38520261030L);
        for(int i=0;i<30000;i++){
            float x=20+random.nextInt(70);
            float y=20+random.nextInt(40);
            float dx=random.nextInt(33)-16;
            float dy=random.nextInt(33)-16;
            TopBarTouch385 g=new TopBarTouch385(i%5,x,y,1f);
            check(g.move(i%5,x+dx,y+dy,i%2==0),"tap stream valid");
            check(g.release(i%5,x+dx,y+dy,i%2==0)==
                    TopBarTouch385.Decision.TAP,"small drift tap");
        }
        for(int i=0;i<30000;i++){
            float dx=(i%2==0?1:-1)*(35+random.nextInt(75));
            TopBarTouch385 g=new TopBarTouch385(i%7,100f,30f,1f);
            check(g.release(i%7,100f+dx,30f,false)==
                    (dx>0?TopBarTouch385.Decision.RIGHT:
                        TopBarTouch385.Decision.LEFT),"clear left/right");
        }
        for(int i=0;i<20000;i++){
            float dx=(i%2==0?1:-1)*(19+random.nextInt(14));
            TopBarTouch385 g=new TopBarTouch385(i%3,100f,30f,1f);
            check(g.release(i%3,100f+dx,30f,true)==
                    TopBarTouch385.Decision.CANCEL,"ambiguous glide canceled");
        }
        for(int i=0;i<10000;i++){
            TopBarTouch385 g=new TopBarTouch385(0,100f,80f,1f);
            float up=41+random.nextInt(50);
            check(g.release(0,100f,80f-up,true)==
                    TopBarTouch385.Decision.UP,"rise visible upward swipe");
        }
        for(int i=0;i<10000;i++){
            TopBarTouch385 g=new TopBarTouch385(0,100f,30f,1f);
            check(g.move(0,140f,30f,false),"start deliberate swipe");
            check(g.release(0,101f,30f,false)==
                    TopBarTouch385.Decision.CANCEL,"return cannot turn to tap");
        }
        System.out.println("PASS TopBarTouch385 "+checks+
                " deterministic assertions across 100,000 randomized gesture cases");
    }
}
